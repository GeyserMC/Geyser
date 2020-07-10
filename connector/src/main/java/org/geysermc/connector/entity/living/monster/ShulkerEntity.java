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

package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.connector.entity.living.GolemEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ShulkerEntity extends GolemEntity {

    public ShulkerEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 15) {
            BlockFace blockFace = (BlockFace) entityMetadata.getValue();
            metadata.put(EntityData.SHULKER_ATTACH_FACE, (byte) blockFace.ordinal());
        }
        if (entityMetadata.getId() == 16) {
            Position position = (Position) entityMetadata.getValue();
            if (position != null) {
                metadata.put(EntityData.SHULKER_ATTACH_POS, Vector3i.from(position.getX(), position.getY(), position.getZ()));
            }
        }
        //TODO Outdated metadata flag SHULKER_PEAK_HEIGHT
//        if (entityMetadata.getId() == 17) {
//            int height = (byte) entityMetadata.getValue();
//            metadata.put(EntityData.SHULKER_PEAK_HEIGHT, height);
//        }
        if (entityMetadata.getId() == 18) {
            int color = Math.abs((byte) entityMetadata.getValue() - 15);
            metadata.put(EntityData.VARIANT, color);
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
