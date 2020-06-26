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

package org.geysermc.connector.entity.living.animal.horse;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.living.animal.AnimalEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbstractHorseEntity extends AnimalEntity {

    // For updating the horse visual easier
    private float health = 20f;

    public AbstractHorseEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {

        if (entityMetadata.getId() == 8) {
            health = (float) entityMetadata.getValue();
            updateBedrockAttributes(session);
        }

        if (entityMetadata.getId() == 16) {
            byte xd = (byte) entityMetadata.getValue();
            metadata.getFlags().setFlag(EntityFlag.TAMED, (xd & 0x02) == 0x02);
            metadata.getFlags().setFlag(EntityFlag.SADDLED, (xd & 0x04) == 0x04);
            metadata.getFlags().setFlag(EntityFlag.EATING, (xd & 0x10) == 0x10);
            metadata.getFlags().setFlag(EntityFlag.STANDING, (xd & 0x20) == 0x20);
        }

        // Needed to control horses
        metadata.getFlags().setFlag(EntityFlag.CAN_POWER_JUMP, true);
        metadata.getFlags().setFlag(EntityFlag.WASD_CONTROLLED, true);

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void updateBedrockAttributes(GeyserSession session) {
        if (!valid) return;

        float maxHealth = attributes.containsKey(AttributeType.MAX_HEALTH) ? attributes.get(AttributeType.MAX_HEALTH).getValue() : 20f;

        List<AttributeData> attributesLocal = new ArrayList<>();
        for (Map.Entry<AttributeType, org.geysermc.connector.entity.attribute.Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;

            attributesLocal.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }
        attributesLocal.add(new AttributeData("minecraft:health", 0.0f, maxHealth, health, maxHealth));

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributesLocal);
        session.sendUpstreamPacket(updateAttributesPacket);
    }
}
