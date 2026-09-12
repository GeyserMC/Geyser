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

import org.cloudburstmc.netty.signalling.provider.ProviderRuntimeConfiguration;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Resolving the NXS settings Geyser hands to the provider client. Uses a neutral provider; only
 * {@link #externalSignallingDefaultsToWarden} is about Warden.
 */
class ProviderConfigurationTest {
    private static final String PROVIDER = "nxs:\n  endpoint: https://signal.example.net\n";

    private static GeyserConfig.SignallingConfig config(String yaml) throws IOException {
        return YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(yaml)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(GeyserConfig.SignallingConfig.class);
    }

    /** What {@code NetherNetServer} hands the resolver, so this covers that mapping too. */
    private static ProviderRuntimeConfiguration resolve(GeyserConfig.SignallingConfig config, Path dir,
                                                        String bindAddress, int udpPort, int capacity)
        throws IOException {
        var nxs = config.nxs();
        return ProviderRuntimeConfiguration.resolve(
            new ProviderRuntimeConfiguration.Settings(nxs.endpoint(), nxs.token(), nxs.advertiseAddresses(), nxs.data()),
            dir, bindAddress, udpPort, capacity, "Geyser");
    }

    private static ProviderRuntimeConfiguration runtime(Path dir, String yaml) throws IOException {
        return resolve(config(yaml), dir, "::", 20000, 40);
    }

    @Test
    void externalSignallingDefaultsToWarden(@TempDir Path dir) throws Exception {
        var config = config("{}");
        assertEquals(GeyserConfig.SignallingConfig.Mode.BUILTIN, config.mode());
        assertEquals("https://agent.warden.cloud", resolve(config, dir, "::", 20000, 40).origin().toString());
    }

    @Test
    void inheritsTheBedrockListener(@TempDir Path dir) throws Exception {
        var result = runtime(dir, PROVIDER);
        assertEquals("https://signal.example.net", result.origin().toString());
        assertEquals("::", result.bindAddress());
        assertEquals(20000, result.udpPort());
        assertEquals(40, result.capacity());
        assertEquals(dir.resolve("provider-state"), result.stateDirectory());
        assertEquals("automatic", result.clientConfiguration().registrationMode());
        assertEquals("anonymous-proof-of-work", result.clientConfiguration().authorizationScheme());
    }

    @Test
    void readsNxsSettingsWithoutLeakingTheToken(@TempDir Path dir) throws Exception {
        var result = runtime(dir, PROVIDER + "  token: yaml-secret\n"
            + "  data: {region: EU, pool: proxy, location: london}\n"
            + "  advertise-addresses: ['1.1.1.1:29133', '[2606:4700:4700::1111]:39133', '1.1.1.1:29133']\n");
        assertEquals("yaml-secret", result.authorizationToken());
        assertEquals("bearer-token", result.clientConfiguration().authorizationScheme());
        assertEquals("EU", result.region());
        assertEquals("proxy", result.pool());
        assertEquals(Map.of("location", "london"), result.tags());
        assertEquals(2, result.advertisedEndpoints().size());
        assertEquals(39133, result.advertisedEndpoints().get(1).getPort());
        assertFalse(result.toString().contains("yaml-secret"));
        assertFalse(result.clientConfiguration().toString().contains("yaml-secret"));
    }

    @Test
    void placementDefaultsWhenOnlyPartlyConfigured(@TempDir Path dir) throws Exception {
        var result = runtime(dir, PROVIDER + "  data:\n    region: EU\n    role: proxy\n");
        assertEquals("EU", result.region());
        assertEquals("default", result.pool());
        assertEquals(Map.of("role", "proxy"), result.tags());
        assertNull(result.authorizationToken());
    }

    @Test
    void readsTokenFilesWithoutLeakingContents(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("token"), "file-secret\n");
        for (String source : List.of("file:token", "./token", dir.resolve("token").toString())) {
            assertEquals("file-secret", runtime(dir, PROVIDER + "  token: '" + source + "'\n").authorizationToken());
        }
        for (String value : List.of("file:missing", "file:", "bad secret")) {
            var failure = assertThrows(IOException.class, () -> runtime(dir, PROVIDER + "  token: '" + value + "'\n"));
            assertFalse(failure.toString().contains("bad secret"));
        }
        Files.writeString(dir.resolve("token"), "\n");
        assertThrows(IOException.class, () -> runtime(dir, PROVIDER + "  token: file:token\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://signal.example.net", "signal.example.net", "https://signal.example.net/path", "https://user@signal.example.net"})
    void rejectsInsecureOrInvalidProviders(String endpoint, @TempDir Path dir) {
        assertThrows(IOException.class, () -> runtime(dir, "nxs:\n  endpoint: '" + endpoint + "'\n"));
    }

    @Test
    void allowsPlainHttpProviderOnLoopback(@TempDir Path dir) throws Exception {
        assertEquals("http://127.0.0.1:8080", runtime(dir, "nxs:\n  endpoint: http://127.0.0.1:8080\n").origin().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com:19133", "::1:19133", "[::]:19133", "1.1.1.1:0", "1.1.1.1:65536", "224.0.0.1:19133", "1.1.1.1:1.5", "[fe80::1]:19133"})
    void rejectsInvalidAdvertisedEndpoints(String endpoint, @TempDir Path dir) {
        assertThrows(IOException.class, () -> runtime(dir, PROVIDER + "  advertise-addresses: ['" + endpoint + "']\n"));
    }

    @Test
    void rejectsInvalidModesAndPorts(@TempDir Path dir) throws Exception {
        assertThrows(IOException.class, () -> config("mode: invalid\n"));
        // The provider endpoint needs a fixed port; 0 would be ephemeral
        assertThrows(IOException.class, () -> resolve(config(PROVIDER), dir, "0.0.0.0", 0, 20));
        assertEquals(65535, resolve(config(PROVIDER), dir, "0.0.0.0", 65535, 20).udpPort());
    }
}
