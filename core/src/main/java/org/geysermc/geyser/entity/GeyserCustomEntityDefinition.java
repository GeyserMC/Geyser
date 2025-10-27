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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.entity.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.JavaEntityType;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.entity.EntitySpawnContext;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.properties.type.PropertyType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.entity.EntityMetadataTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GeyserCustomEntityDefinition<T extends Entity> extends EntityDefinition<T> implements CustomEntityDefinition {
    private final List<MinecraftPredicate<? super EntitySpawnContext>> predicates;
    private final PredicateStrategy predicateStrategy;

    public GeyserCustomEntityDefinition(EntityFactory<T> factory, String bedrockIdentifier, List<MinecraftPredicate<? super EntitySpawnContext>> predicates, PredicateStrategy predicateStrategy,
                                        float width, float height, float offset, GeyserEntityProperties registeredProperties, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
        super(factory, bedrockIdentifier, width, height, offset, registeredProperties, translators);
        this.predicates = predicates;
        this.predicateStrategy = predicateStrategy;
    }

    @Override
    public boolean is(BuiltinEntityType type) {
        return false;
    }

    public static Builder<?> inherited(String bedrockIdentifier, JavaEntityType vanillaType) {
        if (!vanillaType.vanilla()) {
            throw new IllegalArgumentException("vanillaType must be a vanilla entity type, was: " + vanillaType);
        }
        VanillaEntityDefinition<?> parent = Registries.ENTITY_DEFINITIONS.get(vanillaType);
        if (parent == null) {
            throw new IllegalArgumentException("No vanilla entity definition registered for vanilla entity type " + vanillaType);
        }
        // TODO fix the rawtypes/unchecked
        return new Builder<>(bedrockIdentifier, parent.factory(), parent.width(), parent.height(), parent.offset(), new ObjectArrayList(parent.translators()));
    }

    public static class Builder<T extends Entity> extends EntityDefinition.Builder<T> implements CustomEntityDefinition.Builder {
        protected List<MinecraftPredicate<? super EntitySpawnContext>> predicates;
        protected PredicateStrategy predicateStrategy = PredicateStrategy.AND;

        protected Builder(EntityFactory<T> factory, String bedrockIdentifier) {
            super(factory);
            this.bedrockIdentifier = Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier must not be null");
        }

        protected Builder(String bedrockIdentifier, EntityFactory<T> factory, float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
            super(factory, width, height, offset, translators);
            this.bedrockIdentifier = Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier must not be null");
        }

        @Override
        public EntityDefinition.Builder<T> bedrockIdentifier(String bedrockIdentifier) {
            throw new UnsupportedOperationException("bedrockIdentifier should be immutable");
        }

        @Override
        public Builder<T> width(@Positive float width) {
            return (Builder<T>) super.width(width);
        }

        @Override
        public Builder<T> height(@Positive float height) {
            return (Builder<T>) super.height(height);
        }

        @Override
        public Builder<T> heightAndWidth(@Positive float value) {
            return (Builder<T>) super.heightAndWidth(value);
        }

        @Override
        public Builder<T> offset(@Positive float offset) {
            return (Builder<T>) super.offset(offset);
        }

        public Builder<T> predicate(@NonNull MinecraftPredicate<? super EntitySpawnContext> predicate) {
            predicates.add(Objects.requireNonNull(predicate, "predicate must not be null"));
            return this;
        }
        
        public Builder<T> predicateStrategy(@NonNull PredicateStrategy strategy) {
            predicateStrategy = Objects.requireNonNull(strategy, "strategy must not be null");
            return this;
        }
        
        @Override
        public Builder<T> property(PropertyType<?, ?> propertyType) {
            return (Builder<T>) super.property(propertyType);
        }

        @Override
        public <U, EM extends EntityMetadata<U, ? extends MetadataType<U>>> Builder<T> addTranslator(MetadataType<U> type, BiConsumer<T, EM> translateFunction) {
            return (Builder<T>) super.addTranslator(type, translateFunction);
        }

        @Override
        public Builder<T> addTranslator(EntityMetadataTranslator<T, ?, ?> translator) {
            return (Builder<T>) super.addTranslator(translator);
        }

        @Override
        public GeyserCustomEntityDefinition<T> build() {
            if (predicates.isEmpty()) {
                throw new IllegalStateException("predicates must not be empty!");
            }
            return new GeyserCustomEntityDefinition<>(factory, bedrockIdentifier, predicates, predicateStrategy, width, height, offset, propertiesBuilder.build(), translators);
        }
    }
}
