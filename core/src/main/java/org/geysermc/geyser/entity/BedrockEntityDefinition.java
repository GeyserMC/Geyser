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

package org.geysermc.geyser.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.registry.Registries;

import java.util.List;
import java.util.Objects;

@Getter
@Accessors(fluent = true)
@ToString
public class BedrockEntityDefinition implements GeyserEntityDefinition, CustomEntityDefinition {
    private final @NonNull Identifier identifier;
    private final @NonNull GeyserEntityProperties registeredProperties;

    public BedrockEntityDefinition(@NonNull Identifier identifier, @NonNull GeyserEntityProperties registeredProperties) {
        this.identifier = identifier;
        this.registeredProperties = registeredProperties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BedrockEntityDefinition ofVanilla(Identifier identifier) {
        return builder().identifier(identifier).build();
    }

    public static BedrockEntityDefinition getOrCreate(@NonNull Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        if (Registries.BEDROCK_ENTITY_DEFINITIONS.get().containsKey(identifier)) {
            return Registries.BEDROCK_ENTITY_DEFINITIONS.get().get(identifier);
        }

        Objects.requireNonNull(identifier, "identifier");
        if (identifier.vanilla()) {
            throw new IllegalArgumentException("Cannot create custom entity in vanilla namespace! " + identifier);
        }
        return builder().identifier(identifier).build();
    }

    @Override
    public List<GeyserEntityProperty<?>> properties() {
        if (registeredProperties.isEmpty()) {
            return List.of();
        }
        return List.copyOf(registeredProperties.getProperties());
    }

    @Override
    public boolean vanilla() {
        return identifier.vanilla();
    }

    @Override
    public boolean registered() {
        return Registries.BEDROCK_ENTITY_DEFINITIONS.get().containsKey(identifier);
    }

    public static class Builder {
        private Identifier identifier;
        @Setter(AccessLevel.NONE)
        protected GeyserEntityProperties.Builder propertiesBuilder;

        public Builder() {
        }

        public Builder identifier(Identifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder properties(GeyserEntityProperties.@Nullable Builder propertiesBuilder) {
            this.propertiesBuilder = propertiesBuilder;
            return this;
        }

        BedrockEntityDefinition build() {
            return new BedrockEntityDefinition(identifier, propertiesBuilder != null ? propertiesBuilder.build() : new GeyserEntityProperties());
        }
    }
}
