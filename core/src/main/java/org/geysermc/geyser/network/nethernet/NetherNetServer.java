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

import dev.kastle.netty.channel.nethernet.NetherNetAnswerDecorator;
import dev.kastle.netty.channel.nethernet.NetherNetChannelFactory;
import dev.kastle.netty.channel.nethernet.config.NetherChannelOption;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetHttpSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling;
import dev.kastle.webrtc.PeerConnectionFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Nethernet (WebRTC) server that accepts incoming connections from clients
 * using a connection ID. Connections pipe directly into Geyser's session
 * handling via the Bedrock protocol pipeline.
 *
 * Maintains one signaling connection to Microsoft, Type 7 (JSON-RPC): the
 * messaging endpoint with PmsgId, used by Bedrock 26.20+ and Education
 * 26.30+. (The Type 3 legacy WebSocket for pre-26.x Education died with the
 * 26.32 auto update.) The HTTP signaling front end for retail direct
 * connections is separate and served locally.
 *
 * Owns the full lifecycle: PlayFab MCToken acquisition, signaling WebSocket
 * management, periodic health checks, and automatic reconnection.
 *
 * A dead signaling WebSocket is reconnected in place with a fresh MCToken:
 * the server channels, WebRTC factories, event loops, and every established
 * peer connection stay untouched, so players never notice a signaling drop.
 * Only new joins are held up until the socket is back. The watchdog backs
 * off exponentially while the signaling service or PlayFab is unreachable.
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

    private Channel rpcChannel;
    private NetherNetXboxRpcSignaling rpcSignaling;

    private Channel httpChannel;
    private NetherNetHttpSignaling httpSignaling;
    private NetherNetCertificateManager certificateManager;

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

    public NetherNetServer(GeyserImpl geyser, DefaultEventLoopGroup playerEventLoopGroup,
                           String connectionId, String playfabCustomId, String playfabDeviceId) {
        this.geyser = geyser;
        this.logger = geyser.getLogger();
        this.playerEventLoopGroup = playerEventLoopGroup;
        this.tokenManager = new PlayFabTokenManager(logger, playfabCustomId, playfabDeviceId);
        this.connectionId = connectionId;
    }

    /**
     * Starts the Nethernet server. Acquires an MCToken via PlayFab,
     * opens the signaling WebSocket, and begins accepting WebRTC connections.
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

        // The connection ID clients type is the connection id followed by the
        // 32 hex pmid of the signaling MCToken. The pmid is bound to the
        // anonymous PlayFab account (verified: a full re-auth with the same
        // CustomId returns the same pmid), and the account identity is
        // persisted in connection-id.yml, so this value is stable across
        // restarts.
        String pmid = tokenManager.getPmsgId();
        if (pmid != null) {
            logger.info(LOG_PREFIX + "Listening on connection ID: "
                    + connectionId + pmid.replace("-", "").toLowerCase());
        } else {
            logger.warning(LOG_PREFIX + "The MCToken has no pmid claim: there is no connection ID to give out"
                    + " and Nethernet joins will not work (connection id number: " + connectionId + ")");
        }
        return true;
    }

    /**
     * Reconnects the signaling WebSocket in place with a fresh MCToken,
     * preserving the same connection ID. Existing WebRTC peer connections and
     * player sessions are unaffected. On failure the server keeps running;
     * established players keep playing and the watchdog retries.
     *
     * @return true if the signaling socket reconnected successfully
     */
    public boolean restartSignaling() {
        synchronized (reconnectLock) {
            if (!running) {
                return start();
            }

            NetherNetXboxRpcSignaling rpc = this.rpcSignaling;
            if (rpc == null) {
                return false; // racing a shutdown
            }

            // A manual restart is usually a response to something being wrong;
            // don't trust the cached token, get a fresh one.
            tokenManager.invalidate();
            boolean ok = reconnectSocket(rpc);
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
        return running && rpcChannel != null && rpcChannel.isActive();
    }

    public boolean isSignalingAlive() {
        NetherNetXboxRpcSignaling rpc = rpcSignaling;
        return rpc != null && rpc.isChannelAlive();
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getPmsgId() {
        return tokenManager.getPmsgId();
    }

    private boolean bind(String mcToken) {
        this.rpcSignaling = new NetherNetXboxRpcSignaling(connectionId, mcToken);
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup(workerThreads());

        NetherNetServerInitializer childHandler = new NetherNetServerInitializer(geyser, playerEventLoopGroup, playerChannels);

        // Each channel creates its factory pool only after its signaling
        // endpoint bound successfully (the supplier is invoked from doBind),
        // and owns and disposes that pool in NetherNetServerChannel#doClose.
        // A failed bind therefore never creates native engine state; there
        // is nothing to tear down on that path, and no teardown to race
        // engine initialization. Each channel invokes the supplier once, so
        // every signaling endpoint still gets its own pool and closing both
        // channels never disposes one shared native handle twice.
        //
        // One PeerConnectionFactory equals one native network thread carrying
        // the DTLS and SCTP work of every peer connection assigned to it, so
        // the pool size caps how many threads the data plane can spread
        // players across.
        try {
            ServerBootstrap rpcBootstrap = new ServerBootstrap();
            rpcBootstrap.group(bossGroup, workerGroup)
                    .channelFactory(NetherNetChannelFactory.server(NetherNetServer::createFactoryPool, rpcSignaling))
                    .childHandler(childHandler);
            this.rpcChannel = bindChannel(rpcBootstrap, new InetSocketAddress(0));

            // HTTP signaling: the direct connection front end, on TCP under
            // the same port RakNet serves on UDP. Purely additive and always
            // attempted; any failure logs a warning and everything else keeps
            // running, since updated clients silently fall back to RakNet.
            try {
                bindHttpSignaling(childHandler);
            } catch (Exception e) {
                logger.warning(LOG_PREFIX + "HTTP signaling unavailable (" + e.getMessage()
                        + "); direct connections will use RakNet");
            }

            return true;
        } catch (Exception e) {
            logger.error(LOG_PREFIX + "Failed to bind: " + e.getMessage());
            this.rpcSignaling = null;
            // A successfully bound channel disposes its own factory pool on
            // close; a channel whose bind failed never created one.
            if (rpcChannel != null) {
                rpcChannel.close();
                rpcChannel = null;
            }
            if (bossGroup != null) { bossGroup.shutdownGracefully(); bossGroup = null; }
            if (workerGroup != null) { workerGroup.shutdownGracefully(); workerGroup = null; }
            return false;
        }
    }

    /**
     * Binds a bootstrap and returns the bound channel, closing the netty
     * channel object when the bind fails: netty does not close a channel
     * whose registration succeeded but whose bind did not, which would
     * otherwise leak it (idle but registered) on the event loop.
     */
    private static Channel bindChannel(ServerBootstrap bootstrap, InetSocketAddress address) throws Exception {
        ChannelFuture future = bootstrap.bind(address);
        try {
            future.sync();
        } catch (Exception e) {
            future.channel().close();
            throw e;
        }
        return future.channel();
    }

    /**
     * Binds the HTTP signaling front end on the Bedrock port, TCP. Updated
     * clients (26.30+) probe this before falling back to RakNet, so even a
     * fast negative answer speeds their joins up; a completed exchange joins
     * them over NetherNet without RakNet at all.
     */
    private void bindHttpSignaling(NetherNetServerInitializer childHandler) throws Exception {
        // Clients refuse answers without the server identity assertion, so
        // without it every HTTP join would fail only after full native
        // negotiation. No identity means no listener: connection refused is
        // the fastest possible RakNet fallback.
        NetherNetAnswerDecorator identityDecorator = loadIdentityDecorator();

        // Manual certificate files win and are the ACME opt out; files that
        // are present but unloadable mean plaintext with an accurate warning
        // (never silent ACME enrollment against the operator's intent). Only
        // when no files exist at all does automatic management run.
        ManualTls manualTls = loadHttpSignalingTls();
        Supplier<SslContext> tlsSupplier;
        String tlsMode;
        if (manualTls != null) {
            if (manualTls.context() != null) {
                SslContext context = manualTls.context();
                tlsSupplier = () -> context;
                tlsMode = "TLS, manual certificate";
            } else {
                tlsSupplier = () -> null;
                tlsMode = "plaintext; manual certificate failed to load: " + manualTls.error();
            }
        } else {
            this.certificateManager = new NetherNetCertificateManager(
                    geyser.getBootstrap().getConfigFolder().resolve("nethernet"), logger);
            tlsSupplier = certificateManager::currentContext;
            tlsMode = "automatic certificate management";
        }
        this.httpSignaling = new NetherNetHttpSignaling(tlsSupplier, workerGroup);
        try {
            ServerBootstrap httpBootstrap = new ServerBootstrap();
            httpBootstrap.group(bossGroup, workerGroup)
                    .channelFactory(NetherNetChannelFactory.server(NetherNetServer::createFactoryPool, httpSignaling))
                    .option(NetherChannelOption.NETHER_SERVER_ANSWER_DECORATOR, identityDecorator)
                    .childHandler(childHandler);
            InetSocketAddress bindAddress = new InetSocketAddress(
                    geyser.config().bedrock().address(), geyser.config().bedrock().port());
            this.httpChannel = bindChannel(httpBootstrap, bindAddress);
            logger.info(LOG_PREFIX + "HTTP signaling on TCP port " + bindAddress.getPort() + " (" + tlsMode + ")");
            if (certificateManager != null) {
                certificateManager.start();
            }
        } catch (Exception e) {
            // The factory pool is created only after a successful bind, so a
            // failed bind leaves no native state behind.
            this.httpSignaling = null;
            if (certificateManager != null) {
                certificateManager.close();
                certificateManager = null;
            }
            throw e;
        }
    }

    /**
     * Loads (or on first use creates) the server identity that signs every
     * HTTP signaled answer's a=identity assertion. Clients refuse answers
     * without it on every HTTP path (HTTPS, plaintext behind a proxy, and
     * the future TOFU flow all hinge on it), so a failure here aborts the
     * HTTP listener entirely rather than serving joins doomed to fail after
     * negotiation.
     */
    private NetherNetAnswerDecorator loadIdentityDecorator() throws Exception {
        try {
            NetherNetServerIdentity identity = NetherNetServerIdentity.loadOrCreate(
                    geyser.getBootstrap().getConfigFolder().resolve("nethernet"));
            return identity::decorate;
        } catch (Exception e) {
            throw new IllegalStateException("server identity unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * The operator supplied TLS state: both fields null never occurs; a null
     * context with an error means files exist but could not be loaded.
     */
    private record ManualTls(SslContext context, String error) {
    }

    /**
     * Loads the operator supplied TLS material for HTTP signaling from
     * nethernet/cert.pem and nethernet/key.pem in the config folder (PEM
     * certificate chain and PKCS#8 private key). Null when absent (automatic
     * management applies); an unloadable pair is reported rather than
     * silently replaced, since supplying files is the ACME opt out.
     */
    private ManualTls loadHttpSignalingTls() {
        Path nethernetDir = geyser.getBootstrap().getConfigFolder().resolve("nethernet");
        Path cert = nethernetDir.resolve("cert.pem");
        Path key = nethernetDir.resolve("key.pem");
        if (!Files.exists(cert) || !Files.exists(key)) {
            return null;
        }
        try {
            return new ManualTls(SslContextBuilder.forServer(cert.toFile(), key.toFile()).build(), null);
        } catch (Exception e) {
            logger.warning(LOG_PREFIX + "Failed to load nethernet/cert.pem + key.pem (" + e.getMessage()
                    + "); serving plaintext. Fix or remove the files (removing them enables automatic certificates)");
            return new ManualTls(null, e.getMessage());
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

        if (rpcChannel != null) {
            rpcChannel.close().syncUninterruptibly();
            rpcChannel = null;
        }
        if (httpChannel != null) {
            httpChannel.close().syncUninterruptibly();
            httpChannel = null;
        }
        if (certificateManager != null) {
            certificateManager.close();
            certificateManager = null;
        }
        rpcSignaling = null;
        httpSignaling = null;
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
     * Watchdog: checks the signaling socket for liveness (including silent
     * half-open TCP, via the silence threshold) and reconnects it in place
     * when dead. Runs on the scheduler thread.
     */
    private void checkSignaling() {
        synchronized (reconnectLock) {
            if (!running) return;
            NetherNetXboxRpcSignaling rpc = this.rpcSignaling;
            if (rpc == null) return;

            if (rpc.isChannelAlive(SIGNALING_SILENCE_THRESHOLD_MILLIS)) {
                consecutiveReconnectFailures = 0;
                return;
            }

            long now = System.currentTimeMillis();
            if (now < nextReconnectAttemptAt) {
                return;
            }

            logger.info(LOG_PREFIX + "Signaling dead, reconnecting in place...");

            if (reconnectSocket(rpc)) {
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
     * Reconnects the signaling socket in place with a possibly cached MCToken.
     * Established peer connections are never touched.
     *
     * @return true if the socket reconnected successfully
     */
    private boolean reconnectSocket(NetherNetXboxRpcSignaling rpc) {
        String mcToken = tokenManager.authenticate();
        if (mcToken == null) {
            logger.warning(LOG_PREFIX + "Signaling reconnect failed: could not get MCToken");
            return false;
        }
        try {
            rpc.reconnect(mcToken);
            logger.info(LOG_PREFIX + "Signaling reconnected");
            return true;
        } catch (Exception e) {
            logger.warning(LOG_PREFIX + "Signaling reconnect failed: " + e.getMessage());
            return false;
        }
    }
}
