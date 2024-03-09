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

package org.geysermc.geyser.command.defaults;

import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Collections;
import java.util.Map;

public class HelpCommand extends GeyserCommand {
    private final GeyserImpl geyser;
    private final String baseCommand;
    private final Map<String, Command> commands;

    public HelpCommand(GeyserImpl geyser, String name, String description, String permission,
                       String baseCommand, Map<String, Command> commands) {
        super(name, description, permission);
        this.geyser = geyser;
        this.baseCommand = baseCommand;
        this.commands = commands;

        this.setAliases(Collections.singletonList("?"));
    }

    /**
     * Sends the help menu to a command sender. Will not show certain commands depending on the command sender and session.
     *
     * @param session The Geyser session of the command sender, if it is a bedrock player. If null, bedrock-only commands will be hidden.
     * @param sender The CommandSender to send the help message to.
     * @param args Not used.
     */
    @Override
    public void execute(GeyserSession session, GeyserCommandSource sender, String[] args) {
        int page = 1;
        int maxPage = 1;
        String translationKey = this.baseCommand.equals("geyser") ? "geyser.commands.help.header" : "geyser.commands.extensions.header";
        String header = GeyserLocale.getPlayerLocaleString(translationKey, sender.locale(), page, maxPage);
        sender.sendMessage(header);

        this.commands.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Command cmd = entry.getValue();

            // Standalone hack-in since it doesn't have a concept of permissions
            if (geyser.getPlatformType() == PlatformType.STANDALONE || sender.hasPermission(cmd.permission())) {
                // Only list commands the player can actually run
                if (cmd.isBedrockOnly() && session == null) {
                    return;
                }

                sender.sendMessage(ChatColor.YELLOW + "/" + baseCommand + " " + entry.getKey() + ChatColor.WHITE + ": " +
                        GeyserLocale.getPlayerLocaleString(cmd.description(), sender.locale()));
            }
        });
    }
}
