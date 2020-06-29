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

package org.geysermc.connector.edition.mcee.commands;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.edition.mcee.utils.TokenManager;

public class EducationCommand extends GeyserCommand {

    public GeyserConnector connector;
    private final TokenManager tokenManager;

    public EducationCommand(GeyserConnector connector, String name, String description, String permission, TokenManager tokenManager) {
        super(name, description, permission);
        this.connector = connector;
        this.tokenManager = tokenManager;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("---- Education SubCommands ----");
        sender.sendMessage(ChatColor.YELLOW + "/education new" + ChatColor.WHITE + "    - Create new Authorization URL");
        sender.sendMessage(ChatColor.YELLOW + "/education confirm" + ChatColor.WHITE + "    - Confirm an Authorization Response");
        sender.sendMessage("");
        sender.sendMessage("Use '" + ChatColor.YELLOW + "new" + ChatColor.WHITE + "' to generate a URL that you copy into your browser.");
        sender.sendMessage("This will allow you to log into your MCEE account. Once done you will have a white page with a URL both in");
        sender.sendMessage("its title as well as address bar. Copy the full address and provide it as a parameter to '" + ChatColor.YELLOW + "confirm" + ChatColor.WHITE + "'.");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.isConsole()) {
            return;
        }

        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        switch(args[0].toLowerCase()) {
            case "new":
                sender.sendMessage("Copy and paste the following into your web browser:");
                sender.sendMessage("");
                sender.sendMessage("   " + ChatColor.YELLOW + tokenManager.getNewAuthorizationUrl().toString());
                break;
            case "confirm":
                if (args.length < 2) {
                    sender.sendMessage("Missing parameter");
                    return;
                }
                try {
                    tokenManager.createInitialToken(args[1]);
                } catch (TokenManager.TokenException e) {
                    sender.sendMessage("Error: " + e.getMessage());
                }
                sender.sendMessage("Successfully created new token");
                break;
            default:
                showHelp(sender);
        }
    }
}
