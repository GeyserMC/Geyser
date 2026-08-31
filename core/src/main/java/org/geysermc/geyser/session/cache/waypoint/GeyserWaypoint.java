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
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LocatorBarWaypoint;
import org.cloudburstmc.protocol.bedrock.packet.LocatorBarPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerLocationPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyle;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.TrackedWaypoint;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.WaypointData;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class GeyserWaypoint {
    // These 2 from: https://mcsrc.dev/1/26.1.2/net/minecraft/client/resources/WaypointStyle
    // (DEFAULT_NEAR_DISTANCE and DEFAULT_FAR_DISTANCE squared)
    private static final float VANILLA_NEAR_DISTANCE_SQUARED = 16384.0F;
    private static final float VANILLA_FAR_DISTANCE_SQUARED = 110224.0F;

    protected final GeyserSession session;

    // The waypoint group UUID
    // This is decided by the Java server. When Java sends us a waypoint with a String ID, we turn it into a UUID
    private final UUID uuid;
    private final LocatorBarWaypoint bedrockWaypoint;
    private final CustomWaypointStyle style;
    private final Identifier styleIdentifier;

    private Vector3f lastSentPosition = null;

    public GeyserWaypoint(GeyserSession session, UUID uuid, CustomWaypointStyle style, Identifier styleIdentifier, Color color, Optional<Entity> entity) {
        this.session = session;
        this.uuid = uuid;
        this.style = style;
        this.styleIdentifier = styleIdentifier;
        this.bedrockWaypoint = new LocatorBarWaypoint();
        bedrockWaypoint.setVisible(true);
        bedrockWaypoint.setColor(color);
        initialiseWaypointFromEntity(entity);
        setPosition(Vector3f.ZERO);
    }

    private void initialiseWaypointFromEntity(Optional<Entity> entity) {
        bedrockWaypoint.setClientPositionAuthority(entity.isPresent());
        bedrockWaypoint.setEntityUniqueId(entity.map(Entity::geyserId).orElse(null));
    }

    public Color color() {
        return bedrockWaypoint.getColor();
    }

    public void track(WaypointData data) {
        setData(data);
        sendTrackPackets(true);
    }

    private void track() {
        sendTrackPackets(true);
    }

    public void update(WaypointData data) {
        setData(data);
        sendLocationPacket();
    }

    public void untrack() {
        sendTrackPackets(false);
        lastSentPosition = null;
    }

    public void setEntity(Entity entity) {
        untrack();
        initialiseWaypointFromEntity(Optional.ofNullable(entity));
        track();
    }

    protected void setPosition(Vector3f position) {
        bedrockWaypoint.setWorldPosition(new LocatorBarWaypoint.WorldPosition(position, session.getBedrockDimension().bedrockId()));
        float distance = session.playerEntity().position().distance(position);
        String texture = style.texturePath(styleIdentifier, distance);
        Vector2f iconSize = style.textureSize(styleIdentifier, distance);

        if (texture == null) {
            GeyserImpl.getInstance().getLogger().warning("custom waypoint style for " + styleIdentifier + " returned null texture!");
            texture = "ui/locator_bar_dot_0";
        }
        if (iconSize.getX() < 0.0F || iconSize.getY() < 0.0F) {
            GeyserImpl.getInstance().getLogger().warning("custom waypoint style for " + styleIdentifier + " returned a negative texture size!");
            iconSize = Vector2f.ZERO;
        }
        bedrockWaypoint.setTexturePath("textures/" + texture);
        bedrockWaypoint.setIconSize(iconSize);
    }

    protected void sendLocationPacket() {
        Vector3f position = bedrockWaypoint.getWorldPosition().getPosition();
        if (lastSentPosition == null || position.distanceSquared(lastSentPosition) > 1.0F) {
            LocatorBarPacket packet = new LocatorBarPacket();
            bedrockWaypoint.setUpdateFlag(WaypointUpdateFlags.WORLD_POS | WaypointUpdateFlags.TEXTURE_PATH | WaypointUpdateFlags.ICON_SIZE);
            packet.setWaypoints(List.of(new LocatorBarPacket.Payload(LocatorBarPacket.Action.UPDATE, uuid, bedrockWaypoint)));
            session.sendUpstreamPacket(packet);

            lastSentPosition = position;
        }
    }

    private void sendTrackPackets(boolean add) {
        LocatorBarPacket packet = new LocatorBarPacket();
        bedrockWaypoint.setUpdateFlag(add ? WaypointUpdateFlags.ALL : 0);
        packet.setWaypoints(List.of(new LocatorBarPacket.Payload(add ? LocatorBarPacket.Action.ADD : LocatorBarPacket.Action.REMOVE, uuid, bedrockWaypoint)));
        session.sendUpstreamPacket(packet);
    }

    public abstract void setData(WaypointData data);

    public static @Nullable GeyserWaypoint create(GeyserSession session, Optional<Entity> entity, TrackedWaypoint waypoint, Map<Identifier, CustomWaypointStyle> waypointStyles) {
        UUID uuid = Optional.ofNullable(waypoint.uuid())
            .or(() -> Optional.ofNullable(waypoint.id())
                .map(UUID::fromString))
            .orElseThrow();
        Identifier styleIdentifier = MinecraftKey.keyToIdentifier(waypoint.icon().style());
        CustomWaypointStyle style = waypointStyles.getOrDefault(styleIdentifier, VanillaWaypoint.VANILLA_DEFAULT);
        Color color = getWaypointColor(waypoint);
        return switch (waypoint.type()) {
            case EMPTY -> null;
            case VEC3I -> new CoordinatesWaypoint(session, uuid, style, styleIdentifier, color, entity);
            case CHUNK -> new ChunkWaypoint(session, uuid, style, styleIdentifier, color, entity);
            case AZIMUTH -> new AzimuthWaypoint(session, uuid, style, styleIdentifier, color, entity);
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
