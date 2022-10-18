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

package org.geysermc.geyser.platform.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.fabric.command.GeyserFabricCommandExecutor;
import org.geysermc.geyser.platform.fabric.command.GeyserFabricCommandManager;
import org.geysermc.geyser.platform.fabric.world.GeyserFabricWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class GeyserFabricMod implements ModInitializer, GeyserBootstrap {

    private static GeyserFabricMod instance;

    private boolean reloading;

    private GeyserImpl connector;
    private ModContainer mod;
    private Path dataFolder;
    private MinecraftServer server;

    /**
     * Commands that don't require any permission level to ran
     */
    private List<String> playerCommands;
    private final List<GeyserFabricCommandExecutor> commandExecutors = new ArrayList<>();

    private GeyserFabricCommandManager geyserCommandManager;
    private GeyserFabricConfiguration geyserConfig;
    private GeyserFabricLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;
    private WorldManager geyserWorldManager;

    @Override
    public void onInitialize() {
        instance = this;
        mod = FabricLoader.getInstance().getModContainer("geyser-fabric").orElseThrow();

        this.onEnable();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            // Set as an event so we can get the proper IP and port if needed
            ServerLifecycleEvents.SERVER_STARTED.register(this::startGeyser);
        }
    }

    @Override
    public void onEnable() {
        dataFolder = FabricLoader.getInstance().getConfigDir().resolve("Geyser-Fabric");
        if (!dataFolder.toFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.toFile().mkdir();
        }

        // Init dataFolder first as local language overrides call getConfigFolder()
        GeyserLocale.init(this);

        try {
            File configFile = FileUtils.fileOrCopiedFromResource(dataFolder.resolve("config.yml").toFile(), "config.yml",
                    (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserFabricConfiguration.class);
            File permissionsFile = fileOrCopiedFromResource(dataFolder.resolve("permissions.yml").toFile(), "permissions.yml");
            this.playerCommands = Arrays.asList(FileUtils.loadConfig(permissionsFile, GeyserFabricPermissions.class).getCommands());
        } catch (IOException ex) {
            LogManager.getLogger("geyser-fabric").error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return;
        }

        this.geyserLogger = new GeyserFabricLogger(geyserConfig.isDebugMode());

        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        if (server == null) {
            // Server has yet to start
            // Register onDisable so players are properly kicked
            ServerLifecycleEvents.SERVER_STOPPING.register((server) -> onDisable());
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

        if (this.geyserConfig.getRemote().address().equalsIgnoreCase("auto")) {
            this.geyserConfig.setAutoconfiguredRemote(true);
            String ip = server.getLocalIp();
            int port = ((GeyserServerPortGetter) server).geyser$getServerPort();
            if (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0")) {
                this.geyserConfig.getRemote().setAddress(ip);
            }
            this.geyserConfig.getRemote().setPort(port);
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(geyserConfig.getRemote().port());
        }

        Optional<ModContainer> floodgate = FabricLoader.getInstance().getModContainer("floodgate");
        boolean floodgatePresent = floodgate.isPresent();
        if (geyserConfig.getRemote().authType() == AuthType.FLOODGATE && !floodgatePresent) {
            geyserLogger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            return;
        } else if (geyserConfig.isAutoconfiguredRemote() && floodgatePresent) {
            // Floodgate installed means that the user wants Floodgate authentication
            geyserLogger.debug("Auto-setting to Floodgate authentication.");
            geyserConfig.getRemote().setAuthType(AuthType.FLOODGATE);
        }

        geyserConfig.loadFloodgate(this, floodgate.orElse(null));

        this.connector = GeyserImpl.load(PlatformType.FABRIC, this);
        GeyserImpl.start(); // shrug

        this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(connector);

        this.geyserCommandManager = new GeyserFabricCommandManager(connector);
        this.geyserCommandManager.init();

        this.geyserWorldManager = new GeyserFabricWorldManager(server);

        // Start command building
        // Set just "geyser" as the help command
        GeyserFabricCommandExecutor helpExecutor = new GeyserFabricCommandExecutor(connector,
                (GeyserCommand) connector.commandManager().getCommands().get("help"), !playerCommands.contains("help"));
        commandExecutors.add(helpExecutor);
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("geyser").executes(helpExecutor);

        // Register all subcommands as valid
        for (Map.Entry<String, Command> command : connector.commandManager().getCommands().entrySet()) {
            GeyserFabricCommandExecutor executor = new GeyserFabricCommandExecutor(connector, (GeyserCommand) command.getValue(),
                    !playerCommands.contains(command.getKey()));
            commandExecutors.add(executor);
            builder.then(Commands.literal(command.getKey()).executes(executor));
        }
        server.getCommands().getDispatcher().register(builder);
    }

    @Override
    public void onDisable() {
        if (connector != null) {
            connector.shutdown();
            connector = null;
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
        return new GeyserFabricDumpInfo(server);
    }

    @Override
    public String getMinecraftServerVersion() {
        return this.server.getServerVersion();
    }

    @Nullable
    @Override
    public InputStream getResourceOrNull(String resource) {
        // We need to handle this differently, because Fabric shares the classloader across multiple mods
        Path path = this.mod.getPath(resource);

        try {
            return path.getFileSystem()
                    .provider()
                    .newInputStream(path);
        } catch (IOException e) {
            return null;
        }
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    private File fileOrCopiedFromResource(File file, String name) throws IOException {
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream input = getResource(name);

            byte[] bytes = new byte[input.available()];

            //noinspection ResultOfMethodCallIgnored
            input.read(bytes);

            for(char c : new String(bytes).toCharArray()) {
                fos.write(c);
            }

            fos.flush();
            input.close();
            fos.close();
        }

        return file;
    }

    public List<GeyserFabricCommandExecutor> getCommandExecutors() {
        return commandExecutors;
    }

    public static GeyserFabricMod getInstance() {
        return instance;
    }
}
