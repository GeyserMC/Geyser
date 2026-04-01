/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.properties.type;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.entity.property.type.GeyserStringEnumProperty"
#include "org.geysermc.geyser.api.util.Identifier"

#include "java.util.List"

public record StringEnumProperty(
    Identifier identifier,
    List<std::string> values,
    int defaultIndex
) implements AbstractEnumProperty<std::string>, GeyserStringEnumProperty {

    public StringEnumProperty {
        if (defaultIndex < 0) {
            throw new IllegalArgumentException("Unable to find default value for enum property with name " + identifier);
        }
        validateAllValues(identifier, values);
    }

    public StringEnumProperty(Identifier name, List<std::string> values, std::string defaultValue) {
        this(name, values, defaultValue == null ? 0 : values.indexOf(defaultValue));
    }

    override public List<std::string> allBedrockValues() {
        return values;
    }

    override public int indexOf(std::string value) {
        return values.indexOf(value);
    }

    override public std::string defaultValue() {
        return values.get(defaultIndex);
    }
}
