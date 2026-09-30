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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.connection.SessionDefineCustomWaypointsEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyle;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyleRegisterException;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.TrackedWaypoint;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundTrackedWaypointPacket;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WaypointCache {
    private final GeyserSession session;
    private final Map<Identifier, CustomWaypointStyle> waypointStyles;
    private final Map<String, GeyserWaypoint> waypoints = new Object2ObjectOpenHashMap<>();

    public WaypointCache(GeyserSession session) {
        this.session = session;

        Map<Identifier, CustomWaypointStyle> styles = new Object2ObjectOpenHashMap<>();

        SessionDefineCustomWaypointsEvent event = new SessionDefineCustomWaypointsEvent(session) {
            @Override
            public Map<Identifier, CustomWaypointStyle> customWaypointStyles() {
                return Collections.unmodifiableMap(styles);
            }

            @Override
            public void register(Identifier identifier, CustomWaypointStyle style) {
                Objects.requireNonNull(identifier, "identifier may not be null");
                Objects.requireNonNull(style, "style may not be null");
                if (styles.containsKey(identifier)) {
                    throw new CustomWaypointStyleRegisterException("Not registering waypoint style with identifier " + identifier + " as it was already registered");
                } else {
                    styles.put(identifier, style);
                }
            }
        };

        // First, include the session-agnostic mappings
        styles.putAll(Registries.WAYPOINT_STYLE_MAPPINGS.get());
        // Then, fire the event to the API
        GeyserImpl.getInstance().eventBus().fire(event);

        // Include the vanilla default waypoint style if it was not overridden
        if (!styles.containsKey(VanillaWaypoint.VANILLA_WAYPOINT_STYLE)) {
            styles.put(VanillaWaypoint.VANILLA_WAYPOINT_STYLE, VanillaWaypoint.VANILLA_DEFAULT);
        }

        waypointStyles = Collections.unmodifiableMap(styles);
    }

    public void handlePacket(ClientboundTrackedWaypointPacket packet) {
        switch (packet.getOperation()) {
            case TRACK -> track(packet.getWaypoint());
            case UNTRACK -> untrack(packet.getWaypoint());
            case UPDATE -> update(packet.getWaypoint());
        }
    }

    public void addEntity(Entity entity) {
        UUID uuid = entity.uuid();
        if (uuid == null) {
            return;
        }

        GeyserWaypoint waypoint = waypoints.get(uuid.toString());
        if (waypoint != null) {
            // On 26.0 and below:
            // This will remove the fake player packet previously sent to the client,
            // and change the waypoint to use the player's entity ID instead.
            // This is important because sometimes a waypoint is sent before player info telling us to list the player, so a fake player packet is sent to the client
            // When the player becomes listed the right colour will already be used, this is always put in the colours map, no matter if the
            // player info existed or not
            // On 26.10 and above:
            // This will re-initialise the waypoint, adding the entity ID to it and letting the client take authority
            waypoint.setEntity(entity);
        }
    }

    public void removeEntity(Entity entity) {
        UUID uuid = entity.uuid();
        if (uuid == null) {
            return;
        }

        GeyserWaypoint waypoint = waypoints.get(uuid.toString());
        if (waypoint != null) {
            // On 26.0 and below:
            // This will remove the player packet previously sent to the client,
            // and change the waypoint to use the player's entity ID instead.
            // This is important because a player waypoint can still show even when a player becomes unlisted,
            // so a fake player packet has to be sent to the client now
            // On 26.10 and above:
            // This will re-initialise the waypoint, removing the entity ID from it and not letting the client take authority
            waypoint.setEntity(null);
        }
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

        GeyserWaypoint tracked = GeyserWaypoint.create(session, entity, waypoint, waypointStyles);
        if (tracked != null) {
            tracked.track(waypoint.data());
            waypoints.put(waypointId(waypoint), tracked);
        }
    }

    private void update(TrackedWaypoint waypoint) {
        getWaypoint(waypoint).ifPresent(tracked -> tracked.update(waypoint.data()));
    }

    private void untrack(TrackedWaypoint waypoint) {
        getWaypoint(waypoint).ifPresent(GeyserWaypoint::untrack);
        waypoints.remove(waypointId(waypoint));
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
        PlayerListPacket.Entry removeEntry = new PlayerListPacket.Entry(player.uuid());
        removeEntry.setAction(PlayerListPacket.Action.REMOVE);
        removePacket.getEntries().add(removeEntry);
        session.sendUpstreamPacket(removePacket);

        PlayerListPacket addPacket = new PlayerListPacket();
        addPacket.setAction(PlayerListPacket.Action.ADD);
        PlayerListPacket.Entry addEntry = SkinManager.buildEntryFromCachedSkin(session, player);
        addEntry.setAction(PlayerListPacket.Action.ADD);
        addPacket.getEntries().add(addEntry);
        session.sendUpstreamPacket(addPacket);
    }

    public void clear() {
        waypoints.clear();
    }

    private static void sendHidePlayerPacket(GeyserSession session, long playerId) {
        PlayerLocationPacket locationPacket = new PlayerLocationPacket();
        locationPacket.setType(PlayerLocationPacket.Type.HIDE);
        locationPacket.setTargetEntityId(playerId);
        session.sendUpstreamPacket(locationPacket);
    }
}
