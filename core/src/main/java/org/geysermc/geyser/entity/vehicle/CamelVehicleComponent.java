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

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class CamelVehicleComponent extends VehicleComponent<CamelEntity> {
    private float horseJumpStrength = 0.42f; // This is the default for Camels. Not sent by vanilla Java server when spawned
    private int jumpBoost;

    public CamelVehicleComponent(CamelEntity vehicle) {
        super(vehicle);
    }

    public void setHorseJumpStrength(float horseJumpStrength) {
        this.horseJumpStrength = horseJumpStrength;
    }

    @Override
    public void setEffect(Effect effect, int effectAmplifier) {
        if (effect == Effect.JUMP_BOOST) {
            jumpBoost = effectAmplifier + 1;
        } else {
            super.setEffect(effect, effectAmplifier);
        }
    }

    @Override
    public void removeEffect(Effect effect) {
        if (effect == Effect.JUMP_BOOST) {
            jumpBoost = 0;
        } else {
            super.removeEffect(effect);
        }
    }

    @Override
    protected Vector3f getJumpVelocity(CamelEntity vehicle) {
        SessionPlayerEntity player = vehicle.getSession().getPlayerEntity();
        float jumpStrength = player.getVehicleJumpStrength();

        if (jumpStrength > 0) {
            player.setVehicleJumpStrength(0);

            jumpStrength = jumpStrength >= 90 ? 1.0f : 0.4f + 0.4f * jumpStrength / 90.0f;
            return Vector3f.createDirectionDeg(0, -player.getYaw())
                    .mul(22.2222f * jumpStrength * moveSpeed * getVelocityMultiplier(vehicle))
                    .up(1.4285f * jumpStrength * (horseJumpStrength * getJumpVelocityMultiplier(vehicle) + (jumpBoost * 0.1f)));
        }

        return Vector3f.ZERO;
    }
}
