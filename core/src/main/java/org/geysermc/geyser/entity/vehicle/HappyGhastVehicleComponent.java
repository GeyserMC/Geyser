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

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;

@Setter
@Getter
public class HappyGhastVehicleComponent extends VehicleComponent<HappyGhastEntity> {

    private float flyingSpeed;
    private float cameraDistance;

    public HappyGhastVehicleComponent(HappyGhastEntity vehicle, float stepHeight) {
        super(vehicle, stepHeight);
        // Happy Ghast has different defaults
        flyingSpeed = 0.05f;
        moveSpeed = 0.05f;
        cameraDistance = 8.0f;
    }

    @Override
    protected void updateRotation() {
        float yaw = vehicle.getYaw() + MathUtils.wrapDegrees(getRiddenRotation().getX() - vehicle.getYaw()) * 0.08f;
        vehicle.setYaw(yaw);
        vehicle.setHeadYaw(yaw);
    }

    @Override
    public void onMount() {
        super.onMount();
        SessionPlayerEntity playerEntity = vehicle.getSession().getPlayerEntity();
        playerEntity.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, false);
        playerEntity.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, 181f);
        playerEntity.getDirtyMetadata().put(EntityDataTypes.SEAT_THIRD_PERSON_CAMERA_RADIUS, cameraDistance);
        playerEntity.getDirtyMetadata().put(EntityDataTypes.SEAT_CAMERA_RELAX_DISTANCE_SMOOTHING, cameraDistance * 0.75f);
        playerEntity.getDirtyMetadata().put(EntityDataTypes.CONTROLLING_RIDER_SEAT_INDEX, (byte) 0);
    }

    @Override
    public void onDismount() {
        super.onDismount();
        SessionPlayerEntity playerEntity = vehicle.getSession().getPlayerEntity();
        playerEntity.getDirtyMetadata().put(EntityDataTypes.SEAT_THIRD_PERSON_CAMERA_RADIUS, (float) AttributeType.Builtin.CAMERA_DISTANCE.getDef());
        playerEntity.getDirtyMetadata().put(EntityDataTypes.SEAT_CAMERA_RELAX_DISTANCE_SMOOTHING, cameraDistance * 0.75f);
        playerEntity.getDirtyMetadata().put(EntityDataTypes.CONTROLLING_RIDER_SEAT_INDEX, (byte) 0);
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

        // LivingEntity#travelFlying
        Fluid fluid = checkForFluid(ctx);
        float drag = switch (fluid) {
            case WATER -> 0.8f;
            case LAVA -> 0.5f;
            case EMPTY -> 0.91f;
        };
        // HappyGhast#travel
        travel(ctx, flyingSpeed * 5.0f / 3.0f);
        vehicle.setMotion(vehicle.getMotion().mul(drag));
    }
}
