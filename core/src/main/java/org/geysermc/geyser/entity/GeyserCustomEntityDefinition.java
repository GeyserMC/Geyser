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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.entity.EntitySpawnPredicateContext;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.translator.entity.EntityMetadataTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

import java.util.List;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
// TODO CHANGE TO ONLY APPLY THIS TO NON-VANILLA TYPES!
public class GeyserCustomEntityDefinition<T extends Entity> extends EntityDefinition<T> {
    private final List<MinecraftPredicate<? super EntitySpawnPredicateContext>> predicates;
    private final PredicateStrategy predicateStrategy;

    public GeyserCustomEntityDefinition(EntityFactory<T> factory, GeyserEntityType type, List<MinecraftPredicate<? super EntitySpawnPredicateContext>> predicates, PredicateStrategy predicateStrategy,
                                        BedrockEntityDefinition definition, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
        super(factory, type, definition, translators);
        this.predicates = predicates;
        this.predicateStrategy = predicateStrategy;
    }

    @Override
    public boolean is(BuiltinEntityType builtinEntityType) {
        return false;
    }

    // TODO this is relevant for nonvanilla; entity definitions are now split up
//    public static Builder<?> inherited(String bedrockIdentifier, JavaEntityType vanillaType) {
//        if (!vanillaType.vanilla()) {
//            throw new IllegalArgumentException("vanillaType must be a vanilla entity type, was: " + vanillaType);
//        }
//        VanillaEntityDefinition<?> parent = Registries.ENTITY_DEFINITIONS.get(vanillaType);
//        if (parent == null) {
//            throw new IllegalArgumentException("No vanilla entity definition registered for vanilla entity type " + vanillaType);
//        }
//        // TODO fix the rawtypes/unchecked
//        return new Builder<>(bedrockIdentifier, parent.factory(), parent.width(), parent.height(), parent.offset(), new ObjectArrayList(parent.translators()));
//    }

    // TODO
//    public static class Builder<T extends Entity> extends EntityDefinition.Builder<T> implements CustomEntityDefinition.Builder {
//        protected List<MinecraftPredicate<? super EntitySpawnPredicateContext>> predicates;
//        protected PredicateStrategy predicateStrategy = PredicateStrategy.AND;
//
//        protected Builder(EntityFactory<T> factory, String bedrockIdentifier) {
//            super(factory);
//            this.bedrockIdentifier = Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier must not be null");
//        }
//
//        protected Builder(String bedrockIdentifier, EntityFactory<T> factory, float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
//            super(factory, width, height, offset, translators);
//            this.bedrockIdentifier = Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier must not be null");
//        }
//
//        @Override
//        public EntityDefinition.Builder<T> bedrockIdentifier(String bedrockIdentifier) {
//            throw new UnsupportedOperationException("bedrockIdentifier should be immutable");
//        }
//
//        public Builder<T> predicate(@NonNull MinecraftPredicate<? super EntitySpawnPredicateContext> predicate) {
//            predicates.add(Objects.requireNonNull(predicate, "predicate must not be null"));
//            return this;
//        }
//
//        public Builder<T> predicateStrategy(@NonNull PredicateStrategy strategy) {
//            predicateStrategy = Objects.requireNonNull(strategy, "strategy must not be null");
//            return this;
//        }
//
//        @Override
//        public Builder<T> property(PropertyType<?, ?> propertyType) {
//            return (Builder<T>) super.property(propertyType);
//        }
//
//        @Override
//        public <U, EM extends EntityMetadata<U, ? extends MetadataType<U>>> Builder<T> addTranslator(MetadataType<U> type, BiConsumer<T, EM> translateFunction) {
//            return (Builder<T>) super.addTranslator(type, translateFunction);
//        }
//
//        @Override
//        public Builder<T> addTranslator(EntityMetadataTranslator<T, ?, ?> translator) {
//            return (Builder<T>) super.addTranslator(translator);
//        }
//
//        @Override
//        public GeyserCustomEntityDefinition<T> build() {
//            if (predicates.isEmpty()) {
//                throw new IllegalStateException("predicates must not be empty!");
//            }
//            return new GeyserCustomEntityDefinition<>(factory, type, predicates, predicateStrategy, bedrockDefinition, translators);
//        }
//    }
}
