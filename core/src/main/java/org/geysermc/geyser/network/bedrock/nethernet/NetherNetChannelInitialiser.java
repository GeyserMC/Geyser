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

package org.geysermc.geyser.network.bedrock.nethernet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.ZlibCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.common.util.Zlib;
import org.geysermc.geyser.network.bedrock.nethernet.codec.NetherNetPacketDecoder;
import org.geysermc.geyser.network.bedrock.nethernet.codec.NetherNetPacketEncoder;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.bedrock.InvalidPacketHandler;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GameOutcomeReporter;
import org.geysermc.geyser.network.bedrock.UpstreamPacketHandler;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Closely mirrors {@link org.geysermc.geyser.network.GeyserServerInitializer} but with the addition of NetherNet packet encoder/decoder and a different peer implementation.
 */
public class NetherNetChannelInitialiser extends ChannelInitializer<Channel> {
    private static final CompressionStrategy ZLIB_RAW_STRATEGY = new SimpleCompressionStrategy(new ZlibCompression(Zlib.RAW));

    private final GeyserImpl geyser;
    private final GameOutcomeReporter outcomes;

    @Getter
    private final DefaultEventLoopGroup eventLoopGroup;

    public NetherNetChannelInitialiser(GeyserImpl geyser) {
        this(geyser, new GameOutcomeReporter());
    }

    public NetherNetChannelInitialiser(GeyserImpl geyser, GameOutcomeReporter outcomes) {
        this.geyser = geyser;
        this.outcomes = outcomes;
        this.eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser NetherNet player thread"));
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        channel.pipeline()
                .addLast(NetherNetPacketDecoder.NAME, new NetherNetPacketDecoder())
                .addLast(NetherNetPacketEncoder.NAME, new NetherNetPacketEncoder())
                .addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3())
                .addLast(GameOutcomeReporter.HANDLER_NAME, outcomes.observer(protocol -> GameProtocol.getBedrockCodec(protocol) != null))
                .addLast(BedrockPeer.NAME, new NetherNetPeer(channel, this::createSession));
    }

    public static CompressionStrategy getCompression() {
        return ZLIB_RAW_STRATEGY;
    }

    private BedrockServerSession createSession(BedrockPeer peer, int subClientId) {
        BedrockServerSession session = new BedrockServerSession(peer, subClientId);
        initSession(session);
        return session;
    }

    protected void initSession(BedrockServerSession bedrockServerSession) {
        try {
            bedrockServerSession.setLogging(this.geyser.config().debugMode());
            GeyserSession session = new GeyserSession(this.geyser, bedrockServerSession, this.eventLoopGroup.next());

            if (!bedrockServerSession.isSubClient()) {
                Channel channel = bedrockServerSession.getPeer().getChannel();
                // Added after BedrockPeer, not BedrockPacketCodec to ensure exceptions thrown while dispatching
                // to the packet handler also get here if no other handler exists
                channel.pipeline().addAfter(BedrockPeer.NAME, InvalidPacketHandler.NAME, new InvalidPacketHandler(session));
            }

            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(this.geyser, session));
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            this.geyser.getLogger().error("Error occurred while initializing player!", e);
            bedrockServerSession.disconnect(e.getMessage());
        }
    }
}
