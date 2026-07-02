/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.network.nethernet;

import dev.kastle.netty.channel.nethernet.NetherNetChannelFactory;
import dev.kastle.netty.channel.nethernet.signaling.AbstractNetherNetXboxSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetXboxSignaling;
import dev.kastle.webrtc.PeerConnectionFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Nethernet (WebRTC) server that accepts incoming connections from clients
 * using a connection ID. Connections pipe directly into Geyser's session
 * handling via the Bedrock protocol pipeline.
 *
 * Maintains two signaling connections:
 * - Type 3 (legacy): per-network-ID endpoint, used by Education Edition
 * - Type 7 (JSON-RPC): messaging endpoint with PmsgId, used by Bedrock 1.26.20+
 *
 * Owns the full lifecycle: PlayFab MCToken acquisition, signaling WebSocket
 * management, periodic health checks, and automatic reconnection.
 *
 * A dead signaling WebSocket is reconnected in place with a fresh MCToken:
 * the server channels, WebRTC factories, event loops, and every established
 * peer connection stay untouched, so players never notice a signaling drop.
 * Only new joins are held up until the socket is back. Each socket recovers
 * independently; the watchdog backs off exponentially while the signaling
 * service or PlayFab is unreachable.
 */
public class NetherNetServer {

    private static final long SIGNALING_CHECK_INTERVAL_SECONDS = 10;
    /**
     * Max tolerated silence before a socket is considered dead. The signaling
     * layer sends a WebSocket protocol ping every 15 seconds, whose pong is
     * inbound traffic, so a healthy socket never comes close to this.
     */
    private static final long SIGNALING_SILENCE_THRESHOLD_MILLIS = 45_000;
    private static final long RECONNECT_BASE_DELAY_MILLIS = 10_000;
    private static final long RECONNECT_MAX_DELAY_MILLIS = 120_000;
    private static final String LOG_PREFIX = "[Nethernet] ";

    private final GeyserImpl geyser;
    private final GeyserLogger logger;
    private final DefaultEventLoopGroup playerEventLoopGroup;
    private final PlayFabTokenManager tokenManager;
    private final String connectionId;

    // Tracks live player connections so a real teardown (shutdown or an
    // API-initiated stop with players online) can cleanly disconnect them
    // before the event loops they run on go away. On a normal Geyser shutdown
    // the session manager has already disconnected everyone and this group is
    // empty. Signaling reconnects never touch it. Self-managing: channels are
    // removed automatically when they close.
    private final ChannelGroup playerChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel legacyChannel;
    private NetherNetXboxSignaling legacySignaling;

    private Channel rpcChannel;
    private NetherNetXboxRpcSignaling rpcSignaling;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> signalingCheckTask;
    private volatile boolean running;

    // Reconnects can block for tens of seconds (PlayFab auth plus the connect
    // timeout). They serialize on their own lock so shutdown() and start(),
    // which use the instance monitor, are never held up behind one. A shutdown
    // racing a reconnect is safe: closing the signaling makes the in-flight
    // reconnect fail cleanly.
    private final Object reconnectLock = new Object();
    // Watchdog reconnect state, only touched under reconnectLock.
    private long nextReconnectAttemptAt;
    private int consecutiveReconnectFailures;

    public NetherNetServer(GeyserImpl geyser, DefaultEventLoopGroup playerEventLoopGroup, String connectionId) {
        this.geyser = geyser;
        this.logger = geyser.getLogger();
        this.playerEventLoopGroup = playerEventLoopGroup;
        this.tokenManager = new PlayFabTokenManager(logger);
        this.connectionId = connectionId;
    }

    /**
     * Starts the Nethernet server. Acquires an MCToken via PlayFab,
     * opens both signaling WebSockets, and begins accepting WebRTC connections.
     *
     * @return true if the server started successfully
     */
    public synchronized boolean start() {
        if (running) {
            return true;
        }

        String mcToken = tokenManager.authenticate();
        if (mcToken == null) {
            logger.error(LOG_PREFIX + "Failed to obtain MCToken from PlayFab");
            return false;
        }

        if (!bind(mcToken)) {
            shutdown();
            return false;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        signalingCheckTask = scheduler.scheduleAtFixedRate(this::checkSignaling,
                SIGNALING_CHECK_INTERVAL_SECONDS, SIGNALING_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        running = true;
        logger.info(LOG_PREFIX + "Listening on connection ID: " + connectionId);
        return true;
    }

    /**
     * Reconnects both signaling WebSockets in place with a fresh MCToken,
     * preserving the same connection ID. Existing WebRTC peer connections and
     * player sessions are unaffected. On failure the server keeps running;
     * established players keep playing and the watchdog retries.
     *
     * @return true if both signaling sockets reconnected successfully
     */
    public boolean restartSignaling() {
        synchronized (reconnectLock) {
            if (!running) {
                return start();
            }

            NetherNetXboxSignaling legacy = this.legacySignaling;
            NetherNetXboxRpcSignaling rpc = this.rpcSignaling;
            if (legacy == null || rpc == null) {
                return false; // racing a shutdown
            }

            // A manual restart is usually a response to something being wrong;
            // don't trust the cached token, get a fresh one.
            tokenManager.invalidate();
            boolean ok = reconnectSockets(legacy, rpc);
            if (ok) {
                logger.info(LOG_PREFIX + "Signaling reconnected (players unaffected)");
            } else {
                logger.warning(LOG_PREFIX + "Signaling reconnect failed; will keep retrying in the background");
            }
            return ok;
        }
    }

    public synchronized void shutdown() {
        running = false;
        if (signalingCheckTask != null) {
            signalingCheckTask.cancel(false);
            signalingCheckTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        closeChannels();
    }

    public boolean isRunning() {
        return running
                && (legacyChannel != null && legacyChannel.isActive())
                && (rpcChannel != null && rpcChannel.isActive());
    }

    public boolean isSignalingAlive() {
        boolean legacyAlive = legacySignaling != null && legacySignaling.isChannelAlive();
        boolean rpcAlive = rpcSignaling != null && rpcSignaling.isChannelAlive();
        return legacyAlive && rpcAlive;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getPmsgId() {
        return tokenManager.getPmsgId();
    }

    private boolean bind(String mcToken) {
        this.legacySignaling = new NetherNetXboxSignaling(connectionId, mcToken);
        this.rpcSignaling = new NetherNetXboxRpcSignaling(connectionId, mcToken);
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup(workerThreads());

        NetherNetServerInitializer childHandler = new NetherNetServerInitializer(geyser, playerEventLoopGroup, playerChannels);

        // Each channel owns and disposes its own factory pool in
        // NetherNetServerChannel#doClose. Give each signaling endpoint a
        // separate pool so closing both channels doesn't dispose one shared
        // native handle twice (which throws on the second dispose).
        //
        // One PeerConnectionFactory equals one native network thread carrying
        // the DTLS and SCTP work of every peer connection assigned to it, so
        // the pool size caps how many threads the data plane can spread
        // players across.
        List<PeerConnectionFactory> legacyFactories = createFactoryPool();
        List<PeerConnectionFactory> rpcFactories = createFactoryPool();

        try {
            ServerBootstrap legacyBootstrap = new ServerBootstrap();
            legacyBootstrap.group(bossGroup, workerGroup)
                    .channelFactory(NetherNetChannelFactory.server(legacyFactories, legacySignaling))
                    .childHandler(childHandler);
            this.legacyChannel = legacyBootstrap.bind(new InetSocketAddress(0)).sync().channel();

            ServerBootstrap rpcBootstrap = new ServerBootstrap();
            rpcBootstrap.group(bossGroup, workerGroup)
                    .channelFactory(NetherNetChannelFactory.server(rpcFactories, rpcSignaling))
                    .childHandler(childHandler);
            this.rpcChannel = rpcBootstrap.bind(new InetSocketAddress(0)).sync().channel();

            return true;
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Failed to bind: " + e.getMessage());
            this.legacySignaling = null;
            this.rpcSignaling = null;
            // A created channel disposes its own factory pool on close; dispose
            // any pool whose channel was never created.
            if (legacyChannel != null) {
                legacyChannel.close();
                legacyChannel = null;
            } else {
                disposeFactories(legacyFactories);
            }
            if (rpcChannel != null) {
                rpcChannel.close();
                rpcChannel = null;
            } else {
                disposeFactories(rpcFactories);
            }
            if (bossGroup != null) { bossGroup.shutdownGracefully(); bossGroup = null; }
            if (workerGroup != null) { workerGroup.shutdownGracefully(); workerGroup = null; }
            return false;
        }
    }

    /**
     * Sizes the netty worker group that runs every Nethernet player's Bedrock
     * pipeline. Scales with the host but stays bounded; the previous fixed
     * value of 2 was a bottleneck at high player counts.
     */
    private static int workerThreads() {
        return Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
    }

    /**
     * Creates the WebRTC factory pool for one signaling endpoint. Each factory
     * spawns three native threads even when idle, so the pool stays small on
     * small hosts and caps at four.
     */
    private static List<PeerConnectionFactory> createFactoryPool() {
        int size = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        List<PeerConnectionFactory> pool = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            pool.add(new PeerConnectionFactory());
        }
        return pool;
    }

    private static void disposeFactories(List<PeerConnectionFactory> factories) {
        for (PeerConnectionFactory factory : factories) {
            try { factory.dispose(); } catch (Exception ignored) {}
        }
    }

    private void closeChannels() {
        // Cleanly disconnect any players still online first. Closing each child
        // channel fires channelInactive, which disconnects the GeyserSession and
        // synchronously cancels its tick — so nothing is left ticking against the
        // event loops we shut down below (which would otherwise flood
        // RejectedExecutionException). On a normal Geyser shutdown the session
        // manager has already emptied this group.
        if (!playerChannels.isEmpty()) {
            logger.info(LOG_PREFIX + "Disconnecting " + playerChannels.size() + " player(s) for teardown");
            playerChannels.close().awaitUninterruptibly(3, TimeUnit.SECONDS);
        }

        if (legacyChannel != null) {
            legacyChannel.close().syncUninterruptibly();
            legacyChannel = null;
        }
        if (rpcChannel != null) {
            rpcChannel.close().syncUninterruptibly();
            rpcChannel = null;
        }
        legacySignaling = null;
        rpcSignaling = null;
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            workerGroup = null;
        }
    }

    /**
     * Watchdog: checks both signaling sockets for liveness (including silent
     * half-open TCP, via the silence threshold) and reconnects only the dead
     * ones, in place. Runs on the scheduler thread.
     */
    private void checkSignaling() {
        synchronized (reconnectLock) {
            if (!running) return;
            NetherNetXboxSignaling legacy = this.legacySignaling;
            NetherNetXboxRpcSignaling rpc = this.rpcSignaling;
            if (legacy == null || rpc == null) return;

            boolean legacyDead = !legacy.isChannelAlive(SIGNALING_SILENCE_THRESHOLD_MILLIS);
            boolean rpcDead = !rpc.isChannelAlive(SIGNALING_SILENCE_THRESHOLD_MILLIS);
            if (!legacyDead && !rpcDead) {
                consecutiveReconnectFailures = 0;
                return;
            }

            long now = System.currentTimeMillis();
            if (now < nextReconnectAttemptAt) {
                return;
            }

            logger.info(LOG_PREFIX + "Signaling dead ("
                    + (legacyDead ? (rpcDead ? "legacy + JSON-RPC" : "legacy") : "JSON-RPC")
                    + "), reconnecting in place...");

            if (reconnectSockets(legacyDead ? legacy : null, rpcDead ? rpc : null)) {
                consecutiveReconnectFailures = 0;
                // Small grace period so a socket that dies again immediately after
                // connecting doesn't get hammered in a tight loop.
                nextReconnectAttemptAt = now + RECONNECT_BASE_DELAY_MILLIS;
            } else {
                consecutiveReconnectFailures++;
                // The cached token may be the reason the service refused us.
                tokenManager.invalidate();
                long delay = Math.min(RECONNECT_MAX_DELAY_MILLIS,
                        RECONNECT_BASE_DELAY_MILLIS << Math.min(consecutiveReconnectFailures, 4));
                nextReconnectAttemptAt = now + delay;
                logger.warning(LOG_PREFIX + "Signaling reconnect failed (attempt " + consecutiveReconnectFailures
                        + "), next attempt in " + (delay / 1000) + "s");
            }
        }
    }

    /**
     * Reconnects the given sockets (null means healthy, skip) in place with a
     * possibly cached MCToken. Established peer connections are never touched.
     *
     * @return true if every requested socket reconnected successfully
     */
    private boolean reconnectSockets(NetherNetXboxSignaling legacy, NetherNetXboxRpcSignaling rpc) {
        if (legacy == null && rpc == null) {
            return true;
        }
        String mcToken = tokenManager.authenticate();
        if (mcToken == null) {
            logger.warning(LOG_PREFIX + "Signaling reconnect failed: could not get MCToken");
            return false;
        }

        boolean ok = true;
        if (legacy != null) {
            ok = reconnectOne("legacy", legacy, mcToken);
        }
        if (rpc != null) {
            ok &= reconnectOne("JSON-RPC", rpc, mcToken);
        }
        return ok;
    }

    private boolean reconnectOne(String name, AbstractNetherNetXboxSignaling signaling, String mcToken) {
        if (signaling == null) {
            return false;
        }
        try {
            signaling.reconnect(mcToken);
            logger.info(LOG_PREFIX + "Signaling (" + name + ") reconnected");
            return true;
        } catch (Exception e) {
            logger.warning(LOG_PREFIX + "Signaling (" + name + ") reconnect failed: " + e.getMessage());
            return false;
        }
    }
}
