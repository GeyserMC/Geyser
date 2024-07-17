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

import lombok.Setter;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class CamelVehicleComponent extends VehicleComponent<CamelEntity> {
    private static final int STANDING_TICKS = 52;
    private static final int DASH_TICKS = 55;

    @Setter
    private float horseJumpStrength = 0.42f; // Not sent by vanilla Java server when spawned

    @Setter
    private long lastPoseTick;

    private int dashTick;
    private int effectJumpBoost;

    public CamelVehicleComponent(CamelEntity vehicle) {
        super(vehicle, 1.5f);
    }

    public void startDashCooldown() {
        // tickVehicle is only called while the vehicle is mounted. Use session ticks to keep
        // track of time instead of counting down
        this.dashTick = vehicle.getSession().getTicks() + DASH_TICKS;
    }

    @Override
    public void tickVehicle() {
        if (this.dashTick != 0) {
            if (vehicle.getSession().getTicks() > this.dashTick) {
                vehicle.setFlag(EntityFlag.HAS_DASH_COOLDOWN, false);
                this.dashTick = 0;
            } else {
                vehicle.setFlag(EntityFlag.HAS_DASH_COOLDOWN, true);
            }
        }

        vehicle.setFlag(EntityFlag.CAN_DASH, vehicle.getFlag(EntityFlag.SADDLED) && !isStationary());
        vehicle.updateBedrockMetadata();
        super.tickVehicle();
    }

    @Override
    public void onDismount() {
        // Prevent camel from getting stuck in dash animation
        vehicle.setFlag(EntityFlag.HAS_DASH_COOLDOWN, false);
        vehicle.updateBedrockMetadata();
        super.onDismount();
    }

    @Override
    protected boolean travel(VehicleContext ctx, float speed) {
        if (vehicle.isOnGround() && isStationary()) {
            vehicle.setMotion(vehicle.getMotion().mul(0, 1, 0));
        }

        return super.travel(ctx, speed);
    }

    @Override
    protected Vector3f getInputVelocity(VehicleContext ctx, float speed) {
        if (isStationary()) {
            return Vector3f.ZERO;
        }

        SessionPlayerEntity player = vehicle.getSession().getPlayerEntity();
        Vector3f inputVelocity = super.getInputVelocity(ctx, speed);
        float jumpStrength = player.getVehicleJumpStrength();

        if (jumpStrength > 0) {
            player.setVehicleJumpStrength(0);

            if (jumpStrength >= 90) {
                jumpStrength = 1.0f;
            } else {
                jumpStrength = 0.4f + 0.4f * jumpStrength / 90.0f;
            }

            return inputVelocity.add(Vector3f.createDirectionDeg(0, -player.getYaw())
                    .mul(22.2222f * jumpStrength * this.moveSpeed * getVelocityMultiplier(ctx))
                    .up(1.4285f * jumpStrength * (this.horseJumpStrength * getJumpVelocityMultiplier(ctx) + (this.effectJumpBoost * 0.1f))));
        }

        return inputVelocity;
    }

    @Override
    protected Vector2f getVehicleRotation() {
        if (isStationary()) {
            return Vector2f.from(vehicle.getYaw(), vehicle.getPitch());
        }
        return super.getVehicleRotation();
    }

    /**
     * Checks if the camel is sitting
     * or transitioning to standing pose.
     */
    private boolean isStationary() {
        // Java checks if sitting using lastPoseTick
        return this.lastPoseTick < 0 || vehicle.getSession().getWorldTicks() < this.lastPoseTick + STANDING_TICKS;
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
