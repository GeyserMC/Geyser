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

package org.geysermc.geyser.platform.forge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.forge.command.GeyserForgeCommandExecutor;
import org.geysermc.geyser.platform.forge.world.GeyserForgeWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod(ModConstants.MOD_ID)
public class GeyserForgeMod implements GeyserBootstrap {
    private static GeyserForgeMod instance;

    private boolean reloading;

    private GeyserImpl geyser;
    private ModContainer mod;
    private Path dataFolder;
    private MinecraftServer server;

    private GeyserCommandManager geyserCommandManager;
    private GeyserForgeConfiguration geyserConfig;
    private GeyserForgeLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;
    private WorldManager geyserWorldManager;

    public GeyserForgeMod() {
        instance = this;

        mod = ModList.get().getModContainerById(ModConstants.MOD_ID).orElseThrow();

        this.onEnable();

        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            // Set as an event so we can get the proper IP and port if needed
            MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        }
    }

    @Override
    public void onEnable() {
        dataFolder = FMLPaths.CONFIGDIR.get().resolve("Geyser-Forge");
        if (!dataFolder.toFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.toFile().mkdir();
        }

        // Init dataFolder first as local language overrides call getConfigFolder()
        GeyserLocale.init(this);

        try {
            File configFile = FileUtils.fileOrCopiedFromResource(dataFolder.resolve("config.yml").toFile(), "config.yml",
                    (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserForgeConfiguration.class);
        } catch (IOException ex) {
            LogManager.getLogger("geyser=forge").error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return;
        }

        this.geyserLogger = new GeyserForgeLogger(geyserConfig.isDebugMode());

        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        this.geyser = GeyserImpl.load(PlatformType.FORGE, this);

        if (server == null) {
            // Server has yet to start
            // Register onDisable so players are properly kicked
            MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        } else {
            // Server has started and this is a reload
            startGeyser(this.server);
            reloading = false;
        }
    }

    /**
     * Initialize core Geyser.
     * A function, as it needs to be called in different places depending on if Geyser is being reloaded or not.
     *
     * @param server The minecraft server.
     */
    public void startGeyser(MinecraftServer server) {
        this.server = server;

        GeyserImpl.start();

        this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);

        this.geyserCommandManager = new GeyserCommandManager(geyser);
        this.geyserCommandManager.init();

        this.geyserWorldManager = new GeyserForgeWorldManager(server);

        // Start command building
        // Set just "geyser" as the help command
        GeyserForgeCommandExecutor helpExecutor = new GeyserForgeCommandExecutor(this, geyser,
                (GeyserCommand) geyser.commandManager().getCommands().get("help"));
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("geyser").executes(helpExecutor);

        // Register all subcommands as valid
        for (Map.Entry<String, Command> command : geyser.commandManager().getCommands().entrySet()) {
            GeyserForgeCommandExecutor executor = new GeyserForgeCommandExecutor(this, geyser, (GeyserCommand) command.getValue());
            builder.then(Commands.literal(command.getKey())
                    .executes(executor)
                    // Could also test for Bedrock but depending on when this is called it may backfire
                    .requires(executor::testPermission));
        }
        server.getCommands().getDispatcher().register(builder);
    }

    private void onServerStarted(ServerStartedEvent event) {
        this.startGeyser(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        this.onDisable();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        GeyserForgeUpdateListener.onPlayReady(event.getEntity());
    }

    @Override
    public void onDisable() {
        if (geyser != null) {
            geyser.shutdown();
            geyser = null;
        }
        if (!reloading) {
            this.server = null;
        }
    }

    @Override
    public GeyserConfiguration getGeyserConfig() {
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
        return new GeyserForgeDumpInfo(server);
    }

    @Override
    public String getMinecraftServerVersion() {
        return this.server.getServerVersion();
    }

    @NotNull
    @Override
    public String getServerBindAddress() {
        return this.server.getLocalIp();
    }

    @Override
    public int getServerPort() {
        return ((GeyserServerPortGetter) server).geyser$getServerPort();
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        Optional<? extends ModContainer> floodgate = ModList.get().getModContainerById("floodgate");
        if (floodgate.isPresent()) {
            geyserConfig.loadFloodgate(this, floodgate.orElse(null));
            return true;
        }
        return false;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public static GeyserForgeMod getInstance() {
        return instance;
    }
}
