/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network;

import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;

import org.geysermc.connector.GeyserConfiguration;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.MessageUtils;

import java.net.InetSocketAddress;

public class ConnectorServerEventHandler implements BedrockServerEventHandler {

    private GeyserConnector connector;

    public ConnectorServerEventHandler(GeyserConnector connector) {
        this.connector = connector;
    }

    @Override
    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress) {
        connector.getLogger().info(inetSocketAddress + " tried to connect!");
        return true;
    }

    @Override
    public BedrockPong onQuery(InetSocketAddress inetSocketAddress) {
        connector.getLogger().debug(inetSocketAddress + " has pinged you!");

        GeyserConfiguration config = connector.getConfig();
        ServerStatusInfo serverInfo = connector.getPassthroughThread().getInfo();

        BedrockPong pong = new BedrockPong();
        pong.setEdition("MCPE");
        pong.setGameType("Default");
        pong.setNintendoLimited(false);
        pong.setProtocolVersion(GeyserConnector.BEDROCK_PACKET_CODEC.getProtocolVersion());
        pong.setVersion(GeyserConnector.BEDROCK_PACKET_CODEC.getMinecraftVersion());
        pong.setIpv4Port(config.getBedrock().getPort());
        if (connector.getConfig().isPingPassthrough() && serverInfo != null) {
            String[] motd = MessageUtils.getBedrockMessage(serverInfo.getDescription()).split("\n");
            String mainMotd = motd[0]; // First line of the motd.
            String subMotd = (motd.length != 1) ? motd[1] : ""; // Second line of the motd if present, otherwise blank.

            pong.setMotd(mainMotd.trim());
            pong.setSubMotd(subMotd.trim()); // Trimmed to shift it to the left, prevents the universe from collapsing on us just because we went 2 characters over the text box's limit.
            pong.setPlayerCount(serverInfo.getPlayerInfo().getOnlinePlayers());
            pong.setMaximumPlayerCount(serverInfo.getPlayerInfo().getMaxPlayers());
        } else {
            pong.setPlayerCount(connector.getPlayers().size());
            pong.setMaximumPlayerCount(config.getMaxPlayers());
            pong.setMotd(config.getBedrock().getMotd1());
            pong.setMotd(config.getBedrock().getMotd2());
        }

        //Bedrock will not even attempt a connection if the client thinks the server is full
        //so we have to fake it not being full
        if (pong.getPlayerCount() >= pong.getMaximumPlayerCount()) {
            pong.setMaximumPlayerCount(pong.getPlayerCount() + 1);
        }

        return pong;
    }

    @Override
    public void onSessionCreation(BedrockServerSession bedrockServerSession) {
        bedrockServerSession.setLogging(true);
        bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(connector, new GeyserSession(connector, bedrockServerSession)));
        bedrockServerSession.addDisconnectHandler(disconnectReason -> {
            connector.getLogger().info("Bedrock user with ip: " + bedrockServerSession.getAddress().getAddress() + " has disconnected for reason " + disconnectReason);

            GeyserSession player = connector.getPlayers().get(bedrockServerSession.getAddress());
            if (player != null) {
                player.disconnect(disconnectReason.name());
                connector.removePlayer(player);
            }
        });
        bedrockServerSession.setPacketCodec(GeyserConnector.BEDROCK_PACKET_CODEC);
    }
}