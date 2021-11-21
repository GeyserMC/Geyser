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

package org.geysermc.geyser.platform.bungeecord.command;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.text.GeyserLocale;

public class BungeeCommandSender implements CommandSender {

    private final net.md_5.bungee.api.CommandSender handle;

    public BungeeCommandSender(net.md_5.bungee.api.CommandSender handle) {
        this.handle = handle;
        // Ensure even Java players' languages are loaded
        GeyserLocale.loadGeyserLocale(getLocale());
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(TextComponent.fromLegacyText(message));
    }

    @Override
    public boolean isConsole() {
        return !(handle instanceof ProxiedPlayer);
    }

    @Override
    public String getLocale() {
        if (handle instanceof ProxiedPlayer player) {
            String locale = player.getLocale().getLanguage() + "_" + player.getLocale().getCountry();
            return GeyserLocale.formatLocale(locale);
        }
        return GeyserLocale.getDefaultLocale();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }
}
