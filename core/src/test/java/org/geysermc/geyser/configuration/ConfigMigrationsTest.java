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

package org.geysermc.geyser.configuration;

import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Version 12 folds use-haproxy-protocol into its list, which turns HAProxy protocol on by naming proxies.
 */
class ConfigMigrationsTest {
    private final GeyserLogger logger = mock(GeyserLogger.class);
    private final GeyserBootstrap bootstrap = mock(GeyserBootstrap.class);

    @BeforeEach
    void setUp() {
        when(bootstrap.getGeyserLogger()).thenReturn(logger);
    }

    private ConfigurationNode bedrock(String haproxy) throws Exception {
        String yaml = "advanced:\n  bedrock:\n" + haproxy + "config-version: 11\n";
        ConfigurationNode root = YamlConfigurationLoader.builder()
            .source(() -> new BufferedReader(new StringReader(yaml)))
            .build()
            .load();
        ConfigMigrations.TRANSFORMER.apply(GeyserPluginConfig.class, bootstrap).apply(root);
        return root.node("advanced", "bedrock");
    }

    private static List<String> proxies(ConfigurationNode bedrock) throws Exception {
        return bedrock.node("haproxy-protocol-whitelisted-ips").getList(String.class, List.of());
    }

    @Test
    void offEmptiesTheProxiesItIgnored() throws Exception {
        ConfigurationNode bedrock = bedrock("    use-haproxy-protocol: false\n    haproxy-protocol-whitelisted-ips:\n    - 10.0.0.0/8\n");
        assertTrue(bedrock.node("use-haproxy-protocol").virtual());
        assertEquals(List.of(), proxies(bedrock));
        verify(logger, never()).warning(anyString());
    }

    @Test
    void onKeepsItsProxies() throws Exception {
        ConfigurationNode bedrock = bedrock("    use-haproxy-protocol: true\n    haproxy-protocol-whitelisted-ips:\n    - 10.0.0.0/8\n");
        assertTrue(bedrock.node("use-haproxy-protocol").virtual());
        assertEquals(List.of("10.0.0.0/8"), proxies(bedrock));
        verify(logger, never()).warning(anyString());
    }

    @Test
    void onWithNoProxiesTurnsOffAndSaysSo() throws Exception {
        // It took a header from anywhere, which no list says, so it is off until proxies are listed
        ConfigurationNode bedrock = bedrock("    use-haproxy-protocol: true\n");
        assertTrue(bedrock.node("use-haproxy-protocol").virtual());
        assertEquals(List.of(), proxies(bedrock));
        verify(logger).warning(contains("\"use-haproxy-protocol\" has been removed"));
    }

    private static boolean prefersProxyAddress(ConfigurationNode bedrock) {
        return bedrock.node("prefer-signaling-proxy-address").getBoolean(false);
    }

    @Test
    void haproxyProxiesKeepTheirAddress() throws Exception {
        ConfigurationNode bedrock = bedrock("    use-haproxy-protocol: true\n    haproxy-protocol-whitelisted-ips:\n    - 10.0.0.0/8\n");
        assertTrue(prefersProxyAddress(bedrock));
    }

    @Test
    void trustedProxiesKeepTheirAddress() throws Exception {
        ConfigurationNode bedrock = bedrock("    signaling-trusted-proxies:\n    - 10.0.0.1\n");
        assertTrue(prefersProxyAddress(bedrock));
    }

    @Test
    void ignoredOrMissingProxiesDoNotPreferTheirAddress() throws Exception {
        assertFalse(prefersProxyAddress(bedrock("    use-haproxy-protocol: false\n    haproxy-protocol-whitelisted-ips:\n    - 10.0.0.0/8\n")));
        assertFalse(prefersProxyAddress(bedrock("    use-haproxy-protocol: true\n")));
        assertFalse(prefersProxyAddress(bedrock("    compression-level: 6\n")));
    }

    @Test
    void anExplicitChoiceStays() throws Exception {
        ConfigurationNode bedrock = bedrock("    signaling-trusted-proxies:\n    - 10.0.0.1\n    prefer-signaling-proxy-address: false\n");
        assertFalse(prefersProxyAddress(bedrock));
    }
}
