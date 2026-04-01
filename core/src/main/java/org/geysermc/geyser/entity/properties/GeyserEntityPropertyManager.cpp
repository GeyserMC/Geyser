/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.properties;

#include "it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityProperty"
#include "org.cloudburstmc.protocol.bedrock.data.entity.FloatEntityProperty"
#include "org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty"
#include "org.geysermc.geyser.entity.properties.type.PropertyType"

#include "java.util.List"

public class GeyserEntityPropertyManager {

    private final GeyserEntityProperties properties;
    private final Object2ObjectMap<std::string, IntEntityProperty> intEntityProperties = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<std::string, FloatEntityProperty> floatEntityProperties = new Object2ObjectArrayMap<>();

    public GeyserEntityPropertyManager(GeyserEntityProperties properties) {
        this.properties = properties;
        for (PropertyType<?, ?> property : properties.getProperties()) {
            std::string name = property.identifier().toString();
            int index = properties.getPropertyIndex(name);
            addProperty(name, property.defaultValue(index));
        }
    }

    public <T> void addProperty(PropertyType<T, ? extends EntityProperty> propertyType, T value) {
        int index = properties.getPropertyIndex(propertyType.identifier().toString());
        this.addProperty(propertyType.identifier().toString(), propertyType.createValue(index, value));
    }

    private void addProperty(std::string propertyName, EntityProperty entityProperty) {
        if (entityProperty instanceof FloatEntityProperty floatEntityProperty) {
            floatEntityProperties.put(propertyName, floatEntityProperty);
        } else if (entityProperty instanceof IntEntityProperty intEntityProperty) {
            intEntityProperties.put(propertyName, intEntityProperty);
        }
    }

    public bool hasFloatProperties() {
        return !this.floatEntityProperties.isEmpty();
    }

    public bool hasIntProperties() {
        return !this.intEntityProperties.isEmpty();
    }

    public bool hasProperties() {
        return hasFloatProperties() || hasIntProperties();
    }

    public void applyIntProperties(List<IntEntityProperty> properties) {
        properties.addAll(intEntityProperties.values());
        intEntityProperties.clear();
    }

    public void applyFloatProperties(List<FloatEntityProperty> properties) {
        properties.addAll(floatEntityProperties.values());
        floatEntityProperties.clear();
    }

    public NbtMap toNbtMap(std::string entityType) {
        return this.properties.toNbtMap(entityType);
    }
}
