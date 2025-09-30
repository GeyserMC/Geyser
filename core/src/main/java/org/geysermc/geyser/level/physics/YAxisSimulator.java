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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;

// On Java Edition, the formula for normal falling motion is (lastVelocityY - 0.08D) * 0.98F
// However since they're using double for their motion and position but still use 0.98F, rounding error happened
// and now the value is 0.9800000190734863 and lets said for eg: the player velocity is 0, then their next velocity will be
// -0.0784000015258789, however since Bedrock using floating point, this rounding error doesn't happen and their velocity will be
// the nice and neatly -0.0784 value. Some anticheat do check for this however and will flag Geyser every single tick.
// Also apply to jump motion, rounding error make it become 0.41999998688697815, while on Bedrock it's just 0.42
// That why this class exist, simply to account for these cases silently, since the differences mostly are just 1.0E-8 to 1.0E-5.
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

        // Most anticheat is lenient is enough to exempt these cases, if it's not however then we will flag regardless anyway
        // due to how different bedrock and java handle this.
        if (entity.isGliding() || entity.getVehicle() != null || session.getCollisionManager().isPlayerTouchingWater()) {
            this.velocity = Double.MAX_VALUE; // Mark this so we can rest values.
            return Double.parseDouble(Float.toString(yPosition));
        }

        // We haven't cache any value to even start with yet, so use the values bedrock provided.
        float lastTickEndVelY = entity.getLastTickEndVelocity().getY();
        if (this.velocity == Double.MAX_VALUE) {
            this.velocity = Double.parseDouble(Float.toString(lastTickEndVelY));
            this.lastFPYPosition = entity.position().getY();
            this.doubleYPosition = Double.parseDouble(Float.toString(entity.position().getY()));
        }

        // The player just accepted a teleport, no reason to correct them.
        if (packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT)) {
            this.lastFPYPosition = yPosition;
            this.doubleYPosition = Double.parseDouble(Float.toString(yPosition));
            this.velocity = Double.parseDouble(Float.toString(packet.getDelta().getY()));
            return yPosition;
        }

        final float deltaY = yPosition - this.lastFPYPosition;
        double finalDeltaY = Double.parseDouble(Float.toString(deltaY));

        if (entity.isJumpedOnGround()) {
            // If the jump velocity is close enough then use our value since player delta could contain floating point errors.
            // If it's not however, then use player value as it will be more accurate.
            if (Math.abs(deltaY - entity.getJumpVelocity()) <= 1.0e-4) {
                this.velocity = entity.getJumpVelocity();
            } else {
                this.velocity = finalDeltaY;
            }
        }

        boolean collidingVertically = entity.isCollidingVertically();
        if (collidingVertically) {
            // Calculating collision can be really heavy on performance, so we do this little hack instead!
            // By doing this way, we can filter out the small position error while ensuring the velocity is still in fact correct!
            double collidedValue = Double.parseDouble(Float.toString(deltaY - lastTickEndVelY));
            double guessedNewVel = this.velocity + collidedValue;
            // If it's close enough then use our value, if not then just use player value.
            if (Math.abs(guessedNewVel - deltaY) < 1.0e-4) {
                finalDeltaY = guessedNewVel;
            }
        } else {
            // We should be correct! So use our old velocity value instead.
            finalDeltaY = this.velocity;
        }

        // Vanilla behaviour, there is also cases for slimes, honey, beds, .... however most anticheat
        // exempt them anyway, even if they don't, we can still reset the values when it differs enough.
        if (collidingVertically) {
            this.velocity = 0;
        }

        // We can track for slow falling but since latency is a thing, it won't be accurate...
        // Of course, we can still rely on the latency packet to account for well. latency but this is an easier way so.
        boolean isSlowFalling = Math.abs(((packet.getDelta().getY() / 0.98F) - 0.01F) - this.velocity) < 1.0E-3;
        this.velocity = (this.velocity - (isSlowFalling ? 0.01D : 0.08D)) * FAULTY_DRAG_VELOCITY;

        this.lastFPYPosition = yPosition;
        this.doubleYPosition += finalDeltaY;

        // Most anticheats only check for this when player is in the air. And since player is on ground there is nothing much to correct.
        if (entity.isCollidingVertically() && entity.getLastTickEndVelocity().getY() < 0) {
            this.doubleYPosition = Double.parseDouble(Float.toString(yPosition));
        }

        // We want to check for the position different in cases we haven't accounted for (and probably won't be needed to anyway).
        // If it differs too much, then don't use our values, send player one and then reset our values to match.
        double diff = Math.abs(this.doubleYPosition - Double.parseDouble(Float.toString(yPosition)));
        if (diff > 1.0e-4) {
            GeyserImpl.getInstance().getLogger().warning("Not close enough: " + diff + "," + this.doubleYPosition + "," + yPosition + "," + this.velocity + "," + packet.getDelta().getY());

            // We differ too much! Sadly, so use player value and apply the faulty drag value aka 0.9800000190734863
            this.doubleYPosition = Double.parseDouble(Float.toString(yPosition));
            this.velocity = Double.parseDouble(Float.toString(packet.getDelta().getY() / 0.98F)) * FAULTY_DRAG_VELOCITY;
        } else {
            GeyserImpl.getInstance().getLogger().info(diff + "," + this.doubleYPosition + "," + yPosition + "," + this.velocity + "," + packet.getDelta().getY());
        }
        return this.doubleYPosition;
    }
}
