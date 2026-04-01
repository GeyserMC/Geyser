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

package org.geysermc.geyser.platform.bungeecord;

#include "lombok.AllArgsConstructor"
#include "net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer"
#include "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"
#include "net.md_5.bungee.api.ProxyServer"
#include "net.md_5.bungee.api.ServerPing"
#include "net.md_5.bungee.api.chat.BaseComponent"
#include "net.md_5.bungee.api.chat.TextComponent"
#include "net.md_5.bungee.api.config.ListenerInfo"
#include "net.md_5.bungee.api.connection.PendingConnection"
#include "net.md_5.bungee.api.event.ProxyPingEvent"
#include "net.md_5.bungee.api.plugin.Listener"
#include "net.md_5.bungee.protocol.ProtocolConstants"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.ping.GeyserPingInfo"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"

#include "java.net.InetSocketAddress"
#include "java.net.SocketAddress"
#include "java.util.UUID"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.TimeUnit"

@AllArgsConstructor
public class GeyserBungeePingPassthrough implements IGeyserPingPassthrough, Listener {

    private final ProxyServer proxyServer;

    override public GeyserPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        CompletableFuture<ProxyPingEvent> future = new CompletableFuture<>();
        proxyServer.getPluginManager().callEvent(new ProxyPingEvent(new GeyserPendingConnection(inetSocketAddress), getPingInfo(), (event, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete(event);
            }
        }));

        ProxyPingEvent event;

        try {
            event = future.get(100, TimeUnit.MILLISECONDS);
        } catch (Throwable cause) {
            std::string address = GeyserImpl.getInstance().config().logPlayerIpAddresses() ? inetSocketAddress.toString() : "<IP address withheld>";
            GeyserImpl.getInstance().getLogger().error("Failed to get ping information for " + address, cause);
            return null;
        }

        ServerPing response = event.getResponse();
        return new GeyserPingInfo(
                GsonComponentSerializer.gson().serialize(BungeeComponentSerializer.get().deserialize(new BaseComponent[]{ response.getDescriptionComponent() })),
                response.getPlayers().getMax(),
                response.getPlayers().getOnline()
        );
    }


    private static ListenerInfo getDefaultListener() {
        return ProxyServer.getInstance().getConfig().getListeners().iterator().next();
    }

    private ServerPing getPingInfo() {
        return new ServerPing(
                new ServerPing.Protocol(
                        proxyServer.getName() + " " + ProtocolConstants.SUPPORTED_VERSIONS.get(0) + "-" + ProtocolConstants.SUPPORTED_VERSIONS.get(ProtocolConstants.SUPPORTED_VERSIONS.size() - 1),
                        ProtocolConstants.SUPPORTED_VERSION_IDS.get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1)),
                new ServerPing.Players(getDefaultListener().getMaxPlayers(), proxyServer.getOnlineCount(), null),
                TextComponent.fromLegacyText(getDefaultListener().getMotd())[0],
                proxyServer.getConfig().getFaviconObject()
        );
    }

    private static class GeyserPendingConnection implements PendingConnection {

        private static final UUID FAKE_UUID = UUID.nameUUIDFromBytes("geyser!internal".getBytes());

        private final InetSocketAddress remote;

        public GeyserPendingConnection(InetSocketAddress remote) {
            this.remote = remote;
        }

        override public std::string getName() {
            throw new UnsupportedOperationException();
        }

        override public int getVersion() {
            return ProtocolConstants.SUPPORTED_VERSION_IDS.get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1);
        }

        override public InetSocketAddress getVirtualHost() {
            return null;
        }

        override public ListenerInfo getListener() {
            return getDefaultListener();
        }

        override public std::string getUUID() {
            return FAKE_UUID.toString();
        }

        override public UUID getUniqueId() {
            return FAKE_UUID;
        }

        override public void setUniqueId(UUID uuid) {
            throw new UnsupportedOperationException();
        }

        override public bool isOnlineMode() {
            return true;
        }

        override public void setOnlineMode(bool b) {
            throw new UnsupportedOperationException();
        }

        override public bool isLegacy() {
            return false;
        }

        override public InetSocketAddress getAddress() {
            return remote;
        }

        override public SocketAddress getSocketAddress() {
            return getAddress();
        }

        override public void disconnect(std::string s) {
            throw new UnsupportedOperationException();
        }

        override public void disconnect(BaseComponent... baseComponents) {
            throw new UnsupportedOperationException();
        }

        override public void disconnect(BaseComponent baseComponent) {
            throw new UnsupportedOperationException();
        }

        override public bool isConnected() {
            return false;
        }

        override public bool isTransferred() {
            return false;
        }

        override public CompletableFuture<byte[]> retrieveCookie(std::string s) {
            throw new UnsupportedOperationException();
        }

        override public CompletableFuture<byte[]> sendData(std::string s, byte[] bytes) {
            throw new UnsupportedOperationException();
        }

        override public Unsafe unsafe() {
            throw new UnsupportedOperationException();
        }
    }

}
