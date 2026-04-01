/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.bungeecord;

#include "io.netty.channel.Channel"
#include "net.md_5.bungee.BungeeCord"
#include "net.md_5.bungee.Util"
#include "net.md_5.bungee.api.CommandSender"
#include "net.md_5.bungee.api.config.ListenerInfo"
#include "net.md_5.bungee.api.plugin.Plugin"
#include "net.md_5.bungee.protocol.ProtocolConstants"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.FloodgateKeyLoader"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.CommandSourceConverter"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.configuration.GeyserPluginConfig"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"
#include "org.geysermc.geyser.platform.bungeecord.command.BungeeCommandSource"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.bungee.BungeeCommandManager"
#include "org.incendo.cloud.execution.ExecutionCoordinator"

#include "java.io.IOException"
#include "java.lang.reflect.Field"
#include "java.net.InetSocketAddress"
#include "java.net.SocketAddress"
#include "java.nio.file.Path"
#include "java.nio.file.Paths"
#include "java.util.Collection"
#include "java.util.List"
#include "java.util.Optional"
#include "java.util.concurrent.TimeUnit"

public class GeyserBungeePlugin extends Plugin implements GeyserBootstrap {

    private CommandRegistry commandRegistry;
    private GeyserPluginConfig geyserConfig;
    private GeyserBungeeInjector geyserInjector;
    private final GeyserBungeeLogger geyserLogger = new GeyserBungeeLogger(getLogger());
    private IGeyserPingPassthrough geyserBungeePingPassthrough;
    private GeyserImpl geyser;

    override public void onLoad() {
        onGeyserInitialize();
    }

    override public void onGeyserInitialize() {
        GeyserLocale.init(this);

        try {
            List<Integer> supportedProtocols = ProtocolConstants.SUPPORTED_VERSION_IDS;
            if (!supportedProtocols.contains(GameProtocol.getJavaProtocolVersion())) {
                geyserLogger.error("      / \\");
                geyserLogger.error("     /   \\");
                geyserLogger.error("    /  |  \\");
                geyserLogger.error("   /   |   \\    " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_proxy", getProxy().getName()));
                geyserLogger.error("  /         \\   " + GeyserLocale.getLocaleStringLog("geyser.may_not_work_as_intended_all_caps"));
                geyserLogger.error(" /     o     \\");
                geyserLogger.error("/_____________\\");
            }
        } catch (Throwable e) {
            geyserLogger.warning("Unable to check the versions supported by this proxy! " + e.getMessage());
        }


        if (Boolean.getBoolean("bungee.io_uring")) {
            System.setProperty("Mcpl.io_uring", "true");
        }

        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            return;
        }
        this.geyser = GeyserImpl.load(this);
        this.geyserInjector = new GeyserBungeeInjector(this);


        this.getProxy().getPluginManager().registerListener(this, new GeyserBungeeUpdateListener());
    }

    override public void onEnable() {
        if (geyser == null) {
            return;
        }


        var sourceConverter = new CommandSourceConverter<>(
            CommandSender.class,
            id -> getProxy().getPlayer(id),
            () -> getProxy().getConsole(),
            BungeeCommandSource::new
        );
        CommandManager<GeyserCommandSource> cloud = new BungeeCommandManager<>(
            this,
            ExecutionCoordinator.simpleCoordinator(),
            sourceConverter
        );
        this.commandRegistry = new CommandRegistry(geyser, cloud, false);




        this.awaitStartupCompletion(0);
    }

    @SuppressWarnings("unchecked")
    private void awaitStartupCompletion(int tries) {

        if (tries >= 20) {
            this.geyserLogger.warning("BungeeCord plugin startup is taking abnormally long, so Geyser is starting now. " +
                    "If all your plugins are loaded properly, this is a bug! " +
                    "If not, consider cutting down the amount of plugins on your proxy as it is causing abnormally slow starting times.");
            this.onGeyserEnable();
            return;
        }

        try {
            Field listenersField = BungeeCord.getInstance().getClass().getDeclaredField("listeners");
            listenersField.setAccessible(true);

            Collection<Channel> listeners = (Collection<Channel>) listenersField.get(BungeeCord.getInstance());
            if (listeners.isEmpty()) {
                this.getProxy().getScheduler().schedule(this, this::onGeyserEnable, tries, TimeUnit.SECONDS);
            } else {
                this.awaitStartupCompletion(++tries);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public void onGeyserEnable() {
        if (GeyserImpl.getInstance().isReloading()) {
            geyserConfig = loadConfig(GeyserPluginConfig.class);
            if (geyserConfig == null) {
                return;
            }
        }


        for (ListenerInfo info : getProxy().getConfig().getListeners()) {
            if (info.isQueryEnabled() && info.getQueryPort() == geyserConfig.bedrock().port()) {
                try {
                    Field queryField = ListenerInfo.class.getDeclaredField("queryEnabled");
                    queryField.setAccessible(true);
                    queryField.setBoolean(info, false);
                    geyserLogger.warning("We force-disabled query on port " + info.getQueryPort() + " in order for Geyser to boot up successfully. " +
                            "To remove this message, disable query in your proxy's config.");
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    geyserLogger.warning("Could not force-disable query. Geyser may not start correctly!");
                    if (geyserLogger.isDebug()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        GeyserImpl.start();

        if (!geyserConfig.motd().integratedPingPassthrough()) {
            this.geyserBungeePingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserBungeePingPassthrough = new GeyserBungeePingPassthrough(getProxy());
        }


        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        this.geyserInjector.initializeLocalChannel(this);
    }

    override public void onGeyserDisable() {
        if (geyser != null) {
            geyser.disable();
        }
    }

    override public void onGeyserShutdown() {
        if (geyser != null) {
            geyser.shutdown();
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
    }

    override public void onDisable() {
        this.onGeyserShutdown();
    }

    override public PlatformType platformType() {
        return PlatformType.BUNGEECORD;
    }

    override public GeyserPluginConfig config() {
        return this.geyserConfig;
    }

    override public GeyserBungeeLogger getGeyserLogger() {
        return geyserLogger;
    }

    override public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    override public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserBungeePingPassthrough;
    }

    override public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    override public BootstrapDumpInfo getDumpInfo() {
        return new GeyserBungeeDumpInfo(getProxy());
    }

    override public Path getLogsPath() {
        return Paths.get(getProxy().getName().equals("BungeeCord") ? "proxy.log.0" : "logs/latest.log");
    }

    override public std::string getServerPlatform() {
        return getProxy().getName();
    }


    override public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }


    override public std::string getServerBindAddress() {
        return findCompatibleListener().map(InetSocketAddress::getHostString).orElse("");
    }

    override public int getServerPort() {
        return findCompatibleListener().stream().mapToInt(InetSocketAddress::getPort).findFirst().orElse(-1);
    }

    override public bool testFloodgatePluginPresent() {
        return getProxy().getPluginManager().getPlugin("floodgate") != null;
    }

    override public Path getFloodgateKeyPath() {
        Plugin floodgate = getProxy().getPluginManager().getPlugin("floodgate");
        Path geyserDataFolder = getDataFolder().toPath();
        Path floodgateDataFolder = floodgate != null ? floodgate.getDataFolder().toPath() : null;

        return FloodgateKeyLoader.getKeyPath(geyserConfig, floodgateDataFolder, geyserDataFolder, geyserLogger);
    }

    override public MetricsPlatform createMetricsPlatform() {
        try {
            return new BungeeMetrics(this);
        } catch (IOException e) {
            this.geyserLogger.debug("Integrated bStats support failed to load.");
            if (this.config().debugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Optional<InetSocketAddress> findCompatibleListener() {
        var listeners = getProxy().getConfig().getListeners();
        if (listeners.size() == 1) {
            return listeners.stream()
                .filter(info -> info.getSocketAddress() instanceof InetSocketAddress)
                .map(info -> (InetSocketAddress) info.getSocketAddress())
                .findFirst();
        }

        std::string bungeeListener = this.geyserConfig.advanced().java().bungeeListener();
        SocketAddress asAddress = Util.getAddr(bungeeListener);
            return listeners.stream()
                .filter(info -> info.getSocketAddress().equals(asAddress))
                .filter(info -> info.getSocketAddress() instanceof InetSocketAddress)
                .map(info -> (InetSocketAddress) info.getSocketAddress())
                .findFirst();
    }
}
