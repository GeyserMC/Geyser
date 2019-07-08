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

package org.geysermc.api.command;

import java.util.Map;

public interface CommandMap {

    /**
     * Registers a new command
     *
     * @param command the command to register
     */
    void registerCommand(Command command);

    /**
     * Runs a command for the given command sender
     *
     * @param sender the sender to run the command for
     * @param command the command to run
     */
    void runCommand(CommandSender sender, String command);

    /**
     * Returns a map of the commands
     *
     * @return a map of the commands
     */
    Map<String, Command> getCommands();
}
