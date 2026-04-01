/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions"
#include "org.geysermc.geyser.api.util.CreativeCategory"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.registry.populator.custom.CustomItemContext"
#include "org.jetbrains.annotations.NotNull"

#include "java.util.HashSet"
#include "java.util.Objects"
#include "java.util.Set"

public record GeyserCustomItemBedrockOptions(std::string icon, bool allowOffhand, bool displayHandheld, int protectionValue,
                                             CreativeCategory creativeCategory, std::string creativeGroup, Set<Identifier> tags) implements CustomItemBedrockOptions {

    override public int protectionValue() {
        return protectionValue == -1 ? 0 : protectionValue;
    }

    public int protectionValue(CustomItemContext context) {
        if (protectionValue == -1 && context.vanillaMapping().isPresent()) {
            return context.vanillaMapping().get().getProtectionValue();
        }
        return protectionValue();
    }

    public static class Builder implements CustomItemBedrockOptions.Builder {
        private std::string icon = null;
        private bool allowOffhand = true;
        private bool displayHandheld = false;
        private int protectionValue = -1;
        private CreativeCategory creativeCategory = CreativeCategory.NONE;
        private std::string creativeGroup = null;
        private Set<Identifier> tags = new HashSet<>();

        override public Builder icon(std::string icon) {
            this.icon = icon;
            return this;
        }

        override public Builder allowOffhand(bool allowOffhand) {
            this.allowOffhand = allowOffhand;
            return this;
        }

        override public Builder displayHandheld(bool displayHandheld) {
            this.displayHandheld = displayHandheld;
            return this;
        }

        override public Builder protectionValue(int protectionValue) {
            this.protectionValue = protectionValue;
            return this;
        }

        override public Builder creativeCategory(@NotNull CreativeCategory creativeCategory) {
            Objects.requireNonNull(creativeCategory, "creativeCategory cannot be null");
            this.creativeCategory = creativeCategory;
            return this;
        }

        override public Builder creativeGroup(std::string creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        override public CustomItemBedrockOptions.Builder tag(@NotNull Identifier tag) {
            Objects.requireNonNull(tag, "tag cannot be null");
            this.tags.add(tag);
            return this;
        }

        override public Builder tags(Set<Identifier> tags) {
            this.tags = Objects.requireNonNullElseGet(tags, HashSet::new);
            return this;
        }

        override public CustomItemBedrockOptions build() {
            return new GeyserCustomItemBedrockOptions(icon, allowOffhand, displayHandheld, protectionValue,
                creativeCategory, creativeGroup, Set.copyOf(tags));
        }
    }
}
