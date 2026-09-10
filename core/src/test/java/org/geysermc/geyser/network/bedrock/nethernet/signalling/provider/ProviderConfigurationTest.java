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
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProviderConfigurationTest {
    private static GeyserConfig.SignallingConfig config(String yaml) throws IOException {
        return YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(yaml)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(GeyserConfig.SignallingConfig.class);
    }
    private static ProviderRuntimeConfiguration runtime(Path dir, String yaml) throws IOException {
        return ProviderRuntimeConfiguration.resolve(config(yaml), dir, "::", 20000, 40);
    }

    @Test void freshConfigDefaultsToInbuiltAndInheritsGeyser(@TempDir Path dir) throws Exception {
        var config = config("{}");
        assertEquals(GeyserConfig.SignallingConfig.Mode.INBUILT, config.mode());
        var result = ProviderRuntimeConfiguration.resolve(config, dir, "::", 20000, 40);
        assertEquals("https://agent.warden.cloud", result.origin().toString());
        assertEquals("::", result.bindAddress()); assertEquals(20001, result.udpPort());
        assertEquals(40, result.capacity()); assertEquals(dir.resolve("provider-state"), result.stateDirectory());
        assertEquals("automatic", result.clientConfiguration().registrationMode());
        assertEquals("anonymous-proof-of-work", result.clientConfiguration().authorizationScheme());
    }
    @ParameterizedTest @ValueSource(strings = {"inbuilt", "nxs", "hybrid", "none"})
    void supportsEveryMode(String mode) throws Exception {
        assertEquals(mode, config("mode: " + mode + "\n").mode().name().toLowerCase(Locale.ROOT));
    }
    @Test void readsNxsSettingsWithoutLeakingTheToken(@TempDir Path dir) throws Exception {
        var result = runtime(dir, "nxs:\n  token: yaml-secret\n  endpoint: https://signal.example.net\n"
            + "  data: {region: EU, pool: proxy, location: london}\n"
            + "  advertise-addresses: ['1.1.1.1:29133', '[2606:4700:4700::1111]:39133', '1.1.1.1:29133']\n");
        assertEquals("yaml-secret", result.authorizationToken());
        assertEquals("https://signal.example.net", result.origin().toString());
        assertEquals("EU", result.region()); assertEquals("proxy", result.pool()); assertEquals(Map.of("location", "london"), result.tags());
        assertEquals(2, result.advertisedEndpoints().size()); assertEquals(39133, result.advertisedEndpoints().get(1).getPort());
        assertFalse(result.toString().contains("yaml-secret")); assertFalse(result.clientConfiguration().toString().contains("yaml-secret"));
    }
    @Test void arbitraryMetadataAndRegionOnlyDoNotRequireExtraOptions(@TempDir Path dir) throws Exception {
        var result = runtime(dir, "nxs:\n  data:\n    region: EU\n    role: proxy\n");
        assertEquals("EU", result.region()); assertEquals("default", result.pool()); assertEquals(Map.of("role", "proxy"), result.tags());
        assertNull(result.authorizationToken());
    }
    @Test void readsTokenFilesAndFailsClosedWithoutLeakingContents(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("token"), "file-secret\n");
        for (String source : List.of("file:token", "./token", dir.resolve("token").toString())) {
            var result = runtime(dir, "nxs:\n  token: '" + source + "'\n");
            assertEquals("file-secret", result.authorizationToken());
        }
        for (String value : List.of("file:missing", "file:", "bad secret")) {
            var failure = assertThrows(IOException.class, () -> runtime(dir, "nxs:\n  token: '" + value + "'\n"));
            assertFalse(failure.toString().contains("bad secret"));
        }
        Files.writeString(dir.resolve("token"), "\n");
        assertThrows(IOException.class, () -> runtime(dir, "nxs:\n  token: file:token\n"));
    }
    @ParameterizedTest @ValueSource(strings = {"example.com:19133", "::1:19133", "[::]:19133", "1.1.1.1:0", "1.1.1.1:65536", "224.0.0.1:19133", "1.1.1.1:1.5", "[fe80::1]:19133"})
    void rejectsInvalidEndpoints(String endpoint, @TempDir Path dir) {
        assertThrows(IOException.class, () -> runtime(dir, "nxs:\n  advertise-addresses: ['" + endpoint + "']\n"));
    }
    @Test void rejectsInvalidModesAndPorts(@TempDir Path dir) throws Exception {
        assertThrows(IOException.class, () -> config("mode: invalid\n"));
        var config = config("{}");
        assertThrows(IOException.class, () -> ProviderRuntimeConfiguration.resolve(config, dir, "0.0.0.0", 65535, 20));
    }
}
