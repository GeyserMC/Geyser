/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import lombok.SneakyThrows;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.util.CooldownUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.util.CheckedConsumer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigLoaderTest {

    private static final String CONFIG_PREFIX = "configuration";

    @TempDir
    Path tempDirectory;

    @Test
    void testCreateNewConfig() throws Exception {
        // Test that the result of generating a config, and the result of reading it back after writing it, is the same

        File file = tempDirectory.resolve("config.yml").toFile();

        forAllConfigs(type -> {
            CommentedConfigurationNode config1 = new ConfigLoader(file, PlatformType.STANDALONE).loadConfigurationNode(type);

            long initialModification = file.lastModified();
            assertTrue(file.exists()); // should have been created
            List<String> firstContents = Files.readAllLines(file.toPath());

            CommentedConfigurationNode config2 = new ConfigLoader(file, PlatformType.STANDALONE).loadConfigurationNode(type);
            List<String> secondContents = Files.readAllLines(file.toPath());

            assertEquals(initialModification, file.lastModified()); // should not have been touched
            assertEquals(firstContents, secondContents);

            // Must ignore this, as when the config is read back, the header is interpreted as a comment on the first node in the map
            config1.node("java").comment(null);
            config2.node("java").comment(null);
            assertEquals(config1, config2);
        });
    }

    @Test
    void testDefaultConfigMigration() throws Exception {
        // Test that a migrated default v4 config migrates correctly to the latest default config
        forAllConfigs(type -> {
            CommentedConfigurationNode migratedV4 = new ConfigLoader(copyResourceToTempFile("tests", "v4default.yml"), getPlatformType(type))
                .loadConfigurationNode(type);

            CommentedConfigurationNode defaultV6 = new ConfigLoader(tempDirectory.resolve("new-config.yml").toFile(), getPlatformType(type))
                .loadConfigurationNode(type);

            // Compare the two configs - they should be identical
            migratedV4.node("java").comment(null);
            defaultV6.node("java").comment(null);

            // Metric uuids won't equal, ofc
            migratedV4.node("metrics-uuid").set(new UUID(0, 0));
            defaultV6.node("metrics-uuid").set(new UUID(0, 0));

            assertEquals(migratedV4, defaultV6);
        });
    }

    @Test
    void testAllChangedConfigMigration() throws Exception {
        forAllConfigs(type -> {
            GeyserConfig config = new ConfigLoader(copyResourceToTempFile("tests", "all-changed.yml"), getPlatformType(type)).load0(type);

            assertNotNull(config);

            // Verify bedrock section changes
            assertEquals("127.0.0.1", config.bedrock().address());
            assertEquals(19122, config.bedrock().port());
            assertTrue(config.bedrock().cloneRemotePort());

            // Verify Java section (was remote)
            assertEquals(type == GeyserRemoteConfig.class ? "test.geysermc.org" : null, config.java().address());
            assertEquals(type == GeyserRemoteConfig.class ? 25564 : 0, config.java().port());
            assertEquals(AuthType.FLOODGATE, config.java().authType());
            assertTrue(config.java().forwardHostname());

            // Verify motd section
            assertEquals("Gayser", config.motd().primaryMotd());
            assertEquals("Another Gayser server.", config.motd().secondaryMotd());
            assertFalse(config.motd().passthroughMotd());
            assertFalse(config.motd().passthroughPlayerCounts());
            assertEquals(type == GeyserRemoteConfig.class, config.motd().integratedPingPassthrough());
            assertEquals(2, config.motd().pingPassthroughInterval());
            assertEquals(99, config.motd().maxPlayers());

            // Verify gameplay section
            assertFalse(config.gameplay().commandSuggestions());
            assertTrue(config.gameplay().forwardPlayerPing());
            assertEquals(CooldownUtils.CooldownType.CROSSHAIR, config.gameplay().showCooldown());
            assertEquals("Gayser", config.gameplay().serverName());
            assertFalse(config.gameplay().showCoordinates());
            assertTrue(config.gameplay().disableBedrockScaffolding());
            assertEquals(19, config.gameplay().customSkullRenderDistance());
            assertEquals(111, config.gameplay().maxVisibleCustomSkulls());
            assertFalse(config.gameplay().enableCustomContent());
            assertTrue(config.gameplay().netherRoofWorkaround());
            assertFalse(config.gameplay().forceResourcePacks());
            assertTrue(config.gameplay().xboxAchievementsEnabled());
            assertEquals("minecraft:apple", config.gameplay().unusableSpaceBlock());
            assertTrue(config.gameplay().emotesEnabled());

            // Verify advanced section
            assertEquals(10, config.advanced().cacheImages());
            assertEquals(10, config.advanced().scoreboardPacketThreshold());
            assertEquals("keyyy.pem", config.advanced().floodgateKeyFile());

            // Verify advanced bedrock subsection
            assertEquals(19133, config.advanced().bedrock().broadcastPort());
            assertEquals(5, config.advanced().bedrock().compressionLevel());
            assertTrue(config.advanced().bedrock().useHaproxyProtocol());
            assertEquals(List.of("127.0.0.1", "172.18.0.0/13"), config.advanced().bedrock().haproxyProtocolWhitelistedIps());
            assertTrue(config.advanced().bedrock().validateBedrockLogin());
            assertEquals(1399, config.advanced().bedrock().mtu());

            // Verify advanced java subsection
            assertTrue(config.advanced().java().useHaproxyProtocol());
            assertFalse(config.advanced().java().useDirectConnection());
            assertFalse(config.advanced().java().disableCompression());

            // Verify other root-level settings
            assertEquals("en_uk", config.defaultLocale());
            assertFalse(config.logPlayerIpAddresses());
            assertFalse(config.notifyOnNewBedrockUpdate());
            assertEquals(111, config.pendingAuthenticationTimeout());
            assertEquals(List.of("ThisExampleUsername", "ThisOther"), config.savedUserLogins());
            assertTrue(config.debugMode());

            // Verify metrics
            assertFalse(config.enableMetrics());
            assertEquals(new UUID(0, 0), config.metricsUuid());

            // Verify config version
            assertEquals(Constants.CONFIG_VERSION, config.configVersion());
        });
    }

    @Test
    void testLegacyConfigMigration() throws Exception {
        forAllConfigs(type -> {
            GeyserConfig config = new ConfigLoader(copyResourceToTempFile("tests", "legacy.yml"), getPlatformType(type)).load0(type);

            assertNotNull(config);
            // Verify floodgate key migration from legacy public-key.pem
            assertEquals("key.pem", config.advanced().floodgateKeyFile());
            // Show cooldown "true" should be now migrated to the default with integrated pack
            assertEquals(CooldownUtils.CooldownType.CROSSHAIR, config.gameplay().showCooldown());
            assertEquals(Constants.CONFIG_VERSION, config.configVersion());
        });
    }

    @Test
    void testNoEmotesNoSkullsMigration() throws Exception {
        forAllConfigs(type -> {
            GeyserConfig config = new ConfigLoader(copyResourceToTempFile("tests", "no-emotes-no-skulls.yml"), getPlatformType(type))
                .load0(type);

            assertNotNull(config);
            // When allow-custom-skulls is false, max-visible-custom-skulls should be set to 0
            assertEquals(0, config.gameplay().maxVisibleCustomSkulls());
            // When emote-offhand-workaround was "no-emotes", it should become emotes-enabled: false
            assertFalse(config.gameplay().emotesEnabled());
            assertEquals(Constants.CONFIG_VERSION, config.configVersion());
        });
    }

    @Test
    void testInvalidConfig() throws Exception {
        streamResourceFiles(CONFIG_PREFIX + "/invalid").forEach(resource -> {
            try {
                forAllConfigs(type -> {
                    assertThrows(ConfigurateException.class,
                        () -> new ConfigLoader(resource, getPlatformType(type)).load0(type),
                        "Did not get exception while loading %s (file: %s)".formatted(type.getSimpleName(), resource.getName()));
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    File copyResourceToTempFile(String... path) throws Exception {
        File resource = getConfigResource(CONFIG_PREFIX + "/" + String.join("/", path));
        return Files.copy(resource.toPath(), tempDirectory.resolve(resource.getName()), StandardCopyOption.REPLACE_EXISTING).toFile();
    }

    PlatformType getPlatformType(Class<? extends GeyserConfig> configClass) {
        if (configClass == GeyserRemoteConfig.class) {
            return PlatformType.STANDALONE;
        }
        if (configClass == GeyserPluginConfig.class) {
            return PlatformType.SPIGOT;
        }
        throw new IllegalArgumentException("Unsupported config class " + configClass);
    }

    void forAllConfigs(CheckedConsumer<Class<? extends GeyserConfig>, Exception> consumer) throws Exception {
        consumer.accept(GeyserPluginConfig.class);
        consumer.accept(GeyserRemoteConfig.class);
    }

    private static Stream<File> streamResourceFiles(String directory) throws IOException, URISyntaxException {
        URL resourceUrl = ConfigLoaderTest.class.getClassLoader().getResource(directory);
        Objects.requireNonNull(resourceUrl, "Resource directory not found: " + directory);

        Path resourcePath = Path.of(resourceUrl.toURI());
        // Walk the directory, but don't go into subdirectories (maxDepth = 1)
        return Files.walk(resourcePath, 1)
            .filter(path -> !path.equals(resourcePath)) // Exclude the directory itself
            .filter(Files::isRegularFile) // Ensure we only get files
            .map(Path::toFile);
    }

    @SneakyThrows
    private File getConfigResource(String name) {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(name), "No resource for name: " + name);
        return Path.of(url.toURI()).toFile();
    }
}
