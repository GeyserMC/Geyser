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

import com.google.gson.JsonParser;
import org.cloudburstmc.netty.signaling.ProviderTransport;
import org.geysermc.geyser.GeyserLogger;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import static org.cloudburstmc.netty.signaling.ProviderTransport.ConnectivityOutcome.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectivityReporterTest {
    private static final String TARGET = "[{\"address\":\"203.0.113.10\",\"port\":19133}]";

    private static ProviderTransport.HostProfileSnapshot snapshot(long revision, String offers, String probes, boolean assisted) {
        var profile = JsonParser.parseString("{\"candidates\":" + offers + "}").getAsJsonObject();
        return new ProviderTransport.HostProfileSnapshot(profile, revision, revision,
            JsonParser.parseString(probes).getAsJsonArray(), assisted ? Set.of(4, 6) : Set.of(), () -> { });
    }

    private static ProviderTransport.ConnectivityCheck check(String region, int family, String method,
            String address, ProviderTransport.ConnectivityOutcome outcome, long time) {
        return new ProviderTransport.ConnectivityCheck(region, family, method, new InetSocketAddress(address, 19133), outcome, time, time + 60000);
    }

    @Test
    void logsFailuresRepeatsRecoveryAndFailureAgainAtTheRightLevels() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, true, true, true, 19133);
        var snapshot = snapshot(1, "[]", "[]", true);
        reporter.publication(snapshot);
        clearInvocations(logger);
        long now = System.currentTimeMillis() - 5000;
        var first = check("lim1", 4, "per_join", "203.0.113.10", NOT_ESTABLISHED, now);
        reporter.checks(snapshot, List.of(first));
        reporter.checks(snapshot, List.of(first)); // A heartbeat copy is not a new check.
        reporter.checks(snapshot, List.of(check("lim1", 4, "per_join", "203.0.113.11", NOT_ESTABLISHED, now + 1)));
        reporter.checks(snapshot, List.of(check("lim1", 4, "per_join", "203.0.113.12", ESTABLISHED, now + 2)));
        reporter.checks(snapshot, List.of(check("lim1", 4, "per_join", "203.0.113.13", ESTABLISHED, now + 3)));
        reporter.checks(snapshot, List.of(check("lim1", 4, "per_join", "203.0.113.14", NOT_ESTABLISHED, now + 4)));
        var order = inOrder(logger);
        order.verify(logger).warning(contains("check from lim1 failed."));
        order.verify(logger).debug(startsWith("NXS check details:"));
        order.verify(logger).debug(contains("check from lim1 failed."));
        order.verify(logger).debug(startsWith("NXS check details:"));
        order.verify(logger).info(contains("check from lim1 passed."));
        order.verify(logger).debug(startsWith("NXS check details:"));
        order.verify(logger).debug(contains("check from lim1 passed."));
        order.verify(logger).debug(startsWith("NXS check details:"));
        order.verify(logger).warning(contains("check from lim1 failed."));
        order.verify(logger).debug(startsWith("NXS check details:"));
        order.verifyNoMoreInteractions();
    }

    @Test
    void regionsFamiliesAndMethodsHaveIndependentResults() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, true, true, true, 19133);
        var snapshot = snapshot(1, TARGET, TARGET, true);
        reporter.publication(snapshot);
        clearInvocations(logger);
        long now = System.currentTimeMillis() - 100;
        reporter.checks(snapshot, List.of(
            check("lim1", 4, "per_join", "203.0.113.10", NOT_ESTABLISHED, now),
            check("vin1", 4, "per_join", "203.0.113.10", ESTABLISHED, now),
            check("lim1", 6, "per_join", "2001:db8::1", ESTABLISHED, now),
            check("lim1", 4, "defined", "203.0.113.10", ESTABLISHED, now)));
        verify(logger).warning(contains("IPv4 assisted connection check from lim1 failed."));
        verify(logger).info("NXS IPv4 assisted connection check from vin1 passed.");
        verify(logger).info("NXS IPv6 assisted connection check from lim1 passed.");
        verify(logger).info("NXS IPv4 direct connection check from lim1 passed.");
    }

    @Test
    void revisionChangesDoNotRepeatAnUnchangedFailureButNewFixedTargetsDo() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, false, true, true, 19133);
        long now = System.currentTimeMillis() - 100;
        var first = check("lim1", 4, "defined", "203.0.113.10", NOT_ESTABLISHED, now);
        reporter.checks(snapshot(1, TARGET, TARGET, false), List.of(first));
        reporter.checks(snapshot(2, TARGET, TARGET, false), List.of(first));
        reporter.checks(snapshot(1, TARGET, TARGET, false), List.of(check("lim1", 4, "defined", "203.0.113.10", ESTABLISHED, now + 10)));
        verify(logger, times(1)).warning(contains("check from lim1 failed."));
        verify(logger).debug(contains("check from lim1 failed."));
        verify(logger, never()).info(contains("passed"));
        reporter.checks(snapshot(2, TARGET, TARGET, false), List.of(check("lim1", 4, "defined", "203.0.113.11", NOT_ESTABLISHED, now + 1)));
        verify(logger, times(2)).warning(contains("check from lim1 failed."));
    }

    @Test
    void withdrawalAndRecoveryKeepTheirOperatorMeaning() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, false, true, true, 19133);
        long now = System.currentTimeMillis() - 100;
        reporter.checks(snapshot(1, "[]", TARGET, false), List.of(check("lim1", 4, "warm_stun", "203.0.113.10", NOT_ESTABLISHED, now)));
        verify(logger).warning(contains("This address is temporarily unavailable to players."));
        verify(logger).warning(contains("Check UDP port 19133 in your firewall."));
        reporter.checks(snapshot(1, TARGET, TARGET, false), List.of(check("lim1", 4, "warm_stun", "203.0.113.10", ESTABLISHED, now + 1)));
        verify(logger).info("NXS IPv4 is available for player connections.");
        verify(logger).info(contains("check from lim1 passed."));
    }

    @Test
    void partialRegionalFailureDoesNotClaimWithdrawal() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, false, true, true, 19133);
        reporter.checks(snapshot(1, TARGET, TARGET, false), List.of(check("vin1", 4, "defined", "203.0.113.10", NOT_ESTABLISHED, System.currentTimeMillis() - 1)));
        verify(logger).warning(contains("Other regions can still reach this address."));
        verify(logger, never()).warning(contains("temporarily unavailable"));
    }

    @Test
    void inconclusiveAndUnavailableChecksRepeatAtDebugAndInvalidTimesAreIgnored() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, false, true, false, 19133);
        var snapshot = snapshot(1, TARGET, TARGET, false);
        long now = System.currentTimeMillis() - 1000;
        reporter.checks(snapshot, List.of(
            check("lim1", 4, "defined", "203.0.113.10", UNKNOWN, now),
            check("vin1", 4, "defined", "203.0.113.10", UNAVAILABLE, now),
            check("old", 4, "defined", "203.0.113.10", NOT_ESTABLISHED, now - 60001),
            check("future", 4, "defined", "203.0.113.10", NOT_ESTABLISHED, now + 10000)));
        reporter.checks(snapshot, List.of(
            check("lim1", 4, "defined", "203.0.113.10", UNKNOWN, now + 1),
            check("vin1", 4, "defined", "203.0.113.10", UNAVAILABLE, now + 1)));
        verify(logger, times(2)).warning(contains("Player addresses are unchanged."));
        verify(logger).debug(contains("was inconclusive."));
        verify(logger).debug(contains("could not run."));
        verify(logger, never()).warning(contains("failed."));
    }

    @Test
    void disabledDiagnosticsAndStaleSnapshotsAreExplicit() {
        var logger = mock(GeyserLogger.class);
        var reporter = new ConnectivityReporter(logger, false, false, false, 19133);
        var snapshot = snapshot(1, TARGET, TARGET, false);
        reporter.publication(snapshot);
        reporter.publication(snapshot);
        verify(logger, times(1)).warning("NXS connection checks are turned off. Server reachability cannot be checked.");
        var retired = new ProviderTransport.HostProfileSnapshot(snapshot.profile(), 1, () -> {
            throw new IllegalStateException("retired");
        });
        assertThrows(IllegalStateException.class, () -> reporter.checks(retired, List.of()));
    }
}
