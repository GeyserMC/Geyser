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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.session.GeyserSession;

import java.net.InetSocketAddress;

public class GeyserServerInitializer extends BedrockServerInitializer {
    private final GeyserImpl geyser;
    // There is a constructor that doesn't require inputting threads, but older Netty versions don't have it
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser player thread"));

    public GeyserServerInitializer(GeyserImpl geyser) {
        this.geyser = geyser;
    }

    public DefaultEventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @Override
    public void initSession(@NonNull BedrockServerSession bedrockServerSession) {
        try {
            if (this.geyser.getGeyserServer().getProxiedAddresses() != null) {
                InetSocketAddress address = this.geyser.getGeyserServer().getProxiedAddresses().get((InetSocketAddress) bedrockServerSession.getSocketAddress());
                if (address != null) {
                    ((GeyserBedrockPeer) bedrockServerSession.getPeer()).setProxiedAddress(address);
                }
            }

            bedrockServerSession.setLogging(true);
            GeyserSession session = new GeyserSession(this.geyser, bedrockServerSession, this.eventLoopGroup.next());
            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(this.geyser, session));
            this.geyser.eventBus().fire(new SessionInitializeEvent(session));
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
}
