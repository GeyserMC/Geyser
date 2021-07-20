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

package org.geysermc.connector.entity.living;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.Tickable;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

public class SquidEntity extends WaterEntity implements Tickable {

    private float pitch;
    private float yaw;

    private float targetPitch;
    private float targetYaw;

    private boolean inWater;

    public SquidEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        this.yaw = rotation.getX();
    }

    @Override
    public void tick(GeyserSession session) {
        if (inWater) {
            pitch += (targetPitch - pitch) * 0.1f;
            yaw += (targetYaw - yaw) * 0.1f;
        } else {
            pitch += (-90 - pitch) * 0.02f;
        }
        super.moveAbsolute(session, position, Vector3f.from(yaw, 0, yaw), onGround, false);
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        super.moveRelative(session, relX, relY, relZ, rotation, isOnGround);
        checkInWater(session);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(session, position, rotation, isOnGround, teleported);
        checkInWater(session);
    }

    @Override
    public void setRotation(Vector3f rotation) {
        // Let the Java server control yaw when the squid is out of water
        if (!inWater) {
            yaw = rotation.getX();
        }
    }

    @Override
    public void setMotion(Vector3f motion) {
        super.setMotion(motion);

        double horizontalSpeed = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());
        targetPitch = (float) Math.toDegrees(-Math.atan2(horizontalSpeed, motion.getY()));
        targetYaw = (float) Math.toDegrees(-Math.atan2(motion.getX(), motion.getZ()));
    }

    @Override
    public Vector3f getBedrockRotation() {
        return Vector3f.from(pitch, yaw, yaw);
    }

    private void checkInWater(GeyserSession session) {
        if (getMetadata().getFlags().getFlag(EntityFlag.RIDING)) {
            inWater = false;
        } else {
            int block = session.getConnector().getWorldManager().getBlockAt(session, position.toInt());
            inWater = BlockStateValues.getWaterLevel(block) != -1;
        }
    }
}
