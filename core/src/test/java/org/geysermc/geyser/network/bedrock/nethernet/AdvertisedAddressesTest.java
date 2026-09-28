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

import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPServerSignaling;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.configuration.GeyserRemoteConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvertisedAddressesTest {

    private static GeyserConfig config(String yaml) throws IOException {
        return YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(yaml)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(GeyserRemoteConfig.class);
    }

    @Test
    void defaultsToAutomaticSelection() throws IOException {
        var config = config("{}");

        assertEquals(List.of(), config.advanced().bedrock().advertiseAddresses());
        assertEquals(Set.of(), NetherNetServer.advertisedAddresses(config, ""));
    }

    @Test
    void keepsConfiguredPortsWithoutChangingTheListener() throws IOException {
        var config = config("""
            bedrock:
              address: 10.0.0.2
              port: 19132
              webrtc-port: 19133
            advanced:
              bedrock:
                advertise-addresses:
                  - "203.0.113.10:56789"
                  - "203.0.113.10:46565"
                  - "[2001:db8::1]:56789"
                  - "203.0.113.10:56789"
            """);
        Set<String> addresses = NetherNetServer.advertisedAddresses(config, "");

        assertEquals(Set.of("203.0.113.10:56789", "203.0.113.10:46565", "[2001:db8::1]:56789"), addresses);
        assertDoesNotThrow(() -> new NetherNetHTTPServerSignaling.Builder().setAdvertisedAddresses(addresses));
        assertEquals("10.0.0.2", config.bedrock().address());
        assertEquals(19132, config.bedrock().port());
        assertEquals(19133, config.bedrock().webrtcPort());
    }

    @Test
    void systemPropertyOverridesTheConfig() throws IOException {
        var config = config("""
            advanced:
              bedrock:
                advertise-addresses: ["203.0.113.10:56789"]
            """);

        assertEquals(Set.of("198.51.100.1:46565", "[2001:db8::2]:12345"),
            NetherNetServer.advertisedAddresses(config, " 198.51.100.1:46565, , [2001:db8::2]:12345, "));
        assertEquals(Set.of("203.0.113.10:56789"), NetherNetServer.advertisedAddresses(config, " , "));
    }

    @Test
    void acceptsBareAddresses() throws IOException {
        var config = config("""
            advanced:
              bedrock:
                advertise-addresses: ["203.0.113.10", "2001:db8::1"]
            """);
        Set<String> addresses = NetherNetServer.advertisedAddresses(config, "");

        assertEquals(Set.of("203.0.113.10", "2001:db8::1"), addresses);
        assertDoesNotThrow(() -> new NetherNetHTTPServerSignaling.Builder().setAdvertisedAddresses(addresses));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0.2", "2001:db8::1"})
    void emptyListFallsBackToTheBoundAddress(String address) throws IOException {
        var config = config("bedrock:\n  address: \"" + address + "\"\n");

        assertEquals(Set.of(address), NetherNetServer.advertisedAddresses(config, ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "::", "not-an-ip"})
    void doesNotAdvertiseWildcardOrNonNumericBindAddresses(String address) throws IOException {
        var config = config("bedrock:\n  address: \"" + address + "\"\n");

        assertEquals(Set.of(), NetherNetServer.advertisedAddresses(config, ""));
    }
}
