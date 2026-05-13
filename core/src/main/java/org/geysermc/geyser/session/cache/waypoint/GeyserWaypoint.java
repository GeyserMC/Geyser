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
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.TrackedWaypoint;
import org.geysermc.mcprotocollib.protocol.data.game.level.waypoint.WaypointData;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class GeyserWaypoint {
    // These 2 from: https://mcsrc.dev/1/26.1.2/net/minecraft/client/resources/WaypointStyle
    // (DEFAULT_NEAR_DISTANCE and DEFAULT_FAR_DISTANCE squared)
    private static final float VANILLA_NEAR_DISTANCE_SQUARED = 16384.0F;
    private static final float VANILLA_FAR_DISTANCE_SQUARED = 110224.0F;

    protected final GeyserSession session;

    // On 26.10 and above (new waypoint system): the waypoint group UUID
    // On 26.0 and below (old waypoint system): the UUID of the waypoint
    // This is decided by the Java server. When Java sends us a waypoint with a String ID, we turn it into a UUID
    private final UUID uuid;
    private final LocatorBarWaypoint bedrockWaypoint;
    private final CustomWaypointStyle style;
    private final Identifier styleIdentifier;
    private final boolean uses26_10WaypointPacket;
    private final boolean uses26_20WaypointPacket;
    private boolean sendListPackets;

    private Vector3f lastSentPosition = null;

    public GeyserWaypoint(GeyserSession session, UUID uuid, Identifier style, Color color, Optional<Entity> entity) {
        this.session = session;
        this.uuid = uuid;
        this.style = Registries.WAYPOINT_STYLES.getOrDefault(style, VanillaWaypoint.VANILLA_DEFAULT);
        this.styleIdentifier = style;
        this.bedrockWaypoint = new LocatorBarWaypoint();
        this.uses26_10WaypointPacket = uses26_10WaypointPacket(session);
        this.uses26_20WaypointPacket = uses26_20WaypointPacket(session);
        bedrockWaypoint.setVisible(true);
        bedrockWaypoint.setColor(color);
        initialiseWaypointFromEntity(entity);
        setPosition(Vector3f.ZERO);
    }

    private void initialiseWaypointFromEntity(Optional<Entity> entity) {
        bedrockWaypoint.setClientPositionAuthority(entity.isPresent());
        bedrockWaypoint.setEntityUniqueId(entity.map(Entity::geyserId).orElseGet(() -> uses26_10WaypointPacket ? null : session.getEntityCache().nextEntityId()));

        if (uses26_10WaypointPacket) {
            this.sendListPackets = false;
        } else {
            this.sendListPackets = entity.isEmpty();
        }
    }

    public Color color() {
        return bedrockWaypoint.getColor();
    }

    public void track(WaypointData data) {
        setData(data);
        sendTrackPackets(true);
        if (!uses26_10WaypointPacket) {
            sendLocationPacket(false);
        }
    }

    private void track() {
        sendTrackPackets(true);
        if (!uses26_10WaypointPacket) {
            sendLocationPacket(false);
        }
    }

    public void update(WaypointData data) {
        setData(data);
        sendLocationPacket(false);
    }

    public void untrack() {
        if (!uses26_10WaypointPacket) {
            PlayerLocationPacket packet = new PlayerLocationPacket();
            packet.setType(PlayerLocationPacket.Type.HIDE);
            packet.setTargetEntityId(bedrockWaypoint.getEntityUniqueId());
            session.sendUpstreamPacket(packet);
        }
        sendTrackPackets(false);
        lastSentPosition = null;
    }

    public void setEntity(Entity entity) {
        if (uses26_10WaypointPacket) {
            untrack();
            initialiseWaypointFromEntity(Optional.ofNullable(entity));
            track();
        } else {
            if (!(entity instanceof PlayerEntity)) {
                // 26.0 and below does not support non-player entities as waypoint target,
                // as such, this method should never be called with non-player entities
                GeyserImpl.getInstance().getLogger().warning("GeyserWaypoint#setEntity called for non-player entity!");
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
        if (uses26_10WaypointPacket) {
            if (uses26_20WaypointPacket) {
                float distance = session.playerEntity().position().distance(position);
                String texture = style.getTexturePath(session, styleIdentifier, distance);
                Vector2f iconSize = style.getTextureSize(session, styleIdentifier, distance);
                if (iconSize.lengthSquared() < 0.0F) {
                    iconSize = Vector2f.ZERO;
                }
                bedrockWaypoint.setTexturePath("textures/" + texture);
                bedrockWaypoint.setIconSize(iconSize);
            } else {
                float distanceSquared = session.playerEntity().position().distance(position);
                bedrockWaypoint.setTextureId(getLegacyWaypointTexture(distanceSquared));
            }
        }
    }

    protected void sendLocationPacket(boolean force) {
        Vector3f position = bedrockWaypoint.getWorldPosition().getPosition();
        if (force || lastSentPosition == null || position.distanceSquared(lastSentPosition) > 1.0F) {
            if (uses26_10WaypointPacket) {
                LocatorBarPacket packet = new LocatorBarPacket();
                bedrockWaypoint.setUpdateFlag(WaypointUpdateFlags.WORLD_POS | (uses26_20WaypointPacket ? WaypointUpdateFlags.TEXTURE_PATH | WaypointUpdateFlags.ICON_SIZE : WaypointUpdateFlags.TEXTURE_ID));
                packet.setWaypoints(List.of(new LocatorBarPacket.Payload(LocatorBarPacket.Action.UPDATE, uuid, bedrockWaypoint)));
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
        if (uses26_10WaypointPacket) {
            LocatorBarPacket packet = new LocatorBarPacket();
            bedrockWaypoint.setUpdateFlag(add ? WaypointUpdateFlags.ALL : 0);
            packet.setWaypoints(List.of(new LocatorBarPacket.Payload(add ? LocatorBarPacket.Action.ADD : LocatorBarPacket.Action.REMOVE, uuid, bedrockWaypoint)));
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
        Identifier style = MinecraftKey.keyToIdentifier(waypoint.icon().style());
        Color color = getWaypointColor(waypoint);
        return switch (waypoint.type()) {
            case EMPTY -> null;
            case VEC3I -> new CoordinatesWaypoint(session, uuid, style, color, entity);
            case CHUNK -> new ChunkWaypoint(session, uuid, style, color, entity);
            case AZIMUTH -> new AzimuthWaypoint(session, uuid, style, color, entity);
        };
    }

    public static boolean uses26_10WaypointPacket(GeyserSession session) {
        return GameProtocol.is1_26_10orHigher(session.protocolVersion());
    }

    public static boolean uses26_20WaypointPacket(GeyserSession session) {
        return GameProtocol.is1_26_20orHigher(session.protocolVersion());
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

    private static int getLegacyWaypointTexture(float distanceSquared) {
        if (distanceSquared < VANILLA_NEAR_DISTANCE_SQUARED) {
            return 2;
        } else if (distanceSquared >= VANILLA_FAR_DISTANCE_SQUARED) {
            return 5;
        }
        return (int) (3 + Math.floor((distanceSquared - VANILLA_NEAR_DISTANCE_SQUARED) / (VANILLA_FAR_DISTANCE_SQUARED - VANILLA_NEAR_DISTANCE_SQUARED) * 2));
    }
}
