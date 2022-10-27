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

package org.geysermc.geyser.platform.fabric.command;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.text.ChatColor;

import javax.annotation.Nonnull;

public class FabricCommandSender implements GeyserCommandSource {

    private final CommandSourceStack source;

    public FabricCommandSender(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    public String name() {
        return source.getTextName();
    }

    @Override
    public void sendMessage(@Nonnull String message) {
        if (source.getEntity() instanceof ServerPlayer) {
            ((ServerPlayer) source.getEntity()).displayClientMessage(Component.literal(message), false);
        } else {
            GeyserImpl.getInstance().getLogger().info(ChatColor.toANSI(message + ChatColor.RESET));
        }
    }

    @Override
    public void sendMessage(net.kyori.adventure.text.Component message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            String decoded = GsonComponentSerializer.gson().serialize(message);
            player.displayClientMessage(Component.Serializer.fromJson(decoded), false);
            return;
        }
        GeyserCommandSource.super.sendMessage(message);
    }

    @Override
    public boolean isConsole() {
        return !(source.getEntity() instanceof ServerPlayer);
    }

    @Override
    public boolean hasPermission(String permission) {
        return Permissions.check(source, permission);
    }
}
