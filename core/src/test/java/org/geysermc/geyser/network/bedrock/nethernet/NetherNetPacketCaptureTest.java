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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.protocol.bedrock.netty.BedrockBatchWrapper;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherNetPacketCaptureTest {

    @Test
    void bedrockPacketsAreWrittenWithTheirContents(@TempDir Path dir) throws Exception {
        NetherNetStats stats = new NetherNetStats();
        EmbeddedChannel channel = new EmbeddedChannel(new NetherNetStatsHandler.BatchCounter(stats));
        Path file = dir.resolve("capture.tsv");
        CompletableFuture<String> done = new CompletableFuture<>();
        assertTrue(NetherNetPacketCapture.start(channel, file, List.of("test"), 30, done::complete));
        assertFalse(NetherNetPacketCapture.start(channel, file, List.of("test"), 30, done::complete), "a second capture of one session");

        SetTimePacket time = new SetTimePacket();
        time.setTime(6000);
        BedrockBatchWrapper batch = BedrockBatchWrapper.newInstance();
        batch.addPacket(BedrockPacketWrapper.create(10, 0, 0, time, null));
        BedrockPacketWrapper preEncoded = BedrockPacketWrapper.create();
        preEncoded.setPacketId(58);
        batch.addPacket(preEncoded);
        batch.setUncompressed(ByteBufAllocator.DEFAULT.buffer(1).writeZero(1));
        channel.writeOutbound(batch);
        ReferenceCountUtil.release(channel.readOutbound());

        // Closing ends the capture
        channel.close();
        String summary = done.get(5, TimeUnit.SECONDS);
        channel.finishAndReleaseAll();

        List<String> lines = Files.readAllLines(file);
        assertEquals("# test", lines.get(0));
        assertEquals("t_ms\tdirection\tpacket\tcontents", lines.get(1));
        assertTrue(lines.get(2).matches("\\d+\tBEDROCK_OUT\tSetTimePacket\tSetTimePacket\\(time=6000\\)"), lines.get(2));
        assertTrue(lines.get(3).matches("\\d+\tBEDROCK_OUT\tid 58\t\\(pre-encoded, \\? bytes\\)"), lines.get(3));
        assertTrue(summary.startsWith("Captured 2 packets to " + file), summary);
        assertTrue(summary.contains("BEDROCK_OUT"), summary);
    }
}
