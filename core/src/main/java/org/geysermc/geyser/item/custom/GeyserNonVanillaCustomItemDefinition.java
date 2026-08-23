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

package org.geysermc.geyser.item.custom;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;

@EqualsAndHashCode(callSuper = true)
@ToString
public class GeyserNonVanillaCustomItemDefinition extends GeyserCustomItemDefinition implements NonVanillaCustomItemDefinition {

    private final @NonNull Identifier identifier;
    private final int javaId;
    private final String translationString;

    public GeyserNonVanillaCustomItemDefinition(Builder builder) {
        super(builder);
        this.identifier = builder.identifier;
        this.javaId = builder.javaId;
        this.translationString = builder.translationString;
    }

    @Override
    public @NonNull Identifier identifier() {
        return identifier;
    }

    @Override
    public @NonNegative int javaId() {
        return javaId;
    }

    @Override
    public @Nullable String translationString() {
        return translationString;
    }

    public static class Builder extends GeyserCustomItemDefinition.Builder implements NonVanillaCustomItemDefinition.Builder {
        private final Identifier identifier;
        private final int javaId;

        private String translationString;

        public Builder(@NonNull Identifier identifier, @NonNull Identifier bedrockIdentifier, int javaId) {
            super(bedrockIdentifier, identifier); // TODO different models?
            this.identifier = identifier;
            this.javaId = javaId;
        }

        @Override
        public NonVanillaCustomItemDefinition.Builder displayName(@NonNull String displayName) {
            return (Builder) super.displayName(displayName);
        }

        @Override
        public NonVanillaCustomItemDefinition.Builder priority(int priority) {
            throw new IllegalArgumentException("Predicates are not supported for non-vanilla custom item definitions");
        }

        @Override
        public NonVanillaCustomItemDefinition.Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options) {
            return (Builder) super.bedrockOptions(options);
        }

        @Override
        public CustomItemDefinition.Builder predicate(@NonNull MinecraftPredicate<? super ItemPredicateContext> predicate) {
            throw new IllegalArgumentException("Predicates are not supported for non-vanilla custom item definitions");
        }

        @Override
        public CustomItemDefinition.Builder predicateStrategy(@NonNull PredicateStrategy strategy) {
            throw new IllegalArgumentException("Predicates are not supported for non-vanilla custom item definitions");
        }

        @Override
        public <T> NonVanillaCustomItemDefinition.Builder component(@NonNull ItemDataComponent<T> component, @NonNull T value) {
            return (Builder) super.component(component, value);
        }

        @Override
        public CustomItemDefinition.Builder removeComponent(@NonNull Identifier component) {
            throw new UnsupportedOperationException("Removing default item components is not supported for non-vanilla items");
        }

        @Override
        public NonVanillaCustomItemDefinition.Builder translationString(@Nullable String translationString) {
            this.translationString = translationString;
            return this;
        }

        @Override
        public NonVanillaCustomItemDefinition build() {
            return new GeyserNonVanillaCustomItemDefinition(this);
        }
    }
}
