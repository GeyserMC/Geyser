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

        if (packet.getOperation() == WaypointOperation.TRACK || packet.getOperation()== WaypointOperation.UNTRACK) {
            
            
            session.sendGameRule("locatorBar", !waypoints.isEmpty());
        }
    }

    public void listPlayer(PlayerEntity player) {
        GeyserWaypoint waypoint = waypoints.get(player.uuid().toString());
        if (waypoint != null) {
            
            
            
            
            
            waypoint.setPlayer(player);
        } else {
            
            
            
            
            
            PlayerLocationPacket locationPacket = new PlayerLocationPacket();
            locationPacket.setType(PlayerLocationPacket.Type.HIDE);
            locationPacket.setTargetEntityId(player.geyserId());
            session.sendUpstreamPacket(locationPacket);
        }
    }

    public void unlistPlayer(PlayerEntity player) {
        GeyserWaypoint waypoint = waypoints.get(player.uuid().toString());
        if (waypoint != null) {
            
            
            
            
            waypoint.setPlayer(null);
        }
    }

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
        Optional<PlayerEntity> player = uuid.flatMap(id -> Optional.ofNullable(session.getEntityCache().getPlayerEntity(id)));

        GeyserWaypoint tracked = GeyserWaypoint.create(session, player, waypoint);
        if (tracked != null) {
            uuid.ifPresent(id -> waypointColors.put(id, tracked.color()));
            
            player.ifPresent(this::updatePlayerEntry);

            tracked.track(waypoint.data());
            waypoints.put(waypointId(waypoint), tracked);
        } else {
            player.ifPresent(playerEntity -> {
                
                
                
                PlayerLocationPacket locationPacket = new PlayerLocationPacket();
                locationPacket.setType(PlayerLocationPacket.Type.HIDE);
                locationPacket.setTargetEntityId(playerEntity.geyserId());
                session.sendUpstreamPacket(locationPacket);
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
        session.sendGameRule("locatorBar", false);
    }
}
