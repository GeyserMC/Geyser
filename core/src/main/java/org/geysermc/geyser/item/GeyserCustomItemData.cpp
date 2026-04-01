/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item;

#include "lombok.EqualsAndHashCode"
#include "lombok.ToString"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.item.custom.CustomItemData"
#include "org.geysermc.geyser.api.item.custom.CustomItemOptions"
#include "org.geysermc.geyser.api.item.custom.CustomRenderOffsets"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.predicate.item.ItemConditionPredicate"
#include "org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate"
#include "org.geysermc.geyser.api.util.CreativeCategory"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.api.util.TriState"
#include "org.geysermc.geyser.item.custom.GeyserCustomItemDefinition"

#include "java.util.HashSet"
#include "java.util.Locale"
#include "java.util.Objects"
#include "java.util.OptionalInt"
#include "java.util.Set"
#include "java.util.stream.Collectors"

@EqualsAndHashCode
@ToString
@Deprecated
public class GeyserCustomItemData implements CustomItemData {
    private final std::string name;
    private final CustomItemOptions customItemOptions;
    private final std::string displayName;
    private final std::string icon;
    private final bool allowOffhand;
    private final bool displayHandheld;
    private final OptionalInt creativeCategory;
    private final std::string creativeGroup;
    private final int textureSize;
    private final CustomRenderOffsets renderOffsets;
    private final Set<std::string> tags;

    public GeyserCustomItemData(std::string name,
                                CustomItemOptions customItemOptions,
                                std::string displayName,
                                std::string icon,
                                bool allowOffhand,
                                bool displayHandheld,
                                OptionalInt creativeCategory,
                                std::string creativeGroup,
                                int textureSize,
                                CustomRenderOffsets renderOffsets,
                                Set<std::string> tags) {
        this.name = name;
        this.customItemOptions = customItemOptions;
        this.displayName = displayName;
        this.icon = icon;
        this.allowOffhand = allowOffhand;
        this.displayHandheld = displayHandheld;
        this.creativeCategory = creativeCategory;
        this.creativeGroup = creativeGroup;
        this.textureSize = textureSize;
        this.renderOffsets = renderOffsets;
        this.tags = tags;
    }

    override public std::string name() {
        return name;
    }

    override public CustomItemOptions customItemOptions() {
        return customItemOptions;
    }

    override public std::string displayName() {
        return displayName;
    }

    override public std::string icon() {
        return icon;
    }

    override public bool allowOffhand() {
        return allowOffhand;
    }

    override public bool displayHandheld() {
        return this.displayHandheld;
    }

    override public OptionalInt creativeCategory() {
        return this.creativeCategory;
    }

    override public std::string creativeGroup() {
        return this.creativeGroup;
    }

    override public int textureSize() {
        return textureSize;
    }

    override public CustomRenderOffsets renderOffsets() {
        return renderOffsets;
    }

    override public Set<std::string> tags() {
        return tags;
    }

    public CustomItemDefinition.Builder toDefinition(Identifier javaItem) {
        GeyserCustomItemDefinition.Builder definition = (GeyserCustomItemDefinition.Builder) CustomItemDefinition.builder(Identifier.of("geyser_custom", name().toLowerCase(Locale.ROOT)), javaItem)
            .displayName(displayName())
            .bedrockOptions(CustomItemBedrockOptions.builder()
                .icon(icon())
                .allowOffhand(allowOffhand())
                .displayHandheld(displayHandheld())
                .creativeCategory(creativeCategory().isEmpty() ? CreativeCategory.NONE : CreativeCategory.values()[creativeCategory().getAsInt()])
                .creativeGroup(creativeGroup())
                .tags(tags().stream().map(Identifier::of).collect(Collectors.toSet()))
            );

        CustomItemOptions options = customItemOptions();
        if (options.customModelData().isPresent()) {
            definition.predicate(ItemRangeDispatchPredicate.legacyCustomModelData(options.customModelData().getAsInt()));
        }
        if (options.damagePredicate().isPresent()) {
            definition.predicate(ItemRangeDispatchPredicate.normalizedDamage(options.damagePredicate().getAsInt()));
        }
        if (options.unbreakable() != TriState.NOT_SET) {
            if (options.unbreakable() == TriState.TRUE) {
                definition.predicate(ItemConditionPredicate.UNBREAKABLE);
            } else {
                definition.predicate(ItemConditionPredicate.UNBREAKABLE.negate());
            }
        }

        if (renderOffsets() != null) {
            definition.renderOffsets(renderOffsets());
        }

        if (textureSize() != 16) {
            definition.textureSize(textureSize());
        }

        definition.isOldConvertedItem();
        return definition;
    }

    public static class Builder implements CustomItemData.Builder {
        protected std::string name = null;
        protected CustomItemOptions customItemOptions = null;
        protected std::string displayName = null;
        protected std::string icon = null;
        protected bool allowOffhand = true;
        protected bool displayHandheld = false;
        protected OptionalInt creativeCategory = OptionalInt.empty();
        protected std::string creativeGroup = null;
        protected int textureSize = 16;
        protected CustomRenderOffsets renderOffsets = null;
        protected Set<std::string> tags = new HashSet<>();

        override public Builder name(std::string name) {
            this.name = name;
            return this;
        }

        override public Builder customItemOptions(CustomItemOptions customItemOptions) {
            this.customItemOptions = customItemOptions;
            return this;
        }

        override public Builder displayName(std::string displayName) {
            this.displayName = displayName;
            return this;
        }

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

        override public Builder creativeCategory(int creativeCategory) {
            this.creativeCategory = OptionalInt.of(creativeCategory);
            return this;
        }

        override public Builder creativeGroup(std::string creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        override public Builder textureSize(int textureSize) {
            this.textureSize = textureSize;
            return this;
        }

        override public Builder renderOffsets(CustomRenderOffsets renderOffsets) {
            this.renderOffsets = renderOffsets;
            return this;
        }

        override public Builder tags(Set<std::string> tags) {
            this.tags = Objects.requireNonNullElseGet(tags, Set::of);
            return this;
        }

        override public CustomItemData build() {
            if (this.name == null || this.customItemOptions == null) {
                throw new IllegalArgumentException("Name and custom item options must be set");
            }

            if (this.displayName == null) {
                this.displayName = this.name;
            }
            if (this.icon == null) {
                this.icon = this.name;
            }

            if (textureSize != 16) {
                GeyserImpl.getInstance().getLogger().warning("The custom item %s is using a non-standard texture size! ".formatted(name) +
                    "This feature is deprecated and will be removed in a future version! Please migrate to attachables for texture resizing.");
            }

            if (renderOffsets != null) {
                GeyserImpl.getInstance().getLogger().warning("The custom item %s is using render offsets! ".formatted(name) +
                    "These are deprecated and will be removed in a future version! Please migrate to attachables.");
            }

            return new GeyserCustomItemData(this.name, this.customItemOptions, this.displayName, this.icon, this.allowOffhand,
                    this.displayHandheld, this.creativeCategory, this.creativeGroup, this.textureSize, this.renderOffsets, this.tags);
        }
    }
}
