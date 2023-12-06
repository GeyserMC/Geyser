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

package org.geysermc.geyser.level.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;

import java.util.Map;

@Value
public class GeyserCustomBlockState implements CustomBlockState {
    CustomBlockData block;
    Map<String, Object> properties;

    @Override
    public @NonNull CustomBlockData block() {
        return block;
    }

    @Override
    public @NonNull String name() {
        return block.name();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @NonNull T property(@NonNull String propertyName) {
        return (T) properties.get(propertyName);
    }

    @Override
    public @NonNull Map<String, Object> properties() {
        return properties;
    }

    @RequiredArgsConstructor
    public static class CustomBlockStateBuilder implements CustomBlockState.Builder {
        private final CustomBlockData blockData;
        private final Object2ObjectMap<String, Object> properties = new Object2ObjectOpenHashMap<>();

        @Override
        public Builder booleanProperty(@NonNull String propertyName, boolean value) {
            properties.put(propertyName, value ? (byte) 1 : (byte) 0);
            return this;
        }

        @Override
        public Builder intProperty(@NonNull String propertyName, int value) {
            properties.put(propertyName, value);
            return this;
        }

        @Override
        public Builder stringProperty(@NonNull String propertyName, @NonNull String value) {
            properties.put(propertyName, value);
            return this;
        }

        @Override
        public CustomBlockState build() {
            for (String propertyName : blockData.properties().keySet()) {
                if (!properties.containsKey(propertyName)) {
                    throw new IllegalArgumentException("Missing property: " + propertyName);
                }
            }

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Object propertyValue = entry.getValue();

                CustomBlockProperty<?> property = blockData.properties().get(propertyName);
                if (property == null) {
                    throw new IllegalArgumentException("Unknown property: " + propertyName);
                } else if (!property.values().contains(propertyValue)) {
                    throw new IllegalArgumentException("Invalid value: " + propertyValue + " for property: " + propertyName);
                }
            }

            return new GeyserCustomBlockState(blockData, Object2ObjectMaps.unmodifiable(properties));
        }
    }
}
