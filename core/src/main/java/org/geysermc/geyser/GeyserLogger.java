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

package org.geysermc.geyser;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public interface GeyserLogger extends GeyserCommandSource {

    
    void severe(String message);

    
    void severe(String message, Throwable error);

    
    void error(String message);

    
    void error(String message, Throwable error);

    
    void warning(String message);

    
    void info(String message);

    
    default void info(Component message) {
        sendMessage(message);
    }

    
    void debug(String message);

    
    default void debug(@Nullable Object object) {
        if (isDebug()) {
            
            info(String.valueOf(object));
        }
    }

    
    void debug(String message, Object... arguments);

    
    void setDebug(boolean debug);

    
    default void debug(GeyserSession session, String message, Object... arguments) {
        if (isDebug()) {
            debug("(" + session.bedrockUsername() + ") " + message, arguments);
        }
    }

    
    boolean isDebug();

    @Override
    default String name() {
        return "CONSOLE";
    }

    @Override
    default void sendMessage(@NonNull String message) {
        info(message);
    }

    @Override
    default boolean isConsole() {
        return true;
    }

    @Override
    default @Nullable UUID playerUuid() {
        return null;
    }

    @Override
    default boolean hasPermission(String permission) {
        return true;
    }
}
