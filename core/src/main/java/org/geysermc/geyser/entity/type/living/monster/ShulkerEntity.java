/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.monster;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.GolemEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;

public class ShulkerEntity extends GolemEntity {

    public ShulkerEntity(EntitySpawnContext context) {
        super(context);
        
        setFlag(EntityFlag.BRIBED, true);
    }

    @Override
    public boolean shouldLerp() {
        return false;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        
        
        dirtyMetadata.put(EntityDataTypes.VARIANT, 16);

        setFlag(EntityFlag.COLLIDABLE, true);

        
        
        setFlag(EntityFlag.RENDER_WHEN_INVISIBLE, true);
    }

    public void setAttachedFace(EntityMetadata<Direction, ?> entityMetadata) {
        Direction direction = entityMetadata.getValue();
        dirtyMetadata.put(EntityDataTypes.SHULKER_ATTACH_FACE, direction.ordinal());
    }

    public void setShulkerHeight(ByteEntityMetadata entityMetadata) {
        int height = entityMetadata.getPrimitiveValue();
        dirtyMetadata.put(EntityDataTypes.SHULKER_PEEK_AMOUNT, height);
    }

    public void setShulkerColor(ByteEntityMetadata entityMetadata) {
        byte color = entityMetadata.getPrimitiveValue();
        if (color == 16) {
            
            dirtyMetadata.put(EntityDataTypes.VARIANT, 16);
        } else {
            
            dirtyMetadata.put(EntityDataTypes.VARIANT, Math.abs(color - 15));
        }
    }

    @Override
    protected boolean isEnemy() {
        return true;
    }
}
