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

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.type.living.animal.nautilus.AbstractNautilusEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.util.MathUtils;

public class NautilusVehicleComponent extends VehicleComponent<AbstractNautilusEntity> {
    private int dashCooldown;

    public NautilusVehicleComponent(AbstractNautilusEntity vehicle, float stepHeight, float defSpeed) {
        super(vehicle, stepHeight);
        this.moveSpeed = defSpeed;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void tickVehicle() {
        vehicle.setFlag(EntityFlag.HAS_DASH_COOLDOWN, this.dashCooldown > 0);
        vehicle.updateBedrockMetadata();

        super.tickVehicle();
        if (this.dashCooldown > 0) {
            this.dashCooldown--;
        }
    }

    @Override
    protected Vector3f getInputVector(VehicleComponent<AbstractNautilusEntity>.VehicleContext ctx, float speed, Vector3f input) {
        Vector3f inputVelocity = super.getInputVector(ctx, speed, input);

        SessionPlayerEntity player = vehicle.getSession().getPlayerEntity();
        float jumpStrength = player.getVehicleJumpStrength();
        player.setVehicleJumpStrength(0);

        // We don't check for dash cooldown here since we already send a vehicle jump packet beforehand, which the server can send us back
        // the metadata that set dash cooldown before we can handle the input vector.
        if (jumpStrength > 0) {
            final Vector3f viewVector = MathUtils.calculateViewVector(player.getPitch(), player.getYaw());

            float movementMultiplier = getVelocityMultiplier(ctx);
            float strength = (float) (movementMultiplier + movementEfficiency * (1 - movementMultiplier));
            jumpStrength = (jumpStrength >= 90) ? 1.0F : (0.4F + 0.4F * jumpStrength / 90.0F);

            inputVelocity = inputVelocity.add(viewVector.mul(((this.isInWater() ? 1.2F : 0.5F) * jumpStrength) * getMoveSpeed() * strength));
            setDashCooldown(40);
        }

        return inputVelocity;
    }

    @Override
    public void onDismount() {
        vehicle.setFlag(EntityFlag.HAS_DASH_COOLDOWN, false);
        vehicle.updateBedrockMetadata();
        super.onDismount();
    }

    @Override
    protected void updateRotation() {
        float yaw = vehicle.getYaw() + MathUtils.wrapDegrees(getRiddenRotation().getX() - vehicle.getYaw()) * 0.5F;
        vehicle.setYaw(yaw);
        vehicle.setHeadYaw(yaw);
    }

    @Override
    protected void waterMovement(VehicleComponent<AbstractNautilusEntity>.VehicleContext ctx) {
        travel(ctx, vehicle.getVehicleSpeed());
        this.vehicle.setMotion(this.vehicle.getMotion().mul(0.9f));
    }

    public void setDashCooldown(int cooldown) {
        this.dashCooldown = this.dashCooldown == 0 ? cooldown : this.dashCooldown;
        vehicle.setFlag(EntityFlag.HAS_DASH_COOLDOWN, this.dashCooldown > 0);
        vehicle.updateBedrockMetadata();
    }
}
