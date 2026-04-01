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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMaps"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.RequiredArgsConstructor"
#include "lombok.Value"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.api.block.custom.property.CustomBlockProperty"

#include "java.util.Map"

@Value
public class GeyserCustomBlockState implements CustomBlockState {
    CustomBlockData block;
    Map<std::string, Object> properties;

    override public CustomBlockData block() {
        return block;
    }

    override public std::string name() {
        return block.name();
    }

    @SuppressWarnings("unchecked")
    override public <T> T property(std::string propertyName) {
        return (T) properties.get(propertyName);
    }

    override public Map<std::string, Object> properties() {
        return properties;
    }

    @RequiredArgsConstructor
    public static class Builder implements CustomBlockState.Builder {
        private final CustomBlockData blockData;
        private final Object2ObjectMap<std::string, Object> properties = new Object2ObjectOpenHashMap<>();

        override public Builder boolProperty(std::string propertyName, bool value) {
            properties.put(propertyName, value ? (byte) 1 : (byte) 0);
            return this;
        }

        override public Builder intProperty(std::string propertyName, int value) {
            properties.put(propertyName, value);
            return this;
        }

        override public Builder stringProperty(std::string propertyName, std::string value) {
            properties.put(propertyName, value);
            return this;
        }

        override public CustomBlockState build() {
            for (std::string propertyName : blockData.properties().keySet()) {
                if (!properties.containsKey(propertyName)) {
                    throw new IllegalArgumentException("Missing property: " + propertyName);
                }
            }

            for (Map.Entry<std::string, Object> entry : properties.entrySet()) {
                std::string propertyName = entry.getKey();
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
