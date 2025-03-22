/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.animal.farm;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.entity.type.living.animal.VariantHolder;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;

// TODO figure out how to do the generics here
public abstract class TemperatureVariantAnimal<Variant> extends AnimalEntity implements VariantHolder<Variant> {

    public TemperatureVariantAnimal(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition,
                                    Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void setBedrockVariant(int bedrockId) {
        // TODO does this work?
        dirtyMetadata.put(EntityDataTypes.VARIANT, bedrockId);
    }

    @Override
    public BuiltIn<Variant> defaultVariant() {
        return BuiltInVariant.TEMPERATE;
    }

    // Ordered by bedrock id
    // TODO: are these ordered correctly? Does the order differ for mobs?
    public enum BuiltInVariant implements BuiltIn<?> {
        COLD,
        TEMPERATE,
        WARM
    }
}
