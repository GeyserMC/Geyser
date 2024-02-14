/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.platform.viaproxy;

import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.viaproxy.cli.options.Options;
import org.apache.logging.log4j.Logger;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.LoopbackUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class GeyserViaProxyBootstrap implements GeyserBootstrap {

    private final File rootFolder;
    private final GeyserViaProxyLogger logger;
    private GeyserViaProxyConfiguration config;

    private GeyserImpl geyser;
    private GeyserCommandManager commandManager;
    private IGeyserPingPassthrough pingPassthrough;

    public GeyserViaProxyBootstrap(final Logger logger, final File rootFolder) {
        this.logger = new GeyserViaProxyLogger(logger);
        this.rootFolder = rootFolder;
    }

    @Override
    public void onGeyserInitialize() {
        try {
            final File configFile = FileUtils.fileOrCopiedFromResource(new File(this.rootFolder, "config.yml"), "config.yml", s -> s.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.config = FileUtils.loadConfig(configFile, GeyserViaProxyConfiguration.class);
        } catch (IOException e) {
            this.logger.severe(GeyserLocale.getLocaleStringLog("geyser.config.failed"), e);
            return;
        }

        config.getRemote().setAuthType(Files.isRegularFile(this.config.getFloodgateKeyPath()) ? AuthType.FLOODGATE : AuthType.OFFLINE);
        GeyserConfiguration.checkGeyserConfiguration(this.config, this.logger);

        this.geyser = GeyserImpl.load(PlatformType.VIAPROXY, this);
        LoopbackUtil.checkAndApplyLoopback(this.logger);
    }

    @Override
    public void onGeyserEnable() {
        GeyserImpl.start();

        this.commandManager = new GeyserCommandManager(this.geyser);
        this.commandManager.init();

        if (Options.PROTOCOL_VERSION != null && Options.PROTOCOL_VERSION.newerThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            // Only initialize the ping passthrough if the protocol version is above beta 1.7.3, as that's when the status protocol was added
            this.pingPassthrough = GeyserLegacyPingPassthrough.init(this.geyser);
        }
    }

    @Override
    public void onGeyserDisable() {
        this.geyser.disable();
    }

    @Override
    public void onGeyserShutdown() {
        this.geyser.shutdown();
    }

    @Override
    public GeyserConfiguration getGeyserConfig() {
        return this.config;
    }

    @Override
    public GeyserLogger getGeyserLogger() {
        return this.logger;
    }

    @Override
    public GeyserCommandManager getGeyserCommandManager() {
        return this.commandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return this.pingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return this.rootFolder.toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserViaProxyDumpInfo();
    }

    @NotNull
    @Override
    public String getServerBindAddress() {
        if (Options.BIND_ADDRESS instanceof InetSocketAddress socketAddress) {
            return socketAddress.getHostString();
        } else {
            throw new IllegalStateException("Unsupported bind address type: " + Options.BIND_ADDRESS.getClass().getName());
        }
    }

    @Override
    public int getServerPort() {
        if (Options.BIND_ADDRESS instanceof InetSocketAddress socketAddress) {
            return socketAddress.getPort();
        } else {
            throw new IllegalStateException("Unsupported bind address type: " + Options.BIND_ADDRESS.getClass().getName());
        }
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        return false;
    }

}
