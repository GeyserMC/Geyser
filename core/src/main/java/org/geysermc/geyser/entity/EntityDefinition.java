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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.properties.type.PropertyType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.translator.entity.EntityMetadataTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Represents data for an entity. This includes the default bedrock entity definition, as well as the list of Java entity
 * metadata translators needed to translate the properties sent from the server. The translators are structured in such
 * a way that inserting a new one (for example in version updates) is convenient.
 *
 * @param <T> the entity type this definition represents
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class EntityDefinition<T extends Entity> extends EntityDefinitionBase<T> {
    private final EntityFactory<T> factory;
    private final GeyserEntityType type;
    private BedrockEntityDefinition bedrockDefinition;

    public EntityDefinition(EntityFactory<T> factory, GeyserEntityType type, BedrockEntityDefinition bedrockDefinition, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
        super(bedrockDefinition.width(), bedrockDefinition.height(), bedrockDefinition.offset(), translators);
        this.type = type;
        this.factory = factory;
        this.bedrockDefinition = bedrockDefinition;
    }

    public abstract boolean is(BuiltinEntityType builtinEntityType);

    @Setter
    @Accessors(fluent = true, chain = true)
    public static abstract class Builder<T extends Entity> extends EntityDefinitionBase.Builder<T> {
        protected final EntityFactory<T> factory;
        protected String bedrockIdentifier;
        @Setter(AccessLevel.NONE)
        protected GeyserEntityProperties.Builder propertiesBuilder;

        protected Builder(EntityFactory<T> factory) {
            super();
            this.factory = factory;
        }

        protected Builder(EntityFactory<T> factory, float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
            super(width, height, offset, translators);
            this.factory = factory;
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
        public <U, EM extends EntityMetadata<U, ? extends MetadataType<U>>> Builder<T> addTranslator(MetadataType<U> type, BiConsumer<T, EM> translateFunction) {
            return (Builder<T>) super.addTranslator(type, translateFunction);
        }

        @Override
        public Builder<T> addTranslator(EntityMetadataTranslator<T, ?, ?> translator) {
            return (Builder<T>) super.addTranslator(translator);
        }

        public Builder<T> property(PropertyType<?, ?> propertyType) {
            if (this.propertiesBuilder == null) {
                this.propertiesBuilder = new GeyserEntityProperties.Builder(this.bedrockIdentifier);
            }
            propertiesBuilder.add(propertyType);
            return this;
        }
    }
}
