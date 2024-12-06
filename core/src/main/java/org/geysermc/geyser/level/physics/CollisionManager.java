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

package org.geysermc.geyser.level.physics;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.OtherCollision;
import org.geysermc.geyser.translator.collision.ScaffoldingCollision;
import org.geysermc.geyser.translator.collision.SolidCollision;
import org.geysermc.geyser.util.BlockUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CollisionManager {
    public static final BlockCollision SOLID_COLLISION = new SolidCollision(null);
    public static final BlockCollision FLUID_COLLISION = new OtherCollision(new BoundingBox[]{new BoundingBox(0.5, 0.25, 0.5, 1, 0.5, 1)});

    private final GeyserSession session;

    @Getter
    private final BoundingBox playerBoundingBox;

    /**
     * Whether the player is inside scaffolding
     */
    @Setter
    private boolean touchingScaffolding;

    /**
     * Whether the player is on top of scaffolding
     */
    @Setter
    private boolean onScaffolding;

    /**
     * Additional space where blocks are checked, which is helpful for fixing NoCheatPlus's Passable check.
     * This check doesn't allow players right up against the block, so they must be pushed slightly away.
     */
    public static final double COLLISION_TOLERANCE = 0.00001;
    /**
     * Trims Y coordinates when jumping to prevent rounding issues being sent to the server.
     * The locale used is necessary so other regions don't use <code>,</code> as their decimal separator.
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.ENGLISH));

    private static final double PLAYER_STEP_UP = 0.6;

    /**
     * The maximum squared distance between a Bedrock players' movement and our predicted movement before
     * the player is teleported to the correct position
     */
    private static final double INCORRECT_MOVEMENT_THRESHOLD = 0.08;

    public CollisionManager(GeyserSession session) {
        this.session = session;
        this.playerBoundingBox = new BoundingBox(0, 0, 0, 0.6, 1.8, 0.6);
    }

    /**
     * Updates the stored bounding box
     * @param position The new position of the player
     */
    public void updatePlayerBoundingBox(Vector3f position) {
        updatePlayerBoundingBox(position.toDouble());
    }

    /**
     * Updates the stored bounding box
     * @param position The new position of the player
     */
    public void updatePlayerBoundingBox(Vector3d position) {
        updatePlayerBoundingBox();

        playerBoundingBox.setMiddleX(position.getX());
        playerBoundingBox.setMiddleY(position.getY() + (playerBoundingBox.getSizeY() / 2));
        playerBoundingBox.setMiddleZ(position.getZ());
    }

    /**
     * Updates the height of the stored bounding box
     */
    public void updatePlayerBoundingBox() {
        // According to the Minecraft Wiki, when sneaking:
        // - In Bedrock Edition, the height becomes 1.65 blocks, allowing movement through spaces as small as 1.75 (2 - 1⁄4) blocks high.
        // - In Java Edition, the height becomes 1.5 blocks.
        // Other instances have the player's bounding box become as small as 0.6 or 0.2.
        double playerHeight = session.getPlayerEntity().getBoundingBoxHeight();
        playerBoundingBox.setMiddleY(playerBoundingBox.getMiddleY() - (playerBoundingBox.getSizeY() / 2.0) + (playerHeight / 2.0));
        playerBoundingBox.setSizeY(playerHeight);
    }

    /**
     * Gets the bounding box to use for player movement.
     * <p>
     * This will return either the bounding box of a {@link ClientVehicle}, or the player's own bounding box.
     *
     * @return the bounding box to use for movement calculations
     */
    public BoundingBox getActiveBoundingBox() {
        if (session.getPlayerEntity().getVehicle() instanceof ClientVehicle clientVehicle && clientVehicle.isClientControlled()) {
            return clientVehicle.getVehicleComponent().getBoundingBox();
        }

        return playerBoundingBox;
    }

    /**
     * Adjust the Bedrock position before sending to the Java server to account for inaccuracies in movement between
     * the two versions. Will also send corrected movement packets back to Bedrock if they collide with pistons.
     *
     * @param bedrockPosition the current Bedrock position of the client
     * @param onGround whether the Bedrock player is on the ground
     * @param teleported whether the Bedrock player has teleported to a new position. If true, movement correction is skipped.
     * @return the position to send to the Java server, or null to cancel sending the packet
     */
    public @Nullable CollisionResult adjustBedrockPosition(Vector3f bedrockPosition, boolean onGround, boolean teleported) {
        PistonCache pistonCache = session.getPistonCache();
        // Bedrock clients tend to fall off of honey blocks, so we need to teleport them to the new position
        if (pistonCache.isPlayerAttachedToHoney()) {
            return null;
        }
        // We need to parse the float as a string since casting a float to a double causes us to
        // lose precision and thus, causes players to get stuck when walking near walls
        double javaY = bedrockPosition.getY() - EntityDefinitions.PLAYER.offset();

        Vector3d position = Vector3d.from(Double.parseDouble(Float.toString(bedrockPosition.getX())), javaY,
                Double.parseDouble(Float.toString(bedrockPosition.getZ())));

        // Don't correct position if controlling a vehicle
        if (session.getPlayerEntity().getVehicle() instanceof ClientVehicle clientVehicle && clientVehicle.isClientControlled()) {
            playerBoundingBox.setMiddleX(position.getX());
            playerBoundingBox.setMiddleY(position.getY() + playerBoundingBox.getSizeY() / 2);
            playerBoundingBox.setMiddleZ(position.getZ());

            return new CollisionResult(playerBoundingBox.getBottomCenter(), TriState.NOT_SET);
        }

        Vector3d startingPos = playerBoundingBox.getBottomCenter();
        Vector3d movement = position.sub(startingPos);
        Vector3d adjustedMovement = correctPlayerMovement(movement, false, teleported);
        playerBoundingBox.translate(adjustedMovement.getX(), adjustedMovement.getY(), adjustedMovement.getZ());
        playerBoundingBox.translate(pistonCache.getPlayerMotion().getX(), pistonCache.getPlayerMotion().getY(), pistonCache.getPlayerMotion().getZ());
        // Correct player position
        if (!correctPlayerPosition()) {
            // Cancel the movement if it needs to be cancelled
            recalculatePosition();
            return null;
        }
        // The server can't complain about our movement if we never send it
        // TODO get rid of this and handle teleports smoothly
        if (pistonCache.isPlayerCollided()) {
            return null;
        }

        position = playerBoundingBox.getBottomCenter();

        boolean newOnGround = adjustedMovement.getY() != movement.getY() && movement.getY() < 0 || onGround;
        // Send corrected position to Bedrock if they differ by too much to prevent de-syncs
        if (onGround != newOnGround || movement.distanceSquared(adjustedMovement) > INCORRECT_MOVEMENT_THRESHOLD) {
            PlayerEntity playerEntity = session.getPlayerEntity();
            // Client will dismount if on a vehicle
            if (playerEntity.getVehicle() == null && pistonCache.getPlayerMotion().equals(Vector3f.ZERO) && !pistonCache.isPlayerSlimeCollision()) {
                playerEntity.moveAbsolute(position.toFloat(), playerEntity.getYaw(), playerEntity.getPitch(), playerEntity.getHeadYaw(), onGround, true);
            }
        }

        if (!newOnGround) {
            // Trim the position to prevent rounding errors that make Java think we are clipping into a block
            position = Vector3d.from(position.getX(), Double.parseDouble(DECIMAL_FORMAT.format(position.getY())), position.getZ());
        }

        return new CollisionResult(position, TriState.byBoolean(onGround));
    }

    // TODO: This makes the player look upwards for some reason, rotation values must be wrong
    public void recalculatePosition() {
        PlayerEntity entity = session.getPlayerEntity();
        // Gravity might need to be reset...
        entity.updateBedrockMetadata(); // TODO may not be necessary

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
        movePlayerPacket.setPosition(entity.getPosition());
        movePlayerPacket.setRotation(entity.getBedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        session.sendUpstreamPacket(movePlayerPacket);
    }

    public BlockPositionIterator collidableBlocksIterator(BoundingBox box) {
        Vector3d position = Vector3d.from(box.getMiddleX(),
                box.getMiddleY() - (box.getSizeY() / 2),
                box.getMiddleZ());

        // Expand volume by 1 in each direction to include moving blocks
        double pistonExpand = session.getPistonCache().getPistons().isEmpty() ? 0 : 1;

        // Loop through all blocks that could collide
        int minCollisionX = (int) Math.floor(position.getX() - ((box.getSizeX() / 2) + COLLISION_TOLERANCE + pistonExpand));
        int maxCollisionX = (int) Math.floor(position.getX() + (box.getSizeX() / 2) + COLLISION_TOLERANCE + pistonExpand);

        // Y extends 0.5 blocks down because of fence hitboxes
        int minCollisionY = (int) Math.floor(position.getY() - 0.5 - COLLISION_TOLERANCE - pistonExpand / 2.0);
        int maxCollisionY = (int) Math.floor(position.getY() + box.getSizeY() + pistonExpand);

        int minCollisionZ = (int) Math.floor(position.getZ() - ((box.getSizeZ() / 2) + COLLISION_TOLERANCE + pistonExpand));
        int maxCollisionZ = (int) Math.floor(position.getZ() + (box.getSizeZ() / 2) + COLLISION_TOLERANCE + pistonExpand);

        return BlockPositionIterator.fromMinMax(minCollisionX, minCollisionY, minCollisionZ, maxCollisionX, maxCollisionY, maxCollisionZ);
    }

    public BlockPositionIterator playerCollidableBlocksIterator() {
        return collidableBlocksIterator(playerBoundingBox);
    }

    /**
     * Returns false if the movement is invalid, and in this case it shouldn't be sent to the server and should be
     * cancelled
     * See {@link BlockCollision#correctPosition(GeyserSession, int, int, int, BoundingBox)} for more info
     */
    public boolean correctPlayerPosition() {

        // These may be set to true by the correctPosition method in ScaffoldingCollision
        touchingScaffolding = false;
        onScaffolding = false;

        // Used when correction code needs to be run before the main correction
        BlockPositionIterator iter = session.getCollisionManager().playerCollidableBlocksIterator();
        int[] blocks = session.getGeyser().getWorldManager().getBlocksAt(session, iter);
        for (iter.reset(); iter.hasNext(); iter.next()) {
            BlockCollision blockCollision = BlockUtils.getCollision(blocks[iter.getIteration()]);
            if (blockCollision != null) {
                blockCollision.beforeCorrectPosition(iter.getX(), iter.getY(), iter.getZ(), playerBoundingBox);
            }
        }

        // Main correction code
        for (iter.reset(); iter.hasNext(); iter.next()) {
            BlockCollision blockCollision = BlockUtils.getCollision(blocks[iter.getIteration()]);
            if (blockCollision != null) {
                if (!blockCollision.correctPosition(session, iter.getX(), iter.getY(), iter.getZ(), playerBoundingBox)) {
                    return false;
                }
            }
        }

        updateScaffoldingFlags(true);

        return true;
    }

    public Vector3d correctPlayerMovement(Vector3d movement, boolean checkWorld, boolean teleported) {
        // On the teleported check: see https://github.com/GeyserMC/Geyser/issues/2540
        // As of this commit we don't know how it happens but we don't need to check movement here anyway in that case
        if (teleported || (!checkWorld && session.getPistonCache().getPistons().isEmpty())) { // There is nothing to check
            return movement;
        }
        return correctMovement(movement, playerBoundingBox, session.getPlayerEntity().isOnGround(), PLAYER_STEP_UP, checkWorld, false);
    }

    public Vector3d correctMovement(Vector3d movement, BoundingBox boundingBox, boolean onGround, double stepUp, boolean checkWorld, boolean walkOnLava) {
        Vector3d adjustedMovement = movement;
        if (!movement.equals(Vector3d.ZERO)) {
            adjustedMovement = correctMovementForCollisions(movement, boundingBox, checkWorld, walkOnLava);
        }

        boolean verticalCollision = adjustedMovement.getY() != movement.getY();
        boolean horizontalCollision = adjustedMovement.getX() != movement.getX() || adjustedMovement.getZ() != movement.getZ();
        boolean falling = movement.getY() < 0;
        onGround = onGround || (verticalCollision && falling);
        if (onGround && horizontalCollision) {
            Vector3d horizontalMovement = Vector3d.from(movement.getX(), 0, movement.getZ());
            Vector3d stepUpMovement = correctMovementForCollisions(horizontalMovement.up(stepUp), boundingBox, checkWorld, walkOnLava);

            BoundingBox stretchedBoundingBox = boundingBox.clone();
            stretchedBoundingBox.extend(horizontalMovement);
            double maxStepUp = correctMovementForCollisions(Vector3d.from(0, stepUp, 0), stretchedBoundingBox, checkWorld, walkOnLava).getY();
            if (maxStepUp < stepUp) { // The player collided with a block above them
                BoundingBox stepUpBoundingBox = boundingBox.clone();
                stepUpBoundingBox.translate(0, maxStepUp, 0);

                Vector3d adjustedStepUpMovement = correctMovementForCollisions(horizontalMovement, stepUpBoundingBox, checkWorld, walkOnLava);
                if (squaredHorizontalLength(adjustedStepUpMovement) > squaredHorizontalLength(stepUpMovement)) {
                    stepUpMovement = adjustedStepUpMovement.up(maxStepUp);
                }
            }

            if (squaredHorizontalLength(stepUpMovement) > squaredHorizontalLength(adjustedMovement)) {
                BoundingBox stepUpBoundingBox = boundingBox.clone();
                stepUpBoundingBox.translate(stepUpMovement.getX(), stepUpMovement.getY(), stepUpMovement.getZ());

                // Apply the player's remaining vertical movement
                double verticalMovement = correctMovementForCollisions(Vector3d.from(0, movement.getY() - stepUpMovement.getY(), 0), stepUpBoundingBox, checkWorld, walkOnLava).getY();

                stepUpMovement = stepUpMovement.up(verticalMovement);
                adjustedMovement = stepUpMovement;
            }
        }
        return adjustedMovement;
    }

    private double squaredHorizontalLength(Vector3d vector) {
        return vector.getX() * vector.getX() + vector.getZ() * vector.getZ();
    }

    private Vector3d correctMovementForCollisions(Vector3d movement, BoundingBox boundingBox, boolean checkWorld, boolean walkOnLava) {
        double movementX = movement.getX();
        double movementY = movement.getY();
        double movementZ = movement.getZ();

        // Position might change slightly due to floating point error
        double originalX = boundingBox.getMiddleX();
        double originalY = boundingBox.getMiddleY();
        double originalZ = boundingBox.getMiddleZ();

        BoundingBox movementBoundingBox = boundingBox.clone();
        movementBoundingBox.extend(movement);
        BlockPositionIterator iter = collidableBlocksIterator(movementBoundingBox);
        if (Math.abs(movementY) > CollisionManager.COLLISION_TOLERANCE) {
            movementY = computeCollisionOffset(boundingBox, Axis.Y, movementY, iter, checkWorld, walkOnLava);
            boundingBox.translate(0, movementY, 0);
        }
        boolean checkZFirst = Math.abs(movementZ) > Math.abs(movementX);
        if (checkZFirst && Math.abs(movementZ) > CollisionManager.COLLISION_TOLERANCE) {
            movementZ = computeCollisionOffset(boundingBox, Axis.Z, movementZ, iter, checkWorld, walkOnLava);
            boundingBox.translate(0, 0, movementZ);
        }
        if (Math.abs(movementX) > CollisionManager.COLLISION_TOLERANCE) {
            movementX = computeCollisionOffset(boundingBox, Axis.X, movementX, iter, checkWorld, walkOnLava);
            boundingBox.translate(movementX, 0, 0);
        }
        if (!checkZFirst && Math.abs(movementZ) > CollisionManager.COLLISION_TOLERANCE) {
            movementZ = computeCollisionOffset(boundingBox, Axis.Z, movementZ, iter, checkWorld, walkOnLava);
            boundingBox.translate(0, 0, movementZ);
        }

        boundingBox.setMiddleX(originalX);
        boundingBox.setMiddleY(originalY);
        boundingBox.setMiddleZ(originalZ);

        return Vector3d.from(movementX, movementY, movementZ);
    }

    private double computeCollisionOffset(BoundingBox boundingBox, Axis axis, double offset, BlockPositionIterator iter, boolean checkWorld, boolean walkOnLava) {
        for (iter.reset(); iter.hasNext(); iter.next()) {
            int x = iter.getX();
            int y = iter.getY();
            int z = iter.getZ();
            if (checkWorld) {
                int blockId = session.getGeyser().getWorldManager().getBlockAt(session, x, y, z);

                BlockCollision blockCollision = walkOnLava ? getCollisionLavaWalking(blockId, y, boundingBox) : BlockUtils.getCollision(blockId);
                if (blockCollision != null && !(blockCollision instanceof ScaffoldingCollision)) {
                    offset = blockCollision.computeCollisionOffset(x, y, z, boundingBox, axis, offset);
                }
            }
            offset = session.getPistonCache().computeCollisionOffset(Vector3i.from(x, y, z), boundingBox, axis, offset);
            if (Math.abs(offset) < COLLISION_TOLERANCE) {
                return 0;
            }
        }
        return offset;
    }

    /**
     * @return the block collision appropriate for entities that can walk on lava (Strider)
     */
    public BlockCollision getCollisionLavaWalking(int blockId, int blockY, BoundingBox boundingBox) {
        if (BlockStateValues.getLavaLevel(blockId) == 0 && FLUID_COLLISION.isBelow(blockY, boundingBox)) {
            return FLUID_COLLISION;
        }
        return BlockUtils.getCollision(blockId);
    }

    /**
     * @return if the player is currently in a water block
     */
    public boolean isPlayerInWater() {
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, session.getPlayerEntity().getPosition().toInt());
        return state.is(Blocks.WATER) && state.getValue(Properties.LEVEL) == 0;
    }

    public boolean isWaterInEyes() {
        double eyeX = playerBoundingBox.getMiddleX();
        double eyeY = playerBoundingBox.getMiddleY() - playerBoundingBox.getSizeY() / 2d + session.getEyeHeight();
        double eyeZ = playerBoundingBox.getMiddleZ();

        eyeY -= 1 / ((double) BlockStateValues.NUM_FLUID_LEVELS); // Subtract the height of one water layer
        int blockID = session.getGeyser().getWorldManager().getBlockAt(session, GenericMath.floor(eyeX), GenericMath.floor(eyeY), GenericMath.floor(eyeZ));
        double waterHeight = BlockStateValues.getWaterHeight(blockID);

        return waterHeight != -1 && eyeY < (Math.floor(eyeY) + waterHeight);
    }

    /**
     * Updates scaffolding entity flags
     * Scaffolding needs to be checked per-move since it's a flag in Bedrock but Java does it client-side
     *
     * @param updateMetadata whether we should update metadata if something changed
     */
    public void updateScaffoldingFlags(boolean updateMetadata) {
        Entity entity = session.getPlayerEntity();
        boolean isSneakingWithScaffolding = (touchingScaffolding || onScaffolding) && session.isSneaking();

        entity.setFlag(EntityFlag.OVER_DESCENDABLE_BLOCK, onScaffolding);
        entity.setFlag(EntityFlag.IN_ASCENDABLE_BLOCK, touchingScaffolding);
        entity.setFlag(EntityFlag.OVER_SCAFFOLDING, isSneakingWithScaffolding);

        entity.setFlag(EntityFlag.IN_SCAFFOLDING, touchingScaffolding);

        if (updateMetadata) {
            session.getPlayerEntity().updateBedrockMetadata();
        }
    }
}
