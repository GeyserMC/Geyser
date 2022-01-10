/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.spigot.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.geysermc.geyser.command.CommandExecutor;
import org.geysermc.geyser.command.CommandManager;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GeyserSpigotCommandExecutor extends CommandExecutor implements TabExecutor {

    public GeyserSpigotCommandExecutor(CommandManager commandManager) {
        super(commandManager);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        SpigotCommandSender commandSender = new SpigotCommandSender(sender);
        GeyserSession session = commandSender.asGeyserSession();

        if (args.length > 0) {
            GeyserCommand geyserCommand = getCommand(args[0]);
            if (geyserCommand != null) {
                if (!sender.hasPermission(geyserCommand.getPermission())) {
                    String message = GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", commandSender.getLocale());

                    commandSender.sendMessage(ChatColor.RED + message);
                    return true;
                }
                if (geyserCommand.isBedrockOnly() && session == null) {
                    sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", commandSender.getLocale()));
                    return true;
                }
                geyserCommand.execute(session, commandSender, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
                return true;
            } else {
                String message = GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.not_found", commandSender.getLocale());

                commandSender.sendMessage(ChatColor.RED + message);
            }
        } else {
            getCommand("help").execute(session, commandSender, new String[0]);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return tabComplete(new SpigotCommandSender(sender));
        }
        return Collections.emptyList();
    }
}
