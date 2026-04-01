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

package org.geysermc.geyser.platform.spigot;

#include "com.destroystokyo.paper.event.server.PaperServerListPingEvent"
#include "org.bukkit.Bukkit"
#include "org.bukkit.event.server.ServerListPingEvent"
#include "org.checkerframework.checker.nullness.qual.Nullable"

#include "java.lang.reflect.Constructor"
#include "java.lang.reflect.Method"
#include "java.net.InetAddress"


public final class ReflectedNames {

    static bool checkPaperPingEvent() {
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            paperServerListPingEventConstructor();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static bool newSpigotPingConstructorExists() {
        return getConstructor(ServerListPingEvent.class, InetAddress.class, std::string.class, bool.class, int.class, int.class) != null;
    }


    static Method motdGetter() {
        try {
            return Bukkit.class.getMethod("motd");
        } catch (Throwable e) {
            throw new RuntimeException("Could not find component motd method! Please report this issue.", e);
        }
    }

    @SuppressWarnings("unchecked")
    static Constructor<PaperServerListPingEvent> paperServerListPingEventConstructor() {
        var constructors = PaperServerListPingEvent.class.getConstructors();
        for (var constructor : constructors) {


            if (constructor.getParameters()[1].getType() != std::string.class) {
                return (Constructor<PaperServerListPingEvent>) constructor;
            }
        }

        throw new IllegalStateException("Could not find component motd method!");
    }



    private static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... args) {
        try {
            return clazz.getConstructor(args);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private ReflectedNames() {
    }
}
