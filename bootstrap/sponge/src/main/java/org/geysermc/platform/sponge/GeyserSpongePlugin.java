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
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(value = "geyser")
public class GeyserSpongePlugin implements GeyserBootstrap {

    /**
     * True if the plugin should be in a disabled state.
     * This exists because you can't unregister or disable plugins in Sponge
     */
    private boolean enabled = true;

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configPath;

    // Available after construction lifecycle
    private GeyserSpongeConfiguration geyserConfig;
    private GeyserSpongeLogger geyserLogger;
    private GeyserSpongeCommandManager geyserCommandManager; // Commands are only registered after command registration lifecycle

    // Available after command registration lifecycle - never changes afterward
    private GeyserSpongeCommandExecutor geyserCommandExecutor; // This variable should never be changed after initial assignment

    // Available after StartedEngine lifecycle
    private GeyserConnector connector;
    private IGeyserPingPassthrough geyserSpongePingPassthrough;


    /**
     * Only to be used for reloading
     */
    @Override
    public void onEnable() {
        enabled = true;
        onConstruction(null);
        // new commands cannot be registered, and geyser's command manager does not need be reloaded
        onStartedEngine(null);
    }

    @Override
    public void onDisable() {
        enabled = false;
        if (connector != null) {
            connector.shutdown();
            connector = null;
        }
    }

    /**
     * Construct the configuration, logger, and command manager. command manager will only be filled with commands once
     * the connector is started, but it allows us to register events in sponge.
     *
     * @param event Not used.
     */
    @Listener
    public void onConstruction(@Nullable ConstructPluginEvent event) {
        File configDir = configPath.toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        try {
            File configFile = FileUtils.fileOrCopiedFromResource(new File(configDir, "config.yml"), "config.yml", (file) -> file.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserSpongeConfiguration.class);
        } catch (IOException ex) {
            logger.error(LanguageUtils.getLocaleStringLog("geyser.config.failed"));
            ex.printStackTrace();
            onDisable();
            return;
        }

        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        this.geyserLogger = new GeyserSpongeLogger(logger, geyserConfig.isDebugMode());

        // Construct it without the commands
        this.geyserCommandManager = new GeyserSpongeCommandManager(this.geyserLogger);
    }

    /**
     * Construct the {@link GeyserSpongeCommandManager} and register the commands
     *
     * @param event required to register the commands
     */
    @Listener
    public void onRegisterCommands(@Nonnull RegisterCommandEvent<Command.Raw> event) {
        if (enabled) {
            this.geyserCommandExecutor = new GeyserSpongeCommandExecutor(this.geyserCommandManager);
            event.register(this.pluginContainer, this.geyserCommandExecutor, "geyser");
        }
    }

    /**
     * Configure the config properly if remote address is auto. Start connector and ping passthrough, and register subcommands of /geyser
     *
     * @param event not required
     */
    @Listener
    public void onStartedEngine(@Nullable StartedEngineEvent<?> event) {
        if (!enabled) {
            return;
        }

        if (Sponge.server().boundAddress().isPresent()) {
            InetSocketAddress javaAddr = Sponge.server().boundAddress().get();

            // Don't change the ip if its listening on all interfaces
            // By default this should be 127.0.0.1 but may need to be changed in some circumstances
            if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
                this.geyserConfig.setAutoconfiguredRemote(true);
                geyserConfig.getRemote().setPort(javaAddr.getPort());
            }
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(geyserConfig.getRemote().getPort());
        }

        this.connector = GeyserConnector.start(PlatformType.SPONGE, this);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserSpongePingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        } else {
            this.geyserSpongePingPassthrough = new GeyserSpongePingPassthrough();
        }

        this.geyserCommandManager.registerDefaults(connector); // register the subcommands once we have the connector
        this.geyserCommandExecutor.setCommandManager(geyserCommandManager); // re-assign the command manager in the case this was a reload
    }

    @Listener
    public void onEngineStopping(StoppingEngineEvent<?> event) {
        onDisable();
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

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserSpongeDumpInfo();
    }

    @Override
    public String getMinecraftServerVersion() {
        return Sponge.platform().minecraftVersion().name();
    }
}
