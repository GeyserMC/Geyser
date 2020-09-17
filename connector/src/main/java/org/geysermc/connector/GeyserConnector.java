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

package org.geysermc.connector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.network.raknet.RakNetConstants;
import com.nukkitx.protocol.bedrock.BedrockServer;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.common.PlatformType;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.extension.ExtensionManager;
import org.geysermc.connector.metrics.Metrics;
import org.geysermc.connector.network.ConnectorServerEventHandler;
import org.geysermc.connector.network.remote.RemoteServer;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.BiomeTranslator;
import org.geysermc.connector.network.translators.EntityIdentifierRegistry;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.effect.EffectRegistry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.PotionMixRegistry;
import org.geysermc.connector.network.translators.item.RecipeRegistry;
import org.geysermc.connector.network.translators.sound.SoundHandlerRegistry;
import org.geysermc.connector.network.translators.sound.SoundRegistry;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.event.events.geyser.GeyserStopEvent;
import org.geysermc.connector.utils.DimensionUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.LocaleUtils;
import org.geysermc.connector.utils.ResourcePack;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class GeyserConnector {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(JsonParser.Feature.IGNORE_UNDEFINED).enable(JsonParser.Feature.ALLOW_COMMENTS).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final String NAME = "Geyser";
    public static final String VERSION = "DEV"; // A fallback for running in IDEs

    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    private final List<GeyserSession> players = new ArrayList<>();

    private static GeyserConnector instance;

    private RemoteServer remoteServer;
    @Setter
    private AuthType authType;

    private boolean shuttingDown = false;

    private final ScheduledExecutorService generalThreadPool;

    private BedrockServer bedrockServer;
    private PlatformType platformType;
    private GeyserBootstrap bootstrap;

    private final EventManager eventManager;
    private final ExtensionManager extensionManager;

    private final List<String> registeredPluginChannels = new ArrayList<>();

    private Metrics metrics;

    private GeyserConnector(PlatformType platformType, GeyserBootstrap bootstrap) {
        long startupTime = System.currentTimeMillis();

        instance = this;

        this.bootstrap = bootstrap;

        GeyserLogger logger = bootstrap.getGeyserLogger();
        GeyserConfiguration config = bootstrap.getGeyserConfig();

        this.platformType = platformType;

        logger.info("******************************************");
        logger.info("");
        logger.info(LanguageUtils.getLocaleStringLog("geyser.core.load", NAME, VERSION));
        logger.info("");
        logger.info("******************************************");

        this.generalThreadPool = Executors.newScheduledThreadPool(config.getGeneralThreadPool());

        logger.setDebug(config.isDebugMode());

        this.eventManager = new EventManager(this);
        this.extensionManager = new ExtensionManager(this, bootstrap.getConfigFolder().resolve("extensions").toFile());

        PacketTranslatorRegistry.init();

        /* Initialize translators and registries */
        BiomeTranslator.init();
        BlockTranslator.init();
        BlockEntityTranslator.init();
        EffectRegistry.init();
        EntityIdentifierRegistry.init();
        ItemRegistry.init();
        ItemTranslator.init();
        LocaleUtils.init();
        PotionMixRegistry.init();
        RecipeRegistry.init();
        SoundRegistry.init();
        SoundHandlerRegistry.init();

        ResourcePack.loadPacks();

        if (platformType != PlatformType.STANDALONE && config.getRemote().getAddress().equals("auto")) {
            // Set the remote address to localhost since that is where we are always connecting
            try {
                config.getRemote().setAddress(InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException ex) {
                logger.debug("Unknown host when trying to find localhost.");
                if (config.isDebugMode()) {
                    ex.printStackTrace();
                }
            }
        }
        String remoteAddress = config.getRemote().getAddress();
        int remotePort = config.getRemote().getPort();
        // Filters whether it is not an IP address or localhost, because otherwise it is not possible to find out an SRV entry.
        if ((config.isLegacyPingPassthrough() || platformType == PlatformType.STANDALONE) && !remoteAddress.matches(IP_REGEX) && !remoteAddress.equalsIgnoreCase("localhost")) {
            try {
                // Searches for a server address and a port from a SRV record of the specified host name
                InitialDirContext ctx = new InitialDirContext();
                Attribute attr = ctx.getAttributes("dns:///_minecraft._tcp." + remoteAddress, new String[]{"SRV"}).get("SRV");
                // size > 0 = SRV entry found
                if (attr != null && attr.size() > 0) {
                    String[] record = ((String) attr.get(0)).split(" ");
                    // Overwrites the existing address and port with that from the SRV record.
                    config.getRemote().setAddress(remoteAddress = record[3]);
                    config.getRemote().setPort(remotePort = Integer.parseInt(record[2]));
                    logger.debug("Found SRV record \"" + remoteAddress + ":" + remotePort + "\"");
                }
            } catch (Exception | NoClassDefFoundError ex) { // Check for a NoClassDefFoundError to prevent Android crashes
                logger.debug("Exception while trying to find an SRV record for the remote host.");
                if (config.isDebugMode())
                    ex.printStackTrace(); // Otherwise we can get a stack trace for any domain that doesn't have an SRV record
            }
        }

        remoteServer = new RemoteServer(config.getRemote().getAddress(), remotePort);
        authType = AuthType.getByName(config.getRemote().getAuthType());

        if (config.isAboveBedrockNetherBuilding())
            DimensionUtils.changeBedrockNetherId(); // Apply End dimension ID workaround to Nether

        // https://github.com/GeyserMC/Geyser/issues/957
        RakNetConstants.MAXIMUM_MTU_SIZE = (short) config.getMtu();
        logger.debug("Setting MTU to " + config.getMtu());

        bedrockServer = new BedrockServer(new InetSocketAddress(config.getBedrock().getAddress(), config.getBedrock().getPort()));
        bedrockServer.setHandler(new ConnectorServerEventHandler(this));
        bedrockServer.bind().whenComplete((avoid, throwable) -> {
            if (throwable == null) {
                logger.info(LanguageUtils.getLocaleStringLog("geyser.core.start", config.getBedrock().getAddress(), String.valueOf(config.getBedrock().getPort())));
            } else {
                logger.severe(LanguageUtils.getLocaleStringLog("geyser.core.fail", config.getBedrock().getAddress(), String.valueOf(config.getBedrock().getPort())));
                throwable.printStackTrace();
            }
        }).join();

        if (config.getMetrics().isEnabled()) {
            metrics = new Metrics(this, "GeyserMC", config.getMetrics().getUniqueId(), false, java.util.logging.Logger.getLogger(""));
            metrics.addCustomChart(new Metrics.SingleLineChart("servers", () -> 1));
            metrics.addCustomChart(new Metrics.SingleLineChart("players", players::size));
            metrics.addCustomChart(new Metrics.SimplePie("authMode", authType.name()::toLowerCase));
            metrics.addCustomChart(new Metrics.SimplePie("platform", platformType::getPlatformName));
        }

        boolean isGui = false;
        // This will check if we are in standalone and get the 'useGui' variable from there
        if (platformType == PlatformType.STANDALONE) {
            try {
                Class<?> cls = Class.forName("org.geysermc.platform.standalone.GeyserStandaloneBootstrap");
                isGui = (boolean) cls.getMethod("isUseGui").invoke(cls.cast(bootstrap));
            } catch (Exception e) {
                logger.debug("Failed detecting if standalone is using a GUI; if this is a GeyserConnect instance this can be safely ignored.");
            }
        }

        // Enable Plugins
        extensionManager.enableExtensions();

        double completeTime = (System.currentTimeMillis() - startupTime) / 1000D;
        String message = LanguageUtils.getLocaleStringLog("geyser.core.finish.done", new DecimalFormat("#.###").format(completeTime)) + " ";
        if (isGui) {
            message += LanguageUtils.getLocaleStringLog("geyser.core.finish.gui");
        } else {
            message += LanguageUtils.getLocaleStringLog("geyser.core.finish.console");
        }
        logger.info(message);
        
        if (platformType == PlatformType.STANDALONE) {
            logger.warning(LanguageUtils.getLocaleStringLog("geyser.core.movement_warn"));
        }
    }

    public void shutdown() {
        bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown"));
        shuttingDown = true;

        // Trigger GeyserStop Events
        eventManager.triggerEvent(new GeyserStopEvent());

        // Disable Plugins
        extensionManager.disableExtensions();

        if (players.size() >= 1) {
            bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.kick.log", players.size()));

            // Make a copy to prevent ConcurrentModificationException
            final List<GeyserSession> tmpPlayers = new ArrayList<>(players);
            for (GeyserSession playerSession : tmpPlayers) {
                playerSession.disconnect(LanguageUtils.getPlayerLocaleString("geyser.core.shutdown.kick.message", playerSession.getClientData().getLanguageCode()));
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    // Simulate a long-running Job
                    try {
                        while (true) {
                            if (players.size() == 0) {
                                return;
                            }

                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });

            // Block and wait for the future to complete
            try {
                future.get();
                bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.kick.done"));
            } catch (Exception e) {
                // Quietly fail
            }
        }

        generalThreadPool.shutdown();
        bedrockServer.close();
        players.clear();
        remoteServer = null;
        authType = null;
        this.getCommandManager().getCommands().clear();

        bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.done"));
    }

    public void addPlayer(GeyserSession player) {
        players.add(player);
    }

    public void removePlayer(GeyserSession player) {
        players.remove(player);
    }

    public static GeyserConnector start(PlatformType platformType, GeyserBootstrap bootstrap) {
        return new GeyserConnector(platformType, bootstrap);
    }

    public void reload() {
        shutdown();
        bootstrap.onEnable();
    }

    public GeyserLogger getLogger() {
        return bootstrap.getGeyserLogger();
    }

    public GeyserConfiguration getConfig() {
        return bootstrap.getGeyserConfig();
    }

    public CommandManager getCommandManager() {
        return bootstrap.getGeyserCommandManager();
    }

    public WorldManager getWorldManager() {
        return bootstrap.getWorldManager();
    }

    /**
     * Get the production status of the current runtime.
     * Will return true if the version number is not 'DEV'.
     * Should only happen in compiled jars.
     *
     * @return If we are in a production build/environment
     */
    public boolean isProduction() {
        //noinspection ConstantConditions
        return !"DEV".equals(GeyserConnector.VERSION);
    }

    /**
     * Register a Plugin Channel
     *
     * This will maintain what channels are registered and ensure new connections and existing connections
     * are registered correctly
     *
     * @param channel Channel to register
     */
    public void registerPluginChannel(String channel) {
        if (registeredPluginChannels.contains(channel)) {
            return;
        }

        registeredPluginChannels.add(channel);
        for ( GeyserSession session : players) {
            session.registerPluginChannel(channel);
        }
    }

    /**
     * Unregister a Plugin Channel
     *
     * This will maintain what channels are registered and ensure new connections and existing connections
     * are registered correctly
     *
     * @param channel Channel to unregister
     */
    public void unregisterPluginChannel(String channel) {
        if (!registeredPluginChannels.contains(channel)) {
            return;
        }

        registeredPluginChannels.remove(channel);
        for ( GeyserSession session : players ) {
            session.unregisterPluginChannel(channel);
        }
    }

    public static GeyserConnector getInstance() {
        return instance;
    }
}
