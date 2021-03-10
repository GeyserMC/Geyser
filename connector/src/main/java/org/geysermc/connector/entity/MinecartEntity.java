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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class MinecartEntity extends Entity {

    public MinecartEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position.add(0d, entityType.getOffset(), 0d), motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {

        if (entityMetadata.getId() == 7) {
            metadata.put(EntityData.HEALTH, entityMetadata.getValue());
        }

        // Direction in which the minecart is shaking
        if (entityMetadata.getId() == 8) {
            metadata.put(EntityData.HURT_DIRECTION, entityMetadata.getValue());
        }

        // Power in Java, time in Bedrock
        if (entityMetadata.getId() == 9) {
            metadata.put(EntityData.HURT_TIME, Math.min((int) (float) entityMetadata.getValue(), 15));
        }

        if (!(this instanceof DefaultBlockMinecartEntity)) { // Handled in the DefaultBlockMinecartEntity class
            // Custom block
            if (entityMetadata.getId() == 10) {
                metadata.put(EntityData.DISPLAY_ITEM, session.getBlockTranslator().getBedrockBlockId((int) entityMetadata.getValue()));
            }

            // Custom block offset
            if (entityMetadata.getId() == 11) {
                metadata.put(EntityData.DISPLAY_OFFSET, entityMetadata.getValue());
            }

            // If the custom block should be enabled
            if (entityMetadata.getId() == 12) {
                // Needs a byte based off of Java's boolean
                metadata.put(EntityData.CUSTOM_DISPLAY, (byte) ((boolean) entityMetadata.getValue() ? 1 : 0));
            }
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(session, position.add(0d, this.entityType.getOffset(), 0d), rotation, isOnGround, teleported);
    }
}
