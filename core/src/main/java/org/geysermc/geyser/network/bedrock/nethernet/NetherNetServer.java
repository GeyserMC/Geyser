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

package org.geysermc.geyser.network.bedrock.nethernet;

import org.cloudburstmc.netty.util.nethernet.TrustedProxies;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetServerSignaling;
import org.cloudburstmc.netty.util.nethernet.NetherNetLogging;
import org.cloudburstmc.netty.util.nethernet.ServerIdentity;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.signalling.ProviderClient;
import org.cloudburstmc.netty.signalling.ProviderStateStore;
import org.cloudburstmc.netty.signalling.ProviderTransport;
import org.cloudburstmc.netty.signalling.ServerStatus;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.event.type.SessionDisconnectEventImpl;
import org.geysermc.geyser.network.BedrockPingHandler;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GameOutcomeReporter;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GameOutcomeTransport;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GeyserStatusCollector;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.NativeProviderHostFactory;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderHostFactory;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderRuntimeConfiguration;
import org.cloudburstmc.netty.signalling.provider.ProviderRuntimeObservations;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderShutdown;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.WardenClaimAdapter;
import org.geysermc.geyser.session.GeyserSession;
import tel.schich.libdatachannel.LibDataChannelArchDetect;
import tel.schich.libdatachannel.PeerConnectionConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The NetherNet (WebRTC) transport, used instead of RakNet: inbuilt HTTP signalling, NXS provider registration, or both,
 * all on the Bedrock address and port.
 * Its state (signing identity, provider registration and DTLS identity) lives in the "nethernet" folder next to the config.
 */
public final class NetherNetServer implements EventRegistrar {
    private static final long GUID = new Random().nextLong();

    private final GeyserImpl geyser;
    private final GeyserConfig.SignallingConfig config;
    /**
     * The UDP port for WebRTC: "webrtc-port", or the Bedrock port if that is 0.
     */
    private final int webrtcPort;
    private final Path dataFolder;
    private final BedrockPingHandler pingResponder;

    private final Object providerLifecycle = new Object();
    private volatile boolean stopping;
    private volatile ProviderClient providerClient;
    private volatile ProviderShutdown providerShutdown;
    private volatile WardenClaimAdapter wardenClaim;
    private final GameOutcomeReporter gameOutcomes = new GameOutcomeReporter();
    private volatile Supplier<ServerStatus> providerStatusSupplier = this::collectServerStatus;

    private EventLoopGroup eventLoopGroup;
    private Channel netherNetChannel;
    private NetherNetChannelInitialiser providerInitialiser;
    private NetherNetServerSignaling signaling;
    private EventLoopGroup inbuiltEventLoopGroup;
    private Channel inbuiltChannel;
    private NetherNetChannelInitialiser inbuiltInitialiser;

    public NetherNetServer(GeyserImpl geyser) {
        this.geyser = geyser;
        GeyserConfig.BedrockConfig bedrock = geyser.config().bedrock();
        this.config = bedrock.signalling();
        this.webrtcPort = bedrock.webrtcPort() == 0 ? bedrock.port() : bedrock.webrtcPort();
        this.dataFolder = geyser.getBootstrap().getConfigFolder().resolve("nethernet");
        this.pingResponder = new BedrockPingHandler(geyser);
    }

    public void start() {
        GeyserConfig.SignallingConfig.Mode mode = config.mode();
        if (mode == GeyserConfig.SignallingConfig.Mode.NONE) {
            logger().warning("Signalling is disabled, so Bedrock players cannot connect over NetherNet.");
            return;
        }
        boolean inbuilt = mode.builtin();
        boolean provider = mode.nxs();

        GeyserConfig.BedrockConfig listener = geyser.config().bedrock();
        InetAddress bindAddress = new InetSocketAddress(listener.address(), 0).getAddress();
        if (bindAddress != null && bindAddress.isLoopbackAddress()) {
            // ICE binds to this address too, and clients never offer loopback candidates, so no candidate pair can connect
            logger().warning("The Bedrock address " + listener.address() + " is a loopback address, which WebRTC cannot connect over, " +
                "so NetherNet connections will time out, even from this machine. Set \"address\" in the \"bedrock\" section to 0.0.0.0 " +
                "or to this machine's LAN address.");
        }
        if (listener.transport().raknet()) {
            if (webrtcPort == listener.port()) {
                logger().error("With the \"both\" transport, RakNet uses UDP port " + listener.port() + ", so NetherNet needs another one. " +
                    "Set \"webrtc-port\" in the \"bedrock\" section to a free UDP port. NetherNet will not start!");
                return;
            }
            if (listener.cloneRemotePort()) {
                logger().warning("clone-remote-port is enabled, but the \"both\" transport also needs UDP port " + webrtcPort + " for NetherNet. " +
                    "If your host only allows one port, set \"transport\" in the \"bedrock\" section to \"nethernet\" or \"raknet\".");
            }
        }

        if (inbuilt && listener.port() == geyser.config().java().port()) {
            // e.g. clone-remote-port: the Java server owns that TCP port
            inbuilt = false;
            if (!provider) {
                provider = true;
                logger().warning("Builtin signalling cannot share the Bedrock port " + listener.port() + " with the Java server; " +
                    "using the external signalling provider " + config.nxs().endpoint() + " instead.");
            } else {
                logger().warning("Builtin signalling cannot share the Bedrock port " + listener.port() + " with the Java server; " +
                    "only the external signalling provider is used.");
            }
        }

        try {
            // Picks this platform's WebRTC library out of the ones bundled for every platform
            LibDataChannelArchDetect.initialize();
        } catch (LinkageError e) {
            logger().error("NetherNet is not supported on " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + "); " +
                "set \"transport\" in the \"bedrock\" section of the config to \"raknet\".", e);
            return;
        }

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger().error("Failed to create the NetherNet data folder, NetherNet will not start!", e);
            return;
        }

        geyser.eventBus().subscribe(this, SessionJoinEvent.class, this::onSessionJoin);
        geyser.eventBus().subscribe(this, SessionDisconnectEventImpl.class, event -> refreshProviderStatus());

        // TODO TEST hybrid: both use UDP on the WebRTC port. They should share it through libjuice's in-process ICE mux,
        //  where the provider's listener takes unknown requests and inbuilt connections attach as agents; this relies on both
        //  binding the same address (inbuilt ICE leaves a wildcard address unset, the provider passes it explicitly).
        if (inbuilt) startInbuilt();
        if (provider) startProvider();
    }

    private void startInbuilt() {
        // Created on the first start and kept afterwards, as clients pin its public key
        ServerIdentity identity;
        try {
            identity = ServerIdentity.fromPemOrCreate(dataFolder.resolve("identity.pem").toFile(),
                    GeyserImpl.NAME + "-" + geyser.config().gameplay().serverName());
        } catch (Exception e) {
            logger().error("Could not create or load the inbuilt signalling identity in " + dataFolder
                    + ", inbuilt signalling will not start! Only delete identity.pem if it cannot be restored; "
                    + "a new identity makes every player confirm the server again", e);
            return;
        }

        // Start up NetherNet
        try {
            // Keep libdatachannel's own logging out of the way
            NetherNetLogging.setNativeLogLevel("WARN");

            GeyserConfig.SignallingConfig.BuiltinConfig builtin = geyser.config().bedrock().signalling().builtin();

            NetherNetHTTPSignaling.Builder signallingBuilder = new NetherNetHTTPSignaling.Builder()
                    .setIdentity(identity)
                    // The same "behind a proxy" settings RakNet uses, applied to the TCP listener
                    .setTrustedProxies(TrustedProxies.parse(geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps()))
                    .setProxyProtocol(geyser.config().advanced().bedrock().useHaproxyProtocol())
                    // Behind a proxy the peer is the proxy, signing its own assertion. The same
                    // setting already accepts the login chain it forwards
                    .setTokenTrust(geyser.config().advanced().bedrock().validateBedrockLogin()
                            ? TokenTrust.MINECRAFT_AUTH : TokenTrust.ANY)
                    .setMotdProvider((host, remoteAddress) -> {
                        BedrockPong pong = pingResponder.onQuery(GUID, remoteAddress);

                        return new NetherNetServerSignaling.PongData.Builder()
                                .setServerName(pong.motd())
                                .setProtocol(pong.protocolVersion())
                                .setVersion(pong.version())
                                .setPlayerCount(pong.playerCount())
                                .setMaxPlayerCount(pong.maximumPlayerCount())
                                .build();
                    });

            GeyserConfig.SignallingConfig.BuiltinConfig.HttpsConfig https = builtin.https();
            if (!https.certificate().isBlank()) {
                Path certificate = geyser.configDirectory().resolve(https.certificate());
                if (https.privateKey().isBlank()) {
                    signallingBuilder.setHttpsKeystore(certificate.toFile(), https.password());
                } else {
                    signallingBuilder.setHttpsPem(certificate.toFile(),
                            geyser.configDirectory().resolve(https.privateKey()).toFile(),
                            https.password().isBlank() ? null : https.password());
                }
            }

            BedrockListener listener = geyser.config().bedrock();
            // The channel binds HTTP signalling over TCP to the Bedrock port, and by default pins ICE to its UDP side.
            // When "webrtc-port" is another port, ICE goes there instead.
            boolean separateIcePort = webrtcPort != listener.port();
            this.signaling = signallingBuilder.setIceOnLocalPort(!separateIcePort).build();

            this.inbuiltEventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            this.inbuiltInitialiser = new NetherNetChannelInitialiser(geyser);

            ServerBootstrap b = new ServerBootstrap();
            b.group(inbuiltEventLoopGroup)
                    .channelFactory(NetherNetChannelFactory.server(signaling))
                    .childHandler(inbuiltInitialiser);
            if (separateIcePort) {
                // Runs on registration, so before the first connection can arrive
                b.handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        var options = channel.config();
                        options.setOption(NetherChannelOption.NETHER_PEER_CONNECTION_CONFIG,
                            pinIce(options.getOption(NetherChannelOption.NETHER_PEER_CONNECTION_CONFIG), listener.address(), webrtcPort));
                    }
                });
            }

            this.inbuiltChannel = b.bind(new InetSocketAddress(listener.address(), listener.port())).sync().channel();

            // TLS is served on the same port as plaintext, so both schemes reach it when configured
            String endpoint = https.certificate().isBlank()
                    ? "http://" + listener.address() + ":" + listener.port()
                    : "https:// and http:// on " + listener.address() + ":" + listener.port();
            logger().info("Builtin signalling started on " + endpoint
                + (separateIcePort ? ", with WebRTC on UDP port " + webrtcPort : ""));
        } catch (Throwable e) {
            // Throwable: the WebRTC natives are not available on every platform
            closeInbuiltResources();
            logger().warning("Inbuilt signalling could not bind or initialize; check that nothing else uses the Bedrock port over TCP.");
            logger().debug("Inbuilt signalling failure: " + e);
        }
    }

    /**
     * Pins ICE to one UDP port the way the server channel does for its own bind address, but for another port.
     */
    private static PeerConnectionConfiguration pinIce(PeerConnectionConfiguration config, String address, int port) {
        // A wildcard bind is left unset so ICE keeps gathering on every interface
        InetAddress host = new InetSocketAddress(address, port).getAddress();
        if (host != null && !host.isAnyLocalAddress()) {
            config = config.withBindAddress(host);
        }
        return config
            .withEnableIceUdpMux(true)
            .withPortRangeBegin(port)
            .withPortRangeEnd(port);
    }

    private void startProvider() {
        CompletableFuture.runAsync(() -> {
            ProviderStateStore store = null;
            ProviderTransport initializingTransport = null;
            try {
                BedrockListener listener = geyser.config().bedrock();
                ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(config, dataFolder, listener.address(), webrtcPort, collectServerStatus().maxPlayers());
                URI origin = runtime.origin();
                var statePath = runtime.stateDirectory();
                ProviderTransport transport;
                if (stopping) return;
                // Lock the durable instance before identity initialization or opening its endpoint.
                store = new ProviderStateStore(statePath);
                ProviderHostFactory factory = new NativeProviderHostFactory();
                eventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
                providerInitialiser = new NetherNetChannelInitialiser(geyser, gameOutcomes);
                ServerBootstrap bootstrap = new ServerBootstrap().group(eventLoopGroup).childHandler(providerInitialiser);
                ProviderHostFactory.Host host = factory.open(bootstrap, new InetSocketAddress(runtime.bindAddress(), runtime.udpPort()), Map.of("stateDirectory", statePath.toAbsolutePath().toString(), "profile", runtime.profile(),
                        "advertisedEndpoints", runtime.encodedAdvertisedEndpoints(),
                        "localDevelopment", Boolean.toString(Set.of("127.0.0.1", "localhost", "[::1]").contains(origin.getHost())))).toCompletableFuture().get(30, TimeUnit.SECONDS);
                netherNetChannel = host.channel();
                transport = host.transport();
                host.warnings().forEach(message -> logger().warning(message));
                if (stopping) {
                    transport.close();
                    closeNetworkResources();
                    return;
                }
                initializingTransport = transport;
                transport = new GameOutcomeTransport(transport, gameOutcomes);
                ProviderClient client = new ProviderClient(runtime.clientConfiguration(), store, transport,
                        () -> providerStatusSupplier.get(), () -> ProviderRuntimeObservations.health(geyser.getSessionManager().size(), runtime.capacity(), System.currentTimeMillis(), GeyserImpl.VERSION), message -> logger().warning(message));
                store = null; // ProviderClient now owns its lifetime.
                initializingTransport = null;
                synchronized (providerLifecycle) {
                    if (stopping) {
                        client.close();
                        return;
                    }
                    try {
                        providerShutdown = new ProviderShutdown(client::stop, this::closeNetworkResources, message -> logger().warning(message));
                    } catch (RuntimeException failure) {
                        client.close();
                        throw failure;
                    }
                    providerClient = client;
                    wardenClaim = new WardenClaimAdapter(client);
                }
                client.start().whenComplete((registration, failure) -> {
                    if (failure != null) {
                        logger().error("Provider startup failed: " + providerFailure(failure));
                        stopProvider();
                        return;
                    }
                    logger().info(ProviderRuntimeObservations.registrationMessage(registration));
                    WardenClaimAdapter claim = wardenClaim;
                    if (claim != null)
                        claim.current().thenAccept(action -> action.ifPresent(value -> logger().info(value.message())));
                });
            } catch (Throwable e) {
                // Throwable: the WebRTC natives are not available on every platform
                if (initializingTransport != null) initializingTransport.close();
                logger().error("Provider startup failed: " + (e instanceof IOException ? e.getMessage() : providerFailure(e)));
                closeNetworkResources();
            } finally {
                if (store != null) try {
                    store.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    private ServerStatus collectServerStatus() {
        BedrockPong pong = pingResponder.onQuery(GUID, new InetSocketAddress("127.0.0.1", 0));
        return GeyserStatusCollector.snapshot(pong, geyser.getSessionManager().size(), "", 0);
    }

    /**
     * Programmatic complete status override; panel fixed values still take precedence at the provider.
     */
    public void setServerStatus(ServerStatus snapshot) {
        providerStatusSupplier = () -> snapshot;
        refreshProviderStatus();
    }

    public void setServerStatusSupplier(Supplier<ServerStatus> supplier) {
        providerStatusSupplier = Objects.requireNonNull(supplier);
        refreshProviderStatus();
    }

    public void restoreAutomaticServerStatus() {
        providerStatusSupplier = this::collectServerStatus;
        refreshProviderStatus();
    }

    public ServerStatus serverStatus() {
        return providerStatusSupplier.get();
    }

    public @Nullable ProviderClient providerClient() {
        return providerClient;
    }

    public @Nullable WardenClaimAdapter wardenClaim() {
        return wardenClaim;
    }

    public @Nullable Channel providerChannel() {
        return netherNetChannel;
    }

    public GameOutcomeReporter gameOutcomes() {
        return gameOutcomes;
    }

    public static String providerFailure(Throwable failure) {
        while (failure.getCause() != null && (failure instanceof CompletionException || failure instanceof ExecutionException))
            failure = failure.getCause();
        return failure instanceof ProviderClient.ProviderException ? failure.getMessage() : failure.getClass().getSimpleName();
    }

    private void refreshProviderStatus() {
        ProviderClient client = providerClient;
        if (client != null) client.requestStatusRefresh();
    }

    private void onSessionJoin(SessionJoinEvent event) {
        if (event.connection() instanceof GeyserSession session
                && !session.getUpstream().getSession().isSubClient()) {
            gameOutcomes.joined(session.getUpstream().getSession().getPeer().getChannel());
        }
        refreshProviderStatus();
    }

    public void shutdown() {
        synchronized (providerLifecycle) {
            if (stopping) return;
            stopping = true;
            geyser.eventBus().unregisterAll(this);
            closeInbuiltResources();
            stopProvider();
        }
    }

    private void stopProvider() {
        synchronized (providerLifecycle) {
            if (providerShutdown != null) {
                providerShutdown.close();
                providerShutdown = null;
            } else closeNetworkResources();
            providerClient = null;
            wardenClaim = null;
        }
    }

    private void closeInbuiltResources() {
        if (inbuiltChannel != null) inbuiltChannel.close();
        if (signaling != null) signaling.close();
        if (inbuiltEventLoopGroup != null) inbuiltEventLoopGroup.shutdownGracefully();
        if (inbuiltInitialiser != null) inbuiltInitialiser.getEventLoopGroup().shutdownGracefully();
    }

    private void closeNetworkResources() {
        if (this.netherNetChannel != null) {
            this.netherNetChannel.close();
        }
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully();
        }
        if (this.providerInitialiser != null) {
            this.providerInitialiser.getEventLoopGroup().shutdownGracefully();
        }
    }

    private GeyserLogger logger() {
        return geyser.getLogger();
    }
}
