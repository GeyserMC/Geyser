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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.cloudburstmc.netty.signaling.ProviderTransport;
import org.cloudburstmc.netty.signaling.control.AssistedJoin;
import org.cloudburstmc.netty.signaling.diagnostic.DiagnosticHostPolicy;
import org.geysermc.geyser.GeyserLogger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameOutcomeTransportTest {
    private static JsonObject profile() {
        var profile = new JsonObject();
        profile.add("candidates", new JsonArray());
        return profile;
    }

    @Test
    void everyFailedPlayerConnectionWarnsWithoutRepeatingAssistedSetupErrors() {
        var nativeTransport = mock(ProviderTransport.class);
        var logger = mock(GeyserLogger.class);
        var first = new JsonObject();
        first.addProperty("stage", "ticket.failed");
        first.addProperty("reason", "timeout");
        var second = new JsonObject();
        second.addProperty("stage", "ticket.failed");
        second.addProperty("reason", "closed");
        var setup = new JsonObject();
        setup.addProperty("stage", "ticket.failed");
        setup.addProperty("reason", "assisted_answer_failed");
        when(nativeTransport.pollEvents()).thenReturn(List.of(first), List.of(second, setup), List.of());
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), logger, true);
        assertEquals(List.of(first), transport.pollEvents());
        assertEquals(List.of(second, setup), transport.pollEvents());
        assertTrue(transport.pollEvents().isEmpty());
        verify(logger, times(2)).warning("NXS: A player could not connect to the server.");
        verifyNoMoreInteractions(logger);
    }

    @Test
    void doesNotLogFeedbackUntilNativeDeliverySucceeds() {
        var nativeTransport = mock(ProviderTransport.class);
        var logger = mock(GeyserLogger.class);
        var pending = new CompletableFuture<Void>();
        when(nativeTransport.reportConnectivityChecks(anyLong(), anyList())).thenReturn(pending);
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), logger, false);
        long now = System.currentTimeMillis();
        var failed = new ProviderTransport.ConnectivityCheck(4, ProviderTransport.ConnectivityOutcome.NOT_ESTABLISHED, now - 1, now + 60000);
        assertSame(pending, transport.reportConnectivityChecks(1, List.of(failed)));
        verifyNoInteractions(logger);
        pending.completeExceptionally(new IllegalStateException("Snapshot replaced"));
        verifyNoInteractions(logger);
    }

    @Test
    void silentlyIgnoredRetiredFeedbackCannotProduceAConsoleResult() {
        var nativeTransport = mock(ProviderTransport.class);
        var logger = mock(GeyserLogger.class);
        when(nativeTransport.reportConnectivityChecks(anyLong(), anyList()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(nativeTransport.captureHostProfile()).thenReturn(CompletableFuture.completedFuture(
                new ProviderTransport.HostProfileSnapshot(profile(), 2, () -> { })));
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), logger, false);
        long now = System.currentTimeMillis();
        var failed = new ProviderTransport.ConnectivityCheck(4, ProviderTransport.ConnectivityOutcome.NOT_ESTABLISHED, now - 1, now + 60000);
        transport.reportConnectivityChecks(1, List.of(failed));
        verifyNoInteractions(logger);
        when(nativeTransport.captureHostProfile()).thenReturn(CompletableFuture.completedFuture(
                new ProviderTransport.HostProfileSnapshot(profile(), 2, () -> {
                    throw new IllegalStateException("retired");
                })));
        transport.reportConnectivityChecks(2, List.of(failed));
        verifyNoInteractions(logger);
    }

    @Test
    void preservesAssistedSupportAndOriginalOfferGuard() {
        var nativeTransport = mock(ProviderTransport.class);
        var join = mock(AssistedJoin.class);
        var current = new AtomicBoolean(true);
        Runnable requireCurrent = () -> {
            if (!current.get()) {
                throw new IllegalStateException("Control connection replaced");
            }
        };
        var pending = new CompletableFuture<String>();
        when(nativeTransport.supportsAssistedJoins()).thenReturn(true);
        when(nativeTransport.assistedJoin(join, requireCurrent)).thenReturn(pending);
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), mock(GeyserLogger.class), false);

        assertTrue(transport.supportsAssistedJoins());
        var answer = transport.assistedJoin(join, requireCurrent);
        assertSame(pending, answer);
        assertFalse(answer.toCompletableFuture().isDone());
        current.set(false);
        var failure = assertThrows(IllegalStateException.class, requireCurrent::run);
        pending.completeExceptionally(failure);
        assertSame(failure, assertThrows(CompletionException.class, answer.toCompletableFuture()::join).getCause());
        verify(nativeTransport).assistedJoin(same(join), same(requireCurrent));
        verify(nativeTransport, never()).drain();
    }

    @Test
    void preservesDelayedNativeSnapshotAndItsOriginalGuard() {
        var nativeTransport = mock(ProviderTransport.class);
        var pending = new CompletableFuture<ProviderTransport.HostProfileSnapshot>();
        when(nativeTransport.captureHostProfile()).thenReturn(pending);
        when(nativeTransport.candidatePublicationVersion()).thenReturn(7L);
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), mock(GeyserLogger.class), false);

        var captured = transport.captureHostProfile();
        assertSame(pending, captured);
        assertFalse(captured.toCompletableFuture().isDone());
        var current = new AtomicBoolean(true);
        var probes = new JsonArray();
        var probe = new JsonObject();
        probe.addProperty("address", "203.0.113.20");
        probes.add(probe);
        var snapshot = new ProviderTransport.HostProfileSnapshot(profile(), 3, 7, probes, Set.of(6), () -> {
            if (!current.get()) {
                throw new IllegalStateException("Native mapping expired or replaced");
            }
        });
        pending.complete(snapshot);
        assertSame(snapshot, captured.toCompletableFuture().join());
        assertEquals(probes, captured.toCompletableFuture().join().probeCandidates());
        assertEquals(Set.of(6), captured.toCompletableFuture().join().assistedFamilies());
        assertEquals(7, transport.candidatePublicationVersion());
        captured.toCompletableFuture().join().requireCurrent();
        current.set(false);
        assertThrows(IllegalStateException.class, captured.toCompletableFuture().join()::requireCurrent);
        verify(nativeTransport, never()).hostProfile();
    }

    @Test
    void providerStunDiscoveryWaitsForNativeConfigurationAndPreservesFailure() {
        var nativeTransport = mock(ProviderTransport.class);
        var servers = List.of(new ProviderTransport.StunServer("stun.example", 3478));
        var pending = new CompletableFuture<Void>();
        when(nativeTransport.configureStunServers(servers)).thenReturn(pending);
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), mock(GeyserLogger.class), false);

        var configured = transport.configureStunServers(servers);
        assertSame(pending, configured);
        assertFalse(configured.toCompletableFuture().isDone());
        var failure = new IllegalStateException("Native listener unavailable");
        pending.completeExceptionally(failure);
        assertSame(failure, assertThrows(CompletionException.class, configured.toCompletableFuture()::join).getCause());
        verify(nativeTransport).configureStunServers(same(servers));
    }

    @Test
    void preservesBoundedDiagnosticInstallationAndFailure() {
        var nativeTransport = mock(ProviderTransport.class);
        var policy = mock(DiagnosticHostPolicy.class);
        long originalDeadline = 123456789L;
        var pending = new CompletableFuture<Void>();
        when(nativeTransport.supportsDiagnosticAdmission()).thenReturn(true);
        when(nativeTransport.configureDiagnostics(policy, originalDeadline)).thenReturn(pending);
        var disabled = new CompletableFuture<Void>();
        when(nativeTransport.disableDiagnostics()).thenReturn(disabled);
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), mock(GeyserLogger.class), false);

        assertTrue(transport.supportsDiagnosticAdmission());
        var installed = transport.configureDiagnostics(policy, originalDeadline);
        assertSame(pending, installed);
        assertFalse(installed.toCompletableFuture().isDone());
        var failure = new IllegalStateException("Original native deadline expired");
        pending.completeExceptionally(failure);
        assertSame(failure, assertThrows(CompletionException.class, installed.toCompletableFuture()::join).getCause());
        verify(nativeTransport, never()).configureDiagnostics(policy);
        assertSame(disabled, transport.disableDiagnostics());
        assertFalse(disabled.isDone());
        disabled.complete(null);
    }

    @Test
    void forwardsOriginalFeedbackRevisionAndTimesWithoutClaimingEarlyCompletion() {
        var nativeTransport = mock(ProviderTransport.class);
        var checks = List.of(new ProviderTransport.ConnectivityCheck(6,
            ProviderTransport.ConnectivityOutcome.NOT_ESTABLISHED, 1000, 2000));
        var pending = new CompletableFuture<Void>();
        when(nativeTransport.reportConnectivityChecks(3, checks)).thenReturn(pending);
        var transport = new GameOutcomeTransport(nativeTransport, new GameOutcomeReporter(), mock(GeyserLogger.class), false);

        var delivered = transport.reportConnectivityChecks(3, checks);
        assertSame(pending, delivered);
        assertFalse(delivered.toCompletableFuture().isDone());
        verify(nativeTransport).reportConnectivityChecks(3, checks);
        pending.complete(null);
        verify(nativeTransport, never()).drain();
    }
}
