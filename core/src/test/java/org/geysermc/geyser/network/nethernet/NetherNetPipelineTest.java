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

import dev.kastle.netty.channel.nethernet.codec.NetherNetFramingCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.protocol.bedrock.PacketDirection;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchDecoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.batch.BedrockBatchEncoder;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Assembles the exact native NetherNet inbound pipeline (framing, compression,
 * batch, packet codec) as built by NetherNetServerInitializer and feeds it the
 * first packet a Bedrock client sends, verifying it decodes end to end.
 */
class NetherNetPipelineTest {

    private static final int PROTOCOL_VERSION = Bedrock_v2168.CODEC.getProtocolVersion();

    private static ChannelInboundHandlerAdapter tap(List<String> trace, String name) {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof ByteBuf) {
                    trace.add(name + ":" + ((ByteBuf) msg).readableBytes() + "b");
                } else {
                    trace.add(name + ":" + msg.getClass().getSimpleName());
                }
                ctx.fireChannelRead(msg);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                trace.add(name + ":EXCEPTION:" + cause);
                ctx.fireExceptionCaught(cause);
            }
        };
    }

    private static BedrockPacketCodec packetCodec() {
        BedrockPacketCodec codec = new BedrockPacketCodec_v3();
        codec.setCodec(Bedrock_v2168.CODEC);
        return codec;
    }

    @Test
    void requestNetworkSettingsDecodesThroughNativePipeline() {
        List<Object> received = new ArrayList<>();

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND);
        channel.pipeline()
                .addLast(NetherNetFramingCodec.NAME, new NetherNetFramingCodec(10000))
                .addLast(NetherNetBatchWrapperCodec.NAME, NetherNetBatchWrapperCodec.INSTANCE)
                .addLast(CompressionCodec.NAME, new CompressionCodec(
                        BedrockChannelInitializer.getCompression(PacketCompressionAlgorithm.ZLIB, 11, true), false))
                .addLast(BedrockBatchDecoder.NAME, new BedrockBatchDecoder())
                .addLast(BedrockBatchEncoder.NAME, new BedrockBatchEncoder())
                .addLast(BedrockPacketCodec.NAME, packetCodec())
                .addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        received.add(msg);
                    }
                });

        // The first frame from a Bedrock client:
        // 8 bytes total. NetherNet framing header 0 (complete single message),
        // then the batch: varuint packet length 6, v3 packet header varuint for
        // packet id 193 (RequestNetworkSettings), then the protocol version as
        // a big endian int.
        ByteBuf frame = Unpooled.buffer(8);
        frame.writeByte(0x00);           // NetherNet framing header
        frame.writeByte(0x06);           // batch: packet length 6
        frame.writeByte(0xC1);           // v3 header varuint low byte (id 193)
        frame.writeByte(0x01);           // v3 header varuint high byte
        frame.writeInt(PROTOCOL_VERSION); // protocol version, big endian

        // Taps between every stage to locate where the message dies.
        List<String> trace = new ArrayList<>();
        channel.pipeline().addAfter(NetherNetFramingCodec.NAME, "tap-framing", tap(trace, "after-framing"));
        channel.pipeline().addAfter(CompressionCodec.NAME, "tap-compression", tap(trace, "after-compression"));
        channel.pipeline().addAfter(BedrockBatchDecoder.NAME, "tap-batch", tap(trace, "after-batch"));

        channel.writeInbound(frame);

        assertEquals(1, received.size(), "exactly one packet should decode; stage trace: " + trace);
        Object msg = received.get(0);
        assertInstanceOf(BedrockPacketWrapper.class, msg);
        BedrockPacketWrapper wrapper = (BedrockPacketWrapper) msg;
        assertInstanceOf(RequestNetworkSettingsPacket.class, wrapper.getPacket());
        assertEquals(PROTOCOL_VERSION, ((RequestNetworkSettingsPacket) wrapper.getPacket()).getProtocolVersion());

        wrapper.release();

        // Outbound leg: the NetworkSettings response must exit the pipeline
        // as a framed message with a zero header.
        NetworkSettingsPacket response = new NetworkSettingsPacket();
        response.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);
        response.setCompressionThreshold(512);
        assertTrue(channel.writeOutbound(
                org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper.create(0, 0, 0, response, null)),
                "outbound write should produce a framed message; stage trace: " + trace);

        ByteBuf outbound = channel.readOutbound();
        assertNotNull(outbound, "a framed outbound message should be emitted; stage trace: " + trace);
        assertEquals(0, outbound.readUnsignedByte(), "outbound framing header should be 0");
        assertTrue(outbound.readableBytes() > 0, "outbound payload should be non empty");
        outbound.release();

        assertFalse(channel.finish());
    }
}
