/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.sponge;

import com.google.inject.Inject;

import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import org.geysermc.common.PlatformType;
import org.geysermc.common.bootstrap.IGeyserBootstrap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.platform.sponge.command.GeyserSpongeCommandExecutor;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Plugin(id = "geyser", name = GeyserConnector.NAME + "-Sponge", version = GeyserConnector.VERSION_STATIC, url = "https://geysermc.org", authors = "GeyserMC")
public class GeyserSpongePlugin implements IGeyserBootstrap {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private GeyserSpongeConfiguration geyserConfig;
    private GeyserSpongeLogger geyserLogger;

    private GeyserConnector connector;

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
            this.geyserConfig = new GeyserSpongeConfiguration(configDir, loader.load());
        } catch (IOException ex) {
            logger.warn("Failed to load config.yml!");
            ex.printStackTrace();
            return;
        }

        this.geyserLogger = new GeyserSpongeLogger(logger, geyserConfig.isDebugMode());
        this.connector = GeyserConnector.start(PlatformType.SPONGE, this);

        Sponge.getCommandManager().register(this, new GeyserSpongeCommandExecutor(connector), "geyser");
    }

    @Override
    public void onDisable() {
        connector.shutdown();
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
