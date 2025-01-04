/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.AttributeUtils;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.EntityEffectParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class LivingEntity extends Entity {
    protected ItemData helmet = ItemData.AIR;
    protected ItemData chestplate = ItemData.AIR;
    protected ItemData leggings = ItemData.AIR;
    protected ItemData boots = ItemData.AIR;
    protected ItemData body = ItemData.AIR;
    protected ItemData hand = ItemData.AIR;
    protected ItemData offhand = ItemData.AIR;

    @Getter(value = AccessLevel.NONE)
    protected float health = 1f; // The default value in Java Edition before any entity metadata is sent
    @Getter(value = AccessLevel.NONE)
    protected float maxHealth = 20f; // The value Java Edition defaults to if no attribute is given

    /**
     * A convenience variable for if the entity has reached the maximum frozen ticks and should be shaking
     */
    private boolean isMaxFrozenState = false;

    /**
     * The base scale entity data, without attributes applied. Used for such cases as baby variants.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private float scale;
    /**
     * The scale sent through the Java attributes packet
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private float attributeScale;

    public LivingEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setHelmet(ItemStack stack) {
        this.helmet = ItemTranslator.translateToBedrock(session, stack);
    }

    public void setChestplate(ItemStack stack) {
        this.chestplate = ItemTranslator.translateToBedrock(session, stack);
    }

    public void setBody(ItemStack stack) {
        this.body = ItemTranslator.translateToBedrock(session, stack);
    }

    public void setLeggings(ItemStack stack) {
        this.leggings = ItemTranslator.translateToBedrock(session, stack);
    }

    public void setBoots(ItemStack stack) {
        this.boots = ItemTranslator.translateToBedrock(session, stack);
    }

    public void setHand(ItemStack stack) {
        this.hand = ItemTranslator.translateToBedrock(session, stack);
    }

    public void setOffhand(ItemStack stack) {
        this.offhand = ItemTranslator.translateToBedrock(session, stack);
    }

    public void switchHands() {
        ItemData offhand = this.offhand;
        this.offhand = this.hand;
        this.hand = offhand;
    }

    @Override
    protected void initializeMetadata() {
        // Initialize here so overriding classes don't have 0 values
        this.scale = 1f;
        this.attributeScale = 1f;
        super.initializeMetadata();
        // Matches Bedrock behavior; is always set to this
        dirtyMetadata.put(EntityDataTypes.STRUCTURAL_INTEGRITY, 1);
    }

    @Override
    public void updateNametag(@Nullable Team team) {
        // if name not visible, don't mark it as visible
        updateNametag(team, team == null || team.isVisibleFor(session.getPlayerEntity().getUsername()));
    }

    public void hideNametag() {
        setNametag("", false);
    }

    public void setLivingEntityFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();

        boolean isUsingItem = (xd & 0x01) == 0x01;
        boolean isUsingOffhand = (xd & 0x02) == 0x02;

        boolean isUsingShield = hasShield(isUsingOffhand);

        setFlag(EntityFlag.USING_ITEM, isUsingItem && !isUsingShield);
        // Override the blocking
        setFlag(EntityFlag.BLOCKING, isUsingItem && isUsingShield);

        // Riptide spin attack
        setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, (xd & 0x04) == 0x04);

        // OptionalPack usage
        setFlag(EntityFlag.EMERGING, isUsingItem && isUsingOffhand);
    }

    public void setHealth(FloatEntityMetadata entityMetadata) {
        this.health = entityMetadata.getPrimitiveValue();

        AttributeData healthData = createHealthAttribute();
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.setAttributes(Collections.singletonList(healthData));
        session.sendUpstreamPacket(attributesPacket);
    }

    // TODO: support all particle types
    public void setParticles(ObjectEntityMetadata<List<Particle>> entityMetadata) {
        List<Particle> particles = entityMetadata.getValue();
        float r = 0f;
        float g = 0f;
        float b = 0f;

        int count = 0;
        for (Particle particle : particles) {
            if (particle.getType() != ParticleType.ENTITY_EFFECT) {
                continue;
            }

            int color = ((EntityEffectParticleData) particle.getData()).getColor();
            r += ((color >> 16) & 0xFF) / 255f;
            g += ((color >> 8) & 0xFF) / 255f;
            b += ((color) & 0xFF) / 255f;
            count++;
        }

        int result = 0;
        if (count > 0) {
            r = r / count * 255f;
            g = g / count * 255f;
            b = b / count * 255f;
            result = (int) r << 16 | (int) g << 8 | (int) b;
        }

        dirtyMetadata.put(EntityDataTypes.EFFECT_COLOR, result);
    }

    public @Nullable Vector3i setBedPosition(EntityMetadata<Optional<Vector3i>, ?> entityMetadata) {
        Optional<Vector3i> optionalPos = entityMetadata.getValue();
        if (optionalPos.isPresent()) {
            Vector3i bedPosition = optionalPos.get();
            dirtyMetadata.put(EntityDataTypes.BED_POSITION, bedPosition);
            return bedPosition;
        } else {
            return null;
        }
    }

    protected boolean hasShield(boolean offhand) {
        ItemMapping shieldMapping = session.getItemMappings().getStoredItems().shield();
        if (offhand) {
            return this.offhand.getDefinition().equals(shieldMapping.getBedrockDefinition());
        } else {
            return hand.getDefinition().equals(shieldMapping.getBedrockDefinition());
        }
    }

    @Override
    protected boolean isShaking() {
        return isMaxFrozenState;
    }

    @Override
    protected void setDimensions(Pose pose) {
        if (pose == Pose.SLEEPING) {
            setBoundingBoxWidth(0.2f);
            setBoundingBoxHeight(0.2f);
        } else {
            super.setDimensions(pose);
        }
    }

    @Override
    public float setFreezing(IntEntityMetadata entityMetadata) {
        float freezingPercentage = super.setFreezing(entityMetadata);
        this.isMaxFrozenState = freezingPercentage >= 1.0f;
        setFlag(EntityFlag.SHAKING, isShaking());
        return freezingPercentage;
    }

    protected void setScale(float scale) {
        this.scale = scale;
        applyScale();
    }

    private void setAttributeScale(float scale) {
        this.attributeScale = MathUtils.clamp(scale, GeyserAttributeType.SCALE.getMinimum(), GeyserAttributeType.SCALE.getMaximum());
        applyScale();
    }

    private void applyScale() {
        // Take any adjustments Bedrock requires, and compute it alongside the attribute's additional changes
        this.dirtyMetadata.put(EntityDataTypes.SCALE, scale * attributeScale);
    }

    /**
     * @return a Bedrock health attribute constructed from the data sent from the server
     */
    protected AttributeData createHealthAttribute() {
        // Default health needs to be specified as the max health in order for maximum hearts to show correctly on mounted entities
        // Round health value up, so that Bedrock doesn't consider the entity to be dead when health is between 0 and 1
        return new AttributeData(GeyserAttributeType.HEALTH.getBedrockIdentifier(), 0f, this.maxHealth, (float) Math.ceil(this.health), this.maxHealth);
    }

    @Override
    public boolean isAlive() {
        return this.valid && health > 0f;
    }

    @Override
    public InteractionResult interact(Hand hand) {
        GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand(hand);
        if (itemStack.asItem() == Items.NAME_TAG) {
            InteractionResult result = checkInteractWithNameTag(itemStack);
            if (result.consumesAction()) {
                return result;
            }
        }

        return super.interact(hand);
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        if (this instanceof ClientVehicle clientVehicle) {
            if (clientVehicle.isClientControlled()) {
                return;
            }
            clientVehicle.getVehicleComponent().moveRelative(relX, relY, relZ);
        }

        super.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
    }

    @Override
    public boolean setBoundingBoxHeight(float height) {
        if (valid && this instanceof ClientVehicle clientVehicle) {
            clientVehicle.getVehicleComponent().setHeight(height);
        }

        return super.setBoundingBoxHeight(height);
    }

    @Override
    public void setBoundingBoxWidth(float width) {
        if (valid && this instanceof ClientVehicle clientVehicle) {
            clientVehicle.getVehicleComponent().setWidth(width);
        }

        super.setBoundingBoxWidth(width);
    }

    /**
     * Checks to see if a nametag interaction would go through.
     */
    // Implementation note for 1.20.5: this code was moved to the NameTag item.
    protected final InteractionResult checkInteractWithNameTag(GeyserItemStack itemStack) {
        if (itemStack.getComponent(DataComponentType.CUSTOM_NAME) != null) {
            // The mob shall be named
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void updateArmor(GeyserSession session) {
        if (!valid) return;

        ItemData helmet = this.helmet;
        ItemData chestplate = this.chestplate;
        // If an entity has a banner on them, it will be in the helmet slot in Java but the chestplate spot in Bedrock
        // But don't overwrite the chestplate if it isn't empty
        ItemMapping banner = session.getItemMappings().getStoredItems().banner();
        if (ItemData.AIR.equals(chestplate) && helmet.getDefinition().equals(banner.getBedrockDefinition())) {
            chestplate = this.helmet;
            helmet = ItemData.AIR;
        } else if (chestplate.getDefinition().equals(banner.getBedrockDefinition())) {
            // Prevent chestplate banners from showing erroneously
            chestplate = ItemData.AIR;
        }

        MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
        armorEquipmentPacket.setRuntimeEntityId(geyserId);
        armorEquipmentPacket.setHelmet(helmet);
        armorEquipmentPacket.setChestplate(chestplate);
        armorEquipmentPacket.setLeggings(leggings);
        armorEquipmentPacket.setBoots(boots);
        armorEquipmentPacket.setBody(body);

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
        offHandPacket.setItem(offhand);
        offHandPacket.setHotbarSlot(-1);
        offHandPacket.setInventorySlot(0);
        offHandPacket.setContainerId(ContainerId.OFFHAND);

        session.sendUpstreamPacket(offHandPacket);
    }

    /**
     * Called when a SWING_ARM animation packet is received
     *
     * @return true if an ATTACK_START event should be used instead
     */
    public boolean useArmSwingAttack() {
        return false;
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
        if (javaAttribute.getType() instanceof AttributeType.Builtin type) {
            switch (type) {
                case MAX_HEALTH -> {
                    // Since 1.18.0, setting the max health to 0 or below causes the entity to die on Bedrock but not on Java
                    // See https://github.com/GeyserMC/Geyser/issues/2971
                    this.maxHealth = Math.max((float) AttributeUtils.calculateValue(javaAttribute), 1f);
                    newAttributes.add(createHealthAttribute());
                }
                case MOVEMENT_SPEED -> {
                    AttributeData attributeData = calculateAttribute(javaAttribute, GeyserAttributeType.MOVEMENT_SPEED);
                    newAttributes.add(attributeData);
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setMoveSpeed(attributeData.getValue());
                    }
                }
                case STEP_HEIGHT -> {
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setStepHeight((float) AttributeUtils.calculateValue(javaAttribute));
                    }
                }
                case GRAVITY ->  {
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setGravity(AttributeUtils.calculateValue(javaAttribute));
                    }
                }
                case ATTACK_DAMAGE -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.ATTACK_DAMAGE));
                case FLYING_SPEED -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.FLYING_SPEED));
                case FOLLOW_RANGE -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.FOLLOW_RANGE));
                case KNOCKBACK_RESISTANCE -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.KNOCKBACK_RESISTANCE));
                case JUMP_STRENGTH -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.HORSE_JUMP_STRENGTH));
                case SCALE -> {
                    // Attribute on Java, entity data on Bedrock
                    setAttributeScale((float) AttributeUtils.calculateValue(javaAttribute));
                    updateBedrockMetadata();
                }
            }
        }
    }

    /**
     * Calculates the complete attribute value to send to Bedrock. Will be overriden if attributes need to be cached.
     */
    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        return type.getAttribute((float) AttributeUtils.calculateValue(javaAttribute));
    }
}
