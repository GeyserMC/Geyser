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
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchEncoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.NoopCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GeyserServerInitializer;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.network.bedrock.nethernet.codec.NetherNetFrameCodec;
import org.geysermc.geyser.network.bedrock.nethernet.codec.NetherNetPacketEncoder;
import org.geysermc.geyser.network.bedrock.nethernet.signalling.provider.GameOutcomeReporter;

/**
 * The RakNet pipeline minus RakNet: no 0xFE frame ID, and no Bedrock encryption as DTLS already covers the data channel.
 */
public class NetherNetChannelInitialiser extends GeyserServerInitializer {
    private static final CompressionStrategy NOOP_STRATEGY = new SimpleCompressionStrategy(new NoopCompression());

    private final GameOutcomeReporter outcomes;

    public NetherNetChannelInitialiser(GeyserImpl geyser) {
        this(geyser, new GameOutcomeReporter());
    }

    public NetherNetChannelInitialiser(GeyserImpl geyser, GameOutcomeReporter outcomes) {
        super(geyser, "Geyser NetherNet player thread");
        this.outcomes = outcomes;
    }

    @Override
    protected void preInitChannel(Channel channel) {
        // Nothing is compressed until the peer sets compression after RequestNetworkSettings
        channel.pipeline()
                .addLast(NetherNetFrameCodec.NAME, NetherNetFrameCodec.INSTANCE)
                .addLast(CompressionCodec.NAME, new CompressionCodec(NOOP_STRATEGY, false));
    }

    @Override
    protected void initPacketCodec(Channel channel) {
        // The parent picks this by RakNet protocol version, which a NetherNet channel does not have
        channel.pipeline().addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3());
    }

    @Override
    protected void postInitChannel(Channel channel) {
        // TODO TEST batching: the BedrockBatchEncoder added by BedrockChannelInitializer batches every packet written between
        //  flushes into one message, like RakNet. The channel fragments large messages itself, so dropping this replace should
        //  be all that's needed. Until that is tested, keep sending one packet per message.
        channel.pipeline().replace(BedrockBatchEncoder.NAME, NetherNetPacketEncoder.NAME, NetherNetPacketEncoder.INSTANCE);

        channel.pipeline().addBefore(BedrockPeer.NAME, GameOutcomeReporter.HANDLER_NAME,
                outcomes.observer(protocol -> GameProtocol.getBedrockCodec(protocol) != null));
    }

    @Override
    protected BedrockPeer createPeer(Channel channel) {
        return new NetherNetPeer(channel, this::createSession);
    }
}
