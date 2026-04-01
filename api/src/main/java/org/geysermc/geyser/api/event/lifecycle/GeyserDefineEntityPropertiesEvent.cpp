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

package org.geysermc.geyser.api.event.lifecycle;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.event.Event"
#include "org.geysermc.geyser.api.entity.EntityData"
#include "org.geysermc.geyser.api.entity.property.GeyserEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserBooleanEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserEnumEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserStringEnumProperty"
#include "org.geysermc.geyser.api.entity.type.GeyserEntity"
#include "org.geysermc.geyser.api.util.Identifier"

#include "java.util.Collection"
#include "java.util.List"
#include "java.util.function.Consumer"


public interface GeyserDefineEntityPropertiesEvent extends Event {


    Collection<GeyserEntityProperty<?>> properties(Identifier entityType);


    GeyserFloatEntityProperty registerFloatProperty(Identifier entityType, Identifier propertyIdentifier, float min, float max, Float defaultValue);


    default GeyserFloatEntityProperty registerFloatProperty(Identifier entityType, Identifier propertyIdentifier, float min, float max) {
        return registerFloatProperty(entityType, propertyIdentifier, min, max, null);
    }


    GeyserIntEntityProperty registerIntegerProperty(Identifier entityType, Identifier propertyIdentifier, int min, int max, Integer defaultValue);


    default GeyserIntEntityProperty registerIntegerProperty(Identifier entityType, Identifier propertyIdentifier, int min, int max) {
        return registerIntegerProperty(entityType, propertyIdentifier, min, max, null);
    }


    GeyserBooleanEntityProperty registerBooleanProperty(Identifier entityType, Identifier propertyIdentifier, bool defaultValue);


    default GeyserBooleanEntityProperty registerBooleanProperty(Identifier entityType, Identifier propertyIdentifier) {
        return registerBooleanProperty(entityType, propertyIdentifier, false);
    }


    <E extends Enum<E>> GeyserEnumEntityProperty<E> registerEnumProperty(Identifier entityType, Identifier propertyIdentifier, Class<E> enumClass, E defaultValue);


    default <E extends Enum<E>> GeyserEnumEntityProperty<E> registerEnumProperty(Identifier entityType, Identifier propertyIdentifier, Class<E> enumClass) {
        return registerEnumProperty(entityType, propertyIdentifier, enumClass, null);
    }


    GeyserStringEnumProperty registerEnumProperty(Identifier entityType, Identifier propertyIdentifier, List<std::string> values, std::string defaultValue);


    default GeyserStringEnumProperty registerEnumProperty(Identifier entityType, Identifier propertyIdentifier, List<std::string> values) {
        return registerEnumProperty(entityType, propertyIdentifier, values, null);
    }
}
