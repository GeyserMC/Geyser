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

import lombok.Setter;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;

public class HappyGhastVehicleComponent extends VehicleComponent<HappyGhastEntity> {

    @Setter
    private float flyingSpeed;

    public HappyGhastVehicleComponent(HappyGhastEntity vehicle, float stepHeight) {
        super(vehicle, stepHeight);
    }

    protected Vector3f getInputVelocity(VehicleContext ctx, float speed) {
        Vector2f input = vehicle.getSession().getPlayerEntity().getVehicleInput();
        input = input.mul(0.98f); // ?

        float x = input.getX();
        float y = 0.0f;
        float z = 0.0f;

        float playerZ = input.getY();
        if (playerZ != 0.0F) {
            float i = Mth.cos(player.getXRot() * (float) (Math.PI / 180.0));
            float j = -Mth.sin(player.getXRot() * (float) (Math.PI / 180.0));
            if (playerZ < 0.0F) {
                i *= -0.5F;
                j *= -0.5F;
            }

            y = j;
            z = i;
        }

        if (session.isJumping()) {
            y += 0.5F;
        }

        return Vector3f.from((double) x, (double) y, (double)z).mul(3.9F * flyingSpeed);
    }
}
