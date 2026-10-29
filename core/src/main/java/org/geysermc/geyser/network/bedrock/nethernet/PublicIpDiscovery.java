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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserLogger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds this machine's public IPv4 address by asking web APIs, for hosts whose public address cannot be learned
 * through STUN (blocked outbound UDP being the usual reason). Several APIs are rotated through, so one service being
 * down, rate limited or blocked does not stop discovery: the endpoints are tried in turn, each with its own short
 * timeout, until one returns a usable address.
 * <p>
 * The answer is cached in a file next to the NetherNet identity, so restarting a server does not ask the APIs again,
 * which also keeps the server inside their rate limits. The cache is used for {@link #DEFAULT_CACHE_TTL} unless
 * {@code -DgeyserIpDiscoveryCacheMinutes} says otherwise, and is still used when the APIs cannot be reached but have
 * answered before, as a stale address beats no address.
 * <p>
 * Only a public IPv4 address is accepted. A service answering with a private, loopback, link-local or IPv6 address is
 * skipped, as advertising one of those is exactly what makes Bedrock clients time out on NetherNet.
 */
public final class PublicIpDiscovery {
    /**
     * The first IPv4 address in a response body: the plain text endpoints return just the address, while the others
     * embed it ({@code ip=...} in Cloudflare's trace, {@code "ip":"..."} in JSON).
     */
    private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    /**
     * Asked in rotation unless {@code -DgeyserIpDiscoveryEndpoints} says otherwise. A mix of services and response
     * formats, as either may be blocked where a server runs.
     */
    private static final List<String> DEFAULT_ENDPOINTS = List.of(
        "https://api.ipify.org",
        "https://checkip.amazonaws.com",
        "https://1.1.1.1/cdn-cgi/trace",
        "https://ipv4.icanhazip.com",
        "https://ipinfo.io/ip");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    /**
     * How long discovery may take in total. A server with no internet access reaches every endpoint's timeout, and
     * startup should not wait for all of them.
     */
    private static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(6);
    /**
     * How long a cached address is trusted: public addresses rarely change, and asking on every boot only spends the
     * APIs' rate limits.
     */
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(6);

    /**
     * Picks the endpoint this call starts at, so repeated starts do not all depend on the same service.
     */
    private static final AtomicInteger NEXT_ENDPOINT = new AtomicInteger();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private PublicIpDiscovery() {
    }

    /**
     * The cached answer, as long as it is still trusted.
     *
     * @param address the public IPv4 address, e.g. {@code 203.0.113.7}
     * @param storedAt when the APIs were last asked for it
     */
    record CacheEntry(String address, Instant storedAt) {
    }

    /**
     * @return this machine's public IPv4 address, from the cache when it is fresh and from the APIs otherwise, or
     *         empty if no endpoint returned a usable one and nothing was cached
     */
    public static Optional<String> discover(GeyserLogger logger, Path cacheFile) {
        Duration ttl = cacheTtl();
        Optional<CacheEntry> cached = ttl.isZero() ? Optional.empty() : readCache(cacheFile);
        if (cached.isPresent() && isFresh(cached.get(), ttl)) {
            logger.info("ip-discovery is using the cached public address " + cached.get().address() + ", saved "
                + age(cached.get().storedAt()) + " ago. Delete " + cacheFile + " to look it up again now.");
            return Optional.of(cached.get().address());
        }

        Optional<String> found = queryEndpoints();
        if (found.isPresent()) {
            if (!ttl.isZero()) {
                writeCache(logger, cacheFile, found.get());
            }
            return found;
        }

        if (cached.isPresent()) {
            logger.warning("ip-discovery could not refresh this server's public address, so the cached one from "
                + age(cached.get().storedAt()) + " ago is used: " + cached.get().address());
            return Optional.of(cached.get().address());
        }
        return Optional.empty();
    }

    private static Optional<String> queryEndpoints() {
        List<String> endpoints = endpoints();
        int start = Math.floorMod(NEXT_ENDPOINT.getAndIncrement(), endpoints.size());
        long deadline = System.nanoTime() + TOTAL_TIMEOUT.toNanos();
        for (int i = 0; i < endpoints.size(); i++) {
            if (System.nanoTime() >= deadline) {
                return Optional.empty();
            }
            String endpoint = endpoints.get((start + i) % endpoints.size());
            Optional<Inet4Address> address = query(endpoint);
            if (address.isPresent()) {
                return Optional.of(address.get().getHostAddress());
            }
        }
        return Optional.empty();
    }

    /**
     * The endpoints asked for this machine's public address: those in {@code -DgeyserIpDiscoveryEndpoints}, comma
     * separated, else {@link #DEFAULT_ENDPOINTS}. Set it empty to ask none.
     */
    private static List<String> endpoints() {
        String property = System.getProperty("geyserIpDiscoveryEndpoints");
        if (property == null) {
            return DEFAULT_ENDPOINTS;
        }
        List<String> endpoints = Arrays.stream(property.split(",")).map(String::trim)
            .filter(endpoint -> !endpoint.isEmpty())
            .toList();
        return endpoints.isEmpty() ? DEFAULT_ENDPOINTS : endpoints;
    }

    /**
     * How long a cached address is used: {@code -DgeyserIpDiscoveryCacheMinutes} minutes, or
     * {@link #DEFAULT_CACHE_TTL}. Zero or less looks the address up on every start.
     */
    private static Duration cacheTtl() {
        String property = System.getProperty("geyserIpDiscoveryCacheMinutes");
        if (property == null) {
            return DEFAULT_CACHE_TTL;
        }
        try {
            long minutes = Long.parseLong(property.trim());
            return minutes <= 0 ? Duration.ZERO : Duration.ofMinutes(minutes);
        } catch (NumberFormatException e) {
            return DEFAULT_CACHE_TTL;
        }
    }

    private static Optional<Inet4Address> query(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Geyser (ip-discovery)")
                .header("Accept", "text/plain, application/json")
                .GET()
                .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return parse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            // Timeouts, DNS failures, blocked endpoints: try the next one
            return Optional.empty();
        }
    }

    /**
     * @return the address in {@code cacheFile}, or empty if the file is missing, unreadable, or holds something that
     *         may not be advertised
     */
    static Optional<CacheEntry> readCache(Path cacheFile) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(cacheFile)).getAsJsonObject();
            String address = json.get("address").getAsString();
            Instant storedAt = Instant.ofEpochMilli(json.get("stored-at").getAsLong());
            // A file from another machine, or written while a VPN was up, must not be advertised
            return parse(address).map(ipv4 -> new CacheEntry(ipv4.getHostAddress(), storedAt));
        } catch (Exception e) {
            // Missing, unreadable or not ours: ask the APIs instead
            return Optional.empty();
        }
    }

    static void writeCache(GeyserLogger logger, Path cacheFile, String address) {
        JsonObject json = new JsonObject();
        json.addProperty("address", address);
        json.addProperty("stored-at", Instant.now().toEpochMilli());
        Path temporary = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(cacheFile.toAbsolutePath().getParent());
            Files.writeString(temporary, json.toString());
            // Replace in one step, so a stop midway cannot leave half a file behind
            Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.warning("ip-discovery could not save this server's public address to " + cacheFile + ": " + e.getMessage());
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Nothing more to do about it
            }
        }
    }

    static boolean isFresh(CacheEntry entry, Duration ttl) {
        return Duration.between(entry.storedAt(), Instant.now()).compareTo(ttl) < 0;
    }

    private static String age(Instant storedAt) {
        Duration age = Duration.between(storedAt, Instant.now());
        if (age.isNegative()) {
            // The clock moved backwards since the file was written
            return "0 minutes";
        }
        long minutes = age.toMinutes();
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        long hours = age.toHours();
        return hours + (hours == 1 ? " hour" : " hours");
    }

    /**
     * @return the first public IPv4 address in {@code body}, or empty if there is none
     */
    static Optional<Inet4Address> parse(String body) {
        Matcher matcher = IPV4.matcher(body == null ? "" : body);
        while (matcher.find()) {
            // Parsed by hand rather than through InetAddress.getByName, which would resolve anything else as a host name
            byte[] bytes = toBytes(matcher.group());
            if (bytes == null) {
                continue;
            }
            try {
                InetAddress address = InetAddress.getByAddress(bytes);
                if (address instanceof Inet4Address ipv4 && isPublic(ipv4)) {
                    return Optional.of(ipv4);
                }
            } catch (UnknownHostException ignored) {
                // Cannot happen for four bytes, but there is another endpoint to ask
            }
        }
        return Optional.empty();
    }

    private static byte @Nullable [] toBytes(String literal) {
        String[] parts = literal.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            int octet;
            try {
                octet = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (octet < 0 || octet > 255) {
                return null;
            }
            bytes[i] = (byte) octet;
        }
        return bytes;
    }

    private static boolean isPublic(Inet4Address address) {
        return !address.isAnyLocalAddress() && !address.isLoopbackAddress() && !address.isLinkLocalAddress()
            && !address.isSiteLocalAddress() && !address.isMulticastAddress();
    }
}
