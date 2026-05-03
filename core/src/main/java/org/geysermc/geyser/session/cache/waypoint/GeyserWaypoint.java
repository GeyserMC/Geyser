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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LocatorBarWaypoint;
import org.cloudburstmc.protocol.bedrock.packet.LocatorBarPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerLocationPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.TrackedWaypoint;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.WaypointData;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class GeyserWaypoint {
    protected final GeyserSession session;

    // On 26.10 and above (new waypoint system): the waypoint group UUID
    // On 26.0 and below (old waypoint system): the UUID of the waypoint
    // This is decided by the Java server. When Java sends us a waypoint with a String ID, we turn it into a UUID
    private final UUID uuid;
    private final LocatorBarWaypoint bedrockWaypoint;
    private final boolean requiresNewWaypointPacket;
    private boolean sendListPackets;

    private Vector3f lastSentPosition = null;

    public GeyserWaypoint(GeyserSession session, UUID uuid, Optional<Entity> entity, Color color) {
        this.session = session;
        this.uuid = uuid;
        this.bedrockWaypoint = new LocatorBarWaypoint();
        this.requiresNewWaypointPacket = requiresNewWaypointPacket(session);
        bedrockWaypoint.setVisible(true);
        bedrockWaypoint.setWorldPosition(new LocatorBarWaypoint.WorldPosition(Vector3f.ZERO, 0));
        bedrockWaypoint.setTextureId(2);
        bedrockWaypoint.setColor(color);
        initialiseWaypointFromEntity(entity);
    }

    private void initialiseWaypointFromEntity(Optional<Entity> entity) {
        bedrockWaypoint.setClientPositionAuthority(entity.isPresent());
        bedrockWaypoint.setEntityUniqueId(entity.map(Entity::geyserId).orElseGet(() -> session.getEntityCache().nextEntityId()));

        if (requiresNewWaypointPacket) {
            this.sendListPackets = false;
        } else {
            this.sendListPackets = entity.isEmpty();
        }
    }

    public Color color() {
        return bedrockWaypoint.getColor();
    }

    public void track(WaypointData data) {
        sendTrackPackets(true);
        update(data);
    }

    private void track() {
        sendTrackPackets(true);
    }

    public void update(WaypointData data) {
        setData(data);
        sendLocationPacket(false);
    }

    public void untrack() {
        if (!requiresNewWaypointPacket) {
            PlayerLocationPacket packet = new PlayerLocationPacket();
            packet.setType(PlayerLocationPacket.Type.HIDE);
            packet.setTargetEntityId(bedrockWaypoint.getEntityUniqueId());
            session.sendUpstreamPacket(packet);
        }
        sendTrackPackets(false);
    }

    public void setEntity(Entity entity) {
        if (requiresNewWaypointPacket) {
            untrack();
            initialiseWaypointFromEntity(Optional.ofNullable(entity));
            track();
        } else {
            // 26.0 and below does not support non-player entities as waypoint target
            if (!(entity instanceof PlayerEntity)) {
                entity = null;
            }

            if (sendListPackets) {
                if (entity == null) {
                    // We're already emulating the waypoint with player list packets
                    // Could occur due to player list shenanigans for PlayStation devices
                    return;
                }
                untrack();
                initialiseWaypointFromEntity(Optional.of(entity));
                sendLocationPacket(true);
            } else if (entity == null) { // Previously had an attached player, and now that player is gone
                initialiseWaypointFromEntity(Optional.empty());
                sendTrackPackets(true);
                sendLocationPacket(true);
            }
        }
    }

    protected void setPosition(Vector3f position) {
        bedrockWaypoint.setWorldPosition(new LocatorBarWaypoint.WorldPosition(position, session.getBedrockDimension().bedrockId()));
    }

    protected void sendLocationPacket(boolean force) {
        Vector3f position = bedrockWaypoint.getWorldPosition().getPosition();
        if (force || lastSentPosition == null || position.distanceSquared(lastSentPosition) > 1.0F) {
            if (requiresNewWaypointPacket) {
                LocatorBarPacket packet = new LocatorBarPacket();
                bedrockWaypoint.setUpdateFlag(WaypointUpdateFlags.WORLD_POS);
                packet.setWaypoints(List.of(new LocatorBarPacket.Payload(LocatorBarPacket.Action.UPDATE, uuid, bedrockWaypoint)));
                System.out.println("sending " + packet);
                session.sendUpstreamPacket(packet);
            } else {
                PlayerLocationPacket packet = new PlayerLocationPacket();
                packet.setType(PlayerLocationPacket.Type.COORDINATES);
                packet.setTargetEntityId(bedrockWaypoint.getEntityUniqueId());
                packet.setPosition(position);
                session.sendUpstreamPacket(packet);
            }

            lastSentPosition = position;
        }
    }

    private void sendTrackPackets(boolean add) {
        if (requiresNewWaypointPacket) {
            LocatorBarPacket packet = new LocatorBarPacket();
            bedrockWaypoint.setUpdateFlag(add ? WaypointUpdateFlags.ALL : 0);
            packet.setWaypoints(List.of(new LocatorBarPacket.Payload(add ? LocatorBarPacket.Action.ADD : LocatorBarPacket.Action.REMOVE, uuid, bedrockWaypoint)));
            System.out.println("sending " + packet);
            session.sendUpstreamPacket(packet);
        } else if (sendListPackets) {
            PlayerListPacket packet = new PlayerListPacket();
            packet.setAction(add ? PlayerListPacket.Action.ADD : PlayerListPacket.Action.REMOVE);

            // Not sending a skin causes a player list entry to be invalid,
            // leading to waypoints not showing
            PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
            entry.setEntityId(bedrockWaypoint.getEntityUniqueId());
            entry.setColor(bedrockWaypoint.getColor());
            entry.setName("");
            entry.setSkin(SkinProvider.EMPTY_SERIALIZED_SKIN);
            entry.setXuid("");
            entry.setPlatformChatId("");
            entry.setTrustedSkin(true);
            packet.getEntries().add(entry);

            session.sendUpstreamPacket(packet);
        }
    }

    public abstract void setData(WaypointData data);

    public static @Nullable GeyserWaypoint create(GeyserSession session, Optional<Entity> entity, TrackedWaypoint waypoint) {
        UUID uuid = Optional.ofNullable(waypoint.uuid())
            .or(() -> Optional.ofNullable(waypoint.id())
                .map(UUID::fromString))
            .orElseThrow();
        Color color = getWaypointColor(waypoint);
        return switch (waypoint.type()) {
            case EMPTY -> null;
            case VEC3I -> new CoordinatesWaypoint(session, uuid, entity, color);
            case CHUNK -> new ChunkWaypoint(session, uuid, entity, color);
            case AZIMUTH -> new AzimuthWaypoint(session, uuid, entity, color);
        };
    }

    public static boolean requiresNewWaypointPacket(GeyserSession session) {
        return GameProtocol.is1_26_10orHigher(session.protocolVersion());
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
