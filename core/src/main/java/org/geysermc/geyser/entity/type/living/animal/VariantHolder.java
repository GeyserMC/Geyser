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

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

import java.util.Locale;

/**
 * Interface to help set up data-driven entity variants for mobs.
 *
 * <p>Data-driven variants are sent as an int ID of their variant registry by Java, but can be a metadata ID or entity property on bedrock.
 * This interface helps translate data-driven variants to built-in bedrock ones.</p>
 *
 * <p>Implementations usually have to implement {@link VariantHolder#variantRegistry()} and {@link VariantHolder#setBedrockVariant(BuiltIn)}, and should also
 * have an enum with built-in variants on bedrock (implementing {@link BuiltIn}).</p>
 *
 * @param <BedrockVariant> the enum of Bedrock variants.
 */
public interface VariantHolder<BedrockVariant extends VariantHolder.BuiltIn> {

    default void setVariant(IntEntityMetadata variant) {
        setVariantFromJavaId(variant.getPrimitiveValue());
    }

    /**
     * Sets the variant of the entity.
     */
    default void setVariantFromJavaId(int variant) {
        setBedrockVariant(variantRegistry().fromNetworkId(getSession(), variant));
    }

    GeyserSession getSession();

    /**
     * The registry in {@link org.geysermc.geyser.session.cache.registry.JavaRegistries} for this mob's variants. The registry can utilise the {@link VariantHolder#reader(Class, Enum)} method
     * to create a reader to be used in {@link org.geysermc.geyser.session.cache.RegistryCache}.
     */
    JavaRegistryKey<? extends BedrockVariant> variantRegistry();

    /**
     * Should set the variant for bedrock.
     */
    void setBedrockVariant(BedrockVariant bedrockVariant);

    /**
     * Creates a registry reader for this mob's variants.
     *
     * <p>This reader simply matches the identifiers of registry entries with built-in variants. If no built-in variant matches, the fallback/default is returned.</p>
     */
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

    /**
     * Should be implemented on an enum within the entity class. The enum lists vanilla variants that can appear on bedrock.
     *
     * <p>The enum constants should be named the same as their Java identifiers.</p>
     */
    interface BuiltIn {

        String name();

        default Key javaIdentifier() {
            return MinecraftKey.key(name().toLowerCase(Locale.ROOT));
        }
    }
}
