/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Kicks the client on the first exception of its connection. Added to every connection, as an exception can come
 * before the first packet has created the session.
 */
public class InvalidPacketHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "rak-error-handler";

    @Setter
    private @Nullable GeyserSession session;
    private boolean handled;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // The rest of a batch can still fail after the close, and one report per connection is enough
        if (handled) {
            return;
        }
        handled = true;

        GeyserLogger logger = GeyserImpl.getInstance().getLogger();
        if (session == null) {
            logger.debug("Closed a connection on an exception before its session existed: " + cause);
            ctx.channel().close();
            return;
        }

        if (cause instanceof IOException) {
            // The transport lost the connection, as when a player aborts it on leaving, so nobody is at fault.
            // Closing keeps the reason the transport gave, which a disconnect would replace.
            // The name is only a placeholder before login, so the address tells connections apart
            logger.debug("The connection of " + session.bedrockUsername() + " from " + session.getSocketAddress()
                + " failed: " + cause);
            ctx.channel().close();
            return;
        }

        Throwable rootCause = Stream.iterate(cause, Throwable::getCause)
                .filter(element -> element.getCause() == null)
                .findFirst()
                .orElse(cause);

        if (!(rootCause instanceof IllegalPacketException)) {
            // Kick users that cause exceptions
            logger.error("Exception caught in session of " + session.bedrockUsername(), cause);
            session.disconnect("An internal error occurred!");
            session.forciblyCloseUpstream();
            return;
        }

        // Kick users that try to send illegal packets
        logger.error("Illegal packet from " + session.bedrockUsername(), cause);
        session.disconnect("Invalid packet received!");
        session.forciblyCloseUpstream();
    }
}
