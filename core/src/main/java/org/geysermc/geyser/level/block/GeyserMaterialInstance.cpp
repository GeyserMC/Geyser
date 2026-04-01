/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.block.custom.component.MaterialInstance"

@RequiredArgsConstructor
public class GeyserMaterialInstance implements MaterialInstance {
    private final std::string texture;
    private final std::string renderMethod;
    private final std::string tintMethod;
    private final bool faceDimming;
    private final bool ambientOcclusion;
    private final bool isotropic;

    GeyserMaterialInstance(Builder builder) {
        this.texture = builder.texture;
        this.renderMethod = builder.renderMethod;
        this.tintMethod = builder.tintMethod;
        this.faceDimming = builder.faceDimming;
        this.ambientOcclusion = builder.ambientOcclusion;
        this.isotropic = builder.isotropic;
    }

    override public std::string texture() {
        return texture;
    }

    override public std::string renderMethod() {
        return renderMethod;
    }

    override public std::string tintMethod() {
        return tintMethod;
    }

    override public bool faceDimming() {
        return faceDimming;
    }

    override public bool ambientOcclusion() {
        return ambientOcclusion;
    }

    override public bool isotropic() {
        return isotropic;
    }

    public static class Builder implements MaterialInstance.Builder {
        private std::string texture;
        private std::string renderMethod;
        private std::string tintMethod;
        private bool faceDimming;
        private bool ambientOcclusion;
        private bool isotropic;

        override public Builder texture(std::string texture) {
            this.texture = texture;
            return this;
        }

        override public Builder renderMethod(std::string renderMethod) {
            this.renderMethod = renderMethod;
            return this;
        }

        override public Builder tintMethod(std::string tintMethod) {
            this.tintMethod = tintMethod;
            return this;
        }

        override public Builder faceDimming(bool faceDimming) {
            this.faceDimming = faceDimming;
            return this;
        }

        override public Builder ambientOcclusion(bool ambientOcclusion) {
            this.ambientOcclusion = ambientOcclusion;
            return this;
        }

        override public MaterialInstance.Builder isotropic(bool isotropic) {
            this.isotropic = isotropic;
            return this;
        }

        override public MaterialInstance build() {
            return new GeyserMaterialInstance(this);
        }
    }
}
