/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.vehicle;

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.SolidCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;

import java.util.Map;

public class VehicleComponent<T extends LivingEntity & ClientVehicle> {
    private static final ObjectDoublePair<Fluid> EMPTY_FLUID_PAIR = ObjectDoublePair.of(Fluid.EMPTY, 0.0);
    private static final float MAX_LOGICAL_FLUID_HEIGHT = 8.0f / BlockStateValues.NUM_FLUID_LEVELS;
    private static final float BASE_SLIPPERINESS_CUBED = 0.6f * 0.6f * 0.6f;
    private static final float MIN_VELOCITY = 0.003f;
    private static final float CLIMB_SPEED = 0.15f;

    private static final Map<String, Vector3f> MOVEMENT_MULTIPLIERS = Map.of(
            "minecraft:cobweb", Vector3f.from(0.25f, 0.05f, 0.25f),
            "minecraft:powder_snow", Vector3f.from(0.9f, 1.5f, 0.9f),
            "minecraft:sweet_berry_bush", Vector3f.from(0.8f, 0.75f, 0.8f)
    );

    protected final T vehicle;
    protected final BoundingBox boundingBox;

    protected float moveSpeed;
    protected int levitation;
    protected boolean slowFalling;

    public VehicleComponent(T vehicle) {
        this.vehicle = vehicle;

        double width = Double.parseDouble(Float.toString(vehicle.getBoundingBoxWidth()));
        double height = Double.parseDouble(Float.toString(vehicle.getBoundingBoxHeight()));
        this.boundingBox = new BoundingBox(
                vehicle.getPosition().getX(),
                vehicle.getPosition().getY() + height / 2,
                vehicle.getPosition().getZ(),
                width, height, width
        );

        this.moveSpeed = GeyserAttributeType.MOVEMENT_SPEED.getDefaultValue();
    }

    public void setWidth(float width) {
        double doubleWidth = Double.parseDouble(Float.toString(width));
        boundingBox.setSizeX(doubleWidth);
        boundingBox.setSizeZ(doubleWidth);
    }

    public void setHeight(float height) {
        double doubleHeight = Double.parseDouble(Float.toString(height));
        boundingBox.translate(0, (doubleHeight - boundingBox.getSizeY()) / 2, 0);
        boundingBox.setSizeY(doubleHeight);
    }

    public void moveAbsolute(double x, double y, double z) {
        boundingBox.setMiddleX(x);
        boundingBox.setMiddleY(y + boundingBox.getSizeY() / 2);
        boundingBox.setMiddleZ(z);
    }

    public void moveRelative(double x, double y, double z) {
        boundingBox.translate(x, y, z);
    }

    public void setEffect(Effect effect, int effectAmplifier) {
        switch (effect) {
            case LEVITATION -> levitation = effectAmplifier + 1;
            case SLOW_FALLING -> slowFalling = true;
        }
    }

    public void removeEffect(Effect effect) {
        switch (effect) {
            case LEVITATION -> levitation = 0;
            case SLOW_FALLING -> slowFalling = false;
        }
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void onDismount() {
        //
    }

    public void tickVehicle() {
        if (!vehicle.isClientControlled()) {
            return;
        }

        ObjectDoublePair<Fluid> fluidHeight = updateFluidMovement();
        switch (fluidHeight.left()) {
            case WATER -> waterMovement();
            case LAVA -> {
                if (vehicle.canWalkOnLava() && BlockStateValues.getFluid(getBlockAt(boundingBox.getBottomCenter().toInt())) == Fluid.LAVA) {
                    landMovement();
                } else {
                    lavaMovement(fluidHeight.rightDouble());
                }
            }
            case EMPTY -> landMovement();
        }
    }

    protected ObjectDoublePair<Fluid> updateFluidMovement() {
        BoundingBox box = boundingBox.clone();
        box.expand(-0.001);

        Vector3d min = box.getMin();
        Vector3d max = box.getMax();

        BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getFloorX(), min.getFloorY(), min.getFloorZ(), max.getFloorX(), max.getFloorY(), max.getFloorZ());
        int[] blocks = vehicle.getSession().getGeyser().getWorldManager().getBlocksAt(vehicle.getSession(), iter);

        double waterHeight = getFluidHeightAndApplyMovement(Fluid.WATER, 0.014, min.getY(), iter, blocks);
        double lavaHeight = getFluidHeightAndApplyMovement(Fluid.LAVA, vehicle.getSession().getDimensionType().ultrawarm() ? 0.007 : 0.007 / 3, min.getY(), iter, blocks);

        if (lavaHeight > 0 && vehicle.getDefinition().entityType() == EntityType.STRIDER) {
            Vector3i blockPos = boundingBox.getBottomCenter().toInt();
            if (!CollisionManager.FLUID_COLLISION.isBelow(blockPos.getY(), boundingBox) || BlockStateValues.getFluid(getBlockAt(blockPos.up())) == Fluid.LAVA) {
                vehicle.setMotion(vehicle.getMotion().mul(0.5f).add(0, 0.05f, 0));
            } else {
                vehicle.setOnGround(true);
            }
        }

        // Water movement has priority over lava movement
        if (waterHeight > 0) {
            return ObjectDoublePair.of(Fluid.WATER, waterHeight);
        }

        if (lavaHeight > 0) {
            return ObjectDoublePair.of(Fluid.LAVA, lavaHeight);
        }

        return EMPTY_FLUID_PAIR;
    }

    protected double getFluidHeightAndApplyMovement(Fluid fluid, double speed, double minY, BlockPositionIterator iter, int[] blocks) {
        Vector3d totalVelocity = Vector3d.ZERO;
        double maxFluidHeight = 0;
        int fluidBlocks = 0;

        for (iter.reset(); iter.hasNext(); iter.next()) {
            int blockId = blocks[iter.getIteration()];
            if (BlockStateValues.getFluid(blockId) != fluid) {
                continue;
            }

            Vector3i blockPos = Vector3i.from(iter.getX(), iter.getY(), iter.getZ());
            float worldFluidHeight = getWorldFluidHeight(fluid, blockId);

            double vehicleFluidHeight = blockPos.getY() + worldFluidHeight - minY;
            if (vehicleFluidHeight < 0) {
                continue;
            }

            boolean flowBlocked = worldFluidHeight != 1; // This is only used for determining if a falling fluid should drag the vehicle downwards
            Vector3d velocity = Vector3d.ZERO;
            for (Direction direction : Direction.HORIZONTAL) {
                Vector3i adjacentBlockPos = blockPos.add(direction.getUnitVector());
                int adjacentBlockId = getBlockAt(adjacentBlockPos);
                Fluid adjacentFluid = BlockStateValues.getFluid(adjacentBlockId);

                float fluidHeightDiff = 0;
                if (adjacentFluid == fluid) {
                    fluidHeightDiff = getLogicalFluidHeight(fluid, blockId) - getLogicalFluidHeight(fluid, adjacentBlockId);
                } else if (adjacentFluid == Fluid.EMPTY) {
                    // If the adjacent block is not a fluid and does not have collision,
                    // check if there is a fluid under it
                    BlockCollision adjacentBlockCollision = BlockUtils.getCollision(adjacentBlockId);
                    if (adjacentBlockCollision == null) {
                        float adjacentFluidHeight = getLogicalFluidHeight(fluid, getBlockAt(adjacentBlockPos.add(Direction.DOWN.getUnitVector())));
                        if (adjacentFluidHeight != -1) { // Only care about same type of fluid
                            fluidHeightDiff = getLogicalFluidHeight(fluid, blockId) - (adjacentFluidHeight - MAX_LOGICAL_FLUID_HEIGHT);
                        }
                    } else if (!flowBlocked) {
                        flowBlocked = isFlowBlocked(fluid, adjacentBlockId);
                    }
                }

                if (fluidHeightDiff != 0) {
                    velocity = velocity.add(direction.getUnitVector().toDouble().mul(fluidHeightDiff));
                }
            }

            if (worldFluidHeight == 1) { // If falling fluid
                // If flow is not blocked, check if it is blocked for the fluid above
                if (!flowBlocked) {
                    Vector3i blockPosUp = blockPos.up();
                    for (Direction direction : Direction.HORIZONTAL) {
                        flowBlocked = isFlowBlocked(fluid, getBlockAt(blockPosUp.add(direction.getUnitVector())));
                        if (flowBlocked) {
                            break;
                        }
                    }
                }

                if (flowBlocked) {
                    velocity = javaNormalize(velocity).add(0.0, -6.0, 0.0);
                }
            }

            velocity = javaNormalize(velocity);

            maxFluidHeight = Math.max(vehicleFluidHeight, maxFluidHeight);
            if (maxFluidHeight < 0.4) {
                velocity = velocity.mul(maxFluidHeight);
            }

            totalVelocity = totalVelocity.add(velocity);
            fluidBlocks++;
        }

        if (!totalVelocity.equals(Vector3d.ZERO)) {
            Vector3f motion = vehicle.getMotion();

            totalVelocity = javaNormalize(totalVelocity.mul(1.0 / fluidBlocks));
            totalVelocity = totalVelocity.mul(speed);

            if (totalVelocity.length() < 0.0045 && Math.abs(motion.getX()) < MIN_VELOCITY && Math.abs(motion.getZ()) < MIN_VELOCITY) {
                totalVelocity = javaNormalize(totalVelocity).mul(0.0045);
            }

            vehicle.setMotion(motion.add(totalVelocity.toFloat()));
        }

        return maxFluidHeight;
    }

    /**
     * Java edition returns the zero vector if the length of the input vector is less than 0.0001
     */
    protected Vector3d javaNormalize(Vector3d vec) {
        double len = vec.length();
        return len < 1.0E-4 ? Vector3d.ZERO : Vector3d.from(vec.getX() / len, vec.getY() / len, vec.getZ() / len);
    }

    protected int getBlockAt(Vector3i blockPos) {
        return vehicle.getSession().getGeyser().getWorldManager().getBlockAt(vehicle.getSession(), blockPos);
    }

    private float getWorldFluidHeight(Fluid fluidType, int blockId) {
        return (float) switch (fluidType) {
            case WATER -> BlockStateValues.getWaterHeight(blockId);
            case LAVA -> BlockStateValues.getLavaHeight(blockId);
            case EMPTY -> -1;
        };
    }

    private float getLogicalFluidHeight(Fluid fluidType, int blockId) {
        return Math.min(getWorldFluidHeight(fluidType, blockId), MAX_LOGICAL_FLUID_HEIGHT);
    }

    private boolean isFlowBlocked(Fluid fluid, int adjacentBlockId) {
        if (adjacentBlockId == BlockStateValues.JAVA_ICE_ID) {
            return false;
        }

        if (BlockStateValues.getFluid(adjacentBlockId) == fluid) {
            return false;
        }

        // TODO: supposed to check if the opposite face of the block touching the fluid is solid, instead of SolidCollision
        return BlockUtils.getCollision(adjacentBlockId) instanceof SolidCollision;
    }

    protected void waterMovement() {
        float gravity = getGravity();
        float drag = vehicle.getFlag(EntityFlag.SPRINTING) ? 0.9f : 0.8f; // 0.8f: getBaseMovementSpeedMultiplier
        double originalY = boundingBox.getBottomCenter().getY();
        boolean falling = vehicle.getMotion().getY() <= 0;

        // NOT IMPLEMENTED: depth strider and dolphins grace

        boolean horizontalCollision = travel(0.02f);
        if (horizontalCollision && isClimbing()) {
            vehicle.setMotion(Vector3f.from(vehicle.getMotion().getX(), 0.2f, vehicle.getMotion().getZ()));
        }

        vehicle.setMotion(vehicle.getMotion().mul(drag, 0.8f, drag));
        vehicle.setMotion(getFluidGravity(gravity, falling));

        if (horizontalCollision && shouldApplyFluidJumpBoost(originalY)) {
            vehicle.setMotion(Vector3f.from(vehicle.getMotion().getX(), 0.3f, vehicle.getMotion().getZ()));
        }
    }

    protected void lavaMovement(double lavaHeight) {
        float gravity = getGravity();
        double originalY = boundingBox.getBottomCenter().getY();
        boolean falling = vehicle.getMotion().getY() <= 0;

        boolean horizontalCollision = travel(0.02f);

        if (lavaHeight <= (boundingBox.getSizeY() * 0.85 < 0.4 ? 0.0 : 0.4)) { // Swim height
            vehicle.setMotion(vehicle.getMotion().mul(0.5f, 0.8f, 0.5f));
            vehicle.setMotion(getFluidGravity(gravity, falling));
        } else {
            vehicle.setMotion(vehicle.getMotion().mul(0.5f));
        }

        vehicle.setMotion(vehicle.getMotion().down(gravity / 4.0f));

        if (horizontalCollision && shouldApplyFluidJumpBoost(originalY)) {
            vehicle.setMotion(Vector3f.from(vehicle.getMotion().getX(), 0.3f, vehicle.getMotion().getZ()));
        }
    }

    protected void landMovement() {
        float gravity = getGravity();
        float slipperiness = BlockStateValues.getSlipperiness(getBlockAt(getVelocityAffectingPos()));
        float drag = vehicle.isOnGround() ? 0.91f * slipperiness : 0.91f;
        float speed = vehicle.getVehicleSpeed() * (vehicle.isOnGround() ? BASE_SLIPPERINESS_CUBED / (slipperiness * slipperiness * slipperiness) : 0.1f);

        boolean horizontalCollision = travel(speed);
        if (isClimbing()) {
            vehicle.setMotion(getClimbingSpeed(horizontalCollision));
            // NOT IMPLEMENTED: climbing in powdered snow
        }

        if (levitation > 0) {
            vehicle.setMotion(vehicle.getMotion().up((0.05f * levitation - vehicle.getMotion().getY()) * 0.2f));
        } else {
            vehicle.setMotion(vehicle.getMotion().down(gravity));
            // NOT IMPLEMENTED: slow fall when in unloaded chunk
        }

        vehicle.setMotion(vehicle.getMotion().mul(drag, 0.98f, drag));
    }

    protected boolean shouldApplyFluidJumpBoost(double originalY) {
        BoundingBox box = boundingBox.clone();
        box.translate(vehicle.getMotion().toDouble().up(0.6f - boundingBox.getBottomCenter().getY() + originalY));
        box.expand(-1.0E-7);

        BlockPositionIterator iter = vehicle.getSession().getCollisionManager().collidableBlocksIterator(box);
        int[] blocks = vehicle.getSession().getGeyser().getWorldManager().getBlocksAt(vehicle.getSession(), iter);

        for (iter.reset(); iter.hasNext(); iter.next()) {
            int blockId = blocks[iter.getIteration()];

            // Also check for fluids
            BlockCollision blockCollision = BlockUtils.getCollision(blockId);
            if (blockCollision == null && BlockStateValues.getFluid(blockId) != Fluid.EMPTY) {
                blockCollision = CollisionManager.SOLID_COLLISION;
            }

            if (blockCollision != null && blockCollision.checkIntersection(iter.getX(), iter.getY(), iter.getZ(), box)) {
                return false;
            }
        }

        return true;
    }

    protected Vector3f getClimbingSpeed(boolean horizontalCollision) {
        Vector3f motion = vehicle.getMotion();
        return Vector3f.from(
                MathUtils.clamp(motion.getX(), -CLIMB_SPEED, CLIMB_SPEED),
                horizontalCollision ? 0.2f : Math.max(motion.getY(), -CLIMB_SPEED),
                MathUtils.clamp(motion.getZ(), -CLIMB_SPEED, CLIMB_SPEED)
        );
    }

    protected Vector3f getFluidGravity(float gravity, boolean falling) {
        Vector3f motion = vehicle.getMotion();
        if (vehicle.getFlag(EntityFlag.HAS_GRAVITY) && !vehicle.getFlag(EntityFlag.SPRINTING)) {
            float newY = motion.getY() - gravity / 16;
            if (falling && Math.abs(motion.getY() - 0.005f) >= MIN_VELOCITY && Math.abs(newY) < MIN_VELOCITY) {
                newY = -MIN_VELOCITY;
            }
            return Vector3f.from(motion.getX(), newY, motion.getZ());
        }
        return motion;
    }

    protected @Nullable Vector3f getBlockMovementMultiplier() {
        BoundingBox box = boundingBox.clone();
        box.expand(-1.0E-7);

        Vector3i min = box.getMin().toInt();
        Vector3i max = box.getMax().toInt();

        BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        int[] blocks = vehicle.getSession().getGeyser().getWorldManager().getBlocksAt(vehicle.getSession(), iter);

        // Iterate backwards
        for (int i = blocks.length - 1; i >= 0; i--) {
            String cleanIdentifier = BlockRegistries.JAVA_BLOCKS.getOrDefault(blocks[i], BlockMapping.DEFAULT).getCleanJavaIdentifier();
            Vector3f multiplier = MOVEMENT_MULTIPLIERS.get(cleanIdentifier);

            if (multiplier != null) {
                return multiplier;
            }
        }

        return null;
    }

    protected void applyBlockCollisionEffects() {
        BoundingBox box = boundingBox.clone();
        box.expand(-1.0E-7);

        Vector3i min = box.getMin().toInt();
        Vector3i max = box.getMax().toInt();

        BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        int[] blocks = vehicle.getSession().getGeyser().getWorldManager().getBlocksAt(vehicle.getSession(), iter);

        for (iter.reset(); iter.hasNext(); iter.next()) {
            int blockId = blocks[iter.getIteration()];

            if (BlockStateValues.JAVA_HONEY_BLOCK_ID == blockId) {
                onHoneyBlockCollision();
            } else if (BlockStateValues.JAVA_BUBBLE_COLUMN_DRAG_ID == blockId) {
                onBubbleColumnCollision(true);
            } else if (BlockStateValues.JAVA_BUBBLE_COLUMN_UPWARD_ID == blockId) {
                onBubbleColumnCollision(false);
            }
        }
    }

    protected void onHoneyBlockCollision() {
        if (vehicle.isOnGround() || vehicle.getMotion().getY() >= -0.08f) {
            return;
        }

        // NOT IMPLEMENTED: don't slide if inside the honey block
        Vector3f motion = vehicle.getMotion();
        float mul = motion.getY() < -0.13f ? -0.05f / motion.getY() : 1;
        vehicle.setMotion(Vector3f.from(motion.getX() * mul, -0.05f, motion.getZ() * mul));
    }

    protected void onBubbleColumnCollision(boolean drag) {
        Vector3f motion = vehicle.getMotion();
        vehicle.setMotion(Vector3f.from(
                motion.getX(),
                drag ? Math.max(-0.3f, motion.getY() - 0.03f) : Math.min(0.7f, motion.getY() + 0.06f),
                motion.getZ()
        ));
    }

    /**
     * @return True if there was a horizontal collision
     */
    protected boolean travel(float speed) {
        Vector3f motion = vehicle.getMotion();

        // Java only does this client side
        motion = motion.mul(0.98f);

        motion = Vector3f.from(
                Math.abs(motion.getX()) < MIN_VELOCITY ? 0 : motion.getX(),
                Math.abs(motion.getY()) < MIN_VELOCITY ? 0 : motion.getY(),
                Math.abs(motion.getZ()) < MIN_VELOCITY ? 0 : motion.getZ()
        );

        // TODO: isImmobile? set input to 0 and jump to false

        motion = motion.add(getInputVelocity(speed));

        Vector3f movementMultiplier = getBlockMovementMultiplier();
        if (movementMultiplier != null) {
            motion = motion.mul(movementMultiplier);
        }

        Vector3d correctedMovement = vehicle.getSession().getCollisionManager().correctMovement(
                motion.toDouble(), boundingBox, vehicle.isOnGround(), vehicle.getStepHeight(), true, vehicle.canWalkOnLava()
        );

        boundingBox.translate(correctedMovement);
        Vector3d newPos = boundingBox.getBottomCenter();

        // Non-zero values indicate a collision on that axis
        Vector3d moveDiff = motion.toDouble().sub(correctedMovement);

        boolean onGround = moveDiff.getY() != 0 && motion.getY() < 0;
        boolean horizontalCollision = moveDiff.getX() != 0 || moveDiff.getZ() != 0;

        boolean bounced = false;
        if (onGround) {
            Vector3i landingPos = newPos.sub(0, 0.2f, 0).toInt();
            int landingBlockId = getBlockAt(landingPos);

            if (landingBlockId == BlockStateValues.JAVA_SLIME_BLOCK_ID) {
                motion = Vector3f.from(motion.getX(), -motion.getY(), motion.getZ());
                bounced = true;

                // Slow horizontal movement
                float absY = Math.abs(motion.getY());
                if (absY < 0.1f) {
                    float mul = 0.4f + absY * 0.2f;
                    motion = motion.mul(mul, 1.0f, mul);
                }
            } else if (BlockStateValues.getBedColor(landingBlockId) != -1) { // If bed
                motion = Vector3f.from(motion.getX(), -motion.getY() * 0.66f, motion.getZ());
                bounced = true;
            }
        }

        // Set motion to 0 if a movement multiplier was used, else set to 0 on each axis with a collision
        if (movementMultiplier != null) {
            motion = Vector3f.ZERO;
        } else {
            motion = motion.mul(
                    moveDiff.getX() == 0 ? 1 : 0,
                    moveDiff.getY() == 0 || bounced ? 1 : 0,
                    moveDiff.getZ() == 0 ? 1 : 0
            );
        }

        // Send the new position to the bedrock client and java server
        moveVehicle(newPos, onGround);
        vehicle.setMotion(motion);

        applyBlockCollisionEffects();

        float velocityMultiplier = getVelocityMultiplier();
        vehicle.setMotion(vehicle.getMotion().mul(velocityMultiplier, 1.0f, velocityMultiplier));

        return horizontalCollision;
    }

    protected boolean isClimbing() {
        if (!vehicle.canClimb()) {
            return false;
        }

        Vector3i blockPos = boundingBox.getBottomCenter().toInt();
        int blockId = getBlockAt(blockPos);

        if (BlockStateValues.isClimbable(blockId)) {
            return true;
        }

        // Check if the vehicle is in an open trapdoor with a ladder of the same direction under it
        Direction openTrapdoorDirection = BlockStateValues.getOpenTrapdoorDirection(blockId);
        if (openTrapdoorDirection != null) {
            Direction ladderDirection = BlockStateValues.getLadderDirection(getBlockAt(blockPos.down()));
            return ladderDirection != null && ladderDirection == openTrapdoorDirection;
        }

        return false;
    }

    protected Vector2f normalizeInput(Vector2f input) {
        float lenSquared = input.lengthSquared();
        if (lenSquared < 1.0E-7) {
            return Vector2f.ZERO;
        } else if (lenSquared > 1.0) {
            return input.normalize();
        }
        return input;
    }

    protected Vector3f getInputVelocity(float speed) {
        Vector2f input = vehicle.getSession().getPlayerEntity().getVehicleInput();
        input = input.mul(0.98f);
        input = vehicle.getAdjustedInput(input);
        input = normalizeInput(input);
        input = input.mul(speed);

        float yaw = vehicle.getSession().getPlayerEntity().getYaw();
        float sin = TrigMath.sin(yaw * TrigMath.DEG_TO_RAD);
        float cos = TrigMath.cos(yaw * TrigMath.DEG_TO_RAD);
        return Vector3f.from(input.getX() * cos - input.getY() * sin, 0, input.getY() * cos + input.getX() * sin);
    }

    protected void moveVehicle(Vector3d javaPos, boolean isOnGround) {
        Vector3f bedrockPos = javaPos.toFloat();
        float yaw = vehicle.getSession().getPlayerEntity().getYaw();
        float pitch = vehicle.getSession().getPlayerEntity().getPitch() * 0.5f;

        MoveEntityDeltaPacket moveEntityDeltaPacket = new MoveEntityDeltaPacket();
        moveEntityDeltaPacket.setRuntimeEntityId(vehicle.getGeyserId());

        if (isOnGround) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
        }
        vehicle.setOnGround(isOnGround);

        if (vehicle.getPosition().getX() != bedrockPos.getX()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            moveEntityDeltaPacket.setX(bedrockPos.getX());
        }
        if (vehicle.getPosition().getY() != bedrockPos.getY()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            moveEntityDeltaPacket.setY(bedrockPos.getY());
        }
        if (vehicle.getPosition().getZ() != bedrockPos.getZ()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
            moveEntityDeltaPacket.setZ(bedrockPos.getZ());
        }
        vehicle.setPosition(bedrockPos);

        if (vehicle.getYaw() != yaw) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            moveEntityDeltaPacket.setYaw(yaw);
            vehicle.setYaw(yaw);
        }
        if (vehicle.getPitch() != pitch) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
            moveEntityDeltaPacket.setPitch(pitch);
            vehicle.setPitch(pitch);
        }
        if (vehicle.getHeadYaw() != yaw) { // Same as yaw
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
            moveEntityDeltaPacket.setHeadYaw(yaw);
            vehicle.setHeadYaw(yaw);
        }

        if (!moveEntityDeltaPacket.getFlags().isEmpty()) {
            vehicle.getSession().sendUpstreamPacket(moveEntityDeltaPacket);
        }

        ServerboundMoveVehiclePacket moveVehiclePacket = new ServerboundMoveVehiclePacket(javaPos.getX(), javaPos.getY(), javaPos.getZ(), yaw, pitch);
        vehicle.getSession().sendDownstreamPacket(moveVehiclePacket);
        vehicle.getSession().setLastVehicleMoveTimestamp(System.currentTimeMillis());
    }

    protected float getGravity() {
        if (!vehicle.getFlag(EntityFlag.HAS_GRAVITY)) {
            return 0;
        }

        if (vehicle.getMotion().getY() <= 0 && slowFalling) {
            return 0.01f;
        }

        return 0.08f;
    }

    protected @Nullable Vector3i getSupportingBlockPos() {
        Vector3i result = null;

        if (vehicle.isOnGround()) {
            Vector3d bottomCenter = boundingBox.getBottomCenter();

            BoundingBox box = boundingBox.clone();
            box.extend(0, -1.0E-6, 0); // Extend slightly down

            Vector3i min = box.getMin().toInt();
            Vector3i max = box.getMax().toInt();

            // Use minY as maxY
            BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getX(), min.getY(), min.getZ(), max.getX(), min.getY(), max.getZ());
            int[] blocks = vehicle.getSession().getGeyser().getWorldManager().getBlocksAt(vehicle.getSession(), iter);

            double minDistance = Double.MAX_VALUE;
            for (iter.reset(); iter.hasNext(); iter.next()) {
                Vector3i blockPos = Vector3i.from(iter.getX(), iter.getY(), iter.getZ());
                int blockId = blocks[iter.getIteration()];

                BlockCollision blockCollision;
                if (vehicle.canWalkOnLava()) {
                    blockCollision = vehicle.getSession().getCollisionManager().getCollisionLavaWalking(blockId, blockPos.getY(), boundingBox);
                } else {
                    blockCollision = BlockUtils.getCollision(blockId);
                }

                if (blockCollision != null && blockCollision.checkIntersection(blockPos, box)) {
                    double distance = bottomCenter.distanceSquared(blockPos.toDouble().add(0.5f, 0.5f, 0.5f));
                    if (distance <= minDistance) {
                        minDistance = distance;
                        result = blockPos;
                    }
                }
            }
        }

        return result;
    }

    protected Vector3i getVelocityAffectingPos() {
        Vector3i blockPos = getSupportingBlockPos();
        if (blockPos != null) {
            return Vector3i.from(blockPos.getX(), Math.floor(boundingBox.getBottomCenter().getY() - 0.500001f), blockPos.getZ());
        } else {
            return vehicle.getPosition().sub(0, 0.500001f, 0).toInt();
        }
    }

    protected float getVelocityMultiplier() {
        int blockId = getBlockAt(boundingBox.getBottomCenter().toInt());
        if (BlockStateValues.getWaterLevel(blockId) != -1 // getWaterLevel does not include waterlogged blocks
                || blockId == BlockStateValues.JAVA_BUBBLE_COLUMN_DRAG_ID
                || blockId == BlockStateValues.JAVA_BUBBLE_COLUMN_UPWARD_ID) {
            return 1.0f;
        }

        if (blockId == BlockStateValues.JAVA_SOUL_SAND_ID || blockId == BlockStateValues.JAVA_HONEY_BLOCK_ID) {
            return 0.4f;
        }

        blockId = getBlockAt(getVelocityAffectingPos());
        if (blockId == BlockStateValues.JAVA_SOUL_SAND_ID || blockId == BlockStateValues.JAVA_HONEY_BLOCK_ID) {
            return 0.4f;
        }

        return 1.0f;
    }

    protected float getJumpVelocityMultiplier() {
        int blockId = getBlockAt(boundingBox.getBottomCenter().toInt());
        if (blockId == BlockStateValues.JAVA_HONEY_BLOCK_ID) {
            return 0.5f;
        }

        blockId = getBlockAt(getVelocityAffectingPos());
        if (blockId == BlockStateValues.JAVA_HONEY_BLOCK_ID) {
            return 0.5f;
        }

        return 1.0f;
    }
}
