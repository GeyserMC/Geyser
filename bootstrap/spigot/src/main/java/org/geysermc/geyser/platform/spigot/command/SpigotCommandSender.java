/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.spigot.command;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandSender;
import org.geysermc.geyser.text.GeyserLocale;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SpigotCommandSender implements CommandSender {

    /**
     * Whether to use {@code Player.getLocale()} or {@code Player.spigot().getLocale()}, depending on version.
     * 1.12 or greater should not use the legacy method.
     */
    private static boolean USE_LEGACY_METHOD = false;
    private static Method LOCALE_METHOD;

    private final org.bukkit.command.CommandSender handle;
    private final String locale;

    public SpigotCommandSender(org.bukkit.command.CommandSender handle) {
        this.handle = handle;
        this.locale = getSpigotLocale();
        // Ensure even Java players' languages are loaded
        GeyserLocale.loadGeyserLocale(locale);
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(message);
    }

    @Override
    public boolean isConsole() {
        return handle instanceof ConsoleCommandSender;
    }

    @Override
    public String getLocale() {
        return locale;
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }

    /**
     * Set if we are on pre-1.12, and therefore {@code player.getLocale()} doesn't exist and we have to get
     * {@code player.spigot().getLocale()}.
     *
     * @param useLegacyMethod if we are running pre-1.12 and therefore need to use reflection to get the player locale
     */
    public static void setUseLegacyLocaleMethod(boolean useLegacyMethod) {
        USE_LEGACY_METHOD = useLegacyMethod;
        if (USE_LEGACY_METHOD) {
            try {
                //noinspection JavaReflectionMemberAccess - of course it doesn't exist; that's why we're doing it
                LOCALE_METHOD = Player.Spigot.class.getMethod("getLocale");
            } catch (NoSuchMethodException e) {
                GeyserImpl.getInstance().getLogger().debug("Player.Spigot.getLocale() doesn't exist? Not a big deal but if you're seeing this please report it to the developers!");
            }
        }
    }

    /**
     * So we only have to do nasty reflection stuff once per command
     *
     * @return the locale of the Spigot player
     */
    private String getSpigotLocale() {
        if (handle instanceof Player player) {
            if (USE_LEGACY_METHOD) {
                try {
                    // sigh
                    // This was the only option on older Spigot instances and now it's gone
                    return (String) LOCALE_METHOD.invoke(player.spigot());
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                }
            } else {
                return player.getLocale();
            }
        }
        return GeyserLocale.getDefaultLocale();
    }
}
