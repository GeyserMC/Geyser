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
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.jetbrains.annotations.Nullable;

// Note: 1.19.4 requires that the billboard is set to something in order to show, on Java Edition
@Getter
public class TextDisplayEntity extends DisplayBaseEntity {

    /**
     * The height offset per line of text in a text display entity when rendered
     * as an armor stand nametag on Bedrock Edition.
     * <p>
     * This value was empirically adjusted to match Java Edition's multi-line text
     * centering behavior. Note that this differs from the 0.1414f multiplier used
     * in {@link org.geysermc.geyser.util.EntityUtils} for mount offset calculations.
     */
    private static final float LINE_HEIGHT_OFFSET = 0.12f;

    private int lineCount;

    public TextDisplayEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelativeRaw(relX, relY + definition.offset(), relZ, yaw, pitch, headYaw, isOnGround);
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
    private float calculateLineOffset() {
        if (lineCount == 0) {
            return 0;
        }
        return LINE_HEIGHT_OFFSET * lineCount;
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        super.moveAbsoluteRaw(position.add(0, calculateLineOffset(), 0), yaw, pitch, headYaw, isOnGround, teleported);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Remove armor stand body / hitbox
        this.dirtyMetadata.put(EntityDataTypes.HITBOX, NbtMap.EMPTY);
        this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        this.dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
    }

    public void setText(EntityMetadata<Component, ?> entityMetadata) {
        this.dirtyMetadata.put(EntityDataTypes.NAME, MessageTranslator.convertMessage(entityMetadata.getValue(), session.locale()));
        
        int previousLineCount = lineCount;
        calculateLineCount(entityMetadata.getValue());

        // If the line count changed, update the position to account for the new offset
        if (previousLineCount != lineCount) {
            moveAbsoluteRaw(position, yaw, pitch, headYaw, onGround, false);
        }
    }

    private void calculateLineCount(@Nullable Component text) {
        if (text == null) {
            lineCount = 0;
            return;
        }
        lineCount = PlainTextComponentSerializer.plainText().serialize(text).split("\n").length;
    }
}
