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

/**
 * This class is used as a base for minecarts with a default block to display like furnaces and spawners
 */
public class DefaultBlockMinecartEntity extends MinecartEntity {

    public int customBlock = 0;
    public int customBlockOffset = 0;
    public boolean showCustomBlock = false;

    public DefaultBlockMinecartEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        metadata.put(EntityData.CUSTOM_DISPLAY, (byte) 1);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        updateDefaultBlockMetadata(session);
        super.spawnEntity(session);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {

        // Custom block
        if (entityMetadata.getId() == 10) {
            customBlock = (int) entityMetadata.getValue();

            if (showCustomBlock) {
                metadata.put(EntityData.DISPLAY_ITEM, session.getBlockTranslator().getBedrockBlockId(customBlock));
            }
        }

        // Custom block offset
        if (entityMetadata.getId() == 11) {
            customBlockOffset = (int) entityMetadata.getValue();

            if (showCustomBlock) {
                metadata.put(EntityData.DISPLAY_OFFSET, customBlockOffset);
            }
        }

        // If the custom block should be enabled
        if (entityMetadata.getId() == 12) {
            if ((boolean) entityMetadata.getValue()) {
                showCustomBlock = true;
                metadata.put(EntityData.DISPLAY_ITEM, session.getBlockTranslator().getBedrockBlockId(customBlock));
                metadata.put(EntityData.DISPLAY_OFFSET, customBlockOffset);
            } else {
                showCustomBlock = false;
                updateDefaultBlockMetadata(session);
            }
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    public void updateDefaultBlockMetadata(GeyserSession session) { }
}
