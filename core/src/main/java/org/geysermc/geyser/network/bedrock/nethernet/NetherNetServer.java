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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPServerSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.ProviderDiagnostic;
import org.cloudburstmc.netty.signaling.ProviderStateStore;
import org.cloudburstmc.netty.signaling.ProviderTransport;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.cloudburstmc.netty.signaling.provider.NativeProviderHostFactory;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.cloudburstmc.netty.signaling.provider.ProviderShutdown;
import org.cloudburstmc.netty.util.nethernet.NetherNetLogging;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;
import org.cloudburstmc.netty.util.nethernet.SecretValue;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.cloudburstmc.netty.util.nethernet.TrustedProxies;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.configuration.GeyserPluginConfig;
import org.geysermc.geyser.event.type.SessionDisconnectEventImpl;
import org.geysermc.geyser.network.BedrockPingHandler;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.network.bedrock.nethernet.signaling.provider.GameOutcomeReporter;
import org.geysermc.geyser.network.bedrock.nethernet.signaling.provider.GeyserStatusCollector;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;
import tel.schich.libdatachannel.LibDataChannelArchDetect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The NetherNet (WebRTC) transport, used instead of RakNet: inbuilt HTTP signaling, NXS provider registration, or both,
 * all on the Bedrock address, and on the Bedrock port unless "webrtc-port" or -DgeyserSignalingPort move them.
 * Its state (signing identity, provider registration and DTLS identity) lives in the "nethernet" folder next to the config.
 */
public final class NetherNetServer implements EventRegistrar {
    private static final long GUID = new Random().nextLong();
    /**
     * Asked for this machine's public address unless {@code -DgeyserStunServers} says otherwise. Two operators, as
     * either may be blocked where a server runs.
     */
    private static final List<String> DEFAULT_STUN_SERVERS =
        List.of("stun:stun.cloudflare.com:3478", "stun:stun.l.google.com:19302");
    /**
     * Headers built-in signaling adds to every response, as comma separated "Name: value" entries. Only a host sets
     * them, so they have no config option.
     */
    public static final String ADDITIONAL_HEADERS_ENV = "GEYSER_ADDITIONAL_HEADERS";
    /**
     * Holds the provider's link for the operator, in the data folder.
     */
    private static final String OPERATOR_LINK_FILE = "operator-link.txt";
    /**
     * How long {@link #awaitShutdown()} waits, the window ProviderShutdown keeps the JVM alive for the same stop.
     */
    private static final long SHUTDOWN_WAIT_SECONDS = 20;
    /**
     * Whether "nethernet-debug" raised NetherNet's own logs, so a reload with it off lowers them again.
     */
    private static boolean debugLogging;

    private final GeyserImpl geyser;
    private final GeyserConfig.SignalingConfig config;
    /**
     * The UDP port for WebRTC: "webrtc-port", or the Bedrock port if that is 0.
     */
    private final int webrtcPort;
    /**
     * The TCP port built-in signaling listens on.
     */
    private final int signalingPort;
    private final Path dataFolder;
    private final BedrockPingHandler pingResponder;

    private final Object providerLifecycle = new Object();
    private volatile boolean stopping;
    private volatile ProviderClient providerClient;
    /** The id the external signaling service knows this server by, null until registered. */
    private volatile @Nullable String providerInstanceId;
    private volatile ProviderShutdown providerShutdown;
    /**
     * The provider's start, which stops what it opened itself if shut down midway.
     */
    private volatile CompletableFuture<Void> providerStart = CompletableFuture.completedFuture(null);
    /**
     * The last provider stop, done once its state directory and UDP port are free.
     */
    private volatile CompletionStage<Void> providerStopped = CompletableFuture.completedFuture(null);
    private final GameOutcomeReporter gameOutcomes = new GameOutcomeReporter(this::reportGameOutcome);
    private volatile Supplier<ServerStatus> providerStatusSupplier = this::collectServerStatus;

    private EventLoopGroup eventLoopGroup;
    private Channel netherNetChannel;
    private NetherNetChannelInitialiser providerInitialiser;
    private volatile NetherNetHTTPServerSignaling signaling;
    private volatile boolean builtinStun;
    private EventLoopGroup inbuiltEventLoopGroup;
    private volatile Channel inbuiltChannel;
    private NetherNetChannelInitialiser inbuiltInitialiser;

    public NetherNetServer(GeyserImpl geyser) {
        this.geyser = geyser;
        GeyserConfig.BedrockConfig bedrock = geyser.config().bedrock();
        this.config = bedrock.signaling();
        this.webrtcPort = bedrock.webrtcPort() == 0 ? bedrock.port() : bedrock.webrtcPort();
        this.signalingPort = config.port();
        this.dataFolder = geyser.getBootstrap().getConfigFolder().resolve("nethernet");
        this.pingResponder = new BedrockPingHandler(geyser);
    }

    public void start() {
        GeyserConfig.SignalingConfig.Mode mode = config.mode();
        boolean inbuilt = mode.builtin();
        boolean provider = mode.nxs();

        GeyserConfig.BedrockConfig listener = geyser.config().bedrock();
        InetAddress bindAddress = new InetSocketAddress(listener.address(), 0).getAddress();
        if (bindAddress != null && bindAddress.isLoopbackAddress()) {
            // ICE binds to this address too, and clients never offer loopback candidates, so no candidate pair can connect
            logger().warning("The Bedrock \"address\" " + listener.address() + " is a loopback address which only accepts connections from this machine. NetherNet " +
                "cannot work that way: every player will time out, including ones on this machine. " +
                "Set \"address\" in the \"bedrock\" section to 0.0.0.0, or to this machine's local network address.");
        }
        if (listener.transport().raknet() && webrtcPort == listener.raknetPort()) {
            logger().error("NetherNet will not start! With the \"both\" transport, RakNet and NetherNet each need their own UDP port, " +
                "but both are set to " + webrtcPort + ". Set \"webrtc-port\" in the \"bedrock\" section to a different available port.");
            return;
        }

        if (inbuilt && geyser.config() instanceof GeyserPluginConfig pluginConfig && signalingPort == pluginConfig.java().port()) {
            // e.g. clone-remote-port: the Java server owns that TCP port
            inbuilt = false;
            String reason = "Built-in signaling cannot use port " + signalingPort + " because the Java server already uses it. ";
            if (!provider) {
                provider = true;
                logger().warning(reason + "Bedrock players can still find this server through the external signaling service at "
                    + config.nxs().endpoint() + " instead.");
            } else {
                logger().warning(reason + "Only the external signaling service will be used.");
            }
        }

        try {
            // Picks this platform's WebRTC library out of the ones bundled for every platform
            LibDataChannelArchDetect.initialize();
        } catch (LinkageError e) {
            logger().error("NetherNet is not available on " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + "). " +
                "Bedrock players can still join over RakNet: set \"transport\" in the \"bedrock\" section of the config to \"raknet\".", e);
            return;
        }

        // Before either side starts, so their logs follow it from the first line
        applyLogLevel();

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger().error("NetherNet will not start! Could not create its data folder at " + dataFolder + ".", e);
            return;
        }

        geyser.eventBus().subscribe(this, SessionJoinEvent.class, this::onSessionJoin);
        geyser.eventBus().subscribe(this, SessionDisconnectEventImpl.class, event -> refreshProviderStatus());

        List<String> advertised = advertisedAddresses();
        // Players only reach the media port through the relays, so this host's own addresses would serve nobody
        if (advertised.isEmpty() && !geyser.config().advanced().bedrock().webrtcProxyProtocolRelays().isEmpty()) {
            logger().error("NetherNet will not start! \"webrtc-proxy-protocol-relays\" is set, so \"advertise-addresses\" "
                + "under \"bedrock.signaling\" has to name the relays players reach this server through.");
            return;
        }
        warnIfProxyAddressUnused(inbuilt);
        if (inbuilt) startInbuilt(advertised);
        if (provider) startProvider(advertised);
    }

    /** Only built-in signaling learns an address from a proxy, so the option needs it and one of its proxy lists. */
    private void warnIfProxyAddressUnused(boolean inbuilt) {
        GeyserConfig.AdvancedBedrockConfig advanced = geyser.config().advanced().bedrock();
        if (!advanced.preferSignalingProxyAddress()) {
            return;
        }
        if (!inbuilt) {
            logger().warning("\"prefer-signaling-proxy-address\" does nothing, as built-in signaling is not running.");
        } else if (advanced.haproxyProtocolWhitelistedIps().isEmpty() && advanced.signalingTrustedProxies().isEmpty()) {
            logger().warning("\"prefer-signaling-proxy-address\" does nothing, as neither \"haproxy-protocol-whitelisted-ips\" "
                + "nor \"signaling-trusted-proxies\" lists a proxy in front of built-in signaling.");
        }
    }

    private void startInbuilt(List<String> advertised) {
        GeyserConfig.SignalingConfig.BuiltinConfig builtin = config.builtin();
        GeyserConfig.AdvancedBedrockConfig advanced = geyser.config().advanced().bedrock();
        NetherNetHTTPServerSignaling.Builder signalingBuilder = new NetherNetHTTPServerSignaling.Builder();
        try {
            // Configured addresses already say what STUN would find, and network refuses both
            boolean stun = advertised.isEmpty() && config.stunDiscovery();
            if (!stun && System.getProperty("geyserStunServers") != null) {
                logger().warning("-DgeyserStunServers is ignored, as "
                    + (config.stunDiscovery() ? "\"advertise-addresses\" is set" : "\"stun-discovery\" is off"));
            }
            List<String> stunServers = stun ? stunServers() : List.of();
            if (advertised.isEmpty() && stunServers.isEmpty()) {
                advertised = boundAddress(geyser.config().bedrock());
            }
            signalingBuilder.setAdvertisedAddresses(advertised);
            try {
                signalingBuilder.setStunServers(stunServers);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid entry in -DgeyserStunServers: " + e.getMessage(), e);
            }
            builtinStun = !stunServers.isEmpty();
            String additionalHeaders = System.getenv(ADDITIONAL_HEADERS_ENV);
            if (additionalHeaders != null) {
                try {
                    Map<String, String> headers = responseHeaders(additionalHeaders);
                    signalingBuilder.setResponseHeaders(headers);
                    logger().info("Signaling response headers set from environment variable " + ADDITIONAL_HEADERS_ENV + ": "
                        + String.join(", ", headers.keySet()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid entry in " + ADDITIONAL_HEADERS_ENV + ": " + e.getMessage(), e);
                }
            }
        } catch (IllegalArgumentException e) {
            logger().error("Built-in signaling will not start! " + e.getMessage());
            return;
        }

        // Created on the first start and kept afterwards, as clients pin its public key
        OperatorIdentity identity;
        try {
            identity = OperatorIdentity.fromPemOrCreate(dataFolder.resolve("identity.pem").toFile(),
                    GeyserImpl.NAME + "-" + geyser.config().gameplay().serverName());
        } catch (Exception e) {
            logger().error("Built-in signaling will not start! Could not load or create this server's identity file, "
                    + dataFolder.resolve("identity.pem") + ". Only delete that file if it cannot be recovered: "
                    + "a new identity means every player has to confirm this server again.", e);
            return;
        }

        // Start up NetherNet
        try {
            signalingBuilder
                    .setIdentity(identity)
                    .setForwardedForProxies(TrustedProxies.parse(geyser.config().advanced().bedrock().signalingTrustedProxies()))
                    // The same HAProxy proxies RakNet takes, applied to the TCP listener
                    .setProxyProtocolProxies(TrustedProxies.parse(geyser.config().advanced().bedrock().haproxyProtocolWhitelistedIps()))
                    // Behind a proxy the peer is the proxy, signing its own assertion. The same
                    // setting already accepts the login chain it forwards
                    .setTokenTrust(geyser.config().advanced().bedrock().validateBedrockLogin()
                            ? TokenTrust.MINECRAFT_AUTH : TokenTrust.ANY)
                    .setMotdProvider((host, remoteAddress, client) -> {
                        // host is the HTTP Host header the client sent, which may be absent
                        BedrockPong pong = pingResponder.onQuery(GUID, remoteAddress, host);

                        return new PongData.Builder()
                                .setServerName(pong.motd())
                                .setLevelName(pong.subMotd())
                                .setProtocol(pong.protocolVersion())
                                .setVersion(pong.version())
                                .setPlayerCount(pong.playerCount())
                                .setMaxPlayerCount(pong.maximumPlayerCount())
                                // As BDS sets it from online-mode, telling clients signed in to no Xbox account whether they may join
                                .setSelfSignedAuth(!geyser.config().advanced().bedrock().validateBedrockLogin())
                                .build();
                    });

            GeyserConfig.SignalingConfig.BuiltinConfig.HttpsConfig https = builtin.https();
            if (!https.certificate().isBlank()) {
                Path certificate = geyser.configDirectory().resolve(https.certificate());
                String password = SecretValue.resolve(https.password(), geyser.configDirectory());
                if (https.privateKey().isBlank()) {
                    signalingBuilder.setHttpsKeystore(certificate.toFile(), password);
                } else {
                    signalingBuilder.setHttpsPem(certificate.toFile(),
                            geyser.configDirectory().resolve(https.privateKey()).toFile(),
                            password.isBlank() ? null : password);
                }
            }

            BedrockListener listener = geyser.config().bedrock();
            // The channel binds HTTP signaling over TCP to the signaling port, and by default pins ICE to its UDP side.
            // When the WebRTC port is another port, ICE goes there instead.
            boolean separateIcePort = webrtcPort != signalingPort;
            this.signaling = signalingBuilder.setIceOnLocalPort(!separateIcePort).build();

            this.inbuiltEventLoopGroup = TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory()
                .apply(0, new DefaultThreadFactory("NetherNet Built-in Signaling", true));
            this.inbuiltInitialiser = new NetherNetChannelInitialiser(geyser);

            ServerBootstrap b = new ServerBootstrap();
            b.group(inbuiltEventLoopGroup)
                    .channelFactory(NetherNetChannelFactory.server(signaling))
                    .option(NetherChannelOption.NETHER_SERVER_PROXY_PROTOCOL,
                            TrustedProxies.parse(geyser.config().advanced().bedrock().webrtcProxyProtocolRelays()))
                    .option(NetherChannelOption.NETHER_SERVER_PREFER_PROXIED_ADDRESS, advanced.preferSignalingProxyAddress())
                    .childHandler(inbuiltInitialiser);
            if (separateIcePort) {
                b.option(NetherChannelOption.NETHER_SERVER_ICE_ADDRESS, new InetSocketAddress(listener.address(), webrtcPort));
            }

            this.inbuiltChannel = b.bind(new InetSocketAddress(listener.address(), signalingPort)).sync().channel();

            // With a certificate configured, plain HTTP is rejected
            String scheme = https.certificate().isBlank() ? "http://" : "https://";
            logger().info("Built-in signaling started on " + scheme + listener.address() + ":" + signalingPort
                + (separateIcePort ? ", with NetherNet on UDP port " + webrtcPort : ""));
        } catch (Throwable e) {
            // Throwable: the WebRTC natives are not available on every platform
            closeInbuiltResources();
            logger().error("Built-in signaling could not start", e);
        }
    }

    /**
     * The addresses both signaling modes announce: those in {@code -DgeyserAdvertiseAddresses}, comma separated, else
     * those in the config. An address this machine does not hold is announced as the public side of a NAT forwarding
     * the media port here, which is how a container without a public address of its own is reached.
     */
    private List<String> advertisedAddresses() {
        String property = System.getProperty("geyserAdvertiseAddresses", "");
        List<String> fromProperty = addresses(Arrays.stream(property.split(",")));
        if (!fromProperty.isEmpty()) {
            logger().info("Advertised addresses set from system property: " + String.join(", ", fromProperty));
            return fromProperty;
        }
        return addresses(config.advertiseAddresses().stream());
    }

    /**
     * Sets the log level of libdatachannel and NetherNet's own logs alike: DEBUG with "nethernet-debug", else WARN for
     * libdatachannel alone, which keeps its own logging out of the way. {@code -DgeyserNetherNetLogLevel} overrides
     * both with OFF, ERROR, WARN, INFO, DEBUG or TRACE.
     */
    private void applyLogLevel() {
        String property = System.getProperty("geyserNetherNetLogLevel");
        if (property != null) {
            if (NetherNetLogging.setLogLevel(property)) {
                logger().info("NetherNet log level set from system property: " + property.trim());
                return;
            }
            logger().warning("Invalid -DgeyserNetherNetLogLevel \"" + property + "\", expected OFF, ERROR, WARN, INFO, DEBUG or TRACE");
        }
        boolean debug = geyser.config().advanced().bedrock().nethernetDebug();
        if (debug) {
            NetherNetLogging.setLogLevel("DEBUG");
        } else {
            if (debugLogging) {
                NetherNetLogging.setLogLevel("INFO");
            }
            NetherNetLogging.setNativeLogLevel("WARN");
        }
        debugLogging = debug;
    }

    /**
     * The STUN servers asked for this machine's public address: those in {@code -DgeyserStunServers}, comma separated,
     * else {@link #DEFAULT_STUN_SERVERS}. Set it empty to ask none.
     */
    private List<String> stunServers() {
        String property = System.getProperty("geyserStunServers");
        if (property == null) {
            return DEFAULT_STUN_SERVERS;
        }
        List<String> servers = Arrays.stream(property.split(",")).map(String::trim)
            .filter(server -> !server.isEmpty())
            .toList();
        logger().info("STUN servers set from system property: "
            + (servers.isEmpty() ? "none" : String.join(", ", servers)));
        return servers;
    }

    /**
     * The bound address unless it is a wildcard, in which case everything ICE gathers. Only used when neither
     * advertised addresses nor STUN servers say better.
     */
    private static List<String> boundAddress(GeyserConfig.BedrockConfig bedrock) {
        byte[] bound = NetUtil.createByteArrayFromIpAddressString(bedrock.address());
        try {
            if (bound == null || InetAddress.getByAddress(bound).isAnyLocalAddress()) {
                return List.of();
            }
        } catch (UnknownHostException e) {
            return List.of();
        }
        return List.of(bedrock.address());
    }

    private static List<String> addresses(Stream<String> entries) {
        return entries.map(String::trim).filter(address -> !address.isEmpty()).distinct().toList();
    }

    /** See {@link #ADDITIONAL_HEADERS_ENV}. Network refuses names and values no header can have. */
    static Map<String, String> responseHeaders(String entries) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String entry : entries.split(",")) {
            if (entry.isBlank()) {
                continue;
            }
            int colon = entry.indexOf(':');
            if (colon < 0) {
                throw new IllegalArgumentException("\"" + entry.trim() + "\" is not \"Name: value\"");
            }
            headers.put(entry.substring(0, colon).trim(), entry.substring(colon + 1).trim());
        }
        return headers;
    }

    private void startProvider(List<String> advertised) {
        providerStart = CompletableFuture.runAsync(() -> {
            ProviderStateStore store = null;
            ProviderTransport initializingTransport = null;
            try {
                BedrockListener listener = geyser.config().bedrock();
                ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(providerSettings(config, advertised, geyser.config().advanced().nxs(),
                    geyser.config().advanced().bedrock().webrtcProxyProtocolRelays()),
                    dataFolder, listener.address(), webrtcPort, collectServerStatus().maxPlayerCount());
                URI origin = runtime.origin();
                var statePath = runtime.stateDirectory();
                ProviderTransport transport;
                if (stopping) return;
                // Lock the durable instance before identity initialization or opening its endpoint.
                store = new ProviderStateStore(statePath);
                eventLoopGroup = TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory()
                    .apply(0, new DefaultThreadFactory("NetherNet Provider", true));
                providerInitialiser = new NetherNetChannelInitialiser(geyser, gameOutcomes);
                ServerBootstrap bootstrap = new ServerBootstrap().group(eventLoopGroup).childHandler(providerInitialiser);
                NativeProviderHostFactory.Host host = NativeProviderHostFactory.open(bootstrap, runtime)
                        .toCompletableFuture().get(30, TimeUnit.SECONDS);
                netherNetChannel = host.channel();
                transport = host.transport();
                host.warnings().forEach(message -> logger().warning(message));
                if (stopping) {
                    providerStopped = transport.close();
                    closeNetworkResources();
                    return;
                }
                initializingTransport = transport;
                ProviderClient client = new ProviderClient(runtime.clientConfiguration(), store, transport,
                        () -> providerStatusSupplier.get(), () -> health(runtime.capacity()), this::logDiagnostic);
                store = null; // ProviderClient now owns its lifetime.
                initializingTransport = null;
                synchronized (providerLifecycle) {
                    if (stopping) {
                        providerStopped = client.stop();
                        return;
                    }
                    try {
                        providerShutdown = new ProviderShutdown(client::stop, this::closeNetworkResources, message -> logger().warning(message));
                    } catch (RuntimeException failure) {
                        client.close();
                        throw failure;
                    }
                    providerClient = client;
                }
                client.start().whenComplete((registration, failure) -> {
                    if (failure != null) {
                        logProviderFailure("Could not register with the external signaling service", failure);
                        stopProvider();
                        return;
                    }
                    providerInstanceId = registration.instanceId();
                    logger().info(registrationMessage(registration));
                    // Says where logins are vouched for, without putting provider credentials in the log
                    logger().debug("Player logins over NetherNet are verified by the external signaling service at " + origin.getHost() + ".");
                    // The provider may offer the operator a link, such as one to claim this server
                    client.operatorAction().thenAccept(this::announceOperatorAction);
                });
            } catch (Throwable e) {
                // Throwable: the WebRTC natives are not available on every platform
                if (initializingTransport != null) providerStopped = initializingTransport.close();
                if (e instanceof IOException && e.getMessage() != null) {
                    // Says what in the settings is wrong
                    logger().error("Could not start the external signaling service: " + e.getMessage());
                } else {
                    logProviderFailure("Could not start the external signaling service", e);
                }
                closeNetworkResources();
            } finally {
                if (store != null) try {
                    store.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    /**
     * What the provider runtime is configured with. Geyser reports game outcomes itself.
     */
    public static ProviderRuntimeConfiguration.Settings providerSettings(GeyserConfig.SignalingConfig signaling,
                                                                         List<String> advertised,
                                                                         GeyserConfig.AdvancedNxsConfig advanced,
                                                                         List<String> webrtcRelays) throws IOException {
        GeyserConfig.SignalingConfig.NxsConfig nxs = signaling.nxs();
        ProviderClient.Location location;
        try {
            location = ProviderClient.Location.of(advanced.location());
        } catch (IllegalArgumentException invalid) {
            throw new IOException("advanced.nxs.location needs a two-letter country code, a latitude and longitude pair, or both; city is optional");
        }
        return ProviderRuntimeConfiguration.Settings.builder(nxs.endpoint())
            .token(nxs.token())
            .advertiseAddresses(advertised)
            .assistedJoins(advanced.assistedJoins())
            .connectionChecks(advanced.connectionChecks())
            .stunDiscovery(signaling.stunDiscovery())
            .deregisterOnShutdown(advanced.deregisterOnShutdown())
            .location(location)
            .bedrockProtocols(bedrockProtocols())
            .gameOutcomes(true)
            .proxyProtocolRelays(TrustedProxies.parse(webrtcRelays))
            .build();
    }

    /**
     * Every protocol a login is accepted with, one per supported codec.
     */
    static ProviderClient.BedrockProtocols bedrockProtocols() {
        List<Integer> accepted = GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS.intStream().distinct().sorted().boxed().toList();
        return new ProviderClient.BedrockProtocols(accepted, GameProtocol.DEFAULT_BEDROCK_VERSION);
    }

    private ServerStatus collectServerStatus() {
        BedrockPong pong = pingResponder.onQuery(GUID, new InetSocketAddress("127.0.0.1", 0), null);
        return GeyserStatusCollector.snapshot(pong, 0);
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

    /**
     * @return what each running signaling mode currently sends to joining players
     */
    public Advertised advertised() {
        Channel channel = inbuiltChannel;
        NetherNetHTTPServerSignaling builtin = channel != null && channel.isActive() ? signaling : null;
        ProviderClient client = providerClient;
        return new Advertised(
            builtin == null ? null : new Advertised.Builtin(builtin.advertisedAddresses().stream()
                // An entry without a port is on the WebRTC port
                .map(address -> address.getPort() == 0 ? new InetSocketAddress(address.getAddress(), webrtcPort) : address)
                .toList(), builtinStun, builtin.discoveredEndpoints()),
            client == null ? null : new Advertised.Nxs(client.publishedEndpoints().orElse(null), providerInstanceId,
                client.clockOffsetMillis()));
    }

    /**
     * What each signaling mode sends to joining players.
     *
     * @param builtin null when built-in signaling is not running
     * @param nxs null when the external signaling service is not running
     */
    public record Advertised(@Nullable Builtin builtin, @Nullable Nxs nxs) {
        /**
         * @param configured the configured addresses. When empty, this machine's own addresses and {@code discovered}
         * @param stun whether STUN discovery is on
         * @param discovered the public addresses STUN discovery currently finds
         */
        public record Builtin(List<InetSocketAddress> configured, boolean stun, List<InetSocketAddress> discovered) {
        }

        /**
         * @param published what the service last confirmed, or null while it has confirmed nothing
         * @param instanceId the id the service knows this server by, or null until it has registered
         * @param clockOffsetMillis the service's clock minus this machine's, positive when this machine is behind
         */
        public record Nxs(ProviderClient.@Nullable PublishedEndpoints published, @Nullable String instanceId,
                          long clockOffsetMillis) {
        }
    }

    private ProviderClient.Health health(int capacity) {
        return new ProviderClient.Health(true, capacity, new ProviderClient.Software("Geyser", GeyserImpl.VERSION),
            new ProviderClient.ConnectedPlayers(geyser.getSessionManager().size(), System.currentTimeMillis()));
    }

    private boolean reportGameOutcome(String ticketId, ProviderClient.GameOutcome outcome, String reason) {
        ProviderClient client = providerClient;
        return client != null && client.reportGameOutcome(ticketId, outcome, reason);
    }

    private void logDiagnostic(ProviderDiagnostic diagnostic) {
        String message = "NXS: " + diagnostic.message();
        switch (diagnostic.level()) {
            case DEBUG -> logger().debug(message);
            case INFO -> logger().info(message);
            case WARN -> logger().warning(message);
        }
    }

    private static String registrationMessage(ProviderClient.Registration registration) {
        String instanceId = registration.instanceId();
        return registration.publicAddress() != null
            ? "Registered with the external signaling service. Players can join at " + registration.publicAddress() + " (instance " + instanceId + ")."
            : "Registered with the external signaling service (instance " + instanceId + "). "
                + "The addresses players join with are managed by the service.";
    }

    private void announceOperatorAction(Optional<ProviderClient.OperatorAction> action) {
        try {
            String message = storeOperatorAction(action);
            if (message != null) {
                logger().info(message);
            }
        } catch (IOException e) {
            logger().warning("Could not keep the signaling service's link in " + dataFolder.resolve(OPERATOR_LINK_FILE) + ": " + e.getMessage());
        }
    }

    /**
     * Keeps the provider's link for the operator, such as one to claim this server, in the data folder. See
     * {@link #storeOperatorAction(Path, Optional)}.
     */
    public @Nullable String storeOperatorAction(Optional<ProviderClient.OperatorAction> action) throws IOException {
        return storeOperatorAction(dataFolder.resolve(OPERATOR_LINK_FILE), action);
    }

    /**
     * Writes the provider's link to a file only this user can read, as it grants access to this server and a console
     * is often logged. With no link the file is removed.
     *
     * @return the provider's text and where its link is, or null when there is none
     */
    public static @Nullable String storeOperatorAction(Path file, Optional<ProviderClient.OperatorAction> action) throws IOException {
        Files.deleteIfExists(file);
        if (action.isEmpty()) {
            return null;
        }
        ProviderClient.OperatorAction value = action.get();
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        }
        Files.writeString(file, value.text() + "\n" + value.url() + "\nExpires " + Instant.ofEpochMilli(value.expiresAt()) + "\n");
        return value.text() + " The link is in " + file + ".";
    }

    public @Nullable Channel providerChannel() {
        return netherNetChannel;
    }

    public GameOutcomeReporter gameOutcomes() {
        return gameOutcomes;
    }

    /**
     * @return why a request to the external signaling service failed: the service's own explanation when it gave one,
     * else the exception's type and message
     */
    public static String providerFailure(Throwable failure) {
        failure = unwrapped(failure);
        if (failure instanceof ProviderClient.ProviderException) {
            return failure.getMessage();
        }
        String type = failure.getClass().getSimpleName();
        return failure.getMessage() == null ? type : type + ": " + failure.getMessage();
    }

    /**
     * Logs why the external signaling service failed: the service's own explanation when it gave one, else the
     * exception, whose causes say what is in the way, such as a firewall or a proxy.
     */
    private void logProviderFailure(String what, Throwable failure) {
        Throwable cause = unwrapped(failure);
        if (cause instanceof ProviderClient.ProviderException) {
            logger().error(what + ": " + cause.getMessage());
        } else {
            logger().error(what, cause);
        }
    }

    private static Throwable unwrapped(Throwable failure) {
        while (failure.getCause() != null && (failure instanceof CompletionException || failure instanceof ExecutionException))
            failure = failure.getCause();
        return failure;
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

    /**
     * Waits for {@link #shutdown()} to stop the provider, which holds its state directory and UDP port until then.
     * Another instance cannot start the provider while they are held.
     */
    public void awaitShutdown() {
        try {
            providerStart.thenCompose(ignored -> providerStopped).get(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger().warning("The external signaling service has not stopped after " + SHUTDOWN_WAIT_SECONDS
                + " seconds, so it may not start again.");
        } catch (ExecutionException ignored) {
            // A close that failed is still over
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopProvider() {
        synchronized (providerLifecycle) {
            if (providerShutdown != null) {
                providerShutdown.close();
                providerShutdown = null;
                // The same stop close() began
                providerStopped = providerClient.stop();
            } else closeNetworkResources();
            providerClient = null;
            providerInstanceId = null;
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
