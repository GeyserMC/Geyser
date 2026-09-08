/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.github.sendablemetatype.netty.channel.raknet.packet.RakMessage;

import java.nio.charset.StandardCharsets;

public final class BedrockEncryptionControl {
    public static final String HANDLER_NAME = "bedrock-encryption-control";
    public static final String DISABLE_ENCRYPTION_REQUEST = "GEYSER_DISABLE_BEDROCK_ENCRYPTION_V1";

    private static final AttributeKey<Boolean> DISABLE_ENCRYPTION =
            AttributeKey.valueOf("geyser.disable_bedrock_encryption");
    private static final byte[] DISABLE_ENCRYPTION_REQUEST_BYTES =
            DISABLE_ENCRYPTION_REQUEST.getBytes(StandardCharsets.US_ASCII);

    private BedrockEncryptionControl() {
    }

    public static ChannelFuture sendDisableEncryptionRequest(Channel channel) {
        ByteBuf marker = channel.alloc().buffer(DISABLE_ENCRYPTION_REQUEST_BYTES.length);
        marker.writeBytes(DISABLE_ENCRYPTION_REQUEST_BYTES);
        return channel.writeAndFlush(marker);
    }

    /**
     * Directly disables Bedrock encryption for a channel. Used by the
     * Nethernet initializer where we control the pipeline setup and don't
     * need the in-band marker mechanism.
     */
    public static void disableEncryption(Channel channel) {
        channel.attr(DISABLE_ENCRYPTION).set(true);
    }

    public static boolean isEncryptionDisabled(Channel channel) {
        return Boolean.TRUE.equals(channel.attr(DISABLE_ENCRYPTION).get());
    }

    public static ChannelHandler createServerHandler() {
        return new ServerHandler();
    }

    private static boolean isDisableEncryptionRequest(ByteBuf buffer) {
        if (buffer.readableBytes() != DISABLE_ENCRYPTION_REQUEST_BYTES.length) {
            return false;
        }

        int readerIndex = buffer.readerIndex();
        for (int i = 0; i < DISABLE_ENCRYPTION_REQUEST_BYTES.length; i++) {
            if (buffer.getByte(readerIndex + i) != DISABLE_ENCRYPTION_REQUEST_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    private static final class ServerHandler extends ChannelInboundHandlerAdapter {
        private static final InternalLogger log = InternalLoggerFactory.getInstance(ServerHandler.class);

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.pipeline().remove(this);

            if (msg instanceof RakMessage rakMessage && isDisableEncryptionRequest(rakMessage.content())) {
                ctx.channel().attr(DISABLE_ENCRYPTION).set(true);
                log.debug("Bedrock encryption disabled for {}", ctx.channel().remoteAddress());
                rakMessage.release();
                return;
            }

            super.channelRead(ctx, msg);
        }
    }
}
