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

package org.geysermc.geyser.session.cache;

import java.util.Objects;
import java.util.Set;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

public class JavaBlockBreakHandler extends BlockBreakHandler {

    private boolean isDestroying = false;
    private int destroyDelay = 0;
    private Direction direction = null;
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

    public JavaBlockBreakHandler(GeyserSession session) {
        super(session);
    }

    @Override
    public void handleBlockBreaking(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS) &&
            !packet.getPlayerActions().isEmpty()) {
            handleBlockBreakActions(packet);
        } else {
            tick(packet.getTick());
        }

        restoredBlocks.clear();
    }

    public void tick(long tick) {
        if (isDestroying) {
            continueDestroying(currentBlock, direction, tick, false);
        } else if (destroyDelay > 0) {
            destroyDelay--;
        }
    }

    @Override
    protected void startBreaking(Vector3i vector, int blockFace, long tick) {
        // TODO add component check
        if (!isDestroying || !Objects.equals(currentBlock, vector)) {
            BlockState state = session.getGeyser().getWorldManager().blockAt(session, vector);

            if (this.isDestroying) {
                // TODO
                //stopBedrockBreaking(session, vector.toFloat());
                //sendBlockAction(PlayerAction.CANCEL_DIGGING, direction);
            }

            float progress = BlockUtils.getBlockMiningProgressPerTick(session, state.block(), session.getPlayerInventory().getItemInHand());
            // instamining, yippie
            if (progress >= 1.0F) {
                if (canDestroyBlock(state)) {
                    // Block can be mined instantly!
                    // TODO destroy progress check???
                    spawnBlockBreakParticles(session, direction, position, state);

                    sendBlockAction(PlayerAction.START_DIGGING, direction);

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(vector.toFloat());
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
                this.currentBlock = blockPosition;
                this.itemStack = session.getPlayerInventory().getItemInHand();
                this.progress = 0.0F;
                this.currentState = state;
                this.direction = direction;

                // Start the block breaking animation
                LevelEventPacket startBreak = new LevelEventPacket();
                startBreak.setType(LevelEvent.BLOCK_START_BREAK);
                startBreak.setPosition(vector.toFloat());
                startBreak.setData((int) (65535 / BlockUtils.getTotalTimeLeft(progress)));
                session.sendUpstreamPacket(startBreak);

                // Spawn block breaking particles
                spawnBlockBreakParticles(session, direction, blockPosition, state);
                sendBlockAction(PlayerAction.START_DIGGING, direction);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    protected boolean canContinueBreaking(Vector3i vector) {
        if (this.destroyDelay > 0) {
            this.destroyDelay--;
            return false;
        }

        return super.canContinueBreaking(vector);
    }

    @Override
    protected void handleContinueBreaking(Vector3i vector, int blockFace, long tick) {
        this.continueDestroying(vector, direction, tick, false);
    }

    @Override
    protected void handleBlockBreaking(Vector3i vector, int blockFace, long tick) {
        this.continueDestroying(vector, direction, tick, true);
    }

    public void continueDestroying(Vector3i blockPosition, Direction direction, long tick, boolean bedrockDestroyed) {
        if (session.getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = 5;
            this.currentBlock = blockPosition;
            this.itemStack = session.getPlayerInventory().getItemInHand();
            this.currentState = session.getGeyser().getWorldManager().blockAt(session, blockPosition);
            sendBlockAction(PlayerAction.START_DIGGING, direction);

            if (canDestroyBlock(currentState)) {
                BlockUtils.spawnBlockBreakParticles(session, direction, currentBlock, currentState);

                LevelEventPacket effectPacket = new LevelEventPacket();
                effectPacket.setPosition(currentBlock.toFloat());
                effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                effectPacket.setData(session.getBlockMappings().getBedrockBlockId(currentState.javaId()));
                session.sendUpstreamPacket(effectPacket);
            } else {
                stopBedrockBreaking(session, position.toFloat());
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, position);
            }

            clearVariables();
        } else if (Objects.equals(this.currentBlock, blockPosition)) {
            final float currentProgress = BlockUtils.getBlockMiningProgressPerTick(session, this.currentState.block(), itemStack);
            this.progress = this.progress + currentProgress;
            if (this.progress >= 1.0F) {
                if (canDestroyBlock(currentState)) {
                    BlockUtils.spawnBlockBreakParticles(session, direction, currentBlock, currentState);

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(currentBlock.toFloat());
                    effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                    effectPacket.setData(session.getBlockMappings().getBedrockBlockId(currentState.javaId()));
                    session.sendUpstreamPacket(effectPacket);
                } else {
                    stopBedrockBreaking(session, currentBlock.toFloat());
                    BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, currentBlock);
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

    @Override
    protected void handleAbortBreaking(Vector3i vector) {
        super.handleAbortBreaking(vector);
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
        this.currentBlock = null;
        this.currentState = null;
        this.direction = null;
        this.progress = 0.0F;
        this.itemStack = GeyserItemStack.EMPTY;
    }

    private void sendBlockAction(PlayerAction action, Direction direction) {
        var packet = new ServerboundPlayerActionPacket(action, currentBlock,
            direction, session.getWorldCache().nextPredictionSequence());
        GeyserImpl.getInstance().getLogger().info(packet.toString());
        session.sendDownstreamGamePacket(packet);
    }
}
