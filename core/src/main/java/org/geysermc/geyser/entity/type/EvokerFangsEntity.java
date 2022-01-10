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

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EvokerFangsEntity extends Entity implements Tickable {
    private int limitedLife = 22;
    private boolean attackStarted = false;

    public EvokerFangsEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
        // As of 1.18.2 Bedrock, this line is required for the entity to be visible
        // 22 is the starting number on Java Edition
        dirtyMetadata.put(EntityData.LIMITED_LIFE, this.limitedLife);
    }

    @Override
    public void tick() {
        if (attackStarted) {
            if (--this.limitedLife > 0 && this.limitedLife % 2 == 0) { // Matches Bedrock behavior
                dirtyMetadata.put(EntityData.LIMITED_LIFE, this.limitedLife);
                updateBedrockMetadata();
            }
        }
    }

    public void setAttackStarted() {
        this.attackStarted = true;
        if (!getFlag(EntityFlag.SILENT)) {
            // Play the chomp sound
            PlaySoundPacket packet = new PlaySoundPacket();
            packet.setPosition(this.position);
            packet.setSound("mob.evocation_fangs.attack");
            packet.setVolume(1.0f);
            packet.setPitch(ThreadLocalRandom.current().nextFloat() * 0.2f + 0.85f);
            session.sendUpstreamPacket(packet);
        }
    }
}
