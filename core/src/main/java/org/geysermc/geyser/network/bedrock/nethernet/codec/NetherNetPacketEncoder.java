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

package org.geysermc.geyser.network.bedrock.nethernet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchEncoder;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.util.List;

/**
 * Puts every packet in its own batch, and so its own NetherNet message, instead of batching everything written
 * between flushes like {@link BedrockBatchEncoder} does.
 */
@ChannelHandler.Sharable
public class NetherNetPacketEncoder extends MessageToMessageEncoder<BedrockPacketWrapper> {
    public static final String NAME = "nethernet-encoder";
    public static final NetherNetPacketEncoder INSTANCE = new NetherNetPacketEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, BedrockPacketWrapper packet, List<Object> out) {
        ByteBuf message = packet.getPacketBuffer();
        if (message == null) {
            throw new IllegalArgumentException("BedrockPacket is not encoded");
        }

        CompositeByteBuf buf = ctx.alloc().compositeDirectBuffer(2);
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        try {
            ByteBuf header = ctx.alloc().ioBuffer(5);
            VarInts.writeUnsignedInt(header, message.readableBytes());
            buf.addComponent(true, header);
            buf.addComponent(true, message.retain());
            // MessageToMessageEncoder releases the packet once we return
            batch.addPacket(packet.retain());

            batch.setUncompressed(buf.retain());
            out.add(batch.retain());
        } finally {
            buf.release();
            batch.release();
        }
    }
}
