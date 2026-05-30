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

package org.geysermc.geyser.network.nethernet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.netty.codec.FrameIdCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchDecoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchEncoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.NoopCompression;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GeyserBedrockPeer;
import org.geysermc.geyser.network.GeyserServerInitializer;
import org.geysermc.geyser.network.netty.BedrockEncryptionControl;

/**
 * Channel initializer for incoming Nethernet (WebRTC) connections.
 * Mirrors the CloudburstMC BedrockChannelInitializer pipeline for
 * rak protocol version 11, but without reading RakChannelOption from
 * the channel (Nethernet channels don't have RakNet options).
 *
 * The pipeline is:
 * NetherNetFramingAdapter -> FrameIdCodec -> CompressionCodec ->
 * BedrockBatchDecoder/Encoder -> BedrockPacketCodec_v3 -> GeyserBedrockPeer
 */
public class NetherNetServerInitializer extends ChannelInitializer<Channel> {

    private static final FrameIdCodec FRAME_CODEC = new FrameIdCodec(0xFE);
    private static final BedrockBatchDecoder BATCH_DECODER = new BedrockBatchDecoder();

    private final GeyserImpl geyser;
    private final DefaultEventLoopGroup eventLoopGroup;
    private final ChannelGroup playerChannels;

    public NetherNetServerInitializer(GeyserImpl geyser, DefaultEventLoopGroup eventLoopGroup, ChannelGroup playerChannels) {
        this.geyser = geyser;
        this.eventLoopGroup = eventLoopGroup;
        this.playerChannels = playerChannels;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        // Register for clean disconnect on a signaling rebuild. The group removes
        // the channel automatically when it closes.
        playerChannels.add(channel);

        // Disable Bedrock encryption - Nethernet uses DTLS for transport encryption
        BedrockEncryptionControl.disableEncryption(channel);

        // Set packet direction for server-side codec
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);

        // Build the Bedrock pipeline mirroring CloudburstMC's init for rak version 11
        channel.pipeline()
                .addLast(NetherNetFramingAdapter.NAME, new NetherNetFramingAdapter())
                .addLast(FrameIdCodec.NAME, FRAME_CODEC)
                .addLast(CompressionCodec.NAME, new CompressionCodec(
                        new SimpleCompressionStrategy(new NoopCompression()), false))
                .addLast(BedrockBatchDecoder.NAME, BATCH_DECODER)
                .addLast(BedrockBatchEncoder.NAME, new BedrockBatchEncoder())
                .addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3())
                .addLast(BedrockPeer.NAME, new GeyserBedrockPeer(channel, this::createSession));
    }

    private BedrockServerSession createSession(BedrockPeer peer, int subClientId) {
        BedrockServerSession session = new BedrockServerSession(peer, subClientId);
        try {
            GeyserServerInitializer.initGeyserSession(session, geyser, eventLoopGroup);
        } catch (Throwable e) {
            geyser.getLogger().error("Error occurred while initializing Nethernet player!", e);
            session.disconnect(e.getMessage());
        }
        return session;
    }
}
