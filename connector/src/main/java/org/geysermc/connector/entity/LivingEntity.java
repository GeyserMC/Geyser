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
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class LivingEntity extends Entity {

    protected ItemData helmet = ItemData.AIR;
    protected ItemData chestplate = ItemData.AIR;
    protected ItemData leggings = ItemData.AIR;
    protected ItemData boots = ItemData.AIR;
    protected ItemData hand = ItemData.AIR;
    protected ItemData offHand = ItemData.AIR;

    public LivingEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        switch (entityMetadata.getId()) {
            case 8:
                metadata.put(EntityData.HEALTH, entityMetadata.getValue());
                break;
            case 9:
                metadata.put(EntityData.POTION_AUX_VALUE, entityMetadata.getValue()); //TODO: CHECK THIS AND THE BOTTOM ONE
                break;
            case 10:
                metadata.put(EntityData.EFFECT_AMBIENT, (byte) ((boolean) entityMetadata.getValue() ? 1 : 0));
                break;
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    public void updateEquipment(GeyserSession session) {
        if (!valid)
            return;

        MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
        armorEquipmentPacket.setRuntimeEntityId(geyserId);
        armorEquipmentPacket.setHelmet(helmet);
        armorEquipmentPacket.setChestplate(chestplate);
        armorEquipmentPacket.setLeggings(leggings);
        armorEquipmentPacket.setBoots(boots);

        MobEquipmentPacket handPacket = new MobEquipmentPacket();
        handPacket.setRuntimeEntityId(geyserId);
        handPacket.setItem(hand);
        handPacket.setHotbarSlot(-1);
        handPacket.setInventorySlot(0);
        handPacket.setContainerId(ContainerId.INVENTORY);

        MobEquipmentPacket offHandPacket = new MobEquipmentPacket();
        offHandPacket.setRuntimeEntityId(geyserId);
        offHandPacket.setItem(offHand);
        offHandPacket.setHotbarSlot(-1);
        offHandPacket.setInventorySlot(0);
        offHandPacket.setContainerId(ContainerId.OFFHAND);

        session.sendUpstreamPacket(armorEquipmentPacket);
        session.sendUpstreamPacket(handPacket);
        session.sendUpstreamPacket(offHandPacket);
    }

    @Override
    public void updateBedrockAttributes(GeyserSession session) {
        if (!valid) return;

        float maxHealth = this.attributes.containsKey(AttributeType.MAX_HEALTH) ? this.attributes.get(AttributeType.MAX_HEALTH).getValue() : getDefaultMaxHealth();

        List<AttributeData> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, org.geysermc.connector.entity.attribute.Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;
            if (entry.getValue().getType() == AttributeType.HEALTH) {
                // Add health attribute to properly show hearts when mounting
                // TODO: Not a perfect system, since it led to respawn bugs
                attributes.add(new AttributeData("minecraft:health", 0.0f, maxHealth, metadata.getFloat(EntityData.HEALTH, 20f), maxHealth));
                continue;
            }

            attributes.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }

    /**
     * Used for the health visual when mounting an entity.
     * @return the default maximum health for the entity.
     */
    protected float getDefaultMaxHealth() {
        return 20f;
    }
}
