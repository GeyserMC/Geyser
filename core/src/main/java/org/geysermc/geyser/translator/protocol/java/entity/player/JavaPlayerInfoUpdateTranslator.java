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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Translator(packet = ClientboundPlayerInfoUpdatePacket.class)
public class JavaPlayerInfoUpdateTranslator extends PacketTranslator<ClientboundPlayerInfoUpdatePacket> {
    @Override
    public void translate(GeyserSession session, ClientboundPlayerInfoUpdatePacket packet) {
        Set<PlayerListEntryAction> actions = packet.getActions();

        if (actions.contains(PlayerListEntryAction.ADD_PLAYER)) {
            for (PlayerListEntry entry : packet.getEntries()) {
                GameProfile profile = entry.getProfile();
                PlayerEntity playerEntity;
                boolean self = profile.getId().equals(session.getPlayerEntity().getUuid());

                GameProfile.Property textures = profile.getProperty("textures");
                String texturesProperty = textures == null ? null : textures.getValue();

                if (self) {
                    // Entity is ourself
                    playerEntity = session.getPlayerEntity();
                } else {
                    // It's a new player
                    playerEntity = new PlayerEntity(
                            session,
                            -1,
                            session.getEntityCache().getNextEntityId().incrementAndGet(),
                            profile.getId(),
                            Vector3f.ZERO,
                            Vector3f.ZERO,
                            0, 0, 0,
                            profile.getName(),
                            texturesProperty
                    );

                    session.getEntityCache().addPlayerEntity(playerEntity);
                }
                playerEntity.setUsername(profile.getName());
                playerEntity.setTexturesProperty(texturesProperty);

                if (self) {
                    SkinManager.requestAndHandleSkinAndCape(playerEntity, session, skinAndCape ->
                            GeyserImpl.getInstance().getLogger().debug("Loaded Local Bedrock Java Skin Data for " + session.getClientData().getUsername()));
                } else {
                    playerEntity.setValid(true);
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
                    PlayerListPacket.Entry playerListEntry = SkinManager.buildCachedEntry(session, entity);
                    toAdd.add(playerListEntry);
                } else {
                    toRemove.add(new PlayerListPacket.Entry(entity.getTabListUuid()));
                }
            }

            if (!toAdd.isEmpty()) {
                PlayerListPacket tabListPacket = new PlayerListPacket();
                tabListPacket.setAction(PlayerListPacket.Action.ADD);
                tabListPacket.getEntries().addAll(toAdd);
                session.sendUpstreamPacket(tabListPacket);
            }
            if (!toRemove.isEmpty()) {
                PlayerListPacket tabListPacket = new PlayerListPacket();
                tabListPacket.setAction(PlayerListPacket.Action.REMOVE);
                tabListPacket.getEntries().addAll(toRemove);
                session.sendUpstreamPacket(tabListPacket);
            }
        }
    }
}
