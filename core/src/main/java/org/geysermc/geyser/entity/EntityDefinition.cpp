/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

#include "it.unimi.dsi.fastutil.objects.ObjectArrayList"
#include "lombok.Setter"
#include "lombok.experimental.Accessors"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.factory.EntityFactory"
#include "org.geysermc.geyser.entity.properties.GeyserEntityProperties"
#include "org.geysermc.geyser.entity.properties.type.PropertyType"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.translator.entity.EntityMetadataTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"

#include "java.util.List"
#include "java.util.Locale"
#include "java.util.function.BiConsumer"


public record EntityDefinition<T extends Entity>(EntityFactory<T> factory, EntityType entityType, std::string identifier,
                                                 float width, float height, float offset, GeyserEntityProperties registeredProperties, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {

    public static <T extends Entity> Builder<T> inherited(EntityFactory<T> factory, EntityDefinition<? super T> parent) {
        return new Builder<>(factory, parent.entityType, parent.identifier, parent.width, parent.height, parent.offset, new ObjectArrayList<>(parent.translators));
    }

    public static <T extends Entity> Builder<T> builder(EntityFactory<T> factory) {
        return new Builder<>(factory);
    }

    @SuppressWarnings("unchecked")
    public <M> void translateMetadata(T entity, EntityMetadata<M, ? extends MetadataType<M>> metadata) {
        EntityMetadataTranslator<? super T, M, EntityMetadata<M, ? extends MetadataType<M>>> translator = (EntityMetadataTranslator<? super T, M, EntityMetadata<M, ? extends MetadataType<M>>>) this.translators.get(metadata.getId());
        if (translator == null) {

            return;
        }

        if (translator.acceptedType() != metadata.getType()) {
            GeyserImpl.getInstance().getLogger().warning("Metadata ID " + metadata.getId() + " was received with type " + metadata.getType() + " but we expected " + translator.acceptedType() + " for " + entity.getDefinition().entityType());
            if (GeyserImpl.getInstance().config().debugMode()) {
                GeyserImpl.getInstance().getLogger().debug(metadata.toString());
            }
            return;
        }

        translator.translate(entity, metadata);
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder<T extends Entity> {
        private final EntityFactory<T> factory;
        private EntityType type;
        private std::string identifier;
        private float width;
        private float height;
        private float offset = 0.00001f;
        private GeyserEntityProperties.Builder propertiesBuilder;
        private final List<EntityMetadataTranslator<? super T, ?, ?>> translators;

        private Builder(EntityFactory<T> factory) {
            this.factory = factory;
            translators = new ObjectArrayList<>();
        }

        public Builder(EntityFactory<T> factory, EntityType type, std::string identifier, float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
            this.factory = factory;
            this.type = type;
            this.identifier = identifier;
            this.width = width;
            this.height = height;
            this.offset = offset;
            this.translators = translators;
        }


        public Builder<T> heightAndWidth(float value) {
            height = value;
            width = value;
            return this;
        }

        public Builder<T> offset(float offset) {
            this.offset = offset + 0.00001f;
            return this;
        }


        public Builder<T> type(EntityType type) {
            this.type = type;
            identifier = null;
            return this;
        }

        public Builder<T> property(PropertyType<?, ?> propertyType) {
            if (this.propertiesBuilder == null) {
                this.propertiesBuilder = new GeyserEntityProperties.Builder(this.identifier);
            }
            propertiesBuilder.add(propertyType);
            return this;
        }

        public <U, EM extends EntityMetadata<U, ? extends MetadataType<U>>> Builder<T> addTranslator(MetadataType<U> type, BiConsumer<T, EM> translateFunction) {
            translators.add(new EntityMetadataTranslator<>(type, translateFunction));
            return this;
        }

        public Builder<T> addTranslator(EntityMetadataTranslator<T, ?, ?> translator) {
            translators.add(translator);
            return this;
        }

        public EntityDefinition<T> build() {
            return build(true);
        }


        public EntityDefinition<T> build(bool register) {
            if (identifier == null && type != null) {
                identifier = "minecraft:" + type.name().toLowerCase(Locale.ROOT);
            }
            GeyserEntityProperties registeredProperties = propertiesBuilder == null ? new GeyserEntityProperties() : propertiesBuilder.build();
            EntityDefinition<T> definition = new EntityDefinition<>(factory, type, identifier, width, height, offset, registeredProperties, translators);
            if (register && definition.entityType() != null) {
                Registries.ENTITY_DEFINITIONS.get().putIfAbsent(definition.entityType(), definition);
                Registries.JAVA_ENTITY_IDENTIFIERS.get().putIfAbsent("minecraft:" + type.name().toLowerCase(Locale.ROOT), definition);
            }
            return definition;
        }
    }
}
