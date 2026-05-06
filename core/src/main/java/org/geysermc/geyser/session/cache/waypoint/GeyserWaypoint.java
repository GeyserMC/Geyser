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

import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LocatorBarWaypoint;
import org.cloudburstmc.protocol.bedrock.packet.LocatorBarPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerLocationPacket;
import org.geysermc.geyser.GeyserImpl;
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
import java.util.Optional;
import java.util.UUID;

public abstract class GeyserWaypoint {
    // These 2 from: https://mcsrc.dev/1/26.1.2/net/minecraft/client/resources/WaypointStyle
    // (DEFAULT_NEAR_DISTANCE and DEFAULT_FAR_DISTANCE squared)
    private static final float VANILLA_NEAR_DISTANCE_SQUARED = 16384.0F;
    private static final float VANILLA_FAR_DISTANCE_SQUARED = 110224.0F;
    private static final Key VANILLA_WAYPOINT_STYLE = MinecraftKey.key("default");

    // These are hardcoded on Java, extracted from BDS
    private static final Vector2f MEDIUM_WAYPOINT_ICON_SIZE = Vector2f.from(0.6F, 0.6F);
    private static final Vector2f SMALL_WAYPOINT_ICON_SIZE = Vector2f.from(0.2F, 0.2F);

    protected final GeyserSession session;

    // On 26.10 and above (new waypoint system): the waypoint group UUID
    // On 26.0 and below (old waypoint system): the UUID of the waypoint
    // This is decided by the Java server. When Java sends us a waypoint with a String ID, we turn it into a UUID
    private final UUID uuid;
    private final LocatorBarWaypoint bedrockWaypoint;
    private final Key style;
    private final boolean usesNewWaypointPacket;
    private final boolean usesNewNewWaypointPacket;
    private boolean sendListPackets;

    private Vector3f lastSentPosition = null;

    public GeyserWaypoint(GeyserSession session, UUID uuid, Key style, Color color, Optional<Entity> entity) {
        this.session = session;
        this.uuid = uuid;
        this.style = style;
        this.bedrockWaypoint = new LocatorBarWaypoint();
        this.usesNewWaypointPacket = usesNewWaypointPacket(session);
        this.usesNewNewWaypointPacket = usesNewNewWaypointPacket(session);
        bedrockWaypoint.setVisible(true);
        bedrockWaypoint.setColor(color);
        initialiseWaypointFromEntity(entity);
        setPosition(Vector3f.ZERO);
    }

    private void initialiseWaypointFromEntity(Optional<Entity> entity) {
        bedrockWaypoint.setClientPositionAuthority(entity.isPresent());
        bedrockWaypoint.setEntityUniqueId(entity.map(Entity::geyserId).orElseGet(() -> usesNewWaypointPacket ? null : session.getEntityCache().nextEntityId()));

        if (usesNewWaypointPacket) {
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
        if (!usesNewWaypointPacket) {
            sendLocationPacket(false);
        }
    }

    private void track() {
        sendTrackPackets(true);
        if (!usesNewWaypointPacket) {
            sendLocationPacket(false);
        }
    }

    public void update(WaypointData data) {
        setData(data);
        sendLocationPacket(false);
    }

    public void untrack() {
        if (!usesNewWaypointPacket) {
            PlayerLocationPacket packet = new PlayerLocationPacket();
            packet.setType(PlayerLocationPacket.Type.HIDE);
            packet.setTargetEntityId(bedrockWaypoint.getEntityUniqueId());
            session.sendUpstreamPacket(packet);
        }
        sendTrackPackets(false);
        lastSentPosition = null;
    }

    public void setEntity(Entity entity) {
        if (usesNewWaypointPacket) {
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
        float distanceSquared = session.playerEntity().position().distanceSquared(position);
        if (usesNewWaypointPacket) {
            if (usesNewNewWaypointPacket) {
                Pair<String, Vector2f> texture = getWaypointTexture(style, distanceSquared);
                bedrockWaypoint.setTexturePath(texture.first());
                bedrockWaypoint.setIconSize(texture.second());
            } else {
                bedrockWaypoint.setTextureId(getLegacyWaypointTexture(distanceSquared));
            }
        }
    }

    protected void sendLocationPacket(boolean force) {
        Vector3f position = bedrockWaypoint.getWorldPosition().getPosition();
        if (force || lastSentPosition == null || position.distanceSquared(lastSentPosition) > 1.0F) {
            if (usesNewWaypointPacket) {
                LocatorBarPacket packet = new LocatorBarPacket();
                bedrockWaypoint.setUpdateFlag(WaypointUpdateFlags.WORLD_POS | (usesNewNewWaypointPacket ? WaypointUpdateFlags.TEXTURE_PATH | WaypointUpdateFlags.ICON_SIZE : WaypointUpdateFlags.TEXTURE_ID));
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
        if (usesNewWaypointPacket) {
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
        Key style = waypoint.icon().style();
        Color color = getWaypointColor(waypoint);
        return switch (waypoint.type()) {
            case EMPTY -> null;
            case VEC3I -> new CoordinatesWaypoint(session, uuid, style, color, entity);
            case CHUNK -> new ChunkWaypoint(session, uuid, style, color, entity);
            case AZIMUTH -> new AzimuthWaypoint(session, uuid, style, color, entity);
        };
    }

    public static boolean usesNewWaypointPacket(GeyserSession session) {
        return GameProtocol.is1_26_10orHigher(session.protocolVersion());
    }

    public static boolean usesNewNewWaypointPacket(GeyserSession session) {
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

    private static Pair<String, Vector2f> getWaypointTexture(Key style, float distanceSquared) {
        // Small size is only used for the smallest icon, medium only for the second-smallest
        if (distanceSquared < VANILLA_NEAR_DISTANCE_SQUARED) {
            return Pair.of(getWaypointTexture(style, 0), Vector2f.ONE);
        } else if (distanceSquared >= VANILLA_FAR_DISTANCE_SQUARED) {
            return Pair.of(getWaypointTexture(style, 3), SMALL_WAYPOINT_ICON_SIZE);
        }
        int index = (int) (1 + Math.floor((distanceSquared - VANILLA_NEAR_DISTANCE_SQUARED) / (VANILLA_FAR_DISTANCE_SQUARED - VANILLA_NEAR_DISTANCE_SQUARED) * 2));
        return Pair.of(getWaypointTexture(style, index), index > 1  ? MEDIUM_WAYPOINT_ICON_SIZE : Vector2f.ONE);
    }

    private static String getWaypointTexture(Key style, int index) {
        if (style.equals(VANILLA_WAYPOINT_STYLE)) {
            return switch (index) {
                case 0 -> "textures/ui/locator_bar_dot_0";
                case 1 -> "textures/ui/locator_bar_dot_1";
                case 2 -> "textures/ui/locator_bar_dot_2";
                default -> "textures/ui/locator_bar_dot_3";
            };
        }
        return "textures/ui/" + style.namespace() + "/locator_bar_dot/" + style.value() + "_" + index;
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
