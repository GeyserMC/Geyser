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
import org.checkerframework.checker.nullness.qual.NonNull;
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
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.geyser.translator.protocol.java.level.JavaBlockDestructionTranslator;
import org.geysermc.geyser.util.BlockUtils;
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
     * The position of the current block being broken.
     * Null indicates no block breaking in progress.
     */
    protected @Nullable Vector3i currentBlockPos = null;

    /**
     * The current block state that is being broken.
     * Null indicates no block breaking in progress.
     */
    protected @Nullable BlockState currentBlockState = null;

    /**
     * The Bedrock client tick in which block breaking of the current block began.
     * Only set when keeping track of custom blocks / custom items breaking blocks.
     */
    protected long blockStartBreakTime = 0;

    /**
     * The last known face of the block the client was breaking.
     * Only set when keeping track of custom blocks / custom items breaking blocks.
     */
    protected Direction lastBlockBreakFace = null;

    /**
     * The last block position that was instantly broken.
     * Used to ignore subsequent block actions from the Bedrock client.
     */
    protected Vector3i lastInstaMinedPosition = null;

    /**
     * Caches all blocks we had to restore e.g. due to out-of-range or being unable to mine
     * in order to avoid duplicate corrections.
     */
    protected Set<Vector3i> restoredBlocks = new HashSet<>(2);

    /**
     * Used to ignore subsequent block interactions after an item frame interaction
     */
    protected @Nullable Vector3i itemFramePos = null;

    /**
     * See {@link JavaBlockDestructionTranslator} for usage and explanation
     */
    @Getter
    private final Cache<Vector3i, Pair<Long, BlockBreakStage>> destructionStageCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .build();

    /**
     * Used to cache adventure mode can break predicate lookups
     */
    private final BlockPredicateCache blockPredicateCache = new BlockPredicateCache();

    public BlockBreakHandler(final GeyserSession session) {
        this.session = session;
    }

    /**
     * Main entrypoint that handles block breaking actions, if present. Ticks the handler if no breaking actions were performed.
     * @param packet the player auth input packet
     */
    public void handlePlayerAuthInputPacket(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            handleBlockBreakActions(packet);
            restoredBlocks.clear();
            this.itemFramePos = null;
        } else {
            tick();
        }
    }

    protected void tick() {
        // We need to manually check if a block should be destroyed, and send the client progress updates, when mining a custom block, or with a custom item
        // This is because, in CustomItemRegistryPopulator#computeToolProperties, we set a block break speed of 0,
        // meaning the client will only ever send START_BREAK for breaking blocks, and nothing else

        // Check lastBlockBreakFace, currentBlockPos and currentBlockState, just in case
        if (blockStartBreakTime != 0 && lastBlockBreakFace != null && currentBlockPos != null && currentBlockState != null) {
            handleContinueDestroy(currentBlockPos, currentBlockState, lastBlockBreakFace, session.getClientTicks());
        }
    }

    protected void handleBlockBreakActions(PlayerAuthInputPacket packet) {
        for (int i = 0; i < packet.getPlayerActions().size(); i++) {
            PlayerBlockActionData actionData = packet.getPlayerActions().get(i);
            Vector3i position = actionData.getBlockPosition();
            // Worth noting: the bedrock client, as of version  1.21.101, sends weird values for the face, outside the [0;6] range, when sending ABORT_BREAK
            // Not sure why, but, blockFace isn't used for ABORT_BREAK, so it's fine
            // This is why blockFace is individually turned into a Direction in each of the switch statements, except for the ABORT_BREAK one

            switch (actionData.getAction()) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        position, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace).mcpl(), 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                // Must do this ugly as it can also be called from the block_continue_destroy case :(
                case START_BREAK -> preStartBreakHandle(position, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace), packet.getTick());
                case BLOCK_CONTINUE_DESTROY -> {
                    if (testForItemFrameEntity(position) || testForLastInstaBreakPosOrReset(position) || abortDueToBlockRestoring(position)) {
                        continue;
                    }

                    Direction blockFace = Direction.getUntrusted(actionData, PlayerBlockActionData::getFace);
                    // Position mismatch == we break a new block! Bedrock won't send START_BREAK when continuously mining
                    // That applies in creative mode too! (last test in 1.21.100)
                    if (!Objects.equals(position, currentBlockPos) || currentBlockState == null) {
                        if (currentBlockPos != null) {
                            handleAbortBreaking(currentBlockPos);
                        }
                        preStartBreakHandle(position, blockFace, packet.getTick());
                        continue;
                    }

                    // The client loves to send this block action alongside BLOCK_PREDICT_DESTROY in the same packet;
                    // we can skip handling this action if the same position is updated again in the same tick
                    if (i < packet.getPlayerActions().size() - 1) {
                        PlayerBlockActionData nextAction = packet.getPlayerActions().get(i + 1);
                        if (Objects.equals(nextAction.getBlockPosition(), position)) {
                            continue;
                        }
                    }

                    BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);
                    if (!canBreak(position, state)) {
                        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
                        restoredBlocks.add(position);
                        continue;
                    }

                    handleContinueDestroy(position, state, blockFace, packet.getTick());
                }
                case BLOCK_PREDICT_DESTROY -> {
                    if (testForItemFrameEntity(position) || testForLastInstaBreakPosOrReset(position)) {
                        continue;
                    }

                    // Not using abortDueToBlockRestoring method here as we're fully restoring the block,
                    // to counteract Bedrock's own client-side prediction
                    if (!restoredBlocks.isEmpty()) {
                        BlockUtils.restoreCorrectBlock(session, position);
                        continue;
                    }

                    BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);
                    boolean valid = currentBlockState != null && Objects.equals(position, currentBlockPos);
                    if (!canBreak(position, state) || !valid) {
                        if (!valid) {
                            GeyserImpl.getInstance().getLogger().warning("Player %s tried to break block at %s (%s), without starting to destroy it!"
                                .formatted(session.bedrockUsername(), position, currentBlockState));
                        }
                        BlockUtils.stopBreakAndRestoreBlock(session, position, state);
                        restoredBlocks.add(position);
                        continue;
                    }

                    handlePredictDestroy(position, state, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace), packet.getTick());
                }
                case ABORT_BREAK -> {
                    // Abort break can also be sent after the block on that pos was broken.....
                    // At that point it's safe to assume that we won't get subsequent block actions on this position
                    // so reset it and return since there isn't anything to abort
                    if (Objects.equals(lastInstaMinedPosition, position)) {
                        lastInstaMinedPosition = null;
                        continue;
                    }

                    // Also handles item frame interactions in adventure mode
                    if (testForItemFrameEntity(position)) {
                        continue;
                    }

                    handleAbortBreaking(position);
                }
                default -> {
                    GeyserImpl.getInstance().getLogger().warning("Unknown block break action (%s) received! (origin: %s)!"
                        .formatted(actionData.getAction(), session.getDebugInfo()));
                    GeyserImpl.getInstance().getLogger().debug("Odd packet: " + packet);
                    session.disconnect("Invalid block breaking action received!");
                }
            }
        }
    }

    /**
     * Called from either a START_BREAK or BLOCK_CONTINUE_DESTROY case, the latter
     * if the client switches to a new block. This method then runs pre-break checks.
     */
    private void preStartBreakHandle(Vector3i position, Direction blockFace, long tick) {
        // New block being broken -> ignore previous insta-mine pos since that's no longer relevant
        lastInstaMinedPosition = null;

        if (testForItemFrameEntity(position) || abortDueToBlockRestoring(position)) {
            return;
        }

        BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);
        if (!canBreak(position, state)) {
            BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
            restoredBlocks.add(position);
            return;
        }

        handleStartBreak(position, state, blockFace, tick);
    }

    protected void handleStartBreak(@NonNull Vector3i position, @NonNull BlockState state, Direction blockFace, long tick) {
        GeyserItemStack item = session.getPlayerInventory().getItemInHand();

        // Account for fire - the client likes to hit the block behind.
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(position, blockFace);
        Block possibleFireBlock = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        if (possibleFireBlock == Blocks.FIRE || possibleFireBlock == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                blockFace.mcpl(), session.getWorldCache().nextPredictionSequence());
            session.sendDownstreamGamePacket(startBreakingPacket);
        }

        // % block breaking progress in this tick
        float breakProgress = calculateBreakProgress(state, position, item);

        // insta-breaking should be treated differently; don't send STOP_BREAK for these
        if (session.isInstabuild() || breakProgress >= 1.0F) {
            // Avoid sending STOP_BREAK for instantly broken blocks
            lastInstaMinedPosition = position;
            destroyBlock(state, position, blockFace, true);
        } else {
            // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
            ItemMapping mapping = item.getMapping(session);
            ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getComponents(), mapping) : null;
            CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(state.javaId());
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(position);

            this.blockStartBreakTime = 0;
            if (BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(state.javaId()) || blockStateOverride != null ||
                    customItem != null || (skull != null && skull.getBlockDefinition() != null)) {
                this.blockStartBreakTime = tick;
                this.lastBlockBreakFace = blockFace;
            }

            LevelEventPacket startBreak = new LevelEventPacket();
            startBreak.setType(LevelEvent.BLOCK_START_BREAK);
            startBreak.setPosition(position.toFloat());
            startBreak.setData((int) (65535 / BlockUtils.reciprocal(breakProgress)));
            session.sendUpstreamPacket(startBreak);

            BlockUtils.spawnBlockBreakParticles(session, blockFace, position, state);

            this.currentBlockPos = position;
            this.currentBlockState = state;

            session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position,
                blockFace.mcpl(), session.getWorldCache().nextPredictionSequence()));
        }
    }

    protected void handleContinueDestroy(Vector3i position, BlockState state, Direction blockFace, long tick) {
        BlockUtils.spawnBlockBreakParticles(session, blockFace, position, state);
        double totalBreakTime = BlockUtils.reciprocal(calculateBreakProgress(state, position, session.getPlayerInventory().getItemInHand()));

        if (blockStartBreakTime != 0) {
            long ticksSinceStart = tick - blockStartBreakTime;
            // We need to add a slight delay to the break time, otherwise the client breaks blocks too fast
            if (ticksSinceStart >= (totalBreakTime += 2)) {
                destroyBlock(state, position, blockFace, false);
                return;
            }
            // Update in case it has changed
            lastBlockBreakFace = blockFace;
        }

        // Update the break time in the event that player conditions changed (jumping, effects applied)
        LevelEventPacket updateBreak = new LevelEventPacket();
        updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
        updateBreak.setPosition(position.toFloat());
        updateBreak.setData((int) (65535 / totalBreakTime));
        session.sendUpstreamPacket(updateBreak);
    }

    protected void handlePredictDestroy(Vector3i position, BlockState state, Direction blockFace, long tick) {
        destroyBlock(state, position, blockFace, false);
    }

    protected void handleAbortBreaking(Vector3i position) {
        // Bedrock edition "confirms" it stopped breaking blocks by sending an abort packet
        // We don't forward those as a Java client wouldn't send those either
        if (currentBlockPos != null) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, currentBlockPos,
                Direction.DOWN.mcpl(), 0);
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
    }

    /**
     * Tests for a previous item frame block interaction, or the presence
     * of an item frame at the position.
     * @return whether block breaking must stop due to an item frame interaction
     */
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

    /**
     * Tests whether the block action should be processed by testing whether
     * this action (or any other block action in this tick) was already rejected before
     */
    private boolean abortDueToBlockRestoring(Vector3i position) {
        // If it already contains our position, we can assume that a stop / restore was already sent
        if (restoredBlocks.contains(position)) {
            return true;
        }

        // We don't want to continue handling even new blocks as those could be e.g. behind a block which is not broken
        if (!restoredBlocks.isEmpty()) {
            BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
            restoredBlocks.add(position);
            return true;
        }
        return false;
    }

    /**
     * Checks whether a block interaction may proceed, or whether it must be interrupted.
     * This includes world border, "hands busy" (boat steering), and GameMode checks.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canBreak(Vector3i vector, BlockState state) {
        if (session.isHandsBusy() || !session.getWorldBorder().isInsideBorderBoundaries()) {
            return false;
        }

        switch (session.getGameMode()) {
            case SPECTATOR -> {
                return false;
            }
            case ADVENTURE -> {
                if (!blockPredicateCache.calculatePredicate(session, state, session.getPlayerInventory().getItemInHand())) {
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

        if (Registries.GAME_MASTER_BLOCKS.get().contains(state.block().javaIdentifier())) {
            if (!instabuild || session.getOpPermissionLevel() < 2) {
                return false;
            }
        }

        return !state.is(Blocks.AIR);
    }

    protected void destroyBlock(BlockState state, Vector3i vector, Direction direction, boolean instamine) {
        // Send java packet
        session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(instamine ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING,
            vector, direction.mcpl(), session.getWorldCache().nextPredictionSequence()));
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

    /**
     * Helper method to ignore all insta-break actions that were already sent to the Java server.
     * This ensures that Geyser does not send a FINISH_DIGGING player action for instantly mined blocks,
     * or those mined while in creative mode.
     */
    protected boolean testForLastInstaBreakPosOrReset(Vector3i position) {
        if (Objects.equals(lastInstaMinedPosition, position)) {
            return true;
        }
        lastInstaMinedPosition = null;
        return false;
    }

    /**
     * Resets variables after a block was broken.
     */
    protected void clearCurrentVariables() {
        this.currentBlockPos = null;
        this.currentBlockState = null;
        this.blockStartBreakTime = 0L;
    }

    /**
     * Resets the handler, including variables that persist across single packets
     */
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

            this.lastBlockState = state;
            this.lastItemStack = stack;

            // Any of the predicates have to match for the stack to match
            for (AdventureModePredicate.BlockPredicate predicate : canBreak.getPredicates()) {
                if (BlockUtils.blockMatchesPredicate(session, state, predicate)) {
                    return lastResult = true;
                }
            }
            return lastResult = false;
        }
    }
}
