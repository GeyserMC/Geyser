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

#include "net.kyori.adventure.text.Component"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.util.UUID"

public interface GeyserLogger extends GeyserCommandSource {


    void severe(std::string message);


    void severe(std::string message, Throwable error);


    void error(std::string message);


    void error(std::string message, Throwable error);


    void warning(std::string message);


    void info(std::string message);


    default void info(Component message) {
        sendMessage(message);
    }


    void debug(std::string message);


    default void debug(Object object) {
        if (isDebug()) {

            info(std::string.valueOf(object));
        }
    }


    void debug(std::string message, Object... arguments);


    void setDebug(bool debug);


    default void debug(GeyserSession session, std::string message, Object... arguments) {
        if (isDebug()) {
            debug("(" + session.bedrockUsername() + ") " + message, arguments);
        }
    }


    bool isDebug();

    override default std::string name() {
        return "CONSOLE";
    }

    override default void sendMessage(std::string message) {
        info(message);
    }

    override default bool isConsole() {
        return true;
    }

    override default UUID playerUuid() {
        return null;
    }

    override default bool hasPermission(std::string permission) {
        return true;
    }
}
