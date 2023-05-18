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

package org.geysermc.geyser.platform.bungeecord.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandExecutor;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class GeyserBungeeCommandExecutor extends Command implements TabExecutor {
    private final GeyserCommandExecutor commandExecutor;

    public GeyserBungeeCommandExecutor(String name, GeyserImpl geyser, Map<String, org.geysermc.geyser.api.command.Command> commands) {
        super(name);

        this.commandExecutor = new GeyserCommandExecutor(geyser, commands);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        BungeeCommandSource commandSender = new BungeeCommandSource(sender);
        GeyserSession session = this.commandExecutor.getGeyserSession(commandSender);

        if (args.length > 0) {
            GeyserCommand command = this.commandExecutor.getCommand(args[0]);
            if (command != null) {
                if (!sender.hasPermission(command.permission())) {
                    String message = GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", commandSender.locale());

                    commandSender.sendMessage(ChatColor.RED + message);
                    return;
                }
                if (command.isBedrockOnly() && session == null) {
                    String message = GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", commandSender.locale());

                    commandSender.sendMessage(ChatColor.RED + message);
                    return;
                }
                command.execute(session, commandSender, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
            } else {
                String message = GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.not_found", commandSender.locale());
                commandSender.sendMessage(ChatColor.RED + message);
            }
        } else {
            this.commandExecutor.getCommand("help").execute(session, commandSender, new String[0]);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return commandExecutor.tabComplete(new BungeeCommandSource(sender));
        } else {
            return Collections.emptyList();
        }
    }
}
