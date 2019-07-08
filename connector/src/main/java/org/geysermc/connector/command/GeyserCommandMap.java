/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.command;

import org.geysermc.api.command.Command;
import org.geysermc.api.command.CommandMap;
import org.geysermc.api.command.CommandSender;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.defaults.HelpCommand;
import org.geysermc.connector.command.defaults.StopCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GeyserCommandMap implements CommandMap {

    private final Map<String, Command> commandMap = Collections.synchronizedMap(new HashMap<String, Command>());
    private GeyserConnector connector;

    public GeyserCommandMap(GeyserConnector connector) {
        this.connector = connector;

        registerDefaults();
    }

    public void registerDefaults() {
        registerCommand(new HelpCommand(connector, "help", "Shows help for all registered commands."));
        registerCommand(new StopCommand(connector, "stop", "Shut down Geyser."));
    }

    public void registerCommand(Command command) {
        commandMap.put(command.getName(), command);
        connector.getLogger().debug("Registered command " + command.getName());

        for (String alias : command.getAliases())
            commandMap.put(alias, command);
    }

    public void runCommand(CommandSender sender, String command) {
        String trim = command.trim();
        String label = null;
        String[] args = null;

        if (!trim.contains(" ")) {
            label = trim.toLowerCase();
            args = new String[0];
        } else {
            label = trim.substring(0, trim.indexOf(" ")).toLowerCase();
            String argLine = trim.substring(trim.indexOf(" " + 1));
            args = argLine.contains(" ") ? argLine.split(" ") : new String[] { argLine };
        }

        if (label == null) {
            connector.getLogger().warning("Invalid Command! Try /help for a list of commands.");
            return;
        }

        Command cmd = commandMap.get(label);
        if (cmd == null) {
            connector.getLogger().warning("Invalid Command! Try /help for a list of commands.");
            return;
        }

        cmd.execute(sender, args);
    }

    public Map<String, Command> getCommands() {
        return commandMap;
    }
}
