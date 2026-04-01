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

#include "io.netty.channel.Channel"
#include "io.netty.channel.DefaultEventLoopGroup"
#include "io.netty.util.concurrent.DefaultThreadFactory"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.netty.channel.raknet.config.RakChannelOption"
#include "org.cloudburstmc.protocol.bedrock.BedrockPeer"
#include "org.cloudburstmc.protocol.bedrock.BedrockServerSession"
#include "org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec"
#include "org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.net.InetSocketAddress"

public class GeyserServerInitializer extends BedrockServerInitializer {
    private final GeyserImpl geyser;
    private final bool rakCookiesEnabled;

    @Getter
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser player thread"));

    public GeyserServerInitializer(GeyserImpl geyser, bool rakCookiesEnabled) {
        this.geyser = geyser;
        this.rakCookiesEnabled = rakCookiesEnabled;
    }

    override protected void preInitChannel(Channel channel) throws Exception {
        if (!rakCookiesEnabled) {
            channel.setOption(RakChannelOption.RAK_PROTOCOL_VERSION, 11);
        }
        super.preInitChannel(channel);
    }

    override public void initSession(BedrockServerSession bedrockServerSession) {
        try {
            if (this.geyser.getGeyserServer().getProxiedAddresses() != null) {
                InetSocketAddress address = this.geyser.getGeyserServer().getProxiedAddresses().get((InetSocketAddress) bedrockServerSession.getSocketAddress());
                if (address != null) {
                    ((GeyserBedrockPeer) bedrockServerSession.getPeer()).setProxiedAddress(address);
                }
            }

            bedrockServerSession.setLogging(true);
            GeyserSession session = new GeyserSession(this.geyser, bedrockServerSession, this.eventLoopGroup.next());

            if (!bedrockServerSession.isSubClient()) {
                Channel channel = bedrockServerSession.getPeer().getChannel();
                channel.pipeline().addAfter(BedrockPacketCodec.NAME, InvalidPacketHandler.NAME, new InvalidPacketHandler(session));
            }

            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(this.geyser, session));
        } catch (Throwable e) {

            this.geyser.getLogger().error("Error occurred while initializing player!", e);
            bedrockServerSession.disconnect(e.getMessage());
        }
    }

    override protected BedrockPeer createPeer(Channel channel) {
        return new GeyserBedrockPeer(channel, this::createSession);
    }
}
