/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityDataDictionary;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.data.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.AttributeUtils;

import java.util.*;

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

    protected int scale = 1;
    protected boolean movePending;

    protected EntityType entityType;

    protected boolean valid;

    protected Set<Long> passengers = new HashSet<>();
    protected Map<AttributeType, Attribute> attributes = new HashMap<>();
    protected EntityDataDictionary metadata = new EntityDataDictionary();

    public Entity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        this.entityId = entityId;
        this.geyserId = geyserId;
        this.entityType = entityType;
        this.position = position;
        this.motion = motion;
        this.rotation = rotation;

        this.valid = false;
        this.movePending = false;
        this.dimension = 0;

        metadata.put(EntityData.SCALE, 1f);
        metadata.put(EntityData.MAX_AIR, (short) 400);
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

    public Entity(long entityId, long geyserId, Vector3f position) {
    }

    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier("minecraft:" + entityType.name().toLowerCase());
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        valid = true;
        session.getUpstream().sendPacket(addEntityPacket);

        GeyserLogger.DEFAULT.debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }

    /**
     * @return can be deleted
     */
    public boolean despawnEntity(GeyserSession session) {
        if (!valid) return true;

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(geyserId);
        session.getUpstream().sendPacket(removeEntityPacket);

        valid = false;
        return true;
    }

    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch) {
        moveRelative(relX, relY, relZ, Vector3f.from(yaw, pitch, yaw));
    }

    public void moveRelative(double relX, double relY, double relZ, Vector3f rotation) {
        setRotation(rotation);
        this.position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);
        this.movePending = true;
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch) {
        moveAbsolute(position, Vector3f.from(yaw, pitch, yaw));
    }

    public void moveAbsolute(Vector3f position, Vector3f rotation) {
        setPosition(position);
        setRotation(rotation);

        this.movePending = true;
    }

    public void updateBedrockAttributes(GeyserSession session) {
        List<com.nukkitx.protocol.bedrock.data.Attribute> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;

            attributes.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributes);
        session.getUpstream().sendPacket(updateAttributesPacket);

        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(geyserId);
        entityDataPacket.getMetadata().putAll(metadata);
        session.getUpstream().sendPacket(entityDataPacket);
    }

    // To be used at a later date
    public void updateJavaAttributes(GeyserSession session) {
        List<com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;

            attributes.add(AttributeUtils.getJavaAttribute(entry.getValue()));
        }

        ServerEntityPropertiesPacket entityPropertiesPacket = new ServerEntityPropertiesPacket((int) entityId, attributes);
        session.getDownstream().getSession().send(entityPropertiesPacket);
    }

    public void setPosition(Vector3f position) {
        if (is(PlayerEntity.class)) {
            this.position = position.add(0, entityType.getOffset(), 0);
            return;
        }
        this.position = position;
    }

    /**
     * x = Pitch, y = HeadYaw, z = Yaw
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
