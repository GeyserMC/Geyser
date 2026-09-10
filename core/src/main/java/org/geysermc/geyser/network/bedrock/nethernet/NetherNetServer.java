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

import dev.kastle.netty.channel.nethernet.NetherNetChannelFactory;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetHTTPSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetServerSignaling;
import dev.kastle.netty.util.nethernet.NetherNetLogging;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
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
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GameOutcomeReporter;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GameOutcomeTransport;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GeyserStatusCollector;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.InbuiltIdentity;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.NativeProviderHostFactory;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderHostFactory;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderRuntimeConfiguration;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderRuntimeObservations;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.ProviderShutdown;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.WardenClaimAdapter;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
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
 * Runs NetherNet signalling alongside the RakNet listener: inbuilt HTTP signalling, NXS provider registration, or both.
 * Its state (signing identity, provider registration and DTLS identity) lives in the "nethernet" folder next to the config.
 */
public final class NetherNetServer implements EventRegistrar {
    private static final long GUID = new Random().nextLong();

    private final GeyserImpl geyser;
    private final GeyserConfig.SignallingConfig config;
    private final Path dataFolder;

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
        this.config = geyser.config().signalling();
        this.dataFolder = geyser.getBootstrap().getConfigFolder().resolve("nethernet");
    }

    public void start() {
        GeyserConfig.SignallingConfig.Mode mode = config.mode();
        if (mode == GeyserConfig.SignallingConfig.Mode.NONE) return;

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger().error("Failed to create the NetherNet data folder, NetherNet will not start!", e);
            return;
        }

        geyser.eventBus().subscribe(this, SessionJoinEvent.class, this::onSessionJoin);
        geyser.eventBus().subscribe(this, SessionDisconnectEventImpl.class, event -> refreshProviderStatus());

        if (mode.builtin()) startInbuilt();
        if (mode.nxs()) startProvider();
    }

    private void startInbuilt() {
        int port = signallingPort();
        if (port == geyser.config().java().port()) {
            logger().warning("Skipping inbuilt signalling: its TCP port matches the Java server port.");
            return;
        }

        // Start up NetherNet
        try {
            // Keep libdatachannel's own logging out of the way
            NetherNetLogging.setNativeLogLevel("WARN");

            // Create a private signing identity automatically on the first start.
            InbuiltIdentity.ensure(dataFolder);
            NetherNetHTTPSignaling.Builder signallingBuilder = new NetherNetHTTPSignaling.Builder()
                    .setIdentityKeystore(dataFolder.resolve("identity.p12").toFile(), "")
                    .setMotdProvider((host, remoteAddress) -> {
                        BedrockPong pong = geyser.getGeyserServer().onQuery(GUID, remoteAddress);

                        return new NetherNetServerSignaling.PongData.Builder()
                                .setServerName(pong.motd())
                                .setProtocol(pong.protocolVersion())
                                .setVersion(pong.version())
                                .setPlayerCount(pong.playerCount())
                                .setMaxPlayerCount(pong.maximumPlayerCount())
                                .build();
                    });

            this.signaling = signallingBuilder.build();

            this.inbuiltEventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            this.inbuiltInitialiser = new NetherNetChannelInitialiser(geyser);

            ServerBootstrap b = new ServerBootstrap();
            b.group(inbuiltEventLoopGroup)
                    .channelFactory(NetherNetChannelFactory.server(signaling))
                    .childHandler(inbuiltInitialiser);

            BedrockListener listener = geyser.config().bedrock();
            this.inbuiltChannel = b.bind(new InetSocketAddress(listener.address(), port)).sync().channel();

            logger().info("Inbuilt signalling started on http://" + listener.address() + ":" + port);
        } catch (Throwable e) {
            // Throwable: the WebRTC natives are not available on every platform
            closeInbuiltResources();
            logger().warning("Inbuilt signalling could not bind or initialize; check for an occupied TCP port. NXS can still start.");
            logger().debug("Inbuilt signalling failure: " + e);
        }
    }

    /**
     * @return the TCP port for inbuilt signalling; the configured port unless overridden with the geyserSignallingPort system property
     */
    private int signallingPort() {
        String signallingPort = System.getProperty("geyserSignallingPort", "");
        if (!signallingPort.isEmpty()) {
            try {
                int parsedPort = Integer.parseInt(signallingPort);
                if (parsedPort < 1 || parsedPort > 65535) {
                    throw new NumberFormatException("The signalling port must be between 1 and 65535 inclusive!");
                }
                logger().info("Signalling port set from system property: " + parsedPort);
                return parsedPort;
            } catch (NumberFormatException e) {
                logger().error(String.format("Invalid signalling port from system property: %s! Defaulting to configured port.", signallingPort + " (" + e.getMessage() + ")"));
            }
        }
        return config.port();
    }

    private void startProvider() {
        CompletableFuture.runAsync(() -> {
            ProviderStateStore store = null;
            ProviderTransport initializingTransport = null;
            try {
                BedrockListener listener = geyser.config().bedrock();
                ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(config, dataFolder, listener.address(), listener.port(), collectServerStatus().maxPlayers());
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
        BedrockPong pong = geyser.getGeyserServer().onQuery(GUID, new InetSocketAddress("127.0.0.1", 0));
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
