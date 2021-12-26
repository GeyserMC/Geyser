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

package org.geysermc.geyser.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.defaults.*;
import org.geysermc.geyser.configuration.CustomCommandsConfiguration;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    @Getter
    private final Map<String, GeyserCommand> commands = new HashMap<>();
    @Getter
    private final Map<String, String> commandDescriptionOverrides;

    private final GeyserImpl geyser;

    public CommandManager(GeyserImpl geyser) {
        this.geyser = geyser;

        registerCommand(new HelpCommand(geyser, "help", "geyser.commands.help.desc", "geyser.command.help"));
        registerCommand(new ListCommand(geyser, "list", "geyser.commands.list.desc", "geyser.command.list"));
        registerCommand(new ReloadCommand(geyser, "reload", "geyser.commands.reload.desc", "geyser.command.reload"));
        registerCommand(new OffhandCommand(geyser, "offhand", "geyser.commands.offhand.desc", "geyser.command.offhand"));
        registerCommand(new DumpCommand(geyser, "dump", "geyser.commands.dump.desc", "geyser.command.dump"));
        registerCommand(new VersionCommand(geyser, "version", "geyser.commands.version.desc", "geyser.command.version"));
        registerCommand(new SettingsCommand(geyser, "settings", "geyser.commands.settings.desc", "geyser.command.settings"));
        registerCommand(new StatisticsCommand(geyser, "statistics", "geyser.commands.statistics.desc", "geyser.command.statistics"));
        registerCommand(new AdvancementsCommand("advancements", "geyser.commands.advancements.desc", "geyser.command.advancements"));
        registerCommand(new AdvancedTooltipsCommand("tooltips", "geyser.commands.advancedtooltips.desc", "geyser.command.tooltips"));
        if (GeyserImpl.getInstance().getPlatformType() == PlatformType.STANDALONE) {
            registerCommand(new StopCommand(geyser, "stop", "geyser.commands.stop.desc", "geyser.command.stop"));
        }

        // Read or create command overrides
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        File commandsFile = geyser.getBootstrap().getConfigFolder().resolve("commands.yml").toFile();
        if (!commandsFile.exists()) {
            try (InputStream stream = geyser.getBootstrap().getResource("commands.yml")) {
                Files.copy(stream, commandsFile.toPath());
            } catch (IOException e) {
                geyser.getLogger().error("Unable to load commands.yml template from jar!", e);
            }
        }
        CustomCommandsConfiguration commandsConfiguration;
        try {
            commandsConfiguration = objectMapper.readValue(commandsFile, CustomCommandsConfiguration.class);
        } catch (IOException e) {
            geyser.getLogger().error("Unable to load commands configuration!", e);
            commandsConfiguration = new CustomCommandsConfiguration();
        }

        this.commandDescriptionOverrides = new Object2ObjectOpenHashMap<>();
        for (CustomCommandsConfiguration.CustomCommandEntry entry : commandsConfiguration.getDescriptions()) {
            for (String name : entry.getCommands()) {
                commandDescriptionOverrides.put(name, entry.getDescription());
            }
        }
    }

    public void registerCommand(GeyserCommand command) {
        commands.put(command.getName(), command);
        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.commands.registered", command.getName()));

        if (command.getAliases().isEmpty())
            return;

        for (String alias : command.getAliases())
            commands.put(alias, command);
    }

    public void runCommand(CommandSender sender, String command) {
        if (!command.startsWith("geyser "))
            return;

        command = command.trim().replace("geyser ", "");
        String label;
        String[] args;

        if (!command.contains(" ")) {
            label = command.toLowerCase();
            args = new String[0];
        } else {
            label = command.substring(0, command.indexOf(" ")).toLowerCase();
            String argLine = command.substring(command.indexOf(" ") + 1);
            args = argLine.contains(" ") ? argLine.split(" ") : new String[] { argLine };
        }

        GeyserCommand cmd = commands.get(label);
        if (cmd == null) {
            geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.invalid"));
            return;
        }

        if (sender instanceof GeyserSession) {
            cmd.execute((GeyserSession) sender, sender, args);
        } else {
            if (!cmd.isBedrockOnly()) {
                cmd.execute(null, sender, args);
            } else {
                geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.bedrock_only"));
            }
        }
    }

    /**
     * Returns the description of the given command
     *
     * @param command Command to get the description for
     * @return Command description
     */
    @Nonnull
    public String getDescription(String command) {
        // Most platforms don't have an option to get the description, so we rely on our overrides instead
        return commandDescriptionOverrides.getOrDefault(command, "");
    }
}
