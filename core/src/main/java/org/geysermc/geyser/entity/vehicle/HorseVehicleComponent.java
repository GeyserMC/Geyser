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
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class HorseVehicleComponent extends VehicleComponent<AbstractHorseEntity> {
    @Setter
    private float horseJumpStrength = 0.7f; // Not sent by vanilla Java server when spawned
    private int effectJumpBoost;
    @Setter
    private boolean allowStandSliding;

    public HorseVehicleComponent(AbstractHorseEntity vehicle) {
        super(vehicle, 1.5f);
    }

    @Override
    public void tickVehicle() {
        if (!vehicle.getFlag(EntityFlag.STANDING)) {
            this.allowStandSliding = false;
        }

        super.tickVehicle();
    }

    @Override
    protected Vector3f getInputVector(VehicleComponent<AbstractHorseEntity>.VehicleContext ctx, float speed, Vector3f input) {
        SessionPlayerEntity player = vehicle.getSession().getPlayerEntity();
        float jumpLeapStrength = player.getVehicleJumpStrength();
        if (vehicle.isOnGround() && jumpLeapStrength == 0.0F && vehicle.getFlag(EntityFlag.STANDING) && !this.allowStandSliding) {
            return Vector3f.ZERO;
        }
        player.setVehicleJumpStrength(0);

        Vector3f inputVelocity = super.getInputVector(ctx, speed, input);

        if (vehicle.isOnGround() && jumpLeapStrength > 0) {
            if (jumpLeapStrength >= 90) {
                jumpLeapStrength = 1.0f;
            } else {
                jumpLeapStrength = 0.4f + 0.4f * jumpLeapStrength / 90.0f;
            }

            float jumpStrength = this.horseJumpStrength * getJumpVelocityMultiplier(ctx) + (this.effectJumpBoost * 0.1f);
            inputVelocity = Vector3f.from(inputVelocity.getX(), jumpStrength, inputVelocity.getZ());

            if (input.getZ() > 0.0) {
                inputVelocity = inputVelocity.add(-0.4F * TrigMath.sin(vehicle.getYaw() * 0.017453292F) * jumpLeapStrength, 0.0, 0.4F * TrigMath.cos(vehicle.getYaw() * 0.017453292F) * jumpLeapStrength);
            }
        }

        return inputVelocity;
    }

    @Override
    protected float getWaterSlowDown() {
        return vehicle instanceof SkeletonHorseEntity ? 0.96f : super.getWaterSlowDown();
    }

    @Override
    public void setEffect(Effect effect, int effectAmplifier) {
        if (effect == Effect.JUMP_BOOST) {
            effectJumpBoost = effectAmplifier + 1;
        } else {
            super.setEffect(effect, effectAmplifier);
        }
    }

    @Override
    public void removeEffect(Effect effect) {
        if (effect == Effect.JUMP_BOOST) {
            effectJumpBoost = 0;
        } else {
            super.removeEffect(effect);
        }
    }
}
