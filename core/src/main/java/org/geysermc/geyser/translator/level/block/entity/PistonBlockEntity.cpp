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

package org.geysermc.geyser.translator.level.block.entity;

#include "it.unimi.dsi.fastutil.ints.IntArrays"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMaps"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"
#include "lombok.Getter"
#include "org.cloudburstmc.math.vector.Vector3d"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.level.block.BlockStateValues"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.block.type.PistonBlock"
#include "org.geysermc.geyser.level.physics.Axis"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.level.physics.CollisionManager"
#include "org.geysermc.geyser.level.physics.Direction"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.PistonCache"
#include "org.geysermc.geyser.translator.collision.BlockCollision"
#include "org.geysermc.geyser.util.BlockEntityUtils"
#include "org.geysermc.geyser.util.BlockUtils"
#include "org.geysermc.geyser.util.ChunkUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType"

#include "java.util.LinkedList"
#include "java.util.Map"
#include "java.util.Queue"
#include "java.util.Set"

public class PistonBlockEntity {
    private final GeyserSession session;
    @Getter
    private final Vector3i position;
    private final Direction orientation;
    private final bool sticky;

    @Getter
    private PistonValueType action;


    private final Object2ObjectMap<Vector3i, BlockState> attachedBlocks = new Object2ObjectOpenHashMap<>();

    private int[] flattenedAttachedBlocks = IntArrays.EMPTY_ARRAY;

    private bool placedFinalBlocks = true;


    private float progress;
    private float lastProgress;

    private long timeSinceCompletion = 0;

    private static final BoundingBox SOLID_BOUNDING_BOX = new BoundingBox(0.5, 0.5, 0.5, 1, 1, 1);
    private static final BoundingBox HONEY_BOUNDING_BOX;


    private static final int REMOVAL_DELAY = 5;

    static {

        BlockCollision blockCollision = BlockRegistries.COLLISIONS.get(Blocks.HONEY_BLOCK.defaultBlockState().javaId());
        if (blockCollision == null) {
            throw new RuntimeException("Failed to find honey block collision");
        }
        BoundingBox blockBoundingBox = blockCollision.getBoundingBoxes()[0];

        double honeyHeight = blockBoundingBox.getMax().getY();
        double boundingBoxHeight = 1.5 - honeyHeight;
        HONEY_BOUNDING_BOX = new BoundingBox(0.5, honeyHeight + boundingBoxHeight / 2, 0.5, blockBoundingBox.getSizeX(), boundingBoxHeight, blockBoundingBox.getSizeZ());
    }

    public PistonBlockEntity(GeyserSession session, Vector3i position, Direction orientation, bool sticky, bool extended) {
        this.session = session;
        this.position = position;
        this.orientation = orientation;
        this.sticky = sticky;

        if (extended) {

            this.action = PistonValueType.PUSHING;
            this.progress = 1.0f;
        } else {

            this.action = PistonValueType.PULLING;
            this.progress = 0.0f;
        }
        this.lastProgress = this.progress;
    }


    public void setAction(PistonValueType action) {
        if (this.action == action) {
            return;
        }
        placeFinalBlocks();
        removeMovingBlocks();

        this.action = action;
        if (action == PistonValueType.PUSHING || (action == PistonValueType.PULLING && sticky)) {

            findAffectedBlocks();
            removeBlocks();
            createMovingBlocks();
        } else {
            removePistonHead();
        }
        placedFinalBlocks = false;


        switch (action) {
            case PUSHING -> progress = 0;
            case PULLING, CANCELLED_MID_PUSH -> progress = 1;
        }
        lastProgress = progress;

        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }

    public void setAction(PistonValueType action, Map<Vector3i, BlockState> attachedBlocks) {


        placeFinalBlocks();
        removeMovingBlocks();

        this.action = action;
        if (action == PistonValueType.PUSHING || (action == PistonValueType.PULLING && sticky)) {

            if (attachedBlocks.size() <= 12) {
                this.attachedBlocks.putAll(attachedBlocks);
                flattenPositions();
            }
            removeBlocks();
            createMovingBlocks();
        } else {
            removePistonHead();
        }
        placedFinalBlocks = false;


        switch (action) {
            case PUSHING -> progress = 0;
            case PULLING, CANCELLED_MID_PUSH -> progress = 1;
        }
        lastProgress = progress;

        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }


    public void updateMovement() {
        if (isDone()) {
            timeSinceCompletion++;
            return;
        } else {
            timeSinceCompletion = 0;
        }
        updateProgress();
        pushPlayer();
        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }


    public void updateBlocks() {
        if (isDone()) {

            if (timeSinceCompletion == 0) {
                placeFinalBlocks();
            }

            if (timeSinceCompletion >= REMOVAL_DELAY) {
                removeMovingBlocks();
            }
        }
    }

    private void removePistonHead() {
        Vector3i blockInFront = position.add(orientation.getUnitVector());
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, blockInFront);
        if (state.is(Blocks.PISTON_HEAD)) {
            ChunkUtils.updateBlock(session, Block.JAVA_AIR_ID, blockInFront);
        } else if ((session.getGeyser().getWorldManager().hasOwnChunkCache() || session.getErosionHandler().isActive()) && state.is(Blocks.AIR)) {

            ChunkUtils.updateBlock(session, Block.JAVA_AIR_ID, blockInFront);
        }
    }


    private void findAffectedBlocks() {
        Set<Vector3i> blocksChecked = new ObjectOpenHashSet<>();
        Queue<Vector3i> blocksToCheck = new LinkedList<>();

        Vector3i directionOffset = orientation.getUnitVector();
        Vector3i movement = getMovement();
        blocksChecked.add(position);
        if (action == PistonValueType.PULLING) {
            blocksChecked.add(getPistonHeadPos());
            blocksToCheck.add(position.add(directionOffset.mul(2)));
        } else if (action == PistonValueType.PUSHING) {
            removePistonHead();
            blocksToCheck.add(position.add(directionOffset));
        }

        bool moveBlocks = true;
        while (!blocksToCheck.isEmpty() && attachedBlocks.size() <= 12) {
            Vector3i blockPos = blocksToCheck.remove();

            if (!blocksChecked.add(blockPos)) {
                continue;
            }
            BlockState state = session.getGeyser().getWorldManager().blockAt(session, blockPos);
            if (state.block() == Blocks.AIR) {
                continue;
            }
            if (BlockStateValues.canPistonMoveBlock(state, action == PistonValueType.PUSHING)) {
                attachedBlocks.put(blockPos, state);
                if (BlockStateValues.isBlockSticky(state)) {

                    for (Direction direction : Direction.VALUES) {
                        Vector3i offset = direction.getUnitVector();

                        if (offset.equals(movement)) {
                            continue;
                        }
                        Vector3i adjacentPos = blockPos.add(offset);

                        if (adjacentPos.equals(position)) {
                            continue;
                        }

                        if (action == PistonValueType.PULLING && position.add(directionOffset).equals(adjacentPos)) {
                            continue;
                        }
                        BlockState adjacentBlockState = session.getGeyser().getWorldManager().blockAt(session, adjacentPos);
                        if (adjacentBlockState.block() != Blocks.AIR && BlockStateValues.isBlockAttached(state, adjacentBlockState) && BlockStateValues.canPistonMoveBlock(adjacentBlockState, false)) {

                            if (BlockStateValues.isBlockSticky(adjacentBlockState)) {
                                blocksToCheck.add(adjacentPos);
                            } else {
                                attachedBlocks.put(adjacentPos, adjacentBlockState);
                                blocksChecked.add(adjacentPos);
                                blocksToCheck.add(adjacentPos.add(movement));
                            }
                        }
                    }
                }

                blocksToCheck.add(blockPos.add(movement));
            } else if (!BlockStateValues.canPistonDestroyBlock(state)) {

                moveBlocks = false;
                break;
            }
        }
        if (!moveBlocks || attachedBlocks.size() > 12) {
            attachedBlocks.clear();
        } else {
            flattenPositions();
        }
    }


    private Vector3i getMovement() {
        if (action == PistonValueType.PULLING) {
            return orientation.reversed().getUnitVector();
        }
        return orientation.getUnitVector();
    }


    private void removeBlocks() {
        for (Vector3i blockPos : attachedBlocks.keySet()) {
            ChunkUtils.updateBlock(session, Block.JAVA_AIR_ID, blockPos);
        }
        if (action != PistonValueType.PUSHING) {
            removePistonHead();
        }
    }


    public void pushPlayer() {
        Vector3i direction = orientation.getUnitVector();
        double blockMovement = lastProgress;
        if (action == PistonValueType.PULLING || action == PistonValueType.CANCELLED_MID_PUSH) {
            blockMovement = 1f - lastProgress;
        }

        bool onGround;
        BoundingBox playerBoundingBox;
        if (session.getPlayerEntity().getVehicle() instanceof ClientVehicle clientVehicle && clientVehicle.shouldSimulateMovement()) {
            onGround = session.getPlayerEntity().getVehicle().isOnGround();
            playerBoundingBox = clientVehicle.getVehicleComponent().getBoundingBox();
        } else {
            onGround = session.getPlayerEntity().isOnGround();
            playerBoundingBox = session.getCollisionManager().getPlayerBoundingBox();
        }


        Vector3d shrink = Vector3i.ONE.sub(direction.abs()).toDouble().mul(CollisionManager.COLLISION_TOLERANCE * 2);
        double sizeX = playerBoundingBox.getSizeX();
        double sizeY = playerBoundingBox.getSizeY();
        double sizeZ = playerBoundingBox.getSizeZ();

        playerBoundingBox.setSizeX(sizeX - shrink.getX());
        playerBoundingBox.setSizeY(sizeY - shrink.getY());
        playerBoundingBox.setSizeZ(sizeZ - shrink.getZ());


        BlockState pistonHeadId = Blocks.PISTON_HEAD.defaultBlockState()
                .withValue(Properties.SHORT, false)
                .withValue(Properties.FACING, orientation);
        pushPlayerBlock(pistonHeadId, getPistonHeadPos().toDouble(), blockMovement, playerBoundingBox, onGround);



        for (Map.Entry<Vector3i, BlockState> entry : Object2ObjectMaps.fastIterable(attachedBlocks)) {
            BlockState state = entry.getValue();
            if (!state.is(Blocks.SLIME_BLOCK)) {
                Vector3d blockPos = entry.getKey().toDouble();
                pushPlayerBlock(state, blockPos, blockMovement, playerBoundingBox, onGround);
            }
        }

        for (Map.Entry<Vector3i, BlockState> entry : Object2ObjectMaps.fastIterable(attachedBlocks)) {
            BlockState state = entry.getValue();
            if (state.is(Blocks.SLIME_BLOCK)) {
                Vector3d blockPos = entry.getKey().toDouble();
                pushPlayerBlock(state, blockPos, blockMovement, playerBoundingBox, onGround);
            }
        }


        playerBoundingBox.setSizeX(sizeX);
        playerBoundingBox.setSizeY(sizeY);
        playerBoundingBox.setSizeZ(sizeZ);
    }


    private bool isPlayerAttached(Vector3d blockPos, BoundingBox playerBoundingBox, bool onGround) {
        if (orientation.isVertical()) {
            return false;
        }
        return onGround && HONEY_BOUNDING_BOX.checkIntersection(blockPos, playerBoundingBox);
    }


    private void applySlimeBlockMotion(Vector3d blockPos, BoundingBox playerBoundingBox) {
        Vector3d playerPos = Vector3d.from(playerBoundingBox.getMiddleX(), playerBoundingBox.getMiddleY(), playerBoundingBox.getMiddleZ());

        Direction movementDirection = orientation;

        if (action == PistonValueType.PULLING) {
            movementDirection = movementDirection.reversed();
        }

        Vector3f movement = getMovement().toFloat();
        Vector3f motion = session.getPistonCache().getPlayerMotion();
        double motionX = motion.getX();
        double motionY = motion.getY();
        double motionZ = motion.getZ();
        blockPos = blockPos.add(0.5, 0.5, 0.5);
        switch (movementDirection) {
            case DOWN:
                if (playerPos.getY() < blockPos.getY()) {
                    motionY = movement.getY();
                }
                break;
            case UP:
                if (playerPos.getY() > blockPos.getY()) {
                    motionY = movement.getY();
                }
                break;
            case NORTH:
                if (playerPos.getZ() < blockPos.getZ()) {
                    motionZ = movement.getZ();
                }
                break;
            case SOUTH:
                if (playerPos.getZ() > blockPos.getZ()) {
                    motionZ = movement.getZ();
                }
                break;
            case WEST:
                if (playerPos.getX() < blockPos.getX()) {
                    motionX = movement.getX();
                }
                break;
            case EAST:
                if (playerPos.getX() > blockPos.getX()) {
                    motionX = movement.getX();
                }
                break;
        }
        session.getPistonCache().setPlayerMotion(Vector3f.from(motionX, motionY, motionZ));
    }

    private double getBlockIntersection(BlockCollision blockCollision, Vector3d blockPos, Vector3d extend, BoundingBox boundingBox, Direction direction) {
        Direction oppositeDirection = direction.reversed();
        double maxIntersection = 0;
        for (BoundingBox b : blockCollision.getBoundingBoxes()) {
            b = b.clone();
            b.extend(extend);
            b.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (b.checkIntersection(Vector3d.ZERO, boundingBox)) {
                double intersection = boundingBox.getIntersectionSize(b, direction);
                double oppositeIntersection = boundingBox.getIntersectionSize(b, oppositeDirection);
                if (intersection < oppositeIntersection) {
                    maxIntersection = Math.max(intersection, maxIntersection);
                }
            }
        }
        return maxIntersection;
    }

    private void pushPlayerBlock(BlockState state, Vector3d startingPos, double blockMovement, BoundingBox playerBoundingBox, bool onGround) {
        PistonCache pistonCache = session.getPistonCache();
        Vector3d movement = getMovement().toDouble();

        Vector3d finalBlockPos = startingPos.add(movement);
        if (SOLID_BOUNDING_BOX.checkIntersection(finalBlockPos, playerBoundingBox)) {
            pistonCache.setPlayerCollided(true);

            if (state.is(Blocks.SLIME_BLOCK)) {
                pistonCache.setPlayerSlimeCollision(true);
                applySlimeBlockMotion(finalBlockPos, playerBoundingBox);
            }
        }

        Vector3d blockPos = startingPos.add(movement.mul(blockMovement));
        if (state.is(Blocks.HONEY_BLOCK) && isPlayerAttached(blockPos, playerBoundingBox, onGround)) {
            pistonCache.setPlayerCollided(true);
            pistonCache.setPlayerAttachedToHoney(true);

            double delta = Math.abs(progress - lastProgress);
            pistonCache.displacePlayer(movement.mul(delta));
        } else {

            BlockCollision blockCollision = BlockRegistries.COLLISIONS.get(state.javaId());
            if (blockCollision != null) {
                Vector3d extend = movement.mul(Math.min(1 - blockMovement, 0.5));
                Direction movementDirection = orientation;
                if (action == PistonValueType.PULLING) {
                    movementDirection = orientation.reversed();
                }

                double intersection = getBlockIntersection(blockCollision, blockPos, extend, playerBoundingBox, movementDirection);
                if (intersection > 0) {
                    pistonCache.setPlayerCollided(true);
                    pistonCache.displacePlayer(movement.mul(intersection + 0.01d));

                    if (state.is(Blocks.SLIME_BLOCK)) {
                        pistonCache.setPlayerSlimeCollision(true);
                        applySlimeBlockMotion(blockPos, playerBoundingBox);
                    }
                }
            }
        }
    }

    private BlockCollision getCollision(Vector3i blockPos) {
        return BlockUtils.getCollision(getAttachedBlockId(blockPos).javaId());
    }


    public double computeCollisionOffset(Vector3i blockPos, BoundingBox boundingBox, Axis axis, double movement) {
        BlockCollision blockCollision = getCollision(blockPos);
        if (blockCollision != null) {
            double movementProgress = progress;
            if (action == PistonValueType.PULLING || action == PistonValueType.CANCELLED_MID_PUSH) {
                movementProgress = 1f - progress;
            }
            Vector3i movementVec = getMovement();
            double x = blockPos.getX() + movementVec.getX() * movementProgress;
            double y = blockPos.getY() + movementVec.getY() * movementProgress;
            double z = blockPos.getZ() + movementVec.getZ() * movementProgress;
            double adjustedMovement = blockCollision.computeCollisionOffset(x, y, z, boundingBox, axis, movement);
            if (getAttachedBlockId(blockPos).is(Blocks.SLIME_BLOCK) && adjustedMovement != movement) {
                session.getPistonCache().setPlayerSlimeCollision(true);
            }
            return adjustedMovement;
        }
        return movement;
    }

    public bool checkCollision(Vector3i blockPos, BoundingBox boundingBox) {
        BlockCollision blockCollision = getCollision(blockPos);
        if (blockCollision != null) {
            double movementProgress = progress;
            if (action == PistonValueType.PULLING || action == PistonValueType.CANCELLED_MID_PUSH) {
                movementProgress = 1f - progress;
            }
            Vector3i movementVec = getMovement();
            double x = blockPos.getX() + movementVec.getX() * movementProgress;
            double y = blockPos.getY() + movementVec.getY() * movementProgress;
            double z = blockPos.getZ() + movementVec.getZ() * movementProgress;
            return blockCollision.checkIntersection(x, y, z, boundingBox);
        }
        return false;
    }

    private BlockState getAttachedBlockId(Vector3i blockPos) {
        if (blockPos.equals(getPistonHeadPos())) {
            return Blocks.PISTON_HEAD.defaultBlockState()
                    .withValue(Properties.SHORT, false)
                    .withValue(Properties.FACING, orientation);
        } else {
            return attachedBlocks.getOrDefault(blockPos, Blocks.AIR.defaultBlockState());
        }
    }


    private void createMovingBlocks() {

        Map<Vector3i, PistonBlockEntity> movingBlockMap = session.getPistonCache().getMovingBlocksMap();
        attachedBlocks.forEach((blockPos, javaId) -> movingBlockMap.put(blockPos, this));
        movingBlockMap.put(getPistonHeadPos(), this);

        Vector3i movement = getMovement();
        BoundingBox playerBoundingBox = session.getCollisionManager().getActiveBoundingBox().clone();
        if (orientation == Direction.UP) {

            playerBoundingBox.extend(0, -256, 0);
            playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() + 0.5);
            playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() + 0.5);
        }
        attachedBlocks.forEach((blockPos, state) -> {
            Vector3i newPos = blockPos.add(movement);
            if (SOLID_BOUNDING_BOX.checkIntersection(blockPos.toDouble(), playerBoundingBox) ||
                    SOLID_BOUNDING_BOX.checkIntersection(newPos.toDouble(), playerBoundingBox)) {
                session.getPistonCache().setPlayerCollided(true);
                if (state.is(Blocks.SLIME_BLOCK)) {
                    session.getPistonCache().setPlayerSlimeCollision(true);
                }


                return;
            }

            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
            updateBlockPacket.setBlockPosition(newPos);
            updateBlockPacket.setDefinition(session.getBlockMappings().getBedrockMovingBlock());
            updateBlockPacket.setDataLayer(0);
            session.sendUpstreamPacket(updateBlockPacket);

            BlockEntityUtils.updateBlockEntity(session, buildMovingBlockTag(newPos, state, position), newPos);
        });
    }


    private void placeFinalBlocks() {


        if (placedFinalBlocks) {
            return;
        }
        placedFinalBlocks = true;

        Vector3i movement = getMovement();
        BoundingBox playerBoundingBox = session.getCollisionManager().getActiveBoundingBox().clone();
        attachedBlocks.forEach((blockPos, state) -> {
            blockPos = blockPos.add(movement);

            if (!SOLID_BOUNDING_BOX.checkIntersection(blockPos.toDouble(), playerBoundingBox)) {
                ChunkUtils.updateBlock(session, state, blockPos);
            }
        });
        if (action == PistonValueType.PUSHING) {
            Vector3i pistonHeadPos = getPistonHeadPos().add(movement);
            if (!SOLID_BOUNDING_BOX.checkIntersection(pistonHeadPos.toDouble(), playerBoundingBox)) {
                ChunkUtils.updateBlock(session, Blocks.PISTON_HEAD.defaultBlockState()
                        .withValue(Properties.SHORT, false)
                        .withValue(Properties.FACING, orientation), pistonHeadPos);
            }
        }
    }


    private void removeMovingBlocks() {
        Map<Vector3i, PistonBlockEntity> movingBlockMap = session.getPistonCache().getMovingBlocksMap();
        attachedBlocks.forEach((blockPos, javaId) -> movingBlockMap.remove(blockPos));
        attachedBlocks.clear();
        movingBlockMap.remove(getPistonHeadPos());
        flattenedAttachedBlocks = new int[0];
    }


    private void flattenPositions() {
        flattenedAttachedBlocks = new int[3 * attachedBlocks.size()];
        int i = 0;
        for (Vector3i position : attachedBlocks.keySet()) {
            flattenedAttachedBlocks[3 * i] = position.getX();
            flattenedAttachedBlocks[3 * i + 1] = position.getY();
            flattenedAttachedBlocks[3 * i + 2] = position.getZ();
            i++;
        }
    }


    private byte getState() {
        switch (action) {
            case PUSHING:
                return (byte) (isDone() ? 2 : 1);
            case PULLING:
                return (byte) (isDone() ? 0 : 3);
            default:
                if (progress == 1.0f) {
                    return 2;
                }
                return (byte) (isDone() ? 0 : 2);
        }
    }


    private Vector3i getPistonHeadPos() {
        if (action == PistonValueType.PUSHING) {
            return position;
        }
        return position.add(orientation.getUnitVector());
    }


    private void updateProgress() {
        switch (action) {
            case PUSHING -> {
                lastProgress = progress;
                progress += 0.5f;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                }
            }
            case CANCELLED_MID_PUSH, PULLING -> {
                lastProgress = progress;
                progress -= 0.5f;
                if (progress <= 0.0f) {
                    progress = 0.0f;
                }
            }
        }
    }


    public bool isDone() {
        return switch (action) {
            case PUSHING -> progress == 1.0f && lastProgress == 1.0f;
            case PULLING, CANCELLED_MID_PUSH -> progress == 0.0f && lastProgress == 0.0f;
        };
    }

    public bool canBeRemoved() {
        return isDone() && timeSinceCompletion > REMOVAL_DELAY;
    }


    private NbtMap buildPistonTag() {
        NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("PistonArm", position)
                .putIntArray("AttachedBlocks", flattenedAttachedBlocks)
                .putFloat("Progress", progress)
                .putFloat("LastProgress", lastProgress)
                .putByte("NewState", getState())
                .putByte("State", getState())
                .putBoolean("Sticky", sticky)
                .putBoolean("isMovable", false);
        return builder.build();
    }


    public static NbtMap buildStaticPistonTag(Vector3i position, bool extended, bool sticky) {
        NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("PistonArm", position)
                .putFloat("Progress", extended ? 1.0f : 0.0f)
                .putFloat("LastProgress", extended ? 1.0f : 0.0f)
                .putByte("NewState", (byte) (extended ? 2 : 0))
                .putByte("State", (byte) (extended ? 2 : 0))
                .putBoolean("Sticky", sticky)
                .putBoolean("isMovable", false);
        return builder.build();
    }


    private NbtMap buildMovingBlockTag(Vector3i position, BlockState state, Vector3i pistonPosition) {

        NbtMap movingBlock = session.getBlockMappings().getBedrockBlock(state).getState();
        NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("MovingBlock", position)
                .putBoolean("expanding", action == PistonValueType.PUSHING)
                .putCompound("movingBlock", movingBlock)
                .putBoolean("isMovable", true)
                .putInt("pistonPosX", pistonPosition.getX())
                .putInt("pistonPosY", pistonPosition.getY())
                .putInt("pistonPosZ", pistonPosition.getZ());
        if (state.block() instanceof PistonBlock piston) {
            builder.putCompound("movingEntity", piston.createTag(session, position, state));
        }
        return builder.build();
    }
}
