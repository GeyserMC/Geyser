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

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.command.CommandSender;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.platform.fabric.GeyserFabricMod;

public class FabricCommandSender implements CommandSender {

    private final ServerCommandSource source;

    public FabricCommandSender(ServerCommandSource source) {
        this.source = source;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public void sendMessage(String message) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) source.getEntity()).sendMessage(new LiteralText(message), false);
        } else {
            GeyserConnector.getInstance().getLogger().info(ChatColor.toANSI(message + ChatColor.RESET));
        }
    }

    @Override
    public boolean isConsole() {
        return !(source.getEntity() instanceof ServerPlayerEntity);
    }

    @Override
    public boolean hasPermission(String s) {
        // Mostly copied from fabric's world manager since the method there takes a GeyserSession

        // Workaround for our commands because fabric doesn't have native permissions
        for (GeyserFabricCommandExecutor executor : GeyserFabricMod.getInstance().getCommandExecutors()) {
            if (executor.getCommand().getPermission().equals(s)) {
                return executor.canRun(source);
            }
        }

        return false;
    }
}
