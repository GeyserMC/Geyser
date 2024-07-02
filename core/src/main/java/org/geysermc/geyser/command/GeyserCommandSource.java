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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

/**
 * Implemented on top of any class that can send a command.
 * For example, it wraps around Spigot's CommandSender class.
 */
public interface GeyserCommandSource extends CommandSource {

    /**
     * {@inheritDoc}
     */
    default String locale() {
        return GeyserLocale.getDefaultLocale();
    }

    default void sendMessage(Component message) {
        sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
    }

    default void sendLocaleString(String key, Object... values) {
        sendMessage(GeyserLocale.getPlayerLocaleString(key, locale(), values));
    }

    default void sendLocaleString(String key) {
        sendMessage(GeyserLocale.getPlayerLocaleString(key, locale()));
    }

    @Override
    default @Nullable GeyserSession connection() {
        UUID uuid = playerUuid();
        if (uuid == null) {
            return null;
        }
        return GeyserImpl.getInstance().connectionByUuid(uuid);
    }

    /**
     * @return the underlying platform handle that this source represents.
     *         If such handle doesn't exist, this itself is returned.
     */
    default Object handle() {
        return this;
    }
}
