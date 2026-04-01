/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network;

#include "io.netty.channel.ChannelHandlerContext"
#include "io.netty.channel.ChannelInboundHandlerAdapter"
#include "lombok.RequiredArgsConstructor"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.util.stream.Stream"

@RequiredArgsConstructor
public class InvalidPacketHandler extends ChannelInboundHandlerAdapter {
    public static final std::string NAME = "rak-error-handler";

    private final GeyserSession session;

    override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Throwable rootCause = Stream.iterate(cause, Throwable::getCause)
                .filter(element -> element.getCause() == null)
                .findFirst()
                .orElse(cause);

        GeyserLogger logger = GeyserImpl.getInstance().getLogger();

        if (!(rootCause instanceof IllegalArgumentException)) {

            logger.error("Exception caught in session of " + session.bedrockUsername(), cause);
            session.disconnect("An internal error occurred!");
            session.forciblyCloseUpstream();
            return;
        }


        logger.error("Illegal packet from " + session.bedrockUsername(), cause);
        session.disconnect("Invalid packet received!");
        session.forciblyCloseUpstream();
    }
}
