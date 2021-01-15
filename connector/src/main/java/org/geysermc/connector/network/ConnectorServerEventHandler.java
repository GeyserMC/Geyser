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

package org.geysermc.connector.network;

import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.ping.GeyserPingInfo;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.LanguageUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ConnectorServerEventHandler implements BedrockServerEventHandler {

    private final GeyserConnector connector;

    public ConnectorServerEventHandler(GeyserConnector connector) {
        this.connector = connector;
    }

    @Override
    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress) {
        connector.getLogger().info(LanguageUtils.getLocaleStringLog("geyser.network.attempt_connect", inetSocketAddress));
        return true;
    }

    @Override
    public BedrockPong onQuery(InetSocketAddress inetSocketAddress) {
        connector.getLogger().debug(LanguageUtils.getLocaleStringLog("geyser.network.pinged", inetSocketAddress));

        GeyserConfiguration config = connector.getConfig();

        GeyserPingInfo pingInfo = null;
        if (config.isPassthroughMotd() || config.isPassthroughPlayerCounts()) {
            IGeyserPingPassthrough pingPassthrough = connector.getBootstrap().getGeyserPingPassthrough();
            pingInfo = pingPassthrough.getPingInformation(inetSocketAddress);
        }

        BedrockPong pong = new BedrockPong();
        pong.setEdition("MCPE");
        pong.setGameType("Default");
        pong.setNintendoLimited(false);
        pong.setProtocolVersion(BedrockProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion());
        pong.setVersion(null); // Server tries to connect either way and it looks better
        pong.setIpv4Port(config.getBedrock().getPort());

        if (config.isPassthroughMotd() && pingInfo != null && pingInfo.getDescription() != null) {
            String[] motd = MessageTranslator.convertMessageLenient(pingInfo.getDescription()).split("\n");
            String mainMotd = motd[0]; // First line of the motd.
            String subMotd = (motd.length != 1) ? motd[1] : ""; // Second line of the motd if present, otherwise blank.

            pong.setMotd(mainMotd.trim());
            pong.setSubMotd(subMotd.trim()); // Trimmed to shift it to the left, prevents the universe from collapsing on us just because we went 2 characters over the text box's limit.
        } else {
            pong.setMotd(config.getBedrock().getMotd1());
            pong.setSubMotd(config.getBedrock().getMotd2());
        }

        if (config.isPassthroughPlayerCounts() && pingInfo != null) {
            pong.setPlayerCount(pingInfo.getPlayers().getOnline());
            pong.setMaximumPlayerCount(pingInfo.getPlayers().getMax());
        } else {
            pong.setPlayerCount(connector.getPlayers().size());
            pong.setMaximumPlayerCount(config.getMaxPlayers());
        }

        // The ping will not appear if the MOTD + sub-MOTD is of a certain length.
        // We don't know why, though
        byte[] motdArray = pong.getMotd().getBytes(StandardCharsets.UTF_8);
        if (motdArray.length + pong.getSubMotd().getBytes(StandardCharsets.UTF_8).length > 338) {
            // Remove the sub-MOTD first since that only appears locally
            pong.setSubMotd("");
            if (motdArray.length > 338) {
                // If the top MOTD is still too long, we chop it down
                byte[] newMotdArray = new byte[339];
                System.arraycopy(motdArray, 0, newMotdArray, 0, newMotdArray.length);
                pong.setMotd(new String(newMotdArray, StandardCharsets.UTF_8));
            }
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
        // Set the packet codec to default just in case we need to send disconnect packets.
        bedrockServerSession.setPacketCodec(BedrockProtocol.DEFAULT_BEDROCK_CODEC);
    }

    @Override
    public void onUnhandledDatagram(ChannelHandlerContext ctx, DatagramPacket packet) {
        new QueryPacketHandler(connector, packet.sender(), packet.content());
    }
}