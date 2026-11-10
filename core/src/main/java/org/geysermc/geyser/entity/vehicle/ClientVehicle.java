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

import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.Entity;

public interface ClientVehicle {
    VehicleComponent<?> getVehicleComponent();

    // MojMap LivingEntity#getRiddenInput
    Vector3f getRiddenInput(Vector2f input);

    // MojMap LivingEntity#getRiddenSpeed
    float getVehicleSpeed();

    /**
     * Whether the session is controlling this vehicle, i.e. riding a (saddled) horse or holding a
     * carrot on a stick while on a pig (warped fungus on a stick for a strider)
     * <p>
     * @see #shouldSimulateMovement() which double-checks that the vehicle is not riding anything itself
     * <p>
     * MojMap Mob#getControllingPassenger
     *
     * @see #shouldSimulateMovement()
     */
    boolean canSimulateMovement();

    Entity getVehicle();

    /**
     * Whether Geyser should simulate vehicle movement and send it to the backend.
     * <p>
     * Java clients only simulate the main controlled vehicle.
     * MojMap {@code LocalPlayer#tick} only sends a {@code ServerboundMoveVehiclePacket} for {@code getRootVehicle()}.
     * Likewise, a vehicle that is a passenger, e.g. a pig in a minecart, is controlled by the vehicle it is riding...
     * See MojMap {@code Entity#rideTick} - if we simulate a vehicle (while it's a passenger), welcome to
     * "was expecting to be controlling vehicle" galore (Geyser also ignores backend position updates due to this).
     * <p>
     * Unlike {@link #canSimulateMovement()}, this double-checks that the vehicle isn't actually a passenger and <i>should</i> be simulated.
     */
    default boolean shouldSimulateMovement() {
        return getVehicle() == null && canSimulateMovement();
    }

    default boolean canWalkOnLava() {
        return false;
    }

    default boolean canClimb() {
        return true;
    }
}
