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

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.FloatEntityProperty;
import org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IIntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;

import java.util.List;

public class GeyserEntityPropertyManager {

    private final GeyserEntityProperties properties;

    private final Object2ObjectMap<String, IntEntityProperty> intEntityProperties = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<String, FloatEntityProperty> floatEntityProperties = new Object2ObjectArrayMap<>();

    public GeyserEntityPropertyManager(GeyserEntityProperties properties) {
        this.properties = properties;
        for (PropertyType property : properties.getProperties()) {
            String name = property.getName();
            int index = properties.getPropertyIndex(name);
            if (property instanceof FloatProperty floatProperty) {
                floatEntityProperties.put(name, new FloatEntityProperty(index, floatProperty.getDefaultValue()));
            }
            else if (property instanceof IIntProperty intProperty) {
                intEntityProperties.put(name, new IntEntityProperty(index, intProperty.getDefaultValue()));
            }
        }
    }

    public boolean add(String propertyName, int value) {
        if (intEntityProperties.containsKey(propertyName) && value == intEntityProperties.get(propertyName).getValue()) return false;
        int index = properties.getPropertyIndex(propertyName);
        intEntityProperties.put(propertyName, new IntEntityProperty(index, value));
        return true;
    }

    public boolean add(String propertyName, boolean value) {
        int intValue = value ? 1 : 0;
        if (intEntityProperties.containsKey(propertyName) && intValue == intEntityProperties.get(propertyName).getValue()) return false;
        int index = properties.getPropertyIndex(propertyName);
        intEntityProperties.put(propertyName, new IntEntityProperty(index, intValue));
        return true;
    }

    public boolean add(String propertyName, String value) {
        int index = properties.getPropertyIndex(propertyName);
        PropertyType property = properties.getProperties().get(index);
        int enumIndex = ((EnumProperty) property).getIndex(value);
        if (intEntityProperties.containsKey(propertyName) && enumIndex == intEntityProperties.get(propertyName).getValue()) return false;
        intEntityProperties.put(propertyName, new IntEntityProperty(index, enumIndex));
        return true;
    }

    public boolean add(String propertyName, float value) {
        int index = properties.getPropertyIndex(propertyName);
        if (intEntityProperties.containsKey(propertyName) && value == floatEntityProperties.get(propertyName).getValue()) return false;
        floatEntityProperties.put(propertyName, new FloatEntityProperty(index, value));
        return true;
    }

    public boolean hasFloatProperties() {
        return !this.floatEntityProperties.isEmpty();
    }

    public boolean hasIntProperties() {
        return !this.intEntityProperties.isEmpty();
    }

    public boolean hasProperties() {
        return hasFloatProperties() || hasIntProperties();
    }

    public void applyIntProperties(List<IntEntityProperty> properties) {
        properties.addAll(intEntityProperties.values());
    }

    public void applyFloatProperties(List<FloatEntityProperty> properties) {
        properties.addAll(floatEntityProperties.values());
    }

    public NbtMap toNbtMap(String entityType) {
        return this.properties.toNbtMap(entityType);
    }
}
