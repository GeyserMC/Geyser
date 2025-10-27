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
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.translator.entity.EntityMetadataTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;

import java.util.List;
import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public class EntityDefinitionBase<T extends Entity> {
    private final float width;
    private final float height;
    private final float offset;
    private final List<EntityMetadataTranslator<? super T, ?, ?>> translators;

    public EntityDefinitionBase(float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
        this.width = width;
        this.height = height;
        this.offset = offset;
        this.translators = translators;
    }

    public static <T extends Entity> Builder<T> baseBuilder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    // Unused param so Java knows what entity we're talking about
    @SuppressWarnings("unused")
    public static <T extends Entity> Builder<T> baseInherited(Class<T> clazz, EntityDefinitionBase<? super T> parent) {
        return new Builder<>(parent.width(), parent.height(), parent.offset(), new ObjectArrayList<>(parent.translators()));
    }

    @SuppressWarnings("unchecked")
    public <M> void translateMetadata(T entity, EntityMetadata<M, ? extends MetadataType<M>> metadata) {
        EntityMetadataTranslator<? super T, M, EntityMetadata<M, ? extends MetadataType<M>>> translator = (EntityMetadataTranslator<? super T, M, EntityMetadata<M, ? extends MetadataType<M>>>) this.translators.get(metadata.getId());
        if (translator == null) {
            // This can safely happen; it means we don't translate this entity metadata
            return;
        }

        if (translator.acceptedType() != metadata.getType()) {
            GeyserImpl.getInstance().getLogger().warning("Metadata ID " + metadata.getId() + " was received with type " + metadata.getType() + " but we expected " + translator.acceptedType() + " for " + entity.getDefinition().entityType());
            if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                GeyserImpl.getInstance().getLogger().debug(metadata.toString());
            }
            return;
        }

        translator.translate(entity, metadata);
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder<T extends Entity> {
        protected float width;
        protected float height;
        protected float offset = 0.00001f;
        protected final List<EntityMetadataTranslator<? super T, ?, ?>> translators;

        protected Builder() {
            translators = new ObjectArrayList<>();
        }

        // Unused param so Java knows what entity we're talking about
        @SuppressWarnings("unused")
        protected Builder(Class<T> clazz) {
            this();
        }

        protected Builder(float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?, ?>> translators) {
            this.width = width;
            this.height = height;
            this.offset = offset;
            this.translators = translators;
        }

        /**
         * Sets the height and width as one value
         */
        public Builder<T> heightAndWidth(float value) {
            height = value;
            width = value;
            return this;
        }

        public Builder<T> offset(float offset) {
            this.offset = offset + 0.00001f;
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

        public EntityDefinitionBase<T> build() {
            return new EntityDefinitionBase<>(width, height, offset, translators);
        }
    }
}
