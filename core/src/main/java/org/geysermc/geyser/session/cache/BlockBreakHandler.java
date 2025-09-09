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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.Pair;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
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

/**
 * Class responsible for block breaking handling. This is designed to be extensible
 * by extensions (not officially supported!).
 */
public class BlockBreakHandler {
    protected final GeyserSession session;
    /**
     * The position of the current block being broken
     */
    protected @Nullable Vector3i currentBlockPos = null;
    /**
     * The current block state that is being broken
     */
    protected @Nullable BlockState currentBlockState = null;
    /**
     * The tick in which block breaking of the current block began
     */
    protected long blockStartBreakTime = 0;
    /**
     * The last block position that was instantly broken.
     * Used to ignore subsequent interactions that we don't need to send or confirm
     */
    protected Vector3i lastInstaMinedPosition = null;

    // To prevent sending multiple updates, cache all blocks we had to restore
    // e.g. due to out-of-range or being unable to mine
    // TODO can this actually be more than one block??
    protected Set<Vector3i> restoredBlocks = new HashSet<>(2);

    @Getter
    private final Cache<Vector3i, Pair<Long, BlockBreakStage>> destructionStageCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    public BlockBreakHandler(final GeyserSession session) {
        this.session = session;
    }

    public void handleBlockBreaking(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            handleBlockBreakActions(packet);
        }
        restoredBlocks.clear();
    }

    public void handleBlockBreakActions(PlayerAuthInputPacket packet) {
        boolean creativeMode = session.getGameMode() == GameMode.CREATIVE;
        Vector3i itemFramePos = null;

        for (int i = 0; i < packet.getPlayerActions().size(); i++) {
            PlayerBlockActionData actionData = packet.getPlayerActions().get(i);
            Vector3i vector = actionData.getBlockPosition();
            int blockFace = actionData.getFace();

            GeyserImpl.getInstance().getLogger().info(session.getClientTicks() + " " + i + " " + actionData.toString());

            switch (actionData.getAction()) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        vector, Direction.VALUES[blockFace], 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                case START_BREAK -> {
                    // New block broken -> ignore previous insta-mine pos
                    lastInstaMinedPosition = null;

                    // TODO should we handle breaking here instead???
                    if (creativeMode) {
                        continue;
                    }

                    if (testForItemFrameEntity(vector)) {
                        itemFramePos = vector;
                        continue;
                    }

                    if (!restoredBlocks.isEmpty() || !canStartBreaking(vector)) {
                        GeyserImpl.getInstance().getLogger().info("continue due to restored / not able to break");
                        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
                        restoredBlocks.add(vector);
                        continue;
                    }

                    startBreaking(vector, blockFace, packet.getTick());
                }
                case BLOCK_CONTINUE_DESTROY -> {
                    // does creative mode *ever* send these???
                    if (creativeMode || restoredBlocks.contains(vector) || testForLastInstaBreakPosOrReset(vector)) {
                        GeyserImpl.getInstance().getLogger().info("continue ignored due to continue issue");
                        continue;
                    }

                    // The client loves to send this alongside BLOCK_PREDICT_DESTROY
                    // no need to handle this if the same pos is getting destroyed in the same tick
                    if (i < packet.getPlayerActions().size() - 1) {
                        var nextAction = packet.getPlayerActions().get(i + 1);
                        if (Objects.equals(nextAction.getBlockPosition(), vector)) {
                            GeyserImpl.getInstance().getLogger().info("Ignoring action " + actionData);
                            continue;
                        }
                    }

                    if (!restoredBlocks.isEmpty() || !canContinueBreaking(vector)) {
                        GeyserImpl.getInstance().getLogger().info("adfa");
                        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
                        restoredBlocks.add(vector);
                        continue;
                    }

                    // We start breaking a new block, actually
                    // Current block state can ONLY be null if either we didn't get a start break; that should be impossible
                    // without a position change.
                    // The second case would be that we don't store it anymore due to instantly breaking that block, but, that should
                    // have been caught above.
                    // Never trust the Bedrock client, so, better be safe than sorry.
                    // TODO are item frames an issue here?
                    if (Objects.equals(vector, currentBlockPos) && currentBlockState != null) {
                        handleContinueBreaking(vector, blockFace, packet.getTick());
                    } else {
                        GeyserImpl.getInstance().getLogger().error("AAAAAAA! Not same!!!");
                        if (currentBlockPos != null) {
                            GeyserImpl.getInstance().getLogger().error("ABORTING CURRENT!");
                            handleAbortBreaking(currentBlockPos);
                        }
                        startBreaking(vector, blockFace, packet.getTick());
                    }
                }
                case BLOCK_PREDICT_DESTROY -> {
                    // If a block was instantly broken in one tick, only START_BREAK is sent
                    // TODO does bedrock always send this case?
                    if (testForLastInstaBreakPosOrReset(vector)) {
                        GeyserImpl.getInstance().getLogger().info("Ignoring action due to insta " + actionData);
                        continue;
                    }

                    if (!restoredBlocks.isEmpty() && restoredBlocks.contains(vector)) {
                        BlockUtils.restoreCorrectBlock(session, vector);
                        continue;
                    }

                    if (!canContinueBreaking(vector)) {
                        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
                        BlockUtils.restoreCorrectBlock(session, vector);
                        restoredBlocks.add(vector);
                        continue;
                    }

                    if (currentBlockState == null || !Objects.equals(vector, currentBlockPos)) {
                        throw new IllegalStateException("Attempting to break block %s(%s), but desync (us: %s)!".formatted(vector, currentBlockState, currentBlockPos));
                    }

                    handleBlockBreaking(vector, blockFace, packet.getTick());
                }
                case ABORT_BREAK -> {
                    // The Bedrock client seems to not have broken this block
                    // TODO
                    if (testForLastInstaBreakPosOrReset(vector)) {
                        GeyserImpl.getInstance().getLogger().error("HAHAHAHAHHA");
                        BlockUtils.restoreCorrectBlock(session, vector);
                    }

                    // TODO which game mode should this actually apply to?
                    if (session.getGameMode() != GameMode.CREATIVE) {
                        if (Objects.equals(vector, itemFramePos) || testForItemFrameEntity(vector)) {
                            continue;
                        }
                    }

                    handleAbortBreaking(vector);
                }
                default -> {
                    throw new IllegalStateException("Unknown action: " + actionData.getAction());
                }
            }
        }

        GeyserImpl.getInstance().getLogger().info("-----------------------");
    }

    protected void handleAbortBreaking(Vector3i vector) {
        // Bedrock edition "confirms" it stopped breaking blocks by sending an abort packet
        if (currentBlockPos != null) {
            if (!Objects.equals(currentBlockPos, vector)) {
                GeyserImpl.getInstance().getLogger().warning("Got block break desync: Client aborts %s, we are still on %s".formatted(vector, currentBlockPos));
            }

            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, currentBlockPos, Direction.DOWN, 0);
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(session, vector.toFloat());
    }

    protected void startBreaking(Vector3i position, int blockFace, long tick) {
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);
        GeyserItemStack item = session.getPlayerInventory().getItemInHand();
        // % block breaking progress in this tick
        float breakProgress = calculateBreakProgress(state, position, item);
        boolean markInSequence = false;

        // TODO test against BDS, esp insta-breaking
        Direction direction = Direction.VALUES[blockFace];
        BlockUtils.spawnBlockBreakParticles(session, direction, position, state);

        // insta-breaking should be treated differently; don't send STOP_BREAK for these
        if (breakProgress > 1) {
            // Avoid sending STOP_BREAK for instantly broken blocks
            GeyserImpl.getInstance().getLogger().warning("INSTABREAK GOES BRRRRR " + position);
            BlockUtils.sendBedrockBlockDestroy(session, position.toFloat(), state.javaId());
            lastInstaMinedPosition = position;
            markInSequence = true;
            clearVariables();
        } else {
            // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
            ItemMapping mapping = item.getMapping(session);
            ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getComponents(), mapping) : null;
            CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(state.javaId());
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(position);

            this.blockStartBreakTime = 0;
            if (BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(state.javaId()) || blockStateOverride != null || customItem != null || (skull != null && skull.getBlockDefinition() != null)) {
                this.blockStartBreakTime = tick;
            }

            LevelEventPacket startBreak = new LevelEventPacket();
            startBreak.setType(LevelEvent.BLOCK_START_BREAK);
            startBreak.setPosition(position.toFloat());
            startBreak.setData((int) (65535 / BlockUtils.reciprocal(breakProgress)));
            session.sendUpstreamPacket(startBreak);

            this.currentBlockPos = position;
            this.currentBlockState = state;
        }

        // Account for fire - the client likes to hit the block behind.
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(position, blockFace);
        Block block = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                direction, session.getWorldCache().nextPredictionSequence());
            session.sendDownstreamGamePacket(startBreakingPacket);
        }

        int sequence = session.getWorldCache().nextPredictionSequence();
        if (markInSequence) {
            // we broke it, let's keep track of it
            session.getWorldCache().markPositionInSequence(position);
        }
        session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position, direction, sequence));
    }

    protected void handleContinueBreaking(Vector3i vector, int blockFace, long tick) {
        if (currentBlockState == null) {
            throw new IllegalStateException("Current block breaking position is null?");
        }

        Direction direction = Direction.VALUES[blockFace];
        BlockUtils.spawnBlockBreakParticles(session, direction, vector, currentBlockState);
        double totalBreakTime = BlockUtils.getSessionTotalBreakTimeTicks(session, currentBlockState.block());

        if (blockStartBreakTime != 0) {
            long ticksSinceStart = tick - blockStartBreakTime;
            // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
            if (ticksSinceStart >= (totalBreakTime += 2)) {
                // Play break sound and particle
                BlockUtils.sendBedrockBlockDestroy(session, vector.toFloat(), currentBlockState.javaId());

                // Break the block
                ServerboundPlayerActionPacket finishBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
                    vector, direction, session.getWorldCache().nextPredictionSequence());
                session.getWorldCache().markPositionInSequence(vector);
                session.sendDownstreamGamePacket(finishBreakingPacket);

                clearVariables();
                return;
            }
        }

        // Update the break time in the event that player conditions changed (jumping, effects applied)
        LevelEventPacket updateBreak = new LevelEventPacket();
        updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
        updateBreak.setPosition(vector.toFloat());
        updateBreak.setData((int) (65535 / totalBreakTime));
        session.sendUpstreamPacket(updateBreak);
    }

    protected void handleBlockBreaking(Vector3i vector, int blockFace, long tick) {
        if (currentBlockState == null) {
            throw new IllegalStateException("Current block breaking position is null?");
        }

        int sequence = session.getWorldCache().nextPredictionSequence();
        session.getWorldCache().markPositionInSequence(vector);

        PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
        session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(action, vector, Direction.VALUES[blockFace], sequence));

        BlockUtils.sendBedrockBlockDestroy(session, vector.toFloat(), currentBlockState.javaId());
        clearVariables();
    }

    protected boolean testForItemFrameEntity(Vector3i position) {
        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
        if (itemFrameEntity != null) {
            ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                InteractAction.ATTACK, session.isSneaking());
            session.sendDownstreamGamePacket(attackPacket);
            return true;
        }
        return false;
    }

    protected boolean canStartBreaking(Vector3i vector) {
        if (session.isHandsBusy() || !session.getWorldBorder().isInsideBorderBoundaries()) {
            return false;
        }

        Vector3f playerPosition = session.getPlayerEntity().getPosition();
        playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
        return BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canContinueBreaking(Vector3i vector) {
        return canStartBreaking(vector);
    }

    protected float calculateBreakProgress(BlockState state, Vector3i vector, GeyserItemStack stack) {
        return BlockUtils.getBlockMiningProgressPerTick(session, state.block(), stack);
    }

    protected void clearVariables() {
        this.currentBlockPos = null;
        this.currentBlockState = null;
        this.blockStartBreakTime = 0L;
    }

    // Ignore all insta-break actions while they're on the same instant-mine position, reset otherwise
    protected boolean testForLastInstaBreakPosOrReset(Vector3i vector) {
        if (Objects.equals(lastInstaMinedPosition, vector)) {
            return true;
        }
        lastInstaMinedPosition = null;
        return false;
    }

    public void reset() {
        clearVariables();
        this.lastInstaMinedPosition = null;
        this.destructionStageCache.invalidateAll();
    }
}
