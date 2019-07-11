/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.remote.RemoteJavaServer;
import org.geysermc.connector.network.translators.Registry;

import java.util.UUID;

public class GeyserSession implements PlayerSession {

    private GeyserConnector connector;

    @Getter
    private RemoteJavaServer remoteServer;

    @Getter
    private BedrockServerSession upstream;

    @Getter
    private Client downstream;

    @Getter
    private AuthenticationData authenticationData;

    private boolean closed;

    public GeyserSession(GeyserConnector connector, BedrockServerSession bedrockServerSession) {
        this.connector = connector;
        this.upstream = bedrockServerSession;
    }

    public void connect(RemoteJavaServer remoteServer) {
        MinecraftProtocol protocol = new MinecraftProtocol(authenticationData.getName());
        downstream = new Client(remoteServer.getAddress(), remoteServer.getPort(), protocol, new TcpSessionFactory());
        downstream.getSession().addListener(new SessionAdapter() {

            @Override
            public void connected(ConnectedEvent event) {
                connector.getLogger().info(authenticationData.getName() + " has connected to remote java server on address " + remoteServer.getAddress());
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                connector.getLogger().info(authenticationData.getName() + " has disconnected from remote java server on address " + remoteServer.getAddress());
                upstream.disconnect(event.getReason());
            }

            @Override
            public void packetReceived(PacketReceivedEvent event) {
                Registry.JAVA.translate(event.getPacket().getClass(), event.getPacket());
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

    public void setAuthenticationData(String name, UUID uuid, String xboxUUID) {
        authenticationData = new AuthenticationData(name, uuid, xboxUUID);
    }

    @Getter
    @AllArgsConstructor
    public class AuthenticationData {

        private String name;
        private UUID uuid;
        private String xboxUUID;
    }
}