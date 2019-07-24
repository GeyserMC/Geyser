/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.session;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.PlayerSession;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import lombok.Getter;
import org.geysermc.api.Player;
import org.geysermc.api.RemoteServer;
import org.geysermc.api.session.AuthData;
import org.geysermc.api.window.FormWindow;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.cache.WindowCache;
import org.geysermc.connector.network.translators.Registry;

public class GeyserSession implements PlayerSession, Player {

    private GeyserConnector connector;

    @Getter
    private RemoteServer remoteServer;

    @Getter
    private BedrockServerSession upstream;

    @Getter
    private Client downstream;

    private final GeyserSession THIS = this;

    @Getter
    private AuthData authenticationData;

    @Getter
    private WindowCache windowCache;

    private boolean closed;

    public GeyserSession(GeyserConnector connector, BedrockServerSession bedrockServerSession) {
        this.connector = connector;
        this.upstream = bedrockServerSession;

        this.windowCache = new WindowCache(this);
    }

    public void connect(RemoteServer remoteServer) {
        MinecraftProtocol protocol = new MinecraftProtocol(authenticationData.getName());
        downstream = new Client(remoteServer.getAddress(), remoteServer.getPort(), protocol, new TcpSessionFactory());
        downstream.getSession().addListener(new SessionAdapter() {

            @Override
            public void connected(ConnectedEvent event) {
                connector.getLogger().info(authenticationData.getName() + " has connected to remote java server on address " + remoteServer.getAddress());
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                connector.getLogger().info(authenticationData.getName() + " has disconnected from remote java server on address " + remoteServer.getAddress() + " because of " + event.getReason());
                upstream.disconnect(event.getReason());
            }

            @Override
            public void packetReceived(PacketReceivedEvent event) {
                Registry.JAVA.translate(event.getPacket().getClass(), event.getPacket(), THIS);
            }
        });

        downstream.getSession().connect();
        this.remoteServer = remoteServer;
    }

    public void disconnect(String reason) {
        if (!closed) {
            downstream.getSession().disconnect(reason);
            upstream.disconnect(reason);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        disconnect("Server closed.");
    }

    @Override
    public void onDisconnect(DisconnectReason disconnectReason) {
        downstream.getSession().disconnect("Disconnected from server. Reason: " + disconnectReason);
    }

    @Override
    public void onDisconnect(String reason) {
        downstream.getSession().disconnect("Disconnected from server. Reason: " + reason);
    }

    public void setAuthenticationData(AuthData authData) {
        authenticationData = authData;
    }

    @Override
    public String getName() {
        return authenticationData.getName();
    }

    @Override
    public void sendMessage(String message) {
        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid("");
        textPacket.setType(TextPacket.Type.CHAT);
        textPacket.setNeedsTranslation(false);
        textPacket.setMessage(message);

        upstream.sendPacket(textPacket);
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    public void sendForm(FormWindow window, int id) {
        windowCache.showWindow(window, id);
    }

    public void sendForm(FormWindow window) {
        windowCache.showWindow(window);
    }
}