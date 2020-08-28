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

package org.geysermc.platform.standalone;

import lombok.Getter;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.common.PlatformType;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.event.events.geyser.GeyserStartEvent;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.standalone.command.GeyserCommandManager;
import org.geysermc.platform.standalone.gui.GeyserStandaloneGUI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.UUID;

public class GeyserStandaloneBootstrap implements GeyserBootstrap {

    private GeyserCommandManager geyserCommandManager;
    private GeyserStandaloneConfiguration geyserConfig;
    private GeyserStandaloneLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;

    private GeyserStandaloneGUI gui;

    @Getter
    private boolean useGui = System.console() == null && !isHeadless();
    private String configFilename = "config.yml";

    private GeyserConnector connector;


    public static void main(String[] args) {
        GeyserStandaloneBootstrap bootstrap = new GeyserStandaloneBootstrap();
        // Set defaults
        boolean useGuiOpts = bootstrap.useGui;
        String configFilenameOpt = bootstrap.configFilename;

        for (int i = 0; i < args.length; i++) {
            // By default, standalone Geyser will check if it should open the GUI based on if the GUI is null
            // Optionally, you can force the use of a GUI or no GUI by specifying args
            // Allows gui and nogui without options, for backwards compatibility
            String arg = args[i];
            switch (arg) {
                case "--gui":
                case "gui":
                    useGuiOpts = true;
                    break;
                case "--nogui":
                case "nogui":
                    useGuiOpts = false;
                    break;
                case "--config":
                case "-c":
                    if (i >= args.length - 1) {
                        System.err.println(MessageFormat.format(LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.confignotspecified"), "-c"));
                        return;
                    }
                    configFilenameOpt = args[i+1]; i++;
                    System.out.println(MessageFormat.format(LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.configspecified"), configFilenameOpt));
                    break;
                case "--help":
                case "-h":
                    System.out.println(MessageFormat.format(LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.usage"), "[java -jar] Geyser.jar [opts]"));
                    System.out.println("  " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.options"));
                    System.out.println("    -c, --config [file]    " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.config"));
                    System.out.println("    -h, --help             " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.help"));
                    System.out.println("    --gui, --nogui         " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.gui"));
                    return;
                default:
                    String badArgMsg = LanguageUtils.getLocaleStringLog("geyser.bootstrap.args.unrecognised");
                    System.err.println(MessageFormat.format(badArgMsg, arg));
                    return;
            }
        }
        bootstrap.onEnable(useGuiOpts, configFilenameOpt);
    }

    public void onEnable(boolean useGui, String configFilename) {
        this.configFilename = configFilename;
        this.useGui = useGui;
        this.onEnable();
    }

    public void onEnable(boolean useGui) {
        this.useGui = useGui;
        this.onEnable();
    }

    @Override
    public void onEnable() {
        Logger logger = (Logger) LogManager.getRootLogger();
        for (Appender appender : logger.getAppenders().values()) {
            // Remove the appender that is not in use
            // Prevents multiple appenders/double logging and removes harmless errors
            if ((useGui && appender instanceof TerminalConsoleAppender) || (!useGui && appender instanceof ConsoleAppender)) {
                logger.removeAppender(appender);
            }
        }
        if (useGui && gui == null) {
            gui = new GeyserStandaloneGUI();
            gui.redirectSystemStreams();
            gui.startUpdateThread();
        }

        geyserLogger = new GeyserStandaloneLogger();

        LoopbackUtil.checkLoopback(geyserLogger);
        
        try {
            File configFile = FileUtils.fileOrCopiedFromResource(new File(configFilename), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            geyserConfig = FileUtils.loadConfig(configFile, GeyserStandaloneConfiguration.class);
            if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
                geyserConfig.setAutoconfiguredRemote(true); // Doesn't really need to be set but /shrug
                geyserConfig.getRemote().setAddress("127.0.0.1");
            }
        } catch (IOException ex) {
            geyserLogger.severe(LanguageUtils.getLocaleStringLog("geyser.config.failed"), ex);
            System.exit(0);
        }
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        connector = GeyserConnector.start(PlatformType.STANDALONE, this);
        geyserCommandManager = new GeyserCommandManager(connector);

        if (gui != null) {
            gui.setupInterface(geyserLogger, geyserCommandManager);
        }

        geyserPingPassthrough = GeyserLegacyPingPassthrough.init(connector);

        // Trigger GeyserStart Events
        connector.getEventManager().triggerEvent(new GeyserStartEvent());

        if (!useGui) {
            geyserLogger.start(); // Throws an error otherwise
        }
    }

    /**
     * Check using {@link java.awt.GraphicsEnvironment} that we are a headless client
     *
     * @return If the current environment is headless
     */
    private boolean isHeadless() {
        try {
            Class<?> graphicsEnv = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadless = graphicsEnv.getDeclaredMethod("isHeadless");
            return (boolean) isHeadless.invoke(null);
        } catch (Exception ignore) { }

        return true;
    }

    @Override
    public void onDisable() {
        connector.shutdown();
        System.exit(0);
    }

    @Override
    public GeyserConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserStandaloneLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandManager getGeyserCommandManager() {
        return geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        // Return the current working directory
        return Paths.get(System.getProperty("user.dir"));
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserStandaloneDumpInfo(this);
    }
}
