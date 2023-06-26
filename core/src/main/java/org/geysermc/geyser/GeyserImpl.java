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
import io.netty.channel.epoll.Epoll;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.Geyser;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.erosion.packet.Packets;
import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.AesKeyProducer;
import org.geysermc.floodgate.crypto.Base64Topping;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.news.NewsItemAction;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.network.BedrockListener;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.erosion.UnixSocketClientListener;
import org.geysermc.geyser.event.GeyserEventBus;
import org.geysermc.geyser.extension.GeyserExtensionManager;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.network.netty.GeyserServer;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.PendingMicrosoftAuthentication;
import org.geysermc.geyser.session.SessionManager;
import org.geysermc.geyser.skin.FloodgateSkinUploader;
import org.geysermc.geyser.skin.ProvidedSkins;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    public static final String COMMIT = "${commit}";
    public static final String REPOSITORY = "${repository}";

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

    private UnixSocketClientListener erosionUnixListener;

    private volatile boolean shuttingDown = false;

    private ScheduledExecutorService scheduledThread;

    private GeyserServer geyserServer;
    private final PlatformType platformType;
    private final GeyserBootstrap bootstrap;

    private final EventBus<EventRegistrar> eventBus;
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

        GeyserLocale.finalizeDefaultLocale(this);

        /* Initialize event bus */
        this.eventBus = new GeyserEventBus();

        /* Load Extensions */
        this.extensionManager = new GeyserExtensionManager();
        this.extensionManager.init();
        this.eventBus.fire(new GeyserPreInitializeEvent(this.extensionManager, this.eventBus));
    }

    public void initialize() {
        long startupTime = System.currentTimeMillis();

        GeyserLogger logger = bootstrap.getGeyserLogger();

        logger.info("******************************************");
        logger.info("");
        logger.info(GeyserLocale.getLocaleStringLog("geyser.core.load", NAME, VERSION));
        logger.info("");
        logger.info("******************************************");

        /* Initialize registries */
        Registries.init();
        BlockRegistries.init();

        /* Initialize translators */
        EntityDefinitions.init();
        MessageTranslator.init();

        // Download the latest asset list and cache it
        AssetUtils.generateAssetCache().whenComplete((aVoid, ex) -> {
            if (ex != null) {
                return;
            }
            MinecraftLocale.ensureEN_US();
            String locale = GeyserLocale.getDefaultLocale();
            if (!"en_us".equals(locale)) {
                // English will be loaded after assets are downloaded, if necessary
                MinecraftLocale.downloadAndLoadLocale(locale);
            }

            ProvidedSkins.init();

            CompletableFuture.runAsync(AssetUtils::downloadAndRunClientJarTasks);
        });

        startInstance();

        GeyserConfiguration config = bootstrap.getGeyserConfig();

        double completeTime = (System.currentTimeMillis() - startupTime) / 1000D;
        String message = GeyserLocale.getLocaleStringLog("geyser.core.finish.done", new DecimalFormat("#.###").format(completeTime));
        message += " " + GeyserLocale.getLocaleStringLog("geyser.core.finish.console");
        logger.info(message);

        if (platformType == PlatformType.STANDALONE) {
            if (config.getRemote().authType() != AuthType.FLOODGATE) {
                // If the auth-type is Floodgate, then this Geyser instance is probably owned by the Java server
                logger.warning(GeyserLocale.getLocaleStringLog("geyser.core.movement_warn"));
            }
        } else if (config.getRemote().authType() == AuthType.FLOODGATE) {
            VersionCheckUtils.checkForOutdatedFloodgate(logger);
        }
    }

    private void startInstance() {
        this.scheduledThread = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("Geyser Scheduled Thread"));

        GeyserLogger logger = bootstrap.getGeyserLogger();
        GeyserConfiguration config = bootstrap.getGeyserConfig();

        ScoreboardUpdater.init();

        SkinProvider.registerCacheImageTask(this);

        Registries.RESOURCE_PACKS.load();

        String geyserUdpPort = System.getProperty("geyserUdpPort", "");
        String pluginUdpPort = geyserUdpPort.isEmpty() ? System.getProperty("pluginUdpPort", "") : geyserUdpPort;
        if ("-1".equals(pluginUdpPort)) {
            throw new UnsupportedOperationException("This hosting/service provider does not support applications running on the UDP port");
        }
        boolean portPropertyApplied = false;
        String pluginUdpAddress = System.getProperty("geyserUdpAddress", System.getProperty("pluginUdpAddress", ""));

        if (platformType != PlatformType.STANDALONE) {
            int javaPort = bootstrap.getServerPort();
            if (config.getRemote().address().equals("auto")) {
                config.setAutoconfiguredRemote(true);
                String serverAddress = bootstrap.getServerBindAddress();
                if (!serverAddress.isEmpty() && !"0.0.0.0".equals(serverAddress)) {
                    config.getRemote().setAddress(serverAddress);
                } else {
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
                if (javaPort != -1) {
                    config.getRemote().setPort(javaPort);
                }
            }

            boolean forceMatchServerPort = "server".equals(pluginUdpPort);
            if ((config.getBedrock().isCloneRemotePort() || forceMatchServerPort) && javaPort != -1) {
                config.getBedrock().setPort(javaPort);
                if (forceMatchServerPort) {
                    if (geyserUdpPort.isEmpty()) {
                        logger.info("Port set from system generic property to match Java server.");
                    } else {
                        logger.info("Port set from system property to match Java server.");
                    }
                    portPropertyApplied = true;
                }
            }

            if ("server".equals(pluginUdpAddress)) {
                String address = bootstrap.getServerBindAddress();
                if (!address.isEmpty()) {
                    config.getBedrock().setAddress(address);
                }
            } else if (!pluginUdpAddress.isEmpty()) {
                config.getBedrock().setAddress(pluginUdpAddress);
            }

            if (!portPropertyApplied && !pluginUdpPort.isEmpty()) {
                int port = Integer.parseInt(pluginUdpPort);
                config.getBedrock().setPort(port);
                if (geyserUdpPort.isEmpty()) {
                    logger.info("Port set from generic system property: " + port);
                } else {
                    logger.info("Port set from system property: " + port);
                }
            }

            boolean floodgatePresent = bootstrap.testFloodgatePluginPresent();
            if (config.getRemote().authType() == AuthType.FLOODGATE && !floodgatePresent) {
                logger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " "
                        + GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
                return;
            } else if (config.isAutoconfiguredRemote() && floodgatePresent) {
                // Floodgate installed means that the user wants Floodgate authentication
                logger.debug("Auto-setting to Floodgate authentication.");
                config.getRemote().setAuthType(AuthType.FLOODGATE);
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

        Packets.initGeyser();

        if (Epoll.isAvailable()) {
            this.erosionUnixListener = new UnixSocketClientListener();
        } else {
            logger.debug("Epoll is not available; Erosion's Unix socket handling will not work.");
        }

        CooldownUtils.setDefaultShowCooldown(config.getShowCooldown());
        DimensionUtils.changeBedrockNetherId(config.isAboveBedrockNetherBuilding()); // Apply End dimension ID workaround to Nether

        Integer bedrockThreadCount = Integer.getInteger("Geyser.BedrockNetworkThreads");
        if (bedrockThreadCount == null) {
            // Copy the code from Netty's default thread count fallback
            bedrockThreadCount = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        }

        if (shouldStartListener) {
            this.geyserServer = new GeyserServer(this, bedrockThreadCount);
            this.geyserServer.bind(new InetSocketAddress(config.getBedrock().address(), config.getBedrock().port()))
                .whenComplete((avoid, throwable) -> {
                    if (throwable == null) {
                        logger.info(GeyserLocale.getLocaleStringLog("geyser.core.start", config.getBedrock().address(),
                                String.valueOf(config.getBedrock().port())));
                    } else {
                        String address = config.getBedrock().address();
                        int port = config.getBedrock().port();
                        logger.severe(GeyserLocale.getLocaleStringLog("geyser.core.fail", address, String.valueOf(port)));
                        if (!"0.0.0.0".equals(address)) {
                            logger.info(Component.text("Suggestion: try setting `address` under `bedrock` in the Geyser config back to 0.0.0.0", NamedTextColor.GREEN));
                            logger.info(Component.text("Then, restart this server.", NamedTextColor.GREEN));
                        }
                    }
                }).join();
        }

        if (config.getRemote().authType() == AuthType.FLOODGATE) {
            try {
                Key key = new AesKeyProducer().produceFrom(config.getFloodgateKeyPath());
                cipher = new AesCipher(new Base64Topping());
                cipher.init(key);
                logger.debug("Loaded Floodgate key!");
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
            metrics.addCustomChart(new Metrics.SimplePie("platform", platformType::platformName));
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
                platformMap.put(platformType.platformName(), 1);
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
        if (config.isNotifyOnNewBedrockUpdate()) {
            VersionCheckUtils.checkForGeyserUpdate(this::getLogger);
        }

        VersionCheckUtils.checkForOutdatedJava(logger);
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
        geyserServer.shutdown();
        if (skinUploader != null) {
            skinUploader.close();
        }
        newsHandler.shutdown();
        this.commandManager().getCommands().clear();

        if (this.erosionUnixListener != null) {
            this.erosionUnixListener.close();
        }

        Registries.RESOURCE_PACKS.get().clear();

        this.eventBus.fire(new GeyserShutdownEvent(this.extensionManager, this.eventBus));
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
    @NonNull
    public GeyserExtensionManager extensionManager() {
        return this.extensionManager;
    }

    @NonNull
    public GeyserCommandManager commandManager() {
        return this.bootstrap.getGeyserCommandManager();
    }

    @Override
    public <R extends T, T> @NonNull R provider(@NonNull Class<T> apiClass, @Nullable Object... args) {
        return (R) Registries.PROVIDERS.get(apiClass).create(args);
    }

    @Override
    @NonNull
    public EventBus<EventRegistrar> eventBus() {
        return this.eventBus;
    }

    @NonNull
    public RemoteServer defaultRemoteServer() {
        return getConfig().getRemote();
    }

    @Override
    @NonNull
    public BedrockListener bedrockListener() {
        return getConfig().getBedrock();
    }

    @Override
    @NonNull
    public Path configDirectory() {
        return bootstrap.getConfigFolder();
    }

    @Override
    @NonNull
    public Path packDirectory() {
        return bootstrap.getConfigFolder().resolve("packs");
    }

    @Override
    @NonNull
    public PlatformType platformType() {
        return platformType;
    }

    public int buildNumber() {
        if (!this.isProductionEnvironment()) {
            return 0;
        }

        return Integer.parseInt(BUILD_NUMBER);
    }

    public static GeyserImpl load(PlatformType platformType, GeyserBootstrap bootstrap) {
        if (instance == null) {
            return new GeyserImpl(platformType, bootstrap);
        }

        return instance;
    }

    public static void start() {
        if (instance == null) {
            throw new RuntimeException("Geyser has not been loaded yet!");
        }

        // We've been reloaded
        if (instance.isShuttingDown()) {
            instance.shuttingDown = false;
            instance.startInstance();
        } else {
            instance.initialize();
        }
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
