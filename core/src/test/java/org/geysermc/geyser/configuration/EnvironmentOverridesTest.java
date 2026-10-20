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

import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EnvironmentOverridesTest {
    private static final int MAX_KEY_LENGTH = 64;

    @TempDir
    Path tempDirectory;

    static Stream<Arguments> platformTypes() {
        return ConfigLoaderTest.platformTypes();
    }

    @Test
    void testScalarOverrides() throws Exception {
        File file = tempDirectory.resolve("scalar.yml").toFile();
        GeyserConfig config = new ConfigLoader(file, PlatformType.STANDALONE)
            .environment(Map.of(
                "GEYSER_BEDROCK_PORT", "19200",
                "GEYSER_JAVA_AUTH_TYPE", "floodgate",
                "GEYSER_DEBUG_MODE", "true",
                "GEYSER_MOTD_PRIMARY_MOTD", "a lovely motd"
            ))
            .load0(GeyserRemoteConfig.class);

        assertEquals(19200, config.bedrock().port());
        assertEquals(AuthType.FLOODGATE, config.java().authType());
        assertTrue(config.debugMode());
        assertEquals("a lovely motd", config.motd().primaryMotd());

        // Overrides must not be written to the config file
        String contents = Files.readString(file.toPath());
        assertFalse(contents.contains("19200"));
        assertFalse(contents.contains("a lovely motd"));

        GeyserConfig reloaded = new ConfigLoader(file, PlatformType.STANDALONE).load0(GeyserRemoteConfig.class);
        assertEquals(19132, reloaded.bedrock().port());
        assertFalse(reloaded.debugMode());
    }

    @Test
    void testListOverrides() throws Exception {
        File file = tempDirectory.resolve("lists.yml").toFile();
        GeyserConfig config = new ConfigLoader(file, PlatformType.STANDALONE)
            .environment(Map.of(
                "GEYSER_BEDROCK_SIGNALING_ADVERTISE_ADDRESSES", "play.example.com, 198.51.100.1, [2001:db8::1]:19133,,",
                "GEYSER_BEDROCK_SIGNALING_STUN_DISCOVERY", "false",
                "GEYSER_SAVED_USER_LOGINS", "ThisExampleUsername,ThisOther"
            ))
            .load0(GeyserRemoteConfig.class);

        GeyserConfig.SignalingConfig signaling = config.bedrock().signaling();
        assertEquals(List.of("play.example.com", "198.51.100.1", "[2001:db8::1]:19133"), signaling.advertiseAddresses());
        assertFalse(signaling.stunDiscovery());
        assertEquals(List.of("ThisExampleUsername", "ThisOther"), config.savedUserLogins());
    }

    @Test
    void testEmptyListOverrideClearsList() throws Exception {
        File file = tempDirectory.resolve("clear.yml").toFile();
        new ConfigLoader(file, PlatformType.STANDALONE)
            .environment(Map.of("GEYSER_SAVED_USER_LOGINS", "ThisExampleUsername"))
            .load0(GeyserRemoteConfig.class);

        GeyserConfig config = new ConfigLoader(file, PlatformType.STANDALONE)
            .environment(Map.of("GEYSER_SAVED_USER_LOGINS", ""))
            .load0(GeyserRemoteConfig.class);
        assertEquals(List.of(), config.savedUserLogins());
    }

    @Test
    void testUnknownAndConfigVersionWarnings() throws Exception {
        ConfigLoader loader = new ConfigLoader(tempDirectory.resolve("unknown.yml").toFile(), PlatformType.STANDALONE);
        CommentedConfigurationNode node = loader.loadConfigurationNode(GeyserRemoteConfig.class);

        GeyserLogger logger = mock(GeyserLogger.class);
        boolean changed = EnvironmentOverrides.apply(node, loader.platformExcluded, Map.of(
            "GEYSER_DOES_NOT_EXIST", "1",
            "GEYSER_BEDROCK", "section",
            "GEYSER_CONFIG_VERSION", "1",
            "NOT_GEYSER_BEDROCK_PORT", "1",
            // Moved to bedrock.signaling in config version 11
            "GEYSER_BEDROCK_SIGNALING_NXS_ADVERTISE_ADDRESSES", "198.51.100.1"
        ), logger);

        assertFalse(changed);
        assertEquals(Constants.CONFIG_VERSION, node.node("config-version").getInt());
        verify(logger).warning(contains("GEYSER_DOES_NOT_EXIST"));
        verify(logger).warning(contains("GEYSER_BEDROCK does not match"));
        verify(logger).warning(contains("GEYSER_BEDROCK_SIGNALING_NXS_ADVERTISE_ADDRESSES"));
        verify(logger).warning(contains("config version cannot be set"));
        verify(logger, never()).warning(contains("NOT_GEYSER_BEDROCK_PORT"));
        verify(logger, never()).info(anyString());
    }

    @Test
    void testPlatformSpecificOptionsDoNotWarn() throws Exception {
        // Metrics options are excluded on Spigot, plugin-only options on standalone
        Map<PlatformType, Class<? extends GeyserConfig>> platforms = Map.of(
            PlatformType.SPIGOT, GeyserPluginConfig.class,
            PlatformType.STANDALONE, GeyserRemoteConfig.class
        );
        Map<String, String> env = Map.of(
            "GEYSER_ENABLE_METRICS", "false",
            "GEYSER_BEDROCK_CLONE_REMOTE_PORT", "true",
            "GEYSER_ADVANCED_JAVA_BUNGEE_LISTENER", "true"
        );

        for (var entry : platforms.entrySet()) {
            ConfigLoader loader = new ConfigLoader(tempDirectory.resolve("platform-" + entry.getKey() + ".yml").toFile(), entry.getKey());
            CommentedConfigurationNode node = loader.loadConfigurationNode(entry.getValue());

            GeyserLogger logger = mock(GeyserLogger.class);
            EnvironmentOverrides.apply(node, loader.platformExcluded, env, logger);
            verify(logger, never()).warning(anyString());
        }
    }

    @ParameterizedTest
    @MethodSource("platformTypes")
    void testEnvKeysAreUnique(PlatformType platformType, Class<? extends GeyserConfig> configClass) throws Exception {
        CommentedConfigurationNode node = new ConfigLoader(tempDirectory.resolve("keys-" + platformType + ".yml").toFile(), platformType)
            .loadConfigurationNode(configClass);

        Map<String, String> seen = new HashMap<>();
        collectKeys(node, seen);
        // Sanity check that the walk found the options this feature mainly exists for
        assertTrue(seen.containsKey("GEYSER_BEDROCK_PORT"));
        assertTrue(seen.containsKey("GEYSER_BEDROCK_SIGNALING_ADVERTISE_ADDRESSES"));
        assertTrue(node.node("bedrock", "signaling", "advertise-addresses").isList());

        // Well below any real limit; flags unusually deep or long option names so their variable names get a second look
        for (String key : seen.keySet()) {
            assertTrue(key.length() <= MAX_KEY_LENGTH, () -> key + " is longer than " + MAX_KEY_LENGTH + " characters");
        }
    }

    private static void collectKeys(CommentedConfigurationNode node, Map<String, String> seen) {
        if (node.isMap()) {
            for (CommentedConfigurationNode child : node.childrenMap().values()) {
                collectKeys(child, seen);
            }
            return;
        }
        String key = EnvironmentOverrides.envKey(node.path());
        String previous = seen.put(key, node.path().toString());
        assertNull(previous, "Environment variable " + key + " maps to both " + previous + " and " + node.path());
    }

    @Test
    void testRecordsPlatformExcludedPaths() throws Exception {
        ConfigLoader loader = new ConfigLoader(tempDirectory.resolve("excluded.yml").toFile(), PlatformType.STANDALONE);
        loader.loadConfigurationNode(GeyserRemoteConfig.class);

        Set<String> keys = new HashSet<>();
        loader.platformExcluded.forEach(path -> keys.add(EnvironmentOverrides.envKey(path)));
        assertTrue(keys.contains("GEYSER_BEDROCK_CLONE_REMOTE_PORT"), keys::toString);
        assertTrue(keys.contains("GEYSER_ADVANCED_JAVA_BUNGEE_LISTENER"), keys::toString);
    }
}
