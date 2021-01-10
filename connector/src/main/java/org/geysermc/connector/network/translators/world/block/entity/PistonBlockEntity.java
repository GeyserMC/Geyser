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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.value.PistonValue;
import com.github.steveice10.mc.protocol.data.game.world.block.value.PistonValueType;
import com.google.common.collect.ImmutableList;
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
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionManager;
import org.geysermc.connector.network.translators.collision.CollisionTranslator;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.ChunkUtils;

import java.util.*;

public class PistonBlockEntity {
    private final GeyserSession session;
    @Getter
    private final Vector3i position;
    private final PistonValue orientation;
    private final boolean sticky;

    private PistonValueType action;

    /**
     * A map of attached block positions to Java ids.
     */
    private final Object2IntMap<Vector3i> attachedBlocks = new Object2IntOpenHashMap<>();
    /**
     * A flattened array of the positions of attached blocks, stored in XYZ order.
     */
    private int[] flattenedAttachedBlocks = new int[0];

    /**
     * The position of the piston head
     */
    private float progress = 0.0f;
    private float lastProgress = 0.0f;

    private static final NbtMap AIR_TAG = BlockTranslator.BLOCKS.get(BlockTranslator.BEDROCK_AIR_ID).getCompound("block");
    private static final List<Vector3i> ALL_DIRECTIONS = ImmutableList.of(Vector3i.from(1, 0, 0), Vector3i.from(0, 1, 0), Vector3i.from(0, 0, 1), Vector3i.from(-1, 0, 0), Vector3i.from(0, -1, 0), Vector3i.from(0, 0, -1));

    private static final BoundingBox SOLID_BOUNDING_BOX = new BoundingBox(0.5, 0.5, 0.5, 1, 1, 1);
    private static final BoundingBox HONEY_BOUNDING_BOX;

    static {
        // Create a ~1 x ~0.5 x ~1 bounding box above the honey block
        BlockCollision blockCollision = CollisionTranslator.getCollision(BlockTranslator.JAVA_RUNTIME_HONEY_BLOCK_ID, 0, 0, 0);
        BoundingBox blockBoundingBox = blockCollision.getContainingBoundingBox();

        double honeyHeight = blockBoundingBox.getMax().getY();
        double boundingBoxHeight = 1.5 - honeyHeight;
        HONEY_BOUNDING_BOX = new BoundingBox(0.5, honeyHeight + boundingBoxHeight / 2, 0.5, blockBoundingBox.getSizeX(), boundingBoxHeight, blockBoundingBox.getSizeZ());
    }

    public PistonBlockEntity(GeyserSession session, Vector3i position, PistonValue orientation) {
        this.session = session;
        this.position = position;
        this.orientation = orientation;

        if (session.getConnector().getConfig().isCacheChunks()) {
            int blockId = session.getConnector().getWorldManager().getBlockAt(session, position);
            sticky = BlockStateValues.isStickyPiston(blockId);
            boolean extended = BlockStateValues.getPistonValues().get(blockId);
            if (extended) {
                this.action = PistonValueType.PUSHING;
                this.progress = 1.0f;
            } else {
                this.action = PistonValueType.PULLING;
                this.progress = 0.0f;
            }
            this.lastProgress = progress;
        } else {
            sticky = false;
        }
    }

    /**
     * Set whether the piston is pulling or pushing blocks
     *
     * @param action Pulling or Pushing
     */
    public void setAction(PistonValueType action) {
        this.action = action;
        if (action == PistonValueType.PUSHING || (action == PistonValueType.PULLING && sticky)) {
            // Blocks only move when pushing or pulling with sticky pistons
            findAffectedBlocks();
            removeBlocks();
            createMovingBlocks();
        }

        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }

    /**
     * Update the position of the piston head, moving blocks, and players.
     */
    public void updateMovement() {
        updateProgress();
        pushPlayer();
        BlockEntityUtils.updateBlockEntity(session, buildPistonTag(), position);
    }

    /**
     * Place attached blocks in their final position when done pushing or pulling
     */
    public void updateBlocks() {
        if (!isDone()) {
            if (action == PistonValueType.CANCELLED_MID_PUSH) {
                finishMovingBlocks();
                attachedBlocks.clear();
                flattenedAttachedBlocks = new int[0];
            }
        } else {
            if (action != PistonValueType.PUSHING) {
                removePistonHead();
            }
            finishMovingBlocks();
        }
    }

    private boolean isPistonHead(int blockId) {
        String javaId = BlockTranslator.getJavaIdBlockMap().inverse().get(blockId);
        return javaId.contains("piston_head");
    }

    /**
     * Removes lingering piston heads
     */
    private void removePistonHead() {
        Vector3i blockInFront = position.add(getDirectionOffset());
        int blockId = session.getConnector().getWorldManager().getBlockAt(session, blockInFront);
        if (isPistonHead(blockId)) {
            ChunkUtils.updateBlock(session, BlockTranslator.JAVA_AIR_ID, blockInFront);
        }
    }

    /**
     * Find the blocks that will be pushed or pulled by the piston
     */
    private void findAffectedBlocks() {
        attachedBlocks.clear();
        if (!session.getConnector().getConfig().isCacheChunks()) {
            flattenPositions();
            return;
        }
        Set<Vector3i> blocksChecked = new ObjectOpenHashSet<>();
        Queue<Vector3i> blocksToCheck = new LinkedList<>();

        Vector3i directionOffset = getDirectionOffset();
        Vector3i movement = getMovement();
        blocksChecked.add(position); // Don't check the piston itself
        if (action == PistonValueType.PULLING) {
            blocksChecked.add(position.add(directionOffset)); // Don't check the piston head
            blocksToCheck.add(position.add(directionOffset.mul(2)));
        } else if (action == PistonValueType.PUSHING) {
            removePistonHead();
            blocksToCheck.add(position.add(directionOffset));
        }

        boolean moveBlocks = true;
        while (!blocksToCheck.isEmpty() && attachedBlocks.size() < 12) {
            Vector3i blockPos = blocksToCheck.remove();
            // Skip blocks we've already checked
            if (blocksChecked.contains(blockPos)) {
                continue;
            }
            blocksChecked.add(blockPos);
            int blockId = session.getConnector().getWorldManager().getBlockAt(session, blockPos);
            if (blockId == BlockTranslator.JAVA_AIR_ID) {
                continue;
            }
            blocksChecked.add(blockPos);
            if (canMoveBlock(blockId, action == PistonValueType.PUSHING)) {
                attachedBlocks.put(blockPos, blockId);
                if (isBlockSticky(blockId)) {
                    // For honey blocks and slime blocks check the blocks adjacent to it
                    for (Vector3i offset : ALL_DIRECTIONS) {
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
                        int adjacentBlockId = session.getConnector().getWorldManager().getBlockAt(session, adjacentPos);
                        if (adjacentBlockId != BlockTranslator.JAVA_AIR_ID && isBlockAttached(blockId, adjacentBlockId) && canMoveBlock(adjacentBlockId, false)) {
                            // If it is another slime/honey block we need to check its adjacent blocks
                            if (isBlockSticky(adjacentBlockId)) {
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
            } else if (cannotDestroyBlock(blockId)) {
                // Block can't be moved or destroyed, so it blocks all block movement
                moveBlocks = false;
                break;
            }
        }
        // This shouldn't happen as the server does all of these checks before sending a block value packet
        if (!moveBlocks || attachedBlocks.size() > 12) {
            attachedBlocks.clear();
        }
        flattenPositions();
    }

    private boolean canMoveBlock(int javaId, boolean isPushing) {
        if (javaId == BlockTranslator.JAVA_AIR_ID) {
            return true;
        }
        // Pistons can only be moved if they aren't extended
        if (PistonBlockEntityTranslator.isBlock(javaId) && !isPistonHead(javaId)) {
            return !BlockStateValues.getPistonValues().get(javaId);
        }
        // Bedrock, End portal frames, etc. can't be moved
        if (BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(javaId) == -1.0d) {
            return false;
        }
        String pistonBehavior = BlockTranslator.JAVA_RUNTIME_ID_TO_PISTON_BEHAVIOR.getOrDefault(javaId, "normal");
        switch (pistonBehavior) {
            case "block":
            case "destroy":
                return false;
            case "push_only": // Glazed terracotta can only be pushed
                return isPushing;
        }
        // Pistons can't move block entities
        return !BlockTranslator.JAVA_RUNTIME_ID_TO_HAS_BLOCK_ENTITY.getOrDefault(javaId, false);
    }

    private boolean cannotDestroyBlock(int javaId)  {
        return !BlockTranslator.JAVA_RUNTIME_ID_TO_PISTON_BEHAVIOR.getOrDefault(javaId, "normal").equals("destroy");
    }

    /**
     * Checks if a block sticks to other blocks
     * (Slime and honey blocks)
     * @param javaId The block id
     * @return True if the block sticks to adjacent blocks
     */
    private boolean isBlockSticky(int javaId) {
        return javaId == BlockTranslator.JAVA_RUNTIME_SLIME_BLOCK_ID || javaId == BlockTranslator.JAVA_RUNTIME_HONEY_BLOCK_ID;
    }

    /**
     * Check if two blocks are attached to each other
     *
     * @param javaIdA The block id of block a
     * @param javaIdB The block id of block b
     * @return True if the blocks are attached to each other
     */
    private boolean isBlockAttached(int javaIdA, int javaIdB) {
        boolean aSticky = isBlockSticky(javaIdA);
        boolean bSticky = isBlockSticky(javaIdB);
        if (aSticky && bSticky) {
            // Only matching sticky blocks are attached together
            // Honey + Honey & Slime + Slime
            return javaIdA == javaIdB;
        }
        return aSticky || bSticky;
    }

    /**
     * Get the direction the piston head points in
     *
     * @return A Vector3i pointing in the direction of the piston head
     */
    private Vector3i getDirectionOffset() {
        switch (orientation) {
            case DOWN:
                return Vector3i.from(0, -1, 0);
            case UP:
                return Vector3i.from(0, 1, 0);
            case SOUTH:
                return Vector3i.from(0, 0, 1);
            case WEST:
                return Vector3i.from(-1, 0, 0);
            case NORTH:
                return Vector3i.from(0, 0, -1);
            case EAST:
                return Vector3i.from(1, 0, 0);
        }
        return Vector3i.ZERO;
    }

    /**
     * Get the offset from the current position of the attached blocks
     * to the new positions
     *
     * @return The movement of the blocks
     */
    private Vector3i getMovement() {
        if (action == PistonValueType.PULLING) {
            return getDirectionOffset().negate();
        }
        return getDirectionOffset(); // PUSHING and CANCELLED_MID_PUSH
    }

    /**
     * Replace all attached blocks with air
     */
    private void removeBlocks() {
        for (Vector3i blockPos : attachedBlocks.keySet()) {
            ChunkUtils.updateBlock(session, BlockTranslator.JAVA_AIR_ID, blockPos);
        }
    }

    private void pushPlayer() {
        Vector3i direction = getDirectionOffset();
        Vector3d movement = getMovement().toDouble();
        Vector3d attachedBlockOffset = movement.mul(lastProgress);
        if (action == PistonValueType.PULLING || action == PistonValueType.CANCELLED_MID_PUSH) {
            attachedBlockOffset = movement.mul(1f - lastProgress);
        }

        double delta = Math.abs(progress - lastProgress);
        Vector3d extend = movement.mul(delta);

        BoundingBox playerBoundingBox = session.getCollisionManager().getPlayerBoundingBox();
        // Shrink the collision in the other axes slightly, to avoid false positives when pressed up against the side of blocks
        Vector3d shrink = Vector3i.ONE.sub(direction.abs()).toDouble().mul(CollisionManager.COLLISION_TOLERANCE * 2);
        playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() - shrink.getX());
        playerBoundingBox.setSizeY(playerBoundingBox.getSizeY() - shrink.getY());
        playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() - shrink.getZ());

        double displacement = 0;
        // Check collision with the piston head
        Vector3d blockPos = position.toDouble().add(attachedBlockOffset);
        if (action == PistonValueType.PULLING) {
            blockPos = blockPos.add(direction.toDouble());
        }
        int pistonHeadId = BlockStateValues.getPistonHead(orientation);
        BlockCollision pistonCollision = CollisionTranslator.getCollision(pistonHeadId, 0, 0, 0);

        double intersection = getBlockIntersection(blockPos, pistonCollision, playerBoundingBox, extend);
        if (intersection > 0) {
            intersection = Math.min(intersection, delta);
            displacement = intersection + 0.01d;
        }
        // Check collision with all the attached blocks
        for (Object2IntMap.Entry<Vector3i> entry : attachedBlocks.object2IntEntrySet()) {
            blockPos = entry.getKey().toDouble().add(attachedBlockOffset);
            int javaId = entry.getIntValue();
            BlockCollision blockCollision = CollisionTranslator.getCollision(javaId, 0, 0, 0);
            // Check if the player collides with the movingBlock block entity
            Vector3d finalBlockPos = entry.getKey().toDouble().add(movement);
            if (SOLID_BOUNDING_BOX.checkIntersection(finalBlockPos, playerBoundingBox)) {
                session.getPistonCache().setPlayerCollided(true);

                if (javaId == BlockTranslator.JAVA_RUNTIME_SLIME_BLOCK_ID) {
                    session.getPistonCache().setPlayerSlimeCollision(true);
                }
            }

            if (javaId == BlockTranslator.JAVA_RUNTIME_SLIME_BLOCK_ID && testBlockCollision(blockPos, blockCollision, playerBoundingBox, extend)) {
                Vector3d playerPos = Vector3d.from(playerBoundingBox.getMiddleX(), playerBoundingBox.getMiddleY(), playerBoundingBox.getMiddleZ());
                playerPos = playerPos.add(movement.mul(displacement));
                applySlimeBlockMotion(blockPos, playerPos);
            }

            if (javaId == BlockTranslator.JAVA_RUNTIME_HONEY_BLOCK_ID && isPlayerAttached(blockPos, playerBoundingBox)) {
                session.getPistonCache().setPlayerCollided(true);
                displacement = Math.max(delta, displacement);
            } else if (displacement < 0.51d) { // Don't bother to check collision with other blocks as we've reached the max displacement
                intersection = getBlockIntersection(blockPos, blockCollision, playerBoundingBox, extend);
                if (intersection > 0) {
                    intersection = Math.min(intersection, delta);
                    displacement = Math.max(intersection + 0.01d, displacement);
                }
            }
        }
        // Undo shrink
        playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() + shrink.getX());
        playerBoundingBox.setSizeY(playerBoundingBox.getSizeY() + shrink.getY());
        playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() + shrink.getZ());

        if (displacement > 0) {
            Vector3d totalDisplacement = session.getPistonCache().getPlayerDisplacement().add(movement.mul(displacement));
            // Clamp to range -0.51 to 0.51
            totalDisplacement = totalDisplacement.max(-0.51d, -0.51d, -0.51d).min(0.51d, 0.51d, 0.51d);
            session.getPistonCache().setPlayerDisplacement(totalDisplacement);

            Vector3d position = session.getPlayerEntity().getPosition().down(EntityType.PLAYER.getOffset()).toDouble();
            position = position.add(totalDisplacement);
            session.getCollisionManager().updatePlayerBoundingBox(position);
        }
    }

    /**
     * Checks if a player is attached to the top of a honey block
     *
     * @param blockPos The position of the honey block
     * @param playerBoundingBox The player's bounding box
     * @return True if the player attached, otherwise false
     */
    private boolean isPlayerAttached(Vector3d blockPos, BoundingBox playerBoundingBox) {
        if (orientation == PistonValue.UP || orientation == PistonValue.DOWN) {
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
        PistonValue movementDirection = orientation;
        // Invert direction when pulling
        if (action == PistonValueType.PULLING) {
            switch (movementDirection) {
                case DOWN:
                    movementDirection = PistonValue.UP;
                    break;
                case UP:
                    movementDirection = PistonValue.DOWN;
                    break;
                case NORTH:
                    movementDirection = PistonValue.SOUTH;
                    break;
                case SOUTH:
                    movementDirection = PistonValue.NORTH;
                    break;
                case WEST:
                    movementDirection = PistonValue.EAST;
                    break;
                case EAST:
                    movementDirection = PistonValue.WEST;
                    break;
            }
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

    private boolean testBlockCollision(Vector3d blockPos, BlockCollision blockCollision, BoundingBox boundingBox, Vector3d extend) {
        if (blockCollision != null) {
            BoundingBox blockBoundingBox = blockCollision.getContainingBoundingBox();
            if (boundingBox != null) {
                blockBoundingBox.extend(extend);
                return blockBoundingBox.checkIntersection(blockPos, boundingBox);
            }
        }
        return false;
    }

    private double getBlockIntersection(Vector3d blockPos, BlockCollision blockCollision, BoundingBox boundingBox, Vector3d extend) {
        if (!testBlockCollision(blockPos, blockCollision, boundingBox, extend)) {
            return 0;
        }
        double maxIntersection = 0;
        for (BoundingBox b : blockCollision.getBoundingBoxes()) {
            b = b.clone();
            b.extend(extend);

            Vector3d intersectionSize = b.getIntersectionSize(blockPos, boundingBox);
            switch (orientation) {
                case DOWN:
                case UP:
                    maxIntersection = Math.max(intersectionSize.getY(), maxIntersection);
                    break;
                case NORTH:
                case SOUTH:
                    maxIntersection = Math.max(intersectionSize.getZ(), maxIntersection);
                    break;
                case WEST:
                case EAST:
                    maxIntersection = Math.max(intersectionSize.getX(), maxIntersection);
                    break;
            }
        }
        return maxIntersection;
    }

    /**
     * Create moving block entities for each attached block
     */
    private void createMovingBlocks() {
        Vector3i movement = getMovement();
        BoundingBox playerBoundingBox = session.getCollisionManager().getPlayerBoundingBox();
        attachedBlocks.forEach((blockPos, javaId) -> {
            Vector3i newPos = blockPos.add(movement);
            // Don't place a movingBlock if it will collide with the player as it has collision and messes with motion
            if (SOLID_BOUNDING_BOX.checkIntersection(newPos.toDouble(), playerBoundingBox)) {
                return;
            }
            // Place a moving block at the new location of the block
            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
            updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
            updateBlockPacket.setBlockPosition(newPos);
            updateBlockPacket.setRuntimeId(BlockTranslator.BEDROCK_RUNTIME_MOVING_BLOCK_ID);
            updateBlockPacket.setDataLayer(0);
            session.sendUpstreamPacket(updateBlockPacket);
            // Update moving block with correct details
            BlockEntityUtils.updateBlockEntity(session, buildMovingBlockTag(newPos, javaId, position), newPos);
        });
    }

    /**
     * Replace all moving block entities with the final block
     */
    private void finishMovingBlocks() {
        Vector3i movement = getMovement();
        attachedBlocks.forEach((blockPos, javaId) -> {
            blockPos = blockPos.add(movement);
            // Send a final block entity packet to detach blocks
            BlockEntityUtils.updateBlockEntity(session, buildMovingBlockTag(blockPos, javaId, Vector3i.from(0, -1, 0)), blockPos);
            // Replace with final block
            ChunkUtils.updateBlock(session, javaId, blockPos);
            // Send piston block entity data
            if (PistonBlockEntityTranslator.isBlock(javaId)) {
                BlockEntityUtils.updateBlockEntity(session, PistonBlockEntityTranslator.getTag(javaId, blockPos), blockPos);
            }
        });
    }

    /**
     * Flatten the positions of attached blocks into a 1D array
     */
    private void flattenPositions() {
        flattenedAttachedBlocks = new int[3 * attachedBlocks.size()];
        Iterator<Vector3i> attachedBlocksIterator = attachedBlocks.keySet().iterator();
        int i = 0;
        while (attachedBlocksIterator.hasNext()) {
            Vector3i position = attachedBlocksIterator.next();
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
     * Update the progress or position of the piston head
     */
    private void updateProgress() {
        switch (action) {
            case PUSHING:
                lastProgress = progress;
                progress += 0.5f;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                }
                break;
            case CANCELLED_MID_PUSH:
            case PULLING:
                lastProgress = progress;
                progress -= 0.5f;
                if (progress <= 0.0f) {
                    progress = 0.0f;
                }
                break;
        }
    }

    /**
     * @return True if the piston has finished it's movement, otherwise false
     */
    public boolean isDone() {
        switch (action) {
            case PUSHING:
                return progress == 1.0f && lastProgress == 1.0f;
            case PULLING:
            case CANCELLED_MID_PUSH:
                return progress == 0.0f && lastProgress == 0.0f;
        }
        return true;
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
        NbtMap movingBlock = BlockTranslator.BLOCKS.get(BlockTranslator.getBedrockBlockId(javaId)).getCompound("block");
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "MovingBlock")
                .putCompound("movingBlock", movingBlock)
                .putCompound("movingBlockExtra", AIR_TAG) //TODO figure out if this changes
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
