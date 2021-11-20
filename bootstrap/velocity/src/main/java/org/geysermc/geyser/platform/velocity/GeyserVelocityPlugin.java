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

package org.geysermc.geyser.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ListenerType;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.velocity.command.GeyserVelocityCommandExecutor;
import org.geysermc.geyser.platform.velocity.command.GeyserVelocityCommandManager;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.text.GeyserLocale;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Plugin(id = "geyser", name = GeyserImpl.NAME + "-Velocity", version = GeyserImpl.VERSION, url = "https://geysermc.org", authors = "GeyserMC")
public class GeyserVelocityPlugin implements GeyserBootstrap {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxyServer;

    @Inject
    private CommandManager commandManager;

    private GeyserVelocityCommandManager geyserCommandManager;
    private GeyserVelocityConfiguration geyserConfig;
    private GeyserVelocityInjector geyserInjector;
    private GeyserVelocityLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;

    private GeyserImpl geyser;

    @Getter
    private final Path configFolder = Paths.get("plugins/" + GeyserImpl.NAME + "-Velocity/");

    @Override
    public void onEnable() {
        try {
            if (!configFolder.toFile().exists())
                //noinspection ResultOfMethodCallIgnored
                configFolder.toFile().mkdirs();
            File configFile = FileUtils.fileOrCopiedFromResource(configFolder.resolve("config.yml").toFile(),
                    "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserVelocityConfiguration.class);
        } catch (IOException ex) {
            logger.warn(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
        }

        InetSocketAddress javaAddr = proxyServer.getBoundAddress();

        // By default this should be localhost but may need to be changed in some circumstances
        if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
            this.geyserConfig.setAutoconfiguredRemote(true);
            // Don't use localhost if not listening on all interfaces
            if (!javaAddr.getHostString().equals("0.0.0.0") && !javaAddr.getHostString().equals("")) {
                this.geyserConfig.getRemote().setAddress(javaAddr.getHostString());
            }
            geyserConfig.getRemote().setPort(javaAddr.getPort());
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(javaAddr.getPort());
        }

        this.geyserLogger = new GeyserVelocityLogger(logger, geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        // Remove this in like a year
        try {
            // Should only exist on 1.0
            Class.forName("org.geysermc.floodgate.FloodgateAPI");
            geyserLogger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.outdated",
                    "https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/master/"));
            return;
        } catch (ClassNotFoundException ignored) {
        }

        if (geyserConfig.getRemote().getAuthType() == AuthType.FLOODGATE && proxyServer.getPluginManager().getPlugin("floodgate").isEmpty()) {
            geyserLogger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " "
                    + GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            return;
        } else if (geyserConfig.isAutoconfiguredRemote() && proxyServer.getPluginManager().getPlugin("floodgate").isPresent()) {
            // Floodgate installed means that the user wants Floodgate authentication
            geyserLogger.debug("Auto-setting to Floodgate authentication.");
            geyserConfig.getRemote().setAuthType(AuthType.FLOODGATE);
        }

        geyserConfig.loadFloodgate(this, proxyServer, configFolder.toFile());

        this.geyser = GeyserImpl.start(PlatformType.VELOCITY, this);

        this.geyserInjector = new GeyserVelocityInjector(proxyServer);
        // Will be initialized after the proxy has been bound

        this.geyserCommandManager = new GeyserVelocityCommandManager(geyser);
        this.commandManager.register("geyser", new GeyserVelocityCommandExecutor(geyser));
        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserPingPassthrough = new GeyserVelocityPingPassthrough(proxyServer);
        }
    }

    @Override
    public void onDisable() {
        if (geyser != null) {
            geyser.shutdown();
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
    }

    @Override
    public GeyserVelocityConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserVelocityLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public org.geysermc.geyser.command.CommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        onEnable();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        onDisable();
    }

    @Subscribe
    public void onProxyBound(ListenerBoundEvent event) {
        if (event.getListenerType() == ListenerType.MINECRAFT && geyserInjector != null) {
            // After this bound, we know that the channel initializer cannot change without it being ineffective for Velocity, too
            geyserInjector.initializeLocalChannel(this);
        }
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserVelocityDumpInfo(proxyServer);
    }

    @Nullable
    @Override
    public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }
}
