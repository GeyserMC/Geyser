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
import org.cloudburstmc.netty.signaling.ProviderTransport.ConnectivityCheck;
import org.cloudburstmc.netty.signaling.ProviderTransport.ConnectivityOutcome;
import org.cloudburstmc.netty.signaling.ProviderTransport.HostProfileSnapshot;
import org.geysermc.geyser.GeyserLogger;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Operator-facing observations of host policy; this reporter never changes serving or admission. */
final class ConnectivityReporter {
    private record Key(String region, int family, String method) { }
    private final GeyserLogger logger;
    private final boolean assisted;
    private final boolean diagnostics;
    private final boolean warming;
    private final String port;
    private record Observation(long revision, ConnectivityCheck check) { }
    private final Map<Key, Observation> reported = new HashMap<>();
    private final Map<Integer, String> publications = new HashMap<>();
    private final Map<Integer, Boolean> available = new HashMap<>();
    private long revision;
    private boolean announced;

    ConnectivityReporter(GeyserLogger logger, boolean assisted, boolean diagnostics, boolean warming, int udpPort) {
        this.logger = logger;
        this.assisted = assisted;
        this.diagnostics = diagnostics;
        this.warming = warming;
        this.port = udpPort > 0 ? "UDP port " + udpPort : "the NetherNet UDP port";
    }

    synchronized void publication(HostProfileSnapshot snapshot) {
        snapshot.requireCurrent();
        if (!announced) {
            announced = true;
            logger.debug("NXS settings: assisted joins=" + assisted + "; checks=" + diagnostics + "; background discovery=" + warming + ".");
            if (!diagnostics) {
                logger.warning("NXS connection checks are turned off. Server reachability cannot be checked.");
            }
        }
        for (int family : List.of(4, 6)) {
            var offered = endpoints(snapshot.profile().getAsJsonArray("candidates"), family);
            var probes = endpoints(snapshot.probeCandidates(), family);
            boolean assist = assisted && snapshot.assistedFamilies().contains(family);
            String decision = "offered=" + offered + "; test targets=" + probes + "; assisted=" + assist;
            String previousDecision = publications.put(family, decision);
            if (decision.equals(previousDecision)) {
                continue;
            }
            boolean usable = !offered.isEmpty() || assist;
            Boolean wasAvailable = available.put(family, usable);
            if (!Objects.equals(wasAvailable, usable)) {
                if (usable) {
                    logger.info("NXS IPv" + family + " is available for player connections.");
                } else if (Boolean.TRUE.equals(wasAvailable) || !probes.isEmpty()) {
                    logger.warning("NXS IPv" + family + " has no available player address."
                        + (diagnostics ? " Connection checks will continue." : ""));
                }
            }
            logger.debug("NXS IPv" + family + " addresses: " + decision + ".");
        }
    }

    synchronized void checks(HostProfileSnapshot snapshot, List<ConnectivityCheck> checks) {
        snapshot.requireCurrent();
        if (snapshot.candidateRevision() < revision) {
            return;
        }
        revision = snapshot.candidateRevision();
        publication(snapshot);
        long now = System.currentTimeMillis();
        for (var check : checks.stream().sorted(java.util.Comparator.comparingLong(ConnectivityCheck::checkedAt)).toList()) {
            if (check.checkedAt() > now || check.expiresAt() <= now) {
                continue;
            }
            var key = new Key(check.region(), check.family(), check.method());
            var observation = reported.get(key);
            var previous = observation == null ? null : observation.check();
            if (observation != null && observation.revision() == revision && previous.checkedAt() >= check.checkedAt()) {
                continue;
            }
            reported.put(key, new Observation(revision, check));
            String result = switch (check.outcome()) {
                case ESTABLISHED -> {
                    yield "passed";
                }
                case NOT_ESTABLISHED -> {
                    yield "failed";
                }
                case UNKNOWN -> {
                    yield "was inconclusive";
                }
                case UNAVAILABLE -> {
                    yield "could not run";
                }
            };
            String message = "NXS IPv" + check.family() + " " + method(check.method()) + " connection check from " + check.region()
                + " " + result + ".";
            // An assisted check discovers a temporary peer each time; that is not a new operator-visible result.
            boolean unchanged = previous != null && previous.outcome() == check.outcome()
                && ("per_join".equals(check.method()) || Objects.equals(previous.target(), check.target()));
            if (unchanged) {
                logger.debug(message);
            } else if (check.outcome() == ConnectivityOutcome.ESTABLISHED) {
                logger.info(message);
            } else if (check.outcome() == ConnectivityOutcome.NOT_ESTABLISHED) {
                boolean assist = assisted && snapshot.assistedFamilies().contains(check.family());
                boolean offered = check.target() != null && endpoints(snapshot.profile().getAsJsonArray("candidates"), check.family()).contains(endpoint(check.target()));
                String impact = assist ? " Some players may still be able to connect."
                    : check.target() == null ? " Player addresses are unchanged."
                    : offered ? " Other regions can still reach this address."
                    : " This address is temporarily unavailable to players.";
                logger.warning(message + impact + " Check " + port + " in your firewall.");
            } else {
                logger.warning(message + " Player addresses are unchanged. Checks will retry automatically.");
            }
            logger.debug("NXS check details: region=" + check.region() + "; family=IPv" + check.family()
                + "; method=" + check.method() + "; target=" + (check.target() == null ? "unknown" : endpoint(check.target()))
                + "; result=" + check.outcome() + "; checkedAt=" + Instant.ofEpochMilli(check.checkedAt()) + ".");
        }
    }

    private static List<String> endpoints(JsonArray candidates, int family) {
        if (candidates == null) {
            return List.of();
        }
        return candidates.asList().stream().map(value -> value.getAsJsonObject()).filter(value ->
            value.has("address") && value.has("port") && (value.get("address").getAsString().contains(":") ? 6 : 4) == family)
            .map(value -> {
                String address = value.get("address").getAsString();
                return (family == 6 ? "[" + address + "]" : address) + ":" + value.get("port").getAsInt();
            }).sorted().toList();
    }

    private static String endpoint(InetSocketAddress target) {
        String address = target.getAddress().getHostAddress();
        return (address.contains(":") ? "[" + address + "]" : address) + ":" + target.getPort();
    }

    private static String method(String value) {
        return switch (value) {
            case "per_join" -> {
                yield "assisted";
            }
            case "warm_stun" -> {
                yield "automatically discovered";
            }
            default -> {
                yield "direct";
            }
        };
    }
}
