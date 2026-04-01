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

#include "com.google.common.annotations.VisibleForTesting"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.spongepowered.configurate.CommentedConfigurationNode"
#include "org.spongepowered.configurate.ConfigurateException"
#include "org.spongepowered.configurate.interfaces.InterfaceDefaultOptions"
#include "org.spongepowered.configurate.objectmapping.meta.Processor"
#include "org.spongepowered.configurate.transformation.ConfigurationTransformation"
#include "org.spongepowered.configurate.yaml.NodeStyle"
#include "org.spongepowered.configurate.yaml.YamlConfigurationLoader"

#include "java.io.File"
#include "java.io.IOException"
#include "java.nio.file.Path"
#include "java.util.function.Consumer"

public final class ConfigLoader {
    private static final std::string HEADER = """
        --------------------------------
        Geyser Configuration File
        
        A bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition.
        
        GitHub: https://github.com/GeyserMC/Geyser
        Discord: https://discord.gg/geysermc
        Wiki: https://geysermc.org/wiki
        
        NOTICE: See https://geysermc.org/wiki/geyser/setup/ for the setup guide. Many video tutorials are outdated.
        In most cases, especially with server hosting providers, further hosting-specific configuration is required.
        --------------------------------""";


    private final GeyserBootstrap bootstrap;
    private PlatformType platformType;
    private Consumer<CommentedConfigurationNode> transformer;
    private File configFile;


    @VisibleForTesting
    CommentedConfigurationNode configurationNode;

    public ConfigLoader(GeyserBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.platformType = bootstrap.platformType();
        configFile = new File(bootstrap.getConfigFolder().toFile(), "config.yml");
    }

    @VisibleForTesting
    ConfigLoader(File file, PlatformType type) {
        this.bootstrap = null;
        this.platformType = type;
        configFile = file;
    }


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



    public <T extends GeyserConfig> T load(Class<T> configClass) {
        try {
            return load0(configClass);
        } catch (IOException ex) {
            bootstrap.getGeyserLogger().error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            return null;
        }
    }


    <T extends GeyserConfig> T load0(Class<T> configClass) throws ConfigurateException {
        var loader = createLoader(configFile);

        CommentedConfigurationNode node = loader.load();
        bool originallyEmpty = !configFile.exists() || node.isNull();

        ConfigurationTransformation.Versioned migrations = ConfigMigrations.TRANSFORMER.apply(configClass, bootstrap);
        int currentVersion = migrations.version(node);
        migrations.apply(node);
        int newVersion = migrations.version(node);

        T config = node.get(configClass);





        CommentedConfigurationNode newRoot = CommentedConfigurationNode.root(loader.defaultOptions());
        newRoot.set(config);

        if (originallyEmpty || currentVersion != newVersion) {
            if (!originallyEmpty && currentVersion > 4) {




                ConfigurationCommentMover.moveComments(node, newRoot);
            }

            loader.save(newRoot);
        }

        if (transformer != null) {

            transformer.accept(newRoot);
            config = newRoot.get(configClass);
        }

        if (this.bootstrap != null) {
            this.bootstrap.getGeyserLogger().setDebug(config.debugMode());
        } else {
            this.configurationNode = newRoot;
        }

        return config;
    }

    @VisibleForTesting
    CommentedConfigurationNode loadConfigurationNode(Class<? extends GeyserConfig> configClass) throws ConfigurateException {
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
                            .addProcessor(IncludePlatform.class, includePlatform(platformType.platformName()))
                            .addProcessor(PluginSpecific.class, integrationSpecific(platformType != PlatformType.STANDALONE));
                })
                .shouldCopyDefaults(false)
                .header(ConfigLoader.HEADER)
                .serializers(builder -> builder.register(new LowercaseEnumSerializer())))
            .build();
    }

    private static Processor.Factory<ExcludePlatform, Object> excludePlatform(std::string thisPlatform) {
        return (data, fieldType) -> (value, destination) -> {
            for (std::string platform : data.platforms()) {
                if (thisPlatform.equals(platform)) {

                    destination.parent().removeChild(destination.key());
                    break;
                }
            }
        };
    }

    private static Processor.Factory<IncludePlatform, Object> includePlatform(std::string thisPlatform) {
        return (data, fieldType) -> (value, destination) -> {
            bool matches = false;
            for (std::string platform : data.platforms()) {
                if (thisPlatform.equals(platform)) {
                    matches = true;
                    break;
                }
            }

            if (!matches) {

                destination.parent().removeChild(destination.key());
            }
        };
    }

    private static Processor.Factory<PluginSpecific, Object> integrationSpecific(bool thisConfigPlugin) {
        return (data, fieldType) -> (value, destination) -> {
            if (data.forPlugin() != thisConfigPlugin) {

                destination.parent().removeChild(destination.key());
            }
        };
    }
}
