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

package org.geysermc.geyser.entity.type.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.AttributeUtils;

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
     * Whether to check for updated speed after all entity metadata has been processed
     */
    private boolean refreshSpeed = false;
    /**
     * Used in PlayerInputTranslator for movement checks.
     */
    @Getter
    private boolean isRidingInFront;
    /**
     * Used for villager inventory emulation.
     */
    private int fakeTradeXp;

    private final GeyserSession session;

    public SessionPlayerEntity(GeyserSession session) {
        super(session, 1, 1, new GameProfile(UUID.randomUUID(), "unknown"), Vector3f.ZERO, Vector3f.ZERO, 0, 0, 0);

        valid = true;
        this.session = session;
    }

    @Override
    public void spawnEntity() {
        // Already logged in
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        session.getCollisionManager().updatePlayerBoundingBox(this.position.down(definition.offset()));
    }

    @Override
    public void setPosition(Vector3f position) {
        if (session != null) { // null during entity initialization
            session.getCollisionManager().updatePlayerBoundingBox(position);
        }
        super.setPosition(position);
    }

    /**
     * Set the player's position without applying an offset or moving the bounding box
     * This is used in BedrockMovePlayerTranslator which receives the player's position
     * with the offset pre-applied
     *
     * @param position the new position of the Bedrock player
     */
    public void setPositionManual(Vector3f position) {
        this.position = position;
    }

    @Override
    public void setFlags(ByteEntityMetadata entityMetadata) {
        super.setFlags(entityMetadata);
        session.setSwimmingInWater((entityMetadata.getPrimitiveValue() & 0x10) == 0x10 && getFlag(EntityFlag.SPRINTING));
        refreshSpeed = true;
    }

    @Override
    public void setPose(EntityMetadata<Pose, ?> entityMetadata) {
        super.setPose(entityMetadata);
        session.setPose(entityMetadata.getValue());
        refreshSpeed = true;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    @Override
    protected void setAirSupply(int amount) {
        if (amount == getMaxAir()) {
            super.setAirSupply(0); // Hide the bubble counter from the UI for the player
        } else {
            super.setAirSupply(amount);
        }
    }

    @Override
    public void setRiderSeatPosition(Vector3f position) {
        super.setRiderSeatPosition(position);
        this.isRidingInFront = position != null && position.getX() > 0;
    }

    public void addFakeTradeExperience(int tradeXp) {
        fakeTradeXp += tradeXp;
        dirtyMetadata.put(EntityData.TRADE_XP, fakeTradeXp);
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
    public void updateBedrockMetadata() {
        super.updateBedrockMetadata();
        if (refreshSpeed) {
            AttributeData speedAttribute = session.adjustSpeed();
            if (speedAttribute != null) {
                UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(geyserId);
                attributesPacket.setAttributes(Collections.singletonList(speedAttribute));
                session.sendUpstreamPacket(attributesPacket);
            }
            refreshSpeed = false;
        }
    }

    @Override
    protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
        if (javaAttribute.getType() == AttributeType.Builtin.GENERIC_ATTACK_SPEED) {
            session.setAttackSpeed(AttributeUtils.calculateValue(javaAttribute));
        } else {
            super.updateAttribute(javaAttribute, newAttributes);
        }
    }

    @Override
    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        AttributeData attributeData = super.calculateAttribute(javaAttribute, type);

        if (javaAttribute.getType() == AttributeType.Builtin.GENERIC_MOVEMENT_SPEED) {
            session.setOriginalSpeedAttribute(attributeData.getValue());
            AttributeData speedAttribute = session.adjustSpeed();
            if (speedAttribute != null) {
                // Overwrite the attribute with our own
                this.attributes.put(type, speedAttribute);
                return speedAttribute;
            }
        }

        this.attributes.put(type, attributeData);
        return attributeData;
    }
}
