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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.PlayerActionType;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockUtils;

@Translator(packet = PlayerActionPacket.class)
public class BedrockActionTranslator extends PacketTranslator<PlayerActionPacket> {

    @Override
    public void translate(GeyserSession session, PlayerActionPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();

        // Send book update before any player action
        if (packet.getAction() != PlayerActionType.RESPAWN) {
            session.getBookEditCache().checkForSend();
        }

        Vector3i vector = packet.getBlockPosition();

        switch (packet.getAction()) {
            case RESPAWN:
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
                break;
            case START_SWIMMING:
                if (!entity.getFlag(EntityFlag.SWIMMING)) {
                    ServerboundPlayerCommandPacket startSwimPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SPRINTING);
                    session.sendDownstreamPacket(startSwimPacket);

                    session.setSwimming(true);
                }
                break;
            case STOP_SWIMMING:
                // Prevent packet spam when Bedrock players are crawling near the edge of a block
                if (!session.getCollisionManager().mustPlayerCrawlHere()) {
                    ServerboundPlayerCommandPacket stopSwimPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SPRINTING);
                    session.sendDownstreamPacket(stopSwimPacket);

                    session.setSwimming(false);
                }
                break;
            case START_GLIDE:
                // Otherwise gliding will not work in creative
                ServerboundPlayerAbilitiesPacket playerAbilitiesPacket = new ServerboundPlayerAbilitiesPacket(false);
                session.sendDownstreamPacket(playerAbilitiesPacket);
            case STOP_GLIDE:
                ServerboundPlayerCommandPacket glidePacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING);
                session.sendDownstreamPacket(glidePacket);
                break;
            case START_SNEAK:
                ServerboundPlayerCommandPacket startSneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SNEAKING);
                session.sendDownstreamPacket(startSneakPacket);

                session.startSneaking();
                break;
            case STOP_SNEAK:
                ServerboundPlayerCommandPacket stopSneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SNEAKING);
                session.sendDownstreamPacket(stopSneakPacket);

                session.stopSneaking();
                break;
            case START_SPRINT:
                if (!entity.getFlag(EntityFlag.SWIMMING)) {
                    ServerboundPlayerCommandPacket startSprintPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SPRINTING);
                    session.sendDownstreamPacket(startSprintPacket);
                    session.setSprinting(true);
                }
                break;
            case STOP_SPRINT:
                if (!entity.getFlag(EntityFlag.SWIMMING)) {
                    ServerboundPlayerCommandPacket stopSprintPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SPRINTING);
                    session.sendDownstreamPacket(stopSprintPacket);
                }
                session.setSprinting(false);
                break;
            case DROP_ITEM:
                ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        vector, Direction.VALUES[packet.getFace()], 0);
                session.sendDownstreamPacket(dropItemPacket);
                break;
            case STOP_SLEEP:
                ServerboundPlayerCommandPacket stopSleepingPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.LEAVE_BED);
                session.sendDownstreamPacket(stopSleepingPacket);
                break;
            case START_BREAK:
                // Start the block breaking animation
                if (session.getGameMode() != GameMode.CREATIVE) {
                    int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
                    LevelEventPacket startBreak = new LevelEventPacket();
                    startBreak.setType(LevelEventType.BLOCK_START_BREAK);
                    startBreak.setPosition(vector.toFloat());
                    double breakTime = BlockUtils.getSessionBreakTime(session, BlockRegistries.JAVA_BLOCKS.get(blockState)) * 20;
                    startBreak.setData((int) (65535 / breakTime));
                    session.setBreakingBlock(blockState);
                    session.sendUpstreamPacket(startBreak);
                }

                // Account for fire - the client likes to hit the block behind.
                Vector3i fireBlockPos = BlockUtils.getBlockPosition(vector, packet.getFace());
                int blockUp = session.getGeyser().getWorldManager().getBlockAt(session, fireBlockPos);
                String identifier = BlockRegistries.JAVA_IDENTIFIERS.get().get(blockUp);
                if (identifier.startsWith("minecraft:fire") || identifier.startsWith("minecraft:soul_fire")) {
                    ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                            Direction.VALUES[packet.getFace()], session.getWorldCache().nextPredictionSequence());
                    session.sendDownstreamPacket(startBreakingPacket);
                    if (session.getGameMode() == GameMode.CREATIVE) {
                        break;
                    }
                }

                ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
                        vector, Direction.VALUES[packet.getFace()], session.getWorldCache().nextPredictionSequence());
                session.sendDownstreamPacket(startBreakingPacket);
                break;
            case CONTINUE_BREAK:
                if (session.getGameMode() == GameMode.CREATIVE) {
                    break;
                }
                int breakingBlock = session.getBreakingBlock();
                if (breakingBlock == -1) {
                    breakingBlock = BlockStateValues.JAVA_AIR_ID;
                }

                Vector3f vectorFloat = vector.toFloat();
                LevelEventPacket continueBreakPacket = new LevelEventPacket();
                continueBreakPacket.setType(LevelEventType.PARTICLE_CRACK_BLOCK);
                continueBreakPacket.setData((session.getBlockMappings().getBedrockBlockId(breakingBlock)) | (packet.getFace() << 24));
                continueBreakPacket.setPosition(vectorFloat);
                session.sendUpstreamPacket(continueBreakPacket);

                // Update the break time in the event that player conditions changed (jumping, effects applied)
                LevelEventPacket updateBreak = new LevelEventPacket();
                updateBreak.setType(LevelEventType.BLOCK_UPDATE_BREAK);
                updateBreak.setPosition(vectorFloat);
                double breakTime = BlockUtils.getSessionBreakTime(session, BlockRegistries.JAVA_BLOCKS.get(breakingBlock)) * 20;
                updateBreak.setData((int) (65535 / breakTime));
                session.sendUpstreamPacket(updateBreak);
                break;
            case ABORT_BREAK:
                if (session.getGameMode() != GameMode.CREATIVE) {
                    // As of 1.16.210: item frame items are taken out here.
                    // Survival also sends START_BREAK, but by attaching our process here adventure mode also works
                    Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, vector);
                    if (itemFrameEntity != null) {
                        ServerboundInteractPacket interactPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                                InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                        session.sendDownstreamPacket(interactPacket);
                        break;
                    }
                }

                ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, vector, Direction.DOWN, 0);
                session.sendDownstreamPacket(abortBreakingPacket);
                LevelEventPacket stopBreak = new LevelEventPacket();
                stopBreak.setType(LevelEventType.BLOCK_STOP_BREAK);
                stopBreak.setPosition(vector.toFloat());
                stopBreak.setData(0);
                session.setBreakingBlock(-1);
                session.sendUpstreamPacket(stopBreak);
                break;
            case STOP_BREAK:
                // Handled in BedrockInventoryTransactionTranslator
                break;
            case DIMENSION_CHANGE_SUCCESS:
                //sometimes the client doesn't feel like loading
                PlayStatusPacket spawnPacket = new PlayStatusPacket();
                spawnPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
                session.sendUpstreamPacket(spawnPacket);

                attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(entity.getGeyserId());
                attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                session.sendUpstreamPacket(attributesPacket);

                session.getEntityCache().updateBossBars();
                break;
            case JUMP:
                entity.setOnGround(false); // Increase block break time while jumping
                break;
        }
    }
}
