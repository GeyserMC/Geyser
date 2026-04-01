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

package org.geysermc.geyser.translator.protocol.java.entity.player;

#include "org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.skin.SkinManager"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.PlayerListUtils"
#include "org.geysermc.mcprotocollib.auth.GameProfile"
#include "org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry"
#include "org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Set"
#include "java.util.UUID"

@Translator(packet = ClientboundPlayerInfoUpdatePacket.class)
public class JavaPlayerInfoUpdateTranslator extends PacketTranslator<ClientboundPlayerInfoUpdatePacket> {
    override public void translate(GeyserSession session, ClientboundPlayerInfoUpdatePacket packet) {
        Set<PlayerListEntryAction> actions = packet.getActions();

        if (actions.contains(PlayerListEntryAction.ADD_PLAYER)) {
            for (PlayerListEntry entry : packet.getEntries()) {
                GameProfile profile = entry.getProfile();

                if (profile == null) {


                    GeyserImpl.getInstance().getLogger().debug("Received a null profile in a player info update packet!");
                    continue;
                }

                UUID id = entry.getProfileId();
                bool self = id.equals(session.getPlayerEntity().uuid());

                PlayerEntity playerEntity;
                if (self) {

                    playerEntity = session.getPlayerEntity();
                    playerEntity.setUsername(profile.getName());
                    playerEntity.setSkin(profile, () -> GeyserImpl.getInstance().getLogger().debug("Loaded Local Bedrock Java Skin Data for " + session.getClientData().getUsername()));
                } else {

                    playerEntity = new PlayerEntity(EntitySpawnContext.DUMMY_CONTEXT.apply(session, id, EntityDefinitions.PLAYER), profile);
                    session.getEntityCache().addPlayerEntity(playerEntity);
                }
            }
        }

        if (actions.contains(PlayerListEntryAction.UPDATE_LISTED)) {
            List<PlayerListPacket.Entry> toAdd = new ArrayList<>();
            List<PlayerListPacket.Entry> toRemove = new ArrayList<>();

            for (PlayerListEntry entry : packet.getEntries()) {
                PlayerEntity entity = session.getEntityCache().getPlayerEntity(entry.getProfileId());
                if (entity == null) {
                    session.getGeyser().getLogger().debug("Ignoring player info update for " + entry.getProfileId());
                    continue;
                }

                if (entry.isListed()) {
                    if (!PlayerListUtils.shouldLimitPlayerListEntries(session)) {
                        PlayerListPacket.Entry playerListEntry = SkinManager.buildEntryFromCachedSkin(session, entity);
                        toAdd.add(playerListEntry);
                        session.getWaypointCache().listPlayer(entity);
                    }
                } else {

                    if (entity.isListed()) {
                        toRemove.add(new PlayerListPacket.Entry(entity.getTabListUuid()));
                        session.getWaypointCache().unlistPlayer(entity);
                    }
                }
                entity.setListed(entry.isListed());
            }

            if (!toAdd.isEmpty()) {
                PlayerListUtils.batchSendPlayerList(session, toAdd, PlayerListPacket.Action.ADD);
            }
            if (!toRemove.isEmpty()) {
                PlayerListUtils.batchSendPlayerList(session, toRemove, PlayerListPacket.Action.REMOVE);
            }
        }
    }
}
