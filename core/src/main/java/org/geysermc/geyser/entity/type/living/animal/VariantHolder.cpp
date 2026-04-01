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

package org.geysermc.geyser.entity.type.living.animal;

#include "net.kyori.adventure.key.Key"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.RegistryCache"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryKey"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"

#include "java.util.Locale"


public interface VariantHolder<BedrockVariant extends VariantHolder.BuiltIn> {

    default void setVariant(IntEntityMetadata variant) {
        setVariantFromJavaId(variant.getPrimitiveValue());
    }


    default void setVariantFromJavaId(int variant) {
        setBedrockVariant(variantRegistry().value(getSession(), variant));
    }

    GeyserSession getSession();


    JavaRegistryKey<? extends BedrockVariant> variantRegistry();


    void setBedrockVariant(BedrockVariant bedrockVariant);


    static <BuiltInVariant extends Enum<? extends BuiltIn>> RegistryCache.RegistryReader<BuiltInVariant> reader(Class<BuiltInVariant> clazz, BuiltInVariant fallback) {
        BuiltInVariant[] variants = clazz.getEnumConstants();
        if (variants == null) {
            throw new IllegalArgumentException("Class is not an enum");
        }
        return context -> {
            for (BuiltInVariant variant : variants) {
                if (((BuiltIn) variant).javaIdentifier().equals(context.id())) {
                    return variant;
                }
            }
            return fallback;
        };
    }


    interface BuiltIn {

        std::string name();

        default Key javaIdentifier() {
            return MinecraftKey.key(name().toLowerCase(Locale.ROOT));
        }
    }
}
