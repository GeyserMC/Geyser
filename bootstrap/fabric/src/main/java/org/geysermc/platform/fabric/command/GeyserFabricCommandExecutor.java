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
import org.geysermc.connector.command.CommandExecutor;
import org.geysermc.connector.command.GeyserCommand;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.fabric.GeyserFabricMod;
import org.geysermc.platform.fabric.GeyserFabricPermissions;

public class GeyserFabricCommandExecutor extends CommandExecutor implements Command<ServerCommandSource> {

    private final GeyserCommand command;
    /**
     * Whether the command requires an OP permission level of 2 or greater
     */
    private final boolean requiresPermission;

    public GeyserFabricCommandExecutor(GeyserConnector connector, GeyserCommand command, boolean requiresPermission) {
        super(connector);
        this.command = command;
        this.requiresPermission = requiresPermission;
    }

    /**
     * Determine whether or not a command source is allowed to run a given executor.
     *
     * @param source The command source attempting to run the command
     * @return True if the command source is allowed to
     */
    public boolean canRun(ServerCommandSource source) {
        return !requiresPermission() || source.hasPermissionLevel(GeyserFabricPermissions.RESTRICTED_MIN_LEVEL);
    }

    @Override
    public int run(CommandContext context) {
        ServerCommandSource source = (ServerCommandSource) context.getSource();
        FabricCommandSender sender = new FabricCommandSender(source);
        GeyserSession session = getGeyserSession(sender);
        if (!canRun(source)) {
            sender.sendMessage(LanguageUtils.getLocaleStringLog("geyser.bootstrap.command.permission_fail"));
            return 0;
        }
        if (this.command.getName().equals("reload")) {
            GeyserFabricMod.getInstance().setReloading(true);
        }

        if (command.isBedrockOnly() && session == null) {
            sender.sendMessage(ChatColor.RED + LanguageUtils.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", sender.getLocale()));
            return 0;
        }
        command.execute(session, sender, new String[0]);
        return 0;
    }

    public GeyserCommand getCommand() {
        return command;
    }

    /**
     * Returns whether the command requires permission level of {@link GeyserFabricPermissions#RESTRICTED_MIN_LEVEL} or higher to be ran
     */
    public boolean requiresPermission() {
        return requiresPermission;
    }
}
