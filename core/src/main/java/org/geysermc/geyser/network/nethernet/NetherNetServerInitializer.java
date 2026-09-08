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

import io.github.sendablemetatype.netty.channel.nethernet.codec.NetherNetFramingCodec;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import org.cloudburstmc.protocol.bedrock.BedrockDisconnectReasons;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GeyserServerInitializer;
import org.geysermc.geyser.network.netty.BedrockEncryptionControl;

/**
 * Channel initializer for incoming Nethernet (WebRTC) connections, built on
 * CloudburstMC's transport neutral BedrockChannelInitializer seam. NetherNet
 * is a first class transport here: no FrameIdCodec (the 0xFE byte is RakNet
 * framing; data channel payloads are bare batch bytes) and no RakMessage
 * wrapping. The pipeline is:
 *
 * NetherNetFramingCodec (countdown fragmentation/reassembly)
 *   -> CompressionCodec (NOOP until NetworkSettings negotiates, rak v11 semantics)
 *     -> BedrockBatchDecoder/Encoder
 *       -> BedrockPacketCodec_v3
 *         -> NetherNetGeyserPeer -> GeyserSession
 *
 * Bedrock layer encryption stays disabled: DTLS already secures the
 * transport, and clients expect none on NetherNet.
 */
public class NetherNetServerInitializer extends BedrockChannelInitializer<BedrockServerSession> {

    private final GeyserImpl geyser;
    private final DefaultEventLoopGroup eventLoopGroup;
    private final ChannelGroup playerChannels;

    public NetherNetServerInitializer(GeyserImpl geyser, DefaultEventLoopGroup eventLoopGroup, ChannelGroup playerChannels) {
        this.geyser = geyser;
        this.eventLoopGroup = eventLoopGroup;
        this.playerChannels = playerChannels;
    }

    @Override
    protected void preInitChannel(Channel channel) {
        // Register for clean disconnect on a real teardown. The group removes
        // the channel automatically when it closes.
        playerChannels.add(channel);

        // Disable Bedrock encryption - Nethernet uses DTLS for transport encryption
        BedrockEncryptionControl.disableEncryption(channel);

        // Set packet direction for server-side codec
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);

        channel.pipeline()
                .addLast(NetherNetFramingCodec.NAME, new NetherNetFramingCodec())
                .addLast(NetherNetBatchWrapperCodec.NAME, NetherNetBatchWrapperCodec.INSTANCE)
                .addLast(CompressionCodec.NAME, new CompressionCodec(
                        getCompression(PacketCompressionAlgorithm.ZLIB, 11, true), false));
    }

    @Override
    protected void initPacketCodec(Channel channel) {
        channel.pipeline().addLast(BedrockPacketCodec.NAME, new BedrockPacketCodec_v3());
    }

    @Override
    protected BedrockPeer createPeer(Channel channel) {
        return new NetherNetGeyserPeer(channel, this::createSession);
    }

    @Override
    protected BedrockServerSession createSession0(BedrockPeer peer, int subClientId) {
        BedrockServerSession session = new BedrockServerSession(peer, subClientId);
        // On RakNet the transport refines the session's disconnect reason
        // (client quit becomes CLOSED, which Geyser renders as its closed by
        // remote peer message). NetherNet reports no such granularity, so
        // without this the default UNKNOWN surfaces as a raw disconnect.lost
        // in logs. A transport level death here IS the peer going away, so
        // CLOSED is the accurate default; kicks and errors pass their own
        // reason explicitly and never read this.
        session.setDisconnectReason(BedrockDisconnectReasons.CLOSED);
        return session;
    }

    @Override
    protected void initSession(BedrockServerSession session) {
        try {
            GeyserServerInitializer.initGeyserSession(session, geyser, eventLoopGroup);
        } catch (Throwable e) {
            geyser.getLogger().error("Error occurred while initializing Nethernet player!", e);
            session.disconnect(e.getMessage());
        }
    }
}
