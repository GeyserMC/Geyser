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

package org.geysermc.geyser.entity.type.living.animal.tameable;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.UUID;

public class CatEntity extends TameableEntity {

    private byte collarColor;

    public CatEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void updateRotation(float yaw, float pitch, boolean isOnGround) {
        moveRelative(0, 0, 0, yaw, pitch, yaw, isOnGround);
    }

    @Override
    protected float getAdultSize() {
        return 0.8f;
    }

    @Override
    protected float getBabySize() {
        return 0.4f;
    }

    @Override
    public void setTameableFlags(ByteEntityMetadata entityMetadata) {
        super.setTameableFlags(entityMetadata);
        // Update collar color if tamed
        if (getFlag(EntityFlag.TAMED)) {
            dirtyMetadata.put(EntityData.COLOR, collarColor);
        }
    }

    public void setCatVariant(IntEntityMetadata entityMetadata) {
        // Different colors in Java and Bedrock for some reason
        int metadataValue = entityMetadata.getPrimitiveValue();
        int variantColor = switch (metadataValue) {
            case 0 -> 8;
            case 8 -> 0;
            case 9 -> 10;
            case 10 -> 9;
            default -> metadataValue;
        };
        dirtyMetadata.put(EntityData.VARIANT, variantColor);
    }

    public void setResting(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.RESTING, entityMetadata.getPrimitiveValue());
    }

    public void setCollarColor(IntEntityMetadata entityMetadata) {
        collarColor = (byte) entityMetadata.getPrimitiveValue();
        // Needed or else wild cats are a red color
        if (getFlag(EntityFlag.TAMED)) {
            dirtyMetadata.put(EntityData.COLOR, collarColor);
        }
    }

    @Override
    public boolean canEat(String javaIdentifierStripped, ItemMapping mapping) {
        return javaIdentifierStripped.equals("cod") || javaIdentifierStripped.equals("salmon");
    }
}
