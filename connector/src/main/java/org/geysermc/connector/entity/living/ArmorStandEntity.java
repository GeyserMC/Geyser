/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity.living;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ArmorStandEntity extends LivingEntity {

    // These are used to store the state of the armour stand for use when handling invisibility
    private boolean isMarker = false;
    private boolean isInvisible = false;
    private boolean isSmall = false;

    public ArmorStandEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        // Fake the height to be above where it is so the nametag appears in the right location for invisible non-marker armour stands
        if (!isMarker && isInvisible) {
            position = position.add(0d, entityType.getHeight() * (isSmall ? 0.55d : 1d), 0d);
        }

        super.moveAbsolute(session, position, rotation, isOnGround, teleported);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 0 && entityMetadata.getType() == MetadataType.BYTE) {
            byte xd = (byte) entityMetadata.getValue();

            // Check if the armour stand is invisible and store accordingly
            if ((xd & 0x20) == 0x20) {
                metadata.put(EntityData.SCALE, 0.0f);
                isInvisible = true;
            }
        } else if (entityMetadata.getId() == 14 && entityMetadata.getType() == MetadataType.BYTE) {
            byte xd = (byte) entityMetadata.getValue();

            // isSmall
            if ((xd & 0x01) == 0x01) {
                isSmall = true;

                if (metadata.getFloat(EntityData.SCALE) != 0.55f && metadata.getFloat(EntityData.SCALE) != 0.0f) {
                    metadata.put(EntityData.SCALE, 0.55f);
                }

                if (metadata.get(EntityData.BOUNDING_BOX_WIDTH) != null && metadata.get(EntityData.BOUNDING_BOX_WIDTH).equals(0.5f)) {
                    metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.25f);
                    metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.9875f);
                }
            } else if (metadata.get(EntityData.BOUNDING_BOX_WIDTH) != null && metadata.get(EntityData.BOUNDING_BOX_WIDTH).equals(0.25f)) {
                metadata.put(EntityData.BOUNDING_BOX_WIDTH, entityType.getWidth());
                metadata.put(EntityData.BOUNDING_BOX_HEIGHT, entityType.getHeight());
            }

            // setMarker
            if ((xd & 0x10) == 0x10 && (metadata.get(EntityData.BOUNDING_BOX_WIDTH) == null || !metadata.get(EntityData.BOUNDING_BOX_WIDTH).equals(0.0f))) {
                metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.0f);
                metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.0f);
                isMarker = true;
            }
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
