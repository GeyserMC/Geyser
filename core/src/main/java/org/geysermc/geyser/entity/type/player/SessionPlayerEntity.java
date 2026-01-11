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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.input.InputLocksFlag;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.TrapDoorBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.util.AttributeUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The entity class specifically for a {@link GeyserSession}'s player.
 */
public class SessionPlayerEntity extends PlayerEntity {
    /**
     * Used to fix some inconsistencies, especially in respawning.
     */
    @Getter
    protected final Map<GeyserAttributeType, AttributeData> attributes = new Object2ObjectOpenHashMap<>();

    /**
     * Java-only attributes
     */
    @Getter
    private double blockInteractionRange = GeyserAttributeType.BLOCK_INTERACTION_RANGE.getDefaultValue();
    @Getter
    private double miningEfficiency = GeyserAttributeType.MINING_EFFICIENCY.getDefaultValue();
    @Getter
    private double blockBreakSpeed = GeyserAttributeType.BLOCK_BREAK_SPEED.getDefaultValue();
    @Getter
    private double submergedMiningSpeed = GeyserAttributeType.SUBMERGED_MINING_SPEED.getDefaultValue();

    /**
     * Used in PlayerInputTranslator for movement checks.
     */
    @Getter
    private boolean isRidingInFront;
    /**
     * Used when emulating client-side vehicles
     */
    @Getter
    private Vector2f vehicleInput = Vector2f.ZERO;
    /**
     * Used when emulating client-side vehicles
     */
    @Getter
    private int vehicleJumpStrength;

    private int lastAirSupply = getMaxAir();

    @Getter @Setter
    private boolean insideScaffolding = false;

    /**
     * The client last tick end velocity, used for calculating player onGround.
     */
    @Getter @Setter
    private Vector3f lastTickEndVelocity = Vector3f.ZERO;

    /**
     * The client claimed interact rotation, intended for touch (pocket) user.
     */
    @Getter @Setter
    private Vector2f bedrockInteractRotation = Vector2f.ZERO;

    @Getter @Setter
    private float javaYaw;

    public SessionPlayerEntity(GeyserSession session) {
        super(new EntitySpawnContext(session, EntityDefinitions.PLAYER, -1, null), null, null);

        valid = true;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();

        // This allows player to be slowly push towards the closet space when stuck inside block instead of instantly moved out.
        setFlag(EntityFlag.PUSH_TOWARDS_CLOSEST_SPACE, true);
    }

    @Override
    protected void setClientSideSilent() {
        // Do nothing, since we want the session player to hear their own footstep sounds for example.
    }

    @Override
    public void spawnEntity() {
        // Already logged in
    }

    @Override
    public void setYaw(float yaw) {
        super.setYaw(yaw);
        this.javaYaw = yaw;
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        session.getCollisionManager().updatePlayerBoundingBox(this.position.down(definition.offset()));
    }

    @Override
    public void setPosition(Vector3f position) {
        if (valid) { // Don't update during session init
            session.getCollisionManager().updatePlayerBoundingBox(position);

            if (session.isNoClip() && position.getY() >= session.getBedrockDimension().minY() - 5) {
                session.setNoClip(false);
            }
        }
        this.position = position.add(0, definition.offset(), 0);
    }

    /**
     * Special method used only when updating the session player's rotation.
     * For some reason, Mode#NORMAL ignored rotation. Yay.
     * @param yaw the new yaw
     * @param pitch the new pitch
     * @param headYaw the head yaw
     */
    public void updateOwnRotation(float yaw, float pitch, float headYaw) {
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
        movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);

        session.sendUpstreamPacket(movePlayerPacket);

        // We're just setting rotation, player shouldn't lose motion, send motion packet to account for that.
        SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
        entityMotionPacket.setRuntimeEntityId(geyserId);
        entityMotionPacket.setMotion(motion);
        session.sendUpstreamPacket(entityMotionPacket);
    }

    /**
     * Set the player's position without applying an offset or moving the bounding box
     * This is used in BedrockMovePlayer which receives the player's position
     * with the offset pre-applied
     *
     * @param position the new position of the Bedrock player
     */
    public void setPositionManual(Vector3f position) {
        this.position = position;

        // Player is "above" the void so they're not supposed to no clip.
        if (session.isNoClip() && position.getY() - EntityDefinitions.PLAYER.offset() >= session.getBedrockDimension().minY() - 5) {
            session.setNoClip(false);
        }
    }

    /**
     * Sending any updated flags (sprinting, onFire, etc.) to the client while in spectator is not needed
     * Also "fixes" <a href="https://github.com/GeyserMC/Geyser/issues/3318">issue 3318</a>
     */
    @Override
    public void setFlags(ByteEntityMetadata entityMetadata) {
        // TODO: proper fix, BDS somehow does it? https://paste.gg/p/anonymous/3adfb7612f1540be80fa03a2281f93dc (BDS 1.20.13)
        if (!this.session.getGameMode().equals(GameMode.SPECTATOR)) {
            super.setFlags(entityMetadata);
        }
    }

    @Override
    protected void setSneaking(boolean value) {
        if (value) {
            session.startSneaking(false);
        } else {
            session.setShouldSendSneak(false);
            session.stopSneaking(false);
        }
    }

    /**
     * Since 1.19.40, the client must be re-informed of its bounding box on respawn
     * See <a href="https://github.com/GeyserMC/Geyser/issues/3370">issue 3370</a>
     */
    public void updateBoundingBox() {
        dirtyMetadata.put(EntityDataTypes.HEIGHT, getBoundingBoxHeight());
        dirtyMetadata.put(EntityDataTypes.WIDTH, getBoundingBoxWidth());
        updateBedrockMetadata();
    }

    @Override
    public boolean setBoundingBoxHeight(float height) {
        if (super.setBoundingBoxHeight(height)) {
            if (valid) { // Don't update during session init
                session.getCollisionManager().updatePlayerBoundingBox();
            }
            return true;
        }
        return false;
    }

    @Override
    public void setPose(Pose pose) {
        super.setPose(pose);

        if (pose != session.getPose()) {
            session.setPose(pose);
            updateBedrockMetadata();
        }
    }

    @Override
    public void setPitch(float pitch) {
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

    @Override
    protected void setAirSupply(int amount) {
        // Seemingly required to be sent as of Bedrock 1.21. Otherwise, bubbles will appear as empty
        // Also, this changes how the air bubble graphics/sounds are presented. Breathing on means sound effects and
        // the bubbles visually pop
        setFlag(EntityFlag.BREATHING, amount >= this.lastAirSupply);
        this.lastAirSupply = amount;
        super.setAirSupply(amount);
    }

    @Override
    public void setRiderSeatPosition(Vector3f position) {
        super.setRiderSeatPosition(position);
        this.isRidingInFront = position != null && position.getX() > 0;
    }

    @Override
    public AttributeData createHealthAttribute() {
        // Max health must be divisible by two in bedrock
        if ((maxHealth % 2) == 1) {
            maxHealth += 1;
        }
        return super.createHealthAttribute();
    }

    @Override
    protected boolean hasShield(boolean offhand) {
        // Must be overridden to point to the player's inventory cache
        if (offhand) {
            return session.getPlayerInventory().getOffhand().is(Items.SHIELD);
        } else {
            return session.getPlayerInventory().getItemInHand().is(Items.SHIELD);
        }
    }

    @Override
    protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
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

    @Override
    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        AttributeData attributeData = super.calculateAttribute(javaAttribute, type);
        this.attributes.put(type, attributeData);
        return attributeData;
    }

    /**
     * This will ONLY include attributes that have a Bedrock equivalent!!!
     * see {@link LivingEntity#updateAttribute(Attribute, List)}
     */
    public float attributeOrDefault(GeyserAttributeType type) {
        var attribute = this.attributes.get(type);
        if (attribute == null) {
            return type.getDefaultValue();
        }

        return attribute.getValue();
    }

    public void setLastDeathPosition(@Nullable GlobalPos pos) {
        if (pos != null) {
            dirtyMetadata.put(EntityDataTypes.PLAYER_LAST_DEATH_POS, pos.getPosition());
            dirtyMetadata.put(EntityDataTypes.PLAYER_LAST_DEATH_DIMENSION, DimensionUtils.javaToBedrock(pos.getDimension().asString()));
            dirtyMetadata.put(EntityDataTypes.PLAYER_HAS_DIED, true);
        } else {
            dirtyMetadata.put(EntityDataTypes.PLAYER_HAS_DIED, false);
        }
    }

    @Override
    public UUID getTabListUuid() {
        return session.getAuthData().uuid();
    }

    @Override
    public void setAbsorptionHearts(FloatEntityMetadata entityMetadata) {
        // The bedrock client can glitch when sending a health and absorption attribute in the same tick
        // This can happen when switching servers. Resending the absorption attribute fixes the issue
        attributes.put(GeyserAttributeType.ABSORPTION, GeyserAttributeType.ABSORPTION.getAttribute(entityMetadata.getPrimitiveValue()));
        super.setAbsorptionHearts(entityMetadata);
    }

    @Override
    public void setLivingEntityFlags(ByteEntityMetadata entityMetadata) {
        super.setLivingEntityFlags(entityMetadata);

        // Forcefully update flags since we're not tracking thing like using item properly.
        // For eg: when player start using item client-sided (and the USING_ITEM flag is false on geyser side)
        // If the server disagree with the player using item state, it will send a metadata set USING_ITEM flag to false
        // But since it never got set to true, nothing changed, causing the client to not receive the USING_ITEM flag they're supposed to.
        this.forceFlagUpdate();
    }

    @Override
    public void resetMetadata() {
        super.resetMetadata();

        // Reset air
        this.resetAir();

        // Absorption is metadata in java edition
        attributes.remove(GeyserAttributeType.ABSORPTION);
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        attributesPacket.setAttributes(Collections.singletonList(
                GeyserAttributeType.ABSORPTION.getAttribute(0f)));
        session.sendUpstreamPacket(attributesPacket);

        dirtyMetadata.put(EntityDataTypes.EFFECT_COLOR, 0);
        dirtyMetadata.put(EntityDataTypes.EFFECT_AMBIENCE, (byte) 0);
        dirtyMetadata.put(EntityDataTypes.FREEZING_EFFECT_STRENGTH, 0f);

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

    @Override
    public void setVehicle(Entity entity) {
        // For boats, we send width = 0.6 and height = 1.6 since there is otherwise a problem with player "clipping" into the boat when standing on it or running into it.
        // Having a wide bounding box fixed that, however, it is technically incorrect and creates certain problems
        // when you're actually riding the boat (https://github.com/GeyserMC/Geyser/issues/3106), since the box is way too big
        // the boat's motion stops right before the block is hit and doesn't let the actual bounding clip collide into the block,
        // causing the issues. So to fix this, everytime player enter a boat we send the java bounding box and only send the
        // definition box when player is not riding the boat.
        if (entity instanceof BoatEntity) {
            // These bounding box values are based off 1.21.7
            entity.setBoundingBoxWidth(1.375F);
            entity.setBoundingBoxHeight(0.5625F);
            entity.updateBedrockMetadata();
        } else if (entity == null && this.vehicle instanceof BoatEntity) {
            this.vehicle.setBoundingBoxWidth(this.vehicle.getDefinition().width());
            this.vehicle.setBoundingBoxHeight(this.vehicle.getDefinition().height());
            this.vehicle.updateBedrockMetadata();
        }

        // Bedrock player can dismount by pressing jump while Java cannot, so we need to prevent player from jumping to match vanilla behaviour.
        this.session.setLockInput(InputLocksFlag.JUMP, entity != null && entity.doesJumpDismount());
        this.session.updateInputLocks();

        super.setVehicle(entity);
    }
  
    /**
     * Used to calculate player jumping velocity for ground status calculation.
     */
    public float getJumpVelocity() {
        float velocity = 0.42F;

        if (session.getGeyser().getWorldManager().blockAt(session, this.getPosition().sub(0, EntityDefinitions.PLAYER.offset() + 0.1F, 0).toInt()).is(Blocks.HONEY_BLOCK)) {
            velocity *= 0.6F;
        }

        return velocity + 0.1F * session.getEffectCache().getJumpPower();
    }

    public boolean isOnClimbableBlock() {
        if (session.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        Vector3i pos = getPosition().down(EntityDefinitions.PLAYER.offset()).toInt();
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

    public boolean canStartGliding() {
        // You can't start gliding when levitation is applied
        if (session.getEffectCache().getEntityEffects().contains(Effect.LEVITATION)) {
            return false;
        }

        if (this.isOnClimbableBlock() || session.getPlayerEntity().isOnGround()) {
            return false;
        }

        if (session.getCollisionManager().isPlayerTouchingWater()) {
            return false;
        }

        // Unfortunately gliding is still client-side, so we cannot force the client to glide even
        // if we wanted to. However, we still need to check that gliding is possible even with, say,
        // an elytra that does not have the glider component.
        for (Map.Entry<EquipmentSlot, GeyserItemStack> entry : session.getPlayerInventory().getEquipment().entrySet()) {
            if (entry.getValue().getComponent(DataComponentTypes.GLIDER) != null) {
                Equippable equippable = entry.getValue().getComponent(DataComponentTypes.EQUIPPABLE);
                if (equippable != null && equippable.slot() == entry.getKey() && !entry.getValue().nextDamageWillBreak()) {
                    return true;
                }
            }

            // Bedrock will NOT allow flight when not wearing an elytra; even if it doesn't have a glider component
            if (entry.getKey() == EquipmentSlot.CHESTPLATE && !entry.getValue().is(Items.ELYTRA)) {
                return false;
            }
        }

        return false;
    }

    public void forceFlagUpdate() {
        setFlagsDirty(true);
    }

    public boolean isGliding() {
        return getFlag(EntityFlag.GLIDING);
    }
}
