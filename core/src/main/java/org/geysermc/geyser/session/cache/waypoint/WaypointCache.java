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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerLocationPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.TrackedWaypoint;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.WaypointOperation;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundTrackedWaypointPacket;

import java.awt.Color;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WaypointCache {
    private final GeyserSession session;
    private final Map<String, GeyserWaypoint> waypoints = new Object2ObjectOpenHashMap<>();
    // TODO: remove when dropping 1.26.0 and below
    private final Map<UUID, Color> waypointColors = new Object2ObjectOpenHashMap<>();

    public WaypointCache(GeyserSession session) {
        this.session = session;
    }

    public void handlePacket(ClientboundTrackedWaypointPacket packet) {
        switch (packet.getOperation()) {
            case TRACK -> track(packet.getWaypoint());
            case UNTRACK -> untrack(packet.getWaypoint());
            case UPDATE -> update(packet.getWaypoint());
        }

        if ((packet.getOperation() == WaypointOperation.TRACK || packet.getOperation()== WaypointOperation.UNTRACK) && !GeyserWaypoint.usesNewWaypointPacket(session)) {
            // Only show locator bar when there are waypoints on it
            // This is equivalent to Java, and the Java locator_bar game rule won't work otherwise
            session.sendGameRule("locatorBar", !waypoints.isEmpty());
        }
    }

    public void addEntity(Entity entity) {
        GeyserWaypoint waypoint = waypoints.get(entity.uuid().toString());
        if (waypoint != null) {
            // On 1.26.0 and below:
            // This will remove the fake player packet previously sent to the client,
            // and change the waypoint to use the player's entity ID instead.
            // This is important because sometimes a waypoint is sent before player info telling us to list the player, so a fake player packet is sent to the client
            // When the player becomes listed the right colour will already be used, this is always put in the colours map, no matter if the
            // player info existed or not
            // On 1.26.10 and above:
            // This will re-initialise the waypoint, adding the entity ID to it and letting the client take authority
            waypoint.setEntity(entity);
        } else {
            if (!GeyserWaypoint.usesNewWaypointPacket(session)) {
                // On 1.26.0 and below:
                // If we haven't received a waypoint for the player, we need to tell the client to hide them
                // Bedrock likes to create their own waypoints for players in render distance, but Java doesn't do this, and we don't want this either, since it could
                // lead to duplicate/wrong waypoints on the locator bar
                // For example, if a Java server hides a player from the locator bar even when they're not sneaking, bedrock will still show them when in render
                // distance
                sendHidePlayerPacket(session, entity.geyserId());
            }
        }
    }

    public void removeEntity(Entity entity) {
        GeyserWaypoint waypoint = waypoints.get(entity.uuid().toString());
        if (waypoint != null) {
            // On 1.26.0 and below:
            // This will remove the player packet previously sent to the client,
            // and change the waypoint to use the player's entity ID instead.
            // This is important because a player waypoint can still show even when a player becomes unlisted,
            // so a fake player packet has to be sent to the client now
            // On 1.26.10 and above:
            // This will re-initialise the waypoint, removing the entity ID from it and not letting the client take authority
            waypoint.setEntity(null);
        }
    }

    // TODO: remove when dropping 1.26.0 and below
    public Optional<Color> getWaypointColor(UUID uuid) {
        return Optional.ofNullable(waypointColors.get(uuid));
    }

    public void tick() {
        for (GeyserWaypoint waypoint : waypoints.values()) {
            if (waypoint instanceof TickingWaypoint ticking) {
                ticking.tick();
            }
        }
    }

    private void track(TrackedWaypoint waypoint) {
        untrack(waypoint);

        Optional<UUID> uuid = Optional.ofNullable(waypoint.uuid());
        Optional<Entity> entity = uuid.flatMap(id -> Optional.ofNullable(session.getEntityCache().getEntityByUuid(id)));

        GeyserWaypoint tracked = GeyserWaypoint.create(session, entity, waypoint);
        if (tracked != null) {
            uuid.ifPresent(id -> waypointColors.put(id, tracked.color()));
            // On 1.26.0 and below, resend player entry with new waypoint colour
            entity.ifPresent(anEntity -> {
                if (!GeyserWaypoint.usesNewWaypointPacket(session) && anEntity instanceof PlayerEntity player) {
                    updatePlayerEntry(player);
                }
            });

            tracked.track(waypoint.data());
            waypoints.put(waypointId(waypoint), tracked);
        } else {
            entity.ifPresent(anEntity -> {
                if (!GeyserWaypoint.usesNewWaypointPacket(session) && anEntity instanceof PlayerEntity) {
                    // On 1.26.0 and below:
                    // When tracked waypoint is null, the waypoint shouldn't show up on the locator bar (Java type is EMPTY)
                    // If this waypoint is linked to a player, tell the bedrock client to hide it
                    // If we don't do this bedrock will show the waypoint anyway when the player is in render distance (read comments above in trackPlayer)
                    sendHidePlayerPacket(session, anEntity.geyserId());
                }
            });
        }
    }

    private void update(TrackedWaypoint waypoint) {
        getWaypoint(waypoint).ifPresent(tracked -> tracked.update(waypoint.data()));
    }

    private void untrack(TrackedWaypoint waypoint) {
        getWaypoint(waypoint).ifPresent(GeyserWaypoint::untrack);
        waypoints.remove(waypointId(waypoint));
        waypointColors.remove(waypoint.uuid());
    }

    private Optional<GeyserWaypoint> getWaypoint(TrackedWaypoint waypoint) {
        return Optional.ofNullable(waypoints.get(waypointId(waypoint)));
    }

    private static String waypointId(TrackedWaypoint waypoint) {
        return Optional.ofNullable(waypoint.uuid())
            .map(UUID::toString)
            .orElse(waypoint.id());
    }

    private void updatePlayerEntry(PlayerEntity player) {
        // No need to resend the entry if the player wasn't listed anyway,
        // it will become listed later with the right colour
        if (!player.isListed()) {
            return;
        }

        PlayerListPacket removePacket = new PlayerListPacket();
        removePacket.setAction(PlayerListPacket.Action.REMOVE);
        removePacket.getEntries().add(new PlayerListPacket.Entry(player.uuid()));
        session.sendUpstreamPacket(removePacket);

        PlayerListPacket addPacket = new PlayerListPacket();
        addPacket.setAction(PlayerListPacket.Action.ADD);
        addPacket.getEntries().add(SkinManager.buildEntryFromCachedSkin(session, player));
        session.sendUpstreamPacket(addPacket);
    }

    public void clear() {
        waypoints.clear();
        if (!GeyserWaypoint.usesNewWaypointPacket(session)) {
            session.sendGameRule("locatorBar", false);
        }
    }

    private static void sendHidePlayerPacket(GeyserSession session, long playerId) {
        PlayerLocationPacket locationPacket = new PlayerLocationPacket();
        locationPacket.setType(PlayerLocationPacket.Type.HIDE);
        locationPacket.setTargetEntityId(playerId);
        session.sendUpstreamPacket(locationPacket);
    }
}
