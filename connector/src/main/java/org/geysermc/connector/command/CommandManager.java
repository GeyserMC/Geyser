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

package org.geysermc.connector.command;

import lombok.Getter;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.defaults.*;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.*;

public abstract class CommandManager {

    @Getter
    private final Map<String, GeyserCommand> commands = Collections.synchronizedMap(new HashMap<>());

    private final GeyserConnector connector;

    public CommandManager(GeyserConnector connector) {
        this.connector = connector;

        registerCommand(new HelpCommand(connector, "help", "geyser.commands.help.desc", "geyser.command.help"));
        registerCommand(new ListCommand(connector, "list", "geyser.commands.list.desc", "geyser.command.list"));
        registerCommand(new ReloadCommand(connector, "reload", "geyser.commands.reload.desc", "geyser.command.reload"));
        registerCommand(new StopCommand(connector, "stop", "geyser.commands.stop.desc", "geyser.command.stop"));
        registerCommand(new OffhandCommand(connector, "offhand", "geyser.commands.offhand.desc", "geyser.command.offhand"));
        registerCommand(new DumpCommand(connector, "dump", "geyser.commands.dump.desc", "geyser.command.dump"));
        registerCommand(new VersionCommand(connector, "version", "geyser.commands.version.desc", "geyser.command.version"));
        registerCommand(new SettingsCommand(connector, "settings", "geyser.commands.settings.desc", "geyser.command.settings"));
        registerCommand(new StatisticsCommand(connector, "statistics", "geyser.commands.statistics.desc", "geyser.command.statistics"));
        registerCommand(new AdvancementsCommand("advancements", "geyser.commands.advancements.desc", "geyser.command.advancements"));
    }

    public void registerCommand(GeyserCommand command) {
        commands.put(command.getName(), command);
        connector.getLogger().debug(LanguageUtils.getLocaleStringLog("geyser.commands.registered", command.getName()));

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
            connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.commands.invalid"));
            return;
        }

        if (sender instanceof GeyserSession) {
            cmd.execute((GeyserSession) sender, sender, args);
        } else {
            if (!cmd.isBedrockOnly()) {
                cmd.execute(null, sender, args);
            } else {
                connector.getLogger().error(LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.bedrock_only"));
            }
        }
    }

    /**
     * @return a list of all subcommands under {@code /geyser}.
     */
    public List<String> getCommandNames() {
        return Arrays.asList(connector.getCommandManager().getCommands().keySet().toArray(new String[0]));
    }

    /**
     * Returns the description of the given command
     *
     * @param command Command to get the description for
     * @return Command description
     */
    public abstract String getDescription(String command);
}
