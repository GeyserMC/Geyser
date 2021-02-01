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

package org.geysermc.connector.command.defaults;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HelpCommand extends GeyserCommand {

    public GeyserConnector connector;

    public HelpCommand(GeyserConnector connector, String name, String description, String permission) {
        super(name, description, permission);
        this.connector = connector;

        this.setAliases(Collections.singletonList("?"));
    }

    @Override
    public void execute(GeyserSession session, CommandSender sender, String[] args) {
        int page = 1;
        int maxPage = 1;
        String header = LanguageUtils.getPlayerLocaleString("geyser.commands.help.header", sender.getLocale(), page, maxPage);

        sender.sendMessage(header);
        Map<String, GeyserCommand> cmds = connector.getCommandManager().getCommands();
        List<String> commands = connector.getCommandManager().getCommands().keySet().stream().sorted().collect(Collectors.toList());
        commands.forEach(cmd -> sender.sendMessage(ChatColor.YELLOW + "/geyser " + cmd + ChatColor.WHITE + ": " +
                LanguageUtils.getPlayerLocaleString(cmds.get(cmd).getDescription(), sender.getLocale())));
    }
}
