/*
 * Copyright (c) 2020 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.fabric.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.fabric.GeyserFabricMod;

public class GeyserFabricCommandExecutor implements Command<ServerCommandSource> {

    private final String commandName;
    private final GeyserConnector connector;
    /**
     * Whether the command requires an OP permission level of 2 or greater
     */
    private final boolean requiresPermission;

    public GeyserFabricCommandExecutor(GeyserConnector connector, String commandName, boolean requiresPermission) {
        this.commandName = commandName;
        this.connector = connector;
        this.requiresPermission = requiresPermission;
    }

    @Override
    public int run(CommandContext context) {
        ServerCommandSource source = (ServerCommandSource) context.getSource();
        FabricCommandSender sender = new FabricCommandSender(source);
        if (requiresPermission && !source.hasPermissionLevel(2)) {
            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail"));
            return 0;
        }
        if (this.commandName.equals("reload")) {
            GeyserFabricMod.getInstance().setReloading(true);
        }
        getCommand(commandName).execute(sender, new String[0]);
        return 0;
    }

    private GeyserCommand getCommand(String label) {
        return connector.getCommandManager().getCommands().get(label);
    }
}
