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

package org.geysermc.connector.entity.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.UUID;

/**
 * The entity class specifically for a {@link GeyserSession}'s player.
 */
public class SessionPlayerEntity extends PlayerEntity {
    /**
     * Whether to check for updated speed after all entity metadata has been processed
     */
    private boolean refreshSpeed = false;

    private final GeyserSession session;

    public SessionPlayerEntity(GeyserSession session) {
        super(new GameProfile(UUID.randomUUID(), "unknown"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        valid = true;
        this.session = session;
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        // Already logged in
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        session.getCollisionManager().updatePlayerBoundingBox(position);
        super.moveAbsolute(session, position, rotation, isOnGround, teleported);
    }

    @Override
    public void setPosition(Vector3f position) {
        if (session != null) { // null during entity initialization
            session.getCollisionManager().updatePlayerBoundingBox(position);
        }
        super.setPosition(position);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);
        if (entityMetadata.getId() == 0) {
            session.setSwimmingInWater((((byte) entityMetadata.getValue()) & 0x10) == 0x10 && metadata.getFlags().getFlag(EntityFlag.SPRINTING));
            refreshSpeed = true;
        } else if (entityMetadata.getId() == 6) {
            session.setPose((Pose) entityMetadata.getValue());
            refreshSpeed = true;
        }
    }

    @Override
    public void updateBedrockMetadata(GeyserSession session) {
        super.updateBedrockMetadata(session);
        if (refreshSpeed) {
            if (session.adjustSpeed()) {
                updateBedrockAttributes(session);
            }
            refreshSpeed = false;
        }
    }
}
