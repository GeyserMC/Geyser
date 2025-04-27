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

import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.session.GeyserSession;

import java.util.List;

public class PlayerListUtils {
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
}
