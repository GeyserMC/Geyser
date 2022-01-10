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

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import javax.annotation.Nullable;

/**
 * Implemented on top of any class that can send a command.
 * For example, it wraps around Spigot's CommandSender class.
 */
public interface CommandSender {

    String name();

    default void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    void sendMessage(String message);

    /**
     * @return true if the specified sender is from the console.
     */
    boolean isConsole();

    /**
     * Returns the locale of the command sender. Defaults to the default locale at {@link GeyserLocale#getDefaultLocale()}.
     * 
     * @return the locale of the command sender.
     */
    default String getLocale() {
        return GeyserLocale.getDefaultLocale();
    }

    /**
     * Checks if the CommandSender has a permission
     *
     * @param permission The permission node to check
     * @return true if the CommandSender has the requested permission, false if not
     */
    boolean hasPermission(String permission);

    /**
     * Attempt to find the corresponding {@link GeyserSession} of this {@link CommandSender}. Returns null if the Commandsender is console.
     * Will only return a session if {@link GeyserImpl#getInstance()} does not return null,
     * and {@link CommandSender#name()} equals the username of a session.
     *
     * @return The corresponding GeyserSession, if it exists
     */
    @Nullable
    default GeyserSession asGeyserSession() {
        if (isConsole()) {
            return null;
        }

        GeyserImpl geyser = GeyserImpl.getInstance();
        if (geyser == null) {
            return null;
        }

        for (GeyserSession session : geyser.getSessionManager().getSessions().values()) {
            if (name().equals(session.getPlayerEntity().getUsername())) {
                return session;
            }
        }
        return null;
    }
}
