/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.*;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.living.ArmorStandEntity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Entity {
    protected long entityId;
    protected long geyserId;

    protected Vector3f position;
    protected Vector3f motion;

    /**
     * x = Yaw, y = Pitch, z = HeadYaw
     */
    protected Vector3f rotation;

    /**
     * Saves if the entity should be on the ground. Otherwise entities like parrots are flapping when rotating
     */
    protected boolean onGround;

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

        setPosition(position);

        metadata.put(EntityData.SCALE, 1f);
        metadata.put(EntityData.COLOR, 0);
        metadata.put(EntityData.MAX_AIR_SUPPLY, (short) 300);
        metadata.put(EntityData.AIR_SUPPLY, (short) 0);
        metadata.put(EntityData.LEASH_HOLDER_EID, -1L);
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

        for (long passenger : passengers) { // Make sure all passengers on the despawned entity are updated
            Entity entity = session.getEntityCache().getEntityByJavaId(passenger);
            if (entity == null) continue;
            entity.getMetadata().getOrCreateFlags().setFlag(EntityFlag.RIDING, false);
            entity.updateBedrockMetadata(session);
        }

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(geyserId);
        session.sendUpstreamPacket(removeEntityPacket);

        valid = false;
        return true;
    }

    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, relX, relY, relZ, Vector3f.from(yaw, pitch, this.rotation.getZ()), isOnGround);
    }

    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        setRotation(rotation);
        setOnGround(isOnGround);
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
        moveAbsolute(session, position, Vector3f.from(yaw, pitch, this.rotation.getZ()), isOnGround, teleported);
    }

    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        setPosition(position);
        setRotation(rotation);
        setOnGround(isOnGround);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(teleported);

        session.sendUpstreamPacket(moveEntityPacket);
    }

    /**
     * Teleports an entity to a new location. Used in JavaEntityTeleportTranslator.
     * @param session GeyserSession.
     * @param position The new position of the entity.
     * @param yaw The new yaw of the entity.
     * @param pitch The new pitch of the entity.
     * @param isOnGround Whether the entity is currently on the ground.
     */
    public void teleport(GeyserSession session, Vector3f position, float yaw, float pitch, boolean isOnGround) {
        moveAbsolute(session, position, yaw, pitch, isOnGround, false);
    }

    /**
     * Updates an entity's head position. Used in JavaEntityHeadLookTranslator.
     * @param session GeyserSession.
     * @param headYaw The new head rotation of the entity.
     */
    public void updateHeadLookRotation(GeyserSession session, float headYaw) {
        moveRelative(session, 0, 0, 0, Vector3f.from(headYaw, rotation.getY(), rotation.getZ()), onGround);
    }

    /**
     * Updates an entity's position and rotation. Used in JavaEntityPositionRotationTranslator.
     * @param session GeyserSession
     * @param moveX The new X offset of the current position.
     * @param moveY The new Y offset of the current position.
     * @param moveZ The new Z offset of the current position.
     * @param yaw The new yaw of the entity.
     * @param pitch The new pitch of the entity.
     * @param isOnGround Whether the entity is currently on the ground.
     */
    public void updatePositionAndRotation(GeyserSession session, double moveX, double moveY, double moveZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, moveX, moveY, moveZ, Vector3f.from(rotation.getX(), pitch, yaw), isOnGround);
    }

    /**
     * Updates an entity's rotation. Used in JavaEntityRotationTranslator.
     * @param session GeyserSession.
     * @param yaw The new yaw of the entity.
     * @param pitch The new pitch of the entity.
     * @param isOnGround Whether the entity is currently on the ground.
     */
    public void updateRotation(GeyserSession session, float yaw, float pitch, boolean isOnGround) {
        updatePositionAndRotation(session, 0, 0, 0, yaw, pitch, isOnGround);
    }

    public void updateBedrockAttributes(GeyserSession session) {
        if (!valid) return;

        List<AttributeData> attributes = new ArrayList<>();
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

    /**
     * Applies the Java metadata to the local Bedrock metadata copy
     * @param entityMetadata the Java entity metadata
     * @param session GeyserSession
     */
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        switch (entityMetadata.getId()) {
            case 0:
                if (entityMetadata.getType() == MetadataType.BYTE) {
                    byte xd = (byte) entityMetadata.getValue();
                    metadata.getFlags().setFlag(EntityFlag.ON_FIRE, ((xd & 0x01) == 0x01) && !metadata.getFlags().getFlag(EntityFlag.FIRE_IMMUNE)); // Otherwise immune entities sometimes flicker onfire
                    metadata.getFlags().setFlag(EntityFlag.SNEAKING, (xd & 0x02) == 0x02);
                    metadata.getFlags().setFlag(EntityFlag.SPRINTING, (xd & 0x08) == 0x08);
                    metadata.getFlags().setFlag(EntityFlag.SWIMMING, ((xd & 0x10) == 0x10) && metadata.getFlags().getFlag(EntityFlag.SPRINTING)); // Otherwise swimming is enabled on older servers
                    metadata.getFlags().setFlag(EntityFlag.GLIDING, (xd & 0x80) == 0x80);

                    // Armour stands are handled in their own class
                    if (!this.is(ArmorStandEntity.class)) {
                        metadata.getFlags().setFlag(EntityFlag.INVISIBLE, (xd & 0x20) == 0x20);
                    }
                }
                break;
            case 1: // Air/bubbles
                if ((int) entityMetadata.getValue() == 300) {
                    metadata.put(EntityData.AIR_SUPPLY, (short) 0); // Otherwise the bubble counter remains in the UI
                } else {
                    metadata.put(EntityData.AIR_SUPPLY, (short) (int) entityMetadata.getValue());
                }
                break;
            case 2: // custom name
                if (entityMetadata.getValue() instanceof Component) {
                    Component message = (Component) entityMetadata.getValue();
                    if (message != null)
                        // Always translate even if it's a TextMessage since there could be translatable parameters
                        metadata.put(EntityData.NAMETAG, MessageTranslator.convertMessage(message, session.getLocale()));
                }
                break;
            case 3: // is custom name visible
                if (!this.is(PlayerEntity.class))
                    metadata.put(EntityData.NAMETAG_ALWAYS_SHOW, (byte) ((boolean) entityMetadata.getValue() ? 1 : 0));
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
                    metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.2f);
                    metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.2f);
                } else if (metadata.getFlags().getFlag(EntityFlag.SLEEPING)) {
                    metadata.getFlags().setFlag(EntityFlag.SLEEPING, false);
                    metadata.put(EntityData.BOUNDING_BOX_WIDTH, getEntityType().getWidth());
                    metadata.put(EntityData.BOUNDING_BOX_HEIGHT, getEntityType().getHeight());
                }
                break;
        }
    }

    /**
     * Sends the Bedrock metadata to the client
     * @param session GeyserSession
     */
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
