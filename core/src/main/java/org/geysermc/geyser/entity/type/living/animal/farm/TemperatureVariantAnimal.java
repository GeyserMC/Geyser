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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public abstract class TemperatureVariantAnimal<Variant> extends AnimalEntity {

    public TemperatureVariantAnimal(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition,
                                    Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    protected abstract JavaRegistryKey<BuiltInVariant> variantRegistry();

    public void setVariant(EntityMetadata<Holder<Variant>, ? extends MetadataType<Holder<Variant>>> variant) {
        BuiltInVariant animalVariant;
        if (variant.getValue().isId()) {
            animalVariant = variantRegistry().fromNetworkId(session, variant.getValue().id());
            if (animalVariant == null) {
                animalVariant = BuiltInVariant.TEMPERATE;
            }
        } else {
            animalVariant = BuiltInVariant.TEMPERATE;
        }
        dirtyMetadata.put(EntityDataTypes.VARIANT, animalVariant.ordinal());
    }

    public enum BuiltInVariant {
        COLD,
        TEMPERATE,
        WARM;

        public static final Function<RegistryEntryContext, BuiltInVariant> READER = context -> getByJavaIdentifier(context.id());

        private final Key javaIdentifier;

        BuiltInVariant() {
            javaIdentifier = MinecraftKey.key(name().toLowerCase(Locale.ROOT));
        }

        public static @Nullable BuiltInVariant getByJavaIdentifier(Key identifier) {
            for (BuiltInVariant variant : values()) {
                if (variant.javaIdentifier.equals(identifier)) {
                    return variant;
                }
            }
            return null;
        }
    }
}
