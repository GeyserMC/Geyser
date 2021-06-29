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

package org.geysermc.connector.entity.living.animal.tameable;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;

public class CatEntity extends TameableEntity {

    private byte collarColor;

    public CatEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateRotation(GeyserSession session, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, 0, 0, 0, Vector3f.from(this.rotation.getX(), pitch, yaw), isOnGround);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);
        if (entityMetadata.getId() == 16) {
            metadata.put(EntityData.SCALE, (boolean) entityMetadata.getValue() ? 0.4f : 0.8f);
        } else if (entityMetadata.getId() == 17) {
            // Update collar color if tamed
            if (metadata.getFlags().getFlag(EntityFlag.TAMED)) {
                metadata.put(EntityData.COLOR, collarColor);
            }
        }
        if (entityMetadata.getId() == 19) {
            // Different colors in Java and Bedrock for some reason
            int variantColor;
            switch ((int) entityMetadata.getValue()) {
                case 0:
                    variantColor = 8;
                    break;
                case 8:
                    variantColor = 0;
                    break;
                case 9:
                    variantColor = 10;
                    break;
                case 10:
                    variantColor = 9;
                    break;
                default:
                    variantColor = (int) entityMetadata.getValue();
            }
            metadata.put(EntityData.VARIANT, variantColor);
        }
        if (entityMetadata.getId() == 20) {
            metadata.getFlags().setFlag(EntityFlag.RESTING, (boolean) entityMetadata.getValue());
        }
        if (entityMetadata.getId() == 22) {
            collarColor = (byte) (int) entityMetadata.getValue();
            // Needed or else wild cats are a red color
            if (metadata.getFlags().getFlag(EntityFlag.TAMED)) {
                metadata.put(EntityData.COLOR, collarColor);
            }
        }
    }

    @Override
    public boolean canEat(GeyserSession session, String javaIdentifierStripped, ItemEntry itemEntry) {
        return javaIdentifierStripped.equals("cod") || javaIdentifierStripped.equals("salmon");
    }
}
