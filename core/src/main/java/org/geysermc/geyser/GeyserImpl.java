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

package org.geysermc.geyser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.packetlib.tcp.TcpSession;
import com.nukkitx.network.raknet.RakNetConstants;
import com.nukkitx.network.util.EventLoops;
import com.nukkitx.protocol.bedrock.BedrockServer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.Geyser;
import org.geysermc.common.PlatformType;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.AesKeyProducer;
import org.geysermc.floodgate.crypto.Base64Topping;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.news.NewsItemAction;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.event.GeyserEventBus;
import org.geysermc.geyser.extension.GeyserExtensionManager;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.network.ConnectorServerEventHandler;
import org.geysermc.geyser.pack.ResourcePack;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.PendingMicrosoftAuthentication;
import org.geysermc.geyser.session.SessionManager;
import org.geysermc.geyser.skin.FloodgateSkinUploader;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class GeyserImpl implements GeyserApi {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.IGNORE_UNDEFINED)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

    public static final String NAME = "Geyser";
    public static final String GIT_VERSION = "${gitVersion}"; // A fallback for running in IDEs
    public static final String VERSION = "${version}"; // A fallback for running in IDEs

    public static final String BUILD_NUMBER = "${buildNumber}";
    public static final String BRANCH = "${branch}";

    /**
     * Oauth client ID for Microsoft authentication
     */
    public static final String OAUTH_CLIENT_ID = "204cefd1-4818-4de1-b98d-513fae875d88";

    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    private final SessionManager sessionManager = new SessionManager();

    /**
     * This is used in GeyserConnect to stop the bedrock server binding to a port
     */
    @Setter
    private static boolean shouldStartListener = true;

    private FloodgateCipher cipher;
    private FloodgateSkinUploader skinUploader;
    private NewsHandler newsHandler;

    private volatile boolean shuttingDown = false;

    private ScheduledExecutorService scheduledThread;

    private BedrockServer bedrockServer;
    private final PlatformType platformType;
    private final GeyserBootstrap bootstrap;

    private final EventBus eventBus;
    private final GeyserExtensionManager extensionManager;

    private Metrics metrics;

    private PendingMicrosoftAuthentication pendingMicrosoftAuthentication;
    @Getter(AccessLevel.NONE)
    private Map<String, String> savedRefreshTokens;

    private static GeyserImpl instance;

    private GeyserImpl(PlatformType platformType, GeyserBootstrap bootstrap) {
        instance = this;

        Geyser.set(this);

        this.platformType = platformType;
        this.bootstrap = bootstrap;

        long startupTime = System.currentTimeMillis();

        GeyserLocale.finalizeDefaultLocale(this);
        GeyserLogger logger = bootstrap.getGeyserLogger();

        logger.info("******************************************");
        logger.info("");
        logger.info(GeyserLocale.getLocaleStringLog("geyser.core.load", NAME, VERSION));
        logger.info("");
        logger.info("******************************************");

        /* Initialize event bus */
        this.eventBus = new GeyserEventBus();

        /* Load Extensions */
        this.extensionManager = new GeyserExtensionManager();
        this.extensionManager.init();

        this.extensionManager.enableExtensions();
        this.eventBus.fire(new GeyserPreInitializeEvent(this.extensionManager, this.eventBus));

        /* Initialize registries */
        Registries.init();
        BlockRegistries.init();

        /* Initialize translators */
        EntityDefinitions.init();
        ItemTranslator.init();
        MessageTranslator.init();
        MinecraftLocale.init();

        start();

        GeyserConfiguration config = bootstrap.getGeyserConfig();

        boolean isGui = false;
        // This will check if we are in standalone and get the 'useGui' variable from there
        if (platformType == PlatformType.STANDALONE) {
            try {
                Class<?> cls = Class.forName("org.geysermc.geyser.platform.standalone.GeyserStandaloneBootstrap");
                isGui = (boolean) cls.getMethod("isUseGui").invoke(cls.cast(bootstrap));
            } catch (Exception e) {
                logger.debug("Failed detecting if standalone is using a GUI; if this is a GeyserConnect instance this can be safely ignored.");
            }
        }

        double completeTime = (System.currentTimeMillis() - startupTime) / 1000D;
        String message = GeyserLocale.getLocaleStringLog("geyser.core.finish.done", new DecimalFormat("#.###").format(completeTime)) + " ";
        if (isGui) {
            message += GeyserLocale.getLocaleStringLog("geyser.core.finish.gui");
        } else {
            message += GeyserLocale.getLocaleStringLog("geyser.core.finish.console");
        }

        logger.info(message);

        if (platformType == PlatformType.STANDALONE) {
            logger.warning(GeyserLocale.getLocaleStringLog("geyser.core.movement_warn"));
        } else if (config.getRemote().authType() == AuthType.FLOODGATE) {
            VersionCheckUtils.checkForOutdatedFloodgate(logger);
        }
    }

    private void start() {
        this.scheduledThread = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("Geyser Scheduled Thread"));

        GeyserLogger logger = bootstrap.getGeyserLogger();
        GeyserConfiguration config = bootstrap.getGeyserConfig();

        ScoreboardUpdater.init();

        SkinProvider.registerCacheImageTask(this);

        ResourcePack.loadPacks();

        if (platformType != PlatformType.STANDALONE && config.getRemote().address().equals("auto")) {
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
        String remoteAddress = config.getRemote().address();
        // Filters whether it is not an IP address or localhost, because otherwise it is not possible to find out an SRV entry.
        if (!remoteAddress.matches(IP_REGEX) && !remoteAddress.equalsIgnoreCase("localhost")) {
            String[] record = WebUtils.findSrvRecord(this, remoteAddress);
            if (record != null) {
                int remotePort = Integer.parseInt(record[2]);
                config.getRemote().setAddress(remoteAddress = record[3]);
                config.getRemote().setPort(remotePort);
                logger.debug("Found SRV record \"" + remoteAddress + ":" + remotePort + "\"");
            }
        }

        // Ensure that PacketLib does not create an event loop for handling packets; we'll do that ourselves
        TcpSession.USE_EVENT_LOOP_FOR_PACKETS = false;

        pendingMicrosoftAuthentication = new PendingMicrosoftAuthentication(config.getPendingAuthenticationTimeout());

        this.newsHandler = new NewsHandler(BRANCH, this.buildNumber());

        CooldownUtils.setDefaultShowCooldown(config.getShowCooldown());
        DimensionUtils.changeBedrockNetherId(config.isAboveBedrockNetherBuilding()); // Apply End dimension ID workaround to Nether

        // https://github.com/GeyserMC/Geyser/issues/957
        RakNetConstants.MAXIMUM_MTU_SIZE = (short) config.getMtu();
        logger.debug("Setting MTU to " + config.getMtu());

        Integer bedrockThreadCount = Integer.getInteger("Geyser.BedrockNetworkThreads");
        if (bedrockThreadCount == null) {
            // Copy the code from Netty's default thread count fallback
            bedrockThreadCount = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        }

        boolean enableProxyProtocol = config.getBedrock().isEnableProxyProtocol();
        bedrockServer = new BedrockServer(
                new InetSocketAddress(config.getBedrock().address(), config.getBedrock().port()),
                bedrockThreadCount,
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
                    logger.info(GeyserLocale.getLocaleStringLog("geyser.core.start", config.getBedrock().address(),
                            String.valueOf(config.getBedrock().port())));
                } else {
                    String address = config.getBedrock().address();
                    int port = config.getBedrock().port();
                    logger.severe(GeyserLocale.getLocaleStringLog("geyser.core.fail", address, String.valueOf(port)));
                    if (!"0.0.0.0".equals(address)) {
                        logger.info(ChatColor.GREEN + "Suggestion: try setting `address` under `bedrock` in the Geyser config back to 0.0.0.0");
                        logger.info(ChatColor.GREEN + "Then, restart this server.");
                    }
                }
            }).join();
        }

        if (config.getRemote().authType() == AuthType.FLOODGATE) {
            try {
                Key key = new AesKeyProducer().produceFrom(config.getFloodgateKeyPath());
                cipher = new AesCipher(new Base64Topping());
                cipher.init(key);
                logger.debug(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.loaded_key"));
                // Note: this is positioned after the bind so the skin uploader doesn't try to run if Geyser fails
                // to load successfully. Spigot complains about class loader if the plugin is disabled.
                skinUploader = new FloodgateSkinUploader(this).start();
            } catch (Exception exception) {
                logger.severe(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.bad_key"), exception);
            }
        }

        if (config.getMetrics().isEnabled()) {
            metrics = new Metrics(this, "GeyserMC", config.getMetrics().getUniqueId(), false, java.util.logging.Logger.getLogger(""));
            metrics.addCustomChart(new Metrics.SingleLineChart("players", sessionManager::size));
            // Prevent unwanted words best we can
            metrics.addCustomChart(new Metrics.SimplePie("authMode", () -> config.getRemote().authType().toString().toLowerCase(Locale.ROOT)));
            metrics.addCustomChart(new Metrics.SimplePie("platform", platformType::getPlatformName));
            metrics.addCustomChart(new Metrics.SimplePie("defaultLocale", GeyserLocale::getDefaultLocale));
            metrics.addCustomChart(new Metrics.SimplePie("version", () -> GeyserImpl.VERSION));
            metrics.addCustomChart(new Metrics.AdvancedPie("playerPlatform", () -> {
                Map<String, Integer> valueMap = new HashMap<>();
                for (GeyserSession session : sessionManager.getAllSessions()) {
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
                for (GeyserSession session : sessionManager.getAllSessions()) {
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
        } else {
            metrics = null;
        }

        if (config.getRemote().authType() == AuthType.ONLINE) {
            if (config.getUserAuths() != null && !config.getUserAuths().isEmpty()) {
                getLogger().warning("The 'userAuths' config section is now deprecated, and will be removed in the near future! " +
                        "Please migrate to the new 'saved-user-logins' config option: " +
                        "https://wiki.geysermc.org/geyser/understanding-the-config/");
            }

            // May be written/read to on multiple threads from each GeyserSession as well as writing the config
            savedRefreshTokens = new ConcurrentHashMap<>();

            File tokensFile = bootstrap.getSavedUserLoginsFolder().resolve(Constants.SAVED_REFRESH_TOKEN_FILE).toFile();
            if (tokensFile.exists()) {
                TypeReference<Map<String, String>> type = new TypeReference<>() { };

                Map<String, String> refreshTokenFile = null;
                try {
                    refreshTokenFile = JSON_MAPPER.readValue(tokensFile, type);
                } catch (IOException e) {
                    logger.error("Cannot load saved user tokens!", e);
                }
                if (refreshTokenFile != null) {
                    List<String> validUsers = config.getSavedUserLogins();
                    boolean doWrite = false;
                    for (Map.Entry<String, String> entry : refreshTokenFile.entrySet()) {
                        String user = entry.getKey();
                        if (!validUsers.contains(user)) {
                            // Perform a write to this file to purge the now-unused name
                            doWrite = true;
                            continue;
                        }
                        savedRefreshTokens.put(user, entry.getValue());
                    }
                    if (doWrite) {
                        scheduleRefreshTokensWrite();
                    }
                }
            }
        } else {
            savedRefreshTokens = null;
        }

        newsHandler.handleNews(null, NewsItemAction.ON_SERVER_STARTED);

        this.eventBus.fire(new GeyserPostInitializeEvent(this.extensionManager, this.eventBus));
    }

    @Override
    public @NonNull List<GeyserSession> onlineConnections() {
        return sessionManager.getAllSessions();
    }

    @Override
    public int onlineConnectionsCount() {
        return sessionManager.size();
    }

    @Override
    public @MonotonicNonNull String usernamePrefix() {
        return null;
    }

    @Override
    public @Nullable GeyserSession connectionByUuid(@NonNull UUID uuid) {
        return this.sessionManager.getSessions().get(uuid);
    }

    @Override
    public @Nullable GeyserSession connectionByXuid(@NonNull String xuid) {
        return sessionManager.sessionByXuid(xuid);
    }

    @Override
    public boolean isBedrockPlayer(@NonNull UUID uuid) {
        return connectionByUuid(uuid) != null;
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, @NonNull Form form) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(form);
        GeyserSession session = connectionByUuid(uuid);
        if (session == null) {
            return false;
        }
        return session.sendForm(form);
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, @NonNull FormBuilder<?, ?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    @Override
    public boolean transfer(@NonNull UUID uuid, @NonNull String address, int port) {
        Objects.requireNonNull(uuid);
        GeyserSession session = connectionByUuid(uuid);
        if (session == null) {
            return false;
        }
        return session.transfer(address, port);
    }

    public void shutdown() {
        bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown"));
        shuttingDown = true;

        if (sessionManager.size() >= 1) {
            bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.log", sessionManager.size()));
            sessionManager.disconnectAll("geyser.core.shutdown.kick.message");
            bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.done"));
        }

        scheduledThread.shutdown();
        bedrockServer.close();
        if (skinUploader != null) {
            skinUploader.close();
        }
        newsHandler.shutdown();
        this.commandManager().getCommands().clear();

        ResourcePack.PACKS.clear();

        this.eventBus.fire(new GeyserShutdownEvent(this.extensionManager, this.commandManager(), this.eventBus));
        this.extensionManager.disableExtensions();

        bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.done"));
    }

    public void reload() {
        shutdown();
        this.extensionManager.enableExtensions();
        bootstrap.onEnable();
    }

    /**
     * Returns false if this Geyser instance is running in an IDE. This only needs to be used in cases where files
     * expected to be in a jarfile are not present.
     *
     * @return true if the version number is not 'DEV'.
     */
    public boolean isProductionEnvironment() {
        // First is if Blossom runs, second is if Blossom doesn't run
        // noinspection ConstantConditions - changes in production
        return !("git-local/dev-0000000".equals(GeyserImpl.GIT_VERSION) || "${gitVersion}".equals(GeyserImpl.GIT_VERSION));
    }

    @Override
    public GeyserExtensionManager extensionManager() {
        return this.extensionManager;
    }

    @Override
    public GeyserCommandManager commandManager() {
        return this.bootstrap.getGeyserCommandManager();
    }

    @Override
    public <R extends T, T> @NonNull R provider(@NonNull Class<T> apiClass, @Nullable Object... args) {
        return (R) Registries.PROVIDERS.get(apiClass).create(args);
    }

    @Override
    public EventBus eventBus() {
        return this.eventBus;
    }

    public RemoteServer defaultRemoteServer() {
        return getConfig().getRemote();
    }

    @Override
    public BedrockListener bedrockListener() {
        return getConfig().getBedrock();
    }

    public int buildNumber() {
        if (!this.isProductionEnvironment()) {
            return 0;
        }

        return Integer.parseInt(BUILD_NUMBER);
    }

    public static GeyserImpl start(PlatformType platformType, GeyserBootstrap bootstrap) {
        if (instance == null) {
            return new GeyserImpl(platformType, bootstrap);
        }

        // We've been reloaded
        if (instance.isShuttingDown()) {
            instance.shuttingDown = false;
            instance.start();
        }

        return instance;
    }

    public GeyserLogger getLogger() {
        return bootstrap.getGeyserLogger();
    }

    public GeyserConfiguration getConfig() {
        return bootstrap.getGeyserConfig();
    }

    public WorldManager getWorldManager() {
        return bootstrap.getWorldManager();
    }

    @Nullable
    public String refreshTokenFor(@NonNull String bedrockName) {
        return savedRefreshTokens.get(bedrockName);
    }

    public void saveRefreshToken(@NonNull String bedrockName, @NonNull String refreshToken) {
        if (!getConfig().getSavedUserLogins().contains(bedrockName)) {
            // Do not save this login
            return;
        }

        // We can safely overwrite old instances because MsaAuthenticationService#getLoginResponseFromRefreshToken
        // refreshes the token for us
        if (!Objects.equals(refreshToken, savedRefreshTokens.put(bedrockName, refreshToken))) {
            scheduleRefreshTokensWrite();
        }
    }

    private void scheduleRefreshTokensWrite() {
        scheduledThread.execute(() -> {
            // Ensure all writes are handled on the same thread
            File savedTokens = getBootstrap().getSavedUserLoginsFolder().resolve(Constants.SAVED_REFRESH_TOKEN_FILE).toFile();
            TypeReference<Map<String, String>> type = new TypeReference<>() { };
            try (FileWriter writer = new FileWriter(savedTokens)) {
                JSON_MAPPER.writerFor(type)
                        .withDefaultPrettyPrinter()
                        .writeValue(writer, savedRefreshTokens);
            } catch (IOException e) {
                getLogger().error("Unable to write saved refresh tokens!", e);
            }
        });
    }

    public static GeyserImpl getInstance() {
        return instance;
    }
}
