/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.geysermc.geyser.network.netty.GeyserServer;

import java.net.InetSocketAddress;

import static org.cloudburstmc.netty.channel.raknet.RakConstants.ID_CONNECTION_BANNED;
import static org.cloudburstmc.netty.channel.raknet.RakConstants.ID_OPEN_CONNECTION_REQUEST_1;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class RakConnectionRequestHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "rak-connection-request-handler";

    private final GeyserServer server;

    @Override
    public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg) {
        if (!(msg instanceof DatagramPacket packet)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf buf = packet.content();
        if (!buf.isReadable()) {
            return; // No packet ID
        }

        boolean readableMagic = true;
        int startIndex = buf.readerIndex();
        try {
            int packetId = buf.readUnsignedByte();
            if (packetId == ID_OPEN_CONNECTION_REQUEST_1) {
                ByteBuf magicBuf = ctx.channel().config().getOption(RakChannelOption.RAK_UNCONNECTED_MAGIC);
                if (!buf.isReadable(magicBuf.readableBytes()) || !ByteBufUtil.equals(buf.readSlice(magicBuf.readableBytes()), magicBuf)) {
                    readableMagic = false;
                }
            } else {
                readableMagic = false;
            }
        } finally {
            buf.readerIndex(startIndex);
        }

        if (!readableMagic) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf magicBuf = ctx.channel().config().getOption(RakChannelOption.RAK_UNCONNECTED_MAGIC);
        long guid = ctx.channel().config().getOption(RakChannelOption.RAK_GUID);

        if (!this.server.onConnectionRequest(packet.sender())) {
            this.sendConnectionBanned(ctx, packet.sender(), magicBuf, guid);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void sendConnectionBanned(ChannelHandlerContext ctx, InetSocketAddress recipient, ByteBuf magicBuf, long guid) {
        ByteBuf buffer = ctx.alloc().ioBuffer(25, 25);
        buffer.writeByte(ID_CONNECTION_BANNED);
        buffer.writeBytes(magicBuf, magicBuf.readerIndex(), magicBuf.readableBytes());
        buffer.writeLong(guid);
        ctx.writeAndFlush(new DatagramPacket(buffer, recipient));
    }
}
