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

package org.geysermc.geyser.network.bedrock.nethernet.signaling.provider;

import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.netty.signaling.ProviderClient.GameOutcome;
import org.cloudburstmc.netty.signaling.admission.AdmissionPrincipal;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameOutcomeReporterTest {
    private record Report(String ticketId, GameOutcome outcome, String reason) {
    }

    private final List<Report> reports = new ArrayList<>();
    private boolean accepting = true;
    private final GameOutcomeReporter reporter = new GameOutcomeReporter((ticketId, outcome, reason) -> {
        reports.add(new Report(ticketId, outcome, reason));
        return accepting;
    });

    private EmbeddedChannel channel() {
        EmbeddedChannel channel = new EmbeddedChannel(reporter.observer(protocol -> protocol == 100));
        channel.attr(AdmissionPrincipal.KEY).set(new AdmissionPrincipal("ticket", "network", "private-identity", "K001"));
        return channel;
    }

    private static void settings(EmbeddedChannel channel, int protocol) {
        RequestNetworkSettingsPacket packet = new RequestNetworkSettingsPacket();
        packet.setProtocolVersion(protocol);
        BedrockPacketWrapper wrapper = BedrockPacketWrapper.create(0, 0, 0, packet, null);
        assertTrue(channel.writeInbound(wrapper));
        assertSame(wrapper, channel.readInbound());
        assertEquals(1, wrapper.refCnt());
        wrapper.release();
    }

    private static void disconnect(EmbeddedChannel channel, int target) {
        DisconnectPacket packet = new DisconnectPacket();
        packet.setKickMessage("Private text must never reach telemetry");
        BedrockPacketWrapper wrapper = BedrockPacketWrapper.create(0, 0, target, packet, null);
        var promise = channel.newPromise();
        channel.writeAndFlush(wrapper, promise);
        assertTrue(promise.isSuccess());
        assertSame(wrapper, channel.readOutbound());
        assertEquals(1, wrapper.refCnt());
        wrapper.release();
    }

    @Test
    void reportsEarlyUnsupportedVersionWithoutChangingPacketsOrPromiseOwnership() {
        EmbeddedChannel channel = channel();
        try {
            settings(channel, 101);
            disconnect(channel, 0);
            disconnect(channel, 0);
            assertEquals(List.of(new Report("ticket", GameOutcome.REJECTED, "unsupported_version")), reports);
            assertFalse(reports.toString().contains("Private"));
            assertFalse(reports.toString().contains("private-identity"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void reportsOtherExplicitServerRejectionWithoutInferringItFromClientClose() {
        EmbeddedChannel channel = channel();
        settings(channel, 100);
        disconnect(channel, 0);
        channel.finishAndReleaseAll();
        assertEquals("server_rejected", reports.getFirst().reason());
        reports.clear();
        channel = channel();
        settings(channel, 101);
        channel.finishAndReleaseAll();
        assertTrue(reports.isEmpty());
    }

    @Test
    void joinedThenNormalDisconnectIsNotReclassifiedAsRejected() {
        EmbeddedChannel channel = channel();
        try {
            reporter.joined(channel);
            reporter.joined(channel);
            disconnect(channel, 0);
            assertEquals(List.of(new Report("ticket", GameOutcome.JOINED, "play_ready")), reports);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void ignoresNonAdmittedConnectionsAndSplitScreenDisconnects() {
        EmbeddedChannel channel = channel();
        try {
            disconnect(channel, 1);
            assertTrue(reports.isEmpty());
            channel.attr(AdmissionPrincipal.KEY).set(null);
            settings(channel, 101);
            disconnect(channel, 0);
            reporter.joined(channel);
            assertTrue(reports.isEmpty());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void countsDroppedOutcomesOncePerPlayer() {
        accepting = false;
        EmbeddedChannel channel = channel();
        try {
            reporter.joined(channel);
            disconnect(channel, 0);
            assertEquals(1, reporter.droppedEvents());
            assertEquals(1, reports.size(), "A dropped join is not later reported as a rejection");
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
