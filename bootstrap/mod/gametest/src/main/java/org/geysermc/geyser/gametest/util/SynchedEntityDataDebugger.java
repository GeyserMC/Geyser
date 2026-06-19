/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.gametest.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class SynchedEntityDataDebugger {
    public static final Int2ObjectMap<List<Class<?>>> DATA_ACCESSOR_ORIGIN_MAP = new Int2ObjectOpenHashMap<>();

    public static @Nullable List<Class<?>> getPossibleOriginClasses(EntityDataAccessor<?> accessor) {
        return DATA_ACCESSOR_ORIGIN_MAP.get(accessor.id());
    }

    public static String findNameOfAccessor(Class<?> entityClazz, EntityDataAccessor<?> accessor) {
        return Optional.ofNullable(getPossibleOriginClasses(accessor))
            .flatMap(classes -> classes.stream()
                .filter(clazz -> clazz.isAssignableFrom(entityClazz))
                .findFirst() // There should only be one result
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredFields())
                    .filter(field -> Modifier.isStatic(field.getModifiers()))
                    .filter(field -> field.getType() == accessor.getClass())
                    .filter(field -> {
                        try {
                            field.setAccessible(true);
                            return field.get(null).equals(accessor);
                        } catch (IllegalAccessException exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .findFirst()
                    .map(Field::getName)
                    .map(name -> name + " defined in " + clazz.getSimpleName())
                )
            ).orElse("UNKNOWN");
    }

    public static String findNameOfSerializer(EntityDataSerializer<?> serializer) {
        return Arrays.stream(EntityDataSerializers.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
            .filter(field -> {
                try {
                    return field.get(null) == serializer;
                } catch (IllegalAccessException exception) {
                    throw new RuntimeException(exception);
                }
            })
            .findFirst()
            .map(Field::getName)
            .orElse("UNKNOWN");
    }

    public static String prettyPrintEntityDataAccessors(Class<?> entityClazz, SynchedEntityData.DataItem<?>[] items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            EntityDataAccessor<?> accessor = items[i].getAccessor();
            builder.append("At ").append(i).append(": ").append(findNameOfAccessor(entityClazz, accessor))
                .append(" of type ").append(findNameOfSerializer(accessor.serializer())).append("\n");
        }
        return builder.toString();
    }
}
