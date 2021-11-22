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

package org.geysermc.geyser.entity.type.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.living.GolemEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class ShulkerEntity extends GolemEntity {

    public ShulkerEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
        // Indicate that invisibility should be fixed through the resource pack
        setFlag(EntityFlag.BRIBED, true);
    }

    public void setAttachedFace(EntityMetadata<Direction, ?> entityMetadata) {
        Direction direction = entityMetadata.getValue();
        dirtyMetadata.put(EntityData.SHULKER_ATTACH_FACE, (byte) direction.ordinal());
    }

    public void setShulkerHeight(ByteEntityMetadata entityMetadata) {
        int height = entityMetadata.getPrimitiveValue();
        dirtyMetadata.put(EntityData.SHULKER_PEEK_ID, height);
    }

    public void setShulkerColor(ByteEntityMetadata entityMetadata) {
        byte color = ((ByteEntityMetadata) entityMetadata).getPrimitiveValue();
        if (color == 16) {
            // 16 is default on both editions
            dirtyMetadata.put(EntityData.VARIANT, 16);
        } else {
            // Every other shulker color is offset 15 in bedrock edition
            dirtyMetadata.put(EntityData.VARIANT, Math.abs(color - 15));
        }
    }
}
