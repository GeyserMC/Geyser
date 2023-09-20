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
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.translator.inventory.item.CustomItemTranslator;
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

                // Needed here since 1.19.81 for dimension switching
                session.getEntityCache().updateBossBars();
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
            case START_BREAK: {
                // Ignore START_BREAK when the player is CREATIVE to avoid Spigot receiving 2 packets it interpets as block breaking. https://github.com/GeyserMC/Geyser/issues/4021 
                if (session.getGameMode() == GameMode.CREATIVE) { 
                    break;
                }
                
                // Start the block breaking animation
                int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
                LevelEventPacket startBreak = new LevelEventPacket();
                startBreak.setType(LevelEvent.BLOCK_START_BREAK);
                startBreak.setPosition(vector.toFloat());
                double breakTime = BlockUtils.getSessionBreakTime(session, BlockRegistries.JAVA_BLOCKS.get(blockState)) * 20;

                // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
                GeyserItemStack item = session.getPlayerInventory().getItemInHand();
                ItemMapping mapping = item.getMapping(session);
                ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getNbt(), mapping) : null;
                CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(blockState);
                SkullCache.Skull skull = session.getSkullCache().getSkulls().get(vector);

                session.setBlockBreakStartTime(0);
                if (blockStateOverride != null || customItem != null || (skull != null && skull.getBlockDefinition() != null)) {
                    session.setBlockBreakStartTime(System.currentTimeMillis());
                }
                startBreak.setData((int) (65535 / breakTime));
                session.setBreakingBlock(blockState);
                session.sendUpstreamPacket(startBreak);

                // Account for fire - the client likes to hit the block behind.
                Vector3i fireBlockPos = BlockUtils.getBlockPosition(vector, packet.getFace());
                int blockUp = session.getGeyser().getWorldManager().getBlockAt(session, fireBlockPos);
                String identifier = BlockRegistries.JAVA_BLOCKS.getOrDefault(blockUp, BlockMapping.DEFAULT).getJavaIdentifier();
                if (identifier.startsWith("minecraft:fire") || identifier.startsWith("minecraft:soul_fire")) {
                    ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                            Direction.VALUES[packet.getFace()], session.getWorldCache().nextPredictionSequence());
                    session.sendDownstreamPacket(startBreakingPacket);
                }

                ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
                        vector, Direction.VALUES[packet.getFace()], session.getWorldCache().nextPredictionSequence());
                session.sendDownstreamPacket(startBreakingPacket);
                break;
            }
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
                continueBreakPacket.setType(LevelEvent.PARTICLE_CRACK_BLOCK);
                continueBreakPacket.setData((session.getBlockMappings().getBedrockBlockId(breakingBlock)) | (packet.getFace() << 24));
                continueBreakPacket.setPosition(vectorFloat);
                session.sendUpstreamPacket(continueBreakPacket);

                // Update the break time in the event that player conditions changed (jumping, effects applied)
                LevelEventPacket updateBreak = new LevelEventPacket();
                updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
                updateBreak.setPosition(vectorFloat);
                double breakTime = BlockUtils.getSessionBreakTime(session, BlockRegistries.JAVA_BLOCKS.get(breakingBlock)) * 20;


                // If the block is custom, we must keep track of when it should break ourselves
                long blockBreakStartTime = session.getBlockBreakStartTime();
                if (blockBreakStartTime != 0) {
                    long timeSinceStart = System.currentTimeMillis() - blockBreakStartTime;
                    // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
                    if (timeSinceStart >= (breakTime+=2) * 50) {
                        // Play break sound and particle
                        LevelEventPacket effectPacket = new LevelEventPacket();
                        effectPacket.setPosition(vectorFloat);
                        effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                        effectPacket.setData(session.getBlockMappings().getBedrockBlockId(breakingBlock));
                        session.sendUpstreamPacket(effectPacket);
                        
                        // Break the block
                        ServerboundPlayerActionPacket finishBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
                                vector, Direction.VALUES[packet.getFace()], session.getWorldCache().nextPredictionSequence());
                        session.sendDownstreamPacket(finishBreakingPacket);
                        session.setBlockBreakStartTime(0);
                        break;
                    }
                }

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
                stopBreak.setType(LevelEvent.BLOCK_STOP_BREAK);
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
                break;
            case JUMP:
                entity.setOnGround(false); // Increase block break time while jumping
                break;
            case MISSED_SWING:
                // TODO Re-evaluate after pre-1.20.10 is no longer supported?
                if (session.getArmAnimationTicks() == -1) {
                    session.sendDownstreamPacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
                    session.activateArmAnimationTicking();

                    // Send packet to Bedrock so it knows
                    AnimatePacket animatePacket = new AnimatePacket();
                    animatePacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
                    animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
                    session.sendUpstreamPacket(animatePacket);
                }
                break;
        }
    }
}
