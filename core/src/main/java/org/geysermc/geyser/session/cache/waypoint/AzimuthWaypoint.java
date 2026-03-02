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

package org.geysermc.geyser.session.cache.waypoint;

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.AzimuthWaypointData;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.WaypointData;

import java.awt.Color;
import java.util.Optional;

public class AzimuthWaypoint extends GeyserWaypoint implements TickingWaypoint {

    // In Java, this waypoint always appears really far, so set the distance far here too,
    // This also makes the waypoint more accurate on the bar and less susceptible to the player moving
    private static final float WAYPOINT_DISTANCE = 1000.0F;

    // The angle, in radians, where the waypoint should appear on the bar
    private float angle = 0.0F;

    public AzimuthWaypoint(GeyserSession session, Optional<PlayerEntity> player, Color color) {
        super(session, player, color);
    }

    @Override
    public void setData(WaypointData data) {
        if (data instanceof AzimuthWaypointData azimuthData) {
            angle = azimuthData.angle();
            updatePosition();
        } else {
            session.getGeyser().getLogger().warning("Received incorrect waypoint data " + data.getClass() + " for azimuth waypoint");
        }
    }

    @Override
    public void tick() {
        // Update position so that it remains accurate to the angle as the player moves around
        updatePosition();
        sendLocationPacket(false);
    }

    private void updatePosition() {
        Vector3f playerPosition = session.getPlayerEntity().position();
        // Unit circle math!
        // Unintuitively, the delta x corresponds to the sin of the angle, and delta z to the cos
        float dx = (float) -(Math.sin(angle) * WAYPOINT_DISTANCE);
        float dz = (float) (Math.cos(angle) * WAYPOINT_DISTANCE);
        // Set Y to the player's Y since this waypoint always appears in the centre of the bar on Java
        position = Vector3f.from(playerPosition.getX() + dx, playerPosition.getY(), playerPosition.getZ() + dz);
    }
}
