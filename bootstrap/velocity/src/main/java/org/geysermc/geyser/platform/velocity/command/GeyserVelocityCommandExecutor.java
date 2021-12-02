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

package org.geysermc.geyser.platform.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandExecutor;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GeyserVelocityCommandExecutor extends CommandExecutor implements SimpleCommand {

    public GeyserVelocityCommandExecutor(GeyserImpl geyser) {
        super(geyser);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSender sender = new VelocityCommandSender(invocation.source());
        GeyserSession session = getGeyserSession(sender);

        if (invocation.arguments().length > 0) {
            GeyserCommand command = getCommand(invocation.arguments()[0]);
            if (command != null) {
                if (!invocation.source().hasPermission(getCommand(invocation.arguments()[0]).getPermission())) {
                    sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.getLocale()));
                    return;
                }
                if (command.isBedrockOnly() && session == null) {
                    sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", sender.getLocale()));
                    return;
                }
                command.execute(session, sender, invocation.arguments().length > 1 ? Arrays.copyOfRange(invocation.arguments(), 1, invocation.arguments().length) : new String[0]);
            }
        } else {
            getCommand("help").execute(session, sender, new String[0]);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        // Velocity seems to do the splitting a bit differently. This results in the same behaviour in bungeecord/spigot.
        if (invocation.arguments().length == 0 || invocation.arguments().length == 1) {
            return tabComplete(new VelocityCommandSender(invocation.source()));
        }
        return Collections.emptyList();
    }
}
