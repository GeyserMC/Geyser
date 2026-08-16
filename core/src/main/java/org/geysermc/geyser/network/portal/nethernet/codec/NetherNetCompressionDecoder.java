package org.geysermc.geyser.network.portal.nethernet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.BatchCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;

import java.util.List;

public final class NetherNetCompressionDecoder extends MessageToMessageDecoder<ByteBuf> {
    public static final String NAME = "nethernet-compression-decoder";

    private final CompressionStrategy strategy;
    private final boolean prefixed;

    public NetherNetCompressionDecoder(CompressionStrategy strategy, boolean prefixed) {
        this.strategy = strategy;
        this.prefixed = prefixed;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!this.prefixed) {
            BatchCompression compression = this.strategy.getDefaultCompression();
            out.add(compression.decode(ctx, msg));
            return;
        }

        int header = msg.readUnsignedByte();
        BatchCompression compression = switch (header) {
            case 0x00 -> this.strategy.getCompression(PacketCompressionAlgorithm.ZLIB);
            case 0x01 -> this.strategy.getCompression(PacketCompressionAlgorithm.SNAPPY);
            case 0xff -> this.strategy.getCompression(PacketCompressionAlgorithm.NONE);
            default -> throw new IllegalStateException("Unknown compression algorithm header: " + header);
        };

        out.add(compression.decode(ctx, msg));
    }
}
