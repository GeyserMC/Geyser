package org.geysermc.geyser.network.portal.nethernet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kastle.netty.channel.nethernet.NetherNetChannelFactory;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetServerSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetSignaling;
import dev.kastle.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling;
import dev.kastle.webrtc.PeerConnectionFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.configuration.PortalBridgeConfig;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PortalNetherNetServer implements AutoCloseable {
    private final GeyserImpl geyser;
    private final PortalBridgeConfig config;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("GeyserNetherNetBoss", true));
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("GeyserNetherNetChild", true));
    private final GeyserNetherNetServerInitializer initializer;
    private final PeerConnectionFactory peerConnectionFactory;
    private final NetherNetServerSignaling signaling;
    private final String configuredNetworkId;
    private Channel channel;

    public PortalNetherNetServer(GeyserImpl geyser, PortalBridgeConfig config, String configuredNetworkId) {
        this.geyser = geyser;
        this.config = config;
        this.configuredNetworkId = configuredNetworkId == null ? "" : configuredNetworkId;
        this.initializer = new GeyserNetherNetServerInitializer(geyser);
        this.peerConnectionFactory = new PeerConnectionFactory();
        NetherNetXboxRpcSignaling rawSignaling = createSignaling(geyser, config, this.configuredNetworkId);
        this.signaling = config.debugLogging()
            ? new TracingServerSignaling(geyser, rawSignaling)
            : rawSignaling;
    }

    private static NetherNetXboxRpcSignaling createSignaling(GeyserImpl geyser, PortalBridgeConfig config, String configuredNetworkId) {
        String authHeader = resolveAuthHeader(geyser, config);
        if (!configuredNetworkId.isBlank()) {
            return new NetherNetXboxRpcSignaling(configuredNetworkId, authHeader);
        }
        return new NetherNetXboxRpcSignaling(authHeader);
    }

    private static String resolveAuthHeader(GeyserImpl geyser, PortalBridgeConfig config) {
        if (!config.xboxAuthHeader().isBlank()) {
            return config.xboxAuthHeader();
        }
        if (config.xboxAuthHeaderFile().isBlank()) {
            return "";
        }

        try {
            String json = Files.readString(Path.of(config.xboxAuthHeaderFile()));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject minecraftSession = root.getAsJsonObject("minecraftSession");
            if (minecraftSession != null && minecraftSession.has("authorizationHeader")) {
                return minecraftSession.get("authorizationHeader").getAsString();
            }
            geyser.getLogger().warning("[proxy-bridge] minecraftSession.authorizationHeader was not found in " + config.xboxAuthHeaderFile());
        } catch (Exception exception) {
            geyser.getLogger().error("[proxy-bridge] Failed to read Xbox auth header file: " + config.xboxAuthHeaderFile(), exception);
        }
        return "";
    }

    /**
     * Returns a non-secret fingerprint of the effective Xbox auth header. This is
     * intentionally the only value exposed to the reload watcher.
     */
    public static String authHeaderFingerprint(GeyserImpl geyser, PortalBridgeConfig config) {
        String authHeader = resolveAuthHeader(geyser, config);
        if (authHeader.isBlank()) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(authHeader.getBytes(StandardCharsets.UTF_8));
            StringBuilder fingerprint = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                fingerprint.append(String.format("%02x", value));
            }
            return fingerprint.toString();
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 is required by every Java runtime, but do not expose the
            // header if a non-conforming runtime is used.
            return Integer.toHexString(authHeader.hashCode());
        }
    }

    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(this.bossGroup, this.workerGroup)
            .channelFactory(NetherNetChannelFactory.server(this.peerConnectionFactory, this.signaling))
            .childOption(ChannelOption.TCP_NODELAY, false)
            .childHandler(this.initializer);

        this.channel = bootstrap.bind(new InetSocketAddress(0)).syncUninterruptibly().channel();
        this.geyser.getLogger().info("[proxy-bridge] NetherNet ingress started with network ID " + this.signaling.getLocalNetworkId());
        if (config.debugLogging()) {
            this.geyser.getLogger().info("[proxy-bridge] NetherNet control channel bound through Xbox signaling and local server bootstrap.");
        }
    }

    @Override
    public void close() {
        if (this.channel != null) {
            this.channel.close().syncUninterruptibly();
            this.channel = null;
        }
        this.signaling.close();
        this.initializer.close();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();
        try {
            this.peerConnectionFactory.dispose();
        } catch (NullPointerException ignored) {
            // The native handle was never fully initialized.
        }
    }

    public String networkId() {
        return this.signaling.getLocalNetworkId();
    }

    /**
     * Adds stage-only diagnostics around the library signaling callbacks. Signal
     * bodies are deliberately never logged because they contain SDP/candidates.
     */
    private static final class TracingServerSignaling implements NetherNetServerSignaling {
        private final GeyserImpl geyser;
        private final NetherNetServerSignaling delegate;

        private TracingServerSignaling(GeyserImpl geyser, NetherNetServerSignaling delegate) {
            this.geyser = geyser;
            this.delegate = delegate;
        }

        @Override
        public void bind(java.net.SocketAddress address) throws java.net.ConnectException {
            delegate.bind(address);
            geyser.getLogger().info("[proxy-bridge] NetherNet signaling websocket connected.");
        }

        @Override
        public void setNewConnectionHandler(NewConnectionHandler handler) {
            delegate.setNewConnectionHandler((connectionId, peerId, offer) -> {
                geyser.getLogger().info("[proxy-bridge] NetherNet offer received (peer=" + peerId
                    + ", offerBytes=" + (offer == null ? 0 : offer.length()) + ").");
                handler.onConnect(connectionId, peerId, offer);
            });
        }

        @Override
        public void setAdvertisementData(PongData data) {
            delegate.setAdvertisementData(data);
        }

        @Override
        public java.util.List<NetherNetSignaling.IceServerInfo> getIceServers() {
            return delegate.getIceServers();
        }

        @Override
        public void sendSignal(String peerId, String signal) {
            geyser.getLogger().info("[proxy-bridge] NetherNet signal sent (peer=" + peerId
                + ", signalBytes=" + (signal == null ? 0 : signal.length()) + ").");
            delegate.sendSignal(peerId, signal);
        }

        @Override
        public void setSignalHandler(long connectionId, NetherNetSignaling.SignalHandler handler) {
            delegate.setSignalHandler(connectionId, signal -> {
                geyser.getLogger().info("[proxy-bridge] NetherNet signal received (connection="
                    + connectionId + ", signalBytes=" + (signal == null ? 0 : signal.length()) + ").");
                handler.onSignal(signal);
            });
        }

        @Override
        public void removeSignalHandler(long connectionId) {
            delegate.removeSignalHandler(connectionId);
        }

        @Override
        public String getLocalNetworkId() {
            return delegate.getLocalNetworkId();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
