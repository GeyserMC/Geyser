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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import static org.spongepowered.configurate.NodePath.path;
import static org.spongepowered.configurate.transformation.TransformAction.remove;
import static org.spongepowered.configurate.transformation.TransformAction.rename;

public final class ConfigLoader {
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

    public static <T extends GeyserConfig> T load(File file, Class<T> configClass) throws IOException {
        return load(file, configClass, null);
    }

    public static <T extends GeyserConfig> T load(File file, Class<T> configClass, @Nullable Consumer<CommentedConfigurationNode> transformer) throws IOException {
        var loader = YamlConfigurationLoader.builder()
                .file(file)
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions(options -> InterfaceDefaultOptions.addTo(options)
                    .shouldCopyDefaults(false) // If we use ConfigurationNode#get(type, default), do not write the default back to the node.
                    .header(HEADER)
                    .serializers(builder -> builder.register(new LowercaseEnumSerializer())))
                .build();

        CommentedConfigurationNode node = loader.load();
        boolean originallyEmpty = !file.exists() || node.isNull();

        // Note for Tim? Needed or else Configurate breaks.
        var migrations = ConfigurationTransformation.versionedBuilder()
            .versionKey("config-version")
                // Pre-Configurate
                .addVersion(5, ConfigurationTransformation.builder()
                    .addAction(path("legacy-ping-passthrough"), configClass == GeyserRemoteConfig.class ? remove() : (path, value) -> {
                        // Invert value
                        value.set(!value.getBoolean());
                        return new Object[]{"integrated-ping-passthrough"};
                    })
                    .addAction(path("remote"), rename("java"))
                    .addAction(path("floodgate-key-file"), (path, value) -> {
                        // Elimate any legacy config values
                        if ("public-key.pem".equals(value.getString())) {
                            value.set("key.pem");
                        }
                        return null;
                    })
                    .addAction(path("default-locale"), (path, value) -> {
                        if (value.getString() == null) {
                            value.set("system");
                        }
                        return null;
                    })
                    .addAction(path("show-cooldown"), (path, value) -> {
                        String s = value.getString();
                        if (s != null) {
                            switch (s) {
                                case "true" -> value.set("title");
                                case "false" -> value.set("disabled");
                            }
                        }
                        return null;
                    })
                    .addAction(path("metrics", "uuid"), (path, value) -> {
                        if ("generateduuid".equals(value.getString())) {
                            // Manually copied config without Metrics UUID creation?
                            return new Object[]{UUID.randomUUID()};
                        }
                        return null;
                    })
                    .addAction(path("bedrock", "motd1"), rename("primary-motd"))
                    .addAction(path("bedrock", "motd2"), rename("secondary-motd"))
                    // Legacy config values
                    .addAction(path("emote-offhand-workaround"), remove())
                    .addAction(path("allow-third-party-capes"), remove())
                    .addAction(path("allow-third-party-ears"), remove())
                    .addAction(path("general-thread-pool"), remove())
                    .addAction(path("cache-chunks"), remove())
                    .build())
                .build();

        int currentVersion = migrations.version(node);
        migrations.apply(node);
        int newVersion = migrations.version(node);

        T config = node.get(configClass);

        // Serialize the instance to ensure strict field ordering. Additionally, if we serialized back
        // to the old node, existing nodes would only have their value changed, keeping their position
        // at the top of the ordered map, forcing all new nodes to the bottom (regardless of field order).
        // For that reason, we must also create a new node.
        CommentedConfigurationNode newRoot = CommentedConfigurationNode.root(loader.defaultOptions());
        newRoot.set(config);

        if (originallyEmpty || currentVersion != newVersion) {

            if (!originallyEmpty && currentVersion > 4) {
                // Only copy comments over if the file already existed, and we are going to replace it

                // Second case: Version 4 is pre-configurate where there were commented out nodes.
                // These get treated as comments on lower nodes, which produces very undesirable results.

                ConfigurationCommentMover.moveComments(node, newRoot);
            }

            loader.save(newRoot);
        }

        if (transformer != null) {
            // We transform AFTER saving so that these specific transformations aren't applied to file.
            transformer.accept(newRoot);
            config = newRoot.get(configClass);
        }

        return config;
    }

    private ConfigLoader() {
    }
}
