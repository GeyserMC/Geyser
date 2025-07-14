/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponentMap;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.GeyserProvided;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.impl.GeyserCoreProvided;
import org.geysermc.geyser.item.custom.impl.DataComponentImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@EqualsAndHashCode
@ToString
public class GeyserCustomItemDefinition implements CustomItemDefinition {
    private final @NonNull Identifier bedrockIdentifier;
    private final @NonNull String displayName;
    private final @NonNull Identifier model;
    private final @NonNull String icon;
    private final @NonNull List<MinecraftPredicate<? super ItemPredicateContext>> predicates;
    private final PredicateStrategy predicateStrategy;
    private final int priority;
    private final @NonNull CustomItemBedrockOptions bedrockOptions;
    private final @NonNull DataComponentMap components;
    private final @NonNull List<Identifier> removedComponents;

    public GeyserCustomItemDefinition(Builder builder) {
        this.bedrockIdentifier = builder.bedrockIdentifier;
        this.displayName = builder.displayName;
        this.model = builder.model;

        String setIcon = builder.bedrockOptions.icon();
        icon = setIcon == null ? bedrockIdentifier().toString().replaceAll(":", ".").replaceAll("/", "_") : setIcon;

        this.predicates = List.copyOf(builder.predicates);
        this.predicateStrategy = builder.predicateStrategy;
        this.priority = builder.priority;
        this.bedrockOptions = builder.bedrockOptions;
        this.components = new ComponentMap(builder.components);
        this.removedComponents = builder.removedComponents;
    }

    @Override
    public @NonNull Identifier bedrockIdentifier() {
        return bedrockIdentifier;
    }

    @Override
    public @NonNull String displayName() {
        return displayName;
    }

    @Override
    public @NonNull Identifier model() {
        return model;
    }

    @Override
    public @NonNull String icon() {
        return icon;
    }

    @Override
    public @NonNull List<MinecraftPredicate<? super ItemPredicateContext>> predicates() {
        return predicates;
    }

    @Override
    @NonNull
    public PredicateStrategy predicateStrategy() {
        return predicateStrategy;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public @NonNull CustomItemBedrockOptions bedrockOptions() {
        return bedrockOptions;
    }

    @Override
    public @NonNull DataComponentMap components() {
        return components;
    }

    @Override
    public @NonNull List<Identifier> removedComponents() {
        return removedComponents;
    }

    public static class Builder implements CustomItemDefinition.Builder {
        private final Identifier bedrockIdentifier;
        private final Identifier model;
        private final List<MinecraftPredicate<? super ItemPredicateContext>> predicates = new ArrayList<>();
        private final Reference2ObjectMap<DataComponent<?>, Object> components = new Reference2ObjectOpenHashMap<>();
        private final List<Identifier> removedComponents = new ArrayList<>();

        private String displayName;
        private int priority = 0;
        private CustomItemBedrockOptions bedrockOptions = CustomItemBedrockOptions.builder().build();
        private PredicateStrategy predicateStrategy = PredicateStrategy.AND;

        public Builder(@NonNull Identifier bedrockIdentifier, @NonNull Identifier model) {
            Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier cannot be null");
            Objects.requireNonNull(model, "model cannot be null");
            this.bedrockIdentifier = bedrockIdentifier;
            this.displayName = bedrockIdentifier.toString();
            this.model = model;
        }

        @Override
        public CustomItemDefinition.Builder displayName(@NonNull String displayName) {
            Objects.requireNonNull(displayName, "displayName cannot be null");
            this.displayName = displayName;
            return this;
        }

        @Override
        public CustomItemDefinition.Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public CustomItemDefinition.Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options) {
            Objects.requireNonNull(options, "options cannot be null");
            this.bedrockOptions = options.build();
            return this;
        }

        @Override
        public CustomItemDefinition.Builder predicate(@NonNull MinecraftPredicate<? super ItemPredicateContext> predicate) {
            Objects.requireNonNull(predicate, "predicate cannot be null");

            if (predicate instanceof GeyserProvided && !(predicate instanceof GeyserCoreProvided)) {
                throw new IllegalArgumentException("found custom implementation (%s) of Geyser-provided predicate! Use the predicate creators provided in the api instead.".formatted(predicate.getClass().getSimpleName()));
            }

            predicates.add(predicate);
            return this;
        }

        @Override
        public CustomItemDefinition.Builder predicateStrategy(@NonNull PredicateStrategy strategy) {
            Objects.requireNonNull(strategy, "strategy cannot be null");
            predicateStrategy = strategy;
            return this;
        }

        @Override
        public <T> CustomItemDefinition.Builder component(@NonNull DataComponent<T> component, @NonNull T value) {
            Objects.requireNonNull(component, "component cannot be null");
            Objects.requireNonNull(value, "value cannot be null");
            if (!(component instanceof DataComponentImpl<T> dataComponent)) {
                throw new IllegalArgumentException("Cannot use custom implementations of the DataComponent<T> interface! Found: " + component.getClass().getSimpleName());
            } else if (removedComponents.contains(component.identifier())) {
                throw new IllegalArgumentException("Tried to add earlier removed component " + component.identifier());
            }

            if (!component.vanilla() && !(this instanceof GeyserNonVanillaCustomItemDefinition.Builder)) {
                throw new IllegalArgumentException("That component cannot be used for vanilla items");
            } else if (!dataComponent.validate(value)) {
                throw new IllegalArgumentException("Value " + value + " is invalid for " + component);
            }
            components.put(component, value);
            return this;
        }

        @Override
        public CustomItemDefinition.Builder removeComponent(@NonNull Identifier component) {
            Objects.requireNonNull(component, "component cannot be null");
            if (components.keySet().stream().map(DataComponent::identifier).anyMatch(identifier -> identifier.equals(component))) {
                throw new IllegalArgumentException("Tried to remove earlier added component " + component);
            }
            removedComponents.add(component);
            return this;
        }

        @Override
        public CustomItemDefinition build() {
            return new GeyserCustomItemDefinition(this);
        }
    }

    private record ComponentMap(Reference2ObjectMap<DataComponent<?>, Object> components) implements DataComponentMap {

        @Override
        public <T> T get(DataComponent<T> type) {
            return (T) components.get(type);
        }

        @Override
        public Set<DataComponent<?>> keySet() {
            return components.keySet();
        }
    }
}
