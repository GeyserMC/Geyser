/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.data.game.level.block.value.PistonValueType;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.*;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class PistonBlockEntity {
    private final GeyserSession session;
    @Getter
    private final Vector3i position;
    private final Direction orientation;
    private final boolean sticky;

    @Getter
    private PistonValueType action;

    /**
     * A map of attached block positions to Java ids.
     */
    private final Object2IntMap<Vector3i> attachedBlocks = new Object2IntOpenHashMap<>();
    /**
     * A flattened array of the positions of attached blocks, stored in XYZ order.
     */
    private int[] flattenedAttachedBlocks = new int[0];

    private boolean placedFinalBlocks = true;

    /**
     * The position of the piston head
     */
    private float progress;
    private float lastProgress;

    private long timeSinceCompletion = 0;

    private static final BoundingBox SOLID_BOUNDING_BOX = new BoundingBox(0.5, 0.5, 0.5, 1, 1, 1);
    private static final BoundingBox HONEY_BOUNDING_BOX;

    /**
     * The number of ticks to wait after a piston finishes its movement before
     * it can be removed
     */
    private static final int REMOVAL_DELAY = 5;

    static {
        // Create a ~1 x ~0.5 x ~1 bounding box above the honey block
        BlockCollision blockCollision = Registries.COLLISIONS.get(BlockStateValues.JAVA_HONEY_BLOCK_ID);
        if (blockCollision == null) {
            throw new RuntimeException("Failed to find honey block collision");
        }
        BoundingBox blockBoundingBox = blockCollision.getBoundingBoxes()[0];

        double honeyHeight = blockBoundingBox.getMax().getY();
        double boundingBoxHeight = 1.5 - honeyHeight;
        HONEY_BOUNDING_BOX = new BoundingBox(0.5, honeyHeight + boundingBoxHeight / 2, 0.5, blockBoundingBox.getSizeX(), boundingBoxHeight, blockBoundingBox.getSizeZ());
    }

    public PistonBlockEntity(GeyserSession session, Vector3i position, Direction orientation, boolean sticky, boolean extended) {
        this.session = session;
        this.position = position;
        this.orientation = orientation;
        this.sticky = sticky;

        if (extended) {
            // Fully extended
            this.action = PistonValueType.PUSHING;
            this.progress = 1.0f;
        } else {
            // Fully retracted
            this.action = PistonValueType.PULLING;
            this.progress = 0.0f;
        }
        this.lastProgress = this.progress;
    }

    /**
     * Set whether the piston is pulling or pushing blocks
     *
     * @param action PULLING or PUSHING or CANCELED_MID_PUSH
     */
    public void setAction(PistonValueType action) {
        if (this.action == action) {
            return;
        }
        placeFinalBlocks();
        removeMovingBlocks();

        this.action = action;
        if (action == PistonValueType.PUSHING || (action == PistonValueType.PULLING && sticky)) {
            // Blocks only move when pushing or pulling with sticky pistons
            findAffectedBlocks();
            removeBlocks();
            createMovingBlocks();
        } else {
            removePistonHead();
        }
        placedFinalBlocks = false;

        // Set progress and lastProgress to allow 0 tick pistons to animate
        switch (action) {
            case PUSHING -> progress = 0;
            case PULLING, CANCELLED_MID_PUSH -> progress = 1;
        }
        lastProgress = progress;

        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }

    public void setAction(PistonValueType action, Object2IntMap<Vector3i> attachedBlocks) {
        // Don't check if this.action == action, since on some Paper versions BlockPistonRetractEvent is called multiple times
        // with the first 1-2 events being empty.
        placeFinalBlocks();
        removeMovingBlocks();

        this.action = action;
        if (action == PistonValueType.PUSHING || (action == PistonValueType.PULLING && sticky)) {
            // Blocks only move when pushing or pulling with sticky pistons
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

        // Set progress and lastProgress to allow 0 tick pistons to animate
        switch (action) {
            case PUSHING -> progress = 0;
            case PULLING, CANCELLED_MID_PUSH -> progress = 1;
        }
        lastProgress = progress;

        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }

    /**
     * Update the position of the piston head, moving blocks, and players.
     */
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

    /**
     * Place attached blocks in their final position when done pushing or pulling
     */
    public void updateBlocks() {
        if (isDone()) {
            // Update blocks only once
            if (timeSinceCompletion == 0) {
                placeFinalBlocks();
            }
            // Give a few ticks for player collisions to be fully resolved
            if (timeSinceCompletion >= REMOVAL_DELAY) {
                removeMovingBlocks();
            }
        }
    }

    private void removePistonHead() {
        Vector3i blockInFront = position.add(orientation.getUnitVector());
        int blockId = session.getGeyser().getWorldManager().getBlockAt(session, blockInFront);
        if (BlockStateValues.isPistonHead(blockId)) {
            ChunkUtils.updateBlock(session, BlockStateValues.JAVA_AIR_ID, blockInFront);
        } else if (session.getGeyser().getPlatformType() == PlatformType.SPIGOT && blockId == BlockStateValues.JAVA_AIR_ID) {
            // Spigot removes the piston head from the cache, but we need to send the block update ourselves
            ChunkUtils.updateBlock(session, BlockStateValues.JAVA_AIR_ID, blockInFront);
        }
    }

    /**
     * Find the blocks that will be pushed or pulled by the piston
     */
    private void findAffectedBlocks() {
        Set<Vector3i> blocksChecked = new ObjectOpenHashSet<>();
        Queue<Vector3i> blocksToCheck = new LinkedList<>();

        Vector3i directionOffset = orientation.getUnitVector();
        Vector3i movement = getMovement();
        blocksChecked.add(position); // Don't check the piston itself
        if (action == PistonValueType.PULLING) {
            blocksChecked.add(getPistonHeadPos()); // Don't check the piston head
            blocksToCheck.add(position.add(directionOffset.mul(2)));
        } else if (action == PistonValueType.PUSHING) {
            removePistonHead(); // Remove lingering piston heads
            blocksToCheck.add(position.add(directionOffset));
        }

        boolean moveBlocks = true;
        while (!blocksToCheck.isEmpty() && attachedBlocks.size() <= 12) {
            Vector3i blockPos = blocksToCheck.remove();
            // Skip blocks we've already checked
            if (!blocksChecked.add(blockPos)) {
                continue;
            }
            int blockId = session.getGeyser().getWorldManager().getBlockAt(session, blockPos);
            if (blockId == BlockStateValues.JAVA_AIR_ID) {
                continue;
            }
            if (BlockStateValues.canPistonMoveBlock(blockId, action == PistonValueType.PUSHING)) {
                attachedBlocks.put(blockPos, blockId);
                if (BlockStateValues.isBlockSticky(blockId)) {
                    // For honey blocks and slime blocks check the blocks adjacent to it
                    for (Direction direction : Direction.VALUES) {
                        Vector3i offset = direction.getUnitVector();
                        // Only check blocks that aren't being pushed by the current block
                        if (offset.equals(movement)) {
                            continue;
                        }
                        Vector3i adjacentPos = blockPos.add(offset);
                        // Ignore the piston block itself
                        if (adjacentPos.equals(position)) {
                            continue;
                        }
                        // Ignore the piston head
                        if (action == PistonValueType.PULLING && position.add(directionOffset).equals(adjacentPos)) {
                            continue;
                        }
                        int adjacentBlockId = session.getGeyser().getWorldManager().getBlockAt(session, adjacentPos);
                        if (adjacentBlockId != BlockStateValues.JAVA_AIR_ID && BlockStateValues.isBlockAttached(blockId, adjacentBlockId) && BlockStateValues.canPistonMoveBlock(adjacentBlockId, false)) {
                            // If it is another slime/honey block we need to check its adjacent blocks
                            if (BlockStateValues.isBlockSticky(adjacentBlockId)) {
                                blocksToCheck.add(adjacentPos);
                            } else {
                                attachedBlocks.put(adjacentPos, adjacentBlockId);
                                blocksChecked.add(adjacentPos);
                                blocksToCheck.add(adjacentPos.add(movement));
                            }
                        }
                    }
                }
                // Check next block in line
                blocksToCheck.add(blockPos.add(movement));
            } else if (!BlockStateValues.canPistonDestroyBlock(blockId)) {
                // Block can't be moved or destroyed, so it blocks all block movement
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

    /**
     * Get the unit vector for the direction of movement
     *
     * @return The movement of the blocks
     */
    private Vector3i getMovement() {
        if (action == PistonValueType.PULLING) {
            return orientation.reversed().getUnitVector();
        }
        return orientation.getUnitVector(); // PUSHING and CANCELLED_MID_PUSH
    }

    /**
     * Replace all attached blocks with air
     */
    private void removeBlocks() {
        for (Vector3i blockPos : attachedBlocks.keySet()) {
            ChunkUtils.updateBlock(session, BlockStateValues.JAVA_AIR_ID, blockPos);
        }
        if (action != PistonValueType.PUSHING) {
            removePistonHead();
        }
    }

    /**
     * Push the player
     * If the player is pushed, the displacement is added to playerDisplacement in PistonCache
     * If the player contacts a slime block, playerMotion in PistonCache is updated
     */
    public void pushPlayer() {
        Vector3i direction = orientation.getUnitVector();
        double blockMovement = lastProgress;
        if (action == PistonValueType.PULLING || action == PistonValueType.CANCELLED_MID_PUSH) {
            blockMovement = 1f - lastProgress;
        }

        BoundingBox playerBoundingBox = session.getCollisionManager().getPlayerBoundingBox();
        // Shrink the collision in the other axes slightly, to avoid false positives when pressed up against the side of blocks
        Vector3d shrink = Vector3i.ONE.sub(direction.abs()).toDouble().mul(CollisionManager.COLLISION_TOLERANCE * 2);
        playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() - shrink.getX());
        playerBoundingBox.setSizeY(playerBoundingBox.getSizeY() - shrink.getY());
        playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() - shrink.getZ());

        // Resolve collision with the piston head
        int pistonHeadId = BlockStateValues.getPistonHead(orientation);
        pushPlayerBlock(pistonHeadId, getPistonHeadPos().toDouble(), blockMovement, playerBoundingBox);

        // Resolve collision with any attached moving blocks, but skip slime blocks
        // This prevents players from being launched by slime blocks covered by other blocks
        for (Object2IntMap.Entry<Vector3i> entry : attachedBlocks.object2IntEntrySet()) {
            int blockId = entry.getIntValue();
            if (blockId != BlockStateValues.JAVA_SLIME_BLOCK_ID) {
                Vector3d blockPos = entry.getKey().toDouble();
                pushPlayerBlock(blockId, blockPos, blockMovement, playerBoundingBox);
            }
        }
        // Resolve collision with slime blocks
        for (Object2IntMap.Entry<Vector3i> entry : attachedBlocks.object2IntEntrySet()) {
            int blockId = entry.getIntValue();
            if (blockId == BlockStateValues.JAVA_SLIME_BLOCK_ID) {
                Vector3d blockPos = entry.getKey().toDouble();
                pushPlayerBlock(blockId, blockPos, blockMovement, playerBoundingBox);
            }
        }

        // Undo shrink
        playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() + shrink.getX());
        playerBoundingBox.setSizeY(playerBoundingBox.getSizeY() + shrink.getY());
        playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() + shrink.getZ());
    }

    /**
     * Checks if a player is attached to the top of a honey block
     *
     * @param blockPos The position of the honey block
     * @param playerBoundingBox The player's bounding box
     * @return True if the player attached, otherwise false
     */
    private boolean isPlayerAttached(Vector3d blockPos, BoundingBox playerBoundingBox) {
        if (orientation.isVertical()) {
            return false;
        }
        return session.getPlayerEntity().isOnGround() && HONEY_BOUNDING_BOX.checkIntersection(blockPos, playerBoundingBox);
    }

    /**
     * Launches a player if the player is on the pushing side of the slime block
     *
     * @param blockPos The position of the slime block
     * @param playerPos The player's position
     */
    private void applySlimeBlockMotion(Vector3d blockPos, Vector3d playerPos) {
        Direction movementDirection = orientation;
        // Invert direction when pulling
        if (action == PistonValueType.PULLING) {
            movementDirection = movementDirection.reversed();
        }

        Vector3f movement = getMovement().toFloat();
        Vector3f motion = session.getPistonCache().getPlayerMotion();
        double motionX = motion.getX();
        double motionY = motion.getY();
        double motionZ = motion.getZ();
        blockPos = blockPos.add(0.5, 0.5, 0.5); // Move to the center of the slime block
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

    private void pushPlayerBlock(int javaId, Vector3d startingPos, double blockMovement, BoundingBox playerBoundingBox) {
        PistonCache pistonCache = session.getPistonCache();
        Vector3d movement = getMovement().toDouble();
        // Check if the player collides with the movingBlock block entity
        Vector3d finalBlockPos = startingPos.add(movement);
        if (SOLID_BOUNDING_BOX.checkIntersection(finalBlockPos, playerBoundingBox)) {
            pistonCache.setPlayerCollided(true);

            if (javaId == BlockStateValues.JAVA_SLIME_BLOCK_ID) {
                pistonCache.setPlayerSlimeCollision(true);
                applySlimeBlockMotion(finalBlockPos, Vector3d.from(playerBoundingBox.getMiddleX(), playerBoundingBox.getMiddleY(), playerBoundingBox.getMiddleZ()));
            }
        }

        Vector3d blockPos = startingPos.add(movement.mul(blockMovement));
        if (javaId == BlockStateValues.JAVA_HONEY_BLOCK_ID && isPlayerAttached(blockPos, playerBoundingBox)) {
            pistonCache.setPlayerCollided(true);
            pistonCache.setPlayerAttachedToHoney(true);

            double delta = Math.abs(progress - lastProgress);
            pistonCache.displacePlayer(movement.mul(delta));
        } else {
            // Move the player out of collision
            BlockCollision blockCollision = Registries.COLLISIONS.get(javaId);
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

                    if (javaId == BlockStateValues.JAVA_SLIME_BLOCK_ID) {
                        pistonCache.setPlayerSlimeCollision(true);
                        applySlimeBlockMotion(blockPos, Vector3d.from(playerBoundingBox.getMiddleX(), playerBoundingBox.getMiddleY(), playerBoundingBox.getMiddleZ()));
                    }
                }
            }
        }
    }

    private BlockCollision getCollision(Vector3i blockPos) {
        return BlockUtils.getCollision(getAttachedBlockId(blockPos));
    }

    /**
     * Compute the maximum movement of a bounding box that won't collide with the moving block attached to this piston
     *
     * @param blockPos The position of the moving block
     * @param boundingBox The bounding box of the moving entity
     * @param axis The axis of movement
     * @param movement The movement in the axis
     * @return The adjusted movement
     */
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
            if (getAttachedBlockId(blockPos) == BlockStateValues.JAVA_SLIME_BLOCK_ID && adjustedMovement != movement) {
                session.getPistonCache().setPlayerSlimeCollision(true);
            }
            return adjustedMovement;
        }
        return movement;
    }

    public boolean checkCollision(Vector3i blockPos, BoundingBox boundingBox) {
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

    private int getAttachedBlockId(Vector3i blockPos) {
        if (blockPos.equals(getPistonHeadPos())) {
            return BlockStateValues.getPistonHead(orientation);
        } else {
            return attachedBlocks.getOrDefault(blockPos, BlockStateValues.JAVA_AIR_ID);
        }
    }

    /**
     * Create moving block entities for each attached block
     */
    private void createMovingBlocks() {
        // Map the final position of each block to this block entity
        Map<Vector3i, PistonBlockEntity> movingBlockMap = session.getPistonCache().getMovingBlocksMap();
        attachedBlocks.forEach((blockPos, javaId) -> movingBlockMap.put(blockPos, this));
        movingBlockMap.put(getPistonHeadPos(), this);

        Vector3i movement = getMovement();
        BoundingBox playerBoundingBox = session.getCollisionManager().getPlayerBoundingBox().clone();
        if (orientation == Direction.UP) {
            // Extend the bounding box down, to catch collisions when the player is falling down
            playerBoundingBox.extend(0, -256, 0);
            playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() + 0.5);
            playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() + 0.5);
        }
        attachedBlocks.forEach((blockPos, javaId) -> {
            Vector3i newPos = blockPos.add(movement);
            if (SOLID_BOUNDING_BOX.checkIntersection(blockPos.toDouble(), playerBoundingBox) ||
                    SOLID_BOUNDING_BOX.checkIntersection(newPos.toDouble(), playerBoundingBox)) {
                session.getPistonCache().setPlayerCollided(true);
                if (javaId == BlockStateValues.JAVA_SLIME_BLOCK_ID) {
                    session.getPistonCache().setPlayerSlimeCollision(true);
                }
                // Don't place moving blocks that collide with the player
                // because of https://bugs.mojang.com/browse/MCPE-96035
                return;
            }
            // Place a moving block at the new location of the block
            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
            updateBlockPacket.setBlockPosition(newPos);
            updateBlockPacket.setRuntimeId(session.getBlockMappings().getBedrockMovingBlockId());
            updateBlockPacket.setDataLayer(0);
            session.sendUpstreamPacket(updateBlockPacket);
            // Update moving block with correct details
            BlockEntityUtils.updateBlockEntity(session, buildMovingBlockTag(newPos, javaId, position), newPos);
        });
    }

    /**
     * Place blocks that don't collide with the player into their final position
     * otherwise the player will fall off the block.
     * The Java server will handle updating the blocks that do collide later.
     */
    private void placeFinalBlocks() {
        // Prevent blocks from being placed multiple times since it is called in
        // setAction and updateBlocks
        if (placedFinalBlocks) {
            return;
        }
        placedFinalBlocks = true;
        Vector3i movement = getMovement();
        attachedBlocks.forEach((blockPos, javaId) -> {
            blockPos = blockPos.add(movement);
            // Send a final block entity packet to detach blocks
            BlockEntityUtils.updateBlockEntity(session, buildMovingBlockTag(blockPos, javaId, Direction.DOWN.getUnitVector()), blockPos);
            // Don't place blocks that collide with the player
            if (!SOLID_BOUNDING_BOX.checkIntersection(blockPos.toDouble(), session.getCollisionManager().getPlayerBoundingBox())) {
                ChunkUtils.updateBlock(session, javaId, blockPos);
            }
        });
        if (action == PistonValueType.PUSHING) {
            Vector3i pistonHeadPos = getPistonHeadPos().add(movement);
            if (!SOLID_BOUNDING_BOX.checkIntersection(pistonHeadPos.toDouble(), session.getCollisionManager().getPlayerBoundingBox())) {
                ChunkUtils.updateBlock(session, BlockStateValues.getPistonHead(orientation), pistonHeadPos);
            }
        }
    }

    /**
     * Remove moving blocks from the piston cache
     */
    private void removeMovingBlocks() {
        Map<Vector3i, PistonBlockEntity> movingBlockMap = session.getPistonCache().getMovingBlocksMap();
        attachedBlocks.forEach((blockPos, javaId) -> movingBlockMap.remove(blockPos));
        attachedBlocks.clear();
        movingBlockMap.remove(getPistonHeadPos());
        flattenedAttachedBlocks = new int[0];
    }

    /**
     * Flatten the positions of attached blocks into a 1D array
     */
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

    /**
     * Get the Bedrock state of the piston
     *
     * @return 0 - Fully retracted, 1 - Extending, 2 - Fully extended, 3 - Retracting
     */
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

    /**
     * @return The starting position of the piston head
     */
    private Vector3i getPistonHeadPos() {
        if (action == PistonValueType.PUSHING) {
            return position;
        }
        return position.add(orientation.getUnitVector());
    }

    /**
     * Update the progress or position of the piston head
     */
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

    /**
     * @return True if the piston has finished its movement, otherwise false
     */
    public boolean isDone() {
        return switch (action) {
            case PUSHING -> progress == 1.0f && lastProgress == 1.0f;
            case PULLING, CANCELLED_MID_PUSH -> progress == 0.0f && lastProgress == 0.0f;
        };
    }

    public boolean canBeRemoved() {
        return isDone() && timeSinceCompletion > REMOVAL_DELAY;
    }

    /**
     * Create a piston data tag with the data in this block entity
     *
     * @return A piston data tag
     */
    private NbtMap buildPistonTag() {
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "PistonArm")
                .putIntArray("AttachedBlocks", flattenedAttachedBlocks)
                .putFloat("Progress", progress)
                .putFloat("LastProgress", lastProgress)
                .putByte("NewState", getState())
                .putByte("State", getState())
                .putByte("Sticky", (byte) (sticky ? 1 : 0))
                .putByte("isMovable", (byte) 0)
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        return builder.build();
    }

    /**
     * Create a piston data tag that has fully extended/retracted
     *
     * @param position The position for the base of the piston
     * @param extended Whether the piston is extended or retracted
     * @param sticky Whether the piston is a sticky piston or a regular piston
     * @return A piston data tag for a fully extended/retracted piston
     */
    public static NbtMap buildStaticPistonTag(Vector3i position, boolean extended, boolean sticky) {
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "PistonArm")
                .putFloat("Progress", extended ? 1.0f : 0.0f)
                .putFloat("LastProgress", extended ? 1.0f : 0.0f)
                .putByte("NewState", (byte) (extended ? 2 : 0))
                .putByte("State", (byte) (extended ? 2 : 0))
                .putByte("Sticky", (byte) (sticky ? 1 : 0))
                .putByte("isMovable", (byte) 0)
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        return builder.build();
    }

    /**
     * Create a moving block tag of a block that will be moved by a piston
     *
     * @param position The ending position of the block (The location of the movingBlock block entity)
     * @param javaId The Java Id of the block that is moving
     * @param pistonPosition The position for the base of the piston that's moving the block
     * @return A moving block data tag
     */
    private NbtMap buildMovingBlockTag(Vector3i position, int javaId, Vector3i pistonPosition) {
        // Get Bedrock block state data
        NbtMap movingBlock = session.getBlockMappings().getBedrockBlockStates().get(session.getBlockMappings().getBedrockBlockId(javaId));
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "MovingBlock")
                .putCompound("movingBlock", movingBlock)
                .putByte("isMovable", (byte) 1)
                .putInt("pistonPosX", pistonPosition.getX())
                .putInt("pistonPosY", pistonPosition.getY())
                .putInt("pistonPosZ", pistonPosition.getZ())
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        if (PistonBlockEntityTranslator.isBlock(javaId)) {
            builder.putCompound("movingEntity", PistonBlockEntityTranslator.getTag(javaId, position));
        }
        return builder.build();
    }
}
