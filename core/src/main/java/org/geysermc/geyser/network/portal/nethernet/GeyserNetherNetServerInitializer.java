package org.geysermc.geyser.network.portal.nethernet;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.InvalidPacketHandler;
import org.geysermc.geyser.network.UpstreamPacketHandler;
import org.geysermc.geyser.session.GeyserSession;
import io.netty.channel.DefaultEventLoopGroup;

public final class GeyserNetherNetServerInitializer extends NetherNetBedrockChannelInitializer<BedrockServerSession> implements AutoCloseable {
    private static final boolean PROXY_BRIDGE_DEBUG = Boolean.parseBoolean(System.getProperty("Geyser.ProxyBridgeDebug", "false"));

    private final GeyserImpl geyser;
    @Getter
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("GeyserNetherNetPlayer", true));

    public GeyserNetherNetServerInitializer(GeyserImpl geyser) {
        this.geyser = geyser;
    }

    @Override
    protected BedrockServerSession createSession0(BedrockPeer peer, int subClientId) {
        return new BedrockServerSession(peer, subClientId);
    }

    @Override
    protected void initSession(@NonNull BedrockServerSession bedrockServerSession) {
        try {
            this.geyser.getLogger().info("[proxy-bridge] NetherNet peer connected");
            if (PROXY_BRIDGE_DEBUG) {
                this.geyser.getLogger().info("[proxy-bridge] nethernet initSession remote=" + bedrockServerSession.getSocketAddress());
            }

            bedrockServerSession.setLogging(this.geyser.config().debugMode());
            GeyserSession session = new GeyserSession(this.geyser, bedrockServerSession, this.eventLoopGroup.next());
            session.setProxyBridgeIngress(true);
            this.geyser.getLogger().info("[proxy-bridge] NetherNet Bedrock session initialized remote="
                + bedrockServerSession.getSocketAddress());

            if (!bedrockServerSession.isSubClient()) {
                Channel channel = bedrockServerSession.getPeer().getChannel();
                channel.pipeline().addAfter(BedrockPacketCodec.NAME, InvalidPacketHandler.NAME, new InvalidPacketHandler(session));
                if (this.geyser.config().advanced().bedrock().portalBridge().debugLogging()) {
                    channel.closeFuture().addListener(future -> this.geyser.getLogger().info(
                        "[proxy-bridge] NetherNet Bedrock transport closed remote=" + bedrockServerSession.getSocketAddress()));
                }
            }

            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(this.geyser, session));
        } catch (Throwable throwable) {
            this.geyser.getLogger().error("[proxy-bridge] NetherNet/Geyser session initialization failed: "
                + throwable.getClass().getSimpleName(), throwable);
            bedrockServerSession.disconnect(throwable.getMessage());
        }
    }

    @Override
    public void close() {
        this.eventLoopGroup.shutdownGracefully();
    }
}
