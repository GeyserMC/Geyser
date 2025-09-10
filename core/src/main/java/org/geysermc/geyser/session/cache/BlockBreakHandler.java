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
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
     * Only set when keeping track of custom blocks / custom items breaking blocks
     */
    protected long blockStartBreakTime = 0;
    /**
     * The last block position that was instantly broken.
     * Used to ignore subsequent interactions that we don't need to send or confirm
     */
    protected Vector3i lastInstaMinedPosition = null;

    // To prevent sending multiple updates, cache all blocks we had to restore
    // e.g. due to out-of-range or being unable to mine
    protected Set<Vector3i> restoredBlocks = new HashSet<>(2);
    /**
     * Used to ignore block interactions of subsequent item frame interactions
     */
    protected @Nullable Vector3i itemFramePos = null;

    @Getter
    private final Cache<Vector3i, Pair<Long, BlockBreakStage>> destructionStageCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    private final BlockPredicateCache blockPredicateCache = new BlockPredicateCache();

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

    public void handlePlayerAuthInputPacket(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            handleBlockBreakActions(packet);
        }
        restoredBlocks.clear();
    }

    public void handleBlockBreakActions(PlayerAuthInputPacket packet) {
        boolean instabuild = session.isInstabuild();
        this.itemFramePos = null;

        for (int i = 0; i < packet.getPlayerActions().size(); i++) {
            PlayerBlockActionData actionData = packet.getPlayerActions().get(i);
            Vector3i position = actionData.getBlockPosition();
            int blockFace = actionData.getFace();

            switch (actionData.getAction()) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        position, Direction.VALUES[blockFace], 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                // Must do this ugly as it can also be called in block_continue_destroy :(
                case START_BREAK -> preStartBreakHandle(position, blockFace, packet.getTick());
                case BLOCK_CONTINUE_DESTROY -> {
                    if (testForLastInstaBreakPosOrReset(position) || restoredBlocks.contains(position)) {
                        continue;
                    }

                    // We start breaking a new block! Bedrock won't send START_BREAK for continually broken blocks.
                    // The currentblockstate should never be null here; but better be safe than sorry.
                    // Never trust the Bedrock client, so, better be safe than sorry.
                    if (!Objects.equals(position, currentBlockPos) || currentBlockState == null) {
                        if (currentBlockPos != null) {
                            abortBreaking(currentBlockPos);
                        }
                        preStartBreakHandle(position, blockFace, packet.getTick());
                        continue;
                    }

                    // The client loves to send this case alongside BLOCK_PREDICT_DESTROY in the same packet;
                    // we can skip handling this action if the same pos is updated again in the same tick
                    if (i < packet.getPlayerActions().size() - 1) {
                        var nextAction = packet.getPlayerActions().get(i + 1);
                        if (Objects.equals(nextAction.getBlockPosition(), position)) {
                            continue;
                        }
                    }

                    if (!restoredBlocks.isEmpty() || !canBreak(position)) {
                        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
                        restoredBlocks.add(position);
                        continue;
                    }

                    handleContinueDestroy(position, blockFace, packet.getTick());
                }
                case BLOCK_PREDICT_DESTROY -> {
                    // If a block was instantly broken in one tick, only START_BREAK is sent to the Java client
                    if (testForLastInstaBreakPosOrReset(position)) {
                        continue;
                    }

                    if (!restoredBlocks.isEmpty()) {
                        BlockUtils.restoreCorrectBlock(session, position);
                        continue;
                    }

                    boolean valid = currentBlockState != null && Objects.equals(position, currentBlockPos);
                    if (!canBreak(position) || !valid) {
                        if (!valid) {
                            GeyserImpl.getInstance().getLogger().warning("Player %s tried to break block at %s (%s), without starting to destroy it!"
                                .formatted(session.bedrockUsername(), position, currentBlockState));
                        }
                        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
                        BlockUtils.restoreCorrectBlock(session, position);
                        restoredBlocks.add(position);
                        continue;
                    }

                    handlePredictDestroy(position, blockFace, packet.getTick());
                }
                case ABORT_BREAK -> {
                    // The Bedrock client seems to not have broken this block
                    // But it has been!!!
                    if (Objects.equals(lastInstaMinedPosition, position)) {
                        lastInstaMinedPosition = null;
                        continue;
                    }

                    // Triggered in adventure mode
                    if (testForItemFrameEntity(position)) {
                        continue;
                    }

                    abortBreaking(position);
                }
                default -> {
                    throw new IllegalStateException("Unknown block break action: " + actionData.getAction());
                }
            }
        }
    }

    private void preStartBreakHandle(Vector3i position, int blockFace, long tick) {
        // New block being broken -> ignore previous insta-mine pos
        lastInstaMinedPosition = null;

        if (testForItemFrameEntity(position)) {
            return;
        }

        if (!restoredBlocks.isEmpty() || !canBreak(position)) {
            BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
            restoredBlocks.add(position);
            return;
        }

        handleStartBreak(position, blockFace, tick);
    }

    protected void handleStartBreak(Vector3i position, int blockFace, long tick) {
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);
        GeyserItemStack item = session.getPlayerInventory().getItemInHand();
        Direction direction = Direction.VALUES[blockFace];

        // Account for fire - the client likes to hit the block behind.
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(position, blockFace);
        Block block = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                direction, session.getWorldCache().nextPredictionSequence());
            session.sendDownstreamGamePacket(startBreakingPacket);
        }

        // % block breaking progress in this tick
        float breakProgress = calculateBreakProgress(state, position, item);

        // insta-breaking should be treated differently; don't send STOP_BREAK for these
        if (session.isInstabuild() || breakProgress >= 1.0F) {
            // Avoid sending STOP_BREAK for instantly broken blocks
            lastInstaMinedPosition = position;
            destroyBlock(state, position, direction, true);
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

            BlockUtils.spawnBlockBreakParticles(session, direction, position, state);

            this.currentBlockPos = position;
            this.currentBlockState = state;

            session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position, direction, session.getWorldCache().nextPredictionSequence()));
        }
    }

    protected void handleContinueDestroy(Vector3i position, int blockFace, long tick) {
        Objects.requireNonNull(currentBlockState, "currentBlockState");
        Direction direction = Direction.VALUES[blockFace];
        BlockUtils.spawnBlockBreakParticles(session, direction, position, currentBlockState);
        double totalBreakTime = BlockUtils.reciprocal(calculateBreakProgress(currentBlockState, position, session.getPlayerInventory().getItemInHand()));

        if (blockStartBreakTime != 0) {
            long ticksSinceStart = tick - blockStartBreakTime;
            // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
            if (ticksSinceStart >= (totalBreakTime += 2)) {
                destroyBlock(currentBlockState, position, direction, false);
                return;
            }
        }

        // Update the break time in the event that player conditions changed (jumping, effects applied)
        LevelEventPacket updateBreak = new LevelEventPacket();
        updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
        updateBreak.setPosition(position.toFloat());
        updateBreak.setData((int) (65535 / totalBreakTime));
        session.sendUpstreamPacket(updateBreak);
    }

    protected void handlePredictDestroy(Vector3i position, int blockFace, long tick) {
        Objects.requireNonNull(currentBlockState, "currentBlockState");
        destroyBlock(currentBlockState, position, Direction.VALUES[blockFace], false);
    }

    protected void abortBreaking(Vector3i position) {
        // Bedrock edition "confirms" it stopped breaking blocks by sending an abort packet
        // We don't forward those as a Java client wouldn't send those either
        if (currentBlockPos != null) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, currentBlockPos, Direction.DOWN, 0);
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
    }

    protected boolean testForItemFrameEntity(Vector3i position) {
        if (itemFramePos != null && itemFramePos.equals(position)) {
            return true;
        }

        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
        if (itemFrameEntity != null) {
            ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                InteractAction.ATTACK, session.isSneaking());
            session.sendDownstreamGamePacket(attackPacket);
            itemFramePos = position;
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canBreak(Vector3i vector) {
        if (session.isHandsBusy() || !session.getWorldBorder().isInsideBorderBoundaries()) {
            return false;
        }

        switch (session.getGameMode()) {
            case SPECTATOR -> {
                return false;
            }
            case ADVENTURE -> {
                if (/*!blockPredicateCache.calculatePredicate(session, state, session.getPlayerInventory().getItemInHand())*/ false) {
                    return false;
                }
            }
        }

        Vector3f playerPosition = session.getPlayerEntity().getPosition();
        playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
        return BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector);
    }

    protected boolean canDestroyBlock(BlockState state) {
        boolean instabuild = session.isInstabuild();
        if (instabuild) {
            ToolData data = session.getPlayerInventory().getItemInHand().getComponent(DataComponentTypes.TOOL);
            if (data != null && !data.isCanDestroyBlocksInCreative()) {
                return false;
            }
        }

        if (GAME_MASTER_BLOCKS.contains(state.block())) {
            if (!instabuild || session.getOpPermissionLevel() < 2) {
                return false;
            }
        }

        return !state.is(Blocks.AIR);
    }

    protected void destroyBlock(BlockState state, Vector3i vector, Direction direction, boolean instamine) {
        // Send java packet
        session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(instamine ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING,
            vector, direction, session.getWorldCache().nextPredictionSequence()));
        session.getWorldCache().markPositionInSequence(vector);

        if (canDestroyBlock(state)) {
            BlockUtils.spawnBlockBreakParticles(session, direction, vector, state);
            BlockUtils.sendBedrockBlockDestroy(session, vector.toFloat(), state.javaId());
        } else {
            BlockUtils.restoreCorrectBlock(session, vector, state);
        }
        clearCurrentVariables();
    }

    protected float calculateBreakProgress(BlockState state, Vector3i vector, GeyserItemStack stack) {
        return BlockUtils.getBlockMiningProgressPerTick(session, state.block(), stack);
    }

    // Ignore all insta-break actions while they're on the same instant-mine position, reset otherwise
    protected boolean testForLastInstaBreakPosOrReset(Vector3i position) {
        if (Objects.equals(lastInstaMinedPosition, position)) {
            return true;
        }
        lastInstaMinedPosition = null;
        return false;
    }

    protected void clearCurrentVariables() {
        this.currentBlockPos = null;
        this.currentBlockState = null;
        this.blockStartBreakTime = 0L;
    }

    public void reset() {
        clearCurrentVariables();
        this.lastInstaMinedPosition = null;
        this.destructionStageCache.invalidateAll();
    }

    private static class BlockPredicateCache {
        private BlockState lastBlockState;
        private GeyserItemStack lastItemStack;
        private Boolean lastResult;

        private boolean calculatePredicate(GeyserSession session, BlockState state, GeyserItemStack stack) {
            // An empty stack will never pass
            if (stack.isEmpty()) {
                return false;
            }

            AdventureModePredicate canBreak = stack.getComponent(DataComponentTypes.CAN_BREAK);
            if (canBreak == null) { // Neither will a stack without can_break
                return false;
            } else if (state.equals(lastBlockState) && stack.equals(lastItemStack) && lastResult != null) { // Check lastResult just in case.
                return lastResult;
            }

            lastBlockState = state;
            lastItemStack = stack;

            // Any of the predicates have to match for the stack to match
            for (AdventureModePredicate.BlockPredicate predicate : canBreak.getPredicates()) {
                if (BlockUtils.blockMatchesPredicate(session, state, predicate)) {
                    lastResult = true;
                    return true;
                }
            }
            lastResult = false;
            return false;
        }
    }
}
