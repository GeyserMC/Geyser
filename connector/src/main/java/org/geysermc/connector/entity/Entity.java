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

import com.flowpowered.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.Attribute;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityDataDictionary;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Entity {

    protected long entityId;
    protected long geyserId;

    protected int dimension;

    protected Vector3f position;
    protected Vector3f motion;
    protected Vector3f rotation;

    protected int scale = 1;
    protected float yaw;
    protected float pitch;
    protected boolean shouldMove;

    protected EntityType entityType;

    protected boolean valid;

    protected Map<Integer, Attribute> attributes = new HashMap<Integer, Attribute>();

    public Entity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        this.entityId = entityId;
        this.geyserId = geyserId;
        this.entityType = entityType;
        this.position = position;
        this.motion = motion;
        this.rotation = rotation;

        this.valid = false;
        this.shouldMove = false;
    }

    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(rotation);
        addEntityPacket.setEntityType(entityType.getType());

        session.getUpstream().sendPacket(addEntityPacket);
        valid = true;
    }

    public void despawnEntity(GeyserSession session) {
        if (!valid)
            return;

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);
        session.getUpstream().sendPacket(removeEntityPacket);
    }

    public void moveRelative(double relX, double relY, double relZ, float pitch, float yaw) {
        if (relX == 0 && relY != 0 && relZ != 0 && yaw != 0 && pitch != 0)
            return;

        if (pitch != 0)
            this.pitch = pitch;

        if (yaw != 0)
            this.yaw = yaw;

        this.position = new Vector3f(position.getX() + relX, position.getX() + relY, position.getX() + relZ);
        this.shouldMove = true;
    }

    public void moveAbsolute(Vector3f position, float pitch, float yaw) {
        if (position.getX() == 0 && position.getX() != 0 && position.getX() != 0 && yaw != 0 && pitch != 0)
            return;

        this.position = position;
        this.pitch = pitch;
        this.yaw = yaw;
        this.shouldMove = true;
    }

    protected EntityDataDictionary getMetadata() {
        EntityDataDictionary dictionary = new EntityDataDictionary();
        dictionary.put(EntityData.NAMETAG, "");
        dictionary.put(EntityData.ENTITY_AGE, 0);
        dictionary.put(EntityData.SCALE, 1f);
        dictionary.put(EntityData.MAX_AIR, (short) 400);
        dictionary.put(EntityData.AIR, (short) 0);
        dictionary.put(EntityData.BOUNDING_BOX_HEIGHT, entityType.getHeight());
        dictionary.put(EntityData.BOUNDING_BOX_WIDTH, entityType.getWidth());
        return dictionary;
    }
}
