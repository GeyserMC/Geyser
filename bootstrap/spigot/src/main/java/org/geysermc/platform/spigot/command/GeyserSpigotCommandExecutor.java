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

package org.geysermc.platform.spigot.command;

import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class GeyserSpigotCommandExecutor implements TabExecutor {

    private GeyserConnector connector;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (getCommand(args[0]) != null) {
                if (!sender.hasPermission(getCommand(args[0]).getPermission())) {
                    String message = "";
                    if (sender instanceof GeyserSession) {
                        message = LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", ((GeyserSession) sender).getClientData().getLanguageCode());
                    } else {
                        message = LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail");
                    }

                    sender.sendMessage(ChatColor.RED + message);
                    return true;
                }
                getCommand(args[0]).execute(new SpigotCommandSender(sender), args);
                return true;
            }
        } else {
            getCommand("help").execute(new SpigotCommandSender(sender), args);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("?", "help", "reload", "shutdown", "stop");
        }
        return new ArrayList<>();
    }

    private GeyserCommand getCommand(String label) {
        return connector.getCommandManager().getCommands().get(label);
    }
}
