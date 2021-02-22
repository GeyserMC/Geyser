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
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class AbstractArrowEntity extends Entity {

    public AbstractArrowEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        setMotion(motion);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 7) {
            byte data = (byte) entityMetadata.getValue();

            metadata.getFlags().setFlag(EntityFlag.CRITICAL, (data & 0x01) == 0x01);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void setRotation(Vector3f rotation) {
        // Ignore the rotation sent by the Java server since the
        // Java client calculates the rotation from the motion
    }

    @Override
    public void setMotion(Vector3f motion) {
        super.setMotion(motion);

        double horizontalSpeed = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());
        float yaw = (float) Math.toDegrees(Math.atan2(motion.getX(), motion.getZ()));
        float pitch = (float) Math.toDegrees(Math.atan2(motion.getY(), horizontalSpeed));
        rotation = Vector3f.from(yaw, pitch, yaw);
    }
}
