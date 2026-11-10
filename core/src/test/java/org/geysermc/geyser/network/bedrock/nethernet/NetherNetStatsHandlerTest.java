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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.SimpleCompressionStrategy;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.ZlibCompression;
import org.cloudburstmc.protocol.common.util.Zlib;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherNetStatsHandlerTest {

    @Test
    void compressionRatioCountsMultiPacketBatches() {
        NetherNetStats stats = new NetherNetStats();
        // Same order as NetherNetChannelInitialiser: outbound batches pass the counter, then compression, then the handler
        EmbeddedChannel channel = new EmbeddedChannel(
            new NetherNetStatsHandler(null, stats),
            new CompressionCodec(new SimpleCompressionStrategy(new ZlibCompression(Zlib.RAW)), false),
            new NetherNetStatsHandler.BatchCounter(stats));

        // Built like BedrockBatchEncoder builds a batch, from one direct component per packet. Zlib copies such a
        // buffer out, which leaves it unreadable for anything behind compression
        CompositeByteBuf uncompressed = ByteBufAllocator.DEFAULT.compositeDirectBuffer();
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        for (int i = 0; i < 3; i++) {
            ByteBuf packet = ByteBufAllocator.DEFAULT.directBuffer(4096);
            packet.writeZero(4096);
            uncompressed.addComponent(true, packet);
            batch.addPacket(BedrockPacketWrapper.create());
        }
        batch.setUncompressed(uncompressed);

        channel.writeOutbound(batch);
        ReferenceCountUtil.release(channel.readOutbound());
        channel.finishAndReleaseAll();

        // 12 KiB of zeroes compresses to a few dozen bytes; before the counter moved ahead of compression this read as
        // compressed size over 0 uncompressed bytes, which the stats showed as ever growing percentages
        double ratio = stats.compressionRatio();
        assertTrue(ratio > 0 && ratio < 0.1, "compression ratio " + ratio);
    }
}
