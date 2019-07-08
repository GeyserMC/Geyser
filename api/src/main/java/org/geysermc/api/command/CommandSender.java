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

public interface CommandSender {

    /**
     * Returns the name of the sender
     *
     * @return the name of the sender
     */
    String getName();

    /**
     * Sends a message to the command sender
     *
     * @param message the message to be sent
     */
    void sendMessage(String message);

    /**
     * Sends multiple messages to the command sender
     *
     * @param messages the messages to be sent
     */
    void sendMessage(String[] messages);
}
