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

package org.geysermc.geyser.entity.vehicle;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BedBlock;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;

public class BoatVehicleComponent extends VehicleComponent<BoatEntity> {
    private Status status, oldStatus;
    private double waterLevel;
    private float landFriction;
    private double lastYd;
    private float deltaRotation;

    public BoatVehicleComponent(BoatEntity vehicle, float stepHeight) {
        super(vehicle, stepHeight);

        this.gravity = 0.04;
    }

    @Override
    public void tickVehicle() {
        if (!vehicle.isClientControlled()) {
            return;
        }

        final VehicleContext context = new VehicleContext();
        context.loadSurroundingBlocks();

        this.oldStatus = this.status;
        this.status = getStatus(context);

        floatBoat(context);

        final Vector3f lastRotation = vehicle.getBedrockRotation();
        controlBoat();

        Vector3f motion = vehicle.getMotion();
        Vector3f movementMultiplier = getBlockMovementMultiplier(context);
        if (movementMultiplier != null) {
            motion = motion.mul(movementMultiplier);
        }

        Vector3d correctedMovement = vehicle.getSession().getWorldBorder().correctMovement(boundingBox, motion.toDouble());
        correctedMovement = vehicle.getSession().getCollisionManager().correctMovement(
            correctedMovement, boundingBox, vehicle.isOnGround(), this.stepHeight, true, vehicle.canWalkOnLava()
        );

        // Non-zero values indicate a collision on that axis
        Vector3d moveDiff = motion.toDouble().sub(correctedMovement);

        boundingBox.translate(correctedMovement);
        context.loadSurroundingBlocks(); // Context must be reloaded after vehicle is moved

        boolean verticalCollision = moveDiff.getY() != 0;
        vehicle.setOnGround(verticalCollision && motion.getY() < 0);

        boolean bounced = false;
        if (vehicle.isOnGround()) {
            Block landingBlock = getLandingBlock(context).block();

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
                !verticalCollision || bounced ? 1 : 0,
                moveDiff.getZ() == 0 ? 1 : 0
            );
        }

        // Send the new position to the bedrock client and java server
        moveVehicle(context.centerPos(), lastRotation);
        vehicle.setMotion(motion);

        // This got ran twice in Boat entity for certain reason?
        applyBlockCollisionEffects(context);
        applyBlockCollisionEffects(context);
    }

    @Override
    protected void moveVehicle(Vector3d javaPos, Vector3f lastRotation) {
        Vector3f bedrockPos = javaPos.toFloat();

        MoveEntityDeltaPacket moveEntityDeltaPacket = new MoveEntityDeltaPacket();
        moveEntityDeltaPacket.setRuntimeEntityId(vehicle.geyserId());

        if (vehicle.isOnGround()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
        }

        if (vehicle.getPosition().getX() != bedrockPos.getX()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            moveEntityDeltaPacket.setX(bedrockPos.getX());
        }
        if (vehicle.getPosition().getY() != bedrockPos.getY()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            moveEntityDeltaPacket.setY(bedrockPos.getY() + vehicle.getDefinition().offset());
        }
        if (vehicle.getPosition().getZ() != bedrockPos.getZ()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
            moveEntityDeltaPacket.setZ(bedrockPos.getZ());
        }
        vehicle.setPosition(bedrockPos);

        if (vehicle.getPitch() != lastRotation.getX()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
            moveEntityDeltaPacket.setPitch(vehicle.getPitch());
        }
        if (vehicle.getYaw() != lastRotation.getY()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            moveEntityDeltaPacket.setYaw(vehicle.getYaw());
        }
        if (vehicle.getHeadYaw() != lastRotation.getZ()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
            moveEntityDeltaPacket.setHeadYaw(vehicle.getHeadYaw());
        }

        if (!moveEntityDeltaPacket.getFlags().isEmpty()) {
            vehicle.getSession().sendUpstreamPacket(moveEntityDeltaPacket);
        }

        ServerboundMoveVehiclePacket moveVehiclePacket = new ServerboundMoveVehiclePacket(javaPos, vehicle.getYaw() - 90, vehicle.getPitch(), vehicle.isOnGround());
        vehicle.getSession().sendDownstreamPacket(moveVehiclePacket);
    }

    private void controlBoat() {
        float acceleration = 0.0F;

        final Vector2f input = vehicle.getSession().getPlayerEntity().getVehicleInput();
        boolean up = input.getY() > 0.35;
        boolean down = input.getY() < -0.35;
        boolean left = input.getX() > 0.35;
        boolean right = input.getX() < -0.35;

        if (left) this.deltaRotation--;
        if (right) this.deltaRotation++;

        if (right != left && !up && !down) acceleration += 0.005F;

        vehicle.setYaw(vehicle.getYaw() + this.deltaRotation);
        vehicle.setHeadYaw(vehicle.getYaw());

        if (up) acceleration += 0.04F;
        if (down) acceleration -= 0.005F;

        float yaw = vehicle.getYaw() - 90;
        final Vector3f motion = Vector3f.from(TrigMath.sin((-yaw * 0.017453292F)) * acceleration, 0, TrigMath.cos((yaw * 0.017453292F)) * acceleration);
        vehicle.setMotion(vehicle.getMotion().add(motion));

        vehicle.getSession().setSteeringLeft(right && !left || up);
        vehicle.getSession().setSteeringRight(left && !right || up);
    }

    private void floatBoat(final VehicleContext context) {
        double gravity = -getGravity();
        double buoyancy = 0.0D;
        float frictionMutiplier = 0.05F;

        if (this.oldStatus == Status.IN_AIR && this.status != Status.IN_AIR && this.status != Status.ON_LAND) {
            this.waterLevel = getBoundingBox().getMax(Axis.Y);
            double targetY = (getWaterLevelAbove(context) - this.vehicle.getBoundingBoxHeight()) + 0.101D;

            BoundingBox box = boundingBox.clone();
            box.translate(0, targetY - getBoundingBox().getMin(Axis.Y), 0);

            boolean empty = true;
            for (BlockPositionIterator iter = vehicle.getSession().getCollisionManager().collidableBlocksIterator(box); iter.hasNext(); iter.next()) {
                final BlockCollision collision = BlockUtils.getCollision(context.getBlockId(iter.getX(), iter.getY(), iter.getZ()));
                if (collision != null && collision.checkIntersection(Vector3i.from(iter.getX(), iter.getY(), iter.getZ()), box)) {
                    empty = false;
                    break;
                }
            }

            if (empty) {
                vehicle.setMotion(vehicle.getMotion().mul(1, 0, 1));
                boundingBox.setMiddleY(targetY + (boundingBox.getSizeY() / 2));
                this.lastYd = 0;
            }

            this.status = Status.IN_WATER;
        } else {
            if (this.status == Status.IN_WATER) {
                buoyancy = (this.waterLevel - getBoundingBox().getMin(Axis.Y)) / vehicle.getBoundingBoxHeight();
                frictionMutiplier = 0.9F;
            } else if (this.status == Status.UNDER_FLOWING_WATER) {
                gravity = -7.0E-4D;
                frictionMutiplier = 0.9F;
            } else if (this.status == Status.UNDER_WATER) {
                buoyancy = 0.009999999776482582D;
                frictionMutiplier = 0.45F;
            } else if (this.status == Status.IN_AIR) {
                frictionMutiplier = 0.9F;
            } else if (this.status == Status.ON_LAND) {
                frictionMutiplier = this.landFriction;
                this.landFriction /= 2.0F;
            }

            vehicle.setMotion(vehicle.getMotion().mul(frictionMutiplier, 1, frictionMutiplier).up((float) gravity));

            this.deltaRotation *= frictionMutiplier;
            if (buoyancy > 0.0D) {
                Vector3f motion = vehicle.getMotion();
                vehicle.setMotion(Vector3f.from(
                    motion.getX(),
                    (float) (motion.getY() + buoyancy * (this.gravity / 0.65)) * 0.75f,
                    motion.getZ()
                ));
            }
        }
    }

    private float getWaterLevelAbove(final VehicleContext context) {
        BoundingBox aabb = getBoundingBox();

        int minX = GenericMath.floor(aabb.getMin(Axis.X));
        int maxX = GenericMath.ceil(aabb.getMax(Axis.X));
        int minY = GenericMath.floor(aabb.getMax(Axis.Y));
        int maxY = GenericMath.ceil(aabb.getMax(Axis.Y) - this.lastYd);
        int minZ = GenericMath.floor(aabb.getMin(Axis.Z));
        int maxZ = GenericMath.ceil(aabb.getMax(Axis.Z));

        for (int y = minY; y < maxY; y++) {
            float blockHeight = 0.0F;
            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    final float fluidHeight = getLogicalFluidHeight(Fluid.WATER, context.getBlockId(x, y, z));
                    if (fluidHeight > 0) {
                        blockHeight = Math.max(blockHeight, fluidHeight);
                    }
                }
            }

            if (blockHeight < 1.0F) {
                return y + blockHeight;
            }
        }
        return (maxY + 1);
    }

    private float getGroundFriction(final VehicleContext context) {
        BoundingBox boatShape = getBoundingBox().clone();
        // 0.001 high box extending downwards from the boat
        boatShape.setMiddleY(boatShape.getMin(Axis.Y) - 0.0005);
        boatShape.setSizeY(0.001);

        if (boatShape.isEmpty()) {
            return Float.NaN;
        }

        int x0 = GenericMath.floor(boatShape.getMin(Axis.X)) - 1;
        int x1 = GenericMath.ceil(boatShape.getMax(Axis.X)) + 1;
        int y0 = GenericMath.floor(boatShape.getMin(Axis.Y)) - 1;
        int y1 = GenericMath.ceil(boatShape.getMax(Axis.Y)) + 1;
        int z0 = GenericMath.floor(boatShape.getMin(Axis.Z)) - 1;
        int z1 = GenericMath.ceil(boatShape.getMax(Axis.Z)) + 1;

        float friction = 0.0F;
        int count = 0;

        for (int x = x0; x < x1; x++) {
            for (int z = z0; z < z1; z++) {
                int edges = ((x == x0 || x == x1 - 1) ? 1 : 0) + ((z == z0 || z == z1 - 1) ? 1 : 0);
                if (edges == 2) {
                    continue;
                }

                for (int y = y0; y < y1; y++) {
                    if (edges > 0 && !(y != y0 && y != y1 - 1)) {
                        continue;
                    }
                    final BlockState state = context.getBlock(x, y, z);
                    if (state.is(Blocks.LILY_PAD)) {
                        continue;
                    }
                    final BlockCollision collision = BlockUtils.getCollision(state.javaId());
                    if (collision == null || collision.getBoundingBoxes().length == 0) {
                        continue;
                    }

                    if (collision.checkIntersection(Vector3i.from(x, y, z), boatShape)) {
                        friction += BlockStateValues.getSlipperiness(state);
                        count++;
                    }
                }
            }
        }

        return friction / count;
    }

    private Status isUnderwater(final VehicleContext context) {
        BoundingBox boatShape = getBoundingBox().clone();
        double maxY = boatShape.getMax(Axis.Y) + 0.001D;

        int x0 = GenericMath.floor(boatShape.getMin(Axis.X));
        int x1 = GenericMath.ceil(boatShape.getMax(Axis.X));
        int y0 = GenericMath.floor(boatShape.getMax(Axis.Y));
        int y1 = GenericMath.ceil(maxY);
        int z0 = GenericMath.floor(boatShape.getMin(Axis.Z));
        int z1 = GenericMath.ceil(boatShape.getMax(Axis.Z));

        boolean underWater = false;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    final float fluidHeight = getLogicalFluidHeight(Fluid.WATER, context.getBlockId(x, y, z));
                    if (fluidHeight <= 0 || maxY > y + fluidHeight) {
                        continue;
                    }

                    if (fluidHeight == 1) {
                        underWater = true;
                    } else {
                        return Status.UNDER_FLOWING_WATER;
                    }
                }
            }
        }
        return underWater ? Status.UNDER_WATER : null;
    }

    private boolean checkInWater(final VehicleContext context) {
        this.waterLevel = Double.MIN_VALUE;

        final BoundingBox boatShape = getBoundingBox();
        int minX = GenericMath.floor(boatShape.getMin(Axis.X));
        int maxX = GenericMath.ceil(boatShape.getMax(Axis.X));
        int minY = GenericMath.floor(boatShape.getMin(Axis.Y));
        int maxY = GenericMath.ceil(boatShape.getMin(Axis.Y) + 0.001D);
        int minZ = GenericMath.floor(boatShape.getMin(Axis.Z));
        int maxZ = GenericMath.ceil(boatShape.getMax(Axis.Z));

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    final float fluidHeight = getLogicalFluidHeight(Fluid.WATER, context.getBlockId(x, y, z));
                    if (fluidHeight <= 0) {
                        continue;
                    }

                    float height = y + fluidHeight;
                    this.waterLevel = Math.max(height, this.waterLevel);
                    if (boatShape.getMin(Axis.Y) < height) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Status getStatus(final VehicleContext context) {
        Status waterStatus = isUnderwater(context);
        if (waterStatus != null) {
            this.waterLevel = getBoundingBox().getMax(Axis.Y);
            return waterStatus;
        }
        if (checkInWater(context)) {
            return Status.IN_WATER;
        }
        float friction = getGroundFriction(context);
        if (friction > 0.0F) {
            this.landFriction = friction;
            return Status.ON_LAND;
        }
        return Status.IN_AIR;
    }

    public enum Status {
        IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR;
    }
}
