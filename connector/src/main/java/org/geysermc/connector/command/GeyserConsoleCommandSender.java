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

import org.geysermc.api.command.ConsoleCommandSender;
import org.geysermc.connector.console.GeyserLogger;

public class GeyserConsoleCommandSender implements ConsoleCommandSender {

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public void sendMessage(String message) {
        System.out.println(GeyserLogger.printConsole(message, true));
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }
}
