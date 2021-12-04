/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import com.nukkitx.math.vector.Vector3d;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents a teleport ID and corresponding coordinates that need to be confirmed. <br>
 *
 * The vanilla Java client, after getting a
 * {@link com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket},
 * adjusts the player's positions and immediately sends a teleport back. However, we want to acknowledge that the
 * Bedrock player actually moves close to that point, so we store the teleport until we get a movement packet from
 * Bedrock that the teleport was successful.
 */
@RequiredArgsConstructor
@Data
public class TeleportCache {

    private static final double ERROR_X_AND_Z = 0.1;
    private static final double ERROR_Y = 0.1;

    /**
     * How many move packets the teleport can be unconfirmed for before it gets resent to the client
     */
    private static final int RESEND_THRESHOLD = 5;

    private final double x, y, z;
    private final float pitch, yaw;
    private final int teleportConfirmId;

    private int unconfirmedFor = 0;

    public boolean canConfirm(Vector3d position) {
        return (Math.abs(this.x - position.getX()) < ERROR_X_AND_Z &&
                Math.abs(this.y - position.getY()) < ERROR_Y &&
                Math.abs(this.z - position.getZ()) < ERROR_X_AND_Z);
    }

    public void incrementUnconfirmedFor() {
        unconfirmedFor++;
    }

    public boolean shouldResend() {
        return unconfirmedFor >= RESEND_THRESHOLD;
    }
}
