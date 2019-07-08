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
import org.geysermc.api.command.CommandSender;

import java.util.List;

public abstract class GeyserCommand implements Command {

    private String name;
    private String description;
    private GeyserCommandMap commandMap;

    private List<String> aliases;

    public GeyserCommand(String name) {
        this(name,  "A geyser command.");
    }

    public GeyserCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    @Override
    public abstract void execute(CommandSender sender, String[] args);

    public GeyserCommandMap getCommandMap() {
        return commandMap;
    }
}
