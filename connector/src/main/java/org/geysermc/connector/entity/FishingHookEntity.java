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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.object.ProjectileData;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class FishingHookEntity extends Entity {
    public FishingHookEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation, ProjectileData data) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        for (GeyserSession session : GeyserConnector.getInstance().getPlayers().values()) {
            Entity entity = session.getEntityCache().getEntityByJavaId(data.getOwnerId());
            if (entity == null && session.getPlayerEntity().getEntityId() == data.getOwnerId()) {
                entity = session.getPlayerEntity();
            }

            if (entity != null) {
                this.metadata.put(EntityData.OWNER_EID, entity.getGeyserId());
                return;
            }
        }
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 7) {
            Entity entity = session.getEntityCache().getEntityByJavaId((Integer) entityMetadata.getValue() - 1);
            if (entity == null && session.getPlayerEntity().getEntityId() == (Integer) entityMetadata.getValue() - 1) {
                entity = session.getPlayerEntity();
            }

            if (entity != null) {
                metadata.put(EntityData.TARGET_EID, entity.getGeyserId());
            }
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
