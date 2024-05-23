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

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoaderTemp {
    private static final String HEADER = """
            --------------------------------
            Geyser Configuration File

            A bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition.

            GitHub: https://github.com/GeyserMC/Geyser
            Discord: https://discord.gg/geysermc
            Wiki: https://wiki.geysermc.org/

            NOTICE: See https://wiki.geysermc.org/geyser/setup/ for the setup guide. Many video tutorials are outdated.
            In most cases, especially with server hosting providers, further hosting-specific configuration is required.
            --------------------------------""";

    public static <T extends GeyserConfig> T load(Class<T> configClass) throws IOException {
        var loader = YamlConfigurationLoader.builder()
                .file(new File("newconfig.yml"))
                .defaultOptions(InterfaceDefaultOptions.get()
                        .header(HEADER))
                .build();
        ConfigurationNode node = loader.load();
        // temp fix for node.virtual() being broken
        var virtual = !Files.exists(Path.of("newconfig.yml"));

        // TODO needed or else Configurate breaks
        var migrations = ConfigurationTransformation.versionedBuilder()
                // Pre-Configurate
                .addVersion(5, ConfigurationTransformation.builder()
                        .addAction(NodePath.path("legacyPingPassthrough"), (path, value) -> {
                            // Invert value
                            value.set(Boolean.FALSE.equals(value.get(boolean.class)));
                            return new Object[]{"integratedPingPassthrough"};
                        })
                        .addAction(NodePath.path("remote"), (path, value) ->
                                new Object[]{"java"})
                        .build())
                .build();

        migrations.apply(node);

        T config = node.get(configClass);
        System.out.println(config);

        loader.save(node);

        return config;
    }
}
