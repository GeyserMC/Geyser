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

package org.geysermc.geyser.platform.bungeecord.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Locale;

public class BungeeCommandSource implements GeyserCommandSource {

    private final net.md_5.bungee.api.CommandSender handle;

    public BungeeCommandSource(net.md_5.bungee.api.CommandSender handle) {
        this.handle = handle;
        // Ensure even Java players' languages are loaded
        GeyserLocale.loadGeyserLocale(this.locale());
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public void sendMessage(@NonNull String message) {
        handle.sendMessage(TextComponent.fromLegacyText(message));
    }

    private static final int PROTOCOL_HEX_COLOR = 713; // Added 20w17a

    @Override
    public void sendMessage(Component message) {
        if (handle instanceof ProxiedPlayer player && player.getPendingConnection().getVersion() >= PROTOCOL_HEX_COLOR) {
            // Include hex colors
            handle.sendMessage(BungeeComponentSerializer.get().serialize(message));
            return;
        }
        handle.sendMessage(BungeeComponentSerializer.legacy().serialize(message));
    }

    @Override
    public boolean isConsole() {
        return !(handle instanceof ProxiedPlayer);
    }

    @Override
    public String locale() {
        if (handle instanceof ProxiedPlayer player) {
            Locale locale = player.getLocale();
            if (locale != null) {
                // Locale can be null early on in the conneciton
                return GeyserLocale.formatLocale(locale.getLanguage() + "_" + locale.getCountry());
            }
        }
        return GeyserLocale.getDefaultLocale();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }
}
