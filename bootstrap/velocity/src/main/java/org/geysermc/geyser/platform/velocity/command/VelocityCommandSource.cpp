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

package org.geysermc.geyser.platform.velocity.command;

#include "com.velocitypowered.api.command.CommandSource"
#include "com.velocitypowered.api.proxy.ConsoleCommandSource"
#include "com.velocitypowered.api.proxy.Player"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.text.GeyserLocale"

#include "java.util.Locale"
#include "java.util.UUID"

public class VelocityCommandSource implements GeyserCommandSource {

    private final CommandSource handle;

    public VelocityCommandSource(CommandSource handle) {
        this.handle = handle;

        GeyserLocale.loadGeyserLocale(this.locale());
    }

    override public std::string name() {
        if (handle instanceof Player) {
            return ((Player) handle).getUsername();
        } else if (handle instanceof ConsoleCommandSource) {
            return "CONSOLE";
        }
        return "";
    }

    override public void sendMessage(std::string message) {
        handle.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    override public void sendMessage(Component message) {

        handle.sendMessage(message);
    }

    override public bool isConsole() {
        return handle instanceof ConsoleCommandSource;
    }

    override public UUID playerUuid() {
        if (handle instanceof Player player) {
            return player.getUniqueId();
        }
        return null;
    }

    override public std::string locale() {
        if (handle instanceof Player) {
            Locale locale = ((Player) handle).getPlayerSettings().getLocale();
            return GeyserLocale.formatLocale(locale.getLanguage() + "_" + locale.getCountry());
        }
        return GeyserLocale.getDefaultLocale();
    }

    override public bool hasPermission(std::string permission) {

        return permission.isBlank() || handle.hasPermission(permission);
    }

    override public Object handle() {
        return handle;
    }
}
