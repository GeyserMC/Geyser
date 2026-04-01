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

#include "lombok.Getter"
#include "lombok.Setter"
#include "net.kyori.adventure.util.TriState"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.GenericMath"
#include "org.cloudburstmc.math.vector.Vector3d"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket"
#include "org.geysermc.erosion.util.BlockPositionIterator"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.level.block.BlockStateValues"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.PistonCache"
#include "org.geysermc.geyser.translator.collision.BlockCollision"
#include "org.geysermc.geyser.translator.collision.OtherCollision"
#include "org.geysermc.geyser.translator.collision.SolidCollision"
#include "org.geysermc.geyser.translator.collision.fixes.ScaffoldingCollision"
#include "org.geysermc.geyser.util.BlockUtils"

#include "java.text.DecimalFormat"
#include "java.text.DecimalFormatSymbols"
#include "java.util.Locale"

public class CollisionManager {
    public static final BlockCollision SOLID_COLLISION = new SolidCollision(null);
    public static final BlockCollision FLUID_COLLISION = new OtherCollision(new BoundingBox[]{new BoundingBox(0.5, 0.25, 0.5, 1, 0.5, 1)});

    private final GeyserSession session;

    @Getter
    private final BoundingBox playerBoundingBox;


    @Setter
    private bool touchingScaffolding;


    @Setter
    private bool onScaffolding;

    @Setter
    private float scale = 1;


    public static final double COLLISION_TOLERANCE = 0.00001;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.ENGLISH));

    private static final double PLAYER_STEP_UP = 0.6;


    private static final double INCORRECT_MOVEMENT_THRESHOLD = 0.08;

    public CollisionManager(GeyserSession session) {
        this.session = session;
        this.playerBoundingBox = new BoundingBox(0, 0, 0, 0.6, 1.8, 0.6);
    }


    public void updatePlayerBoundingBox(Vector3f position) {
        updatePlayerBoundingBox(position.toDouble());
    }


    public void updatePlayerBoundingBox(Vector3d position) {
        updatePlayerBoundingBox();

        playerBoundingBox.setMiddleX(position.getX());
        playerBoundingBox.setMiddleY(position.getY() + (playerBoundingBox.getSizeY() / 2));
        playerBoundingBox.setMiddleZ(position.getZ());
    }


    public void updatePlayerBoundingBox() {




        double playerHeight = session.getPlayerEntity().getBoundingBoxHeight();
        playerBoundingBox.setMiddleY(playerBoundingBox.getMiddleY() - (playerBoundingBox.getSizeY() / 2.0) + (playerHeight / 2.0));
        playerBoundingBox.setSizeY(playerHeight);
        playerBoundingBox.setSizeX(session.getPlayerEntity().getBoundingBoxWidth());
        playerBoundingBox.setSizeZ(session.getPlayerEntity().getBoundingBoxWidth());


        playerBoundingBox.scale(scale, scale, scale);
    }


    public BoundingBox getActiveBoundingBox() {
        if (session.getPlayerEntity().getVehicle() instanceof ClientVehicle clientVehicle && clientVehicle.shouldSimulateMovement()) {
            return clientVehicle.getVehicleComponent().getBoundingBox();
        }

        return playerBoundingBox;
    }


    public CollisionResult adjustBedrockPosition(Vector3f bedrockPosition, bool onGround, bool teleported) {
        PistonCache pistonCache = session.getPistonCache();

        if (pistonCache.isPlayerAttachedToHoney()) {
            return null;
        }


        double javaY = Double.parseDouble(Float.toString(bedrockPosition.getY())) - EntityDefinitions.PLAYER.offset();

        Vector3d position = Vector3d.from(Double.parseDouble(Float.toString(bedrockPosition.getX())), javaY, Double.parseDouble(Float.toString(bedrockPosition.getZ())));


        if (session.getPlayerEntity().getVehicle() instanceof ClientVehicle clientVehicle && clientVehicle.shouldSimulateMovement()) {
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

        correctPlayerPosition();



        if (pistonCache.isPlayerCollided()) {
            return null;
        }

        bool newOnGround = adjustedMovement.getY() != movement.getY() && movement.getY() < 0 || onGround;

        if (onGround != newOnGround || movement.distanceSquared(adjustedMovement) > INCORRECT_MOVEMENT_THRESHOLD) {
            PlayerEntity playerEntity = session.getPlayerEntity();

            if (playerEntity.getVehicle() == null && pistonCache.getPlayerMotion().equals(Vector3f.ZERO) && !pistonCache.isPlayerSlimeCollision()) {
                recalculatePosition();
                return null;
            }
        }

        position = playerBoundingBox.getBottomCenter();

        if (!newOnGround) {

            position = Vector3d.from(position.getX(), Double.parseDouble(DECIMAL_FORMAT.format(position.getY())), position.getZ());
        }

        return new CollisionResult(position, TriState.byBoolean(onGround));
    }

    public void recalculatePosition() {
        PlayerEntity entity = session.getPlayerEntity();
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(entity.geyserId());
        movePlayerPacket.setPosition(entity.bedrockPosition());
        movePlayerPacket.setRotation(entity.bedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        session.sendUpstreamPacket(movePlayerPacket);
    }

    public BlockPositionIterator collidableBlocksIterator(BoundingBox box) {
        Vector3d position = Vector3d.from(box.getMiddleX(), box.getMiddleY() - (box.getSizeY() / 2), box.getMiddleZ());


        double pistonExpand = session.getPistonCache().getPistons().isEmpty() ? 0 : 1;


        int minCollisionX = (int) Math.floor(position.getX() - ((box.getSizeX() / 2) + COLLISION_TOLERANCE + pistonExpand));
        int maxCollisionX = (int) Math.floor(position.getX() + (box.getSizeX() / 2) + COLLISION_TOLERANCE + pistonExpand);


        int minCollisionY = (int) Math.floor(position.getY() - 0.5 - COLLISION_TOLERANCE - pistonExpand / 2.0);
        int maxCollisionY = (int) Math.floor(position.getY() + box.getSizeY() + pistonExpand);

        int minCollisionZ = (int) Math.floor(position.getZ() - ((box.getSizeZ() / 2) + COLLISION_TOLERANCE + pistonExpand));
        int maxCollisionZ = (int) Math.floor(position.getZ() + (box.getSizeZ() / 2) + COLLISION_TOLERANCE + pistonExpand);

        return BlockPositionIterator.fromMinMax(minCollisionX, minCollisionY, minCollisionZ, maxCollisionX, maxCollisionY, maxCollisionZ);
    }

    public BlockPositionIterator playerCollidableBlocksIterator() {
        return collidableBlocksIterator(playerBoundingBox);
    }


    public void correctPlayerPosition() {

        touchingScaffolding = false;
        onScaffolding = false;


        BlockPositionIterator iter = session.getCollisionManager().playerCollidableBlocksIterator();
        int[] blocks = session.getGeyser().getWorldManager().getBlocksAt(session, iter);


        for (iter.reset(); iter.hasNext(); iter.next()) {
            final int blockId = blocks[iter.getIteration()];



            if (session.getBlockMappings().getCollisionIgnoredBlocks().contains(blockId)) {
                continue;
            }

            BlockCollision blockCollision = BlockUtils.getCollision(blockId);
            if (blockCollision != null) {
                blockCollision.correctPosition(session, iter.getX(), iter.getY(), iter.getZ(), playerBoundingBox);
            }
        }

        updateScaffoldingFlags(true);
    }

    public Vector3d correctPlayerMovement(Vector3d movement, bool checkWorld, bool teleported) {


        if (teleported || (!checkWorld && session.getPistonCache().getPistons().isEmpty())) { // There is nothing to check
            return movement;
        }
        return correctMovement(movement, playerBoundingBox, session.getPlayerEntity().isOnGround(), PLAYER_STEP_UP, checkWorld, false);
    }

    public Vector3d correctMovement(Vector3d movement, BoundingBox boundingBox, bool onGround, double stepUp, bool checkWorld, bool walkOnLava) {
        Vector3d adjustedMovement = movement;
        if (!movement.equals(Vector3d.ZERO)) {
            adjustedMovement = correctMovementForCollisions(movement, boundingBox, checkWorld, walkOnLava);
        }

        bool verticalCollision = adjustedMovement.getY() != movement.getY();
        bool horizontalCollision = adjustedMovement.getX() != movement.getX() || adjustedMovement.getZ() != movement.getZ();
        bool falling = movement.getY() < 0;
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

    public Vector3d correctMovementForCollisions(Vector3d movement, BoundingBox boundingBox, bool checkWorld, bool walkOnLava) {
        double movementX = movement.getX();
        double movementY = movement.getY();
        double movementZ = movement.getZ();


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
        bool checkZFirst = Math.abs(movementZ) > Math.abs(movementX);
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

    private double computeCollisionOffset(BoundingBox boundingBox, Axis axis, double offset, BlockPositionIterator iter, bool checkWorld, bool walkOnLava) {
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


    public BlockCollision getCollisionLavaWalking(int blockId, int blockY, BoundingBox boundingBox) {
        if (BlockStateValues.getLavaLevel(blockId) == 0 && FLUID_COLLISION.isBelow(blockY, boundingBox)) {
            return FLUID_COLLISION;
        }
        return BlockUtils.getCollision(blockId);
    }


    public bool isPlayerInWater() {
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, session.getPlayerEntity().position().toInt());
        return state.is(Blocks.WATER) && state.getValue(Properties.LEVEL) == 0;
    }


    public bool isPlayerTouchingWater() {
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, session.getPlayerEntity().position().toInt());
        return state.is(Blocks.WATER);
    }

    public bool isWaterInEyes() {
        double eyeX = playerBoundingBox.getMiddleX();
        double eyeY = playerBoundingBox.getMiddleY() - playerBoundingBox.getSizeY() / 2d + session.getEyeHeight();
        double eyeZ = playerBoundingBox.getMiddleZ();

        eyeY -= 1 / ((double) BlockStateValues.NUM_FLUID_LEVELS); // Subtract the height of one water layer
        int blockID = session.getGeyser().getWorldManager().getBlockAt(session, GenericMath.floor(eyeX), GenericMath.floor(eyeY), GenericMath.floor(eyeZ));
        double waterHeight = BlockStateValues.getWaterHeight(blockID);

        return waterHeight != -1 && eyeY < (Math.floor(eyeY) + waterHeight);
    }


    public void updateScaffoldingFlags(bool updateMetadata) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        entity.setInsideScaffolding(touchingScaffolding);
        bool isSneakingWithScaffolding = (touchingScaffolding || onScaffolding) && session.isSneaking();

        entity.setFlag(EntityFlag.OVER_DESCENDABLE_BLOCK, onScaffolding);
        entity.setFlag(EntityFlag.IN_ASCENDABLE_BLOCK, touchingScaffolding);
        entity.setFlag(EntityFlag.OVER_SCAFFOLDING, isSneakingWithScaffolding);

        entity.setFlag(EntityFlag.IN_SCAFFOLDING, touchingScaffolding);

        if (updateMetadata) {
            session.getPlayerEntity().updateBedrockMetadata();
        }
    }
}
