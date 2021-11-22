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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
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
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.util.BlockUtils;

import java.util.ArrayList;

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
        Position position = new Position(vector.getX(), vector.getY(), vector.getZ());

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
                attributesPacket.setAttributes(new ArrayList<>(entity.getAttributes().values()));
                session.sendUpstreamPacket(attributesPacket);
                break;
            case START_SWIMMING:
                ServerboundPlayerCommandPacket startSwimPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.START_SPRINTING);
                session.sendDownstreamPacket(startSwimPacket);

                session.setSwimming(true);
                break;
            case STOP_SWIMMING:
                ServerboundPlayerCommandPacket stopSwimPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.STOP_SPRINTING);
                session.sendDownstreamPacket(stopSwimPacket);

                session.setSwimming(false);
                break;
            case START_GLIDE:
                // Otherwise gliding will not work in creative
                ServerboundPlayerAbilitiesPacket playerAbilitiesPacket = new ServerboundPlayerAbilitiesPacket(false);
                session.sendDownstreamPacket(playerAbilitiesPacket);
            case STOP_GLIDE:
                ServerboundPlayerCommandPacket glidePacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.START_ELYTRA_FLYING);
                session.sendDownstreamPacket(glidePacket);
                break;
            case START_SNEAK:
                ServerboundPlayerCommandPacket startSneakPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.START_SNEAKING);
                session.sendDownstreamPacket(startSneakPacket);

                // Toggle the shield, if relevant
                PlayerInventory playerInv = session.getPlayerInventory();
                ItemMapping shield = session.getItemMappings().getMapping("minecraft:shield");
                if ((playerInv.getItemInHand().getJavaId() == shield.getJavaId()) ||
                        (playerInv.getOffhand().getJavaId() == shield.getJavaId())) {
                    ServerboundUseItemPacket useItemPacket;
                    if (playerInv.getItemInHand().getJavaId() == shield.getJavaId()) {
                        useItemPacket = new ServerboundUseItemPacket(Hand.MAIN_HAND);
                    } else {
                        // Else we just assume it's the offhand, to simplify logic and to assure the packet gets sent
                        useItemPacket = new ServerboundUseItemPacket(Hand.OFF_HAND);
                    }
                    session.sendDownstreamPacket(useItemPacket);
                    session.getPlayerEntity().setFlag(EntityFlag.BLOCKING, true);
                    // metadata will be updated when sneaking
                }

                session.setSneaking(true);
                break;
            case STOP_SNEAK:
                ServerboundPlayerCommandPacket stopSneakPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.STOP_SNEAKING);
                session.sendDownstreamPacket(stopSneakPacket);

                // Stop shield, if necessary
                if (session.getPlayerEntity().getFlag(EntityFlag.BLOCKING)) {
                    ServerboundPlayerActionPacket releaseItemPacket = new ServerboundPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, BlockUtils.POSITION_ZERO, Direction.DOWN);
                    session.sendDownstreamPacket(releaseItemPacket);
                    session.getPlayerEntity().setFlag(EntityFlag.BLOCKING, false);
                    // metadata will be updated when sneaking
                }

                session.setSneaking(false);
                break;
            case START_SPRINT:
                ServerboundPlayerCommandPacket startSprintPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.START_SPRINTING);
                session.sendDownstreamPacket(startSprintPacket);
                session.setSprinting(true);
                break;
            case STOP_SPRINT:
                ServerboundPlayerCommandPacket stopSprintPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.STOP_SPRINTING);
                session.sendDownstreamPacket(stopSprintPacket);
                session.setSprinting(false);
                break;
            case DROP_ITEM:
                ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM, position, Direction.VALUES[packet.getFace()]);
                session.sendDownstreamPacket(dropItemPacket);
                break;
            case STOP_SLEEP:
                ServerboundPlayerCommandPacket stopSleepingPacket = new ServerboundPlayerCommandPacket((int) entity.getEntityId(), PlayerState.LEAVE_BED);
                session.sendDownstreamPacket(stopSleepingPacket);
                break;
            case BLOCK_INTERACT:
                // Client means to interact with a block; cancel bucket interaction, if any
                if (session.getBucketScheduledFuture() != null) {
                    session.getBucketScheduledFuture().cancel(true);
                    session.setBucketScheduledFuture(null);
                }
                // Otherwise handled in BedrockInventoryTransactionTranslator
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
                Vector3i fireBlockPos = BlockUtils.getBlockPosition(packet.getBlockPosition(), packet.getFace());
                int blockUp = session.getGeyser().getWorldManager().getBlockAt(session, fireBlockPos);
                String identifier = BlockRegistries.JAVA_IDENTIFIERS.get().get(blockUp);
                if (identifier.startsWith("minecraft:fire") || identifier.startsWith("minecraft:soul_fire")) {
                    ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, new Position(fireBlockPos.getX(),
                            fireBlockPos.getY(), fireBlockPos.getZ()), Direction.VALUES[packet.getFace()]);
                    session.sendDownstreamPacket(startBreakingPacket);
                    if (session.getGameMode() == GameMode.CREATIVE) {
                        break;
                    }
                }

                ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position, Direction.VALUES[packet.getFace()]);
                session.sendDownstreamPacket(startBreakingPacket);
                break;
            case CONTINUE_BREAK:
                if (session.getGameMode() == GameMode.CREATIVE) {
                    break;
                }
                Vector3f vectorFloat = vector.toFloat();
                LevelEventPacket continueBreakPacket = new LevelEventPacket();
                continueBreakPacket.setType(LevelEventType.PARTICLE_CRACK_BLOCK);
                continueBreakPacket.setData((session.getBlockMappings().getBedrockBlockId(session.getBreakingBlock())) | (packet.getFace() << 24));
                continueBreakPacket.setPosition(vectorFloat);
                session.sendUpstreamPacket(continueBreakPacket);

                // Update the break time in the event that player conditions changed (jumping, effects applied)
                LevelEventPacket updateBreak = new LevelEventPacket();
                updateBreak.setType(LevelEventType.BLOCK_UPDATE_BREAK);
                updateBreak.setPosition(vectorFloat);
                double breakTime = BlockUtils.getSessionBreakTime(session, BlockRegistries.JAVA_BLOCKS.get(session.getBreakingBlock())) * 20;
                updateBreak.setData((int) (65535 / breakTime));
                session.sendUpstreamPacket(updateBreak);
                break;
            case ABORT_BREAK:
                if (session.getGameMode() != GameMode.CREATIVE) {
                    // As of 1.16.210: item frame items are taken out here.
                    // Survival also sends START_BREAK, but by attaching our process here adventure mode also works
                    Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, packet.getBlockPosition());
                    if (itemFrameEntity != null) {
                        ServerboundInteractPacket interactPacket = new ServerboundInteractPacket((int) itemFrameEntity.getEntityId(),
                                InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                        session.sendDownstreamPacket(interactPacket);
                        break;
                    }
                }

                ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, position, Direction.DOWN);
                session.sendDownstreamPacket(abortBreakingPacket);
                LevelEventPacket stopBreak = new LevelEventPacket();
                stopBreak.setType(LevelEventType.BLOCK_STOP_BREAK);
                stopBreak.setPosition(vector.toFloat());
                stopBreak.setData(0);
                session.setBreakingBlock(BlockStateValues.JAVA_AIR_ID);
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
                attributesPacket.setAttributes(new ArrayList<>(entity.getAttributes().values()));
                session.sendUpstreamPacket(attributesPacket);

                session.getEntityCache().updateBossBars();
                break;
            case JUMP:
                entity.setOnGround(false); // Increase block break time while jumping
                break;
        }
    }
}
