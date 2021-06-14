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
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TNTEntity extends Entity {

    private int currentTick;

    public TNTEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 8) {
            currentTick = (int) entityMetadata.getValue();
            metadata.getFlags().setFlag(EntityFlag.IGNITED, true);
            metadata.put(EntityData.FUSE_LENGTH, currentTick);
            ScheduledFuture<?> future = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(() -> {
                if (currentTick % 5 == 0) {
                    metadata.put(EntityData.FUSE_LENGTH, currentTick);
                }
                currentTick--;
                super.updateBedrockMetadata(entityMetadata, session);
            }, 50, 50, TimeUnit.MILLISECONDS); // 5 ticks
            session.getConnector().getGeneralThreadPool().schedule(() -> future.cancel(true), (int) entityMetadata.getValue() / 20, TimeUnit.SECONDS);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
