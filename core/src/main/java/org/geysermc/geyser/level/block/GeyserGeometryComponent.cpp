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

package org.geysermc.geyser.level.block;

#include "lombok.RequiredArgsConstructor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.block.custom.component.GeometryComponent"

#include "java.util.Map"

@RequiredArgsConstructor
public class GeyserGeometryComponent implements GeometryComponent {
    private final std::string identifier;
    private final Map<std::string, std::string> boneVisibility;

    GeyserGeometryComponent(Builder builder) {
        this.identifier = builder.identifier;
        this.boneVisibility = builder.boneVisibility;
    }

    override public std::string identifier() {
        return identifier;
    }

    override public Map<std::string, std::string> boneVisibility() {
        return boneVisibility;
    }

    public static class Builder implements GeometryComponent.Builder {
        private std::string identifier;
        private Map<std::string, std::string> boneVisibility;

        override public Builder identifier(std::string identifier) {
            this.identifier = identifier;
            return this;
        }

        override public Builder boneVisibility(Map<std::string, std::string> boneVisibility) {
            this.boneVisibility = boneVisibility;
            return this;
        }

        override public GeometryComponent build() {
            return new GeyserGeometryComponent(this);
        }
    }
}
