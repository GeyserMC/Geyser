/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import org.apache.logging.log4j.Logger;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.sponge.command.GeyserSpongeCommandExecutor;
import org.geysermc.platform.sponge.command.GeyserSpongeCommandManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(value = "geyser")
public class GeyserSpongePlugin implements GeyserBootstrap {

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configPath;

    private GeyserSpongeCommandManager geyserCommandManager;
    private GeyserSpongeConfiguration geyserConfig;
    private GeyserSpongeLogger geyserLogger;
    private IGeyserPingPassthrough geyserSpongePingPassthrough;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        File configDir = configPath.toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File configFile = null;
        try {
            configFile = FileUtils.fileOrCopiedFromResource(new File(configDir, "config.yml"), "config.yml", (file) -> file.replaceAll("generateduuid", UUID.randomUUID().toString()));
        } catch (IOException ex) {
            logger.warn(LanguageUtils.getLocaleStringLog("geyser.config.failed"));
            ex.printStackTrace();
        }

        try {
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserSpongeConfiguration.class);
        } catch (IOException ex) {
            logger.warn(LanguageUtils.getLocaleStringLog("geyser.config.failed"));
            ex.printStackTrace();
            return;
        }

        if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
            // Can't access the server instance during the ConstructPluginEvent to determine the server address.
            // Can't use later lifecycle events after this event because commands can no longer be registered (at least not without hacks)
            this.geyserConfig.setAutoconfiguredRemote(true);
            logger.warn("Remote address in the config is set to auto but Geyser-sponge doesn't support automatically determining the remote port.");
            // GeyserConnector should still attempt to determine an okay address
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(geyserConfig.getRemote().getPort());
        }

        this.geyserLogger = new GeyserSpongeLogger(logger, geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        this.connector = GeyserConnector.start(PlatformType.SPONGE, this);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserSpongePingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        } else {
            this.geyserSpongePingPassthrough = new GeyserSpongePingPassthrough();
        }
    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Raw> event) {
        this.geyserCommandManager = new GeyserSpongeCommandManager(Sponge.server().commandManager(), connector);
        event.register(this.pluginContainer, new GeyserSpongeCommandExecutor(connector), "geyser");
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

    @Override
    public CommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserSpongePingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return configPath;
    }

    @Listener
    public void onConstruction(ConstructPluginEvent event) {
        // this event must be used instead of StartingEngineEvent/StartedEngineEvent/LoadedGame event, as command registration events are called before the latter 3
        onEnable();
    }

    @Listener
    public void onEngineStopping(StoppingEngineEvent<?> event) {
        onDisable();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserSpongeDumpInfo();
    }

    @Override
    public String getMinecraftServerVersion() {
        return Sponge.platform().minecraftVersion().name();
    }
}
