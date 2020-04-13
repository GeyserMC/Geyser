/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.velocity;

import com.google.inject.Inject;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;

import org.geysermc.common.PlatformType;
import org.geysermc.common.bootstrap.IGeyserBootstrap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.platform.velocity.command.GeyserVelocityCommandExecutor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Plugin(id = "geyser", name = GeyserConnector.NAME + "-Velocity", version = GeyserConnector.VERSION_STATIC, url = "https://geysermc.org", authors = "GeyserMC")
public class GeyserVelocityPlugin implements IGeyserBootstrap {

    @Inject
    private Logger logger;

    @Inject
    private CommandManager commandManager;

    private GeyserVelocityConfiguration geyserConfig;
    private GeyserVelocityLogger geyserLogger;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        try {
            File configDir = new File("plugins/" + GeyserConnector.NAME + "-Velocity/");
            if (!configDir.exists())
                configDir.mkdir();
            File configFile = FileUtils.fileOrCopiedFromResource(new File(configDir, "config.yml"), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserVelocityConfiguration.class);
        } catch (IOException ex) {
            logger.warn("Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!", ex);
            ex.printStackTrace();
        }

        this.geyserLogger = new GeyserVelocityLogger(logger, geyserConfig.isDebugMode());
        this.connector = GeyserConnector.start(PlatformType.VELOCITY, this);

        this.commandManager.register(new GeyserVelocityCommandExecutor(connector), "geyser");
    }

    @Override
    public void onDisable() {
        connector.shutdown();
    }

    @Override
    public GeyserVelocityConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserVelocityLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        onEnable();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        onDisable();
    }
}
