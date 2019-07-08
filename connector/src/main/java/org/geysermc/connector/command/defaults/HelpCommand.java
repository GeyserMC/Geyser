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

package org.geysermc.connector.command.defaults;

import org.geysermc.api.ChatColor;
import org.geysermc.api.command.Command;
import org.geysermc.api.command.CommandSender;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HelpCommand extends GeyserCommand {

    public GeyserConnector connector;

    public HelpCommand(GeyserConnector connector, String name, String description) {
        super(name, description);
        this.connector = connector;

        this.setAliases(Arrays.asList("?"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("---- Showing Help For: Geyser (Page 1/1) ----");
        Map<String, Command> cmds = connector.getCommandMap().getCommands();

        List<String> commands = new ArrayList<String>(cmds.keySet());
        Collections.sort(commands);

        for (String cmd : commands) {
            sender.sendMessage(ChatColor.YELLOW + "/" + cmd + ChatColor.WHITE + ": " + cmds.get(cmd).getDescription());
        }
    }
}
