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
import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VanillaEntityDefinition<T extends Entity> extends EntityDefinition<T> {
    private final BuiltinEntityType builtinType;
    
    /**
     * @param identifier the Bedrock identifier of this entity
     */
    public VanillaEntityDefinition(EntityFactory<T> factory, GeyserEntityType entityType, String identifier,
                                   float width, float height, float offset, GeyserEntityProperties registeredProperties, List<EntityMetadataTranslator<? super T, ?, ?>> translators,
                                   BuiltinEntityType builtinType) {
        super(factory, entityType, identifier, width, height, offset, registeredProperties, translators);
        this.builtinType = builtinType;
    }

    public static <T extends Entity> Builder<T> inherited(EntityFactory<T> factory, EntityDefinition<? super T> parent) {
        return new Builder<>(factory, parent.entityType(), parent.identifier(), parent.width(), parent.height(), parent.offset(), new ObjectArrayList<>(parent.translators()));
    }

    public static <T extends Entity> Builder<T> builder(EntityFactory<T> factory) {
        return new Builder<>(factory);
    }

    public static class Builder<T extends Entity> extends EntityDefinition.Builder<T> {
        private BuiltinEntityType builtinType;

        private Builder(EntityFactory<T> factory) {
            super(factory);
        }

        public Builder(EntityFactory<T> factory, GeyserEntityType type, String identifier, float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> entityMetadataTranslators) {
            super(factory, type, identifier, width, height, offset, entityMetadataTranslators);
        }

        /**
         * Resets the identifier as well
         */
        public Builder<T> type(BuiltinEntityType type) {
            this.type = GeyserEntityType.ofVanilla(type);
            builtinType = type;
            identifier(null);
            return this;
        }

        @Override
        public Builder<T> identifier(String identifier) {
            return (Builder<T>) super.identifier(identifier);
        }

        @Override
        public Builder<T> width(float width) {
            return (Builder<T>) super.width(width);
        }

        @Override
        public Builder<T> height(float height) {
            return (Builder<T>) super.height(height);
        }

        @Override
        public Builder<T> heightAndWidth(float value) {
            return (Builder<T>) super.heightAndWidth(value);
        }

        @Override
        public Builder<T> offset(float offset) {
            return (Builder<T>) super.offset(offset);
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
        public VanillaEntityDefinition<T> build() {
            return build(true);
        }

        /**
         * @param register whether to register this entity in the Registries for entity types. Generally this should be
         * set to false if we're not expecting this entity to spawn from the network.
         */
        // TODO fix code duplication
        public VanillaEntityDefinition<T> build(boolean register) {
            if (identifier == null && type != null) {
                identifier = type.javaIdentifier().toString();
            }
            GeyserEntityProperties registeredProperties = propertiesBuilder == null ? null : propertiesBuilder.build();
            VanillaEntityDefinition<T> definition = new VanillaEntityDefinition<>(factory, type, identifier, width, height, offset, registeredProperties, translators, builtinType);
            if (register && definition.entityType() != null) {
                Registries.ENTITY_DEFINITIONS.get().putIfAbsent(definition.entityType(), definition);
                Registries.JAVA_ENTITY_IDENTIFIERS.get().putIfAbsent(type.javaIdentifier().toString(), definition);
            }
            return definition;
        }
    }
}
