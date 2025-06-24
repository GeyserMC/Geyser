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

import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;

public class HappyGhastVehicleComponent extends VehicleComponent<HappyGhastEntity> {

    @Setter
    private float flyingSpeed;

    public HappyGhastVehicleComponent(HappyGhastEntity vehicle, float stepHeight) {
        super(vehicle, stepHeight);
        flyingSpeed = (float) AttributeType.Builtin.FLYING_SPEED.getDef();
    }

    /**
     * Called every session tick while the player is mounted on the vehicle.
     */
    public void tickVehicle() {
        if (!vehicle.isClientControlled()) {
            return;
        }

        // LivingEntity#travelFlying
        VehicleContext ctx = new VehicleContext();
        ctx.loadSurroundingBlocks();

        // TODO tickRidden here (deals with rotations)

        // TODO verify that updateFluidMovement applies to happy ghasts
        ObjectDoublePair<Fluid> fluidHeight = updateFluidMovement(ctx);

        // HappyGhast#travel
        float speed = flyingSpeed * 5.0f / 3.0f;

        // TODO implement LivingEntity#travelFlying cases
//        switch (fluidHeight.left()) {
//            default -> {
//                throw new GoodLuckImplementingThisException();
//            }
//        }
    }

    protected Vector3f getInputVelocity(VehicleContext ctx, float speed) {
        Vector2f input = vehicle.getSession().getPlayerEntity().getVehicleInput();
        input = input.mul(0.98f); // ?

        float x = input.getX();
        float y = 0.0F;
        float z = 0.0F;

        float playerZ = input.getY();
        if (playerZ != 0.0F) {
            float i = (float) Math.cos(vehicle.getSession().getPlayerEntity().getPitch() * (Math.PI / 180.0F));
            float j = (float) -Math.sin(vehicle.getSession().getPlayerEntity().getPitch() * (Math.PI / 180.0F));
            if (playerZ < 0.0F) {
                i *= -0.5F;
                j *= -0.5F;
            }

            y = j;
            z = i;
        }

        if (vehicle.getSession().getInputCache().wasJumping()) {
            y += 0.5F;
        }

        return Vector3f.from(x, y, z).mul(3.9F * flyingSpeed);
    }

    public class GoodLuckImplementingThisException extends RuntimeException {
    }
}
