package org.geysermc.geyser.network.portal.nethernet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.BatchCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;

public final class NetherNetCompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    public static final String NAME = "nethernet-compression-encoder";

    private final CompressionStrategy strategy;
    private final boolean prefixed;
    private final int threshold;

    public NetherNetCompressionEncoder(CompressionStrategy strategy, boolean prefixed, int threshold) {
        this.strategy = strategy;
        this.prefixed = prefixed;
        this.threshold = threshold;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        BatchCompression compression = msg.readableBytes() > this.threshold
            ? this.strategy.getDefaultCompression()
            : this.strategy.getCompression(PacketCompressionAlgorithm.NONE);

        ByteBuf compressed = compression.encode(ctx, msg);
        try {
            if (this.prefixed) {
                out.writeByte(switch ((PacketCompressionAlgorithm) compression.getAlgorithm()) {
                    case ZLIB -> 0x00;
                    case SNAPPY -> 0x01;
                    default -> (byte) 0xff;
                });
            }
            out.writeBytes(compressed);
        } finally {
            compressed.release();
        }
    }
}
