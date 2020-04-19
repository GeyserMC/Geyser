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
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.packet.*;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import lombok.Getter;
import lombok.Setter;

import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.connector.utils.MessageUtils;

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

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
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

        session.getUpstream().sendPacket(moveEntityPacket);
    }

    public void moveAbsolute(GeyserSession session, Vector3f position, float yaw, float pitch, boolean isOnGround) {
        moveAbsolute(session, position, Vector3f.from(yaw, pitch, yaw), isOnGround);
    }

    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround) {
        setPosition(position);
        setRotation(rotation);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(false);

        session.getUpstream().sendPacket(moveEntityPacket);
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
        session.getUpstream().sendPacket(updateAttributesPacket);
    }

    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        switch (entityMetadata.getId()) {
            case 0:
                if (entityMetadata.getType() == MetadataType.BYTE) {
                    byte xd = (byte) entityMetadata.getValue();
                    metadata.getFlags().setFlag(EntityFlag.ON_FIRE, (xd & 0x01) == 0x01);
                    metadata.getFlags().setFlag(EntityFlag.SNEAKING, (xd & 0x02) == 0x02);
                    metadata.getFlags().setFlag(EntityFlag.SPRINTING, (xd & 0x08) == 0x08);
                    metadata.getFlags().setFlag(EntityFlag.SWIMMING, (xd & 0x10) == 0x10);
                    metadata.getFlags().setFlag(EntityFlag.GLIDING, (xd & 0x80) == 0x80);
                    // metadata.getFlags().setFlag(EntityFlag.INVISIBLE, (xd & 0x20) == 0x20);
                    if ((xd & 0x20) == 0x20)
                        metadata.put(EntityData.SCALE, 0.0f);
                    else
                        metadata.put(EntityData.SCALE, scale);
                }
                break;
            case 2: // custom name
                TextMessage name = (TextMessage) entityMetadata.getValue();
                if (name != null)
                    metadata.put(EntityData.NAMETAG, MessageUtils.getBedrockMessage(name));
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
        }

        updateBedrockMetadata(session);
    }

    public void updateBedrockMetadata(GeyserSession session) {
        if (!valid) return;

        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(geyserId);
        entityDataPacket.getMetadata().putAll(metadata);
        session.getUpstream().sendPacket(entityDataPacket);
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
