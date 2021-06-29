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

package org.geysermc.connector.entity.living;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IronGolemEntity extends GolemEntity {

    public IronGolemEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        // Indicate that we should show cracks through a resource pack
        metadata.getFlags().setFlag(EntityFlag.BRIBED, true);
        // Required, or else the overlay is black
        metadata.put(EntityData.COLOR_2, (byte) 0);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);
        if (entityMetadata.getId() == 9) {
            // Required so the resource pack sees the entity health
            attributes.put(AttributeType.HEALTH, AttributeType.HEALTH.getAttribute(metadata.getFloat(EntityData.HEALTH), 100f));
            updateBedrockAttributes(session);
        }
    }

    @Override
    public void updateBedrockAttributes(GeyserSession session) {
        if (!valid) return;

        List<AttributeData> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, org.geysermc.connector.entity.attribute.Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;

            attributes.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }
}
