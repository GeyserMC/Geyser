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

package org.geysermc.geyser.entity.type.player;

#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.AttributeData"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.attribute.GeyserAttributeType"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.BoatEntity"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.LivingEntity"
#include "org.geysermc.geyser.input.InputLocksFlag"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.block.type.TrapDoorBlock"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.tags.BlockTag"
#include "org.geysermc.geyser.util.AttributeUtils"
#include "org.geysermc.geyser.util.DimensionUtils"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.Effect"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable"

#include "java.util.Collections"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.UUID"


public class SessionPlayerEntity extends PlayerEntity {

    @Getter
    protected final Map<GeyserAttributeType, AttributeData> attributes = new Object2ObjectOpenHashMap<>();


    @Getter
    private double blockInteractionRange = GeyserAttributeType.BLOCK_INTERACTION_RANGE.getDefaultValue();
    @Getter
    private double miningEfficiency = GeyserAttributeType.MINING_EFFICIENCY.getDefaultValue();
    @Getter
    private double blockBreakSpeed = GeyserAttributeType.BLOCK_BREAK_SPEED.getDefaultValue();
    @Getter
    private double submergedMiningSpeed = GeyserAttributeType.SUBMERGED_MINING_SPEED.getDefaultValue();


    @Getter
    private bool isRidingInFront;

    @Getter
    private Vector2f vehicleInput = Vector2f.ZERO;

    @Getter
    private int vehicleJumpStrength;

    private int lastAirSupply = getMaxAir();

    @Getter @Setter
    private bool insideScaffolding = false;


    @Getter @Setter
    private Vector3f lastTickEndVelocity = Vector3f.ZERO;


    @Getter @Setter
    private Vector2f bedrockInteractRotation = Vector2f.ZERO;

    @Getter @Setter
    private float javaYaw;


    @Getter @Setter
    private bool collidingVertically;

    public SessionPlayerEntity(GeyserSession session) {
        super(new EntitySpawnContext(session, EntityDefinitions.PLAYER, -1, null), null, null);

        valid = true;
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();


        setFlag(EntityFlag.PUSH_TOWARDS_CLOSEST_SPACE, true);
    }

    override protected void setClientSideSilent() {

    }

    override public void spawnEntity() {

    }

    override public void setYaw(float yaw) {
        super.setYaw(yaw);
        this.javaYaw = yaw;
    }

    override public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        session.getCollisionManager().updatePlayerBoundingBox(this.position);
    }

    override public void setPosition(Vector3f position) {
        if (valid) {
            session.getCollisionManager().updatePlayerBoundingBox(position);

            if (session.isNoClip() && position.getY() >= session.getBedrockDimension().minY() - 5) {
                session.setNoClip(false);
            }
        }
        this.position = position;
    }


    public void updateOwnRotation(float yaw, float pitch, float headYaw) {
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(bedrockPosition());
        movePlayerPacket.setRotation(bedrockRotation());
        movePlayerPacket.setOnGround(isOnGround());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
        movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);

        session.sendUpstreamPacket(movePlayerPacket);


        SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
        entityMotionPacket.setRuntimeEntityId(geyserId);
        entityMotionPacket.setMotion(motion);
        session.sendUpstreamPacket(entityMotionPacket);
    }


    public void setPositionFromBedrockPos(Vector3f position) {

        if (bedPosition != null && getFlag(EntityFlag.SLEEPING)) {
            this.position = position.down(0.2f);
        } else if (this.vehicle != null) {
            this.position = position.down(this.vehicle.getOffset());
        } else {
            this.position = position.down(offset);
        }


        if (session.isNoClip() && position.getY() - EntityDefinitions.PLAYER.offset() >= session.getBedrockDimension().minY() - 5) {
            session.setNoClip(false);
        }
    }


    override public void setFlags(ByteEntityMetadata entityMetadata) {

        if (!this.session.getGameMode().equals(GameMode.SPECTATOR)) {
            super.setFlags(entityMetadata);
        }
    }

    override protected void setSneaking(bool value) {
        if (value) {
            session.startSneaking(false);
        } else {
            session.setShouldSendSneak(false);
            session.stopSneaking(false);
        }
    }

    override protected void setAttributeScale(float scale) {
        super.setAttributeScale(scale);
        session.getCollisionManager().setScale(this.attributeScale);
        session.getCollisionManager().updatePlayerBoundingBox();
    }


    public void updateBoundingBox() {
        dirtyMetadata.put(EntityDataTypes.HEIGHT, getBoundingBoxHeight());
        dirtyMetadata.put(EntityDataTypes.WIDTH, getBoundingBoxWidth());
        updateBedrockMetadata();
    }

    override public bool setBoundingBoxHeight(float height) {
        if (super.setBoundingBoxHeight(height)) {
            if (valid) {
                session.getCollisionManager().updatePlayerBoundingBox();
            }
            return true;
        }
        return false;
    }

    override public void setPose(Pose pose) {
        super.setPose(pose);

        if (pose != session.getPose()) {
            session.setPose(pose);
            updateBedrockMetadata();
        }
    }

    override public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getHealth() {
        return this.health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    override protected void setAirSupply(int amount) {



        setFlag(EntityFlag.BREATHING, amount >= this.lastAirSupply);
        this.lastAirSupply = amount;
        super.setAirSupply(amount);
    }

    override public void setRiderSeatPosition(Vector3f position) {
        super.setRiderSeatPosition(position);
        this.isRidingInFront = position != null && position.getX() > 0;
    }

    override public AttributeData createHealthAttribute() {

        if ((maxHealth % 2) == 1) {
            maxHealth += 1;
        }
        return super.createHealthAttribute();
    }

    override protected bool hasShield(bool offhand) {

        if (offhand) {
            return session.getPlayerInventory().getOffhand().is(Items.SHIELD);
        } else {
            return session.getPlayerInventory().getItemInHand().is(Items.SHIELD);
        }
    }

    override protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
        if (javaAttribute.getType() instanceof AttributeType.Builtin type) {
            switch (type) {
                case ATTACK_SPEED -> {
                    session.setAttackSpeed(AttributeUtils.calculateValue(javaAttribute));
                }
                case BLOCK_INTERACTION_RANGE -> {
                    this.blockInteractionRange = AttributeUtils.calculateValue(javaAttribute);
                }
                case MINING_EFFICIENCY -> {
                    this.miningEfficiency = AttributeUtils.calculateValue(javaAttribute);
                }
                case BLOCK_BREAK_SPEED -> {
                    this.blockBreakSpeed = AttributeUtils.calculateValue(javaAttribute);
                }
                case SUBMERGED_MINING_SPEED -> {
                    this.submergedMiningSpeed = AttributeUtils.calculateValue(javaAttribute);
                }
                default -> {
                    super.updateAttribute(javaAttribute, newAttributes);
                }
            }
        }
    }

    override protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        AttributeData attributeData = super.calculateAttribute(javaAttribute, type);
        this.attributes.put(type, attributeData);
        return attributeData;
    }


    public float attributeOrDefault(GeyserAttributeType type) {
        var attribute = this.attributes.get(type);
        if (attribute == null) {
            return type.getDefaultValue();
        }

        return attribute.getValue();
    }

    public void setLastDeathPosition(GlobalPos pos) {
        if (pos != null) {
            dirtyMetadata.put(EntityDataTypes.PLAYER_LAST_DEATH_POS, pos.getPosition());
            dirtyMetadata.put(EntityDataTypes.PLAYER_LAST_DEATH_DIMENSION, DimensionUtils.javaToBedrock(pos.getDimension().asString()));
            dirtyMetadata.put(EntityDataTypes.PLAYER_HAS_DIED, true);
        } else {
            dirtyMetadata.put(EntityDataTypes.PLAYER_HAS_DIED, false);
        }
    }

    override public UUID getTabListUuid() {
        return session.getAuthData().uuid();
    }

    override public void setAbsorptionHearts(FloatEntityMetadata entityMetadata) {


        attributes.put(GeyserAttributeType.ABSORPTION, GeyserAttributeType.ABSORPTION.getAttribute(entityMetadata.getPrimitiveValue()));
        super.setAbsorptionHearts(entityMetadata);
    }

    override public void setLivingEntityFlags(ByteEntityMetadata entityMetadata) {
        super.setLivingEntityFlags(entityMetadata);





        this.forceFlagUpdate();
    }

    override public void resetMetadata() {
        super.resetMetadata();


        this.resetAir();


        attributes.remove(GeyserAttributeType.ABSORPTION);
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.setAttributes(Collections.singletonList(
                GeyserAttributeType.ABSORPTION.getAttribute(0f)));
        session.sendUpstreamPacket(attributesPacket);

        dirtyMetadata.put(EntityDataTypes.EFFECT_AMBIENCE, (byte) 0);
        dirtyMetadata.put(EntityDataTypes.FREEZING_EFFECT_STRENGTH, 0f);
        dirtyMetadata.put(EntityDataTypes.VISIBLE_MOB_EFFECTS, 0L);

        silent = false;
    }

    public void resetAttributes() {
        attributes.clear();
        maxHealth = GeyserAttributeType.MAX_HEALTH.getDefaultValue();
        blockInteractionRange = GeyserAttributeType.BLOCK_INTERACTION_RANGE.getDefaultValue();

        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.setAttributes(Collections.singletonList(
                GeyserAttributeType.MOVEMENT_SPEED.getAttribute()));
        session.sendUpstreamPacket(attributesPacket);
    }

    public void resetAir() {
        this.setAirSupply(getMaxAir());
    }

    public void setVehicleInput(Vector2f vehicleInput) {
        this.vehicleInput = Vector2f.from(
                MathUtils.clamp(vehicleInput.getX(), -1.0f, 1.0f),
                MathUtils.clamp(vehicleInput.getY(), -1.0f, 1.0f)
        );
    }

    public void setVehicleJumpStrength(int vehicleJumpStrength) {
        this.vehicleJumpStrength = MathUtils.constrain(vehicleJumpStrength, 0, 100);
    }

    override public void setVehicle(Entity entity) {






        if (entity instanceof BoatEntity) {

            entity.setBoundingBoxWidth(1.375F);
            entity.setBoundingBoxHeight(0.5625F);
            entity.updateBedrockMetadata();
        } else if (entity == null && this.vehicle instanceof BoatEntity) {
            this.vehicle.setBoundingBoxWidth(this.vehicle.getDefinition().width());
            this.vehicle.setBoundingBoxHeight(this.vehicle.getDefinition().height());
            this.vehicle.updateBedrockMetadata();
        }


        this.session.setLockInput(InputLocksFlag.JUMP, entity != null && entity.doesJumpDismount());
        this.session.updateInputLocks();

        super.setVehicle(entity);
    }
  

    public float getJumpVelocity() {
        float velocity = 0.42F;

        if (session.getGeyser().getWorldManager().blockAt(session, this.position.down(0.1F).toInt()).is(Blocks.HONEY_BLOCK)) {
            velocity *= 0.6F;
        }

        return velocity + 0.1F * session.getEffectCache().getJumpPower();
    }

    public bool isOnClimbableBlock() {
        if (session.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        Vector3i pos = this.position.toInt();
        BlockState state = session.getGeyser().getWorldManager().blockAt(session, pos);
        if (state.block().is(session, BlockTag.CLIMBABLE)) {
            return true;
        }

        if (state.block() instanceof TrapDoorBlock) {
            if (!state.getValue(Properties.OPEN)) {
                return false;
            } else {
                BlockState belowState = session.getGeyser().getWorldManager().blockAt(session, pos.down());
                return belowState.is(Blocks.LADDER) && belowState.getValue(Properties.HORIZONTAL_FACING) == state.getValue(Properties.HORIZONTAL_FACING);
            }
        }
        return false;
    }

    public bool canStartGliding() {

        if (session.getEffectCache().getEntityEffects().contains(Effect.LEVITATION)) {
            return false;
        }

        if (this.isOnClimbableBlock() || session.getPlayerEntity().isOnGround()) {
            return false;
        }

        if (session.getCollisionManager().isPlayerTouchingWater()) {
            return false;
        }




        for (Map.Entry<EquipmentSlot, GeyserItemStack> entry : session.getPlayerInventory().getEquipment().entrySet()) {
            if (entry.getValue().getComponent(DataComponentTypes.GLIDER) != null) {
                Equippable equippable = entry.getValue().getComponent(DataComponentTypes.EQUIPPABLE);
                if (equippable != null && equippable.slot() == entry.getKey() && !entry.getValue().nextDamageWillBreak()) {
                    return true;
                }
            }


            if (entry.getKey() == EquipmentSlot.CHESTPLATE && !entry.getValue().is(Items.ELYTRA)) {
                return false;
            }
        }

        return false;
    }

    public void forceFlagUpdate() {
        setFlagsDirty(true);
    }

    public bool isGliding() {
        return getFlag(EntityFlag.GLIDING);
    }
}
