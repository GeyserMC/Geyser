package org.geysermc.geyser.network.portal.nethernet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.util.List;

public final class NetherNetPacketDecoder extends ByteToMessageDecoder {
    public static final String NAME = "nethernet-decoder";

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        in.markReaderIndex();
        final int length;
        try {
            length = VarInts.readUnsignedInt(in);
        } catch (Exception exception) {
            in.resetReaderIndex();
            return;
        }

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(length));
    }
}
