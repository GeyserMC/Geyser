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

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListPingEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 * A utility class for checking on the existence of classes, constructors, fields, methods
 */
public final class ReflectedNames {

    static boolean checkPaperPingEvent() {
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            paperServerListPingEventConstructor();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean newSpigotPingConstructorExists() {
        return getConstructor(ServerListPingEvent.class, InetAddress.class, String.class, boolean.class, int.class, int.class) != null;
    }

    // Ugly workaround that's necessary due to relocation of adventure components
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
            // We want to get the constructor with the adventure component motd, but without referencing the
            // component class as that's relocated
            if (constructor.getParameters()[1].getType() != String.class) {
                return (Constructor<PaperServerListPingEvent>) constructor;
            }
        }

        throw new IllegalStateException("Could not find component motd method!");
    }

    /**
     * @return if this class has a constructor with the specified arguments
     */
    @Nullable
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
