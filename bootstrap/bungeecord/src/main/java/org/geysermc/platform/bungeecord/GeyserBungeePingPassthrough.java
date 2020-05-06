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

package org.geysermc.platform.bungeecord;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Listener;
import org.geysermc.common.ping.GeyserPingInfo;
import org.geysermc.common.ping.IGeyserPingPassthrough;

public class GeyserBungeePingPassthrough implements IGeyserPingPassthrough, Listener {

    private final ListenerInfo listener;
    private final ProxyServer proxyServer;

    public GeyserBungeePingPassthrough(ProxyServer proxyServer) {
        this.listener = proxyServer.getConfig().getListeners().iterator().next();;
        this.proxyServer = proxyServer;
    }

    @Override
    public GeyserPingInfo getPingInformation() {
        GeyserPingInfo geyserPingInfo = new GeyserPingInfo(listener.getMotd(), proxyServer.getOnlineCount(), listener.getMaxPlayers());
        proxyServer.getPlayers().forEach(proxiedPlayer -> {
            geyserPingInfo.addPlayer(proxiedPlayer.getName());
        });
        return geyserPingInfo;
    }
}
