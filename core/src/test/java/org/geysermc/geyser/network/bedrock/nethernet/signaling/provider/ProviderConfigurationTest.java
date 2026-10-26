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

import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.network.bedrock.nethernet.NetherNetServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What Geyser's configuration binds to and hands the resolver. How the resolver treats those values
 * is covered by the transport library, which owns it.
 */
class ProviderConfigurationTest {

    private static <T> T load(String yaml, Class<T> type) throws IOException {
        return YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(yaml)))
            .defaultOptions(InterfaceDefaultOptions::addTo)
            .build().load().get(type);
    }

    private static GeyserConfig.SignalingConfig config(String yaml) throws IOException {
        return load(yaml, GeyserConfig.SignalingConfig.class);
    }

    private static GeyserConfig.AdvancedNxsConfig advanced(String yaml) throws IOException {
        return load(yaml, GeyserConfig.AdvancedNxsConfig.class);
    }

    private static ProviderRuntimeConfiguration.Settings settings(GeyserConfig.SignalingConfig config,
                                                                  GeyserConfig.AdvancedNxsConfig advanced) throws IOException {
        return NetherNetServer.providerSettings(config, config.advertiseAddresses(), advanced);
    }

    private static ProviderRuntimeConfiguration resolve(GeyserConfig.SignalingConfig config,
                                                        GeyserConfig.AdvancedNxsConfig advanced, Path dir) throws IOException {
        return ProviderRuntimeConfiguration.resolve(settings(config, advanced), dir, "::", 20000, 40);
    }

    private static ProviderRuntimeConfiguration resolve(GeyserConfig.SignalingConfig config, Path dir) throws IOException {
        return resolve(config, advanced("{}"), dir);
    }

    @Test
    void signalingIsBuiltinAndPointsAtWardenUntilConfigured(@TempDir Path dir) throws Exception {
        var config = config("{}");

        assertEquals(GeyserConfig.SignalingConfig.Mode.BUILTIN, config.mode());
        assertEquals("https://agent.warden.cloud", resolve(config, dir).origin().toString());
    }

    @Test
    void bindsEveryConfiguredNxsSetting(@TempDir Path dir) throws Exception {
        var config = config("""
            advertise-addresses: ['1.1.1.1:29133']
            nxs:
              endpoint: https://signal.example.net
              token: yaml-secret
            """);
        var settings = settings(config, advanced("{}"));
        var result = resolve(config, dir);

        assertEquals("https://signal.example.net", result.origin().toString());
        assertEquals("yaml-secret", result.clientConfiguration().authorizationToken());
        assertEquals(List.of("1.1.1.1:29133"), settings.advertiseAddresses());
        assertEquals("defined", result.clientConfiguration().connectivityMethod());
        assertTrue(result.clientConfiguration().gameOutcomes());
    }

    @Test
    void defaultsCheckConnectivityWithoutAssistance(@TempDir Path dir) throws Exception {
        var settings = settings(config("{}"), advanced("{}"));
        var client = resolve(config("{}"), dir).clientConfiguration();

        assertEquals(ProviderClient.ControlTransport.HTTP, client.controlTransport());
        assertFalse(client.assistedJoins());
        assertTrue(client.diagnosticAdmission());
        assertTrue(settings.stunDiscovery());
        assertFalse(client.deregisterOnShutdown());
        assertNull(settings.label(), "The provider names the instance after the host");
    }

    @Test
    void assistedJoinsUseTheWebSocket(@TempDir Path dir) throws Exception {
        var client = resolve(config("{}"), advanced("assisted-joins: true\n"), dir).clientConfiguration();

        assertEquals(ProviderClient.ControlTransport.AUTO, client.controlTransport());
        assertTrue(client.assistedJoins());
    }

    @Test
    void passesTheOptOuts(@TempDir Path dir) throws Exception {
        var config = config("stun-discovery: false\n");
        var advanced = advanced("""
            connection-checks: false
            deregister-on-shutdown: true
            """);
        var settings = settings(config, advanced);
        var client = resolve(config, advanced, dir).clientConfiguration();

        assertFalse(client.diagnosticAdmission());
        assertFalse(settings.stunDiscovery());
        assertTrue(client.deregisterOnShutdown());
    }

    @Test
    void takesTheStunDiscoveryOverride() throws Exception {
        // As -DgeyserStunDiscovery sets it
        var config = config("{}");
        config.stunDiscovery(false);

        assertFalse(settings(config, advanced("{}")).stunDiscovery());
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
        assertEquals("discovered", result.clientConfiguration().connectivityMethod());
    }

    @Test
    void passesTheDeclaredHostLocation() throws Exception {
        var settings = settings(config("{}"), advanced("location: {country: nl, city: Amsterdam, latitude: 52.37, longitude: 4.89}\n"));

        assertEquals(new ProviderClient.Location("NL", "Amsterdam", 52.37, 4.89), settings.location());
        assertEquals(new ProviderClient.Location("GB", null, null, null),
            settings(config("{}"), advanced("location: {country: ' gb '}\n")).location());
        assertNull(settings(config("{}"), advanced("{}")).location(),
            "Without a location the provider estimates it");
    }

    @Test
    void refusesInvalidHostLocation() {
        for (String location : List.of("{country: ZZZ}", "{country: T1}", "{city: London}", "{latitude: 0}", "{longitude: 0}",
            "{latitude: 91, longitude: 0}", "{latitude: 0, longitude: 181}", "{latitude: NaN, longitude: 0}",
            "{country: NL, extra: value}")) {
            assertThrows(IOException.class, () -> settings(config("{}"), advanced("location: " + location + "\n")), location);
        }
    }

    @Test
    void theOperatorLinkGoesToAFileOnlyThisUserReads(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("operator-link.txt");
        var action = new ProviderClient.OperatorAction(URI.create("https://panel.example.net/claim/secret"), "Claim it.",
            1_800_000_000_000L);
        String message = NetherNetServer.storeOperatorAction(file, Optional.of(action));
        assertEquals("Claim it. The link is in " + file + ".", message);
        assertEquals("Claim it.\nhttps://panel.example.net/claim/secret\nExpires 2027-01-15T08:00:00Z\n", Files.readString(file));
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
        }
        // Once the service offers none, as after a claim, the old link goes
        assertNull(NetherNetServer.storeOperatorAction(file, Optional.empty()));
        assertFalse(Files.exists(file));
    }
}
