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

package org.geysermc.platform.spigot.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandExecutor;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeyserSpigotCommandExecutor extends CommandExecutor implements TabExecutor {

    public GeyserSpigotCommandExecutor(GeyserConnector connector) {
        super(connector);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            GeyserCommand geyserCommand = getCommand(args[0]);
            if (geyserCommand != null) {
                SpigotCommandSender commandSender = new SpigotCommandSender(sender);
                if (!sender.hasPermission(geyserCommand.getPermission())) {
                    String message = LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", commandSender.getLocale());

                    commandSender.sendMessage(ChatColor.RED + message);
                    return true;
                }
                GeyserSession session = null;
                if (geyserCommand.isBedrockOnly()) {
                    session = getGeyserSession(commandSender);
                    if (session == null) {
                        sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", commandSender.getLocale()));
                        return true;
                    }
                }
                geyserCommand.execute(session, commandSender, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
                return true;
            }
        } else {
            getCommand("help").execute(null, new SpigotCommandSender(sender), new String[0]);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return connector.getCommandManager().getCommandNames();
        }
        return new ArrayList<>();
    }
}
