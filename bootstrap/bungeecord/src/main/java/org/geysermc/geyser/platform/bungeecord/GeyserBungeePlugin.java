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

import io.netty.buffer.AdaptiveByteBufAllocator;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.bungeecord.command.BungeeCommandSource;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bungee.BungeeCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GeyserBungeePlugin extends Plugin implements GeyserBootstrap {

    private CommandRegistry commandRegistry;
    private GeyserBungeeConfiguration geyserConfig;
    private GeyserBungeeInjector geyserInjector;
    private final GeyserBungeeLogger geyserLogger = new GeyserBungeeLogger(getLogger());
    private IGeyserPingPassthrough geyserBungeePingPassthrough;
    private GeyserImpl geyser;

    @Override
    public void onLoad() {
        onGeyserInitialize();
    }

    @Override
    public void onGeyserInitialize() {
        GeyserLocale.init(this);

        // TODO remove when this isn't an issue anymore
        boolean adaptiveAllocatorUsed = System.getProperty("io.netty.allocator.type") == null && ByteBufAllocator.DEFAULT instanceof AdaptiveByteBufAllocator;

        try {
            List<Integer> supportedProtocols = ProtocolConstants.SUPPORTED_VERSION_IDS;
            if (!supportedProtocols.contains(GameProtocol.getJavaProtocolVersion()) || adaptiveAllocatorUsed) {
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

        // See https://github.com/SpigotMC/BungeeCord/blob/e62fc6c2916a991e00177c580986d8b1a22fdb41/proxy/src/main/java/net/md_5/bungee/netty/PipelineUtils.java#L138
        if (Boolean.getBoolean("bungee.io_uring")) {
            System.setProperty("Mcpl.io_uring", "true");
        }

        if (!this.loadConfig()) {
            return;
        }
        this.geyserLogger.setDebug(geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        this.geyser = GeyserImpl.load(PlatformType.BUNGEECORD, this);
        this.geyserInjector = new GeyserBungeeInjector(this);

        // Registration of listeners occurs only once
        this.getProxy().getPluginManager().registerListener(this, new GeyserBungeeUpdateListener());
    }

    @Override
    public void onEnable() {
        if (geyser == null) {
            return; // Config did not load properly!
        }

        // After Geyser initialize for parity with other platforms.
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
        this.commandRegistry = new CommandRegistry(geyser, cloud, false); // applying root permission would be a breaking change because we can't register permission defaults

        // Big hack - Bungee does not provide us an event to listen to, so schedule a repeating
        // task that waits for a field to be filled which is set after the plugin enable
        // process is complete
        this.awaitStartupCompletion(0);
    }

    @SuppressWarnings("unchecked")
    private void awaitStartupCompletion(int tries) {
        // After 20 tries give up waiting. This will happen just after 3 minutes approximately
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
            if (!loadConfig()) {
                return;
            }
            this.geyserLogger.setDebug(geyserConfig.isDebugMode());
            GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        }

        // Force-disable query if enabled, or else Geyser won't enable
        for (ListenerInfo info : getProxy().getConfig().getListeners()) {
            if (info.isQueryEnabled() && info.getQueryPort() == geyserConfig.getBedrock().port()) {
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

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserBungeePingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserBungeePingPassthrough = new GeyserBungeePingPassthrough(getProxy());
        }

        // No need to re-register commands or re-init injector when reloading
        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        this.geyserInjector.initializeLocalChannel(this);
    }

    @Override
    public void onGeyserDisable() {
        if (geyser != null) {
            geyser.disable();
        }
    }

    @Override
    public void onGeyserShutdown() {
        if (geyser != null) {
            geyser.shutdown();
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
    }

    @Override
    public void onDisable() {
        this.onGeyserShutdown();
    }

    @Override
    public GeyserBungeeConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBungeeLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserBungeePingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserBungeeDumpInfo(getProxy());
    }

    @Override
    public Path getLogsPath() {
        return Paths.get(getProxy().getName().equals("BungeeCord") ? "proxy.log.0" : "logs/latest.log");
    }

    @Nullable
    @Override
    public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }

    @NonNull
    @Override
    public String getServerBindAddress() {
        return findCompatibleListener().map(InetSocketAddress::getHostString).orElse("");
    }

    @Override
    public int getServerPort() {
        return findCompatibleListener().stream().mapToInt(InetSocketAddress::getPort).findFirst().orElse(-1);
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        if (getProxy().getPluginManager().getPlugin("floodgate") != null) {
            geyserConfig.loadFloodgate(this);
            return true;
        }
        return false;
    }

    private Optional<InetSocketAddress> findCompatibleListener() {
        return getProxy().getConfig().getListeners().stream()
                .filter(info -> info.getSocketAddress() instanceof InetSocketAddress)
                .map(info -> (InetSocketAddress) info.getSocketAddress())
                .findFirst();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean loadConfig() {
        try {
            if (!getDataFolder().exists()) //noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdir();
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"),
                    "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserBungeeConfiguration.class);
        } catch (IOException ex) {
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
