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

package org.geysermc.geyser.entity.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

public class MinecartEntity extends Entity {

    public MinecartEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position.add(0d, definition.offset(), 0d), motion, yaw, pitch, headYaw);
    }

    public void setCustomBlock(IntEntityMetadata entityMetadata) {
        dirtyMetadata.put(EntityData.DISPLAY_ITEM, session.getBlockMappings().getBedrockBlockId(entityMetadata.getPrimitiveValue()));
    }

    public void setCustomBlockOffset(IntEntityMetadata entityMetadata) {
        dirtyMetadata.put(EntityData.DISPLAY_OFFSET, entityMetadata.getPrimitiveValue());
    }

    public void setShowCustomBlock(BooleanEntityMetadata entityMetadata) {
        // If the custom block should be enabled
        // Needs a byte based off of Java's boolean
        dirtyMetadata.put(EntityData.CUSTOM_DISPLAY, (byte) (entityMetadata.getPrimitiveValue() ? 1 : 0));
    }

    @Override
    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(position.add(0d, this.definition.offset(), 0d), yaw, pitch, headYaw, isOnGround, teleported);
    }

    @Override
    public Vector3f getBedrockRotation() {
        // Note: minecart rotation on rails does not care about the actual rotation value
        return Vector3f.from(0, yaw, 0);
    }
}
