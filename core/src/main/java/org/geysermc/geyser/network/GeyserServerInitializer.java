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
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.netty.BedrockEncryptionControl;
import org.geysermc.geyser.session.GeyserSession;

public class GeyserServerInitializer extends BedrockServerInitializer {
    private final GeyserImpl geyser;
    private final boolean rakCookiesEnabled;
    @Getter
    private final DefaultEventLoopGroup eventLoopGroup;

    public GeyserServerInitializer(GeyserImpl geyser, boolean rakCookiesEnabled) {
        this(geyser, rakCookiesEnabled, new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser player thread")));
    }

    public GeyserServerInitializer(GeyserImpl geyser, boolean rakCookiesEnabled, DefaultEventLoopGroup eventLoopGroup) {
        this.geyser = geyser;
        this.rakCookiesEnabled = rakCookiesEnabled;
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    protected void preInitChannel(Channel channel) throws Exception {
        if (!rakCookiesEnabled) {
            channel.setOption(RakChannelOption.RAK_PROTOCOL_VERSION, 11);
        }
        channel.pipeline().addLast(BedrockEncryptionControl.HANDLER_NAME, BedrockEncryptionControl.createServerHandler());
        super.preInitChannel(channel);
    }

    @Override
    public void initSession(@NonNull BedrockServerSession bedrockServerSession) {
        try {
            initGeyserSession(bedrockServerSession, this.geyser, this.eventLoopGroup);
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            this.geyser.getLogger().error("Error occurred while initializing player!", e);
            bedrockServerSession.disconnect(e.getMessage());
        }
    }

    /**
     * Shared session initialization used by both RakNet and Nethernet initializers.
     */
    public static void initGeyserSession(BedrockServerSession bedrockServerSession, GeyserImpl geyser,
                                   DefaultEventLoopGroup eventLoopGroup) {
        bedrockServerSession.setLogging(geyser.config().debugMode());
        GeyserSession session = new GeyserSession(geyser, bedrockServerSession, eventLoopGroup.next());

        if (!bedrockServerSession.isSubClient()) {
            Channel channel = bedrockServerSession.getPeer().getChannel();
            channel.pipeline().addAfter(BedrockPacketCodec.NAME, InvalidPacketHandler.NAME, new InvalidPacketHandler(session));
        }

        bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(geyser, session));
    }

    @Override
    protected BedrockPeer createPeer(Channel channel) {
        return new GeyserBedrockPeer(channel, this::createSession);
    }
}
