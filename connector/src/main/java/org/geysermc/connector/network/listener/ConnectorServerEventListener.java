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

package org.geysermc.connector.network.listener;

import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.configuration.GeyserConfiguration;

import java.net.InetSocketAddress;

public class ConnectorServerEventListener implements BedrockServerEventHandler {

    private GeyserConnector connector;

    public ConnectorServerEventListener(GeyserConnector connector) {
        this.connector = connector;
    }

    @Override
    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress) {
        return true;
    }

    @Override
    public BedrockPong onQuery(InetSocketAddress inetSocketAddress) {
        GeyserConfiguration config = connector.getConfig();
        BedrockPong pong = new BedrockPong();
        pong.setEdition("MCPE");
        pong.setMotd(config.getBedrock().getMotd1());
        pong.setSubMotd(config.getBedrock().getMotd2());
        pong.setPlayerCount(0);
        pong.setMaximumPlayerCount(config.getMaxPlayers());
        pong.setGameType("Default");
        pong.setNintendoLimited(false);
        pong.setProtocolVersion(GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion());

        return pong;
    }

    @Override
    public void onSessionCreation(BedrockServerSession bedrockServerSession) {

    }
}