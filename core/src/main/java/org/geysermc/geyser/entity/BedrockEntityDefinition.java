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
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.registry.Registries;

import java.util.List;
import java.util.Objects;

public class BedrockEntityDefinition implements GeyserEntityDefinition {

    private final Identifier identifier;
    private final GeyserEntityProperties registeredProperties;

    protected BedrockEntityDefinition(@NonNull Identifier identifier, @NonNull GeyserEntityProperties registeredProperties) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        this.registeredProperties = Objects.requireNonNull(registeredProperties, "registeredProperties");
    }

    public static Builder builder() {
        return new Builder();
    }

    static BedrockEntityDefinition ofVanilla(Identifier identifier) {
        BedrockEntityDefinition bedrockEntityDefinition = builder().identifier(identifier).build();
        Registries.BEDROCK_ENTITY_DEFINITIONS.register(identifier, bedrockEntityDefinition);
        return bedrockEntityDefinition;
    }

    public static BedrockEntityDefinition getVanilla(@NonNull Identifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        BedrockEntityDefinition existing = Registries.BEDROCK_ENTITY_DEFINITIONS.get().get(identifier);
        if (existing == null || existing instanceof CustomBedrockEntityDefinition) {
            throw new IllegalArgumentException("Unknown vanilla Bedrock entity definition: " + identifier);
        }
        return existing;
    }

    @Override
    public @NonNull Identifier identifier() {
        return identifier;
    }

    public @NonNull GeyserEntityProperties registeredProperties() {
        return registeredProperties;
    }

    @Override
    public @NonNull List<GeyserEntityProperty<?>> properties() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BedrockEntityDefinition that)) return false;
        return identifier.equals(that.identifier) && registeredProperties.equals(that.registeredProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, registeredProperties);
    }

    @Override
    public String toString() {
        return "BedrockEntityDefinition[identifier=" + identifier + ", registeredProperties=" + registeredProperties + "]";
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
            return new BedrockEntityDefinition(identifier, propertiesBuilder != null ?
                propertiesBuilder.build() : new GeyserEntityProperties());
        }
    }
}
