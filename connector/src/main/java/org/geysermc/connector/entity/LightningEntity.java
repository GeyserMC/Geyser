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

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.ThreadLocalRandom;

public class LightningEntity extends Entity {

    public LightningEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        super.spawnEntity(session);

        // Add these two sound effects - they're done completely clientside on Java Edition as of 1.17.1
        ThreadLocalRandom random = ThreadLocalRandom.current();

        PlaySoundPacket thunderPacket = new PlaySoundPacket();
        thunderPacket.setPosition(this.position);
        thunderPacket.setSound("ambient.weather.thunder");
        thunderPacket.setPitch(0.8f + random.nextFloat() * 0.2f);
        thunderPacket.setVolume(10000f); // Really.
        session.sendUpstreamPacket(thunderPacket);

        PlaySoundPacket impactPacket = new PlaySoundPacket();
        impactPacket.setPosition(this.position);
        impactPacket.setSound("ambient.weather.lightning.impact");
        impactPacket.setPitch(0.5f + random.nextFloat() * 0.2f);
        impactPacket.setVolume(2.0f);
        session.sendUpstreamPacket(impactPacket);
    }
}
