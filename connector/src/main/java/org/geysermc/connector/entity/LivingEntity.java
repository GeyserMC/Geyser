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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
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
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.connector.utils.ChunkUtils;

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
            case 7: // blocking
                byte xd = (byte) entityMetadata.getValue();

                //blocking gets triggered when using a bow, but if we set USING_ITEM for all items, it may look like
                //you're "mining" with ex. a shield.
                boolean isUsingShield = (getHand().getId() == ItemRegistry.SHIELD.getBedrockId() ||
                                         getHand().equals(ItemData.AIR) && getOffHand().getId() == ItemRegistry.SHIELD.getBedrockId());
                metadata.getFlags().setFlag(EntityFlag.USING_ITEM, (xd & 0x01) == 0x01 && !isUsingShield);
                metadata.getFlags().setFlag(EntityFlag.BLOCKING, (xd & 0x01) == 0x01);

                // Riptide spin attack
                metadata.getFlags().setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, (xd & 0x04) == 0x04);
                break;
            case 8:
                metadata.put(EntityData.HEALTH, entityMetadata.getValue());
                break;
            case 9:
                metadata.put(EntityData.EFFECT_COLOR, entityMetadata.getValue());
                break;
            case 10:
                metadata.put(EntityData.EFFECT_AMBIENT, (byte) ((boolean) entityMetadata.getValue() ? 1 : 0));
                break;
            case 13: // Bed Position
                Position bedPosition = (Position) entityMetadata.getValue();
                if (bedPosition != null) {
                    metadata.put(EntityData.BED_POSITION, Vector3i.from(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ()));
                    if (session.getConnector().getConfig().isCacheChunks()) {
                        int bed = session.getConnector().getWorldManager().getBlockAt(session, bedPosition);
                        // Bed has to be updated, or else player is floating in the air
                        ChunkUtils.updateBlock(session, bed, bedPosition);
                    }
                    // Indicate that the player should enter the sleep cycle
                    // Has to be a byte or it does not work
                    // (Bed position is what actually triggers sleep - "pose" is only optional)
                    metadata.put(EntityData.PLAYER_FLAGS, (byte) 2);
                } else {
                    // Player is no longer sleeping
                    metadata.put(EntityData.PLAYER_FLAGS, (byte) 0);
                }
                break;
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    public void updateAllEquipment(GeyserSession session) {
        if (!valid) return;

        updateArmor(session);
        updateMainHand(session);
        updateOffHand(session);
    }

    public void updateArmor(GeyserSession session) {
        if (!valid) return;

        ItemData helmet = this.helmet;
        ItemData chestplate = this.chestplate;
        // If an entity has a banner on them, it will be in the helmet slot in Java but the chestplate spot in Bedrock
        // But don't overwrite the chestplate if it isn't empty
        if (chestplate.getId() == ItemData.AIR.getId() && helmet.getId() == ItemRegistry.BANNER.getBedrockId()) {
            chestplate = this.helmet;
            helmet = ItemData.AIR;
        } else if (chestplate.getId() == ItemRegistry.BANNER.getBedrockId()) {
            // Prevent chestplate banners from showing erroneously
            chestplate = ItemData.AIR;
        }

        MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
        armorEquipmentPacket.setRuntimeEntityId(geyserId);
        armorEquipmentPacket.setHelmet(helmet);
        armorEquipmentPacket.setChestplate(chestplate);
        armorEquipmentPacket.setLeggings(leggings);
        armorEquipmentPacket.setBoots(boots);

        session.sendUpstreamPacket(armorEquipmentPacket);
    }

    public void updateMainHand(GeyserSession session) {
        if (!valid) return;

        MobEquipmentPacket handPacket = new MobEquipmentPacket();
        handPacket.setRuntimeEntityId(geyserId);
        handPacket.setItem(hand);
        handPacket.setHotbarSlot(-1);
        handPacket.setInventorySlot(0);
        handPacket.setContainerId(ContainerId.INVENTORY);

        session.sendUpstreamPacket(handPacket);
    }

    public void updateOffHand(GeyserSession session) {
        if (!valid) return;

        MobEquipmentPacket offHandPacket = new MobEquipmentPacket();
        offHandPacket.setRuntimeEntityId(geyserId);
        offHandPacket.setItem(offHand);
        offHandPacket.setHotbarSlot(-1);
        offHandPacket.setInventorySlot(0);
        offHandPacket.setContainerId(ContainerId.OFFHAND);

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
