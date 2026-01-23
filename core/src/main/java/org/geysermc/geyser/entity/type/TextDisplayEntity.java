/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.jetbrains.annotations.Nullable;

// Note: 1.19.4 requires that the billboard is set to something in order to show, on Java Edition
@Getter
public class TextDisplayEntity extends DisplayBaseEntity {

    /**
     * On Java Edition, armor stands can have a custom name shown additionally to
     * the text in the display. They are rendered separately, and can cross each other...
     */
    private @Nullable ArmorStandEntity secondEntity = null;
    private boolean isNameTagVisible = false;
    private boolean isVisible = true;

    /**
     * The height offset per line of text in a text display entity when rendered
     * as an armor stand nametag on Bedrock Edition. This value was empirically adjusted
     * to match Java Edition's multi-line text centering behavior.
     */
    private static final float LINE_HEIGHT_OFFSET = 0.1414f;

    private int lineCount;

    public TextDisplayEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Remove armor stand body / hitbox
        this.dirtyMetadata.put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        this.dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
    }

    @Override
    protected void setInvisible(boolean value) {
        // we'll keep the text display armor stand always invisible; would reveal the armor stand otherwise
        // but we would need to adjust the nametag
        isVisible = value;
        this.updateNameTag();
    }

    @Override
    public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        this.isNameTagVisible = entityMetadata.getPrimitiveValue();
        if (entityMetadata.getPrimitiveValue()) {
            // Java client behavior, must show a nametag
            // If no custom name is set, it'll be the entity type name
            if (this.nametag.isBlank()) {
                setNametag(getDisplayName(), true);
            }
        } else {
            // If we have no custom name override, we have to reset the nametag
            if (customName == null) {
                setNametag(null, true);
            }
        }
    }

    @Override
    public void despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();
    }

    /**
     * Calculates the Y offset needed to match Java Edition's text centering
     * behavior for multi-line text displays.
     * <p>
     * In Java Edition, multi-line text displays are centered vertically.
     * This value differs from the 0.1414f multiplier used in {@link org.geysermc.geyser.util.EntityUtils}
     * for text displays mounted on players, as this handles the base positioning
     * rather than mount offset calculations.
     *
     * @return the Y offset to apply based on the number of lines
     */
    public float calculateLineOffset() {
        if (lineCount == 0) {
            return 0;
        }
        return -0.6f + LINE_HEIGHT_OFFSET * lineCount;
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        moveAbsoluteRaw(position.add(relX, relY, relZ), yaw, pitch, headYaw, isOnGround, false);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position.up(0.6f), yaw, pitch, headYaw, isOnGround, teleported);
        }
        super.moveAbsoluteRaw(position.up(calculateLineOffset()), yaw, pitch, headYaw, isOnGround, teleported);
    }

    public void setText(EntityMetadata<Component, ?> entityMetadata) {
        this.dirtyMetadata.put(EntityDataTypes.NAME, MessageTranslator.convertMessage(entityMetadata.getValue(), session.locale()));
        int newLineCount = calculateLineCount(entityMetadata.getValue());

        // If the line count changed, update the position to account for the new offset
        if (this.lineCount != newLineCount) {
            Vector3f positionWithoutOffset = position.down(calculateLineOffset());
            this.lineCount = newLineCount;
            moveAbsoluteRaw(positionWithoutOffset, yaw, pitch, headYaw, onGround, false);
        }
    }

    private int calculateLineCount(@Nullable Component text) {
        if (text == null) {
            return 0;
        }
        return PlainTextComponentSerializer.plainText().serialize(text).split("\n").length;
    }

    @Override
    protected void setNameEntityData(String nametag) {
        updateNameTag();
    }

    public void updateNameTag() {
        // Text displays are special: isNameTagVisible must be set for the custom name to ever show
        if (this.nametag.isBlank() || !isVisible || !isNameTagVisible) {
            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            return;
        }

        if (this.secondEntity == null) {
            secondEntity = new ArmorStandEntity(EntitySpawnContext.inherited(session, EntityDefinitions.ARMOR_STAND, this, position.down(calculateLineOffset()).up(0.6f)));
        }
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAME, this.nametag);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
        // Scale to 0 to show nametag
        secondEntity.setScale(0f);
        // No bounding box as we don't want to interact with this entity
        secondEntity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
        secondEntity.getDirtyMetadata().put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        if (!secondEntity.valid) { // Spawn the entity once
            secondEntity.spawnEntity();
        } else {
            secondEntity.updateBedrockMetadata();
        }
    }
}
