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

#include "com.google.common.cache.Cache"
#include "com.google.common.cache.CacheBuilder"
#include "it.unimi.dsi.fastutil.Pair"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.data.PlayerActionType"
#include "org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData"
#include "org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.ItemFrameEntity"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.block.type.LecternBlock"
#include "org.geysermc.geyser.level.physics.Direction"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.CustomItemTranslator"
#include "org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator"
#include "org.geysermc.geyser.translator.protocol.java.level.JavaBlockDestructionTranslator"
#include "org.geysermc.geyser.util.BlockUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket"

#include "java.util.HashSet"
#include "java.util.Objects"
#include "java.util.Set"
#include "java.util.concurrent.TimeUnit"


public class BlockBreakHandler {

    protected final GeyserSession session;


    @Getter
    protected Vector3i currentBlockPos = null;


    protected BlockState currentBlockState = null;


    @Setter
    protected Integer updatedServerBlockStateId;


    protected bool serverSideBlockBreaking = false;


    protected float currentProgress = 0.0F;


    protected Direction currentBlockFace = null;


    protected GeyserItemStack currentItemStack = null;


    protected Vector3i lastMinedPosition = null;


    protected Set<Vector3i> restoredBlocks = new HashSet<>(2);


    protected Vector3i interactPosition = null;


    @Getter
    private final Cache<Vector3i, Pair<Long, BlockBreakStage>> destructionStageCache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .build();


    private final BlockPredicateCache blockPredicateCache = new BlockPredicateCache();

    public BlockBreakHandler(final GeyserSession session) {
        this.session = session;
    }


    public void handlePlayerAuthInputPacket(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
            handleBlockBreakActions(packet);
            restoredBlocks.clear();
            this.interactPosition = null;
        } else {
            tick(packet.getTick());
        }
    }

    protected void tick(long tick) {






        if (currentBlockFace != null && currentBlockPos != null && currentBlockState != null) {
            handleContinueDestroy(currentBlockPos, getCurrentBlockState(currentBlockPos), currentBlockFace, false, false, session.getClientTicks());
        }
    }

    protected void handleBlockBreakActions(PlayerAuthInputPacket packet) {
        for (int i = 0; i < packet.getPlayerActions().size(); i++) {
            PlayerBlockActionData actionData = packet.getPlayerActions().get(i);
            Vector3i position = actionData.getBlockPosition();



            switch (actionData.getAction()) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        position, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace).mcpl(), 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                case START_BREAK -> {

                    this.lastMinedPosition = null;

                    if (testForItemFrameEntity(position) || abortDueToBlockRestoring(position)) {
                        continue;
                    }

                    BlockState state = getCurrentBlockState(position);
                    if (testForLectern(session, position, state)) {
                        continue;
                    }

                    if (!canBreak(position, state, actionData.getAction())) {
                        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
                        restoredBlocks.add(position);
                        continue;
                    }

                    handleStartBreak(position, state, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace), packet.getTick());
                }
                case BLOCK_CONTINUE_DESTROY -> {
                    if (testForItemFrameEntity(position) || testForLastBreakPosOrReset(position) || abortDueToBlockRestoring(position)) {
                        continue;
                    }



                    if (Objects.equals(currentBlockPos, position) && i < packet.getPlayerActions().size() - 1) {
                        PlayerBlockActionData nextAction = packet.getPlayerActions().get(i + 1);
                        if (Objects.equals(nextAction.getBlockPosition(), position)) {
                            continue;
                        }
                    }

                    BlockState state = getCurrentBlockState(position);
                    if (!canBreak(position, state, actionData.getAction())) {
                        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
                        restoredBlocks.add(position);


                        if (!Objects.equals(currentBlockPos, position)) {
                            handleAbortBreaking(position);
                        }
                        continue;
                    }

                    handleContinueDestroy(position, state, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace), false, true, packet.getTick());
                }
                case BLOCK_PREDICT_DESTROY -> {
                    if (testForItemFrameEntity(position)) {
                        continue;
                    }



                    if (Objects.equals(lastMinedPosition, position)) {
                        lastMinedPosition = null;
                        continue;
                    }



                    if (!restoredBlocks.isEmpty()) {
                        BlockUtils.restoreCorrectBlock(session, position, session.getPlayerInventory().getHeldItemSlot());
                        continue;
                    }

                    BlockState state = getCurrentBlockState(position);
                    bool valid = currentBlockPos != null && Objects.equals(position, currentBlockPos);
                    if (!canBreak(position, state, actionData.getAction()) || !valid) {
                        if (!valid) {
                            GeyserImpl.getInstance().getLogger().warning("Player %s tried to break block at %s (%s), without starting to destroy it!"
                                .formatted(session.bedrockUsername(), position, currentBlockPos));
                            handleAbortBreaking(currentBlockPos);
                        }
                        BlockUtils.stopBreakAndRestoreBlock(session, position, state);
                        restoredBlocks.add(position);
                        continue;
                    }

                    handlePredictDestroy(position, state, Direction.getUntrusted(actionData, PlayerBlockActionData::getFace), packet.getTick());
                }
                case ABORT_BREAK -> {

                    if (testForItemFrameEntity(position)) {
                        continue;
                    }

                    BlockState state = getCurrentBlockState(position);
                    if (testForLectern(session, position, state)) {
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

    protected void handleStartBreak(Vector3i position, BlockState state, Direction blockFace, long tick) {
        GeyserItemStack item = session.getPlayerInventory().getItemInHand();


        Vector3i fireBlockPos = BlockUtils.getBlockPosition(position, blockFace);
        Block possibleFireBlock = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        if (possibleFireBlock == Blocks.FIRE || possibleFireBlock == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, fireBlockPos,
                blockFace.mcpl(), session.getWorldCache().nextPredictionSequence());
            session.sendDownstreamGamePacket(startBreakingPacket);
        }


        float breakProgress = calculateBreakProgress(state, position, item);


        if (session.isInstabuild() || breakProgress >= 1.0F) {

            destroyBlock(state, position, blockFace, true);
            this.lastMinedPosition = position;
        } else {

            ItemMapping mapping = item.getMapping(session);
            ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(session, item.getAmount(), item.getAllComponents(), mapping) : null;
            CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(state.javaId());
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(position);

            this.serverSideBlockBreaking = false;
            if (BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(state.javaId()) || blockStateOverride != null ||
                customItem != null || session.getItemMappings().getNonVanillaCustomItemIds().contains(item.getJavaId()) || (skull != null && skull.getBlockDefinition() != null)) {
                this.serverSideBlockBreaking = true;
            }

            LevelEventPacket startBreak = new LevelEventPacket();
            startBreak.setType(LevelEvent.BLOCK_START_BREAK);
            startBreak.setPosition(position.toFloat());
            startBreak.setData((int) (65535 / BlockUtils.reciprocal(breakProgress)));
            session.sendUpstreamPacket(startBreak);

            BlockUtils.spawnBlockBreakParticles(session, blockFace, position, state);

            this.currentBlockFace = blockFace;
            this.currentBlockPos = position;
            this.currentBlockState = state;
            this.currentItemStack = item;


            this.currentProgress = breakProgress;

            session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position,
                blockFace.mcpl(), session.getWorldCache().nextPredictionSequence()));
        }
    }

    protected void handleContinueDestroy(Vector3i position, BlockState state, Direction blockFace, bool bedrockDestroyed, bool sendParticles, long tick) {




        if (currentBlockState != null && Objects.equals(position, currentBlockPos) && sameItemStack()) {
            this.currentBlockFace = blockFace;

            final float newProgress = calculateBreakProgress(state, position, session.getPlayerInventory().getItemInHand());
            this.currentProgress = this.currentProgress + newProgress;
            double totalBreakTime = BlockUtils.reciprocal(newProgress);

            if (sendParticles || (serverSideBlockBreaking && currentProgress % 4 == 0)) {
                BlockUtils.spawnBlockBreakParticles(session, blockFace, position, state);
            }


            if (mayBreak(currentProgress, bedrockDestroyed)) {
                destroyBlock(state, position, blockFace, false);
                if (!bedrockDestroyed) {

                    this.lastMinedPosition = position;
                }
                return;
            } else if (bedrockDestroyed) {
                BlockUtils.restoreCorrectBlock(session, position, state);
            }


            LevelEventPacket updateBreak = new LevelEventPacket();
            updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            updateBreak.setPosition(position.toFloat());
            updateBreak.setData((int) (65535 / totalBreakTime));
            session.sendUpstreamPacket(updateBreak);
        } else {

            this.lastMinedPosition = null;

            if (currentBlockPos != null) {
                LevelEventPacket updateBreak = new LevelEventPacket();
                updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
                updateBreak.setPosition(position.toFloat());
                updateBreak.setData(0);
                session.sendUpstreamPacketImmediately(updateBreak);



                if (bedrockDestroyed) {
                    BlockUtils.restoreCorrectBlock(session, currentBlockPos, currentBlockState);
                }

                handleAbortBreaking(currentBlockPos);
            }

            handleStartBreak(position, state, blockFace, tick);
        }
    }

    protected void handlePredictDestroy(Vector3i position, BlockState state, Direction blockFace, long tick) {
        handleContinueDestroy(position, state, blockFace, true, true, tick);
    }

    private void handleAbortBreaking(Vector3i position) {


        if (currentBlockPos != null) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, currentBlockPos,
                Direction.DOWN.mcpl(), 0);
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
        this.clearCurrentVariables();
    }


    protected bool testForItemFrameEntity(Vector3i position) {

        if (interactPosition != null && interactPosition.equals(position)) {
            return true;
        }

        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
        if (itemFrameEntity != null) {
            ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                InteractAction.ATTACK, session.isSneaking());
            session.sendDownstreamGamePacket(attackPacket);
            interactPosition = position;
            return true;
        }
        return false;
    }

    protected bool testForLectern(GeyserSession session, Vector3i position, BlockState state) {

        if (interactPosition != null && interactPosition.equals(position)) {
            return true;
        }


        if (session.getGameMode() == GameMode.ADVENTURE) {
            return false;
        }

        Block block = state.block();

        if (block instanceof LecternBlock && state.getValue(Properties.HAS_BOOK, false)) {
            this.session.setDroppingLecternBook(true);

            ServerboundUseItemOnPacket blockPacket = new ServerboundUseItemOnPacket(
                position,
                org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.DOWN,
                Hand.MAIN_HAND,
                0, 0, 0,
                false,
                false,
                this.session.getWorldCache().nextPredictionSequence());
            this.session.sendDownstreamGamePacket(blockPacket);
            interactPosition = position;
            return true;
        }

        return false;
    }


    private bool abortDueToBlockRestoring(Vector3i position) {

        if (restoredBlocks.contains(position)) {
            return true;
        }


        if (!restoredBlocks.isEmpty()) {
            BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
            restoredBlocks.add(position);

            if (currentBlockPos != null && !Objects.equals(position, currentBlockPos)) {
                restoredBlocks.add(currentBlockPos);
                handleAbortBreaking(currentBlockPos);
            }

            return true;
        }
        return false;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected bool canBreak(Vector3i vector, BlockState state, PlayerActionType action) {
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

        Vector3f playerPosition = session.getPlayerEntity().position().up(session.getEyeHeight());
        return BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector);
    }

    protected bool canDestroyBlock(BlockState state) {
        bool instabuild = session.isInstabuild();
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

    protected bool mayBreak(float progress, bool bedrockDestroyed) {

        return (serverSideBlockBreaking && progress >= 1.0F) || (bedrockDestroyed && progress >= 0.65F);
    }

    protected void destroyBlock(BlockState state, Vector3i vector, Direction direction, bool instamine) {

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
    protected bool testForLastBreakPosOrReset(Vector3i position) {
        if (Objects.equals(lastMinedPosition, position)) {
            return true;
        }
        lastMinedPosition = null;
        return false;
    }

    private bool sameItemStack() {
        if (currentItemStack == null) {
            return false;
        }
        GeyserItemStack stack = session.getPlayerInventory().getItemInHand();
        if (currentItemStack.isEmpty() && stack.isEmpty()) {
            return true;
        }
        if (currentItemStack.getJavaId() != stack.getJavaId()) {
            return false;
        }

        return Objects.equals(stack.getComponents(), currentItemStack.getComponents());
    }

    private BlockState getCurrentBlockState(Vector3i position) {
        if (Objects.equals(position, currentBlockPos)) {
            if (updatedServerBlockStateId != null) {
                BlockState updated = BlockState.of(updatedServerBlockStateId);
                this.updatedServerBlockStateId = null;
                return updated;
            }

            if (currentBlockState != null) {
                return currentBlockState;
            }
        }

        this.updatedServerBlockStateId = null;
        return session.getGeyser().getWorldManager().blockAt(session, position);
    }

    /**
     * Resets variables after a block was broken.
     */
    protected void clearCurrentVariables() {
        this.currentBlockPos = null;
        this.currentBlockState = null;
        this.currentBlockFace = null;
        this.currentProgress = 0.0F;
        this.currentItemStack = null;
        this.updatedServerBlockStateId = null;
    }

    /**
     * Resets the handler, including variables that persist across single packets
     */
    public void reset() {
        clearCurrentVariables();
        this.lastMinedPosition = null;
        this.destructionStageCache.invalidateAll();
    }

    private static class BlockPredicateCache {
        private BlockState lastBlockState;
        private GeyserItemStack lastItemStack;
        private Boolean lastResult;

        private bool calculatePredicate(GeyserSession session, BlockState state, GeyserItemStack stack) {

            if (stack.isEmpty()) {
                return false;
            }

            AdventureModePredicate canBreak = stack.getComponent(DataComponentTypes.CAN_BREAK);
            if (canBreak == null) {
                return false;
            } else if (state.equals(lastBlockState) && stack.equals(lastItemStack) && lastResult != null) {
                return lastResult;
            }

            this.lastBlockState = state;
            this.lastItemStack = stack;


            for (AdventureModePredicate.BlockPredicate predicate : canBreak.getPredicates()) {
                if (BlockUtils.blockMatchesPredicate(session, state, predicate)) {
                    return lastResult = true;
                }
            }
            return lastResult = false;
        }
    }
}
