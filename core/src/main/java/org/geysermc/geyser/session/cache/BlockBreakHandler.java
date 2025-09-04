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

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.Pair;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
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
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

public class BlockBreakHandler {

    protected final GeyserSession session;
    protected int currentTickBlockState = 0;
    protected Vector3i currentBlock = Vector3i.ZERO;
    protected BlockState currentState = Blocks.AIR.defaultBlockState();
    protected long blockStartBreakTime = 0;

    // To prevent sending multiple updates, cache all blocks we had to restore
    // e.g. due to out-of-range or being unable to mine
    protected Set<Vector3i> restoredBlocks = new HashSet<>();
    protected Set<Vector3i> instaBreakBlocks = new HashSet<>();

    @Getter
    private final Cache<Vector3i, Pair<Integer, BlockBreakStage>> destructionStageCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    public BlockBreakHandler(final GeyserSession session) {
        this.session = session;
    }

    public void handleBlockBreaking(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            handleBlockBreakActions(packet);
        }
        restoredBlocks.clear();
        instaBreakBlocks.clear();
        currentTickBlockState = 0;
    }

    public void handleBlockBreakActions(PlayerAuthInputPacket packet) {
        boolean creativeMode = session.getGameMode() == GameMode.CREATIVE;

        for (var actionData : packet.getPlayerActions()) {
            Vector3i vector = actionData.getBlockPosition();
            int blockFace = actionData.getFace();

            switch (actionData.getAction()) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        vector, Direction.VALUES[blockFace], 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                case START_BREAK -> {
                    if (creativeMode) {
                        continue;
                    }

                    // TODO test if this is actually a good idea
                    if (!restoredBlocks.isEmpty() || !canStartBreaking(vector)) {
                        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
                        restoredBlocks.add(vector);
                        continue;
                    }

                    startBreaking(vector, blockFace, packet.getTick());
                }
                case BLOCK_CONTINUE_DESTROY -> {
                    if (creativeMode || restoredBlocks.contains(vector) || instaBreakBlocks.contains(vector)) {
                        continue;
                    }
                    
                    if (!restoredBlocks.isEmpty() || !canContinueBreaking(vector)) {
                        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
                        restoredBlocks.add(vector);
                        continue;
                    }

                    handleContinueBreaking(vector, blockFace, packet.getTick());
                }
                case BLOCK_PREDICT_DESTROY -> {
                    if (creativeMode) {
                        continue;
                    }

                    // TODO properly reset vars
                    if (instaBreakBlocks.contains(vector)) {
                        continue;
                    }

                    if (!restoredBlocks.isEmpty() && restoredBlocks.contains(vector)) {
                        BlockUtils.restoreCorrectBlock(session, vector);
                        continue;
                    }

                    Preconditions.checkArgument(vector == currentBlock);

                    if (!canContinueBreaking(vector)) {
                        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
                        BlockUtils.restoreCorrectBlock(session, vector);
                        restoredBlocks.add(vector);
                        continue;
                    }

                    handleBlockBreaking(vector, blockFace, packet.getTick());
                }
                case ABORT_BREAK -> handleAbortBreaking(vector);
                default -> {
                    throw new IllegalStateException("Unknown action: " + actionData.getAction());
                }
            }
        }
    }

    protected void handleAbortBreaking(Vector3i vector) {
        if (session.getGameMode() != GameMode.CREATIVE) {
            if (testForItemFrameEntity(vector)) {
                return;
            }
        }

        // Bedrock edition "confirms" it stopped breaking blocks by sending
        if (currentBlock != null) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, vector, Direction.DOWN, 0);
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
    }

    protected void startBreaking(Vector3i vector, int blockFace, long tick) {
        int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
        float breakProgress = BlockUtils.getBlockMiningProgressPerTick(session, BlockState.of(blockState).block(), session.getPlayerInventory().getItemInHand());
        double breakTime = calculateBlockBreakTime(blockState, vector, breakProgress);

        // insta-breaking should be treated differently; don't send STOP_BREAK for these
        if (breakProgress > 1) {
            // Avoids sending STOP_BREAK for instantly broken blocks
            instaBreakBlocks.add(vector);
        } else {
            // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
            GeyserItemStack item = session.getPlayerInventory().getItemInHand();
            ItemMapping mapping = item.getMapping(session);
            ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getComponents(), mapping) : null;
            CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(blockState);
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(vector);

            this.blockStartBreakTime = 0;
            if (BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(blockState) || blockStateOverride != null || customItem != null || (skull != null && skull.getBlockDefinition() != null)) {
                this.blockStartBreakTime = System.currentTimeMillis();
            }

            LevelEventPacket startBreak = new LevelEventPacket();
            startBreak.setType(LevelEvent.BLOCK_START_BREAK);
            startBreak.setPosition(vector.toFloat());
            startBreak.setData((int) (65535 / breakTime));
            session.sendUpstreamPacket(startBreak);

            this.currentBlock = vector;
            this.currentState = BlockState.of(blockState);
        }

        // Account for fire - the client likes to hit the block behind.
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(vector, blockFace);
        Block block = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        Direction direction = Direction.VALUES[blockFace];
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                direction, session.getWorldCache().nextPredictionSequence());
            session.sendDownstreamGamePacket(startBreakingPacket);
        }

        session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
            vector, direction, session.getWorldCache().nextPredictionSequence()));
        BlockUtils.spawnBlockBreakParticles(session, direction, vector, currentState);
    }

    protected boolean canStartBreaking(Vector3i vector) {
        if (session.isHandsBusy() || !session.getWorldBorder().isInsideBorderBoundaries()) {
            return false;
        }

        Vector3f playerPosition = session.getPlayerEntity().getPosition();
        playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
        return BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector);
    }

    protected void handleContinueBreaking(Vector3i vector, int blockFace, long tick) {
        // quick hack to avoid multiple lookups in the same tick
        if (currentTickBlockState == 0) {
            int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
            if (blockState != this.currentState.javaId()) {
                throw new IllegalStateException("TODO check java client!");
            }
        }

        Direction direction = Direction.VALUES[blockFace];

        BlockUtils.spawnBlockBreakParticles(session, direction, vector, currentState);
        double breakTime = BlockUtils.getSessionBreakTimeTicks(session, currentState.block(), 0);

        if (blockStartBreakTime != 0) {
            long timeSinceStart = System.currentTimeMillis() - blockStartBreakTime;
            // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
            if (timeSinceStart >= (breakTime += 2) * 50) {
                // Play break sound and particle
                LevelEventPacket effectPacket = new LevelEventPacket();
                effectPacket.setPosition(vector.toFloat());
                effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                effectPacket.setData(session.getBlockMappings().getBedrockBlockId(currentState.javaId()));
                session.sendUpstreamPacket(effectPacket);

                // Break the block
                ServerboundPlayerActionPacket finishBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
                    vector, direction, session.getWorldCache().nextPredictionSequence());
                session.sendDownstreamGamePacket(finishBreakingPacket);
                blockStartBreakTime = 0;
                return;
            }
        }

        // Update the break time in the event that player conditions changed (jumping, effects applied)
        LevelEventPacket updateBreak = new LevelEventPacket();
        updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
        updateBreak.setPosition(vector.toFloat());
        updateBreak.setData((int) (65535 / breakTime));
        session.sendUpstreamPacket(updateBreak);
    }

    protected void handleBlockBreaking(Vector3i vector, int blockFace, long tick) {
        // TODO creative will ONLY "hit" here

        // TODO if creative mode is used here; ONLY send START_BREAK
        int sequence = session.getWorldCache().nextPredictionSequence();
        session.getWorldCache().markPositionInSequence(vector);


        PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
        ServerboundPlayerActionPacket breakPacket = new ServerboundPlayerActionPacket(action, vector, Direction.VALUES[blockFace], sequence);

        BlockUtils.sendBedrockBlockDestroy(session, vector.toFloat(), currentState.javaId());
    }

    private boolean testForItemFrameEntity(Vector3i position) {
        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
        if (itemFrameEntity != null) {
            ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                InteractAction.ATTACK, session.isSneaking());
            session.sendDownstreamGamePacket(attackPacket);
            return true;
        }
        return false;
    }

    protected boolean canContinueBreaking(Vector3i vector) {
        if (session.isHandsBusy() || !session.getWorldBorder().isInsideBorderBoundaries()) {
            return false;
        }

        Vector3f playerPosition = session.getPlayerEntity().getPosition();
        playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
        return BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector);
    }

    protected double calculateBlockBreakTime(int state, Vector3i vector, float progress) {
        return BlockUtils.getSessionBreakTimeTicks(session, BlockState.of(state).block(), progress);
    }

    protected void clearCurrentVariables() {
        this.currentBlock = null;
        this.currentState = null;
        this.blockStartBreakTime = 0L;
    }
}
