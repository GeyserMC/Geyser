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

package org.geysermc.connector.network.translators.java.entity.player;

import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.SkinUtils;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;

@Translator(packet = ServerPlayerListEntryPacket.class)
public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER) return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setAction(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Action.ADD : PlayerListPacket.Action.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
                boolean self = entry.getProfile().getId().equals(session.getPlayerEntity().getUuid());

                PlayerEntity playerEntity = session.getPlayerEntity();
                if (self) playerEntity.setProfile(entry.getProfile());
                else {
                    playerEntity = new PlayerEntity(
                            entry.getProfile(),
                            -1,
                            session.getEntityCache().getNextEntityId().incrementAndGet(),
                            Vector3f.ZERO,
                            Vector3f.ZERO,
                            Vector3f.ZERO
                    );
                }

                session.getEntityCache().addPlayerEntity(playerEntity);
                playerEntity.setPlayerList(true);

                translate.getEntries().add(SkinUtils.buildCachedEntry(entry.getProfile(), playerEntity.getGeyserId()));
            } else {
                PlayerEntity entity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                if (entity != null && entity.isValid()) {
                    // remove from tablist but player entity is still there
                    entity.setPlayerList(false);
                } else {
                    // just remove it from caching
                    session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
                }
                translate.getEntries().add(new PlayerListPacket.Entry(entry.getProfile().getId()));
            }
        }

        if (packet.getAction() == PlayerListEntryAction.REMOVE_PLAYER || session.getUpstream().isInitialized()) {
            session.getUpstream().sendPacket(translate);
        }
    }
}
