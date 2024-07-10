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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;

@RequiredArgsConstructor
public class GeyserMaterialInstance implements MaterialInstance {
    private final String texture;
    private final String renderMethod;
    private final boolean faceDimming;
    private final boolean ambientOcclusion;

    GeyserMaterialInstance(Builder builder) {
        this.texture = builder.texture;
        this.renderMethod = builder.renderMethod;
        this.faceDimming = builder.faceDimming;
        this.ambientOcclusion = builder.ambientOcclusion;
    }

    @Override
    public @Nullable String texture() {
        return texture;
    }

    @Override
    public @Nullable String renderMethod() {
        return renderMethod;
    }

    @Override
    public boolean faceDimming() {
        return faceDimming;
    }

    @Override
    public boolean ambientOcclusion() {
        return ambientOcclusion;
    }

    public static class Builder implements MaterialInstance.Builder {
        private String texture;
        private String renderMethod;
        private boolean faceDimming;
        private boolean ambientOcclusion;

        @Override
        public Builder texture(@Nullable String texture) {
            this.texture = texture;
            return this;
        }

        @Override
        public Builder renderMethod(@Nullable String renderMethod) {
            this.renderMethod = renderMethod;
            return this;
        }

        @Override
        public Builder faceDimming(boolean faceDimming) {
            this.faceDimming = faceDimming;
            return this;
        }

        @Override
        public Builder ambientOcclusion(boolean ambientOcclusion) {
            this.ambientOcclusion = ambientOcclusion;
            return this;
        }

        @Override
        public MaterialInstance build() {
            return new GeyserMaterialInstance(this);
        }
    }
}
