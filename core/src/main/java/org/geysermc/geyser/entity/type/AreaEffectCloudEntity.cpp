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

#include "org.cloudburstmc.protocol.bedrock.data.ParticleType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.ColorParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle"

public class AreaEffectCloudEntity extends Entity {

    public AreaEffectCloudEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();

        dirtyMetadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_DURATION, Integer.MAX_VALUE);


        dirtyMetadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_RADIUS, 3.0f);
        dirtyMetadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP, Float.MIN_VALUE);


        dirtyMetadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_CHANGE_RATE, Float.MIN_VALUE);
        setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    public void setRadius(FloatEntityMetadata entityMetadata) {

        float value = MathUtils.clamp(entityMetadata.getPrimitiveValue(), 0.5f, 32.0f);
        dirtyMetadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_RADIUS, value);
        dirtyMetadata.put(EntityDataTypes.WIDTH, 2.0f * value);
    }

    public void setParticle(EntityMetadata<Particle, ?> entityMetadata) {
        Particle particle = entityMetadata.getValue();
        Registries.PARTICLES.map(particle.getType(), p -> p.levelEventType() instanceof ParticleType particleType ? particleType : null).ifPresent(type ->
                dirtyMetadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_PARTICLE, type));

        if (particle.getData() instanceof ColorParticleData effectParticleData) {
            dirtyMetadata.put(EntityDataTypes.EFFECT_COLOR, effectParticleData.getColor());
        }
    }
}
