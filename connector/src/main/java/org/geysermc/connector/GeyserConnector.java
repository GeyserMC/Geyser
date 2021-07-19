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

package org.geysermc.connector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.nukkitx.network.raknet.RakNetConstants;
import com.nukkitx.network.util.EventLoops;
import com.nukkitx.protocol.bedrock.BedrockServer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.metrics.Metrics;
import org.geysermc.connector.network.ConnectorServerEventHandler;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.registry.BlockRegistries;
import org.geysermc.connector.registry.Registries;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.skin.FloodgateSkinUploader;
import org.geysermc.connector.utils.*;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.AesKeyProducer;
import org.geysermc.floodgate.crypto.Base64Topping;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.news.NewsItemAction;
import org.geysermc.floodgate.time.TimeSyncer;
import org.jetbrains.annotations.Contract;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class GeyserConnector {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.IGNORE_UNDEFINED)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

    public static final String NAME = "Geyser";
    public static final String GIT_VERSION = "DEV"; // A fallback for running in IDEs
    public static final String VERSION = "DEV"; // A fallback for running in IDEs
    public static final String MINECRAFT_VERSION = MinecraftConstants.GAME_VERSION; // Change if multiple version strings are supported

    /**
     * Oauth client ID for Microsoft authentication
     */
    public static final String OAUTH_CLIENT_ID = "204cefd1-4818-4de1-b98d-513fae875d88";

    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    private final List<GeyserSession> players = new ArrayList<>();

    private static GeyserConnector instance;

    /**
     * This is used in GeyserConnect to stop the bedrock server binding to a port
     */
    @Setter
    private static boolean shouldStartListener = true;

    @Setter
    private AuthType defaultAuthType;

    private final TimeSyncer timeSyncer;
    private FloodgateCipher cipher;
    private FloodgateSkinUploader skinUploader;
    private final NewsHandler newsHandler;

    private boolean shuttingDown = false;

    private final ScheduledExecutorService generalThreadPool;

    private final BedrockServer bedrockServer;
    private final PlatformType platformType;
    private final GeyserBootstrap bootstrap;

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

        PacketTranslatorRegistry.init();

        /* Initialize translators and registries */
        BlockRegistries.init();
        Registries.init();

        ItemTranslator.init();
        LocaleUtils.init();

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
                config.getRemote().setAddress(InetAddress.getLoopbackAddress().getHostAddress());
            }
        }
        String remoteAddress = config.getRemote().getAddress();
        // Filters whether it is not an IP address or localhost, because otherwise it is not possible to find out an SRV entry.
        if (!remoteAddress.matches(IP_REGEX) && !remoteAddress.equalsIgnoreCase("localhost")) {
            int remotePort;
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

        defaultAuthType = AuthType.getByName(config.getRemote().getAuthType());

        TimeSyncer timeSyncer = null;
        if (defaultAuthType == AuthType.FLOODGATE) {
            timeSyncer = new TimeSyncer(Constants.NTP_SERVER);
            try {
                Key key = new AesKeyProducer().produceFrom(config.getFloodgateKeyPath());
                cipher = new AesCipher(new Base64Topping());
                cipher.init(key);
                logger.info(LanguageUtils.getLocaleStringLog("geyser.auth.floodgate.loaded_key"));
                skinUploader = new FloodgateSkinUploader(this).start();
            } catch (Exception exception) {
                logger.severe(LanguageUtils.getLocaleStringLog("geyser.auth.floodgate.bad_key"), exception);
            }
        }
        this.timeSyncer = timeSyncer;

        String branch = "unknown";
        int buildNumber = -1;
        if (this.isProductionEnvironment()) {
            try {
                Properties gitProperties = new Properties();
                gitProperties.load(FileUtils.getResource("git.properties"));
                branch = gitProperties.getProperty("git.branch");
                String build = gitProperties.getProperty("git.build.number");
                if (build != null) {
                    buildNumber = Integer.parseInt(build);
                }
            } catch (Throwable e) {
                logger.error("Failed to read git.properties", e);
            }
        } else {
            logger.debug("Not getting git properties for the news handler as we are in a development environment.");
        }
        newsHandler = new NewsHandler(branch, buildNumber);

        CooldownUtils.setDefaultShowCooldown(config.getShowCooldown());
        DimensionUtils.changeBedrockNetherId(config.isAboveBedrockNetherBuilding()); // Apply End dimension ID workaround to Nether
        SkullBlockEntityTranslator.ALLOW_CUSTOM_SKULLS = config.isAllowCustomSkulls();

        // https://github.com/GeyserMC/Geyser/issues/957
        RakNetConstants.MAXIMUM_MTU_SIZE = (short) config.getMtu();
        logger.debug("Setting MTU to " + config.getMtu());

        boolean enableProxyProtocol = config.getBedrock().isEnableProxyProtocol();
        bedrockServer = new BedrockServer(
                new InetSocketAddress(config.getBedrock().getAddress(), config.getBedrock().getPort()),
                1,
                EventLoops.commonGroup(),
                enableProxyProtocol
        );

        if (config.isDebugMode()) {
            logger.debug("EventLoop type: " + EventLoops.getChannelType());
            if (EventLoops.getChannelType() == EventLoops.ChannelType.NIO) {
                if (System.getProperties().contains("disableNativeEventLoop")) {
                    logger.debug("EventLoop type is NIO because native event loops are disabled.");
                } else {
                    logger.debug("Reason for no Epoll: " + Epoll.unavailabilityCause().toString());
                    logger.debug("Reason for no KQueue: " + KQueue.unavailabilityCause().toString());
                }
            }
        }

        bedrockServer.setHandler(new ConnectorServerEventHandler(this));

        if (shouldStartListener) {
            bedrockServer.bind().whenComplete((avoid, throwable) -> {
                if (throwable == null) {
                    logger.info(LanguageUtils.getLocaleStringLog("geyser.core.start", config.getBedrock().getAddress(), String.valueOf(config.getBedrock().getPort())));
                } else {
                    logger.severe(LanguageUtils.getLocaleStringLog("geyser.core.fail", config.getBedrock().getAddress(), String.valueOf(config.getBedrock().getPort())));
                    throwable.printStackTrace();
                }
            }).join();
        }

        if (config.getMetrics().isEnabled()) {
            metrics = new Metrics(this, "GeyserMC", config.getMetrics().getUniqueId(), false, java.util.logging.Logger.getLogger(""));
            metrics.addCustomChart(new Metrics.SingleLineChart("players", players::size));
            // Prevent unwanted words best we can
            metrics.addCustomChart(new Metrics.SimplePie("authMode", () -> AuthType.getByName(config.getRemote().getAuthType()).toString().toLowerCase()));
            metrics.addCustomChart(new Metrics.SimplePie("platform", platformType::getPlatformName));
            metrics.addCustomChart(new Metrics.SimplePie("defaultLocale", LanguageUtils::getDefaultLocale));
            metrics.addCustomChart(new Metrics.SimplePie("version", () -> GeyserConnector.VERSION));
            metrics.addCustomChart(new Metrics.AdvancedPie("playerPlatform", () -> {
                Map<String, Integer> valueMap = new HashMap<>();
                for (GeyserSession session : players) {
                    if (session == null) continue;
                    if (session.getClientData() == null) continue;
                    String os = session.getClientData().getDeviceOs().toString();
                    if (!valueMap.containsKey(os)) {
                        valueMap.put(os, 1);
                    } else {
                        valueMap.put(os, valueMap.get(os) + 1);
                    }
                }
                return valueMap;
            }));
            metrics.addCustomChart(new Metrics.AdvancedPie("playerVersion", () -> {
                Map<String, Integer> valueMap = new HashMap<>();
                for (GeyserSession session : players) {
                    if (session == null) continue;
                    if (session.getClientData() == null) continue;
                    String version = session.getClientData().getGameVersion();
                    if (!valueMap.containsKey(version)) {
                        valueMap.put(version, 1);
                    } else {
                        valueMap.put(version, valueMap.get(version) + 1);
                    }
                }
                return valueMap;
            }));

            String minecraftVersion = bootstrap.getMinecraftServerVersion();
            if (minecraftVersion != null) {
                Map<String, Map<String, Integer>> versionMap = new HashMap<>();
                Map<String, Integer> platformMap = new HashMap<>();
                platformMap.put(platformType.getPlatformName(), 1);
                versionMap.put(minecraftVersion, platformMap);

                metrics.addCustomChart(new Metrics.DrilldownPie("minecraftServerVersion", () -> {
                    // By the end, we should return, for example:
                    // 1.16.5 => (Spigot, 1)
                    return versionMap;
                }));
            }

            // The following code can be attributed to the PaperMC project
            // https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0005-Paper-Metrics.patch#L614
            metrics.addCustomChart(new Metrics.DrilldownPie("javaVersion", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>();
                String javaVersion = System.getProperty("java.version");
                Map<String, Integer> entry = new HashMap<>();
                entry.put(javaVersion, 1);

                // http://openjdk.java.net/jeps/223
                // Java decided to change their versioning scheme and in doing so modified the
                // java.version system property to return $major[.$minor][.$security][-ea], as opposed to
                // 1.$major.0_$identifier we can handle pre-9 by checking if the "major" is equal to "1",
                // otherwise, 9+
                String majorVersion = javaVersion.split("\\.")[0];
                String release;

                int indexOf = javaVersion.lastIndexOf('.');

                if (majorVersion.equals("1")) {
                    release = "Java " + javaVersion.substring(0, indexOf);
                } else {
                    // of course, it really wouldn't be all that simple if they didn't add a quirk, now
                    // would it valid strings for the major may potentially include values such as -ea to
                    // denote a pre release
                    Matcher versionMatcher = Pattern.compile("\\d+").matcher(majorVersion);
                    if (versionMatcher.find()) {
                        majorVersion = versionMatcher.group(0);
                    }
                    release = "Java " + majorVersion;
                }
                map.put(release, entry);
                return map;
            }));
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

        newsHandler.handleNews(null, NewsItemAction.ON_SERVER_STARTED);
    }

    public void shutdown() {
        bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown"));
        shuttingDown = true;

        if (players.size() >= 1) {
            bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.kick.log", players.size()));

            // Make a copy to prevent ConcurrentModificationException
            final List<GeyserSession> tmpPlayers = new ArrayList<>(players);
            for (GeyserSession playerSession : tmpPlayers) {
                playerSession.disconnect(LanguageUtils.getPlayerLocaleString("geyser.core.shutdown.kick.message", playerSession.getLocale()));
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
        if (timeSyncer != null) {
            timeSyncer.shutdown();
        }
        if (skinUploader != null) {
            skinUploader.close();
        }
        newsHandler.shutdown();
        players.clear();
        defaultAuthType = null;
        this.getCommandManager().getCommands().clear();

        bootstrap.getGeyserLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.done"));
    }

    public void addPlayer(GeyserSession player) {
        players.add(player);
    }

    public void removePlayer(GeyserSession player) {
        players.remove(player);
    }

    /**
     * Gets a player by their current UUID
     *
     * @param uuid the uuid
     * @return the player or <code>null</code> if there is no player online with this UUID
     */
    @Contract("null -> null")
    public GeyserSession getPlayerByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        for (GeyserSession session : players) {
            if (uuid.equals(session.getPlayerEntity().getUuid())) {
                return session;
            }
        }

        return null;
    }

    /**
     * Gets a player by their Xbox user identifier
     *
     * @param xuid the Xbox user identifier
     * @return the player or <code>null</code> if there is no player online with this xuid
     */
    @SuppressWarnings("unused") // API usage
    public GeyserSession getPlayerByXuid(String xuid) {
        for (GeyserSession session : players) {
            if (session.getAuthData() != null && session.getAuthData().getXboxUUID().equals(xuid)) {
                return session;
            }
        }

        return null;
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

    public TimeSyncer getTimeSyncer() {
        return timeSyncer;
    }

    /**
     * Returns false if this Geyser instance is running in an IDE. This only needs to be used in cases where files
     * expected to be in a jarfile are not present.
     *
     * @return true if the version number is not 'DEV'.
     */
    public boolean isProductionEnvironment() {
        //noinspection ConstantConditions - changes in production
        return !"DEV".equals(GeyserConnector.VERSION);
    }

    /**
     * Whether to use XML reflections in the jar or manually find the reflections.
     * Will return true if in production and the platform is not Fabric.
     * On Fabric - it complains about being unable to create a default XMLReader.
     * On other platforms this should only be true in compiled jars.
     *
     * @return whether to use XML reflections
     */
    public boolean useXmlReflections() {
        return !this.getPlatformType().equals(PlatformType.FABRIC) && isProductionEnvironment();
    }

    public static GeyserConnector getInstance() {
        return instance;
    }
}
