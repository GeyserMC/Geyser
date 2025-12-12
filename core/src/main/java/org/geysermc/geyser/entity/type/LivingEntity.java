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
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.HappyGhastVehicleComponent;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.AttributeUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ColorParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class LivingEntity extends Entity {
    protected EnumMap<EquipmentSlot, GeyserItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

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

    public GeyserItemStack getItemInSlot(EquipmentSlot slot) {
        GeyserItemStack stack = equipment.get(slot);
        if (stack == null) {
            return GeyserItemStack.EMPTY;
        }
        return stack;
    }

    public GeyserItemStack getMainHandItem() {
        return getItemInSlot(EquipmentSlot.MAIN_HAND);
    }

    public GeyserItemStack getOffHandItem() {
        return getItemInSlot(EquipmentSlot.OFF_HAND);
    }

    public boolean isHolding(Item item) {
        return getMainHandItem().is(item) || getOffHandItem().is(item);
    }

    public void setHelmet(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.HELMET, stack);
    }

    public void setChestplate(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.CHESTPLATE, stack);
    }

    public void setLeggings(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.LEGGINGS, stack);
    }

    public void setBoots(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.BOOTS, stack);
    }

    public void setBody(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.BODY, stack);
    }

    public void setSaddle(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.SADDLE, stack);

        boolean saddled = false;
        if (!stack.isEmpty()) {
            Equippable equippable = stack.getComponent(DataComponentTypes.EQUIPPABLE);
            saddled = equippable != null && equippable.slot() == EquipmentSlot.SADDLE;
        }

        updateSaddled(saddled);
    }

    public void setHand(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.MAIN_HAND, stack);
    }

    public void setOffhand(GeyserItemStack stack) {
        this.equipment.put(EquipmentSlot.OFF_HAND, stack);
    }

    protected void updateSaddled(boolean saddled) {
        setFlag(EntityFlag.SADDLED, saddled);
        updateBedrockMetadata();

        // Update the interactive tag, if necessary
        Entity mouseoverEntity = session.getMouseoverEntity();
        if (mouseoverEntity != null && mouseoverEntity.getEntityId() == entityId) {
            mouseoverEntity.updateInteractiveTag();
        }
    }

    public void switchHands() {
        GeyserItemStack offhand = this.equipment.get(EquipmentSlot.OFF_HAND);
        this.equipment.put(EquipmentSlot.OFF_HAND, this.equipment.get(EquipmentSlot.MAIN_HAND));
        this.equipment.put(EquipmentSlot.MAIN_HAND, offhand);
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

    public void setLivingEntityFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();

        boolean isUsingItem = (xd & 0x01) == 0x01;
        boolean isUsingOffhand = (xd & 0x02) == 0x02;

        boolean isUsingShield = hasShield(isUsingOffhand);

        setFlag(EntityFlag.USING_ITEM, isUsingItem && !isUsingShield);
        // Override the blocking
        setFlag(EntityFlag.BLOCKING, isUsingItem && isUsingShield);

        // Riptide spin attack
        setSpinAttack((xd & 0x04) == 0x04);

        // OptionalPack usage
        setFlag(EntityFlag.EMERGING, isUsingItem && isUsingOffhand);
    }

    protected void setSpinAttack(boolean value) {
        setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, value);
    }

    public void setHealth(FloatEntityMetadata entityMetadata) {
        // The server can send health value early, causing health to be larger than maxHealth, which can freaks out Bedrock client
        // in some weird way, eg: https://github.com/GeyserMC/Geyser/issues/5918 or it could be intentional....  Either way
        // we do this to account for it, since currently it seems like Java client doesn't care while Bedrock does.
        this.health = Math.max(0, entityMetadata.getPrimitiveValue());
        if (this.health > this.maxHealth) {
            this.maxHealth = this.health;
        }

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

            int color = ((ColorParticleData) particle.getData()).getColor();
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
        if (offhand) {
            return getOffHandItem().is(Items.SHIELD);
        } else {
            return getMainHandItem().is(Items.SHIELD);
        }
    }

    @Override
    protected boolean isShaking() {
        return isMaxFrozenState;
    }

    @Override
    protected void setDimensionsFromPose(Pose pose) {
        if (pose == Pose.SLEEPING) {
            setBoundingBoxWidth(0.2f);
            setBoundingBoxHeight(0.2f);
        } else {
            super.setDimensionsFromPose(pose);
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
        if (itemStack.is(Items.NAME_TAG)) {
            InteractionResult result = checkInteractWithNameTag(itemStack);
            if (result.consumesAction()) {
                return result;
            }
        }

        return super.interact(hand);
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
        if (itemStack.getComponent(DataComponentTypes.CUSTOM_NAME) != null) {
            // The mob shall be named
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void updateArmor() {
        if (!valid) return;

        GeyserItemStack helmet = getItemInSlot(EquipmentSlot.HELMET);
        GeyserItemStack chestplate = getItemInSlot(EquipmentSlot.CHESTPLATE);
        // If an entity has a banner on them, it will be in the helmet slot in Java but the chestplate spot in Bedrock
        // But don't overwrite the chestplate if it isn't empty
        if (chestplate.isEmpty() && helmet.is(session, ItemTag.BANNERS)) {
            chestplate = helmet;
            helmet = GeyserItemStack.EMPTY;
        } else if (chestplate.is(session, ItemTag.BANNERS)) {
            // Prevent chestplate banners from showing erroneously
            chestplate = GeyserItemStack.EMPTY;
        }

        MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
        armorEquipmentPacket.setRuntimeEntityId(geyserId);
        armorEquipmentPacket.setHelmet(ItemTranslator.translateToBedrock(session, helmet));
        armorEquipmentPacket.setChestplate(ItemTranslator.translateToBedrock(session, chestplate));
        armorEquipmentPacket.setLeggings(ItemTranslator.translateToBedrock(session, getItemInSlot(EquipmentSlot.LEGGINGS)));
        armorEquipmentPacket.setBoots(ItemTranslator.translateToBedrock(session, getItemInSlot(EquipmentSlot.BOOTS)));
        armorEquipmentPacket.setBody(ItemTranslator.translateToBedrock(session, getItemInSlot(EquipmentSlot.BODY)));

        session.sendUpstreamPacket(armorEquipmentPacket);
    }

    public void updateMainHand() {
        if (!valid) return;

        MobEquipmentPacket handPacket = new MobEquipmentPacket();
        handPacket.setRuntimeEntityId(geyserId);
        handPacket.setItem(ItemTranslator.translateToBedrock(session, getMainHandItem()));
        handPacket.setHotbarSlot(-1);
        handPacket.setInventorySlot(0);
        handPacket.setContainerId(ContainerId.INVENTORY);

        session.sendUpstreamPacket(handPacket);
    }

    public void updateOffHand() {
        if (!valid) return;

        MobEquipmentPacket offHandPacket = new MobEquipmentPacket();
        offHandPacket.setRuntimeEntityId(geyserId);
        offHandPacket.setItem(ItemTranslator.translateToBedrock(session, getOffHandItem()));
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
                case FLYING_SPEED -> {
                    AttributeData attributeData = calculateAttribute(javaAttribute, GeyserAttributeType.FLYING_SPEED);
                    newAttributes.add(attributeData);
                    if (this instanceof HappyGhastEntity ghast && ghast.getVehicleComponent() instanceof HappyGhastVehicleComponent component) {
                        component.setFlyingSpeed(attributeData.getValue());
                    }
                }
                case FOLLOW_RANGE -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.FOLLOW_RANGE));
                case KNOCKBACK_RESISTANCE -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.KNOCKBACK_RESISTANCE));
                case JUMP_STRENGTH -> newAttributes.add(calculateAttribute(javaAttribute, GeyserAttributeType.HORSE_JUMP_STRENGTH));
                case SCALE -> {
                    // Attribute on Java, entity data on Bedrock
                    setAttributeScale((float) AttributeUtils.calculateValue(javaAttribute));
                    updateBedrockMetadata();
                }
                case WATER_MOVEMENT_EFFICIENCY -> {
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setWaterMovementEfficiency(AttributeUtils.calculateValue(javaAttribute));
                    }
                }
            }
        }
    }

    protected boolean hasBodyArmor() {
        return this.hasValidEquippableItemForSlot(EquipmentSlot.BODY);
    }

    private boolean hasValidEquippableItemForSlot(EquipmentSlot slot) {
        // MojMap LivingEntity#hasItemInSlot
        GeyserItemStack itemInSlot = equipment.get(slot);
        if (itemInSlot != null) {
            // MojMap LivingEntity#isEquippableInSlot
            Equippable equippable = itemInSlot.getComponent(DataComponentTypes.EQUIPPABLE);
            if (equippable != null) {
                return slot == equippable.slot() &&
                    canUseSlot(slot) &&
                    EntityUtils.equipmentUsableByEntity(session, equippable, this.definition.entityType());
            } else {
                return slot == EquipmentSlot.MAIN_HAND && canUseSlot(EquipmentSlot.MAIN_HAND);
            }
        }

        return false;
    }

    protected boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    /**
     * Calculates the complete attribute value to send to Bedrock. Will be overriden if attributes need to be cached.
     */
    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        return type.getAttribute((float) AttributeUtils.calculateValue(javaAttribute));
    }
}
