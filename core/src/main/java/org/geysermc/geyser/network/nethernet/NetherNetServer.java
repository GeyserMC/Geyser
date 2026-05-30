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

import java.net.InetSocketAddress;
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
 */
public class NetherNetServer {

    private static final long SIGNALING_CHECK_INTERVAL_SECONDS = 120;
    private static final String LOG_PREFIX = "[Nethernet] ";

    private final GeyserImpl geyser;
    private final GeyserLogger logger;
    private final DefaultEventLoopGroup playerEventLoopGroup;
    private final PlayFabTokenManager tokenManager;
    private final String connectionId;

    // Tracks live player connections so a signaling rebuild can cleanly disconnect
    // them before tearing down the event loops they run on. Self-managing: channels
    // are removed automatically when they close.
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
     * Rebuilds both signaling WebSockets with a fresh MCToken, preserving the
     * same connection ID. Existing WebRTC peer connections are unaffected.
     *
     * @return true if the signaling reconnected successfully
     */
    public synchronized boolean restartSignaling() {
        if (!running) {
            return start();
        }

        String mcToken = tokenManager.authenticate();
        if (mcToken == null) {
            logger.warning(LOG_PREFIX + "Signaling rebuild failed: could not get MCToken");
            return false;
        }

        closeChannels();
        if (bind(mcToken)) {
            logger.info(LOG_PREFIX + "Signaling rebuilt successfully");
            return true;
        } else {
            logger.warning(LOG_PREFIX + "Signaling rebuild failed, shutting down");
            shutdown();
            return false;
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
        this.workerGroup = new NioEventLoopGroup(2);

        NetherNetServerInitializer childHandler = new NetherNetServerInitializer(geyser, playerEventLoopGroup, playerChannels);

        // Each channel owns and disposes its own factory in NetherNetServerChannel#doClose.
        // Give each signaling endpoint a separate factory so closing both channels doesn't
        // dispose one shared native handle twice (which throws on the second dispose).
        PeerConnectionFactory legacyFactory = new PeerConnectionFactory();
        PeerConnectionFactory rpcFactory = new PeerConnectionFactory();

        try {
            ServerBootstrap legacyBootstrap = new ServerBootstrap();
            legacyBootstrap.group(bossGroup, workerGroup)
                    .channelFactory(NetherNetChannelFactory.server(legacyFactory, legacySignaling))
                    .childHandler(childHandler);
            this.legacyChannel = legacyBootstrap.bind(new InetSocketAddress(0)).sync().channel();

            ServerBootstrap rpcBootstrap = new ServerBootstrap();
            rpcBootstrap.group(bossGroup, workerGroup)
                    .channelFactory(NetherNetChannelFactory.server(rpcFactory, rpcSignaling))
                    .childHandler(childHandler);
            this.rpcChannel = rpcBootstrap.bind(new InetSocketAddress(0)).sync().channel();

            return true;
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Failed to bind: " + e.getMessage());
            this.legacySignaling = null;
            this.rpcSignaling = null;
            // A created channel disposes its own factory on close; dispose any factory
            // whose channel was never created.
            if (legacyChannel != null) {
                legacyChannel.close();
                legacyChannel = null;
            } else {
                try { legacyFactory.dispose(); } catch (Exception ignored) {}
            }
            if (rpcChannel != null) {
                rpcChannel.close();
                rpcChannel = null;
            } else {
                try { rpcFactory.dispose(); } catch (Exception ignored) {}
            }
            if (bossGroup != null) { bossGroup.shutdownGracefully(); bossGroup = null; }
            if (workerGroup != null) { workerGroup.shutdownGracefully(); workerGroup = null; }
            return false;
        }
    }

    private void closeChannels() {
        // Cleanly disconnect live players first. Closing each child channel fires
        // channelInactive, which disconnects the GeyserSession and synchronously
        // cancels its tick — so nothing is left ticking against the event loops we
        // shut down below (which would otherwise flood RejectedExecutionException).
        if (!playerChannels.isEmpty()) {
            logger.info(LOG_PREFIX + "Disconnecting " + playerChannels.size() + " player(s) for rebuild");
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

    private void checkSignaling() {
        if (!running) return;
        if (isSignalingAlive()) return;

        logger.info(LOG_PREFIX + "Signaling dead, rebuilding...");
        restartSignaling();
    }
}
