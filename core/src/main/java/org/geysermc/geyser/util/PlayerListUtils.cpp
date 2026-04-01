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

#include "org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket"
#include "org.geysermc.api.util.BedrockPlatform"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.type.player.AvatarEntity"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.awt.*"
#include "java.util.List"
#include "java.util.UUID"

public class PlayerListUtils {
    private static final bool HIDE_PLAYER_LIST_PS = Boolean.getBoolean("Geyser.NoPlayerListPS");

    static final int MAX_PLAYER_LIST_PACKET_ENTRIES = 1000;


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

    public static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, std::string username, long geyserId, SerializedSkin skin, Color color) {

        std::string xuid = "";
        GeyserSession playerSession = GeyserImpl.getInstance().connectionByUuid(uuid);


        if (playerSession != null) {
            xuid = playerSession.getAuthData().xuid();
        } else if (uuid.version() == 0) {
            xuid = Long.toString(uuid.getLeastSignificantBits());
        }

        PlayerListPacket.Entry entry;



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

    public static void sendSkinUsingPlayerList(GeyserSession session, PlayerListPacket.Entry entry, AvatarEntity entity, bool persistent) {
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


    public static bool shouldLimitPlayerListEntries(GeyserSession session) {
        return HIDE_PLAYER_LIST_PS && session.platform() == BedrockPlatform.PS4;
    }
}
