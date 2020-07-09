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

package org.geysermc.platform.velocity.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;

import lombok.AllArgsConstructor;

import net.kyori.text.TextComponent;

import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.utils.LanguageUtils;

@AllArgsConstructor
public class GeyserVelocityCommandExecutor implements Command {

    private GeyserConnector connector;

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length > 0) {
            if (getCommand(args[0]) != null) {
                if (!source.hasPermission(getCommand(args[0]).getPermission())) {
                    // Not ideal to use log here but we dont get a session
                    source.sendMessage(TextComponent.of(ChatColor.RED + LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail")));
                    return;
                }
                getCommand(args[0]).execute(new VelocityCommandSender(source), args);
            }
        } else {
            getCommand("help").execute(new VelocityCommandSender(source), args);
        }
    }

    private GeyserCommand getCommand(String label) {
        return connector.getCommandManager().getCommands().get(label);
    }
}
