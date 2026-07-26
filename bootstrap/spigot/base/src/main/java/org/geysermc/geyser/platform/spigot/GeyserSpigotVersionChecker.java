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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.text.GeyserLocale;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public final class GeyserSpigotVersionChecker {
    private static final String VIAVERSION_DOWNLOAD_URL = "https://ci.viaversion.com/job/ViaVersion/";

    /**
     * @return the server version before ViaVersion finishes initializing
     */
    public static ProtocolVersion serverProtocolVersion(String minecraftVersion) {
        return ProtocolVersion.getClosest(minecraftVersion);
    }

    /**
     * This method should not run unless ViaVersion is installed on the server.
     *
     * @return true if there is any block mappings difference between the server and client.
     */
    public static boolean isViaVersionNeeded(String minecraftVersion) {
        ProtocolVersion serverVersion = serverProtocolVersion(minecraftVersion);
        List<ProtocolPathEntry> protocolList = Via.getManager().getProtocolManager().getProtocolPath(GameProtocol.getJavaProtocolVersion(),
            serverVersion.getVersion());
        if (protocolList == null) {
            // No translation needed!
            return false;
        }
        for (int i = protocolList.size() - 1; i >= 0; i--) {
            MappingData mappingData = protocolList.get(i).protocol().getMappingData();
            if (mappingData != null) {
                return true;
            }
        }
        // All mapping data is null, which means client and server block states are the same
        return false;
    }

    @SuppressWarnings("deprecation")
    public static void checkForSupportedProtocol(GeyserLogger logger, boolean viaversion) {
        if (viaversion) {
            checkViaVersionSupportedVersions(logger);
            return;
        }

        try {
            // This method is only present on later versions of Paper
            UnsafeValues.class.getMethod("getProtocolVersion");
            if (Bukkit.getUnsafe().getProtocolVersion() != GameProtocol.getJavaProtocolVersion()) {
                sendOutdatedMessage(logger);
            }
            return;
        } catch (NoSuchMethodException ignored) {
        }

        // Otherwise, we can just try to find the SharedConstants class
        // It isn't present in all server versions, but if we can't find it, then we're probably not in the latest version
        Class<?> sharedConstants;
        try {
            sharedConstants = Class.forName("net.minecraft.SharedConstants");
        } catch (ClassNotFoundException e) {
            // We're using pre-1.17
            String prefix = Bukkit.getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit", "net.minecraft.server");
            try {
                sharedConstants = Class.forName(prefix + ".SharedConstants");
            } catch (ClassNotFoundException e2) {
                sendOutdatedMessage(logger);
                return;
            }
        }
        for (Method method : sharedConstants.getMethods()) {
            if (method.getReturnType() == int.class && Modifier.isStatic(method.getModifiers())) {
                int protocolVersion;
                try {
                    protocolVersion = (int) method.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.warning("Could not determine server version! This is safe to ignore, but please report to the developers: " + e.getMessage());
                    if (logger.isDebug()) {
                        e.printStackTrace();
                    }
                    return;
                }
                if (protocolVersion != GameProtocol.getJavaProtocolVersion()) {
                    sendOutdatedMessage(logger);
                }
                return;
            }
        }
        sendOutdatedMessage(logger);
    }

    private static void checkViaVersionSupportedVersions(GeyserLogger logger) {
        // Run after ViaVersion has obtained the server protocol version
        Via.getPlatform().runSync(() -> {
            if (Via.getAPI().getSupportedVersions().contains(GameProtocol.getJavaProtocolVersion())) {
                // Via supports this protocol version; we will be able to connect.
                return;
            }
            if (Via.getAPI().getFullSupportedVersions().contains(GameProtocol.getJavaProtocolVersion())) {
                // ViaVersion supports our protocol, but the user has blocked them from connecting.
                logger.warning(GeyserLocale.getLocaleStringLog("geyser.bootstrap.viaversion.blocked", GameProtocol.getAllSupportedJavaVersions()));
                return;
            }
            // Else, presumably, ViaVersion is not updated.
            sendOutdatedViaVersionMessage(logger);
        });
    }

    public static void sendOutdatedViaVersionMessage(GeyserLogger logger) {
        logger.warning(GeyserLocale.getLocaleStringLog("geyser.bootstrap.viaversion.too_old",
                VIAVERSION_DOWNLOAD_URL));
    }

    private static void sendOutdatedMessage(GeyserLogger logger) {
        logger.warning(GeyserLocale.getLocaleStringLog("geyser.bootstrap.no_supported_protocol", GameProtocol.getAllSupportedJavaVersions(), VIAVERSION_DOWNLOAD_URL));
    }

    private GeyserSpigotVersionChecker() {
    }
}
