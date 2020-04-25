/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import org.geysermc.common.command.ICommandManager;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.defaults.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class CommandManager implements ICommandManager {

    @Getter
    private final Map<String, GeyserCommand> commands = Collections.synchronizedMap(new HashMap<>());

    private GeyserConnector connector;

    public CommandManager(GeyserConnector connector) {
        this.connector = connector;

        registerCommand(new HelpCommand(connector, "help", "Shows help for all registered commands.", "geyser.command.help"));
        registerCommand(new ListCommand(connector, "list", "List all players connected through Geyser.", "geyser.command.list"));
        registerCommand(new ReloadCommand(connector, "reload", "Reloads the Geyser configurations. Kicks all players when used!", "geyser.command.reload"));
        registerCommand(new StopCommand(connector, "stop", "Shuts down Geyser.", "geyser.command.stop"));
        registerCommand(new OffhandCommand(connector, "offhand", "Puts an items in your offhand.", "geyser.command.offhand"));
    }

    public void registerCommand(GeyserCommand command) {
        commands.put(command.getName(), command);
        connector.getLogger().debug("Registered command " + command.getName());

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
            String argLine = command.substring(command.indexOf(" " + 1));
            args = argLine.contains(" ") ? argLine.split(" ") : new String[] { argLine };
        }

        GeyserCommand cmd = commands.get(label);
        if (cmd == null) {
            connector.getLogger().error("Invalid Command! Try /geyser help for a list of commands.");
            return;
        }

        cmd.execute(sender, args);
    }
}
