/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.message.TranslationMessage;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.data.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.*;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.living.ArmorStandEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.connector.utils.ChunkUtils;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Entity {
    protected long entityId;
    protected long geyserId;

    protected int dimension;

    protected Vector3f position;
    protected Vector3f motion;

    /**
     * x = Yaw, y = Pitch, z = HeadYaw
     */
    protected Vector3f rotation;

    protected float scale = 1;

    protected EntityType entityType;

    protected boolean valid;

    protected LongOpenHashSet passengers = new LongOpenHashSet();
    protected Map<AttributeType, Attribute> attributes = new HashMap<>();
    protected EntityDataMap metadata = new EntityDataMap();

    public Entity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        this.entityId = entityId;
        this.geyserId = geyserId;
        this.entityType = entityType;
        this.motion = motion;
        this.rotation = rotation;

        this.valid = false;
        this.dimension = 0;

        setPosition(position);

        metadata.put(EntityData.SCALE, 1f);
        metadata.put(EntityData.COLOR, 0);
        metadata.put(EntityData.MAX_AIR, (short) 300);
        metadata.put(EntityData.AIR, (short) 0);
        metadata.put(EntityData.LEAD_HOLDER_EID, -1L);
        metadata.put(EntityData.BOUNDING_BOX_HEIGHT, entityType.getHeight());
        metadata.put(EntityData.BOUNDING_BOX_WIDTH, entityType.getWidth());
        EntityFlags flags = new EntityFlags();
        flags.setFlag(EntityFlag.HAS_GRAVITY, true);
        flags.setFlag(EntityFlag.HAS_COLLISION, true);
        flags.setFlag(EntityFlag.CAN_SHOW_NAME, true);
        flags.setFlag(EntityFlag.CAN_CLIMB, true);
        metadata.putFlags(flags);
    }

    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier(entityType.getIdentifier());
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        valid = true;
        session.sendUpstreamPacket(addEntityPacket);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }

    /**
     * Despawns the entity
     *
     * @param session The GeyserSession
     * @return can be deleted
     */
    public boolean despawnEntity(GeyserSession session) {
        if (!valid) return true;

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(geyserId);
        session.sendUpstreamPacket(removeEntityPacket);

        valid = false;
        return true;
    }

    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, relX, relY, relZ, Vector3f.from(yaw, pitch, yaw), isOnGround);
    }

    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        setRotation(rotation);
        this.position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(false);

        session.sendUpstreamPacket(moveEntityPacket);
    }

    public void moveAbsolute(GeyserSession session, Vector3f position, float yaw, float pitch, boolean isOnGround, boolean teleported) {
        moveAbsolute(session, position, Vector3f.from(yaw, pitch, yaw), isOnGround, teleported);
    }

    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        setPosition(position);
        setRotation(rotation);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(teleported);

        session.sendUpstreamPacket(moveEntityPacket);
    }

    public void updateBedrockAttributes(GeyserSession session) {
        if (!valid) return;

        List<com.nukkitx.protocol.bedrock.data.Attribute> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;

            attributes.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }

    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        switch (entityMetadata.getId()) {
            case 0:
                if (entityMetadata.getType() == MetadataType.BYTE) {
                    byte xd = (byte) entityMetadata.getValue();
                    metadata.getFlags().setFlag(EntityFlag.ON_FIRE, (xd & 0x01) == 0x01);
                    metadata.getFlags().setFlag(EntityFlag.SNEAKING, (xd & 0x02) == 0x02);
                    metadata.getFlags().setFlag(EntityFlag.SPRINTING, (xd & 0x08) == 0x08);
                    metadata.getFlags().setFlag(EntityFlag.SWIMMING, ((xd & 0x10) == 0x10) && metadata.getFlags().getFlag(EntityFlag.SPRINTING)); // Otherwise swimming is enabled on older servers
                    metadata.getFlags().setFlag(EntityFlag.GLIDING, (xd & 0x80) == 0x80);

                    if ((xd & 0x20) == 0x20) {
                        // Armour stands are handled in their own class
                        if (!this.is(ArmorStandEntity.class)) {
                            metadata.getFlags().setFlag(EntityFlag.INVISIBLE, true);
                        }
                    } else {
                        metadata.getFlags().setFlag(EntityFlag.INVISIBLE, false);
                    }

                    // Shield code
                    if (session.getPlayerEntity().getEntityId() == entityId && metadata.getFlags().getFlag(EntityFlag.SNEAKING)) {
                        if ((session.getInventory().getItemInHand() != null && session.getInventory().getItemInHand().getId() == ItemRegistry.SHIELD) ||
                                (session.getInventoryCache().getPlayerInventory().getItem(45) != null && session.getInventoryCache().getPlayerInventory().getItem(45).getId() == ItemRegistry.SHIELD)) {
                            ClientPlayerUseItemPacket useItemPacket;
                            metadata.getFlags().setFlag(EntityFlag.BLOCKING, true);
                            if (session.getInventory().getItemInHand() != null && session.getInventory().getItemInHand().getId() == ItemRegistry.SHIELD) {
                                useItemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                            }
                            // Else we just assume it's the offhand, to simplify logic and to assure the packet gets sent
                            else {
                                useItemPacket = new ClientPlayerUseItemPacket(Hand.OFF_HAND);
                            }
                            session.sendDownstreamPacket(useItemPacket);
                        }
                    } else if (session.getPlayerEntity().getEntityId() == entityId && !metadata.getFlags().getFlag(EntityFlag.SNEAKING) && metadata.getFlags().getFlag(EntityFlag.BLOCKING)) {
                        metadata.getFlags().setFlag(EntityFlag.BLOCKING, false);
                        metadata.getFlags().setFlag(EntityFlag.DISABLE_BLOCKING, true);
                        ClientPlayerActionPacket releaseItemPacket = new ClientPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, new Position(0, 0, 0), BlockFace.DOWN);
                        session.sendDownstreamPacket(releaseItemPacket);
                    }
                }
                break;
            case 1: // Air/bubbles
                if ((int) entityMetadata.getValue() == 300) {
                    metadata.put(EntityData.AIR, (short) 0); // Otherwise the bubble counter remains in the UI
                } else {
                    metadata.put(EntityData.AIR, (short) (int) entityMetadata.getValue());
                }
                break;
            case 2: // custom name
                if (entityMetadata.getValue() instanceof TextMessage) {
                    TextMessage name = (TextMessage) entityMetadata.getValue();
                    if (name != null)
                        metadata.put(EntityData.NAMETAG, MessageUtils.getBedrockMessage(name));
                } else if (entityMetadata.getValue() instanceof TranslationMessage) {
                    TranslationMessage message = (TranslationMessage) entityMetadata.getValue();
                    if (message != null)
                        metadata.put(EntityData.NAMETAG, MessageUtils.getTranslatedBedrockMessage(message, session.getClientData().getLanguageCode(), true));
                }
                break;
            case 3: // is custom name visible
                if (!this.is(PlayerEntity.class))
                    metadata.put(EntityData.ALWAYS_SHOW_NAMETAG, (byte) ((boolean) entityMetadata.getValue() ? 1 : 0));
                break;
            case 4: // silent
                metadata.getFlags().setFlag(EntityFlag.SILENT, (boolean) entityMetadata.getValue());
                break;
            case 5: // no gravity
                metadata.getFlags().setFlag(EntityFlag.HAS_GRAVITY, !(boolean) entityMetadata.getValue());
                break;
            case 6: // Pose change
                if (entityMetadata.getValue().equals(Pose.SLEEPING)) {
                    metadata.getFlags().setFlag(EntityFlag.SLEEPING, true);
                    // Has to be a byte or it does not work
                    metadata.put(EntityData.CAN_START_SLEEP, (byte) 2);
                    if (entityId == session.getPlayerEntity().getEntityId()) {
                        Vector3i lastInteractionPos = session.getLastInteractionPosition();
                        metadata.put(EntityData.BED_RESPAWN_POS, lastInteractionPos);
                        if (session.getConnector().getConfig().isCacheChunks()) {
                            BlockState bed = session.getConnector().getWorldManager().getBlockAt(session, lastInteractionPos.getX(),
                                    lastInteractionPos.getY(), lastInteractionPos.getZ());
                            // Bed has to be updated, or else player is floating in the air
                            ChunkUtils.updateBlock(session, bed, lastInteractionPos);
                        }
                    } else {
                        metadata.put(EntityData.BED_RESPAWN_POS, Vector3i.from(position.getFloorX(), position.getFloorY() - 2, position.getFloorZ()));
                    }
                    metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.2f);
                    metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.2f);
                } else if (metadata.getFlags().getFlag(EntityFlag.SLEEPING)) {
                    metadata.getFlags().setFlag(EntityFlag.SLEEPING, false);
                    metadata.put(EntityData.BOUNDING_BOX_WIDTH, getEntityType().getWidth());
                    metadata.put(EntityData.BOUNDING_BOX_HEIGHT, getEntityType().getHeight());
                    metadata.put(EntityData.CAN_START_SLEEP, (byte) 0);
                }
                break;
            case 7: // blocking
                if (entityMetadata.getType() == MetadataType.BYTE) {
                    byte xd = (byte) entityMetadata.getValue();
                    metadata.getFlags().setFlag(EntityFlag.BLOCKING, (xd & 0x01) == 0x01);
                }
                break;
        }

        updateBedrockMetadata(session);
    }

    public void updateBedrockMetadata(GeyserSession session) {
        if (!valid) return;

        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(geyserId);
        entityDataPacket.getMetadata().putAll(metadata);
        session.sendUpstreamPacket(entityDataPacket);
    }

    /**
     * x = Pitch, y = HeadYaw, z = Yaw
     *
     * @return the bedrock rotation
     */
    public Vector3f getBedrockRotation() {
        return Vector3f.from(rotation.getY(), rotation.getZ(), rotation.getX());
    }

    @SuppressWarnings("unchecked")
    public <I extends Entity> I as(Class<I> entityClass) {
        return entityClass.isInstance(this) ? (I) this : null;
    }

    public <I extends Entity> boolean is(Class<I> entityClass) {
        return entityClass.isInstance(this);
    }
}
