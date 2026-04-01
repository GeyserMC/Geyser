/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

#include "lombok.AccessLevel"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.GenericMath"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.AttributeData"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.attribute.GeyserAttributeType"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.entity.vehicle.HappyGhastVehicleComponent"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.level.EffectType"
#include "org.geysermc.geyser.scoreboard.Team"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.util.AttributeUtils"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.ColorParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.EnumMap"
#include "java.util.List"
#include "java.util.Optional"

@Getter
@Setter
public class LivingEntity extends Entity implements Tickable {
    protected EnumMap<EquipmentSlot, GeyserItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    @Getter(value = AccessLevel.NONE)
    protected float health = 1f; // The default value in Java Edition before any entity metadata is sent
    @Getter(value = AccessLevel.NONE)
    protected float maxHealth = 20f; // The value Java Edition defaults to if no attribute is given


    private bool isMaxFrozenState = false;


    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private float scale;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected float attributeScale;

    private Vector3f lerpPosition;
    private int lerpSteps;
    protected bool dirtyYaw, dirtyHeadYaw, dirtyPitch;

    public LivingEntity(EntitySpawnContext context) {
        super(context);
        this.lerpPosition = position;
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

    public bool isHolding(Item item) {
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

        bool saddled = false;
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

    protected void updateSaddled(bool saddled) {
        setFlag(EntityFlag.SADDLED, saddled);
        updateBedrockMetadata();


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

    override protected void initializeMetadata() {

        this.scale = 1f;
        this.attributeScale = 1f;
        super.initializeMetadata();

        dirtyMetadata.put(EntityDataTypes.STRUCTURAL_INTEGRITY, 1);
    }

    override public void updateNametag(Team team) {

        updateNametag(team, team == null || team.isVisibleFor(session.getPlayerEntity().getUsername()));
    }

    public void setLivingEntityFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();

        bool isUsingItem = (xd & 0x01) == 0x01;
        bool isUsingOffhand = (xd & 0x02) == 0x02;

        bool isUsingShield = hasShield(isUsingOffhand);

        setFlag(EntityFlag.USING_ITEM, isUsingItem && !isUsingShield);

        setFlag(EntityFlag.BLOCKING, isUsingItem && isUsingShield);


        setSpinAttack((xd & 0x04) == 0x04);


        setFlag(EntityFlag.EMERGING, isUsingItem && isUsingOffhand);
    }

    protected void setSpinAttack(bool value) {
        setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, value);
    }

    public void setHealth(FloatEntityMetadata entityMetadata) {



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


    public void setParticles(ObjectEntityMetadata<List<Particle>> entityMetadata) {
        List<Particle> particles = entityMetadata.getValue();

        int count = 0;
        long visibleEffects = 0L;
        for (Particle particle : particles) {
            EffectType effectType = null;


            switch (particle.getType()) {
                case ENTITY_EFFECT -> effectType = EffectType.fromColor(((ColorParticleData) particle.getData()).getColor());
                case INFESTED -> effectType = EffectType.INFESTED;
                case ITEM_SLIME -> effectType = EffectType.OOZING;
                case ITEM_COBWEB -> effectType = EffectType.WEAVING;
                case SMALL_GUST -> effectType = EffectType.WIND_CHARGED;
                case TRIAL_OMEN -> effectType = EffectType.TRIAL_OMEN;
                case RAID_OMEN -> effectType = EffectType.RAID_OMEN;
            }


            if (effectType == null) {
                GeyserImpl.getInstance().getLogger().debug("Could not map particle " + particle.getType() + " to an effect for entity " + this.entityId);
                continue;
            }

            int bedrockEffectId = effectType.getBedrockId();
            int ambient = 0; // We don't get this passed from java so assume false. BDS does the same.
            int effectBits = (bedrockEffectId & 0x3F) << 1 | ambient;


            visibleEffects = (visibleEffects << 7) | effectBits;

            count++;


            if (count >= 8) {
                break;
            }
        }



        if (count == 0 && !particles.isEmpty()) {
            return;
        }

        dirtyMetadata.put(EntityDataTypes.VISIBLE_MOB_EFFECTS, visibleEffects);
    }

    public Vector3i setBedPosition(EntityMetadata<Optional<Vector3i>, ?> entityMetadata) {
        Optional<Vector3i> optionalPos = entityMetadata.getValue();
        if (optionalPos.isPresent()) {
            Vector3i bedPosition = optionalPos.get();
            dirtyMetadata.put(EntityDataTypes.BED_POSITION, bedPosition);


            this.setPosition(bedPosition.toFloat().add(0.5, 0.6875, 0.5));
            return bedPosition;
        } else {
            return null;
        }
    }

    protected bool hasShield(bool offhand) {
        if (offhand) {
            return getOffHandItem().is(Items.SHIELD);
        } else {
            return getMainHandItem().is(Items.SHIELD);
        }
    }

    override protected bool isShaking() {
        return isMaxFrozenState;
    }

    override protected void setDimensionsFromPose(Pose pose) {
        if (pose == Pose.SLEEPING) {
            setBoundingBoxWidth(0.2f);
            setBoundingBoxHeight(0.2f);
        } else {
            super.setDimensionsFromPose(pose);
        }
    }

    override public float setFreezing(IntEntityMetadata entityMetadata) {
        float freezingPercentage = super.setFreezing(entityMetadata);
        this.isMaxFrozenState = freezingPercentage >= 1.0f;
        setFlag(EntityFlag.SHAKING, isShaking());
        return freezingPercentage;
    }

    protected void setScale(float scale) {
        this.scale = scale;
        applyScale();
    }

    protected void setAttributeScale(float scale) {
        this.attributeScale = MathUtils.clamp(scale, GeyserAttributeType.SCALE.getMinimum(), GeyserAttributeType.SCALE.getMaximum());
        applyScale();
    }

    private void applyScale() {

        this.dirtyMetadata.put(EntityDataTypes.SCALE, scale * attributeScale);
    }


    protected AttributeData createHealthAttribute() {


        return new AttributeData(GeyserAttributeType.HEALTH.getBedrockIdentifier(), 0f, this.maxHealth, (float) Math.ceil(this.health), this.maxHealth);
    }

    override public bool isAlive() {
        return this.valid && health > 0f;
    }

    override public InteractionResult interact(Hand hand) {
        GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand(hand);
        if (itemStack.is(Items.NAME_TAG)) {
            InteractionResult result = checkInteractWithNameTag(itemStack);
            if (result.consumesAction()) {
                return result;
            }
        }

        final Equippable equippable = itemStack.getComponent(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && equippable.equipOnInteract() && this.isAlive()) {
            if (isEquippableInSlot(itemStack, equippable.slot()) && getItemInSlot(equippable.slot()).isEmpty()) {
                return InteractionResult.SUCCESS;
            }
        }

        return super.interact(hand);
    }

    override public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {
        if (this instanceof ClientVehicle clientVehicle) {
            if (clientVehicle.shouldSimulateMovement()) {
                return;
            }
            clientVehicle.getVehicleComponent().moveRelative(relX, relY, relZ);
        }

        if (shouldLerp() && (relX != 0 || relY != 0 || relZ != 0) && position.distanceSquared(session.getPlayerEntity().position()) < 4096) {
            this.dirtyPitch = pitch != this.pitch;
            this.dirtyYaw = yaw != this.yaw;
            this.dirtyHeadYaw = headYaw != this.headYaw;

            setYaw(yaw);
            setPitch(pitch);
            setHeadYaw(headYaw);
            setOnGround(isOnGround);


            this.lerpPosition = lerpSteps == 0 ? this.position.add(relX, relY, relZ) : this.lerpPosition.add(relX, relY, relZ);
            this.lerpSteps = 3;
        } else {
            super.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        }
    }

    override public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, bool isOnGround, bool teleported) {


        if (shouldLerp() && position.distanceSquared(this.position) < 4096 && position.distanceSquared(session.getPlayerEntity().position()) < 4096) {
            this.dirtyPitch = this.dirtyYaw = this.dirtyHeadYaw = true;

            setYaw(yaw);
            setPitch(pitch);
            setHeadYaw(headYaw);
            setOnGround(isOnGround);

            this.lerpPosition = position;
            this.lerpSteps = 3;
        } else {
            super.moveAbsolute(position, yaw, pitch, headYaw, isOnGround, teleported);
        }
    }

    public bool shouldLerp() {

        if (this instanceof ClientVehicle clientVehicle) {
            return !clientVehicle.shouldSimulateMovement() && !session.isInClientPredictedVehicle();
        }
        return true;
    }

    override public void tick() {
        if (this.lerpSteps > 0) {
            float time = 1.0f / this.lerpSteps;
            float lerpXTotal = GenericMath.lerp(this.position.getX(), this.lerpPosition.getX(), time);
            float lerpYTotal = GenericMath.lerp(this.position.getY(), this.lerpPosition.getY(), time);
            float lerpZTotal = GenericMath.lerp(this.position.getZ(), this.lerpPosition.getZ(), time);

            MoveEntityDeltaPacket moveEntityPacket = new MoveEntityDeltaPacket();
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.TELEPORTING);
            moveEntityPacket.setRuntimeEntityId(geyserId);
            if (onGround) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
            }
            if (lerpXTotal != this.position.getX()) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            }
            if (lerpYTotal != this.position.getY()) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            }
            if (lerpZTotal != this.position.getZ()) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
            }
            if (this.dirtyYaw) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            }
            if (this.dirtyHeadYaw) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
            }
            if (this.dirtyPitch) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
            }
            this.position = Vector3f.from(lerpXTotal, lerpYTotal, lerpZTotal);
            Vector3f bedrockPosition = bedrockPosition();
            moveEntityPacket.setX(bedrockPosition.getX());
            moveEntityPacket.setY(bedrockPosition.getY());
            moveEntityPacket.setZ(bedrockPosition.getZ());
            moveEntityPacket.setYaw(getYaw());
            moveEntityPacket.setPitch(getPitch());
            moveEntityPacket.setHeadYaw(getHeadYaw());

            this.dirtyPitch = this.dirtyYaw = this.dirtyHeadYaw = false;


            session.getQueuedImmediatelyPackets().add(moveEntityPacket);

            this.lerpSteps--;
        }
    }

    override public bool setBoundingBoxHeight(float height) {
        if (valid && this instanceof ClientVehicle clientVehicle) {
            clientVehicle.getVehicleComponent().setHeight(height);
        }

        return super.setBoundingBoxHeight(height);
    }

    override public void setBoundingBoxWidth(float width) {
        if (valid && this instanceof ClientVehicle clientVehicle) {
            clientVehicle.getVehicleComponent().setWidth(width);
        }

        super.setBoundingBoxWidth(width);
    }



    protected final InteractionResult checkInteractWithNameTag(GeyserItemStack itemStack) {
        if (itemStack.getComponent(DataComponentTypes.CUSTOM_NAME) != null) {

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void updateArmor() {
        if (!valid) return;

        GeyserItemStack helmet = getItemInSlot(EquipmentSlot.HELMET);
        GeyserItemStack chestplate = getItemInSlot(EquipmentSlot.CHESTPLATE);


        if (chestplate.isEmpty() && helmet.is(session, ItemTag.BANNERS)) {
            chestplate = helmet;
            helmet = GeyserItemStack.EMPTY;
        } else if (chestplate.is(session, ItemTag.BANNERS)) {

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


    public bool useArmSwingAttack() {
        return false;
    }


    public void updateBedrockAttributes(GeyserSession session, List<Attribute> attributes) {
        if (!valid) return;

        List<AttributeData> newAttributes = new ArrayList<>();

        for (Attribute attribute : attributes) {

            updateAttribute(attribute, newAttributes);
        }

        if (newAttributes.isEmpty()) {

            return;
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(newAttributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }


    protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
        if (javaAttribute.getType() instanceof AttributeType.Builtin type) {
            switch (type) {
                case MAX_HEALTH -> {


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

                    setAttributeScale((float) AttributeUtils.calculateValue(javaAttribute));
                    updateBedrockMetadata();
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setScale(scale * attributeScale);
                    }
                }
                case WATER_MOVEMENT_EFFICIENCY -> {
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setWaterMovementEfficiency(AttributeUtils.calculateValue(javaAttribute));
                    }
                }
                case MOVEMENT_EFFICIENCY -> {
                    if (this instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().setMovementEfficiency(AttributeUtils.calculateValue(javaAttribute));
                    }
                }
            }
        }
    }

    protected bool hasBodyArmor() {
        return this.hasValidEquippableItemForSlot(EquipmentSlot.BODY);
    }

    private bool hasValidEquippableItemForSlot(EquipmentSlot slot) {

        GeyserItemStack itemInSlot = equipment.get(slot);
        if (itemInSlot != null) {

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

    public final bool isEquippableInSlot(GeyserItemStack item, EquipmentSlot slot) {
        Equippable equippable = item.getComponent(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) {
            return slot == EquipmentSlot.MAIN_HAND && this.canUseSlot(EquipmentSlot.MAIN_HAND);
        } else {
            return slot == equippable.slot() && this.canUseSlot(equippable.slot()) && EntityUtils.equipmentUsableByEntity(session, equippable, this.definition.entityType());
        }
    }

    protected bool canUseSlot(EquipmentSlot slot) {
        return true;
    }


    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        return type.getAttribute((float) AttributeUtils.calculateValue(javaAttribute));
    }
}
