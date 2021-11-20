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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.level.particle.Particle;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.Registries;

import java.util.UUID;

public class AreaEffectCloudEntity extends Entity {

    public AreaEffectCloudEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Without this the cloud doesn't appear,
        dirtyMetadata.put(EntityData.AREA_EFFECT_CLOUD_DURATION, 600);

        // This disabled client side shrink of the cloud
        dirtyMetadata.put(EntityData.AREA_EFFECT_CLOUD_RADIUS, 0.0f);
        dirtyMetadata.put(EntityData.AREA_EFFECT_CLOUD_CHANGE_RATE, -0.005f);
        dirtyMetadata.put(EntityData.AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP, -0.5f);

        setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    public void setRadius(FloatEntityMetadata entityMetadata) {
        float value = entityMetadata.getPrimitiveValue();
        dirtyMetadata.put(EntityData.AREA_EFFECT_CLOUD_RADIUS, value);
        dirtyMetadata.put(EntityData.BOUNDING_BOX_WIDTH, 2.0f * value);
    }

    public void setParticle(EntityMetadata<Particle, ?> entityMetadata) {
        Particle particle = entityMetadata.getValue();
        int particleId = Registries.PARTICLES.map(particle.getType(), mapping -> mapping.getParticleId(this.session)).orElse(-1);
        if (particleId != -1) {
            dirtyMetadata.put(EntityData.AREA_EFFECT_CLOUD_PARTICLE_ID, particleId);
        }
    }
}
