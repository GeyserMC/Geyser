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

import org.geysermc.geyser.entity.type.living.animal.nautilus.AbstractNautilusEntity;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.util.MathUtils;

public class NautilusVehicleComponent extends BoostableVehicleComponent<AbstractNautilusEntity> {
    public NautilusVehicleComponent(AbstractNautilusEntity vehicle, float stepHeight, float defSpeed) {
        super(vehicle, stepHeight);
        this.moveSpeed = defSpeed;
    }

    @Override
    protected void updateRotation() {
        float yaw = vehicle.getYaw() + MathUtils.wrapDegrees(getRiddenRotation().getX() - vehicle.getYaw()) * 0.08f;
        vehicle.setYaw(yaw);
        vehicle.setHeadYaw(yaw);
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

        // LivingEntity#travel
        Fluid fluid = checkForFluid(ctx);
        float drag = switch (fluid) {
            case WATER -> 0.9f; // AbstractNautilus#travelInWater
            case LAVA -> 0.5f; // LivingEntity#travelInLava
            case EMPTY -> 1f; // TODO No drag it seems? Should probably check the block below, e.g. soul sand
        };

        travel(ctx, getRiddenSpeed(fluid));
        vehicle.setMotion(vehicle.getMotion().mul(drag));
    }

    // AbstractNautilus#getRiddenSpeed
    private float getRiddenSpeed(Fluid fluid) {
        return fluid == Fluid.WATER ? 0.0325F * this.getMoveSpeed() :
                0.02F * this.getMoveSpeed();
    }
}
