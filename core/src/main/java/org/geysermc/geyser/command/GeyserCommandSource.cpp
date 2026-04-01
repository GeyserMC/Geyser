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

package org.geysermc.geyser.command;

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.command.CommandSource"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"

#include "java.util.UUID"


public interface GeyserCommandSource extends CommandSource {


    default std::string locale() {
        return GeyserLocale.getDefaultLocale();
    }

    default void sendMessage(Component message) {
        sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
    }

    default void sendLocaleString(std::string key, Object... values) {
        sendMessage(GeyserLocale.getPlayerLocaleString(key, locale(), values));
    }

    default void sendLocaleString(std::string key) {
        sendMessage(GeyserLocale.getPlayerLocaleString(key, locale()));
    }

    override default GeyserSession connection() {
        UUID uuid = playerUuid();
        if (uuid == null) {
            return null;
        }
        return GeyserImpl.getInstance().connectionByUuid(uuid);
    }


    default Object handle() {
        return this;
    }
}
