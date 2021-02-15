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

package org.geysermc.platform.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandExecutor;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeyserVelocityCommandExecutor extends CommandExecutor implements SimpleCommand {

    public GeyserVelocityCommandExecutor(GeyserConnector connector) {
        super(connector);
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length > 0) {
            GeyserCommand command = getCommand(invocation.arguments()[0]);
            if (command != null) {
                CommandSender sender = new VelocityCommandSender(invocation.source());
                if (!invocation.source().hasPermission(getCommand(invocation.arguments()[0]).getPermission())) {
                    sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.getLocale()));
                    return;
                }
                GeyserSession session = null;
                if (command.isBedrockOnly()) {
                    session = getGeyserSession(sender);
                    if (session == null) {
                        sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", sender.getLocale()));
                        return;
                    }
                }
                command.execute(session, sender, invocation.arguments().length > 1 ? Arrays.copyOfRange(invocation.arguments(), 1, invocation.arguments().length) : new String[0]);
            }
        } else {
            getCommand("help").execute(null, new VelocityCommandSender(invocation.source()), new String[0]);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return connector.getCommandManager().getCommandNames();
        }
        return new ArrayList<>();
    }
}
