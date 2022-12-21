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

package org.geysermc.geyser.network;

import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.v554.Bedrock_v554;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.ping.GeyserPingInfo;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConnectorServerEventHandler implements BedrockServerEventHandler {
    private static final boolean PRINT_DEBUG_PINGS = Boolean.parseBoolean(System.getProperty("Geyser.PrintPingsInDebugMode", "true"));

    /*
    The following constants are all used to ensure the ping does not reach a length where it is unparsable by the Bedrock client
     */
    private static final int MINECRAFT_VERSION_BYTES_LENGTH = GameProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion().getBytes(StandardCharsets.UTF_8).length;
    private static final int BRAND_BYTES_LENGTH = GeyserImpl.NAME.getBytes(StandardCharsets.UTF_8).length;
    /**
     * The MOTD, sub-MOTD and Minecraft version ({@link #MINECRAFT_VERSION_BYTES_LENGTH}) combined cannot reach this length.
     */
    private static final int MAGIC_RAKNET_LENGTH = 338;

    private final GeyserImpl geyser;
    // There is a constructor that doesn't require inputting threads, but older Netty versions don't have it
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser player thread"));

    public ConnectorServerEventHandler(GeyserImpl geyser) {
        this.geyser = geyser;
    }

    @Override
    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress) {
        List<String> allowedProxyIPs = geyser.getConfig().getBedrock().getProxyProtocolWhitelistedIPs();
        if (geyser.getConfig().getBedrock().isEnableProxyProtocol() && !allowedProxyIPs.isEmpty()) {
            boolean isWhitelistedIP = false;
            for (CIDRMatcher matcher : geyser.getConfig().getBedrock().getWhitelistedIPsMatchers()) {
                if (matcher.matches(inetSocketAddress.getAddress())) {
                    isWhitelistedIP = true;
                    break;
                }
            }

            if (!isWhitelistedIP) {
                return false;
            }
        }

        String ip = geyser.getConfig().isLogPlayerIpAddresses() ? inetSocketAddress.toString() : "<IP address withheld>";
        geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.network.attempt_connect", ip));
        return true;
    }

    @Override
    public BedrockPong onQuery(InetSocketAddress inetSocketAddress) {
        if (geyser.getConfig().isDebugMode() && PRINT_DEBUG_PINGS) {
            String ip = geyser.getConfig().isLogPlayerIpAddresses() ? inetSocketAddress.toString() : "<IP address withheld>";
            geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.network.pinged", ip));
        }

        GeyserConfiguration config = geyser.getConfig();

        GeyserPingInfo pingInfo = null;
        if (config.isPassthroughMotd() || config.isPassthroughPlayerCounts()) {
            IGeyserPingPassthrough pingPassthrough = geyser.getBootstrap().getGeyserPingPassthrough();
            pingInfo = pingPassthrough.getPingInformation(inetSocketAddress);
        }

        BedrockPong pong = new BedrockPong();
        pong.setEdition("MCPE");
        pong.setGameType("Survival"); // Can only be Survival or Creative as of 1.16.210.59
        pong.setNintendoLimited(false);
        pong.setProtocolVersion(GameProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion());
        pong.setVersion(GameProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion()); // Required to not be empty as of 1.16.210.59. Can only contain . and numbers.
        pong.setIpv4Port(config.getBedrock().port());

        if (config.isPassthroughMotd() && pingInfo != null && pingInfo.getDescription() != null) {
            String[] motd = MessageTranslator.convertMessageLenient(pingInfo.getDescription()).split("\n");
            String mainMotd = motd[0]; // First line of the motd.
            String subMotd = (motd.length != 1) ? motd[1] : GeyserImpl.NAME; // Second line of the motd if present, otherwise default.

            pong.setMotd(mainMotd.trim());
            pong.setSubMotd(subMotd.trim()); // Trimmed to shift it to the left, prevents the universe from collapsing on us just because we went 2 characters over the text box's limit.
        } else {
            pong.setMotd(config.getBedrock().primaryMotd());
            pong.setSubMotd(config.getBedrock().secondaryMotd());
        }

        // https://github.com/GeyserMC/Geyser/issues/3388
        pong.setMotd(pong.getMotd().replace(';', ':'));
        pong.setSubMotd(pong.getSubMotd().replace(';', ':'));

        // Fallbacks to prevent errors and allow Bedrock to see the server
        if (pong.getMotd() == null || pong.getMotd().isBlank()) {
            pong.setMotd(GeyserImpl.NAME);
        }
        if (pong.getSubMotd() == null || pong.getSubMotd().isBlank()) {
            // Sub-MOTD cannot be empty as of 1.16.210.59
            pong.setSubMotd(GeyserImpl.NAME);
        }

        // The ping will not appear if the MOTD + sub-MOTD is of a certain length.
        // We don't know why, though
        byte[] motdArray = pong.getMotd().getBytes(StandardCharsets.UTF_8);
        int subMotdLength = pong.getSubMotd().getBytes(StandardCharsets.UTF_8).length;
        if (motdArray.length + subMotdLength > (MAGIC_RAKNET_LENGTH - MINECRAFT_VERSION_BYTES_LENGTH)) {
            // Shorten the sub-MOTD first since that only appears locally
            if (subMotdLength > BRAND_BYTES_LENGTH) {
                pong.setSubMotd(GeyserImpl.NAME);
                subMotdLength = BRAND_BYTES_LENGTH;
            }
            if (motdArray.length > (MAGIC_RAKNET_LENGTH - MINECRAFT_VERSION_BYTES_LENGTH - subMotdLength)) {
                // If the top MOTD is still too long, we chop it down
                byte[] newMotdArray = new byte[MAGIC_RAKNET_LENGTH - MINECRAFT_VERSION_BYTES_LENGTH - subMotdLength];
                System.arraycopy(motdArray, 0, newMotdArray, 0, newMotdArray.length);
                pong.setMotd(new String(newMotdArray, StandardCharsets.UTF_8));
            }
        }

        if (config.isPassthroughPlayerCounts() && pingInfo != null) {
            pong.setPlayerCount(pingInfo.getPlayers().getOnline());
            pong.setMaximumPlayerCount(pingInfo.getPlayers().getMax());
        } else {
            pong.setPlayerCount(geyser.getSessionManager().getSessions().size());
            pong.setMaximumPlayerCount(config.getMaxPlayers());
        }

        //Bedrock will not even attempt a connection if the client thinks the server is full
        //so we have to fake it not being full
        if (pong.getPlayerCount() >= pong.getMaximumPlayerCount()) {
            pong.setMaximumPlayerCount(pong.getPlayerCount() + 1);
        }

        return pong;
    }

    @Override
    public void onSessionCreation(@Nonnull BedrockServerSession bedrockServerSession) {
        try {
            bedrockServerSession.setPacketCodec(Bedrock_v554.V554_CODEC); // Has the RequestNetworkSettingsPacket
            bedrockServerSession.setLogging(true);
            bedrockServerSession.setCompressionLevel(geyser.getConfig().getBedrock().getCompressionLevel());
            bedrockServerSession.setPacketHandler(new UpstreamPacketHandler(geyser, new GeyserSession(geyser, bedrockServerSession, eventLoopGroup.next())));
            // Set the packet codec to default just in case we need to send disconnect packets.
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            geyser.getLogger().error("Error occurred while initializing player!", e);
            bedrockServerSession.disconnect(e.getMessage());
        }
    }

    @Override
    public void onUnhandledDatagram(@Nonnull ChannelHandlerContext ctx, @Nonnull DatagramPacket packet) {
        try {
            ByteBuf content = packet.content();
            if (QueryPacketHandler.isQueryPacket(content)) {
                new QueryPacketHandler(geyser, packet.sender(), content);
            }
        } catch (Throwable e) {
            // Error must be caught or it will be swallowed
            if (geyser.getConfig().isDebugMode()) {
                geyser.getLogger().error("Error occurred during unhandled datagram!", e);
            }
        }
    }
}