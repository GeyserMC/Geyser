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
import org.geysermc.geyser.api.util.PlatformType;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            CommentedConfigurationNode config1 = new ConfigLoader(file).loadConfigurationNode(type, PlatformType.STANDALONE);

            long initialModification = file.lastModified();
            assertTrue(file.exists()); // should have been created
            List<String> firstContents = Files.readAllLines(file.toPath());

            CommentedConfigurationNode config2 = new ConfigLoader(file).loadConfigurationNode(type, PlatformType.STANDALONE);
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
        testConfiguration("default");
    }

    @Test
    void testAllChangedConfigMigration() throws Exception {
        testConfiguration("all-changed");
    }

    @Test
    void testLegacyConfigMigration() throws Exception {
        testConfiguration("legacy");
    }

    @Test
    void allowCustomSkullsMigration() throws Exception {
        testConfiguration("allow-custom-skulls");
    }

    @Test
    void testNoEmotesMigration() throws Exception {
        testConfiguration("migrate-no-emotes");
    }

    @Test
    void testChewsOldConfig() throws Exception {
        testConfiguration("chew");
    }

    @Test
    void testInvalidConfig() throws Exception {
        streamResourceFiles(CONFIG_PREFIX + "/invalid").forEach(resource -> {
            try {
                forAllConfigs(type -> {
                    assertThrows(ConfigurateException.class,
                        () -> new ConfigLoader(resource).loadConfigurationNode(type, getPlatformType(type)),
                        "Did not get exception while loading %s (file: %s)".formatted(type.getSimpleName(), resource.getName()));
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void testConfiguration(String folder) throws Exception {
        forAllConfigs(type -> {
            CommentedConfigurationNode oldTransformed = new ConfigLoader(copyResourceToTempFile(folder, "before.yml"))
                .loadConfigurationNode(type, getPlatformType(type));

            String configName = type == GeyserRemoteConfig.class ? "remote" : "plugin";
            CommentedConfigurationNode newTransformed = new ConfigLoader(copyResourceToTempFile(folder, configName + ".yml"))
                .loadConfigurationNode(type, getPlatformType(type));

            assertEquals(oldTransformed, newTransformed);
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
