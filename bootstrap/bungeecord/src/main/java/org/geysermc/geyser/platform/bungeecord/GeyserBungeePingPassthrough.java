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

package org.geysermc.geyser.platform.bungeecord;

import lombok.AllArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.geysermc.geyser.ping.GeyserPingInfo;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class GeyserBungeePingPassthrough implements IGeyserPingPassthrough, Listener {

    private final ProxyServer proxyServer;

    @Override
    public GeyserPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        CompletableFuture<ProxyPingEvent> future = new CompletableFuture<>();
        proxyServer.getPluginManager().callEvent(new ProxyPingEvent(new GeyserPendingConnection(inetSocketAddress), getPingInfo(), (event, throwable) -> {
            if (throwable != null) future.completeExceptionally(throwable);
            else future.complete(event);
        }));
        ProxyPingEvent event = future.join();
        ServerPing response = event.getResponse();
        GeyserPingInfo geyserPingInfo = new GeyserPingInfo(
                response.getDescriptionComponent().toLegacyText(),
                new GeyserPingInfo.Players(response.getPlayers().getMax(), response.getPlayers().getOnline()),
                new GeyserPingInfo.Version(response.getVersion().getName(), response.getVersion().getProtocol())
        );
        if (event.getResponse().getPlayers().getSample() != null) {
            Arrays.stream(event.getResponse().getPlayers().getSample()).forEach(proxiedPlayer ->
                    geyserPingInfo.getPlayerList().add(proxiedPlayer.getName()));
        }
        return geyserPingInfo;
    }

    // This is static so pending connection can use it
    private static ListenerInfo getDefaultListener() {
        return ProxyServer.getInstance().getConfig().getListeners().iterator().next();
    }

    private ServerPing getPingInfo() {
        return new ServerPing(
                new ServerPing.Protocol(proxyServer.getName() + " " + proxyServer.getGameVersion(), ProtocolConstants.SUPPORTED_VERSION_IDS.get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1)),
                new ServerPing.Players(getDefaultListener().getMaxPlayers(), proxyServer.getOnlineCount(), null),
                getDefaultListener().getMotd(), proxyServer.getConfig().getFaviconObject()
        );
    }

    private static class GeyserPendingConnection implements PendingConnection {

        private static final UUID FAKE_UUID = UUID.nameUUIDFromBytes("geyser!internal".getBytes());

        private final InetSocketAddress remote;

        public GeyserPendingConnection(InetSocketAddress remote) {
            this.remote = remote;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getVersion() {
            return ProtocolConstants.SUPPORTED_VERSION_IDS.get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1);
        }

        @Override
        public InetSocketAddress getVirtualHost() {
            return null;
        }

        @Override
        public ListenerInfo getListener() {
            return getDefaultListener();
        }

        @Override
        public String getUUID() {
            return FAKE_UUID.toString();
        }

        @Override
        public UUID getUniqueId() {
            return FAKE_UUID;
        }

        @Override
        public void setUniqueId(UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOnlineMode() {
            return true;
        }

        @Override
        public void setOnlineMode(boolean b) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLegacy() {
            return false;
        }

        @Override
        public InetSocketAddress getAddress() {
            return remote;
        }

        @Override
        public SocketAddress getSocketAddress() {
            return getAddress();
        }

        @Override
        public void disconnect(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void disconnect(BaseComponent... baseComponents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void disconnect(BaseComponent baseComponent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public Unsafe unsafe() {
            throw new UnsupportedOperationException();
        }
    }

}
