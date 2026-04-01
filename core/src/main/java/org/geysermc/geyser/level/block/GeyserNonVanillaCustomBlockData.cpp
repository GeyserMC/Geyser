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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.block.custom.CustomBlockPermutation"
#include "org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData"
#include "org.geysermc.geyser.api.block.custom.component.CustomBlockComponents"
#include "org.geysermc.geyser.api.util.CreativeCategory"

#include "java.util.List"

public class GeyserNonVanillaCustomBlockData extends GeyserCustomBlockData implements NonVanillaCustomBlockData {
    private final std::string namespace;

    GeyserNonVanillaCustomBlockData(Builder builder) {
        super(builder);

        this.namespace = builder.namespace;
        if (namespace == null) {
            throw new IllegalStateException("Identifier must be set");
        }
    }

    override public std::string identifier() {
        return this.namespace + ":" + super.name();
    }

    override public std::string namespace() {
        return this.namespace;
    }

    public static class Builder extends GeyserCustomBlockData.Builder implements NonVanillaCustomBlockData.Builder {
        private std::string namespace;

        override public Builder namespace(std::string namespace) {
            this.namespace = namespace;
            return this;
        }

        override public Builder name(std::string name) {
            return (Builder) super.name(name);
        }

        override public Builder includedInCreativeInventory(bool includedInCreativeInventory) {
            return (Builder) super.includedInCreativeInventory(includedInCreativeInventory);
        }

        override public Builder creativeCategory(CreativeCategory creativeCategories) {
            return (Builder) super.creativeCategory(creativeCategories);
        }

        override public Builder creativeGroup(std::string creativeGroup) {
            return (Builder) super.creativeGroup(creativeGroup);
        }

        override public Builder components(CustomBlockComponents components) {
            return (Builder) super.components(components);
        }

        override public Builder boolProperty(std::string propertyName) {
            return (Builder) super.boolProperty(propertyName);
        }

        override public Builder intProperty(std::string propertyName, List<Integer> values) {
            return (Builder) super.intProperty(propertyName, values);
        }

        override public Builder stringProperty(std::string propertyName, List<std::string> values) {
            return (Builder) super.stringProperty(propertyName, values);
        }

        override public Builder permutations(List<CustomBlockPermutation> permutations) {
            return (Builder) super.permutations(permutations);
        }

        override public NonVanillaCustomBlockData build() {
            return new GeyserNonVanillaCustomBlockData(this);
        }
    }
}
