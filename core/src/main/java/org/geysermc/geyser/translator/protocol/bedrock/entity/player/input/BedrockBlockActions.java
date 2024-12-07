/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player.input;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.List;

final class BedrockBlockActions {

    static void translate(GeyserSession session, List<PlayerBlockActionData> playerActions) {
        // Send book update before any player action
        session.getBookEditCache().checkForSend();

        for (PlayerBlockActionData blockActionData : playerActions) {
            handle(session, blockActionData);
        }
    }

    private static void handle(GeyserSession session, PlayerBlockActionData blockActionData) {
        PlayerActionType action = blockActionData.getAction();
        Vector3i vector = blockActionData.getBlockPosition();
        int blockFace = blockActionData.getFace();

        switch (action) {
            case DROP_ITEM -> {
                ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                    vector, Direction.VALUES[blockFace], 0);
                session.sendDownstreamGamePacket(dropItemPacket);
            }
            case START_BREAK -> {
                // Ignore START_BREAK when the player is CREATIVE to avoid Spigot receiving 2 packets it interpets as block breaking. https://github.com/GeyserMC/Geyser/issues/4021
                if (session.getGameMode() == GameMode.CREATIVE) {
                    break;
                }

                // Start the block breaking animation
                int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
                LevelEventPacket startBreak = new LevelEventPacket();
                startBreak.setType(LevelEvent.BLOCK_START_BREAK);
                startBreak.setPosition(vector.toFloat());
                double breakTime = BlockUtils.getSessionBreakTime(session, BlockState.of(blockState).block()) * 20;

                // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
                GeyserItemStack item = session.getPlayerInventory().getItemInHand();
                ItemMapping mapping = item.getMapping(session);
                ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getComponents(), mapping) : null;
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
                Vector3i fireBlockPos = BlockUtils.getBlockPosition(vector, blockFace);
                Block block = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
                Direction direction = Direction.VALUES[blockFace];
                if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
                    ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                        direction, session.getWorldCache().nextPredictionSequence());
                    session.sendDownstreamGamePacket(startBreakingPacket);
                }

                ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
                    vector, direction, session.getWorldCache().nextPredictionSequence());
                session.sendDownstreamGamePacket(startBreakingPacket);

                spawnBlockBreakParticles(session, direction, vector, BlockState.of(blockState));
            }
            case CONTINUE_BREAK -> {
                if (session.getGameMode() == GameMode.CREATIVE) {
                    break;
                }
                int breakingBlock = session.getBreakingBlock();
                if (breakingBlock == -1) {
                    breakingBlock = Block.JAVA_AIR_ID;
                }

                Vector3f vectorFloat = vector.toFloat();

                BlockState breakingBlockState = BlockState.of(breakingBlock);
                Direction direction = Direction.VALUES[blockFace];
                spawnBlockBreakParticles(session, direction, vector, breakingBlockState);

                double breakTime = BlockUtils.getSessionBreakTime(session, breakingBlockState.block()) * 20;
                // If the block is custom, we must keep track of when it should break ourselves
                long blockBreakStartTime = session.getBlockBreakStartTime();
                if (blockBreakStartTime != 0) {
                    long timeSinceStart = System.currentTimeMillis() - blockBreakStartTime;
                    // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
                    if (timeSinceStart >= (breakTime += 2) * 50) {
                        // Play break sound and particle
                        LevelEventPacket effectPacket = new LevelEventPacket();
                        effectPacket.setPosition(vectorFloat);
                        effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                        effectPacket.setData(session.getBlockMappings().getBedrockBlockId(breakingBlock));
                        session.sendUpstreamPacket(effectPacket);

                        // Break the block
                        ServerboundPlayerActionPacket finishBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
                            vector, direction, session.getWorldCache().nextPredictionSequence());
                        session.sendDownstreamGamePacket(finishBreakingPacket);
                        session.setBlockBreakStartTime(0);
                        break;
                    }
                }
                // Update the break time in the event that player conditions changed (jumping, effects applied)
                LevelEventPacket updateBreak = new LevelEventPacket();
                updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
                updateBreak.setPosition(vectorFloat);
                updateBreak.setData((int) (65535 / breakTime));
                session.sendUpstreamPacket(updateBreak);
            }
            case ABORT_BREAK -> {
                if (session.getGameMode() != GameMode.CREATIVE) {
                    // As of 1.16.210: item frame items are taken out here.
                    // Survival also sends START_BREAK, but by attaching our process here adventure mode also works
                    Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, vector);
                    if (itemFrameEntity != null) {
                        ServerboundInteractPacket interactPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                            InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                        session.sendDownstreamGamePacket(interactPacket);
                        break;
                    }
                }

                ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, vector, Direction.DOWN, 0);
                session.sendDownstreamGamePacket(abortBreakingPacket);
                LevelEventPacket stopBreak = new LevelEventPacket();
                stopBreak.setType(LevelEvent.BLOCK_STOP_BREAK);
                stopBreak.setPosition(vector.toFloat());
                stopBreak.setData(0);
                session.setBreakingBlock(-1);
                session.sendUpstreamPacket(stopBreak);
            }
            // Handled in BedrockInventoryTransactionTranslator
            case STOP_BREAK -> {
            }
        }
    }

    private static void spawnBlockBreakParticles(GeyserSession session, Direction direction, Vector3i position, BlockState blockState) {
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        switch (direction) {
            case UP -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_UP);
            case DOWN -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_DOWN);
            case NORTH -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_NORTH);
            case EAST -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_EAST);
            case SOUTH -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH);
            case WEST -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_WEST);
        }
        levelEventPacket.setPosition(position.toFloat());
        levelEventPacket.setData(session.getBlockMappings().getBedrockBlock(blockState).getRuntimeId());
        session.sendUpstreamPacket(levelEventPacket);
    }
}
