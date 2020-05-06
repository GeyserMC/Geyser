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

package org.geysermc.platform.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.geysermc.common.ping.GeyserPingInfo;
import org.geysermc.common.ping.IGeyserPingPassthrough;

import java.net.InetAddress;

public class GeyserBukkitPingPassthrough implements IGeyserPingPassthrough {

    @Override
    public GeyserPingInfo getPingInformation() {
        try {
            // Currently makes Essentials complain about not being able to hide vanished players with an UnsupportedOperationException
            ServerListPingEvent event = new ServerListPingEvent(
                    InetAddress.getLocalHost(), Bukkit.getMotd(), Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers());
            Bukkit.getPluginManager().callEvent(event);
            GeyserPingInfo geyserPingInfo = new GeyserPingInfo(event.getMotd(), event.getNumPlayers(), event.getMaxPlayers());
            Bukkit.getOnlinePlayers().forEach(player -> {
                geyserPingInfo.addPlayer(player.getName());
            });
            return geyserPingInfo;
        } catch (Exception e) {
            return new GeyserPingInfo(null, 0, 0);
        }
    }
}
