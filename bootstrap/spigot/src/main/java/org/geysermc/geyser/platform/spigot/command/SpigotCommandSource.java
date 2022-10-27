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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.platform.spigot.PaperAdventure;
import org.geysermc.geyser.text.GeyserLocale;

public class SpigotCommandSource implements GeyserCommandSource {
    private final org.bukkit.command.CommandSender handle;

    public SpigotCommandSource(org.bukkit.command.CommandSender handle) {
        this.handle = handle;
        // Ensure even Java players' languages are loaded
        GeyserLocale.loadGeyserLocale(locale());
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
        if (PaperAdventure.canSendMessageUsingComponent()) {
            PaperAdventure.sendMessage(handle, message);
            return;
        }

        // CommandSender#sendMessage(BaseComponent[]) is Paper-only
        handle.spigot().sendMessage(BungeeComponentSerializer.get().serialize(message));
    }

    @Override
    public boolean isConsole() {
        return handle instanceof ConsoleCommandSender;
    }

    @Override
    public String locale() {
        if (this.handle instanceof Player player) {
            return player.getLocale();
        }

        return GeyserLocale.getDefaultLocale();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }
}
