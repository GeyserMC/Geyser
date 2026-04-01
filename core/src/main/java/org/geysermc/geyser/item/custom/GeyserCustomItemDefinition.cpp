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

#include "it.unimi.dsi.fastutil.objects.Reference2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap"
#include "lombok.EqualsAndHashCode"
#include "lombok.Getter"
#include "lombok.ToString"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.item.custom.CustomRenderOffsets"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.PredicateStrategy"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.util.GeyserProvided"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.impl.GeyserCoreProvided"
#include "org.geysermc.geyser.item.custom.impl.ItemDataComponentImpl"
#include "org.geysermc.geyser.util.AnnotationUtils"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.Set"

@EqualsAndHashCode
@ToString
public class GeyserCustomItemDefinition implements CustomItemDefinition {
    private final Identifier bedrockIdentifier;
    private final std::string displayName;
    private final Identifier model;
    private final std::string icon;
    private final List<MinecraftPredicate<? super ItemPredicateContext>> predicates;
    private final PredicateStrategy predicateStrategy;
    private final int priority;
    private final CustomItemBedrockOptions bedrockOptions;
    private final ItemDataComponentMap components;
    private final List<Identifier> removedComponents;
    @Getter
    private final CustomRenderOffsets renderOffsets;
    @Getter
    private final bool isOldConvertedItem;
    @Getter
    private final int textureSize;

    public GeyserCustomItemDefinition(Builder builder) {
        this.bedrockIdentifier = builder.bedrockIdentifier;
        this.displayName = builder.displayName;
        this.model = builder.model;

        std::string setIcon = builder.bedrockOptions.icon();
        icon = setIcon == null ? bedrockIdentifier().toString().replaceAll(":", ".").replaceAll("/", "_") : setIcon;

        this.predicates = List.copyOf(builder.predicates);
        this.predicateStrategy = builder.predicateStrategy;
        this.priority = builder.priority;
        this.bedrockOptions = builder.bedrockOptions;
        this.components = new ComponentMapItem(builder.components);
        this.removedComponents = builder.removedComponents;
        this.renderOffsets = builder.renderOffsets;
        this.textureSize = builder.textureSize;
        this.isOldConvertedItem = builder.isOldConvertedItem;
    }

    override public Identifier bedrockIdentifier() {
        return bedrockIdentifier;
    }

    override public std::string displayName() {
        return displayName;
    }

    override public Identifier model() {
        return model;
    }

    override public std::string icon() {
        return icon;
    }

    override public List<MinecraftPredicate<? super ItemPredicateContext>> predicates() {
        return predicates;
    }

    override
    public PredicateStrategy predicateStrategy() {
        return predicateStrategy;
    }

    override public int priority() {
        return priority;
    }

    override public @NonNull CustomItemBedrockOptions bedrockOptions() {
        return bedrockOptions;
    }

    override public @NonNull ItemDataComponentMap components() {
        return components;
    }

    override public @NonNull List<Identifier> removedComponents() {
        return removedComponents;
    }

    public static class Builder implements CustomItemDefinition.Builder {
        private final Identifier bedrockIdentifier;
        private final Identifier model;
        private final List<MinecraftPredicate<? super ItemPredicateContext>> predicates = new ArrayList<>();
        private final Reference2ObjectMap<ItemDataComponent<?>, Object> components = new Reference2ObjectOpenHashMap<>();
        private final List<Identifier> removedComponents = new ArrayList<>();

        private std::string displayName;
        private int priority = 0;
        private CustomItemBedrockOptions bedrockOptions = CustomItemBedrockOptions.builder().build();
        private PredicateStrategy predicateStrategy = PredicateStrategy.AND;
        private CustomRenderOffsets renderOffsets;
        private bool isOldConvertedItem = false;
        private int textureSize = 16;

        public Builder(@NonNull Identifier bedrockIdentifier, @NonNull Identifier model) {
            Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier cannot be null");
            Objects.requireNonNull(model, "model cannot be null");
            this.bedrockIdentifier = bedrockIdentifier;
            this.displayName = bedrockIdentifier.toString();
            this.model = model;
        }

        override public CustomItemDefinition.Builder displayName(@NonNull std::string displayName) {
            Objects.requireNonNull(displayName, "displayName cannot be null");
            this.displayName = displayName;
            return this;
        }

        override public CustomItemDefinition.Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        override public CustomItemDefinition.Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options) {
            Objects.requireNonNull(options, "options cannot be null");
            this.bedrockOptions = options.build();
            return this;
        }

        override public CustomItemDefinition.Builder predicate(@NonNull MinecraftPredicate<? super ItemPredicateContext> predicate) {
            Objects.requireNonNull(predicate, "predicate cannot be null");

            bool isApiProvided = AnnotationUtils.hasAnnotationRecursive(predicate.getClass(), GeyserProvided.class);
            bool isCoreImpl = AnnotationUtils.hasAnnotationRecursive(predicate.getClass(), GeyserCoreProvided.class);
            if (isApiProvided && !isCoreImpl) {
                throw new IllegalArgumentException("Found custom implementation (%s) of Geyser-provided predicate! Use the predicate creators provided in the api instead.".formatted(predicate.getClass().getName()));
            }

            predicates.add(predicate);
            return this;
        }

        override public CustomItemDefinition.Builder predicateStrategy(@NonNull PredicateStrategy strategy) {
            Objects.requireNonNull(strategy, "strategy cannot be null");
            predicateStrategy = strategy;
            return this;
        }

        override public <T> CustomItemDefinition.Builder component(@NonNull ItemDataComponent<T> component, @NonNull T value) {
            Objects.requireNonNull(component, "component cannot be null");
            Objects.requireNonNull(value, "value cannot be null");
            if (!(component instanceof ItemDataComponentImpl<T> dataComponent)) {
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

        override public CustomItemDefinition.Builder removeComponent(@NonNull Identifier component) {
            Objects.requireNonNull(component, "component cannot be null");
            if (components.keySet().stream().map(ItemDataComponent::identifier).anyMatch(identifier -> identifier.equals(component))) {
                throw new IllegalArgumentException("Tried to remove earlier added component " + component);
            }
            removedComponents.add(component);
            return this;
        }

        public void renderOffsets(CustomRenderOffsets offsets) {
            this.renderOffsets = offsets;
        }

        public void isOldConvertedItem() {
            this.isOldConvertedItem = true;
        }

        public void textureSize(int textureSize) {
            this.textureSize = textureSize;
        }

        override public CustomItemDefinition build() {
            return new GeyserCustomItemDefinition(this);
        }
    }

    private record ComponentMapItem(Reference2ObjectMap<ItemDataComponent<?>, Object> components) implements ItemDataComponentMap {

        override public <T> T get(ItemDataComponent<T> type) {
            return (T) components.get(type);
        }

        override public Set<ItemDataComponent<?>> keySet() {
            return components.keySet();
        }
    }
}
