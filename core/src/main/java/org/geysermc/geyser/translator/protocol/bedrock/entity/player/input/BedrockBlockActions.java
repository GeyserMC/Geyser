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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.entity.EntityDefinitions;
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
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
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
            PlayerActionType action = blockActionData.getAction();
            Vector3i vector = blockActionData.getBlockPosition();
            int blockFace = blockActionData.getFace();

            GeyserImpl.getInstance().getLogger().info(blockActionData.toString());

            switch (action) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        vector, Direction.VALUES[blockFace], 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                case START_BREAK -> handleStartBreak(session, vector, blockFace);
                case BLOCK_CONTINUE_DESTROY -> {
                    if (session.getGameMode() == GameMode.CREATIVE || isSteeringBoat(session, vector)) {
                        break;
                    }

                    Direction direction = Direction.VALUES[blockFace];

                    // The Bedrock client won't send a new start_break packet, but just continue breaking blocks
                    if (!vector.equals(session.getBlockBreakPosition())) {
                        if (session.getBlockBreakProgress() < 1) {
                            // Abort block breaking for previous block, and start breaking this new block
                            // Otherwise the client will continue showing a block breaking animation for the previous block.
                            stopBreakingBlock(session, session.getBlockBreakPosition().toFloat());
                            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, session.getBlockBreakPosition(), Direction.DOWN, 0);
                            session.sendDownstreamGamePacket(abortBreakingPacket);
                        }

                        // Start breaking the new block
                        handleStartBreak(session, vector, blockFace);
                        break;
                    }

                    int breakingBlock = session.getBreakingBlock();
                    if (breakingBlock == -1) {
                        breakingBlock = Block.JAVA_AIR_ID;
                    }

                    Vector3f vectorFloat = vector.toFloat();

                    BlockState breakingBlockState = BlockState.of(breakingBlock);
                    spawnBlockBreakParticles(session, direction, vector, breakingBlockState);

                    double breakTime = BlockUtils.getSessionBreakTimeTicks(session, breakingBlockState.block());
                    // If the block is custom, we must keep track of when it should break ourselves
                    long blockBreakStartTime = session.getBlockBreakStartTime();
                    if (blockBreakStartTime != 0) {
                        long timeSinceStart = System.currentTimeMillis() - blockBreakStartTime;
                        // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
                        if (timeSinceStart >= (breakTime += 2) * 50) {
                            stopBreakingBlock(session, vector.toFloat());
                            // Play break sound and particle
                            LevelEventPacket effectPacket = new LevelEventPacket();
                            effectPacket.setPosition(vectorFloat);
                            effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                            effectPacket.setData(session.getBlockMappings().getBedrockBlockId(breakingBlock));
                            session.sendUpstreamPacket(effectPacket);

                            // Break the block
                            sendJavaPlayerActionPacket(session, PlayerAction.FINISH_DIGGING, vector, Direction.values()[blockFace]);
                            resetSessionVariables(session);
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

                    // Bedrock "confirms" that it stopped breaking blocks by sending an abort packet after breaking the block
                    if (session.getBlockBreakPosition() == null) {
                        break;
                    }


                    ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, vector, Direction.DOWN, 0);
                    session.sendDownstreamGamePacket(abortBreakingPacket);

                    stopBreakingBlock(session, session.getBlockBreakPosition().toFloat());
                    resetSessionVariables(session);
                }
                // Handled in BedrockInventoryTransactionTranslator
                case BLOCK_PREDICT_DESTROY -> {
                    handleBlockDestroy(session, vector, blockFace);
                }
            }
        }
    }

    private static void handleStartBreak(GeyserSession session, Vector3i vector, int blockFace) {

        // Only send block breaking in the BLOCK_PREDICT_DESTROY case
        if (session.getGameMode() == GameMode.CREATIVE || isSteeringBoat(session, vector)) {
            return;
        }

        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, vector);
        if (itemFrameEntity != null) {
            // Will be handled in ABORT_BREAK
            return;
        }

        Vector3f playerPosition = session.getPlayerEntity().getPosition();
        playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
        if (!BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector)) {
            resetSessionVariables(session);
            stopBreakingBlock(session, vector.toFloat());
            return;
        }

        int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
        double breakTime = BlockUtils.getSessionBreakTimeTicks(session, BlockState.of(blockState).block());

        // Start the block breaking animation
        LevelEventPacket startBreak = new LevelEventPacket();
        startBreak.setType(LevelEvent.BLOCK_START_BREAK);
        startBreak.setPosition(vector.toFloat());
        startBreak.setData((int) (65535 / breakTime));
        session.sendUpstreamPacket(startBreak);

        session.setBreakingBlock(blockState);
        session.setBlockBreakPosition(vector);
        session.setBlockBreakStartTime(0);

        // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
        // TODO 1.21.5 use tool attribute
        GeyserItemStack item = session.getPlayerInventory().getItemInHand();
        ItemMapping mapping = item.getMapping(session);
        ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getComponents(), mapping) : null;
        CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(blockState);
        SkullCache.Skull skull = session.getSkullCache().getSkulls().get(vector);

        if (blockStateOverride != null || customItem != null || (skull != null && skull.getBlockDefinition() != null)) {
            session.setBlockBreakStartTime(System.currentTimeMillis());
        }

        Direction direction = Direction.VALUES[blockFace];
        spawnBlockBreakParticles(session, direction, vector, BlockState.of(blockState));

        // Account for fire - the client likes to hit the block behind.
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(vector, blockFace);
        Block block = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
            // TODO test
            sendJavaPlayerActionPacket(session, PlayerAction.START_DIGGING, fireBlockPos, direction);
            sendJavaPlayerActionPacket(session, PlayerAction.FINISH_DIGGING, fireBlockPos, direction);
        }

        sendJavaPlayerActionPacket(session, PlayerAction.START_DIGGING, vector, direction);
    }

    private static void handleBlockDestroy(GeyserSession session, Vector3i vector, int blockFace) {
        boolean creative = session.getGameMode() == GameMode.CREATIVE;

        int blockState = creative ? session.getGeyser().getWorldManager().getBlockAt(session, vector) : session.getBreakingBlock();

        // TODO move
        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, vector);
        if (itemFrameEntity != null) {
            ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                InteractAction.ATTACK, session.isSneaking());
            session.sendDownstreamGamePacket(attackPacket);
            return;
        }

        // Already done this check in survival mode
        if (creative) {
            Vector3f playerPosition = session.getPlayerEntity().getPosition();
            playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
            if (!BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector)) {
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, vector);
                return;
            }
        } else {
            if (!vector.equals(session.getBlockBreakPosition())) {
                // Restore correct block if we aren't breaking this one
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, vector);
                return;
            }
        }

        session.setLastBlockPlaced(null);
        session.setLastBlockPlacePosition(null);

        // Same deal with vanilla block placing as above.
        if (!session.getWorldBorder().isInsideBorderBoundaries()) {
            BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, vector);
            return;
        }

        // -1 means we don't know what block they're breaking
        if (blockState == -1) {
            blockState = Block.JAVA_AIR_ID;
        }

        // TODO
        session.setBlockBreakProgress(1.0);
        stopBreakingBlock(session, vector.toFloat());

        LevelEventPacket blockDestroyParticlePacket = new LevelEventPacket();
        blockDestroyParticlePacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
        blockDestroyParticlePacket.setPosition(vector.toFloat());
        blockDestroyParticlePacket.setData(session.getBlockMappings().getBedrockBlockId(blockState));
        session.sendUpstreamPacket(blockDestroyParticlePacket);

        resetSessionVariables(session);

        PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
        sendJavaPlayerActionPacket(session, action, vector, Direction.VALUES[blockFace]);
        session.getWorldCache().markPositionInSequence(vector);
    }

    private static boolean isSteeringBoat(GeyserSession session, Vector3i vector) {
        if (session.isHandsBusy()) {
            resetSessionVariables(session);
            stopBreakingBlock(session, vector.toFloat());
            return true;
        }
        return false;
    }

    private static void resetSessionVariables(GeyserSession session) {
        session.setBreakingBlock(-1);
        session.setBlockBreakPosition(null);
        session.setBlockBreakStartTime(0);
    }

    private static void stopBreakingBlock(GeyserSession session, Vector3f vector) {
        LevelEventPacket stopBreak = new LevelEventPacket();
        stopBreak.setType(LevelEvent.BLOCK_STOP_BREAK);
        stopBreak.setPosition(vector.toFloat());
        stopBreak.setData(0);
        session.sendUpstreamPacket(stopBreak);
    }

    public static void spawnBlockBreakParticles(GeyserSession session, Direction direction, Vector3i position, BlockState blockState) {
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

    private static void sendJavaPlayerActionPacket(GeyserSession session, PlayerAction action, Vector3i vector, Direction direction) {
        int sequence = session.getWorldCache().nextPredictionSequence();
        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(action, vector, direction, sequence);
        session.sendDownstreamGamePacket(packet);
        GeyserImpl.getInstance().getLogger().info(packet.toString());
    }
}
