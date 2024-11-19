/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

@Translator(packet = PlayerActionPacket.class)
public class BedrockPlayerActionTranslator extends PacketTranslator<PlayerActionPacket> {

    @Override
    public void translate(GeyserSession session, PlayerActionPacket packet) {
        // This packet was used more before server auth movement was needed, but it's still used for a couple things...
        switch (packet.getAction()) {
            case RESPAWN -> {
                SessionPlayerEntity entity = session.getPlayerEntity();
                // Respawn process is finished and the server and client are both OK with respawning.
                EntityEventPacket eventPacket = new EntityEventPacket();
                eventPacket.setRuntimeEntityId(entity.getGeyserId());
                eventPacket.setType(EntityEventType.RESPAWN);
                eventPacket.setData(0);
                session.sendUpstreamPacket(eventPacket);
                // Resend attributes or else in rare cases the user can think they're not dead when they are, upon joining the server
                UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(entity.getGeyserId());
                attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                session.sendUpstreamPacket(attributesPacket);

                // Bounding box must be sent after a player dies and respawns since 1.19.40
                entity.updateBoundingBox();

                // Needed here since 1.19.81 for dimension switching
                session.getEntityCache().updateBossBars();
            }
            case STOP_SLEEP -> {
                ServerboundPlayerCommandPacket stopSleepingPacket = new ServerboundPlayerCommandPacket(session.getPlayerEntity().getEntityId(), PlayerState.LEAVE_BED);
                session.sendDownstreamGamePacket(stopSleepingPacket);
            }
            case DIMENSION_CHANGE_REQUEST_OR_CREATIVE_DESTROY_BLOCK -> { // Used by client to get book from lecterns and items from item frame in creative mode since 1.20.70
                Vector3i vector = packet.getBlockPosition();
                BlockState state = session.getGeyser().getWorldManager().blockAt(session, vector);

                if (state.getValue(Properties.HAS_BOOK, false)) {
                    session.setDroppingLecternBook(true);

                    ServerboundUseItemOnPacket blockPacket = new ServerboundUseItemOnPacket(
                        vector,
                        Direction.DOWN,
                        Hand.MAIN_HAND,
                        0, 0, 0,
                        false,
                        false,
                        session.getWorldCache().nextPredictionSequence());
                    session.sendDownstreamGamePacket(blockPacket);
                    break;
                }

                Entity itemFrame = ItemFrameEntity.getItemFrameEntity(session, vector);
                if (itemFrame != null) {
                    ServerboundInteractPacket interactPacket = new ServerboundInteractPacket(itemFrame.getEntityId(),
                        InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                    session.sendDownstreamGamePacket(interactPacket);
                }
            }
            case DIMENSION_CHANGE_SUCCESS -> {
                SessionPlayerEntity entity = session.getPlayerEntity();
                // Sometimes the client doesn't feel like loading
                PlayStatusPacket spawnPacket = new PlayStatusPacket();
                spawnPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
                session.sendUpstreamPacket(spawnPacket);

                UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(entity.getGeyserId());
                attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                session.sendUpstreamPacket(attributesPacket);
            }
        }
    }
}
