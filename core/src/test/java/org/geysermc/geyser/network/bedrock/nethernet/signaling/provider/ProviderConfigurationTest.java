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
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.provider.NativeProviderHostFactory;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Geyser's configuration binds to and hands the resolver. How the resolver treats those values
 * is covered by the transport library, which owns it.
 */
class ProviderConfigurationTest {

    private static GeyserConfig.SignalingConfig config(String yaml) throws IOException {
        return YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(yaml)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(GeyserConfig.SignalingConfig.class);
    }

    private static ProviderRuntimeConfiguration resolve(GeyserConfig.SignalingConfig config, Path dir)
        throws IOException {
        var nxs = config.nxs();
        return ProviderRuntimeConfiguration.resolve(
            new ProviderRuntimeConfiguration.Settings(nxs.endpoint(), nxs.token(), nxs.advertiseAddresses(), nxs.data(),
                nxs.effectiveControlTransport(), nxs.diagnosticAdmission(), nxs.maintainedCandidates(), nxs.assistedJoins()),
            dir, "::", 20000, 40, "Geyser");
    }

    @Test
    void signalingIsBuiltinAndPointsAtWardenUntilConfigured(@TempDir Path dir) throws Exception {
        var config = config("{}");

        assertEquals(GeyserConfig.SignalingConfig.Mode.BUILTIN, config.mode());
        assertEquals("https://agent.warden.cloud", resolve(config, dir).origin().toString());
    }

    @Test
    void bindsEveryConfiguredNxsSetting(@TempDir Path dir) throws Exception {
        var result = resolve(config("""
            nxs:
              endpoint: https://signal.example.net
              token: yaml-secret
              data: {region: EU, pool: proxy, location: london}
              advertise-addresses: ['1.1.1.1:29133']
            """), dir);

        assertEquals("https://signal.example.net", result.origin().toString());
        assertEquals("yaml-secret", result.authorizationToken());
        assertEquals("EU", result.region());
        assertEquals("proxy", result.pool());
        assertEquals(Map.of("location", "london"), result.tags());
        assertEquals(1, result.advertisedEndpoints().size());
        assertEquals("Geyser", result.label());
    }

    @Test
    void refusesAModeItCannotServe() {
        assertThrows(IOException.class, () -> config("mode: invalid\n"));
    }

    @Test
    void carriesTheListenerItWasGiven(@TempDir Path dir) throws Exception {
        var result = resolve(config("nxs:\n  endpoint: https://signal.example.net\n"), dir);

        assertEquals("::", result.bindAddress());
        assertEquals(20000, result.udpPort());
        assertEquals(List.of(), result.advertisedEndpoints());
    }

    @Test
    void defaultsEnableMaintainedCandidatesAndAuthenticatedChecks(@TempDir Path dir) throws Exception {
        var result = resolve(config("{}"), dir);
        assertEquals(ProviderClient.ControlTransport.HTTP, result.controlTransport());
        assertFalse(result.clientConfiguration().assistedJoins());
        assertEquals("false", result.nativeHostOptions().get("assistedJoins"));
        assertTrue(result.maintainedCandidates());
        assertTrue(result.diagnosticAdmission());
        assertFalse(result.nativeHostOptions().containsKey("stunServers"));
        assertEquals(NativeProviderHostFactory.EXPLICIT_OR_PUBLIC_LOCAL, result.nativeHostOptions().get("endpointPolicy"));
        assertEquals(NativeProviderHostFactory.MAINTAINED_V1, result.nativeHostOptions().get("candidatePublication"));
    }

    @Test
    void enablingAssistedJoinsAutomaticallySelectsAutoTransport(@TempDir Path dir) throws Exception {
        var result = resolve(config("nxs:\n  assisted-joins: true\n"), dir);

        assertEquals(ProviderClient.ControlTransport.AUTO, result.clientConfiguration().controlTransport());
        assertTrue(result.clientConfiguration().assistedJoins());
        assertEquals("true", result.nativeHostOptions().get("assistedJoins"));
    }

    @Test
    void assistedJoinsOverrideTheHttpSettingWrittenToExistingConfigs(@TempDir Path dir) throws Exception {
        var config = config("""
            nxs:
              control-transport: http
              assisted-joins: true
            """);

        assertEquals(ProviderClient.ControlTransport.HTTP, config.nxs().controlTransport());
        var result = resolve(config, dir);
        assertEquals(ProviderClient.ControlTransport.AUTO, result.controlTransport());
        assertTrue(result.clientConfiguration().assistedJoins());
    }

    @Test
    void autoTransportCanBeSelectedWithoutEnablingAssistedJoins(@TempDir Path dir) throws Exception {
        var result = resolve(config("nxs:\n  control-transport: auto\n"), dir);

        assertEquals(ProviderClient.ControlTransport.AUTO, result.controlTransport());
        assertFalse(result.clientConfiguration().assistedJoins());
    }

    @Test
    void passesOptInsToNativeAndClientSettings(@TempDir Path dir) throws Exception {
        var result = resolve(config("""
            nxs:
              control-transport: auto
              assisted-joins: true
              maintained-candidates: true
              diagnostic-admission: true
            """), dir);
        assertEquals(ProviderClient.ControlTransport.AUTO, result.clientConfiguration().controlTransport());
        assertTrue(result.clientConfiguration().assistedJoins());
        assertEquals("true", result.nativeHostOptions().get("assistedJoins"));
        assertTrue(result.clientConfiguration().diagnosticAdmission());
        assertEquals("discovered", result.clientConfiguration().connectivityMethod());
        var options = result.nativeHostOptions();
        assertEquals(NativeProviderHostFactory.MAINTAINED_V1, options.get("candidatePublication"));
        assertEquals("true", options.get("diagnosticAdmission"));
        assertFalse(options.containsKey("stunServers"));
        assertEquals(dir.resolve("provider-state").toString(), options.get("stateDirectory"));
    }

    @Test
    void explicitAddressesArePassedToTheHostPolicy(@TempDir Path dir) throws Exception {
        var result = resolve(config("""
            nxs:
              advertise-addresses: ['8.8.8.8:19133']
              maintained-candidates: true
            """), dir);
        assertFalse(result.nativeHostOptions().containsKey("stunServers"));
        assertEquals("defined", result.clientConfiguration().connectivityMethod());
        assertEquals(1, JsonParser.parseString(result.nativeHostOptions().get("advertisedEndpoints")).getAsJsonArray().size());
    }

    @Test
    void refusesMalformedSettingsAndAllowsMaintainedCandidateOptOut(@TempDir Path dir) throws Exception {
        assertThrows(IOException.class, () -> config("nxs:\n  control-transport: websocket-only\n"));
        var optedOut = resolve(config("nxs:\n  maintained-candidates: false\n"), dir);
        assertFalse(optedOut.maintainedCandidates());
        assertEquals(NativeProviderHostFactory.MAINTAINED_V1, optedOut.nativeHostOptions().get("candidatePublication"));
        assertEquals("false", optedOut.nativeHostOptions().get("stunWarming"));
    }
}
