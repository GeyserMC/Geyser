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

import com.google.common.annotations.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.text.GeyserLocale;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.objectmapping.meta.Processor;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class ConfigLoader {
    private static final String HEADER = """
        --------------------------------
        Geyser Configuration File
        
        A bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition.
        
        GitHub: https://github.com/GeyserMC/Geyser
        Discord: https://discord.gg/geysermc
        Wiki: https://geysermc.org/wiki
        
        NOTICE: See https://geysermc.org/wiki/geyser/setup/ for the setup guide. Many video tutorials are outdated.
        In most cases, especially with server hosting providers, further hosting-specific configuration is required.
        --------------------------------""";

    /**
     * Only nullable for testing.
     */
    private final @Nullable GeyserBootstrap bootstrap;
    private PlatformType platformType;
    private @Nullable Consumer<CommentedConfigurationNode> transformer;
    private File configFile;

    /**
     * Only set during testing.
     */
    @VisibleForTesting
    CommentedConfigurationNode configurationNode;

    public ConfigLoader(GeyserBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.platformType = bootstrap.platformType();
        configFile = new File(bootstrap.getConfigFolder().toFile(), "config.yml");
    }

    @VisibleForTesting
    ConfigLoader(File file) {
        this.bootstrap = null;
        configFile = file;
    }

    /**
     * Creates the directory as indicated by {@link GeyserBootstrap#getConfigFolder()}
     */
    @This
    public ConfigLoader createFolder() {
        Path dataFolder = this.bootstrap.getConfigFolder();
        if (!dataFolder.toFile().exists()) {
            if (!dataFolder.toFile().mkdir()) {
                GeyserImpl.getInstance().getLogger().warning("Failed to create config folder: " + dataFolder);
            }
        }
        return this;
    }

    @This
    public ConfigLoader transformer(Consumer<CommentedConfigurationNode> transformer) {
        this.transformer = transformer;
        return this;
    }

    @This
    public ConfigLoader configFile(File configFile) {
        this.configFile = configFile;
        return this;
    }

    /**
     * @return null if the config failed to load.
     */
    @Nullable
    public <T extends GeyserConfig> T load(Class<T> configClass) {
        try {
            return load0(configClass);
        } catch (IOException ex) {
            bootstrap.getGeyserLogger().error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            return null;
        }
    }

    private <T extends GeyserConfig> T load0(Class<T> configClass) throws ConfigurateException {
        var loader = createLoader(configFile);

        CommentedConfigurationNode node = loader.load();
        boolean originallyEmpty = !configFile.exists() || node.isNull();

        ConfigurationTransformation.Versioned migrations = ConfigMigrations.TRANSFORMER.apply(configClass, bootstrap);
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

        if (this.bootstrap != null) { // Null for testing only.
            this.bootstrap.getGeyserLogger().setDebug(config.debugMode());
        } else {
            this.configurationNode = newRoot;
        }

        return config;
    }

    @VisibleForTesting
    CommentedConfigurationNode loadConfigurationNode(Class<? extends GeyserConfig> configClass, PlatformType platformType) throws ConfigurateException {
        this.platformType = platformType;
        load0(configClass);
        return configurationNode.copy();
    }

    private YamlConfigurationLoader createLoader(File file) {
        return YamlConfigurationLoader.builder()
            .file(file)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(options -> InterfaceDefaultOptions.addTo(options, builder -> {
                        builder.addProcessor(ExcludePlatform.class, excludePlatform(platformType.platformName()))
                            .addProcessor(PluginSpecific.class, integrationSpecific(platformType != PlatformType.STANDALONE));
                })
                .shouldCopyDefaults(false) // If we use ConfigurationNode#get(type, default), do not write the default back to the node.
                .header(ConfigLoader.HEADER)
                .serializers(builder -> builder.register(new LowercaseEnumSerializer())))
            .build();
    }

    private static Processor.Factory<ExcludePlatform, Object> excludePlatform(String thisPlatform) {
        return (data, fieldType) -> (value, destination) -> {
            for (String platform : data.platforms()) {
                if (thisPlatform.equals(platform)) {
                    //noinspection DataFlowIssue
                    destination.parent().removeChild(destination.key());
                    break;
                }
            }
        };
    }

    private static Processor.Factory<PluginSpecific, Object> integrationSpecific(boolean thisConfigPlugin) {
        return (data, fieldType) -> (value, destination) -> {
            if (data.forPlugin() != thisConfigPlugin) {
                //noinspection DataFlowIssue
                destination.parent().removeChild(destination.key());
            }
        };
    }
}
