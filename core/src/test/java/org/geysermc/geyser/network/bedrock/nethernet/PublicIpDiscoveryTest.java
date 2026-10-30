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

import org.geysermc.geyser.scoreboard.network.util.EmptyGeyserLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublicIpDiscoveryTest {
    /**
     * Refused immediately, so a test that reaches the APIs fails fast instead of waiting on the real ones.
     */
    private static final String UNREACHABLE_ENDPOINT = "http://127.0.0.1:1";

    @TempDir
    Path tempDirectory;

    @ParameterizedTest
    @ValueSource(strings = {
        // Plain text endpoints
        "203.0.113.7",
        "203.0.113.7\n",
        "  198.51.100.23  ",
        // Cloudflare's cdn-cgi/trace
        "fl=123abc\nh=1.1.1.1\nip=198.51.100.24\nts=1.0\nvisit_scheme=https\n",
        // JSON
        "{\"ip\":\"192.0.2.44\"}",
        "{\"ip\":\"192.0.2.44\",\"country\":\"ID\"}"
    })
    void findsPublicIpv4(String body) {
        Optional<java.net.Inet4Address> address = PublicIpDiscovery.parse(body);
        assertEquals(true, address.isPresent(), "expected an address in: " + body);
    }

    @Test
    void keepsOnlyTheFirstAddress() {
        assertEquals("192.0.2.44", PublicIpDiscovery.parse("ip=::1 ip=192.0.2.44").orElseThrow().getHostAddress());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "not an ip",
        // IPv6 only
        "2001:db8::1",
        // Never advertise where clients cannot reach
        "127.0.0.1",
        "10.0.0.5",
        "172.16.4.2",
        "192.168.1.10",
        "169.254.1.1",
        "0.0.0.0",
        "224.0.0.1",
        // Invalid octets
        "999.1.1.1"
    })
    void rejectsUnusableAddresses(String body) {
        assertEquals(Optional.empty(), PublicIpDiscovery.parse(body));
    }

    @Test
    void cachesTheAnswer() {
        PublicIpDiscovery.writeCache(new EmptyGeyserLogger(), cacheFile(), "203.0.113.7");

        PublicIpDiscovery.CacheEntry entry = PublicIpDiscovery.readCache(cacheFile()).orElseThrow();
        assertEquals("203.0.113.7", entry.address());
        assertTrue(PublicIpDiscovery.isFresh(entry, Duration.ofHours(6)));
    }

    @Test
    void cacheExpiresAfterTheTtl() {
        writeRawCache(Instant.now().minus(Duration.ofHours(7)));

        PublicIpDiscovery.CacheEntry entry = PublicIpDiscovery.readCache(cacheFile()).orElseThrow();
        assertFalse(PublicIpDiscovery.isFresh(entry, Duration.ofHours(6)));
    }

    @Test
    void ignoresACacheItDidNotWrite() throws IOException {
        Files.createDirectories(cacheFile().getParent());
        Files.writeString(cacheFile(), "203.0.113.7");

        assertEquals(Optional.empty(), PublicIpDiscovery.readCache(cacheFile()));
    }

    @Test
    void ignoresACachedAddressThatMustNotBeAdvertised() throws IOException {
        Files.createDirectories(cacheFile().getParent());
        Files.writeString(cacheFile(), "{\"address\":\"192.168.1.5\",\"stored-at\":" + Instant.now().toEpochMilli() + "}");

        assertEquals(Optional.empty(), PublicIpDiscovery.readCache(cacheFile()));
    }

    @Test
    void usesAFreshCacheWithoutAskingTheApis() {
        PublicIpDiscovery.writeCache(new EmptyGeyserLogger(), cacheFile(), "203.0.113.9");

        assertEquals("203.0.113.9", withEndpoints(UNREACHABLE_ENDPOINT, () ->
            PublicIpDiscovery.discover(new EmptyGeyserLogger(), cacheFile())).orElseThrow());
    }

    @Test
    void usesAStaleCacheWhenTheApisCannotBeReached() {
        writeRawCache(Instant.now().minus(Duration.ofHours(7)));

        assertEquals("203.0.113.7", withEndpoints(UNREACHABLE_ENDPOINT, () ->
            PublicIpDiscovery.discover(new EmptyGeyserLogger(), cacheFile())).orElseThrow());
    }

    @Test
    void disappearsWithoutACacheAndWithoutTheApis() {
        assertEquals(Optional.empty(), withEndpoints(UNREACHABLE_ENDPOINT, () ->
            PublicIpDiscovery.discover(new EmptyGeyserLogger(), cacheFile())));
    }

    private Path cacheFile() {
        return tempDirectory.resolve("nethernet/public-ip.json");
    }

    private void writeRawCache(Instant storedAt) {
        try {
            Files.createDirectories(cacheFile().getParent());
            Files.writeString(cacheFile(),
                "{\"address\":\"203.0.113.7\",\"stored-at\":" + storedAt.toEpochMilli() + "}");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static <T> T withEndpoints(String endpoints, java.util.function.Supplier<T> action) {
        String previous = System.setProperty("geyserIpDiscoveryEndpoints", endpoints);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                System.clearProperty("geyserIpDiscoveryEndpoints");
            } else {
                System.setProperty("geyserIpDiscoveryEndpoints", previous);
            }
        }
    }
}
