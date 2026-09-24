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

import com.google.gson.JsonObject;
import org.cloudburstmc.netty.signaling.ProviderTransport;
import org.cloudburstmc.netty.signaling.control.AssistedJoin;
import org.cloudburstmc.netty.signaling.diagnostic.DiagnosticHostPolicy;
import org.geysermc.geyser.GeyserLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Uses the provider client's existing signed, durable ticket-event delivery.
 */
public final class GameOutcomeTransport implements ProviderTransport {
    private final ProviderTransport delegate;
    private final GameOutcomeReporter outcomes;
    private final ConnectivityReporter connectivity;
    private final GeyserLogger logger;

    public GameOutcomeTransport(ProviderTransport delegate, GameOutcomeReporter outcomes, GeyserLogger logger, boolean assistedJoins) {
        this(delegate, outcomes, logger, assistedJoins, true, true, 0);
    }

    public GameOutcomeTransport(ProviderTransport delegate, GameOutcomeReporter outcomes, GeyserLogger logger,
                                boolean assistedJoins, boolean diagnostics, boolean warming, int udpPort) {
        this.delegate = delegate;
        this.outcomes = outcomes;
        this.logger = logger;
        this.connectivity = new ConnectivityReporter(logger, assistedJoins, diagnostics, warming, udpPort);
    }

    @Override
    public CompletionStage<JsonObject> hostProfile() {
        return delegate.hostProfile();
    }

    @Override
    public CompletionStage<HostProfileSnapshot> captureHostProfile() {
        var captured = delegate.captureHostProfile();
        captured.thenAccept(connectivity::publication);
        return captured;
    }

    @Override
    public long candidatePublicationVersion() {
        return delegate.candidatePublicationVersion();
    }

    @Override
    public CompletionStage<Void> reportConnectivityChecks(long candidateRevision, List<ConnectivityCheck> checks) {
        var delivered = delegate.reportConnectivityChecks(candidateRevision, checks);
        var observed = List.copyOf(checks);
        // Native delivery can successfully ignore a retired revision. Confirm it is still current before logging.
        delivered.thenCompose(ignored -> delegate.captureHostProfile()).thenAccept(snapshot -> {
            if (snapshot.candidateRevision() != candidateRevision) {
                return;
            }
            snapshot.requireCurrent();
            connectivity.checks(snapshot, observed);
        });
        return delivered;
    }

    @Override
    public boolean supportsAssistedJoins() {
        return delegate.supportsAssistedJoins();
    }

    @Override
    public CompletionStage<Void> configureStunServers(List<StunServer> servers) {
        return delegate.configureStunServers(servers);
    }

    @Override
    public CompletionStage<String> assistedJoin(AssistedJoin join, Runnable requireCurrent) {
        return delegate.assistedJoin(join, requireCurrent);
    }

    @Override
    public boolean supportsDiagnosticAdmission() {
        return delegate.supportsDiagnosticAdmission();
    }

    @Override
    public CompletionStage<Void> configureDiagnostics(DiagnosticHostPolicy policy) {
        return delegate.configureDiagnostics(policy);
    }

    @Override
    public CompletionStage<Void> configureDiagnostics(DiagnosticHostPolicy policy, long deadlineNanos) {
        return delegate.configureDiagnostics(policy, deadlineNanos);
    }

    @Override
    public CompletionStage<Void> disableDiagnostics() {
        return delegate.disableDiagnostics();
    }

    @Override
    public CompletionStage<Void> installTicketKeys(List<TicketKey> keys) {
        return delegate.installTicketKeys(keys);
    }

    @Override
    public boolean supportsGameOutcomes() {
        return true;
    }

    @Override
    public List<JsonObject> pollEvents() {
        List<JsonObject> batch = new ArrayList<>(delegate.pollEvents());
        // Native player failures are separate from maintenance checks. Report each new attempt,
        // even when the last player also failed. Assisted setup errors are logged by the control carrier.
        for (JsonObject event : batch) {
            if (!event.has("stage") || !"ticket.failed".equals(event.get("stage").getAsString())) {
                continue;
            }
            String reason = event.has("reason") ? event.get("reason").getAsString() : "";
            if (!"assisted_failed".equals(reason) && !"assisted_answer_failed".equals(reason)) {
                logger.warning("NXS: A player could not connect to the server.");
            }
        }
        outcomes.drainTo(batch, Math.max(0, 100 - batch.size()));
        return batch;
    }

    @Override
    public CompletionStage<Void> drain() {
        return delegate.drain();
    }

    @Override
    public CompletionStage<Void> close() {
        outcomes.close();
        return delegate.close();
    }
}
