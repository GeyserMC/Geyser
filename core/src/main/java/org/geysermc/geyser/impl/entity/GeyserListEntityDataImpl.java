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

package org.geysermc.geyser.impl.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.api.entity.data.GeyserListEntityDataType;
import org.geysermc.geyser.api.entity.data.types.Hitbox;
import org.geysermc.geyser.entity.type.Entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class GeyserListEntityDataImpl<ListType> extends GeyserEntityDataImpl<List<ListType>> implements GeyserListEntityDataType<ListType> {

    public static Map<String, GeyserListEntityDataImpl<?>> TYPES;
    static {
        TYPES = new Object2ObjectOpenHashMap<>();
        TYPES.put("hitboxes", new GeyserListEntityDataImpl<>(Hitbox.class, "hitboxes",
            (entity, hitboxes) -> entity.getDirtyMetadata().put(EntityDataTypes.HITBOX, HitboxImpl.toNbtMap(hitboxes)),
            (entity -> HitboxImpl.fromMetaData((NbtMap) entity.getMetadata().get(EntityDataTypes.HITBOX)))));
    }

    private final Class<ListType> listTypeClass;

    public GeyserListEntityDataImpl(Class<ListType> typeClass, String name, BiConsumer<Entity, List<ListType>> consumer, Function<Entity, List<ListType>> getter) {
        //noinspection unchecked - we do not talk about it
        super((Class<List<ListType>>) (Class<?>) List.class, name, consumer, getter);
        this.listTypeClass = typeClass;
    }

    @Override
    public @NonNull Class<ListType> listEntryClass() {
        return listTypeClass;
    }

    public static GeyserListEntityDataImpl<?> lookup(Class<?> clazz, Class<?> listTypeClass, String name) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(listTypeClass);
        Objects.requireNonNull(name);

        if (clazz != List.class) {
            throw new IllegalStateException("Cannot look up list entity data for " + clazz + " and " + listTypeClass + " for " + name);
        }

        var type = TYPES.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown entity data type: " + name);
        }
        if (type.listEntryClass() == listTypeClass) {
            return TYPES.get(name);
        }
        throw new IllegalArgumentException("Unknown entity data type: " + name);
    }
}
