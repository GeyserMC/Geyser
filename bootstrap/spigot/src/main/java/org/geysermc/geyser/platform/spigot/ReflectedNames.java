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
import com.destroystokyo.paper.network.StatusClient;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.net.InetAddress;

/**
 * A utility class for checking on the existence of classes, constructors, fields, methods
 */
public final class ReflectedNames {

    static boolean checkPaperPingEvent() {
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean newSpigotPingConstructorExists() {
        return getConstructor(ServerListPingEvent.class, InetAddress.class, String.class, boolean.class, int.class, int.class) != null;
    }

    static @Nullable Constructor<PaperServerListPingEvent> getOldPaperPingConstructor() {
        if (getConstructor(PaperServerListPingEvent.class, StatusClient.class, String.class, int.class,
                int.class, String.class, int.class, CachedServerIcon.class) != null) {
            // @NotNull StatusClient client, @NotNull String motd, int numPlayers, int maxPlayers,
            //            @NotNull String version, int protocolVersion, @Nullable CachedServerIcon favicon
            // New constructor is present
            return null;
        }
        // @NotNull StatusClient client, @NotNull String motd, boolean shouldSendChatPreviews, int numPlayers, int maxPlayers,
        //            @NotNull String version, int protocolVersion, @Nullable CachedServerIcon favicon
        return getConstructor(PaperServerListPingEvent.class, StatusClient.class, String.class, boolean.class, int.class, int.class,
                String.class, int.class, CachedServerIcon.class);
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
