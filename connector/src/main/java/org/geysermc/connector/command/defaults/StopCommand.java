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

import org.geysermc.api.command.CommandSender;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserCommand;

import java.util.Arrays;

public class StopCommand extends GeyserCommand {

    public GeyserConnector connector;

    public StopCommand(GeyserConnector connector, String name, String description) {
        super(name, description);
        this.connector = connector;

        this.setAliases(Arrays.asList("shutdown"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        connector.shutdown();
    }
}
