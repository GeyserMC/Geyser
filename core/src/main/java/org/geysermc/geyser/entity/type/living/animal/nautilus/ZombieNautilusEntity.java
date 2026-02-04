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

package org.geysermc.geyser.entity.type.living.animal.nautilus;

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.animal.VariantHolder;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;

public class ZombieNautilusEntity extends AbstractNautilusEntity implements VariantHolder<ZombieNautilusEntity.BuiltInVariant> {

    public static final EnumProperty<BuiltInVariant> VARIANT_ENUM_PROPERTY = new EnumProperty<>(
        IdentifierImpl.of("variant"),
        BuiltInVariant.class,
        BuiltInVariant.DEFAULT
    );

    public static final RegistryCache.RegistryReader<BuiltInVariant> VARIANT_READER = VariantHolder.reader(BuiltInVariant.class, BuiltInVariant.DEFAULT);

    public ZombieNautilusEntity(EntitySpawnContext context) {
        super(context, 1.1f);
    }

    @Override
    public void setBedrockVariant(BuiltInVariant variant) {
        VARIANT_ENUM_PROPERTY.apply(propertyManager, variant);
        updateBedrockEntityProperties();
    }

    @Override
    public JavaRegistryKey<BuiltInVariant> variantRegistry() {
        return JavaRegistries.ZOMBIE_NAUTILUS_VARIANT;
    }

    public enum BuiltInVariant implements VariantHolder.BuiltIn {
        DEFAULT("temperate"),
        CORAL("warm");

        private final String javaId;

        BuiltInVariant(String javaId) {
            this.javaId = javaId;
        }

        @Override
        public Key javaIdentifier() {
            return MinecraftKey.key(javaId);
        }
    }
}
