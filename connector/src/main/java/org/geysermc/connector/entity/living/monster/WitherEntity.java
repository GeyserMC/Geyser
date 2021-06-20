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

package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class WitherEntity extends MonsterEntity {

    public WitherEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        metadata.put(EntityData.WITHER_AERIAL_ATTACK, (short) 1);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        long targetID = 0;

        if (entityMetadata.getId() >= 16 && entityMetadata.getId() <= 18) {
            Entity entity = session.getEntityCache().getEntityByJavaId((int) entityMetadata.getValue());
            if (entity == null && session.getPlayerEntity().getEntityId() == (int) entityMetadata.getValue()) {
                entity = session.getPlayerEntity();
            }

            if (entity != null) {
                targetID = entity.getGeyserId();
            }
        }

        if (entityMetadata.getId() == 16) {
            metadata.put(EntityData.WITHER_TARGET_1, targetID);
        } else if (entityMetadata.getId() == 17) {
            metadata.put(EntityData.WITHER_TARGET_2, targetID);
        } else if (entityMetadata.getId() == 18) {
            metadata.put(EntityData.WITHER_TARGET_3, targetID);
        } else if (entityMetadata.getId() == 19) {
            metadata.put(EntityData.WITHER_INVULNERABLE_TICKS, entityMetadata.getValue());

            // Show the shield for the first few seconds of spawning (like Java)
            if ((int) entityMetadata.getValue() >= 165) {
                metadata.put(EntityData.WITHER_AERIAL_ATTACK, (short) 0);
            } else {
                metadata.put(EntityData.WITHER_AERIAL_ATTACK, (short) 1);
            }
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
