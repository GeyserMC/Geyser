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
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataType;
import org.geysermc.geyser.entity.type.Entity;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@AllArgsConstructor
public class GeyserEntityDataImpl<T> implements GeyserEntityDataType<T> {

    public static Map<String, GeyserEntityDataImpl<?>> TYPES;
    static {
        TYPES = new Object2ObjectOpenHashMap<>();
        TYPES.put("color", new GeyserEntityDataImpl<>(Byte.class, "color", EntityDataTypes.COLOR));
        TYPES.put("variant", new GeyserEntityDataImpl<>(Integer.class, "variant", EntityDataTypes.VARIANT));
        TYPES.put("width", new GeyserEntityDataImpl<>(Float.class, "width", EntityDataTypes.WIDTH));
        TYPES.put("height", new GeyserEntityDataImpl<>(Float.class, "height", EntityDataTypes.HEIGHT));
        TYPES.put("scale", new GeyserEntityDataImpl<>(Float.class, "scale", EntityDataTypes.SCALE));

        // "custom"
        TYPES.put("vertical_offset", new GeyserEntityDataImpl<>(Float.class, "offset", (entity, value) -> entity.offset(value, true), Entity::getOffset));
    }

    public static GeyserEntityDataImpl<?> lookup(Class<?> clazz, String name) {
        var type = TYPES.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown entity data type: " + name);
        }
        if (type.typeClass == clazz) {
            return TYPES.get(name);
        }
        throw new IllegalArgumentException("Unknown entity data type: " + name);
    }

    private final Class<T> typeClass;
    private final String name;
    private final BiConsumer<Entity, T> consumer;
    private final Function<Entity, T> getter;

    public GeyserEntityDataImpl(Class<T> typeClass, String name, EntityDataType<T> type) {
        this.typeClass = typeClass;
        this.name = name;
        this.consumer = (entity, data) -> entity.getDirtyMetadata().put(type, data);
        this.getter = entity -> {
            var value = entity.getMetadata().get(type);
            // Always the case!
            if (typeClass.isInstance(value)) {
                return typeClass.cast(value);
            } else {
                return null;
            }
        };
    }

    public GeyserEntityDataImpl(Class<T> typeClass, String name, EntityFlag flag) {
        this.typeClass = typeClass;
        this.name = name;
        this.consumer = (entity, data) -> {
            if (!(data instanceof Boolean bool)) {
                throw new RuntimeException("Invalid data type: " + data.getClass().getName());
            }
            entity.setFlag(flag, bool);
        };
        this.getter = entity -> typeClass.cast(entity.getFlag(flag));
    }

    @Override
    public @NonNull Class<T> typeClass() {
        return typeClass;
    }

    @Override
    public @NonNull String name() {
        return name;
    }

    public void update(Entity entity, T value) {
        consumer.accept(entity, value);
    }

    public T value(Entity entity) {
        return getter.apply(entity);
    }
}
