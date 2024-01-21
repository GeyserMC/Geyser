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

import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.bungeecord.command.GeyserBungeeCommandExecutor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GeyserBungeePlugin extends Plugin implements GeyserBootstrap {

    private GeyserCommandManager geyserCommandManager;
    private GeyserBungeeConfiguration geyserConfig;
    private GeyserBungeeInjector geyserInjector;
    private GeyserBungeeLogger geyserLogger;
    private IGeyserPingPassthrough geyserBungeePingPassthrough;

    private GeyserImpl geyser;

    private static boolean INITIALIZED = false;

    @SuppressWarnings({"JavaReflectionMemberAccess", "ResultOfMethodCallIgnored"})
    @Override
    public void onLoad() {
        GeyserLocale.init(this);

        // Copied from ViaVersion.
        // https://github.com/ViaVersion/ViaVersion/blob/b8072aad86695cc8ec6f5e4103e43baf3abf6cc5/bungee/src/main/java/us/myles/ViaVersion/BungeePlugin.java#L43
        try {
            ProtocolConstants.class.getField("MINECRAFT_1_20_3");
        } catch (NoSuchFieldException e) {
            getLogger().warning("      / \\");
            getLogger().warning("     /   \\");
            getLogger().warning("    /  |  \\");
            getLogger().warning("   /   |   \\    " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_proxy", getProxy().getName()));
            getLogger().warning("  /         \\   " + GeyserLocale.getLocaleStringLog("geyser.may_not_work_as_intended_all_caps"));
            getLogger().warning(" /     o     \\");
            getLogger().warning("/_____________\\");
        }

        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"),
                    "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserBungeeConfiguration.class);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return;
        }

        this.geyserLogger = new GeyserBungeeLogger(getLogger(), geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        this.geyser = GeyserImpl.load(PlatformType.BUNGEECORD, this);
    }

    @Override
    public void onEnable() {

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

        // Big hack - Bungee does not provide us an event to listen to, so schedule a repeating
        // task that waits for a field to be filled which is set after the plugin enable
        // process is complete
        if (!INITIALIZED) {
            this.awaitStartupCompletion(0);
        } else {
            // No need to "wait" for startup completion, just start Geyser - we're reloading.
            this.postStartup();
        }
    }

    @SuppressWarnings("unchecked")
    private void awaitStartupCompletion(int tries) {
        // After 20 tries give up waiting. This will happen
        // just after 3 minutes approximately
        if (tries >= 20) {
            this.geyserLogger.warning("BungeeCord plugin startup is taking abnormally long, so Geyser is starting now. " +
                    "If all your plugins are loaded properly, this is a bug! " +
                    "If not, consider cutting down the amount of plugins on your proxy as it is causing abnormally slow starting times.");
            this.postStartup();
            return;
        }

        try {
            Field listenersField = BungeeCord.getInstance().getClass().getDeclaredField("listeners");
            listenersField.setAccessible(true);

            Collection<Channel> listeners = (Collection<Channel>) listenersField.get(BungeeCord.getInstance());
            if (listeners.isEmpty()) {
                this.getProxy().getScheduler().schedule(this, this::postStartup, tries, TimeUnit.SECONDS);
            } else {
                this.awaitStartupCompletion(++tries);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    private void postStartup() {
        GeyserImpl.start();

        if (!INITIALIZED) {
            this.geyserInjector = new GeyserBungeeInjector(this);
            this.geyserInjector.initializeLocalChannel(this);
        }

        this.geyserCommandManager = new GeyserCommandManager(geyser);
        this.geyserCommandManager.init();

        this.getProxy().getPluginManager().registerCommand(this, new GeyserBungeeCommandExecutor("geyser", this.geyser, this.geyserCommandManager.getCommands()));
        for (Map.Entry<Extension, Map<String, Command>> entry : this.geyserCommandManager.extensionCommands().entrySet()) {
            Map<String, Command> commands = entry.getValue();
            if (commands.isEmpty()) {
                continue;
            }

            this.getProxy().getPluginManager().registerCommand(this, new GeyserBungeeCommandExecutor(entry.getKey().description().id(), this.geyser, commands));
        }

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserBungeePingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserBungeePingPassthrough = new GeyserBungeePingPassthrough(getProxy());
        }

        INITIALIZED = true;
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
    public GeyserBungeeConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBungeeLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public GeyserCommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
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
}
