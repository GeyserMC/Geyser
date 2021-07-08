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

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.attribute.GeyserAttributeType;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.connector.utils.ChunkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class LivingEntity extends Entity {

    protected ItemData helmet = ItemData.AIR;
    protected ItemData chestplate = ItemData.AIR;
    protected ItemData leggings = ItemData.AIR;
    protected ItemData boots = ItemData.AIR;
    protected ItemData hand = ItemData.AIR;
    protected ItemData offHand = ItemData.AIR;

    @Getter(value = AccessLevel.NONE)
    protected float health = 1f; // The default value in Java Edition before any entity metadata is sent
    @Getter(value = AccessLevel.NONE)
    protected float maxHealth = 20f; // The value Java Edition defaults to if no attribute is given

    /**
     * A convenience variable for if the entity has reached the maximum frozen ticks and should be shaking
     */
    private boolean isMaxFrozenState = false;

    public LivingEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        // Matches Bedrock behavior; is always set to this
        metadata.put(EntityData.HEALTH, 1);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        switch (entityMetadata.getId()) {
            case 8: // blocking
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
            case 9:
                this.health = (float) entityMetadata.getValue();

                AttributeData healthData = createHealthAttribute();
                UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(geyserId);
                attributesPacket.setAttributes(Collections.singletonList(healthData));
                session.sendUpstreamPacket(attributesPacket);
                break;
            case 10:
                metadata.put(EntityData.EFFECT_COLOR, entityMetadata.getValue());
                break;
            case 11:
                metadata.put(EntityData.EFFECT_AMBIENT, (byte) ((boolean) entityMetadata.getValue() ? 1 : 0));
                break;
            case 14: // Bed Position
                Position bedPosition = (Position) entityMetadata.getValue();
                if (bedPosition != null) {
                    metadata.put(EntityData.BED_POSITION, Vector3i.from(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ()));
                    int bed = session.getConnector().getWorldManager().getBlockAt(session, bedPosition);
                    // Bed has to be updated, or else player is floating in the air
                    ChunkUtils.updateBlock(session, bed, bedPosition);
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

    @Override
    protected boolean isShaking(GeyserSession session) {
        return isMaxFrozenState;
    }

    @Override
    protected void setDimensions(Pose pose) {
        if (pose == Pose.SLEEPING) {
            metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.2f);
            metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.2f);
        } else {
            super.setDimensions(pose);
        }
    }

    @Override
    protected void setFreezing(GeyserSession session, float amount) {
        super.setFreezing(session, amount);
        this.isMaxFrozenState = amount >= 1.0f;
        metadata.getFlags().setFlag(EntityFlag.SHAKING, isShaking(session));
    }

    /**
     * @return a Bedrock health attribute constructed from the data sent from the server
     */
    protected AttributeData createHealthAttribute() {
        // Default health needs to be specified as the max health in order for maximum hearts to show correctly on mounted entities
        return new AttributeData(GeyserAttributeType.HEALTH.getBedrockIdentifier(), 0f, this.maxHealth, this.health, this.maxHealth);
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

    /**
     * Attributes are properties of an entity that are generally more runtime-based instead of permanent properties.
     * Movement speed, current attack damage with a weapon, current knockback resistance.
     *
     * @param attributes the Java list of attributes sent from the server
     */
    public void updateBedrockAttributes(GeyserSession session, List<Attribute> attributes) {
        if (!valid) return;

        List<AttributeData> newAttributes = new ArrayList<>();

        for (Attribute attribute : attributes) {
            // Convert the attribute to a Bedrock version, if relevant
            updateAttribute(attribute, newAttributes);
        }

        if (newAttributes.isEmpty()) {
            // If there are Java-only attributes or only attributes that are not translated by us
            return;
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(newAttributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }

    /**
     * Takes the Java attribute and adds it to newAttributes as a Bedrock-formatted attribute
     */
    protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
        switch (javaAttribute.getType()) {
            case GENERIC_MAX_HEALTH:
                this.maxHealth = (float) AttributeUtils.calculateValue(javaAttribute);
                newAttributes.add(createHealthAttribute());
                break;
            case GENERIC_ATTACK_DAMAGE:
                newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.ATTACK_DAMAGE));
                break;
            case GENERIC_FLYING_SPEED:
                newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.FLYING_SPEED));
                break;
            case GENERIC_MOVEMENT_SPEED:
                newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.MOVEMENT_SPEED));
                break;
            case GENERIC_FOLLOW_RANGE:
                newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.FOLLOW_RANGE));
                break;
            case GENERIC_KNOCKBACK_RESISTANCE:
                newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.KNOCKBACK_RESISTANCE));
                break;
            case HORSE_JUMP_STRENGTH:
                newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.HORSE_JUMP_STRENGTH));
                break;
        }
    }

    /**
     * Calculates the complete attribute value to send to Bedrock. Will be overriden if attributes need to be cached.
     */
    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        return type.getAttribute((float) AttributeUtils.calculateValue(javaAttribute));
    }
}
