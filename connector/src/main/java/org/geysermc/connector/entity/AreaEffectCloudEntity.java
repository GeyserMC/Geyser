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
import com.github.steveice10.mc.protocol.data.game.world.particle.Particle;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.effect.EffectRegistry;

public class AreaEffectCloudEntity extends Entity {

    public AreaEffectCloudEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        // Without this the cloud doesn't appear,
        metadata.put(EntityData.AREA_EFFECT_CLOUD_DURATION, 600);

        // This disabled client side shrink of the cloud
        metadata.put(EntityData.AREA_EFFECT_CLOUD_RADIUS, 0.0f);
        metadata.put(EntityData.AREA_EFFECT_CLOUD_CHANGE_RATE, -0.005f);
        metadata.put(EntityData.AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP, -0.5f);

        metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 8) {
            metadata.put(EntityData.AREA_EFFECT_CLOUD_RADIUS, entityMetadata.getValue());
            metadata.put(EntityData.BOUNDING_BOX_WIDTH, 2.0f * (float) entityMetadata.getValue());
        } else if (entityMetadata.getId() == 9) {
            metadata.put(EntityData.EFFECT_COLOR, entityMetadata.getValue());
        } else if (entityMetadata.getId() == 11) {
            Particle particle = (Particle) entityMetadata.getValue();
            int particleId = EffectRegistry.getParticleId(session, particle.getType());
            if (particleId != -1) {
                metadata.put(EntityData.AREA_EFFECT_CLOUD_PARTICLE_ID, particleId);
            }
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
