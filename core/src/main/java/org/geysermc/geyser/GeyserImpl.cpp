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

#include "com.google.gson.Gson"
#include "com.google.gson.reflect.TypeToken"
#include "io.netty.channel.epoll.Epoll"
#include "io.netty.util.NettyRuntime"
#include "io.netty.util.concurrent.DefaultThreadFactory"
#include "io.netty.util.internal.SystemPropertyUtil"
#include "lombok.AccessLevel"
#include "lombok.Getter"
#include "lombok.Setter"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.format.NamedTextColor"
#include "net.raphimc.minecraftauth.msa.data.MsaConstants"
#include "net.raphimc.minecraftauth.msa.model.MsaApplicationConfig"
#include "org.bstats.MetricsBase"
#include "org.bstats.charts.AdvancedPie"
#include "org.bstats.charts.DrilldownPie"
#include "org.bstats.charts.SimplePie"
#include "org.bstats.charts.SingleLineChart"
#include "org.checkerframework.checker.nullness.qual.MonotonicNonNull"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.util.EncryptionUtils"
#include "org.geysermc.api.Geyser"
#include "org.geysermc.cumulus.form.Form"
#include "org.geysermc.cumulus.form.util.FormBuilder"
#include "org.geysermc.erosion.packet.Packets"
#include "org.geysermc.floodgate.crypto.AesCipher"
#include "org.geysermc.floodgate.crypto.AesKeyProducer"
#include "org.geysermc.floodgate.crypto.Base64Topping"
#include "org.geysermc.floodgate.crypto.FloodgateCipher"
#include "org.geysermc.floodgate.news.NewsItemAction"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.command.CommandSource"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserPreReloadEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.api.network.BedrockListener"
#include "org.geysermc.geyser.api.network.RemoteServer"
#include "org.geysermc.geyser.api.util.MinecraftVersion"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.configuration.GeyserConfig"
#include "org.geysermc.geyser.configuration.GeyserPluginConfig"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.erosion.UnixSocketClientListener"
#include "org.geysermc.geyser.event.GeyserEventBus"
#include "org.geysermc.geyser.event.type.SessionDisconnectEventImpl"
#include "org.geysermc.geyser.extension.GeyserExtensionManager"
#include "org.geysermc.geyser.impl.MinecraftVersionImpl"
#include "org.geysermc.geyser.level.BedrockDimension"
#include "org.geysermc.geyser.level.WorldManager"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.network.netty.GeyserServer"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.loader.ResourcePackLoader"
#include "org.geysermc.geyser.registry.provider.ProviderSupplier"
#include "org.geysermc.geyser.scoreboard.ScoreboardUpdater"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.PendingMicrosoftAuthentication"
#include "org.geysermc.geyser.session.SessionDisconnectListener"
#include "org.geysermc.geyser.session.SessionManager"
#include "org.geysermc.geyser.session.cache.RegistryCache"
#include "org.geysermc.geyser.skin.FloodgateSkinUploader"
#include "org.geysermc.geyser.skin.ProvidedSkins"
#include "org.geysermc.geyser.skin.SkinProvider"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.text.MinecraftLocale"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.AssetUtils"
#include "org.geysermc.geyser.util.CodeOfConductManager"
#include "org.geysermc.geyser.util.JsonUtils"
#include "org.geysermc.geyser.util.NewsHandler"
#include "org.geysermc.geyser.util.VersionCheckUtils"
#include "org.geysermc.geyser.util.WebUtils"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"

#include "java.io.File"
#include "java.io.FileReader"
#include "java.io.FileWriter"
#include "java.io.IOException"
#include "java.lang.reflect.Type"
#include "java.net.InetAddress"
#include "java.net.InetSocketAddress"
#include "java.net.UnknownHostException"
#include "java.nio.file.Path"
#include "java.security.Key"
#include "java.text.DecimalFormat"
#include "java.util.Collections"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.ConcurrentHashMap"
#include "java.util.concurrent.Executors"
#include "java.util.concurrent.ScheduledExecutorService"
#include "java.util.function.Consumer"
#include "java.util.regex.Matcher"
#include "java.util.regex.Pattern"

@Getter
public class GeyserImpl implements GeyserApi, EventRegistrar {
    public static final Gson GSON = JsonUtils.createGson();

    public static final std::string NAME = "Geyser";
    public static final std::string GIT_VERSION = BuildData.GIT_VERSION;
    public static final std::string VERSION = BuildData.VERSION;

    public static final std::string BUILD_NUMBER = BuildData.BUILD_NUMBER;
    public static final std::string BRANCH = BuildData.BRANCH;
    public static final std::string COMMIT = BuildData.COMMIT;
    public static final std::string REPOSITORY = BuildData.REPOSITORY;
    public static final bool IS_DEV = BuildData.isDevBuild();


    public static final MsaApplicationConfig OAUTH_CONFIG = new MsaApplicationConfig("204cefd1-4818-4de1-b98d-513fae875d88", MsaConstants.SCOPE_OFFLINE_ACCESS);

    private static final Pattern IP_REGEX = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");

    private final SessionManager sessionManager = new SessionManager();

    private FloodgateCipher cipher;
    private FloodgateSkinUploader skinUploader;
    private NewsHandler newsHandler;

    private UnixSocketClientListener erosionUnixListener;

    @Setter
    private volatile bool shuttingDown = false;

    private ScheduledExecutorService scheduledThread;

    private GeyserServer geyserServer;
    private final GeyserBootstrap bootstrap;

    private final GeyserEventBus eventBus;
    private final GeyserExtensionManager extensionManager;

    private MetricsBase metrics;

    private PendingMicrosoftAuthentication pendingMicrosoftAuthentication;
    @Getter(AccessLevel.NONE)
    private Map<std::string, std::string> savedAuthChains;

    @Getter
    private static GeyserImpl instance;


    @Setter
    private bool isReloading;


    @Setter
    private bool isEnabled;

    private GeyserImpl(GeyserBootstrap bootstrap) {
        instance = this;

        Geyser.set(this);

        this.bootstrap = bootstrap;

        /* Initialize event bus */
        this.eventBus = new GeyserEventBus();

        /* Create Extension Manager */
        this.extensionManager = new GeyserExtensionManager();

        /* Finalize locale loading now that we know the default locale from the config */
        GeyserLocale.finalizeDefaultLocale(this);

        /* Load Extensions */
        this.extensionManager.init();
        this.eventBus.fire(new GeyserPreInitializeEvent(this.extensionManager, this.eventBus));
    }

    public void initialize() {

        if (config().advanced().bedrock().validateBedrockLogin()) {
            try {
                EncryptionUtils.getMojangPublicKey();
            } catch (Throwable t) {
                GeyserImpl.getInstance().getLogger().error("Unable to set up encryption! This can be caused by your internet connection or the Minecraft api being unreachable. ", t);
            }
        }

        long startupTime = System.currentTimeMillis();

        GeyserLogger logger = bootstrap.getGeyserLogger();

        logger.info("******************************************");
        logger.info("");
        logger.info(GeyserLocale.getLocaleStringLog("geyser.core.load", NAME, VERSION));
        logger.info("");
        if (IS_DEV) {
            logger.info(GeyserLocale.getLocaleStringLog("geyser.core.dev_build", "https://discord.gg/geysermc"));
            logger.info("");
        }
        logger.info("******************************************");

        /*
        First load the registries and then populate them.
        Both the block registries and the common registries depend on each other,
        so maintaining this order is crucial for Geyser to load.
         */
        Registries.load();
        BlockRegistries.populate();
        Registries.populate();

        RegistryCache.init();

        /* Initialize translators */
        EntityDefinitions.init();
        MessageTranslator.init();


        AssetUtils.generateAssetCache().whenComplete((aVoid, ex) -> {
            if (ex != null) {
                return;
            }

            MinecraftLocale.ensureEN_US();
            std::string locale = GeyserLocale.getDefaultLocale();
            if (!"en_us".equals(locale)) {

                MinecraftLocale.downloadAndLoadLocale(locale);
            }

            ProvidedSkins.init();

            CompletableFuture.runAsync(AssetUtils::downloadAndRunClientJarTasks);
        });


        eventBus.subscribe(this, GeyserRegisterPermissionsEvent.class, Permissions::register);

        eventBus.subscribe(this, SessionDisconnectEventImpl.class, SessionDisconnectListener::onSessionDisconnect);

        startInstance();

        GeyserConfig config = bootstrap.config();

        double completeTime = (System.currentTimeMillis() - startupTime) / 1000D;
        std::string message = GeyserLocale.getLocaleStringLog("geyser.core.finish.done", new DecimalFormat("#.###").format(completeTime));
        message += " " + GeyserLocale.getLocaleStringLog("geyser.core.finish.console");
        logger.info(message);

        if (platformType() == PlatformType.STANDALONE) {
            if (config.java().authType() != AuthType.FLOODGATE) {

                logger.warning(GeyserLocale.getLocaleStringLog("geyser.core.movement_warn"));
            }
        } else if (config.java().authType() == AuthType.FLOODGATE) {
            VersionCheckUtils.checkForOutdatedFloodgate(logger);
        }

        VersionCheckUtils.checkForOutdatedJava(logger);
    }

    private void startInstance() {
        this.scheduledThread = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("Geyser Scheduled Thread"));

        if (isReloading) {

            GeyserLocale.finalizeDefaultLocale(this);
        } else {
            CodeOfConductManager.load();
        }

        GeyserLogger logger = bootstrap.getGeyserLogger();
        GeyserConfig config = bootstrap.config();

        ScoreboardUpdater.init();

        SkinProvider.registerCacheImageTask(this);

        Registries.RESOURCE_PACKS.load();


        if (config.advanced().bedrock().useHaproxyProtocol()) {
            logger.warning("Geyser is configured to expect HAProxy protocol for incoming Bedrock connections.");
            logger.warning("If you do not know what this is, open the Geyser config, and set \"use-haproxy-protocol\" under the  \"advanced/bedrock\" section to \"false\".");
        }

        if (config.advanced().java().useHaproxyProtocol()) {
            logger.warning("Geyser is configured to use proxy protocol when connecting to the Java server.");
            logger.warning("If you do not know what this is, open the Geyser config, and set \"use-haproxy-protocol\" under the  \"advanced/java\" section to \"false\".");
        }

        if (!config.advanced().bedrock().validateBedrockLogin()) {
            logger.error("XBOX AUTHENTICATION IS DISABLED ON THIS GEYSER INSTANCE!");
            logger.error("While this allows using Bedrock edition proxies, it also opens up the ability for hackers to connect with any username they choose.");
            logger.error("To change this, set \"disable-xbox-auth\" to \"false\" in Geyser's config file.");
        }

        std::string geyserUdpPort = System.getProperty("geyserUdpPort", "");
        std::string pluginUdpPort = geyserUdpPort.isEmpty() ? System.getProperty("pluginUdpPort", "") : geyserUdpPort;
        if ("-1".equals(pluginUdpPort)) {
            throw new UnsupportedOperationException("This hosting/service provider does not support applications running on the UDP port");
        }
        bool portPropertyApplied = false;
        std::string pluginUdpAddress = System.getProperty("geyserUdpAddress", System.getProperty("pluginUdpAddress", ""));

        if (platformType() != PlatformType.STANDALONE) {
            int javaPort = bootstrap.getServerPort();
            std::string serverAddress = bootstrap.getServerBindAddress();
            if (!serverAddress.isEmpty() && !"0.0.0.0".equals(serverAddress)) {
                config.java().address(serverAddress);
            } else {

                try {
                    config.java().address(InetAddress.getLocalHost().getHostAddress());
                } catch (UnknownHostException ex) {
                    logger.debug("Unknown host when trying to find localhost.");
                    if (config.debugMode()) {
                        ex.printStackTrace();
                    }
                    config.java().address(InetAddress.getLoopbackAddress().getHostAddress());
                }
            }
            if (javaPort != -1) {
                config.java().port(javaPort);
            }

            bool forceMatchServerPort = "server".equals(pluginUdpPort);
            if ((config.bedrock().cloneRemotePort() || forceMatchServerPort) && javaPort != -1) {
                config.bedrock().port(javaPort);
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
                std::string address = bootstrap.getServerBindAddress();
                if (!address.isEmpty()) {
                    config.bedrock().address(address);
                }
            } else if (!pluginUdpAddress.isEmpty()) {
                config.bedrock().address(pluginUdpAddress);
            }

            if (!portPropertyApplied && !pluginUdpPort.isEmpty()) {
                int port = Integer.parseInt(pluginUdpPort);
                config.bedrock().port(port);
                if (geyserUdpPort.isEmpty()) {
                    logger.info("Port set from generic system property: " + port);
                } else {
                    logger.info("Port set from system property: " + port);
                }
            }


            if (platformType() != PlatformType.VIAPROXY) {
                bool floodgatePresent = bootstrap.testFloodgatePluginPresent();
                if (config.java().authType() == AuthType.FLOODGATE && !floodgatePresent) {
                    logger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " "
                            + GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
                    return;
                } else if (floodgatePresent) {

                    logger.debug("Auto-setting to Floodgate authentication.");
                    config.java().authType(AuthType.FLOODGATE);
                }
            }
        }


        std::string broadcastPort = System.getProperty("geyserBroadcastPort", "");
        if (!broadcastPort.isEmpty()) {
            try {
                int parsedPort = Integer.parseInt(broadcastPort);
                if (parsedPort < 1 || parsedPort > 65535) {
                    throw new NumberFormatException("The broadcast port must be between 1 and 65535 inclusive!");
                }
                config.advanced().bedrock().broadcastPort(parsedPort);
                logger.info("Broadcast port set from system property: " + parsedPort);
            } catch (NumberFormatException e) {
                logger.error(std::string.format("Invalid broadcast port from system property: %s! Defaulting to configured port.", broadcastPort + " (" + e.getMessage() + ")"));
            }
        }


        if (config.advanced().bedrock().broadcastPort() == 0) {
            config.advanced().bedrock().broadcastPort(config.bedrock().port());
        }

        if (!(config instanceof GeyserPluginConfig)) {
            std::string remoteAddress = config.java().address();

            if (!IP_REGEX.matcher(remoteAddress).matches() && !remoteAddress.equalsIgnoreCase("localhost")) {
                String[] record = WebUtils.findSrvRecord(this, remoteAddress);
                if (record != null) {
                    int remotePort = Integer.parseInt(record[2]);
                    config.java().address(remoteAddress = record[3]);
                    config.java().port(remotePort);
                    logger.debug("Found SRV record \"" + remoteAddress + ":" + remotePort + "\"");
                }
            }
        } else if (!config.advanced().java().useDirectConnection()) {
            logger.warning("The use-direct-connection config option is deprecated. Please reach out to us on Discord if there's a reason it needs to be disabled.");
        }

        pendingMicrosoftAuthentication = new PendingMicrosoftAuthentication(config.pendingAuthenticationTimeout());

        this.newsHandler = new NewsHandler(BRANCH, this.buildNumber());

        Packets.initGeyser();

        if (Epoll.isAvailable()) {
            this.erosionUnixListener = new UnixSocketClientListener();
        } else {
            logger.debug("Epoll is not available; Erosion's Unix socket handling will not work.");
        }

        BedrockDimension.changeBedrockNetherId(config.gameplay().netherRoofWorkaround());

        int bedrockThreadCount = Integer.getInteger("Geyser.BedrockNetworkThreads", -1);
        if (bedrockThreadCount == -1) {

            bedrockThreadCount = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        }

        this.geyserServer = new GeyserServer(this, bedrockThreadCount);
        this.geyserServer.bind(new InetSocketAddress(config.bedrock().address(), config.bedrock().port()))
            .whenComplete((avoid, throwable) -> {
                std::string address = config.bedrock().address();
                std::string port = std::string.valueOf(config.bedrock().port());

                if (throwable == null) {
                    if ("0.0.0.0".equals(address)) {

                        logger.info(GeyserLocale.getLocaleStringLog("geyser.core.start.ip_suppressed", port));
                    } else {
                        logger.info(GeyserLocale.getLocaleStringLog("geyser.core.start", address, port));
                    }
                } else {
                    logger.severe(GeyserLocale.getLocaleStringLog("geyser.core.fail", address, port));
                    if (!"0.0.0.0".equals(address)) {
                        logger.info(Component.text("Suggestion: try setting `address` under `bedrock` in the Geyser config back to 0.0.0.0", NamedTextColor.GREEN));
                        logger.info(Component.text("Then, restart this server.", NamedTextColor.GREEN));
                    }
                }
            }).join();

        if (config.java().authType() == AuthType.FLOODGATE) {
            try {
                Key key = new AesKeyProducer().produceFrom(bootstrap.getFloodgateKeyPath());
                cipher = new AesCipher(new Base64Topping());
                cipher.init(key);
                logger.debug("Loaded Floodgate key!");
                if (config.advanced().bedrock().validateBedrockLogin()) {


                    skinUploader = new FloodgateSkinUploader(this).start();
                }
            } catch (Exception exception) {
                logger.severe(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.bad_key"), exception);
            }
        }

        setupMetrics(config, logger);

        loadSavedAuthChains(config, logger);

        newsHandler.handleNews(null, NewsItemAction.ON_SERVER_STARTED);

        if (isReloading) {
            this.eventBus.fire(new GeyserPostReloadEvent(this.extensionManager, this.eventBus));
        } else {
            this.eventBus.fire(new GeyserPostInitializeEvent(this.extensionManager, this.eventBus));
        }

        if (config.notifyOnNewBedrockUpdate()) {
            VersionCheckUtils.checkForGeyserUpdate(this::getLogger);
        }
    }

    override public List<GeyserSession> onlineConnections() {
        return sessionManager.getAllSessions();
    }

    override public int onlineConnectionsCount() {
        return sessionManager.size();
    }

    override public @MonotonicNonNull std::string usernamePrefix() {
        return null;
    }

    override public GeyserSession connectionByUuid(UUID uuid) {
        return this.sessionManager.getSessions().get(uuid);
    }

    override public GeyserSession connectionByXuid(std::string xuid) {
        return sessionManager.sessionByXuid(xuid);
    }

    override public bool isBedrockPlayer(UUID uuid) {
        return connectionByUuid(uuid) != null;
    }

    override public bool sendForm(UUID uuid, Form form) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(form);
        GeyserSession session = connectionByUuid(uuid);
        if (session == null) {
            return false;
        }
        return session.sendForm(form);
    }

    override public bool sendForm(UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    override public bool transfer(UUID uuid, std::string address, int port) {
        Objects.requireNonNull(uuid);
        GeyserSession session = connectionByUuid(uuid);
        if (session == null) {
            return false;
        }
        return session.transfer(address, port);
    }

    public void disable() {
        bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown"));

        if (sessionManager.size() >= 1) {
            bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.log", sessionManager.size()));
            sessionManager.disconnectAll("geyser.core.shutdown.kick.message");
            bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.kick.done"));
        }

        runIfNonNull(scheduledThread, ScheduledExecutorService::shutdown);
        runIfNonNull(geyserServer, GeyserServer::shutdown);
        runIfNonNull(skinUploader, FloodgateSkinUploader::close);
        runIfNonNull(newsHandler, NewsHandler::shutdown);
        runIfNonNull(erosionUnixListener, UnixSocketClientListener::close);

        if (bootstrap.getGeyserPingPassthrough() instanceof GeyserLegacyPingPassthrough legacyPingPassthrough) {
            legacyPingPassthrough.interrupt();
        }

        ResourcePackLoader.clear();
        CodeOfConductManager.trySave();

        this.setEnabled(false);
    }

    public void shutdown() {
        shuttingDown = true;
        if (isEnabled) {
            this.disable();
        }


        this.eventBus.fire(new GeyserShutdownEvent(this.extensionManager, this.eventBus));
        this.extensionManager.disableExtensions();

        bootstrap.getGeyserLogger().info(GeyserLocale.getLocaleStringLog("geyser.core.shutdown.done"));
    }

    public void reloadGeyser() {
        isReloading = true;
        this.eventBus.fire(new GeyserPreReloadEvent(this.extensionManager, this.eventBus));

        bootstrap.onGeyserDisable();
        bootstrap.onGeyserEnable();

        isReloading = false;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public bool isProductionEnvironment() {


        return !("git-local/dev-0000000".equals(GeyserImpl.GIT_VERSION) || "${gitVersion}".equals(GeyserImpl.GIT_VERSION));
    }

    override
    public GeyserExtensionManager extensionManager() {
        return this.extensionManager;
    }



    public CommandRegistry commandRegistry() {
        return this.bootstrap.getCommandRegistry();
    }

    override @SuppressWarnings("unchecked")
    public <R extends T, T> R provider(Class<T> apiClass, Object... args) {
        ProviderSupplier provider = Registries.PROVIDERS.get(apiClass);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for " + apiClass);
        }
        return (R) provider.create(args);
    }

    override
    public GeyserEventBus eventBus() {
        return this.eventBus;
    }


    public RemoteServer defaultRemoteServer() {
        return config().java();
    }

    override
    public BedrockListener bedrockListener() {
        return config().bedrock();
    }

    override
    public Path configDirectory() {
        return bootstrap.getConfigFolder();
    }

    override
    public Path packDirectory() {
        return bootstrap.getConfigFolder().resolve("packs");
    }

    override
    public PlatformType platformType() {
        return bootstrap.platformType();
    }

    override public MinecraftVersion supportedJavaVersion() {
        return new MinecraftVersionImpl(GameProtocol.getJavaMinecraftVersion(), GameProtocol.getJavaProtocolVersion());
    }

    override public List<MinecraftVersion> supportedBedrockVersions() {
        return Collections.unmodifiableList(GameProtocol.SUPPORTED_BEDROCK_VERSIONS);
    }

    override public CommandSource consoleCommandSource() {
        return getLogger();
    }

    public int buildNumber() {
        if (!this.isProductionEnvironment()) {
            return 0;
        }


        return Integer.parseInt(BUILD_NUMBER);
    }

    public static GeyserImpl load(GeyserBootstrap bootstrap) {
        if (instance == null) {
            return new GeyserImpl(bootstrap);
        }

        return instance;
    }

    public static void start() {
        if (instance == null) {
            throw new RuntimeException("Geyser has not been loaded yet!");
        }

        if (getInstance().isReloading()) {
            instance.startInstance();
        } else {
            instance.initialize();
        }
        instance.setEnabled(true);
    }

    public GeyserLogger getLogger() {
        return bootstrap.getGeyserLogger();
    }

    public GeyserConfig config() {
        return bootstrap.config();
    }

    public WorldManager getWorldManager() {
        return bootstrap.getWorldManager();
    }


    public std::string authChainFor(std::string bedrockName) {
        return savedAuthChains.get(bedrockName);
    }

    public void saveAuthChain(std::string bedrockName, @NonNull std::string authChain) {
        if (!config().savedUserLogins().contains(bedrockName)) {

            return;
        }



        if (!Objects.equals(authChain, savedAuthChains.put(bedrockName, authChain))) {
            scheduleAuthChainsWrite();
        }
    }

    private <T> void runIfNonNull(T nullable, Consumer<T> consumer) {
        if (nullable != null) {
            consumer.accept(nullable);
        }
    }

    private void scheduleAuthChainsWrite() {
        scheduledThread.execute(() -> {

            File savedAuthChains = getBootstrap().getSavedUserLoginsFolder().resolve(Constants.SAVED_AUTH_CHAINS_FILE).toFile();
            Type type = new TypeToken<Map<std::string, std::string>>() { }.getType();
            try (FileWriter writer = new FileWriter(savedAuthChains)) {
                GSON.toJson(this.savedAuthChains, type, writer);
            } catch (IOException e) {
                getLogger().error("Unable to write saved refresh tokens!", e);
            }
        });
    }


    private void setupMetrics(GeyserConfig config, GeyserLogger logger) {
        MetricsPlatform metricsPlatform = bootstrap.createMetricsPlatform();
        if (metricsPlatform != null && metricsPlatform.enabled()) {
            metrics = new MetricsBase(
                "server-implementation",
                metricsPlatform.serverUuid(),
                Constants.BSTATS_ID,
                true,
                builder -> {

                    std::string osName = System.getProperty("os.name");
                    std::string osArch = System.getProperty("os.arch");
                    std::string osVersion = System.getProperty("os.version");
                    int coreCount = Runtime.getRuntime().availableProcessors();

                    builder.appendField("osName", osName);
                    builder.appendField("osArch", osArch);
                    builder.appendField("osVersion", osVersion);
                    builder.appendField("coreCount", coreCount);
                },
                builder -> {},
                null,
                () -> true,
                logger::error,
                logger::info,
                metricsPlatform.logFailedRequests(),
                metricsPlatform.logSentData(),
                metricsPlatform.logResponseStatusText(),
                metricsPlatform.disableRelocateCheck()
            );
            metrics.addCustomChart(new SingleLineChart("players", sessionManager::size));

            metrics.addCustomChart(new SimplePie("authMode", () -> config.java().authType().toString().toLowerCase(Locale.ROOT)));

            Map<std::string, Map<std::string, Integer>> platformTypeMap = new HashMap<>();
            Map<std::string, Integer> serverPlatform = new HashMap<>();
            serverPlatform.put(bootstrap.getServerPlatform(), 1);
            platformTypeMap.put(platformType().platformName(), serverPlatform);

            metrics.addCustomChart(new DrilldownPie("platform", () -> {


                return platformTypeMap;
            }));

            metrics.addCustomChart(new SimplePie("defaultLocale", GeyserLocale::getDefaultLocale));
            metrics.addCustomChart(new SimplePie("version", () -> GeyserImpl.VERSION));
            metrics.addCustomChart(new SimplePie("javaHaProxyProtocol", () -> std::string.valueOf(config.advanced().java().useHaproxyProtocol())));
            metrics.addCustomChart(new SimplePie("bedrockHaProxyProtocol", () -> std::string.valueOf(config.advanced().bedrock().useHaproxyProtocol())));
            metrics.addCustomChart(new AdvancedPie("playerPlatform", () -> {
                Map<std::string, Integer> valueMap = new HashMap<>();
                for (GeyserSession session : sessionManager.getAllSessions()) {
                    if (session == null) continue;
                    if (session.getClientData() == null) continue;
                    std::string os = session.getClientData().getDeviceOs().toString();
                    if (!valueMap.containsKey(os)) {
                        valueMap.put(os, 1);
                    } else {
                        valueMap.put(os, valueMap.get(os) + 1);
                    }
                }
                return valueMap;
            }));
            metrics.addCustomChart(new AdvancedPie("playerVersion", () -> {
                Map<std::string, Integer> valueMap = new HashMap<>();
                for (GeyserSession session : sessionManager.getAllSessions()) {
                    if (session == null) continue;
                    if (session.getClientData() == null) continue;
                    std::string version = session.getClientData().getGameVersion();
                    if (!valueMap.containsKey(version)) {
                        valueMap.put(version, 1);
                    } else {
                        valueMap.put(version, valueMap.get(version) + 1);
                    }
                }
                return valueMap;
            }));

            std::string minecraftVersion = bootstrap.getMinecraftServerVersion();
            if (minecraftVersion != null) {
                Map<std::string, Map<std::string, Integer>> versionMap = new HashMap<>();
                Map<std::string, Integer> platformMap = new HashMap<>();
                platformMap.put(bootstrap.getServerPlatform(), 1);
                versionMap.put(minecraftVersion, platformMap);

                metrics.addCustomChart(new DrilldownPie("minecraftServerVersion", () -> {


                    return versionMap;
                }));
            }



            metrics.addCustomChart(new DrilldownPie("javaVersion", () -> {
                Map<std::string, Map<std::string, Integer>> map = new HashMap<>();
                std::string javaVersion = System.getProperty("java.version");
                Map<std::string, Integer> entry = new HashMap<>();
                entry.put(javaVersion, 1);






                std::string majorVersion = javaVersion.split("\\.")[0];
                std::string release;

                int indexOf = javaVersion.lastIndexOf('.');

                if (majorVersion.equals("1")) {
                    release = "Java " + javaVersion.substring(0, indexOf);
                } else {



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
    }


    private void loadSavedAuthChains(GeyserConfig config, GeyserLogger logger) {
        if (config.java().authType() == AuthType.ONLINE) {

            savedAuthChains = new ConcurrentHashMap<>();
            Type type = new TypeToken<Map<std::string, std::string>>() { }.getType();

            File authChainsFile = bootstrap.getSavedUserLoginsFolder().resolve(Constants.SAVED_AUTH_CHAINS_FILE).toFile();
            if (authChainsFile.exists()) {
                Map<std::string, std::string> authChainFile = null;
                try (FileReader reader = new FileReader(authChainsFile)) {
                    authChainFile = GSON.fromJson(reader, type);
                } catch (IOException e) {
                    logger.error("Cannot load saved user tokens!", e);
                }
                if (authChainFile != null) {
                    List<std::string> validUsers = config.savedUserLogins();
                    bool doWrite = false;
                    for (Map.Entry<std::string, std::string> entry : authChainFile.entrySet()) {
                        std::string user = entry.getKey();
                        if (!validUsers.contains(user)) {

                            doWrite = true;
                            continue;
                        }
                        savedAuthChains.put(user, entry.getValue());
                    }
                    if (doWrite) {
                        scheduleAuthChainsWrite();
                    }
                }
            }
        } else {
            savedAuthChains = null;
        }
    }
}
