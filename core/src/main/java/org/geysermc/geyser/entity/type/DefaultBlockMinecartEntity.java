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

package org.geysermc.geyser.entity.type;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

/**
 * This class is used as a base for minecarts with a default block to display like furnaces and spawners
 */
public class DefaultBlockMinecartEntity extends MinecartEntity {

    public int customBlock = 0;
    public int customBlockOffset = 0;
    public boolean showCustomBlock = false;

    public DefaultBlockMinecartEntity(EntitySpawnContext context) {
        super(context);

        dirtyMetadata.put(EntityDataTypes.CUSTOM_DISPLAY, (byte) 1);
    }

    @Override
    public void spawnEntity() {
        updateDefaultBlockMetadata();
        super.spawnEntity();
    }

    @Override
    public void setCustomBlock(IntEntityMetadata entityMetadata) {
        customBlock = entityMetadata.getPrimitiveValue();
        showCustomBlock = entityMetadata.getPrimitiveValue() != 0;

        if (showCustomBlock) {
            dirtyMetadata.put(EntityDataTypes.DISPLAY_BLOCK_STATE, session.getBlockMappings().getBedrockBlock(customBlock));
            dirtyMetadata.put(EntityDataTypes.DISPLAY_OFFSET, customBlockOffset);
        } else {
            updateDefaultBlockMetadata();
        }
    }

    @Override
    public void setCustomBlockOffset(IntEntityMetadata entityMetadata) {
        customBlockOffset = entityMetadata.getPrimitiveValue();

        if (showCustomBlock) {
            dirtyMetadata.put(EntityDataTypes.DISPLAY_OFFSET, customBlockOffset);
        }
    }

    public void updateDefaultBlockMetadata() {
    }
}
