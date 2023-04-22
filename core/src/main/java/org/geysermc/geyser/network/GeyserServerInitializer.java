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

package org.geysermc.geyser.network;

import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.session.GeyserSession;

import javax.annotation.Nonnull;

public class GeyserServerInitializer extends BedrockServerInitializer {
    private final GeyserImpl geyser;
    // There is a constructor that doesn't require inputting threads, but older Netty versions don't have it
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser player thread"));

    public GeyserServerInitializer(GeyserImpl geyser) {
        this.geyser = geyser;
    }

    @Override
    public void initSession(@Nonnull BedrockServerSession bedrockServerSession) {
        try {
            bedrockServerSession.setLogging(true);
            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(this.geyser, new GeyserSession(this.geyser, bedrockServerSession, this.eventLoopGroup.next())));
            this.geyser.eventBus().fire(new SessionInitializeEvent(bedrockServerSession));
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            this.geyser.getLogger().error("Error occurred while initializing player!", e);
            bedrockServerSession.disconnect(e.getMessage());
        }
    }

    @Override
    protected BedrockPeer createPeer(Channel channel) {
        return new GeyserBedrockPeer(channel, this::createSession);
    }

    /*
    @Override
    public void onUnhandledDatagram(@Nonnull ChannelHandlerContext ctx, @Nonnull DatagramPacket packet) {
        try {
            ByteBuf content = packet.content();
            if (QueryPacketHandler.isQueryPacket(content)) {
                new QueryPacketHandler(geyser, packet.sender(), content);
            }
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            if (geyser.getConfig().isDebugMode()) {
                geyser.getLogger().error("Error occurred during unhandled datagram!", e);
            }
        }
    }
     */
}