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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;

import java.util.HashSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;

@EqualsAndHashCode
@ToString
public class GeyserCustomItemData implements CustomItemData {
    private final String name;
    private final CustomItemOptions customItemOptions;
    private final String displayName;
    private final String icon;
    private final boolean allowOffhand;
    private final boolean displayHandheld;
    private final OptionalInt creativeCategory;
    private final String creativeGroup;
    private final int textureSize;
    private final CustomRenderOffsets renderOffsets;
    private final Set<String> tags;
    private final int stackSize;
    private final int maxDamage;
    private final int attackDamage;
    private final String toolType;
    private final String toolTier;
    private final String translationString;
    private final int protectionValue;
    private final boolean isFoil;
    private final boolean isEdible;
    private final boolean canAlwaysEat;

    public GeyserCustomItemData(Builder builder) {
        this.name = builder.name;
        this.customItemOptions = builder.customItemOptions;
        this.displayName = builder.displayName;
        this.icon = builder.icon;
        this.allowOffhand = builder.allowOffhand;
        this.displayHandheld = builder.displayHandheld;
        this.creativeCategory = builder.creativeCategory;
        this.creativeGroup = builder.creativeGroup;
        this.textureSize = builder.textureSize;
        this.renderOffsets = builder.renderOffsets;
        this.tags = builder.tags;
        this.stackSize = builder.stackSize;
        this.maxDamage = builder.maxDamage;
        this.attackDamage = builder.attackDamage;
        this.toolType = builder.toolType;
        this.toolTier = builder.toolTier;
        this.translationString = builder.translationString;
        this.protectionValue = builder.protectionValue;
        this.isFoil = builder.foil;
        this.isEdible = builder.edible;
        this.canAlwaysEat = builder.canAlwaysEat;
    }

    @Override
    public @NonNull String name() {
        return name;
    }

    @Override
    public CustomItemOptions customItemOptions() {
        return customItemOptions;
    }

    @Override
    public @NonNull String displayName() {
        return displayName;
    }

    @Override
    public @NonNull String icon() {
        return icon;
    }

    @Override
    public boolean allowOffhand() {
        return allowOffhand;
    }

    @Override
    public boolean displayHandheld() {
        return this.displayHandheld;
    }

    @Override
    public @NonNull OptionalInt creativeCategory() {
        return this.creativeCategory;
    }

    @Override
    public @Nullable String creativeGroup() {
        return this.creativeGroup;
    }

    @Override
    public int textureSize() {
        return textureSize;
    }

    @Override
    public CustomRenderOffsets renderOffsets() {
        return renderOffsets;
    }

    @Override
    public @NonNull Set<String> tags() {
        return tags;
    }

    @Override
    public int stackSize() {
        return stackSize;
    }

    @Override
    public int maxDamage() {
        return maxDamage;
    }

    @Override
    public int attackDamage() {
        return attackDamage;
    }

    @Override
    public String toolType() {
        return toolType;
    }

    @Override
    public String toolTier() {
        return toolTier;
    }

    @Override
    public int protectionValue() {
        return protectionValue;
    }

    @Override
    public String translationString() {
        return translationString;
    }

    @Override
    public boolean isFoil() {
        return isFoil;
    }

    @Override
    public boolean isEdible() {
        return isEdible;
    }

    @Override
    public boolean canAlwaysEat() {
        return canAlwaysEat;
    }

    public static class Builder implements CustomItemData.Builder {
        protected String name = null;
        protected CustomItemOptions customItemOptions = null;
        protected String displayName = null;
        protected String icon = null;
        protected boolean allowOffhand = true; // Bedrock doesn't give items offhand allowance unless they serve gameplay purpose, but we want to be friendly with Java
        protected boolean displayHandheld = false;
        protected OptionalInt creativeCategory = OptionalInt.empty();
        protected String creativeGroup = null;
        protected int textureSize = 16;
        protected CustomRenderOffsets renderOffsets = null;
        protected Set<String> tags = new HashSet<>();
        private int stackSize = 0;
        private int maxDamage = -1;
        private int attackDamage = 0;
        private String toolType = null;
        private String toolTier = null;
        private int protectionValue = 0;
        private String translationString;
        private boolean foil = false;
        private boolean tool = false;
        private boolean edible = false;
        private boolean canAlwaysEat = false;

        @Override
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder customItemOptions(@NonNull CustomItemOptions customItemOptions) {
            this.customItemOptions = customItemOptions;
            return this;
        }

        @Override
        public Builder displayName(@NonNull String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public Builder icon(@NonNull String icon) {
            this.icon = icon;
            return this;
        }

        @Override
        public Builder allowOffhand(boolean allowOffhand) {
            this.allowOffhand = allowOffhand;
            return this;
        }

        @Override
        public Builder displayHandheld(boolean displayHandheld) {
            this.displayHandheld = displayHandheld;
            return this;
        }

        @Override
        public Builder creativeCategory(int creativeCategory) {
            this.creativeCategory = OptionalInt.of(creativeCategory);
            return this;
        }

        @Override
        public Builder creativeGroup(@Nullable String creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        @Override
        public Builder textureSize(int textureSize) {
            this.textureSize = textureSize;
            return this;
        }

        @Override
        public Builder renderOffsets(CustomRenderOffsets renderOffsets) {
            this.renderOffsets = renderOffsets;
            return this;
        }

        @Override
        public Builder tags(@Nullable Set<String> tags) {
            this.tags = Objects.requireNonNullElseGet(tags, Set::of);
            return this;
        }

        @Override
        public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        @Override
        public Builder maxDamage(int maxDamage) {
            this.maxDamage = maxDamage;
            return this;
        }

        @Override
        public Builder attackDamage(int attackDamage) {
            this.attackDamage = attackDamage;
            return this;
        }

        @Override
        public Builder toolType(@Nullable String toolType) {
            this.toolType = toolType;
            return this;
        }

        @Override
        public Builder toolTier(@Nullable String toolTier) {
            this.toolTier = toolTier;
            return this;
        }

        @Override
        public Builder protectionValue(int protectionValue) {
            this.protectionValue = protectionValue;
            return this;
        }

        @Override
        public Builder translationString(@Nullable String translationString) {
            this.translationString = translationString;
            return this;
        }

        @Override
        public Builder foil(boolean isFoil) {
            this.foil = isFoil;
            return this;
        }

        @Override
        public Builder edible(boolean isEdible) {
            this.edible = isEdible;
            return this;
        }

        @Override
        public Builder canAlwaysEat(boolean canAlwaysEat) {
            this.canAlwaysEat = canAlwaysEat;
            return this;
        }

        @Override
        public CustomItemData build() {
            if (this.name == null || this.customItemOptions == null) {
                throw new IllegalArgumentException("Name and custom item options must be set");
            }

            if (this.displayName == null) {
                this.displayName = this.name;
            }
            if (this.icon == null) {
                this.icon = this.name;
            }

            return new GeyserCustomItemData(this);
        }
    }
}
