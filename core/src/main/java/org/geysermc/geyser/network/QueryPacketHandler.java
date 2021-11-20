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

package org.geysermc.geyser.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.ping.GeyserPingInfo;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class QueryPacketHandler {
    public static final byte HANDSHAKE = 0x09;
    public static final byte STATISTICS = 0x00;

    private final GeyserImpl geyser;
    private final InetSocketAddress sender;
    private final byte type;
    private final int sessionId;
    private byte[] token;

    /**
     * The Query packet handler instance. The unsigned short magic handshake should already be read at this point,
     * and the packet should be verified to have enough buffer space to be a qualified query packet.
     *
     * @param geyser Geyser
     * @param sender The Sender IP/Port for the Query
     * @param buffer The Query data
     */
    public QueryPacketHandler(GeyserImpl geyser, InetSocketAddress sender, ByteBuf buffer) {
        this.geyser = geyser;
        this.sender = sender;
        this.type = buffer.readByte();
        this.sessionId = buffer.readInt();

        regenerateToken();
        handle();
    }

    /**
     * Checks the packet is in fact a query packet
     *
     * @param buffer Query data
     * @return if the packet is a query packet
     */
    public static boolean isQueryPacket(ByteBuf buffer) {
        // 2 for magic short, 1 for type byte and 4 for session ID int
        return buffer.readableBytes() >= (2 + 1 + 4) && buffer.readUnsignedShort() == 0xFEFD;
    }

    /**
     * Handles the query
     */
    private void handle() {
        switch (type) {
            case HANDSHAKE:
                sendToken();
            case STATISTICS:
                sendQueryData();
        }
    }

    /**
     * Sends the token to the sender
     */
    private void sendToken() {
        ByteBuf reply = ByteBufAllocator.DEFAULT.ioBuffer(10);
        reply.writeByte(HANDSHAKE);
        reply.writeInt(sessionId);
        reply.writeBytes(getTokenString(this.token, this.sender.getAddress()));
        reply.writeByte(0);

        sendPacket(reply);
    }

    /**
     * Sends the query data to the sender
     */
    private void sendQueryData() {
        byte[] gameData = getGameData();
        byte[] playerData = getPlayers();

        ByteBuf reply = ByteBufAllocator.DEFAULT.ioBuffer(1 + 4 + gameData.length + playerData.length);
        reply.writeByte(STATISTICS);
        reply.writeInt(sessionId);

        // Game Info
        reply.writeBytes(gameData);

        // Players
        reply.writeBytes(playerData);

        sendPacket(reply);
    }

    /**
     * Gets the game data for the query
     *
     * @return the game data for the query
     */
    private byte[] getGameData() {
        ByteArrayOutputStream query = new ByteArrayOutputStream();

        GeyserPingInfo pingInfo = null;
        String motd;
        String currentPlayerCount;
        String maxPlayerCount;
        String map;

        if (geyser.getConfig().isPassthroughMotd() || geyser.getConfig().isPassthroughPlayerCounts()) {
            pingInfo = geyser.getBootstrap().getGeyserPingPassthrough().getPingInformation();
        }

        if (geyser.getConfig().isPassthroughMotd() && pingInfo != null) {
            String[] javaMotd = MessageTranslator.convertMessageLenient(pingInfo.getDescription()).split("\n");
            motd = javaMotd[0].trim(); // First line of the motd.
        } else {
            motd = geyser.getConfig().getBedrock().getMotd1();
        }

        // If passthrough player counts is enabled lets get players from the server
        if (geyser.getConfig().isPassthroughPlayerCounts() && pingInfo != null) {
            currentPlayerCount = String.valueOf(pingInfo.getPlayers().getOnline());
            maxPlayerCount = String.valueOf(pingInfo.getPlayers().getMax());
        } else {
            currentPlayerCount = String.valueOf(geyser.getSessionManager().getSessions().size());
            maxPlayerCount = String.valueOf(geyser.getConfig().getMaxPlayers());
        }

        // If passthrough protocol name is enabled let's get the protocol name from the ping response.
        if (geyser.getConfig().isPassthroughProtocolName() && pingInfo != null) {
            map = pingInfo.getVersion().getName();
        } else {
            map = GeyserImpl.NAME;
        }

        // Create a hashmap of all game data needed in the query
        Map<String, String> gameData = new HashMap<>();
        gameData.put("hostname", motd);
        gameData.put("gametype", "SMP");
        gameData.put("game_id", "MINECRAFT");
        gameData.put("version", GeyserImpl.NAME + " (" + GeyserImpl.GIT_VERSION + ") " + MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion());
        gameData.put("plugins", "");
        gameData.put("map", map);
        gameData.put("numplayers", currentPlayerCount);
        gameData.put("maxplayers", maxPlayerCount);
        gameData.put("hostport", String.valueOf(geyser.getConfig().getBedrock().getPort()));
        gameData.put("hostip", geyser.getConfig().getBedrock().getAddress());

        try {
            writeString(query, "GeyserMC");
            query.write((byte) 0x80);
            query.write((byte) 0x00);

            // Fills the game data
            for (Map.Entry<String, String> entry : gameData.entrySet()) {
                writeString(query, entry.getKey());
                writeString(query, entry.getValue());
            }

            // Final byte to show the end of the game data
            query.write(new byte[] { 0x00, 0x01 });
            return query.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Generate a byte[] storing the player names
     *
     * @return The byte[] representation of players
     */
    private byte[] getPlayers() {
        ByteArrayOutputStream query = new ByteArrayOutputStream();

        GeyserPingInfo pingInfo = null;
        if (geyser.getConfig().isPassthroughMotd() || geyser.getConfig().isPassthroughPlayerCounts()) {
            pingInfo = geyser.getBootstrap().getGeyserPingPassthrough().getPingInformation();
        }

        try {
            // Start the player section
            writeString(query, "player_");
            query.write((byte) 0x00);

            // Fill player names
            if (pingInfo != null) {
                for (String username : pingInfo.getPlayerList()) {
                    writeString(query, username);
                }
            }

            // Final byte to show the end of the player data
            query.write((byte) 0x00);
            return query.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Partially mimics {@link java.io.DataOutputStream#writeBytes(String)} which is what the Minecraft server uses as of 1.17.1.
     */
    private void writeString(OutputStream stream, String value) throws IOException {
        int length = value.length();
        for (int i = 0; i < length; i++) {
            stream.write((byte) value.charAt(i));
        }
        // Padding to indicate the end of the string
        stream.write((byte) 0x00);
    }

    /**
     * Sends a packet to the sender
     *
     * @param data packet data
     */
    private void sendPacket(ByteBuf data) {
        geyser.getBedrockServer().getRakNet().send(sender, data);
    }

    /**
     * Regenerates a token
     */
    public void regenerateToken() {
        byte[] token = new byte[16];
        for (int i = 0; i < 16; i++) {
            token[i] = (byte) ThreadLocalRandom.current().nextInt(255);
        }

        this.token = token;
    }

    /**
     * Gets an MD5 token for the current IP/Port.
     * This should reset every 30 seconds but a new one is generated per instance
     * Seems wasteful to code something in to clear it when it has no use.
     *
     * @param token the token
     * @param address the address
     * @return an MD5 token for the current IP/Port
     */
    public static byte[] getTokenString(byte[] token, InetAddress address) {
        try {
            // Generate an MD5 hash from the address
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(address.toString().getBytes(StandardCharsets.UTF_8));
            digest.update(token);

            // Get the first 4 bytes of the digest
            byte[] digestBytes = Arrays.copyOf(digest.digest(), 4);

            // Convert the bytes to a buffer
            ByteBuffer byteBuffer = ByteBuffer.wrap(digestBytes);

            // Turn the number into a null terminated string
            return (byteBuffer.getInt() + "\0").getBytes();
        } catch (NoSuchAlgorithmException e) {
            return (ByteBuffer.allocate(4).putInt(ThreadLocalRandom.current().nextInt()).getInt() + "\0").getBytes();
        }
    }
}
