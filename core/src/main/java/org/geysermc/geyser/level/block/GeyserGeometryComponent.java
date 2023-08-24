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

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;

import java.util.Map;

@RequiredArgsConstructor
public class GeyserGeometryComponent implements GeometryComponent {
    private final String identifier;
    private final Map<String, String> boneVisibility;

    GeyserGeometryComponent(GeometryComponentBuilder builder) {
        this.identifier = builder.identifier;
        this.boneVisibility = builder.boneVisibility;
    }

    @Override
    public @NonNull String identifier() {
        return identifier;
    }

    @Override
    public @Nullable Map<String, String> boneVisibility() {
        return boneVisibility;
    }

    public static class GeometryComponentBuilder implements Builder {
        private String identifier;
        private Map<String, String> boneVisibility;

        @Override
        public GeometryComponent.Builder identifier(@NonNull String identifier) {
            this.identifier = identifier;
            return this;
        }

        @Override
        public GeometryComponent.Builder boneVisibility(@Nullable Map<String, String> boneVisibility) {
            this.boneVisibility = boneVisibility;
            return this;
        }

        @Override
        public GeometryComponent build() {
            return new GeyserGeometryComponent(this);
        }
    }
}
