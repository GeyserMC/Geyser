package org.geysermc.geyser.network.portal.nethernet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;
import org.geysermc.geyser.network.GeyserBedrockPeer;
import org.geysermc.geyser.network.portal.nethernet.codec.NetherNetCompressionDecoder;
import org.geysermc.geyser.network.portal.nethernet.codec.NetherNetCompressionEncoder;
import org.geysermc.geyser.network.portal.nethernet.codec.NetherNetPacketDecoder;
import org.geysermc.geyser.network.portal.nethernet.codec.NetherNetPacketEncoder;

import javax.crypto.SecretKey;
import java.util.Objects;

public final class GeyserNetherNetBedrockPeer extends GeyserBedrockPeer {
    public GeyserNetherNetBedrockPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        // NetherNet already secures the WebRTC transport.
    }

    @Override
    public void setCompression(PacketCompressionAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        this.setCompression(NetherNetBedrockChannelInitializer.getCompression());
    }

    @Override
    public void setCompression(CompressionStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");

        boolean prefixed = this.getCodec().getProtocolVersion() >= 649;
        ChannelPipeline pipeline = this.getChannel().pipeline();

        if (pipeline.get(NetherNetCompressionDecoder.NAME) == null) {
            pipeline.addBefore(NetherNetPacketDecoder.NAME, NetherNetCompressionDecoder.NAME,
                new NetherNetCompressionDecoder(strategy, prefixed));
        }
        if (pipeline.get(NetherNetCompressionEncoder.NAME) == null) {
            pipeline.addBefore(NetherNetPacketEncoder.NAME, NetherNetCompressionEncoder.NAME,
                new NetherNetCompressionEncoder(strategy, prefixed, 1));
        }
    }
}
