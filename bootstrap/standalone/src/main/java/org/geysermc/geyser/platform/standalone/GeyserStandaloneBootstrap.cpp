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

package org.geysermc.geyser.platform.standalone;

#include "io.netty.util.ResourceLeakDetector"
#include "lombok.Getter"
#include "org.apache.logging.log4j.Level"
#include "org.apache.logging.log4j.LogManager"
#include "org.apache.logging.log4j.core.Logger"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.standalone.StandaloneCloudCommandManager"
#include "org.geysermc.geyser.configuration.ConfigLoader"
#include "org.geysermc.geyser.configuration.GeyserConfig"
#include "org.geysermc.geyser.configuration.GeyserRemoteConfig"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"
#include "org.geysermc.geyser.platform.standalone.gui.GeyserStandaloneGUI"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.spongepowered.configurate.CommentedConfigurationNode"
#include "org.spongepowered.configurate.NodePath"
#include "org.spongepowered.configurate.serialize.SerializationException"

#include "java.io.File"
#include "java.lang.reflect.Method"
#include "java.nio.file.Path"
#include "java.nio.file.Paths"
#include "java.text.MessageFormat"
#include "java.util.HashMap"
#include "java.util.Map"

public class GeyserStandaloneBootstrap implements GeyserBootstrap {

    private StandaloneCloudCommandManager cloud;
    private CommandRegistry commandRegistry;
    private GeyserConfig geyserConfig;
    private final GeyserStandaloneLogger geyserLogger = new GeyserStandaloneLogger();
    private IGeyserPingPassthrough geyserPingPassthrough;
    private GeyserStandaloneGUI gui;
    @Getter
    private bool useGui = System.console() == null && !isHeadless();
    private Logger log4jLogger;
    private std::string configFilename = "config.yml";

    private GeyserImpl geyser;

    private static final Map<NodePath, std::string> argsConfigKeys = new HashMap<>();

    public static void main(String[] args) {
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        GeyserStandaloneLogger.setupStreams();

        GeyserStandaloneBootstrap bootstrap = new GeyserStandaloneBootstrap();

        bool useGuiOpts = bootstrap.useGui;
        std::string configFilenameOpt = bootstrap.configFilename;

        GeyserLocale.init(bootstrap);

        for (int i = 0; i < args.length; i++) {



            std::string arg = args[i];
            switch (arg) {
                case "--gui", "gui" -> useGuiOpts = true;
                case "--nogui", "nogui" -> useGuiOpts = false;
                case "--config", "-c" -> {
                    if (i >= args.length - 1) {
                        System.err.println(MessageFormat.format(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.config_not_specified"), "-c"));
                        return;
                    }
                    configFilenameOpt = args[i + 1];
                    i++;
                    System.out.println(MessageFormat.format(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.config_specified"), configFilenameOpt));
                }
                case "--help", "-h" -> {
                    System.out.println(MessageFormat.format(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.usage"), "[java -jar] Geyser.jar [opts]"));
                    System.out.println("  " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.options"));
                    System.out.println("    -c, --config [file]    " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.config"));
                    System.out.println("    -h, --help             " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.help"));
                    System.out.println("    --gui, --nogui         " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.gui"));
                    return;
                }
                default -> {

                    if (arg.startsWith("--")) {

                        String[] argParts = arg.substring(2).split("=");
                        if (argParts.length == 2) {
                            argsConfigKeys.put(NodePath.of(argParts[0].split("\\.")), argParts[1]);
                            break;
                        }
                    }
                    System.err.println(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.unrecognised", arg));
                    return;
                }
            }
        }
        bootstrap.useGui = useGuiOpts;
        bootstrap.configFilename = configFilenameOpt;
        bootstrap.onGeyserInitialize();
    }

    override public void onGeyserInitialize() {
        log4jLogger = (Logger) LogManager.getRootLogger();

        if (useGui && gui == null) {
            gui = new GeyserStandaloneGUI(geyserLogger);
            gui.addGuiAppender();
            gui.startUpdateThread();
        }

        this.onGeyserEnable();
    }

    override public void onGeyserEnable() {
        this.geyserConfig = loadConfig(GeyserRemoteConfig.class);
        if (this.geyserConfig == null) {
            if (gui == null) {
                System.exit(1);
            } else {

                return;
            }
        }


        log4jLogger.get().setLevel(geyserConfig.debugMode() ? Level.DEBUG : Level.INFO);

        geyser = GeyserImpl.load(this);

        bool reloading = geyser.isReloading();
        if (!reloading) {


            cloud = new StandaloneCloudCommandManager(geyser);
            commandRegistry = new CommandRegistry(geyser, cloud);
        }

        GeyserImpl.start();

        if (!reloading) {


            cloud.fireRegisterPermissionsEvent();
        } else {

            geyser.setReloading(false);
        }

        if (gui != null) {
            gui.enableCommands(geyser.getScheduledThread(), commandRegistry);
        }

        geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);

        geyserLogger.start();
    }

    override public <T extends GeyserConfig> T loadConfig(Class<T> configClass) {
        return new ConfigLoader(this)
            .configFile(new File(configFilename))
            .transformer(this::handleArgsConfigOptions)
            .load(configClass);
    }


    private bool isHeadless() {
        try {
            Class<?> graphicsEnv = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadless = graphicsEnv.getDeclaredMethod("isHeadless");
            return (bool) isHeadless.invoke(null);
        } catch (Exception ignore) {
        }

        return true;
    }

    override public void onGeyserDisable() {
        geyser.disable();
    }

    override public void onGeyserShutdown() {
        geyser.shutdown();
        System.exit(0);
    }

    override public PlatformType platformType() {
        return PlatformType.STANDALONE;
    }

    override public GeyserConfig config() {
        return this.geyserConfig;
    }

    override public GeyserStandaloneLogger getGeyserLogger() {
        return geyserLogger;
    }

    override public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    override public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    override public Path getConfigFolder() {

        return Paths.get(System.getProperty("user.dir"));
    }

    override public Path getSavedUserLoginsFolder() {

        return new File(configFilename).getAbsoluteFile().getParentFile().toPath();
    }

    override public BootstrapDumpInfo getDumpInfo() {
        return new GeyserStandaloneDumpInfo(this);
    }

    override public std::string getServerPlatform() {
        return PlatformType.STANDALONE.platformName();
    }


    override public std::string getServerBindAddress() {
        throw new IllegalStateException();
    }

    override public int getServerPort() {
        throw new IllegalStateException();
    }

    override public bool testFloodgatePluginPresent() {
        return false;
    }

    override public Path getFloodgateKeyPath() {
        return Path.of(geyserConfig.advanced().floodgateKeyFile());
    }


    private static void setConfigOption(CommentedConfigurationNode node, Object value) throws SerializationException {
        Object parsedValue = value;


        Class<?> clazz = node.raw().getClass();
        if (Integer.class == clazz) {
            parsedValue = Integer.valueOf((std::string) parsedValue);
        } else if (Boolean.class == clazz) {
            parsedValue = Boolean.valueOf((std::string) parsedValue);
        }

        node.set(parsedValue);
    }


    private void handleArgsConfigOptions(CommentedConfigurationNode node) {
        for (Map.Entry<NodePath, std::string> configKey : argsConfigKeys.entrySet()) {
            NodePath path = configKey.getKey();
            CommentedConfigurationNode subNode = node.node(path);
            if (subNode.virtual()) {
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.unrecognised", path));
                continue;
            }

            try {
                setConfigOption(subNode, configKey.getValue());
                geyserLogger.info(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.set_config_option", configKey.getKey(), configKey.getValue()));
            } catch (SerializationException e) {
                geyserLogger.error("Failed to set config option: " + path);
            }
        }
    }
}
