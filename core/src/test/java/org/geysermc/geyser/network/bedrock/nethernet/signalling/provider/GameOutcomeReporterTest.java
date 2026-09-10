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

package org.geysermc.geyser.network.bedrock.nethernet.signalling.provider;

import com.google.gson.JsonObject;
import dev.kastle.netty.channel.nethernet.admission.AdmissionPrincipal;
import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.netty.signalling.ProviderTransport;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static org.junit.jupiter.api.Assertions.*;

class GameOutcomeReporterTest {
    private static EmbeddedChannel channel(GameOutcomeReporter reporter) {
        EmbeddedChannel channel = new EmbeddedChannel(reporter.observer(protocol -> protocol == 100));
        channel.attr(AdmissionPrincipal.KEY).set(new AdmissionPrincipal("ticket", "network", "private-identity", "K001"));
        return channel;
    }
    private static List<JsonObject> events(GameOutcomeReporter reporter) {
        List<JsonObject> events = new ArrayList<>(); reporter.drainTo(events, 1000); return events;
    }
    private static void settings(EmbeddedChannel channel, int protocol) {
        RequestNetworkSettingsPacket packet = new RequestNetworkSettingsPacket(); packet.setProtocolVersion(protocol);
        BedrockPacketWrapper wrapper = BedrockPacketWrapper.create(0, 0, 0, packet, null);
        assertTrue(channel.writeInbound(wrapper));
        assertSame(wrapper, channel.readInbound());
        assertEquals(1, wrapper.refCnt()); wrapper.release();
    }
    private static void disconnect(EmbeddedChannel channel, int target) {
        DisconnectPacket packet = new DisconnectPacket(); packet.setKickMessage("Private text must never reach telemetry");
        BedrockPacketWrapper wrapper = BedrockPacketWrapper.create(0, 0, target, packet, null);
        var promise = channel.newPromise();
        channel.writeAndFlush(wrapper, promise);
        assertTrue(promise.isSuccess()); assertSame(wrapper, channel.readOutbound());
        assertEquals(1, wrapper.refCnt()); wrapper.release();
    }

    @Test void reportsEarlyUnsupportedVersionWithoutChangingPacketsOrPromiseOwnership() {
        GameOutcomeReporter reporter = new GameOutcomeReporter(); EmbeddedChannel channel = channel(reporter);
        try {
            settings(channel, 101); disconnect(channel, 0); disconnect(channel, 0);
            List<JsonObject> events = events(reporter); assertEquals(1, events.size());
            assertEquals("ticket.game_rejected", events.getFirst().get("stage").getAsString());
            assertEquals("unsupported_version", events.getFirst().get("reason").getAsString());
            assertEquals("ticket", events.getFirst().get("ticketId").getAsString());
            assertEquals(4, events.getFirst().size());
            assertFalse(events.toString().contains("Private")); assertFalse(events.toString().contains("private-identity"));
        } finally { channel.finishAndReleaseAll(); }
    }
    @Test void reportsOtherExplicitServerRejectionWithoutInferringItFromClientClose() {
        GameOutcomeReporter reporter = new GameOutcomeReporter(); EmbeddedChannel channel = channel(reporter);
        settings(channel, 100); disconnect(channel, 0); channel.finishAndReleaseAll();
        assertEquals("server_rejected", events(reporter).getFirst().get("reason").getAsString());
        channel = channel(reporter); settings(channel, 101); channel.finishAndReleaseAll();
        assertTrue(events(reporter).isEmpty());
    }
    @Test void joinedThenNormalDisconnectIsNotReclassifiedAsRejected() {
        GameOutcomeReporter reporter = new GameOutcomeReporter(); EmbeddedChannel channel = channel(reporter);
        try {
            reporter.joined(channel); reporter.joined(channel); disconnect(channel, 0);
            List<JsonObject> events = events(reporter); assertEquals(1, events.size());
            assertEquals("ticket.game_joined", events.getFirst().get("stage").getAsString());
        } finally { channel.finishAndReleaseAll(); }
    }
    @Test void ignoresNonAdmittedConnectionsAndSplitScreenDisconnects() {
        GameOutcomeReporter reporter = new GameOutcomeReporter(); EmbeddedChannel channel = channel(reporter);
        try {
            disconnect(channel, 1); assertTrue(events(reporter).isEmpty());
            channel.attr(AdmissionPrincipal.KEY).set(null);
            settings(channel, 101); disconnect(channel, 0); reporter.joined(channel);
            assertTrue(events(reporter).isEmpty());
        } finally { channel.finishAndReleaseAll(); }
    }
    @Test void boundsQueueAndProviderBatchesWithoutLosingAlreadyPolledNativeEvents() {
        GameOutcomeReporter reporter = new GameOutcomeReporter();
        for (int i = 0; i < 260; i++) {
            EmbeddedChannel channel = channel(reporter);
            reporter.joined(channel); disconnect(channel, 0); channel.finishAndReleaseAll();
        }
        assertEquals(4, reporter.droppedEvents());
        FakeTransport nativeTransport = new FakeTransport();
        for (int i = 0; i < 99; i++) nativeTransport.events.add(new JsonObject());
        GameOutcomeTransport transport = new GameOutcomeTransport(nativeTransport, reporter);
        List<JsonObject> first = transport.pollEvents(); assertEquals(100, first.size());
        assertEquals("ticket.game_joined", first.getLast().get("stage").getAsString());
        assertEquals(100, transport.pollEvents().size()); assertEquals(100, transport.pollEvents().size());
        assertEquals(55, transport.pollEvents().size()); assertTrue(transport.pollEvents().isEmpty());
        EmbeddedChannel channel = channel(reporter); reporter.joined(channel); channel.finishAndReleaseAll();
        transport.close(); assertTrue(transport.pollEvents().isEmpty()); assertTrue(nativeTransport.closed);
    }
    private static final class FakeTransport implements ProviderTransport {
        final List<JsonObject> events = new ArrayList<>(); boolean closed;
        public CompletionStage<JsonObject> hostProfile() { return CompletableFuture.completedFuture(new JsonObject()); }
        public CompletionStage<Void> installTicketKeys(List<TicketKey> keys) { return CompletableFuture.completedFuture(null); }
        public CompletionStage<ApplyResult> applyState(String state) { return CompletableFuture.completedFuture(ApplyResult.APPLIED); }
        public List<JsonObject> pollEvents() { List<JsonObject> result = new ArrayList<>(events); events.clear(); return result; }
        public CompletionStage<Void> drain() { return CompletableFuture.completedFuture(null); }
        public CompletionStage<Void> close() { closed = true; return CompletableFuture.completedFuture(null); }
    }
}
