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

package org.geysermc.geyser.platform.mod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.mod.command.GeyserModCommandExecutor;
import org.geysermc.geyser.platform.mod.platform.GeyserModPlatform;
import org.geysermc.geyser.platform.mod.world.GeyserModWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class GeyserModBootstrap implements GeyserBootstrap {
    private static GeyserModBootstrap instance;

    private final GeyserModPlatform platform;

    @Setter
    private boolean reloading;

    private GeyserImpl geyser;
    private Path dataFolder;
    private MinecraftServer server;

    private GeyserCommandManager geyserCommandManager;
    private GeyserModConfiguration geyserConfig;
    private GeyserModInjector geyserInjector;
    private GeyserModLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;
    private WorldManager geyserWorldManager;

    @Override
    public void onEnable() {
        instance = this;

        dataFolder = this.platform.dataFolder(this.platform.configPath());
        if (!dataFolder.toFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.toFile().mkdir();
        }

        // Init dataFolder first as local language overrides call getConfigFolder()
        GeyserLocale.init(this);

        try {
            File configFile = FileUtils.fileOrCopiedFromResource(dataFolder.resolve("config.yml").toFile(), "config.yml",
                    (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserModConfiguration.class);
        } catch (IOException ex) {
            LogManager.getLogger("geyser").error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return;
        }

        this.geyserLogger = new GeyserModLogger(geyserConfig.isDebugMode());

        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        this.geyser = GeyserImpl.load(this.platform.platformType(), this);

        this.geyserCommandManager = new GeyserCommandManager(geyser);
        this.geyserCommandManager.init();

        if (server == null) {
            // Server has started and this is the first time
            this.onInitialStartup();
        } else {
            // Server has started and this is a reload
            startGeyser(this.server);
            reloading = false;
        }
    }

    public abstract void onInitialStartup();

    /**
     * Initialize core Geyser.
     * A function, as it needs to be called in different places depending on if Geyser is being reloaded or not.
     *
     * @param server The minecraft server.
     */
    public void startGeyser(MinecraftServer server) {
        this.server = server;

        GeyserImpl.start();

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserPingPassthrough = new ModPingPassthrough(server, geyserLogger);
        }

        this.geyserWorldManager = new GeyserModWorldManager(server);

        // We want to do this late in the server startup process to allow other mods
        // To do their job injecting, then connect into *that*
        this.geyserInjector = new GeyserModInjector(server, this.platform);
        this.geyserInjector.initializeLocalChannel(this);

        // Start command building
        // Set just "geyser" as the help command
        GeyserModCommandExecutor helpExecutor = new GeyserModCommandExecutor(geyser,
                (GeyserCommand) geyser.commandManager().getCommands().get("help"));
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("geyser").executes(helpExecutor);

        // Register all subcommands as valid
        for (Map.Entry<String, Command> command : geyser.commandManager().getCommands().entrySet()) {
            GeyserModCommandExecutor executor = new GeyserModCommandExecutor(geyser, (GeyserCommand) command.getValue());
            builder.then(Commands.literal(command.getKey())
                    .executes(executor)
                    // Could also test for Bedrock but depending on when this is called it may backfire
                    .requires(executor::testPermission)
                    // Allows parsing of arguments; e.g. for /geyser dump logs or the connectiontest command
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                            .executes(context -> executor.runWithArgs(context, StringArgumentType.getString(context, "args")))
                            .requires(executor::testPermission)));
        }
        server.getCommands().getDispatcher().register(builder);

        // Register extension commands
        for (Map.Entry<Extension, Map<String, Command>> extensionMapEntry : geyser.commandManager().extensionCommands().entrySet()) {
            Map<String, Command> extensionCommands = extensionMapEntry.getValue();
            if (extensionCommands.isEmpty()) {
                continue;
            }

            // Register help command for just "/<extensionId>"
            GeyserModCommandExecutor extensionHelpExecutor = new GeyserModCommandExecutor(geyser,
                    (GeyserCommand) extensionCommands.get("help"));
            LiteralArgumentBuilder<CommandSourceStack> extCmdBuilder = Commands.literal(extensionMapEntry.getKey().description().id()).executes(extensionHelpExecutor);

            for (Map.Entry<String, Command> command : extensionCommands.entrySet()) {
                GeyserModCommandExecutor executor = new GeyserModCommandExecutor(geyser, (GeyserCommand) command.getValue());
                extCmdBuilder.then(Commands.literal(command.getKey())
                        .executes(executor)
                        .requires(executor::testPermission)
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .executes(context -> executor.runWithArgs(context, StringArgumentType.getString(context, "args")))
                                .requires(executor::testPermission)));
            }
            server.getCommands().getDispatcher().register(extCmdBuilder);
        }
    }

    @Override
    public void onDisable() {
        if (geyser != null) {
            geyser.shutdown();
            geyser = null;
        }
        if (!reloading) {
            this.server = null;
            this.geyserInjector.shutdown();
        }
    }

    @Override
    public GeyserModConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public GeyserCommandManager getGeyserCommandManager() {
        return geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Override
    public WorldManager getWorldManager() {
        return geyserWorldManager;
    }

    @Override
    public Path getConfigFolder() {
        return dataFolder;
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return this.platform.dumpInfo(this.server);
    }

    @Override
    public String getMinecraftServerVersion() {
        return this.server.getServerVersion();
    }

    @NonNull
    @Override
    public String getServerBindAddress() {
        String ip = this.server.getLocalIp();
        return ip != null ? ip : ""; // See issue #3812
    }

    @Override
    public int getServerPort() {
        return ((GeyserServerPortGetter) server).geyser$getServerPort();
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        return this.platform.testFloodgatePluginPresent(this);
    }

    @Nullable
    @Override
    public InputStream getResourceOrNull(String resource) {
        return this.platform.resolveResource(resource);
    }

    public abstract boolean hasPermission(@NonNull Player source, @NonNull String permissionNode);

    public abstract boolean hasPermission(@NonNull CommandSourceStack source, @NonNull String permissionNode, int permissionLevel);

    public static GeyserModBootstrap getInstance() {
        return instance;
    }
}
