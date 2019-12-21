package org.geysermc.platform.sponge;

import com.google.inject.Inject;

import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import org.geysermc.common.PlatformType;
import org.geysermc.common.bootstrap.IGeyserBootstrap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.slf4j.Logger;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Plugin(id = "geyser", name = GeyserConnector.NAME + "-Sponge", version = GeyserConnector.VERSION, url = "https://geysermc.org", authors = "GeyserMC")
public class GeyserSpongePlugin implements IGeyserBootstrap {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private GeyserSpongeConfiguration geyserConfig;
    private GeyserSpongeLogger geyserLogger;

    @Override
    public void onEnable() {
        if (!configDir.exists())
            configDir.mkdirs();

        File configFile = null;
        try {
            configFile = FileUtils.fileOrCopiedFromResource(new File(configDir, "config.yml"), "config.yml", (file) -> file.replaceAll("generateduuid", UUID.randomUUID().toString()));
        } catch (IOException ex) {
            logger.warn("Failed to copy config.yml from jar path!");
            ex.printStackTrace();
        }

        ConfigurationLoader loader = YAMLConfigurationLoader.builder().setPath(configFile.toPath()).build();
        try {
            this.geyserConfig = new GeyserSpongeConfiguration(loader.load());
        } catch (IOException ex) {
            logger.warn("Failed to load config.yml!");
            ex.printStackTrace();
            return;
        }

        this.geyserLogger = new GeyserSpongeLogger(logger, geyserConfig.isDebugMode());

        GeyserConnector.start(PlatformType.SPONGE, this);
    }

    @Override
    public void onDisable() {
        GeyserConnector.stop();
    }

    @Override
    public GeyserSpongeConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserSpongeLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        onEnable();
    }

    @Listener
    public void onServerStop(GameStoppedEvent event) {
        onDisable();
    }
}
