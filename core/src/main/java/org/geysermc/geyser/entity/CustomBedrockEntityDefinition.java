/*
 * Copyright (c) 2025-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.registry.Registries;

import java.util.Objects;

public final class CustomBedrockEntityDefinition extends BedrockEntityDefinition implements CustomEntityDefinition {

    public CustomBedrockEntityDefinition(@NonNull Identifier identifier, @NonNull GeyserEntityProperties registeredProperties) {
        super(identifier, registeredProperties);
    }

    /**
     * Retrieves an existing registered entity definition by identifier, or creates a new unregistered
     * {@link CustomBedrockEntityDefinition} for the given identifier.
     * Custom identifiers must not use the "minecraft" namespace.
     */
    public static CustomBedrockEntityDefinition getOrCreate(@NonNull Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        BedrockEntityDefinition existing = Registries.BEDROCK_ENTITY_DEFINITIONS.get().get(identifier);
        if (existing instanceof CustomBedrockEntityDefinition custom) {
            return custom;
        }
        if (identifier.vanilla()) {
            throw new IllegalArgumentException("Cannot create custom entity in vanilla namespace! " + identifier);
        }
        return new CustomBedrockEntityDefinition(identifier, new GeyserEntityProperties());
    }

    @Override
    public boolean vanilla() {
        return false;
    }

    @Override
    public String toString() {
        return "CustomBedrockEntityDefinition[identifier=" + identifier() + ", registeredProperties=" + registeredProperties() + "]";
    }
}
