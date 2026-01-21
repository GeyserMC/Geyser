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

import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerLocationPacket;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.TrackedWaypoint;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.WaypointData;

import java.awt.Color;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

@Accessors(fluent = true)
public abstract class GeyserWaypoint {
    protected final GeyserSession session;

    @Getter
    private final Color color;
    private final UUID entityUuid;
    private long entityId;
    private boolean sendListPackets;

    protected Vector3f position = Vector3f.ZERO;
    private Vector3f lastSent = null;

    public GeyserWaypoint(GeyserSession session, Optional<UUID> uuid, OptionalLong entityId, Color color) {
        this.session = session;
        this.color = color;

        this.entityUuid = uuid.orElseGet(UUID::randomUUID);
        this.entityId = entityId.orElseGet(() -> session.getEntityCache().getNextEntityId().incrementAndGet());
        this.sendListPackets = entityId.isEmpty();
    }

    public void track(WaypointData data) {
        sendListPackets(PlayerListPacket.Action.ADD);
        update(data);
    }

    public void update(WaypointData data) {
        setData(data);
        sendLocationPacket(false);
    }

    public void untrack() {
        PlayerLocationPacket packet = new PlayerLocationPacket();
        packet.setType(PlayerLocationPacket.Type.HIDE);
        packet.setTargetEntityId(entityId);
        session.sendUpstreamPacket(packet);
        sendListPackets(PlayerListPacket.Action.REMOVE);
    }

    public void setPlayer(PlayerEntity entity) {
        if (sendListPackets) {
            if (entity == null) {
                // We're already emulating the waypoint with player list packets
                // Could occur due to player list shenanigans for PlayStation devices
                return;
            }
            untrack();
            entityId = entity.geyserId();
            sendListPackets = false;
            sendLocationPacket(true);
        } else if (entity == null) { // Previously had an attached player, and now that player is gone
            entityId = session.getEntityCache().getNextEntityId().incrementAndGet();
            sendListPackets = true;
            sendListPackets(PlayerListPacket.Action.ADD);
            sendLocationPacket(true);
        }
    }

    protected void sendLocationPacket(boolean force) {
        if (force || lastSent == null || position.distanceSquared(lastSent) > 1.0F) {
            PlayerLocationPacket packet = new PlayerLocationPacket();
            packet.setType(PlayerLocationPacket.Type.COORDINATES);
            packet.setTargetEntityId(entityId);
            packet.setPosition(position);
            session.sendUpstreamPacket(packet);

            lastSent = position;
        }
    }

    private void sendListPackets(PlayerListPacket.Action action) {
        if (sendListPackets) {
            PlayerListPacket packet = new PlayerListPacket();
            packet.setAction(action);

            PlayerListPacket.Entry entry = new PlayerListPacket.Entry(entityUuid);
            entry.setEntityId(entityId);
            entry.setColor(color);
            packet.getEntries().add(entry);

            session.sendUpstreamPacket(packet);
        }
    }

    public abstract void setData(WaypointData data);

    public static @Nullable GeyserWaypoint create(GeyserSession session, Optional<UUID> uuid, OptionalLong entityId, TrackedWaypoint waypoint) {
        Color color = getWaypointColor(waypoint);
        return switch (waypoint.type()) {
            case EMPTY -> null;
            case VEC3I -> new CoordinatesWaypoint(session, uuid, entityId, color);
            case CHUNK -> new ChunkWaypoint(session, uuid, entityId, color);
            case AZIMUTH -> new AzimuthWaypoint(session, uuid, entityId, color);
        };
    }

    private static Color getWaypointColor(TrackedWaypoint waypoint) {
        // Use icon's colour, or calculate from UUID/ID if it is not specified
        // This is similar to how Java does it, but they do some brightness modifications too, which is a lot of math (see LocatorBarRenderer)
        return waypoint.icon().color()
            .or(() -> Optional.ofNullable(waypoint.uuid()).map(UUID::hashCode))
            .or(() -> Optional.ofNullable(waypoint.id()).map(String::hashCode))
            .map(i -> new Color(i & 0xFFFFFF))
            .orElseThrow();
    }
}
