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
#include "com.destroystokyo.paper.network.StatusClient"
#include "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"
#include "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"
#include "org.bukkit.Bukkit"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.ping.GeyserPingInfo"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"

#include "java.lang.reflect.Constructor"
#include "java.lang.reflect.Method"
#include "java.net.InetSocketAddress"


public final class GeyserPaperPingPassthrough implements IGeyserPingPassthrough {
    private static final Constructor<PaperServerListPingEvent> EVENT_CONSTRUCTOR = ReflectedNames.paperServerListPingEventConstructor();

    private static final bool CHAT_PREVIEWS = EVENT_CONSTRUCTOR.getParameters()[2].getType() == bool.class;
    private static final Method MOTD_COMPONENT_GETTER = ReflectedNames.motdGetter();

    private final GeyserSpigotLogger logger;

    public GeyserPaperPingPassthrough(GeyserSpigotLogger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("deprecation")

    override public GeyserPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        try {
            PaperServerListPingEvent event;
            if (CHAT_PREVIEWS) {
                event = EVENT_CONSTRUCTOR.newInstance(new GeyserStatusClient(inetSocketAddress),
                    MOTD_COMPONENT_GETTER.invoke(null), false, Bukkit.getOnlinePlayers().size(),
                    Bukkit.getMaxPlayers(), Bukkit.getVersion(), GameProtocol.getJavaProtocolVersion(), null);
            } else {
                event = EVENT_CONSTRUCTOR.newInstance(new GeyserStatusClient(inetSocketAddress),
                    MOTD_COMPONENT_GETTER.invoke(null), Bukkit.getOnlinePlayers().size(),
                    Bukkit.getMaxPlayers(), Bukkit.getVersion(), GameProtocol.getJavaProtocolVersion(), null);
            }
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {

                return null;
            }

            GeyserPingInfo.Players players;
            if (event.shouldHidePlayers()) {
                players = new GeyserPingInfo.Players(1, 0);
            } else {
                players = new GeyserPingInfo.Players(event.getMaxPlayers(), event.getNumPlayers());
            }

            return new GeyserPingInfo(
                GsonComponentSerializer.gson().serialize(LegacyComponentSerializer.legacySection().deserialize(event.getMotd())),
                players
            );
        } catch (Exception | LinkageError e) {
            logger.debug("Error while getting Paper ping passthrough: " + e);
            return null;
        }
    }

    private record GeyserStatusClient(InetSocketAddress address) implements StatusClient {
        override public InetSocketAddress getAddress() {
            return address;
        }

        override public int getProtocolVersion() {
            return GameProtocol.getJavaProtocolVersion();
        }

        override public InetSocketAddress getVirtualHost() {
            return null;
        }
    }
}
