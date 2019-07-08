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

import java.util.List;

public interface Command {

    /**
     * Returns the name of this command
     *
     * @return the name of this command
     */
    String getName();

    /**
     * Returns the description of this command
     *
     * @return the description of this command
     */
    String getDescription();

    /**
     * Returns the aliases of this command
     *
     * @return the aliases of this command
     */
    List<String> getAliases();

    /**
     * Sets the aliases of this command
     *
     * @param aliases the a liases of the command
     */
    void setAliases(List<String> aliases);

    /**
     * Executes the command
     *
     * @param sender the sender of the command
     * @param args the arguments of the command
     */
    void execute(CommandSender sender, String[] args);
}
