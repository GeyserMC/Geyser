/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.Objects;
import java.util.Set;

public final class BlockBreakHandler {

    private final GeyserSession session;
    private boolean isDestroying = false;
    private int destroyDelay = 0;
    @Getter
    private Vector3i position = null;
    private Direction direction = null;
    private BlockState blockState;
    private GeyserItemStack itemStack;
    private float progress;

    private final Set<Block> GAME_MASTER_BLOCKS = Set.of(
        Blocks.COMMAND_BLOCK,
        Blocks.CHAIN_COMMAND_BLOCK,
        Blocks.REPEATING_COMMAND_BLOCK,
        Blocks.JIGSAW,
        Blocks.STRUCTURE_BLOCK,
        Blocks.TEST_BLOCK,
        Blocks.TEST_INSTANCE_BLOCK
    );

    public BlockBreakHandler(final GeyserSession session) {
        this.session = session;
    }

    public void tick(long tick) {
        if (isDestroying) {
            continueDestroying(position, direction, tick, false);
        } else if (destroyDelay > 0) {
            destroyDelay--;
        }
    }

    public void startDestroying(BlockState state, Vector3i blockPosition, Direction direction, long tick) {
        // TODO add component check
        if (!isDestroying || !Objects.equals(position, blockPosition)) {
            if (this.isDestroying) {
                stopBedrockBreaking(session, position.toFloat());
                sendBlockAction(PlayerAction.CANCEL_DIGGING, direction);
            }

            float progress = BlockUtils.getBlockMiningProgressPerTick(session, state.block(), session.getPlayerInventory().getItemInHand());
            if (progress >= 1.0F) {
                if (canDestroyBlock(state)) {
                    // Block can be mined instantly!
                    // TODO destroy progress check???
                    spawnBlockBreakParticles(session, direction, position, state);

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(position.toFloat());
                    effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                    effectPacket.setData(session.getBlockMappings().getBedrockBlockId(state.javaId()));
                    session.sendUpstreamPacket(effectPacket);
                } else {
                    stopBedrockBreaking(session, position.toFloat());
                    BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, position);
                }

                clearVariables();
            } else {
                this.isDestroying = true;
                this.position = blockPosition;
                this.itemStack = session.getPlayerInventory().getItemInHand();
                this.progress = 0.0F;
                this.blockState = state;
                this.direction = direction;

                // Start the block breaking animation
                LevelEventPacket startBreak = new LevelEventPacket();
                startBreak.setType(LevelEvent.BLOCK_START_BREAK);
                startBreak.setPosition(blockPosition.toFloat());
                startBreak.setData((int) (65535 / BlockUtils.getTotalTimeLeft(progress)));
                session.sendUpstreamPacket(startBreak);

                // Spawn block breaking particles
                spawnBlockBreakParticles(session, direction, blockPosition, state);
                sendBlockAction(PlayerAction.START_DIGGING, direction);

                // TODO handle fire???????????
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public void continueDestroying(Vector3i blockPosition, Direction direction, long tick, boolean bedrockDestroyed) {
        if (this.destroyDelay > 0) {
            this.destroyDelay--;
            // Add delay to match Java's block destroy speed
            stopBedrockBreaking(session, blockPosition.toFloat());
            BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, blockPosition);
            return;
        }

        if (session.getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = 5;
            this.position = blockPosition;
            this.itemStack = session.getPlayerInventory().getItemInHand();
            this.blockState = session.getGeyser().getWorldManager().blockAt(session, blockPosition);
            sendBlockAction(PlayerAction.START_DIGGING, direction);

            if (canDestroyBlock(blockState)) {
                spawnBlockBreakParticles(session, direction, position, blockState);

                LevelEventPacket effectPacket = new LevelEventPacket();
                effectPacket.setPosition(position.toFloat());
                effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                effectPacket.setData(session.getBlockMappings().getBedrockBlockId(blockState.javaId()));
                session.sendUpstreamPacket(effectPacket);
            } else {
                stopBedrockBreaking(session, position.toFloat());
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, position);
            }

            clearVariables();
        } else if (Objects.equals(this.position, blockPosition)) {
            final float currentProgress = BlockUtils.getBlockMiningProgressPerTick(session, this.blockState.block(), itemStack);
            this.progress = this.progress + currentProgress;
            if (this.progress >= 1.0F) {
                if (canDestroyBlock(blockState)) {
                    spawnBlockBreakParticles(session, direction, position, blockState);

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(position.toFloat());
                    effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                    effectPacket.setData(session.getBlockMappings().getBedrockBlockId(blockState.javaId()));
                    session.sendUpstreamPacket(effectPacket);
                } else {
                    stopBedrockBreaking(session, position.toFloat());
                    BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, position);
                }

                this.isDestroying = false;
                sendBlockAction(PlayerAction.FINISH_DIGGING, direction);

                clearVariables();
                return;
            }

            // Prevent the Bedrock client from destroying blocks quicker than Java allows.
            if (bedrockDestroyed) {
                // TODO test
                stopBedrockBreaking(session, blockPosition.toFloat());
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, blockPosition);
            }

            LevelEventPacket updateBreak = new LevelEventPacket();
            updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            updateBreak.setPosition(blockPosition.toFloat());
            updateBreak.setData((int) (65535 / BlockUtils.getTotalTimeLeft(currentProgress)));
            session.sendUpstreamPacket(updateBreak);

            spawnBlockBreakParticles(session, direction, blockPosition, blockState);
        } else {
            // Add check here for previous block - is that gone????
            if (isDestroying) {
                // TODO
                GeyserImpl.getInstance().getLogger().info("STILL DESTROYING!!!");
            }
            BlockState state = session.getGeyser().getWorldManager().blockAt(session, blockPosition);
            startDestroying(state, blockPosition, direction, tick);
        }
    }

    public void abortBlockBreaking(Vector3i blockPosition) {
        if (session.getGameMode() != GameMode.CREATIVE) {
            // As of 1.16.210: item frame items are taken out here.
            // Survival also sends START_BREAK, but by attaching our process here adventure mode also works
            Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, blockPosition);
            if (itemFrameEntity != null) {
                ServerboundInteractPacket interactPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                    InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamGamePacket(interactPacket);
                return;
            }
        }

        stopBedrockBreaking(session, blockPosition.toFloat());

        // Bedrock "confirms" that it stopped breaking blocks by sending an abort packet after breaking the block
        if (position != null && isDestroying) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, blockPosition, Direction.DOWN, 0);
            GeyserImpl.getInstance().getLogger().info(abortBreakingPacket.toString());
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        clearVariables();
    }

    public boolean canDestroyBlock(BlockState state) {
        boolean creative = session.getGameMode() != GameMode.CREATIVE;
        ToolData toolData = itemStack.getComponent(DataComponentTypes.TOOL);
        if (toolData != null && !toolData.isCanDestroyBlocksInCreative()) {
            if (creative) {
                return false;
            }
        }

        if (GAME_MASTER_BLOCKS.contains(state.block())) {
            if (!creative || session.getOpPermissionLevel() < 2) {
                return false;
            }
        }

        return !state.is(Blocks.AIR);
    }

    public static boolean restrictedBlockActions(GeyserSession session) {
        if (session.isHandsBusy() || !session.getWorldBorder().isInsideBorderBoundaries()) {
            return true;
        }

        GameMode gameMode = session.getGameMode();
        if (gameMode != GameMode.ADVENTURE) {
            // Spectator mode cannot break blocks
            return gameMode == GameMode.SPECTATOR;
        }

        // Vanilla includes a mayBuild check here; which seems duplicate?
        GeyserItemStack stack = session.getPlayerInventory().getItemInHand();
        if (!stack.isEmpty()) {
            AdventureModePredicate canBreak = stack.getComponent(DataComponentTypes.CAN_BREAK);
            if (canBreak != null) {
                for (var predicate : canBreak.getPredicates()) {
                    // TODO
                }
            }
        }

        return false;
    }

    private void clearVariables() {
        this.isDestroying = false;
        this.blockState = null;
        this.position = null;
        this.direction = null;
        this.progress = 0.0F;
        this.itemStack = GeyserItemStack.EMPTY;
    }

    private void sendBlockAction(PlayerAction action, Direction direction) {
        var packet = new ServerboundPlayerActionPacket(action, position,
            direction, session.getWorldCache().nextPredictionSequence());
        GeyserImpl.getInstance().getLogger().info(packet.toString());
        session.sendDownstreamGamePacket(packet);
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

    public static void stopBedrockBreaking(GeyserSession session, Vector3f vector) {
        LevelEventPacket stopBreak = new LevelEventPacket();
        stopBreak.setType(LevelEvent.BLOCK_STOP_BREAK);
        stopBreak.setPosition(vector.toFloat());
        stopBreak.setData(0);
        session.sendUpstreamPacket(stopBreak);
    }
}
