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

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent;
import com.mojang.brigadier.tree.CommandNode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

public final class GeyserPaperCommandListener implements Listener {

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onCommandSend(AsyncPlayerSendCommandsEvent<?> event) {
        // Documentation says to check (event.isAsynchronous() || !event.hasFiredAsync()), but as of Paper 1.18.2
        // event.hasFiredAsync is never true
        if (event.isAsynchronous()) {
            CommandNode<?> geyserBrigadier = event.getCommandNode().getChild("geyser");
            if (geyserBrigadier != null) {
                Player player = event.getPlayer();
                boolean isJavaPlayer = isProbablyJavaPlayer(player);
                Map<String, Command> commands = GeyserImpl.getInstance().commandManager().getCommands();
                Iterator<? extends CommandNode<?>> it = geyserBrigadier.getChildren().iterator();

                while (it.hasNext()) {
                    CommandNode<?> subnode = it.next();
                    Command command = commands.get(subnode.getName());
                    if (command != null) {
                        if ((command.isBedrockOnly() && isJavaPlayer) || !player.hasPermission(command.permission())) {
                            // Remove this from the node as we don't have permission to use it
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * This early on, there is a rare chance that Geyser has yet to process the connection. We'll try to minimize that
     * chance, though.
     */
    private boolean isProbablyJavaPlayer(Player player) {
        if (GeyserImpl.getInstance().connectionByUuid(player.getUniqueId()) != null) {
            // For sure this is a Bedrock player
            return false;
        }

        if (GeyserImpl.getInstance().getConfig().isUseDirectConnection()) {
            InetSocketAddress address = player.getAddress();
            if (address != null) {
                return address.getPort() != 0;
            }
        }
        return true;
    }
}
