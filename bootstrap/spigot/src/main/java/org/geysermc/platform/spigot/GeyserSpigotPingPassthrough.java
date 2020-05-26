/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.spigot;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.geysermc.common.ping.GeyserPingInfo;
import org.geysermc.connector.ping.IGeyserPingPassthrough;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;

@AllArgsConstructor
public class GeyserSpigotPingPassthrough implements IGeyserPingPassthrough {

    private final GeyserSpigotLogger logger;

    @Override
    public GeyserPingInfo getPingInformation() {
        try {
            ServerListPingEvent event = new GeyserPingEvent(InetAddress.getLocalHost(), Bukkit.getMotd(), Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers());
            Bukkit.getPluginManager().callEvent(event);
            GeyserPingInfo geyserPingInfo = new GeyserPingInfo(event.getMotd(), event.getNumPlayers(), event.getMaxPlayers());
            Bukkit.getOnlinePlayers().forEach(player -> {
                geyserPingInfo.addPlayer(player.getName());
            });
            return geyserPingInfo;
        } catch (Exception e) {
            logger.debug("Error while getting Bukkit ping passthrough: " + e.toString());
            return new GeyserPingInfo(null, 0, 0);
        }
    }

    // These methods are unimplemented on spigot api by default so we add stubs so plugins don't complain
    private static class GeyserPingEvent extends ServerListPingEvent {

        public GeyserPingEvent(InetAddress address, String motd, int numPlayers, int maxPlayers) {
            super(address, motd, numPlayers, maxPlayers);
        }

        @Override
        public void setServerIcon(CachedServerIcon icon) throws IllegalArgumentException, UnsupportedOperationException {
        }

        @Override
        public Iterator<Player> iterator() throws UnsupportedOperationException {
            return Collections.EMPTY_LIST.iterator();
        }
    }

}
