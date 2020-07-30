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

package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.connector.entity.living.InsentientEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class EnderDragonEntity extends InsentientEntity {

    public EnderDragonEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 15) {
            metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
            switch ((int) entityMetadata.getValue()) {
                // Performing breath attack
                case 5:
                    EntityEventPacket entityEventPacket = new EntityEventPacket();
                    entityEventPacket.setType(EntityEventType.DRAGON_FLAMING);
                    entityEventPacket.setRuntimeEntityId(geyserId);
                    entityEventPacket.setData(0);
                    session.sendUpstreamPacket(entityEventPacket);
                case 6:
                case 7:
                    metadata.getFlags().setFlag(EntityFlag.SITTING, true);
                    break;
            }
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
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

        // Otherwise dragon is always 'dying'
        addEntityPacket.getAttributes().add(new AttributeData("minecraft:health", 0.0f, 200f, 200f, 200f));

        valid = true;
        session.sendUpstreamPacket(addEntityPacket);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }
}
