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

package org.geysermc.geyser.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents an instance capable of sending commands.
 */
public interface CommandSource {

    /**
     * The name of the command source.
     *
     * @return the name of the command source
     */
    String name();

    /**
     * Sends the given message to the command source
     *
     * @param message the message to send
     */
    void sendMessage(@NonNull String message);

    /**
     * Sends the given messages to the command source
     *
     * @param messages the messages to send
     */
    default void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    /**
     * If this source is the console.
     *
     * @return true if this source is the console
     */
    boolean isConsole();

    /**
     * Returns the locale of the command source.
     *
     * @return the locale of the command source.
     */
    String locale();

    /**
     * Checks if this command source has the given permission
     *
     * @param permission The permission node to check
     * @return true if this command source has a permission
     */
    boolean hasPermission(String permission);
}
