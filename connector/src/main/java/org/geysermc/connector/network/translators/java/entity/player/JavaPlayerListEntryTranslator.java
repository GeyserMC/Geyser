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

package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.skin.SkinManager;

@Translator(packet = ServerPlayerListEntryPacket.class)
public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER)
            return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setAction(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Action.ADD : PlayerListPacket.Action.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            switch (packet.getAction()) {
                case ADD_PLAYER:
                    PlayerEntity playerEntity;
                    boolean self = entry.getProfile().getId().equals(session.getPlayerEntity().getUuid());

                    if (self) {
                        // Entity is ourself
                        playerEntity = session.getPlayerEntity();
                    } else {
                        playerEntity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                    }

                    if (playerEntity == null) {
                        // It's a new player
                        playerEntity = new PlayerEntity(
                                entry.getProfile(),
                                -1,
                                session.getEntityCache().getNextEntityId().incrementAndGet(),
                                Vector3f.ZERO,
                                Vector3f.ZERO,
                                Vector3f.ZERO
                        );

                        session.getEntityCache().addPlayerEntity(playerEntity);
                    } else {
                        playerEntity.setProfile(entry.getProfile());
                    }

                    playerEntity.setPlayerList(true);

                    // We'll send our own PlayerListEntry in requestAndHandleSkinAndCape
                    // But we need to send other player's entries so they show up in the player list
                    // without processing their skin information - that'll be processed when they spawn in
                    if (self) {
                        SkinManager.requestAndHandleSkinAndCape(playerEntity, session, skinAndCape ->
                                GeyserConnector.getInstance().getLogger().debug("Loaded Local Bedrock Java Skin Data for " + session.getClientData().getUsername()));
                    } else {
                        playerEntity.setValid(true);
                        PlayerListPacket.Entry playerListEntry = SkinManager.buildCachedEntry(session, playerEntity);

                        translate.getEntries().add(playerListEntry);
                    }
                    break;
                case REMOVE_PLAYER:
                    // As the player entity is no longer present, we can remove the entry
                    PlayerEntity entity = session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
                    if (entity != null) {
                        // Just remove the entity's player list status
                        // Don't despawn the entity - the Java server will also take care of that.
                        entity.setPlayerList(false);
                    }
                    if (entity == session.getPlayerEntity()) {
                        // If removing ourself we use our AuthData UUID
                        translate.getEntries().add(new PlayerListPacket.Entry(session.getAuthData().getUUID()));
                    } else {
                        translate.getEntries().add(new PlayerListPacket.Entry(entry.getProfile().getId()));
                    }
                    break;
            }
        }

        if (!translate.getEntries().isEmpty() && (packet.getAction() == PlayerListEntryAction.REMOVE_PLAYER || session.getUpstream().isInitialized())) {
            session.sendUpstreamPacket(translate);
        }
    }
}
