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

package org.geysermc.geyser.level.physics;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;

@RequiredArgsConstructor
public class YAxisSimulator {
    private final GeyserSession session;
    private float lastFPYPosition;
    @Setter
    private double doubleYPosition;
    private double velocity = Double.MAX_VALUE;

    // This is correct, don't change it, we use floating point to simulate the rounding errors that JE also have.
    private static final double FAULTY_DRAG_VELOCITY = 0.98F;

    public double simulate(PlayerAuthInputPacket packet, float yPosition) {
        final SessionPlayerEntity entity = session.getPlayerEntity();
        if (entity.isGliding() || entity.getVehicle() != null || session.getCollisionManager().isPlayerTouchingWater()) {
            this.velocity = Double.MAX_VALUE;
            return Double.parseDouble(Float.toString(yPosition));
        }

        final float deltaY = yPosition - this.lastFPYPosition;

        float lastTickEndVelY = entity.getLastTickEndVelocity().getY();
        if (this.velocity == Double.MAX_VALUE) {
            this.velocity = Double.parseDouble(Float.toString(lastTickEndVelY));
            this.lastFPYPosition = entity.position().getY();
            this.doubleYPosition = Double.parseDouble(Float.toString(entity.position().getY()));
        }

        if (packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT)) {
            this.lastFPYPosition = yPosition;
            this.doubleYPosition = Double.parseDouble(Float.toString(yPosition));
            this.velocity = Double.parseDouble(Float.toString(packet.getDelta().getY()));
            return yPosition;
        }

        double finalDeltaY = Double.parseDouble(Float.toString(deltaY));

        if (entity.isJumpedOnGround()) {
            if (Math.abs(deltaY - entity.getJumpVelocity()) <= 1.0e-4) {
                this.velocity = entity.getJumpVelocity();
            } else {
                this.velocity = finalDeltaY;
            }
        }

        boolean collidingVertically = entity.isCollidingVertically();
        if (collidingVertically) {
            // Calculating collision can be really heavy on performance, so we do this little hack instead!
            double collidedValue = Double.parseDouble(Float.toString(deltaY - lastTickEndVelY));
            double guessedNewVel = this.velocity + collidedValue;
            if (Math.abs(guessedNewVel - deltaY) < 1.0e-4) {
                finalDeltaY = guessedNewVel;
            }
        } else {
            finalDeltaY = this.velocity;
        }

        if (collidingVertically) {
            this.velocity = 0;
        }

        this.velocity = (this.velocity - 0.08D) * FAULTY_DRAG_VELOCITY;

        this.lastFPYPosition = yPosition;
        this.doubleYPosition += finalDeltaY;

        // We can trust the position when on the ground.
        if (entity.isCollidingVertically() && entity.getLastTickEndVelocity().getY() < 0) {
            this.doubleYPosition = Double.parseDouble(Float.toString(yPosition));
        }

        // Only send our position if it's close enough.
        double diff = Math.abs(this.doubleYPosition - Double.parseDouble(Float.toString(yPosition)));
        if (diff <= 1.0e-4) {
            System.out.println(diff + "," + this.doubleYPosition + "," + yPosition + "," + this.velocity + "," + packet.getDelta().getY());
        } else {
            System.out.println("Not close enough: " + diff + "," + this.doubleYPosition + "," + yPosition + "," + this.velocity + "," + packet.getDelta().getY());
            this.doubleYPosition = Double.parseDouble(Float.toString(yPosition));
            this.velocity = Double.parseDouble(Float.toString(packet.getDelta().getY() / 0.98F)) * FAULTY_DRAG_VELOCITY;
            System.out.println("New guessed velocity: " + this.velocity);
        }
        return this.doubleYPosition;
    }
}
