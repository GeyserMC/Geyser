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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.util.CheckedConsumer;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigLoaderTest {

    private static final String CONFIG_PREFIX = "configuration/";

    @TempDir
    Path tempDirectory;

    @Test
    void testCreateNewConfig() throws Exception {
        // Test that the result of generating a config, and the result of reading it back after writing it, is the same

        File file = tempDirectory.resolve("config.yml").toFile();

        forAllConfigs(type -> {
            CommentedConfigurationNode config1 = new ConfigLoader(file).loadConfigurationNode(type);

            long initialModification = file.lastModified();
            assertTrue(file.exists()); // should have been created
            List<String> firstContents = Files.readAllLines(file.toPath());

            CommentedConfigurationNode config2 = new ConfigLoader(file).loadConfigurationNode(type);
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

    public void testConfiguration(String folder) throws Exception {
        forAllConfigs(type -> {
            CommentedConfigurationNode oldTransformed = new ConfigLoader(getConfigResource(folder + "/before.yml")).loadConfigurationNode(type);

            String configName = type == GeyserRemoteConfig.class ? "remote" : "plugin";
            CommentedConfigurationNode newTransformed = new ConfigLoader(getConfigResource(folder + "/" + configName + ".yml")).loadConfigurationNode(type);

            assertEquals(oldTransformed, newTransformed);
        });
    }

    void forAllConfigs(CheckedConsumer<Class<? extends GeyserConfig>, Exception> consumer) throws Exception {
        consumer.accept(GeyserRemoteConfig.class);
        consumer.accept(GeyserPluginConfig.class);
    }

    @SneakyThrows
    private File getConfigResource(String name) {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(CONFIG_PREFIX + name), "No resource for name: " + name);
        return Path.of(url.toURI()).toFile();
    }
}
