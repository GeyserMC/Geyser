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

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * Used as a class for any projectile entity that looks like an item
 */
public class ThrowableItemEntity extends ThrowableEntity {
    /**
     * Number of ticks since the entity was spawned by the Java server
     */
    private int age;
    private boolean invisible;

    public ThrowableItemEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        metadata.getFlags().setFlag(EntityFlag.INVISIBLE, true);
        invisible = false;
    }

    private void checkVisibility(GeyserSession session) {
        if (invisible != metadata.getFlags().getFlag(EntityFlag.INVISIBLE)) {
            if (!invisible) {
                Vector3f playerPos = session.getPlayerEntity().getPosition();
                if (age >= 4 || position.distanceSquared(playerPos.getX(), playerPos.getY(), playerPos.getZ()) > 16) {
                    metadata.getFlags().setFlag(EntityFlag.INVISIBLE, false);
                    updateBedrockMetadata(session);
                }
            } else {
                metadata.getFlags().setFlag(EntityFlag.INVISIBLE, true);
                updateBedrockMetadata(session);
            }
        }
    }

    @Override
    public void tick(GeyserSession session) {
        checkVisibility(session);
        super.tick(session);
        age++;
    }

    @Override
    protected void setInvisible(GeyserSession session, boolean value) {
        invisible = value;
    }
}
