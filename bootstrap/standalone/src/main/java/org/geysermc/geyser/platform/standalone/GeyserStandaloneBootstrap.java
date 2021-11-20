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

package org.geysermc.geyser.platform.standalone;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.netty.util.ResourceLeakDetector;
import lombok.Getter;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.command.CommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.configuration.GeyserJacksonConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.platform.standalone.command.GeyserCommandManager;
import org.geysermc.geyser.platform.standalone.gui.GeyserStandaloneGUI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GeyserStandaloneBootstrap implements GeyserBootstrap {

    private GeyserCommandManager geyserCommandManager;
    private GeyserStandaloneConfiguration geyserConfig;
    private GeyserStandaloneLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;

    private GeyserStandaloneGUI gui;

    @Getter
    private boolean useGui = System.console() == null && !isHeadless();
    private String configFilename = "config.yml";

    private GeyserImpl geyser;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, String> argsConfigKeys = new HashMap<>();

    public static void main(String[] args) {
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED); // Can eat performance
        }

        GeyserStandaloneBootstrap bootstrap = new GeyserStandaloneBootstrap();
        // Set defaults
        boolean useGuiOpts = bootstrap.useGui;
        String configFilenameOpt = bootstrap.configFilename;

        List<BeanPropertyDefinition> availableProperties = getPOJOForClass(GeyserJacksonConfiguration.class);

        for (int i = 0; i < args.length; i++) {
            // By default, standalone Geyser will check if it should open the GUI based on if the GUI is null
            // Optionally, you can force the use of a GUI or no GUI by specifying args
            // Allows gui and nogui without options, for backwards compatibility
            String arg = args[i];
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
                    // We have likely added a config option argument
                    if (arg.startsWith("--")) {
                        // Split the argument by an =
                        String[] argParts = arg.substring(2).split("=");
                        if (argParts.length == 2) {
                            // Split the config key by . to allow for nested options
                            String[] configKeyParts = argParts[0].split("\\.");

                            // Loop the possible config options to check the passed key is valid
                            boolean found = false;
                            for (BeanPropertyDefinition property : availableProperties) {
                                if (configKeyParts[0].equals(property.getName())) {
                                    if (configKeyParts.length > 1) {
                                        // Loop sub-section options to check the passed key is valid
                                        for (BeanPropertyDefinition subProperty : getPOJOForClass(property.getRawPrimaryType())) {
                                            if (configKeyParts[1].equals(subProperty.getName())) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        found = true;
                                    }

                                    break;
                                }
                            }

                            // Add the found key to the stored list for later usage
                            if (found) {
                                argsConfigKeys.put(argParts[0], argParts[1]);
                                break;
                            }
                        }
                    }
                    System.err.println(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.unrecognised", arg));
                    return;
                }
            }
        }
        bootstrap.onEnable(useGuiOpts, configFilenameOpt);
    }

    public void onEnable(boolean useGui, String configFilename) {
        this.configFilename = configFilename;
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

            handleArgsConfigOptions();

            if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
                geyserConfig.setAutoconfiguredRemote(true); // Doesn't really need to be set but /shrug
                geyserConfig.getRemote().setAddress("127.0.0.1");
            }
        } catch (IOException ex) {
            geyserLogger.severe(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            if (gui == null) {
                System.exit(1);
            } else {
                // Leave the process running so the GUI is still visible
                return;
            }
        }
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        // Allow libraries like Protocol to have their debug information passthrough
        logger.get().setLevel(geyserConfig.isDebugMode() ? Level.DEBUG : Level.INFO);

        geyser = GeyserImpl.start(PlatformType.STANDALONE, this);
        geyserCommandManager = new GeyserCommandManager(geyser);

        if (gui != null) {
            gui.setupInterface(geyserLogger, geyserCommandManager);
        }

        geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);

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
        geyser.shutdown();
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

    /**
     * Get the {@link BeanPropertyDefinition}s for the given class
     *
     * @param clazz The class to get the definitions for
     * @return A list of {@link BeanPropertyDefinition} for the given class
     */
    public static List<BeanPropertyDefinition> getPOJOForClass(Class<?> clazz) {
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructType(clazz);

        // Introspect the given type
        BeanDescription beanDescription = OBJECT_MAPPER.getSerializationConfig().introspect(javaType);

        // Find properties
        List<BeanPropertyDefinition> properties = beanDescription.findProperties();

        // Get the ignored properties
        Set<String> ignoredProperties = OBJECT_MAPPER.getSerializationConfig().getAnnotationIntrospector()
                .findPropertyIgnorals(beanDescription.getClassInfo()).getIgnored();

        // Filter properties removing the ignored ones
        return properties.stream()
                .filter(property -> !ignoredProperties.contains(property.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Set a POJO property value on an object
     *
     * @param property The {@link BeanPropertyDefinition} to set
     * @param parentObject The object to alter
     * @param value The new value of the property
     */
    @SuppressWarnings("unchecked") // Required for enum usage
    private static void setConfigOption(BeanPropertyDefinition property, Object parentObject, Object value) {
        Object parsedValue = value;

        // Change the values type if needed
        if (int.class.equals(property.getRawPrimaryType())) {
            parsedValue = Integer.valueOf((String) parsedValue);
        } else if (boolean.class.equals(property.getRawPrimaryType())) {
            parsedValue = Boolean.valueOf((String) parsedValue);
        } else if (Enum.class.isAssignableFrom(property.getRawPrimaryType())) {
            parsedValue = Enum.valueOf((Class<? extends Enum>) property.getRawPrimaryType(), ((String) parsedValue).toUpperCase(Locale.ROOT));
        }

        // Force the value to be set
        AnnotatedField field = property.getField();
        field.fixAccess(true);
        field.setValue(parentObject, parsedValue);
    }

    /**
     * Update the loaded {@link GeyserStandaloneConfiguration} with any values passed in the command line arguments
     */
    private void handleArgsConfigOptions() {
        // Get the available properties from the class
        List<BeanPropertyDefinition> availableProperties = getPOJOForClass(GeyserJacksonConfiguration.class);

        for (Map.Entry<String, String> configKey : argsConfigKeys.entrySet()) {
            String[] configKeyParts = configKey.getKey().split("\\.");

            // Loop over the properties looking for any matches against the stored one from the argument
            for (BeanPropertyDefinition property : availableProperties) {
                if (configKeyParts[0].equals(property.getName())) {
                    if (configKeyParts.length > 1) {
                        // Loop through the sub property if the first part matches
                        for (BeanPropertyDefinition subProperty : getPOJOForClass(property.getRawPrimaryType())) {
                            if (configKeyParts[1].equals(subProperty.getName())) {
                                geyserLogger.info(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.set_config_option", configKey.getKey(), configKey.getValue()));

                                // Set the sub property value on the config
                                try {
                                    Object subConfig = property.getGetter().callOn(geyserConfig);
                                    setConfigOption(subProperty, subConfig, configKey.getValue());
                                } catch (Exception e) {
                                    geyserLogger.error("Failed to set config option: " + property.getFullName());
                                }

                                break;
                            }
                        }
                    } else {
                        geyserLogger.info(GeyserLocale.getLocaleStringLog("geyser.bootstrap.args.set_config_option", configKey.getKey(), configKey.getValue()));

                        // Set the property value on the config
                        try {
                            setConfigOption(property, geyserConfig, configKey.getValue());
                        } catch (Exception e) {
                            geyserLogger.error("Failed to set config option: " + property.getFullName());
                        }
                    }

                    break;
                }
            }
        }
    }
}
