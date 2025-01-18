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
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BedBlock;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.TrapDoorBlock;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.SolidCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;

public class VehicleComponent<T extends LivingEntity & ClientVehicle> {
    private static final ObjectDoublePair<Fluid> EMPTY_FLUID_PAIR = ObjectDoublePair.of(Fluid.EMPTY, 0.0);
    private static final float MAX_LOGICAL_FLUID_HEIGHT = 8.0f / BlockStateValues.NUM_FLUID_LEVELS;
    private static final float BASE_SLIPPERINESS_CUBED = 0.6f * 0.6f * 0.6f;
    private static final float MIN_VELOCITY = 0.003f;

    protected final T vehicle;
    protected final BoundingBox boundingBox;

    protected float stepHeight;
    protected float moveSpeed;
    protected double gravity;
    protected int effectLevitation;
    protected boolean effectSlowFalling;
    protected boolean effectWeaving;

    public VehicleComponent(T vehicle, float stepHeight) {
        this.vehicle = vehicle;
        this.stepHeight = stepHeight;
        this.moveSpeed = (float) AttributeType.Builtin.MOVEMENT_SPEED.getDef();
        this.gravity = AttributeType.Builtin.GRAVITY.getDef();

        double width = vehicle.getBoundingBoxWidth();
        double height = vehicle.getBoundingBoxHeight();
        this.boundingBox = new BoundingBox(
                vehicle.getPosition().getX(),
                vehicle.getPosition().getY() + height / 2,
                vehicle.getPosition().getZ(),
                width, height, width
        );
    }

    public void setWidth(float width) {
        boundingBox.setSizeX(width);
        boundingBox.setSizeZ(width);
    }

    public void setHeight(float height) {
        boundingBox.translate(0, (height - boundingBox.getSizeY()) / 2, 0);
        boundingBox.setSizeY(height);
    }

    public void moveAbsolute(double x, double y, double z) {
        boundingBox.setMiddleX(x);
        boundingBox.setMiddleY(y + boundingBox.getSizeY() / 2);
        boundingBox.setMiddleZ(z);
    }

    public void moveAbsolute(Vector3d vec) {
        moveAbsolute(vec.getX(), vec.getY(), vec.getZ());
    }

    public void moveRelative(double x, double y, double z) {
        boundingBox.translate(x, y, z);
    }

    public void moveRelative(Vector3d vec) {
        boundingBox.translate(vec);
    }

    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public void setEffect(Effect effect, int effectAmplifier) {
        switch (effect) {
            case LEVITATION -> effectLevitation = effectAmplifier + 1;
            case SLOW_FALLING -> effectSlowFalling = true;
            case WEAVING -> effectWeaving = true;
        }
    }

    public void removeEffect(Effect effect) {
        switch (effect) {
            case LEVITATION -> effectLevitation = 0;
            case SLOW_FALLING -> effectSlowFalling = false;
            case WEAVING -> effectWeaving = false;
        }
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setStepHeight(float stepHeight) {
        this.stepHeight = MathUtils.clamp(stepHeight, 1.0f, 10.0f);
    }

    public void setGravity(double gravity) {
        this.gravity = MathUtils.constrain(gravity, -1.0, 1.0);
    }

    public Vector3d correctMovement(Vector3d movement) {
        return vehicle.getSession().getCollisionManager().correctMovement(
            movement, boundingBox, vehicle.isOnGround(), this.stepHeight, true, vehicle.canWalkOnLava()
        );
    }

    public void onMount() {
        vehicle.getSession().getPlayerEntity().setVehicleInput(Vector2f.ZERO);
        vehicle.getSession().getPlayerEntity().setVehicleJumpStrength(0);
    }

    public void onDismount() {
        //
    }

    /**
     * Called every session tick while the player is mounted on the vehicle.
     */
    public void tickVehicle() {
        if (!vehicle.isClientControlled()) {
            return;
        }

        VehicleContext ctx = new VehicleContext();
        ctx.loadSurroundingBlocks();

        ObjectDoublePair<Fluid> fluidHeight = updateFluidMovement(ctx);
        switch (fluidHeight.left()) {
            case WATER -> waterMovement(ctx);
            case LAVA -> {
                if (vehicle.canWalkOnLava() && ctx.centerBlock().is(Blocks.LAVA)) {
                    landMovement(ctx);
                } else {
                    lavaMovement(ctx, fluidHeight.rightDouble());
                }
            }
            case EMPTY -> landMovement(ctx);
        }
    }

    /**
     * Adds velocity of all colliding fluids to the vehicle, and returns the height of the fluid to use for movement.
     *
     * @param ctx context
     * @return type and height of fluid to use for movement
     */
    protected ObjectDoublePair<Fluid> updateFluidMovement(VehicleContext ctx) {
        BoundingBox box = boundingBox.clone();
        box.expand(-0.001);

        Vector3d min = box.getMin();
        Vector3d max = box.getMax();

        BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getFloorX(), min.getFloorY(), min.getFloorZ(), max.getFloorX(), max.getFloorY(), max.getFloorZ());

        double waterHeight = getFluidHeightAndApplyMovement(ctx, iter, Fluid.WATER, 0.014, min.getY());
        double lavaHeight = getFluidHeightAndApplyMovement(ctx, iter, Fluid.LAVA, vehicle.getSession().getDimensionType().ultrawarm() ? 0.007 : 0.007 / 3, min.getY());

        // Apply upward motion if the vehicle is a Strider, and it is submerged in lava
        if (lavaHeight > 0 && vehicle.getDefinition().entityType() == EntityType.STRIDER) {
            Vector3i blockPos = ctx.centerPos().toInt();
            if (!CollisionManager.FLUID_COLLISION.isBelow(blockPos.getY(), boundingBox) || ctx.getBlock(blockPos.up()).is(Blocks.LAVA)) {
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

    /**
     * Calculates how deep the vehicle is in a fluid, and applies its velocity.
     *
     * @param ctx context
     * @param iter iterator of colliding blocks
     * @param fluid type of fluid
     * @param speed multiplier for fluid motion
     * @param minY minY of the bounding box used to check for fluid collision; not exactly the same as the vehicle's bounding box
     * @return height of fluid compared to minY
     */
    protected double getFluidHeightAndApplyMovement(VehicleContext ctx, BlockPositionIterator iter, Fluid fluid, double speed, double minY) {
        Vector3d totalVelocity = Vector3d.ZERO;
        double maxFluidHeight = 0;
        int fluidBlocks = 0;

        for (iter.reset(); iter.hasNext(); iter.next()) {
            int blockId = ctx.getBlockId(iter);
            if (BlockStateValues.getFluid(blockId) != fluid) {
                continue;
            }

            Vector3i blockPos = Vector3i.from(iter.getX(), iter.getY(), iter.getZ());
            float worldFluidHeight = getWorldFluidHeight(fluid, blockId);

            double vehicleFluidHeight = blockPos.getY() + worldFluidHeight - minY;
            if (vehicleFluidHeight < 0) {
                // Vehicle is not submerged in this fluid block
                continue;
            }

            // flowBlocked is only used when determining if a falling fluid should drag the vehicle downwards.
            // If this block is not a falling fluid, set to true to avoid unnecessary checks.
            boolean flowBlocked = worldFluidHeight != 1;

            Vector3d velocity = Vector3d.ZERO;
            for (Direction direction : Direction.HORIZONTAL) {
                Vector3i adjacentBlockPos = blockPos.add(direction.getUnitVector());
                int adjacentBlockId = ctx.getBlockId(adjacentBlockPos);
                Fluid adjacentFluid = BlockStateValues.getFluid(adjacentBlockId);

                float fluidHeightDiff = 0;
                if (adjacentFluid == fluid) {
                    fluidHeightDiff = getLogicalFluidHeight(fluid, blockId) - getLogicalFluidHeight(fluid, adjacentBlockId);
                } else if (adjacentFluid == Fluid.EMPTY) {
                    // If the adjacent block is not a fluid and does not have collision,
                    // check if there is a fluid under it
                    BlockCollision adjacentBlockCollision = BlockUtils.getCollision(adjacentBlockId);
                    if (adjacentBlockCollision == null) {
                        float adjacentFluidHeight = getLogicalFluidHeight(fluid, ctx.getBlockId(adjacentBlockPos.add(Direction.DOWN.getUnitVector())));
                        if (adjacentFluidHeight != -1) { // Only care about same type of fluid
                            fluidHeightDiff = getLogicalFluidHeight(fluid, blockId) - (adjacentFluidHeight - MAX_LOGICAL_FLUID_HEIGHT);
                        }
                    } else if (!flowBlocked) {
                        // No need to check if flow is already blocked from another direction, or if this isn't a falling fluid.
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
                        flowBlocked = isFlowBlocked(fluid, ctx.getBlockId(blockPosUp.add(direction.getUnitVector())));
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

    protected float getWorldFluidHeight(Fluid fluidType, int blockId) {
        return (float) switch (fluidType) {
            case WATER -> BlockStateValues.getWaterHeight(blockId);
            case LAVA -> BlockStateValues.getLavaHeight(blockId);
            case EMPTY -> -1;
        };
    }

    protected float getLogicalFluidHeight(Fluid fluidType, int blockId) {
        return Math.min(getWorldFluidHeight(fluidType, blockId), MAX_LOGICAL_FLUID_HEIGHT);
    }

    protected boolean isFlowBlocked(Fluid fluid, int adjacentBlockId) {
        if (BlockState.of(adjacentBlockId).is(Blocks.ICE)) {
            return false;
        }

        if (BlockStateValues.getFluid(adjacentBlockId) == fluid) {
            return false;
        }

        // TODO: supposed to check if the opposite face of the block touching the fluid is solid, instead of SolidCollision
        return BlockUtils.getCollision(adjacentBlockId) instanceof SolidCollision;
    }

    protected void waterMovement(VehicleContext ctx) {
        double gravity = getGravity();
        float drag = vehicle.getFlag(EntityFlag.SPRINTING) ? 0.9f : 0.8f; // 0.8f: getBaseMovementSpeedMultiplier
        double originalY = ctx.centerPos().getY();
        boolean falling = vehicle.getMotion().getY() <= 0;

        // NOT IMPLEMENTED: depth strider and dolphins grace

        boolean horizontalCollision = travel(ctx, 0.02f);

        if (horizontalCollision && isClimbing(ctx)) {
            vehicle.setMotion(Vector3f.from(vehicle.getMotion().getX(), 0.2f, vehicle.getMotion().getZ()));
        }

        vehicle.setMotion(vehicle.getMotion().mul(drag, 0.8f, drag));
        vehicle.setMotion(getFluidGravity(gravity, falling));

        if (horizontalCollision && shouldApplyFluidJumpBoost(ctx, originalY)) {
            vehicle.setMotion(Vector3f.from(vehicle.getMotion().getX(), 0.3f, vehicle.getMotion().getZ()));
        }
    }

    protected void lavaMovement(VehicleContext ctx, double lavaHeight) {
        double gravity = getGravity();
        double originalY = ctx.centerPos().getY();
        boolean falling = vehicle.getMotion().getY() <= 0;

        boolean horizontalCollision = travel(ctx, 0.02f);

        if (lavaHeight <= (boundingBox.getSizeY() * 0.85 < 0.4 ? 0.0 : 0.4)) { // Swim height
            vehicle.setMotion(vehicle.getMotion().mul(0.5f, 0.8f, 0.5f));
            vehicle.setMotion(getFluidGravity(gravity, falling));
        } else {
            vehicle.setMotion(vehicle.getMotion().mul(0.5f));
        }

        vehicle.setMotion(vehicle.getMotion().down((float) (gravity / 4.0)));

        if (horizontalCollision && shouldApplyFluidJumpBoost(ctx, originalY)) {
            vehicle.setMotion(Vector3f.from(vehicle.getMotion().getX(), 0.3f, vehicle.getMotion().getZ()));
        }
    }

    protected void landMovement(VehicleContext ctx) {
        double gravity = getGravity();
        float slipperiness = BlockStateValues.getSlipperiness(getVelocityBlock(ctx));
        float drag = vehicle.isOnGround() ? 0.91f * slipperiness : 0.91f;
        float speed = vehicle.getVehicleSpeed() * (vehicle.isOnGround() ? BASE_SLIPPERINESS_CUBED / (slipperiness * slipperiness * slipperiness) : 0.1f);

        boolean horizontalCollision = travel(ctx, speed);

        if (isClimbing(ctx)) {
            Vector3f motion = vehicle.getMotion();
            vehicle.setMotion(
                Vector3f.from(
                    MathUtils.clamp(motion.getX(), -0.15f, 0.15f),
                    horizontalCollision ? 0.2f : Math.max(motion.getY(), -0.15f),
                    MathUtils.clamp(motion.getZ(), -0.15f, 0.15f)
                )
            );
            // NOT IMPLEMENTED: climbing in powdered snow
        }

        if (effectLevitation > 0) {
            vehicle.setMotion(vehicle.getMotion().up((0.05f * effectLevitation - vehicle.getMotion().getY()) * 0.2f));
        } else {
            vehicle.setMotion(vehicle.getMotion().down((float) gravity));
            // NOT IMPLEMENTED: slow fall when in unloaded chunk
        }

        vehicle.setMotion(vehicle.getMotion().mul(drag, 0.98f, drag));
    }

    protected boolean shouldApplyFluidJumpBoost(VehicleContext ctx, double originalY) {
        BoundingBox box = boundingBox.clone();
        box.translate(vehicle.getMotion().toDouble().up(0.6f - ctx.centerPos().getY() + originalY));
        box.expand(-1.0E-7);

        BlockPositionIterator iter = vehicle.getSession().getCollisionManager().collidableBlocksIterator(box);
        for (iter.reset(); iter.hasNext(); iter.next()) {
            int blockId = ctx.getBlockId(iter);

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

    protected Vector3f getFluidGravity(double gravity, boolean falling) {
        Vector3f motion = vehicle.getMotion();
        if (gravity != 0 && !vehicle.getFlag(EntityFlag.SPRINTING)) {
            float newY = (float) (motion.getY() - gravity / 16);
            if (falling && Math.abs(motion.getY() - 0.005f) >= MIN_VELOCITY && Math.abs(newY) < MIN_VELOCITY) {
                newY = -MIN_VELOCITY;
            }
            return Vector3f.from(motion.getX(), newY, motion.getZ());
        }
        return motion;
    }

    /**
     * Check if any blocks the vehicle is colliding with should multiply movement. (Cobweb, powder snow, berry bush)
     * <p>
     * This is different from the speed factor of a block the vehicle is standing on, such as soul sand.
     *
     * @param ctx context
     * @return the multiplier
     */
    protected @Nullable Vector3f getBlockMovementMultiplier(VehicleContext ctx) {
        BoundingBox box = boundingBox.clone();
        box.expand(-1.0E-7);

        Vector3i min = box.getMin().toInt();
        Vector3i max = box.getMax().toInt();

        // Iterate xyz backwards
        // Minecraft iterates forwards but only the last multiplier affects movement
        for (int x = max.getX(); x >= min.getX(); x--) {
            for (int y = max.getY(); y >= min.getY(); y--) {
                for (int z = max.getZ(); z >= min.getZ(); z--) {
                    Block block = ctx.getBlock(x, y, z).block();
                    Vector3f multiplier = null;

                    if (block == Blocks.COBWEB) {
                        if (effectWeaving) {
                            multiplier = Vector3f.from(0.5, 0.25, 0.5);
                        } else {
                            multiplier = Vector3f.from(0.25, 0.05f, 0.25);
                        }
                    } else if (block == Blocks.POWDER_SNOW) {
                        multiplier = Vector3f.from(0.9f, 1.5, 0.9f);
                    } else if (block == Blocks.SWEET_BERRY_BUSH) {
                        multiplier = Vector3f.from(0.8f, 0.75, 0.8f);
                    }

                    if (multiplier != null) {
                        return multiplier;
                    }
                }
            }
        }

        return null;
    }

    protected void applyBlockCollisionEffects(VehicleContext ctx) {
        BoundingBox box = boundingBox.clone();
        box.expand(-1.0E-7);

        Vector3i min = box.getMin().toInt();
        Vector3i max = box.getMax().toInt();

        BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        for (iter.reset(); iter.hasNext(); iter.next()) {
            BlockState blockState = ctx.getBlock(iter);

            if (blockState.is(Blocks.HONEY_BLOCK)) {
                onHoneyBlockCollision();
            } else if (blockState.is(Blocks.BUBBLE_COLUMN)) {
                onBubbleColumnCollision(blockState.getValue(Properties.DRAG));
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
     * Calculates the next position of the vehicle while checking for collision and adjusting velocity.
     *
     * @return true if there was a horizontal collision
     */
    protected boolean travel(VehicleContext ctx, float speed) {
        Vector3f motion = vehicle.getMotion();

        // Java only does this client side
        motion = motion.mul(0.98f);

        motion = Vector3f.from(
                Math.abs(motion.getX()) < MIN_VELOCITY ? 0 : motion.getX(),
                Math.abs(motion.getY()) < MIN_VELOCITY ? 0 : motion.getY(),
                Math.abs(motion.getZ()) < MIN_VELOCITY ? 0 : motion.getZ()
        );

        // !isImmobile
        if (vehicle.isAlive()) {
            motion = motion.add(getInputVelocity(ctx, speed));
        }

        Vector3f movementMultiplier = getBlockMovementMultiplier(ctx);
        if (movementMultiplier != null) {
            motion = motion.mul(movementMultiplier);
        }

        // Check world border before blocks
        Vector3d correctedMovement = vehicle.getSession().getWorldBorder().correctMovement(boundingBox, motion.toDouble());
        correctedMovement = vehicle.getSession().getCollisionManager().correctMovement(
            correctedMovement, boundingBox, vehicle.isOnGround(), this.stepHeight, true, vehicle.canWalkOnLava()
        );

        boundingBox.translate(correctedMovement);
        ctx.loadSurroundingBlocks(); // Context must be reloaded after vehicle is moved

        // Non-zero values indicate a collision on that axis
        Vector3d moveDiff = motion.toDouble().sub(correctedMovement);

        vehicle.setOnGround(moveDiff.getY() != 0 && motion.getY() < 0);
        boolean horizontalCollision = moveDiff.getX() != 0 || moveDiff.getZ() != 0;

        boolean bounced = false;
        if (vehicle.isOnGround()) {
            Block landingBlock = getLandingBlock(ctx).block();

            if (landingBlock == Blocks.SLIME_BLOCK) {
                motion = Vector3f.from(motion.getX(), -motion.getY(), motion.getZ());
                bounced = true;

                // Slow horizontal movement
                float absY = Math.abs(motion.getY());
                if (absY < 0.1f) {
                    float mul = 0.4f + absY * 0.2f;
                    motion = motion.mul(mul, 1.0f, mul);
                }
            } else if (landingBlock instanceof BedBlock) {
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
        moveVehicle(ctx.centerPos());
        vehicle.setMotion(motion);

        applyBlockCollisionEffects(ctx);

        float velocityMultiplier = getVelocityMultiplier(ctx);
        vehicle.setMotion(vehicle.getMotion().mul(velocityMultiplier, 1.0f, velocityMultiplier));

        return horizontalCollision;
    }

    protected boolean isClimbing(VehicleContext ctx) {
        if (!vehicle.canClimb()) {
            return false;
        }

        BlockState blockState = ctx.centerBlock();
        if (vehicle.getSession().getTagCache().is(BlockTag.CLIMBABLE, blockState.block())) {
            return true;
        }

        // Check if the vehicle is in an open trapdoor with a ladder of the same direction under it
        if (blockState.block() instanceof TrapDoorBlock && blockState.getValue(Properties.OPEN)) {
            BlockState ladderState = ctx.getBlock(ctx.centerPos().toInt().down());
            return ladderState.is(Blocks.LADDER) &&
                    ladderState.getValue(Properties.HORIZONTAL_FACING) == blockState.getValue(Properties.HORIZONTAL_FACING);
        }

        return false;
    }

    /**
     * Translates the player's input into velocity.
     *
     * @param ctx context
     * @param speed multiplier for input
     * @return velocity
     */
    protected Vector3f getInputVelocity(VehicleContext ctx, float speed) {
        Vector2f input = vehicle.getSession().getPlayerEntity().getVehicleInput();
        input = input.mul(0.98f);
        input = vehicle.getAdjustedInput(input);
        input = normalizeInput(input);
        input = input.mul(speed);

        // Match player rotation
        float yaw = vehicle.getSession().getPlayerEntity().getYaw();
        float sin = TrigMath.sin(yaw * TrigMath.DEG_TO_RAD);
        float cos = TrigMath.cos(yaw * TrigMath.DEG_TO_RAD);
        return Vector3f.from(input.getX() * cos - input.getY() * sin, 0, input.getY() * cos + input.getX() * sin);
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

    /**
     * Gets the rotation to use for the vehicle. This is based on the player's head rotation.
     */
    protected Vector2f getVehicleRotation() {
        LivingEntity player = vehicle.getSession().getPlayerEntity();
        return Vector2f.from(player.getYaw(), player.getPitch() * 0.5f);
    }

    /**
     * Sets the new position for the vehicle and sends packets to both the java server and bedrock client.
     * <p>
     * This also updates the session's last vehicle move timestamp.
     * @param javaPos the new java position of the vehicle
     */
    protected void moveVehicle(Vector3d javaPos) {
        Vector3f bedrockPos = javaPos.toFloat();
        Vector2f rotation = getVehicleRotation();

        MoveEntityDeltaPacket moveEntityDeltaPacket = new MoveEntityDeltaPacket();
        moveEntityDeltaPacket.setRuntimeEntityId(vehicle.getGeyserId());

        if (vehicle.isOnGround()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
        }

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

        if (vehicle.getYaw() != rotation.getX()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            moveEntityDeltaPacket.setYaw(rotation.getX());
            vehicle.setYaw(rotation.getX());
        }
        if (vehicle.getPitch() != rotation.getY()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
            moveEntityDeltaPacket.setPitch(rotation.getY());
            vehicle.setPitch(rotation.getY());
        }
        if (vehicle.getHeadYaw() != rotation.getX()) { // Same as yaw
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
            moveEntityDeltaPacket.setHeadYaw(rotation.getX());
            vehicle.setHeadYaw(rotation.getX());
        }

        if (!moveEntityDeltaPacket.getFlags().isEmpty()) {
            vehicle.getSession().sendUpstreamPacket(moveEntityDeltaPacket);
        }

        ServerboundMoveVehiclePacket moveVehiclePacket = new ServerboundMoveVehiclePacket(javaPos, rotation.getX(), rotation.getY(), vehicle.isOnGround());
        vehicle.getSession().sendDownstreamPacket(moveVehiclePacket);
    }

    protected double getGravity() {
        if (!vehicle.getFlag(EntityFlag.HAS_GRAVITY)) {
            return 0;
        }

        if (vehicle.getMotion().getY() <= 0 && effectSlowFalling) {
            return Math.min(0.01, this.gravity);
        }

        return this.gravity;
    }

    /**
     * Finds the position of the main block supporting the vehicle.
     * Used when determining slipperiness, speed, etc.
     * <p>
     * Should use {@link VehicleContext#supportingBlockPos()}, instead of calling this directly.
     *
     * @param ctx context
     * @return position of the main block supporting this entity
     */
    private @Nullable Vector3i getSupportingBlockPos(VehicleContext ctx) {
        Vector3i result = null;

        if (vehicle.isOnGround()) {
            BoundingBox box = boundingBox.clone();
            box.extend(0, -1.0E-6, 0); // Extend slightly down

            Vector3i min = box.getMin().toInt();
            Vector3i max = box.getMax().toInt();

            // Use minY as maxY
            BlockPositionIterator iter = BlockPositionIterator.fromMinMax(min.getX(), min.getY(), min.getZ(), max.getX(), min.getY(), max.getZ());

            double minDistance = Double.MAX_VALUE;
            for (iter.reset(); iter.hasNext(); iter.next()) {
                Vector3i blockPos = Vector3i.from(iter.getX(), iter.getY(), iter.getZ());
                int blockId = ctx.getBlockId(iter);

                BlockCollision blockCollision;
                if (vehicle.canWalkOnLava()) {
                    blockCollision = vehicle.getSession().getCollisionManager().getCollisionLavaWalking(blockId, blockPos.getY(), boundingBox);
                } else {
                    blockCollision = BlockUtils.getCollision(blockId);
                }

                if (blockCollision != null && blockCollision.checkIntersection(blockPos, box)) {
                    double distance = ctx.centerPos().distanceSquared(blockPos.toDouble().add(0.5f, 0.5f, 0.5f));
                    if (distance <= minDistance) {
                        minDistance = distance;
                        result = blockPos;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the block that is x amount of blocks under the main supporting block.
     */
    protected BlockState getBlockUnderSupport(VehicleContext ctx, float dist) {
        Vector3i supportingBlockPos = ctx.supportingBlockPos();

        Vector3i blockPos;
        if (supportingBlockPos != null) {
            blockPos = Vector3i.from(supportingBlockPos.getX(), Math.floor(ctx.centerPos().getY() - dist), supportingBlockPos.getZ());
        } else {
            blockPos = ctx.centerPos().sub(0, dist, 0).toInt();
        }

        return ctx.getBlock(blockPos);
    }

    /**
     * The block to use when determining if the vehicle should bounce after landing. Currently just slime and bed blocks.
     */
    protected BlockState getLandingBlock(VehicleContext ctx) {
        return getBlockUnderSupport(ctx, 0.2f);
    }

    /**
     * The block to use when calculating slipperiness and speed. If on a slab, this will be the block under the slab.
     */
    protected BlockState getVelocityBlock(VehicleContext ctx) {
        return getBlockUnderSupport(ctx, 0.500001f);
    }

    protected float getVelocityMultiplier(VehicleContext ctx) {
        Block block = ctx.centerBlock().block();
        if (block == Blocks.WATER || block == Blocks.BUBBLE_COLUMN) {
            return 1.0f;
        }

        if (block == Blocks.SOUL_SAND || block == Blocks.HONEY_BLOCK) {
            return 0.4f;
        }

        block = getVelocityBlock(ctx).block();
        if (block == Blocks.SOUL_SAND || block == Blocks.HONEY_BLOCK) {
            return 0.4f;
        }

        return 1.0f;
    }

    protected float getJumpVelocityMultiplier(VehicleContext ctx) {
        Block block = ctx.centerBlock().block();
        if (block == Blocks.HONEY_BLOCK) {
            return 0.5f;
        }

        block = getVelocityBlock(ctx).block();
        if (block == Blocks.HONEY_BLOCK) {
            return 0.5f;
        }

        return 1.0f;
    }

    protected class VehicleContext {
        private Vector3d centerPos;
        private Vector3d cachePos;
        private BlockState centerBlock;
        private Vector3i supportingBlockPos;
        private BlockPositionIterator blockIter;
        private int[] blocks;

        /**
         * Cache frequently used data and blocks used in movement calculations.
         * <p>
         * Can be called multiple times, and must be called at least once before using the VehicleContext.
         */
        protected void loadSurroundingBlocks() {
            this.centerPos = boundingBox.getBottomCenter();

            // Reuse block cache if vehicle moved less than 1 block
            if (this.cachePos == null || this.cachePos.distanceSquared(this.centerPos) > 1) {
                BoundingBox box = boundingBox.clone();
                box.expand(2);

                Vector3i min = box.getMin().toInt();
                Vector3i max = box.getMax().toInt();
                this.blockIter = BlockPositionIterator.fromMinMax(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
                this.blocks = vehicle.getSession().getGeyser().getWorldManager().getBlocksAt(vehicle.getSession(), this.blockIter);

                this.cachePos = this.centerPos;
            }

            this.centerBlock = getBlock(this.centerPos.toInt());
            this.supportingBlockPos = null;
        }

        protected Vector3d centerPos() {
            return this.centerPos;
        }

        protected BlockState centerBlock() {
            return this.centerBlock;
        }

        protected Vector3i supportingBlockPos() {
            if (this.supportingBlockPos == null) {
                this.supportingBlockPos = getSupportingBlockPos(this);
            }

            return this.supportingBlockPos;
        }

        protected int getBlockId(int x, int y, int z) {
            int index = this.blockIter.getIndex(x, y, z);
            if (index == -1) {
                vehicle.getSession().getGeyser().getLogger().debug("[client-vehicle] Block cache miss");
                return vehicle.getSession().getGeyser().getWorldManager().getBlockAt(vehicle.getSession(), x, y, z);
            }

            return blocks[index];
        }

        protected int getBlockId(Vector3i pos) {
            return getBlockId(pos.getX(), pos.getY(), pos.getZ());
        }

        protected int getBlockId(BlockPositionIterator iter) {
            return getBlockId(iter.getX(), iter.getY(), iter.getZ());
        }

        protected BlockState getBlock(int x, int y, int z) {
            return BlockState.of(getBlockId(x, y, z));
        }

        protected BlockState getBlock(Vector3i pos) {
            return BlockState.of(getBlockId(pos.getX(), pos.getY(), pos.getZ()));
        }

        protected BlockState getBlock(BlockPositionIterator iter) {
            return BlockState.of(getBlockId(iter.getX(), iter.getY(), iter.getZ()));
        }
    }
}
