/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class PlayerListUtils {
    private static final boolean HIDE_PLAYER_LIST_PS = Boolean.getBoolean("Geyser.NoPlayerListPS");

    static final int MAX_PLAYER_LIST_PACKET_ENTRIES = 1000;

    /**
     * Sends a player list packet to the client with the given entries.
     * If there are too many provided entries, multiple packets will be sent.
     *
     * @param session the Geyser session
     * @param entries the list of player list packet entries to send
     * @param action  the action to perform with the player list
     */
    public static void batchSendPlayerList(GeyserSession session, List<PlayerListPacket.Entry> entries, PlayerListPacket.Action action) {
        if (entries.size() > MAX_PLAYER_LIST_PACKET_ENTRIES) {
            int batches = entries.size() / MAX_PLAYER_LIST_PACKET_ENTRIES + (entries.size() % MAX_PLAYER_LIST_PACKET_ENTRIES > 0 ? 1 : 0);
            for (int i = 0; i < batches; i++) {
                int start = i * MAX_PLAYER_LIST_PACKET_ENTRIES;
                int end = Math.min(start + MAX_PLAYER_LIST_PACKET_ENTRIES, entries.size());

                PlayerListPacket packet = new PlayerListPacket();
                packet.setAction(action);
                packet.getEntries().addAll(entries.subList(start, end));
                session.sendUpstreamPacket(packet);
            }
        } else {
            PlayerListPacket packet = new PlayerListPacket();
            packet.setAction(action);
            packet.getEntries().addAll(entries);
            session.sendUpstreamPacket(packet);
        }
    }

    public static PlayerListPacket.Entry forSkullPlayerEntity(AvatarEntity entity, SerializedSkin skin) {
        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(entity.uuid());
        entry.setName(entity.getUsername());
        entry.setEntityId(entity.geyserId());
        entry.setSkin(skin);
        entry.setXuid("");
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        entry.setColor(Color.LIGHT_GRAY);
        return entry;
    }

    public static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, String username, long geyserId, SerializedSkin skin, Color color) {
        // This attempts to find the XUID of the player so profile images show up for Xbox accounts
        String xuid = "";
        GeyserSession playerSession = GeyserImpl.getInstance().connectionByUuid(uuid);

        // Prefer looking up xuid using the session to catch linked players
        if (playerSession != null) {
            xuid = playerSession.getAuthData().xuid();
        } else if (uuid.version() == 0) {
            xuid = Long.toString(uuid.getLeastSignificantBits());
        }

        PlayerListPacket.Entry entry;

        // If we are building a PlayerListEntry for our own session we use our AuthData UUID instead of the Java UUID
        // as Bedrock expects to get back its own provided UUID
        if (session.getPlayerEntity().uuid().equals(uuid)) {
            entry = new PlayerListPacket.Entry(session.getAuthData().uuid());
        } else {
            entry = new PlayerListPacket.Entry(uuid);
        }

        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(skin);
        entry.setXuid(xuid);
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        entry.setColor(color);
        return entry;
    }

    public static void sendSkinUsingPlayerList(GeyserSession session, PlayerListPacket.Entry entry, AvatarEntity entity, boolean persistent) {
        PlayerListPacket listPacket = new PlayerListPacket();
        listPacket.setAction(PlayerListPacket.Action.ADD);
        listPacket.getEntries().add(entry);
        session.sendUpstreamPacket(listPacket);

        if (!persistent) {
            PlayerListPacket unlistPacket = new PlayerListPacket();
            unlistPacket.setAction(PlayerListPacket.Action.REMOVE);
            unlistPacket.getEntries().add(new PlayerListPacket.Entry(entity.uuid()));
            session.sendUpstreamPacket(unlistPacket);
        }
    }

    /**
     * Whether Geyser should limit the player list entries shown to the amount of players actually displayed / near the player
     * Avoids client crashes when opening the chat on playstation consoles
     */
    public static boolean shouldLimitPlayerListEntries(GeyserSession session) {
        return HIDE_PLAYER_LIST_PS && session.platform() == BedrockPlatform.PS4;
    }
}
