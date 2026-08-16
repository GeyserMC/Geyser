package org.geysermc.geyser.network.portal.nethernet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.common.util.VarInts;

public final class NetherNetPacketEncoder extends MessageToByteEncoder<BedrockPacketWrapper> {
    public static final String NAME = "nethernet-encoder";

    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockPacketWrapper wrapper, ByteBuf out) {
        VarInts.writeUnsignedInt(out, wrapper.getPacketBuffer().readableBytes());
        out.writeBytes(wrapper.getPacketBuffer());
    }
}
