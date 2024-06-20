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

    public GeyserCustomItemData(String name,
                                CustomItemOptions customItemOptions,
                                String displayName,
                                String icon,
                                boolean allowOffhand,
                                boolean displayHandheld,
                                OptionalInt creativeCategory,
                                String creativeGroup,
                                int textureSize,
                                CustomRenderOffsets renderOffsets,
                                Set<String> tags) {
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
            return new GeyserCustomItemData(this.name, this.customItemOptions, this.displayName, this.icon, this.allowOffhand,
                    this.displayHandheld, this.creativeCategory, this.creativeGroup, this.textureSize, this.renderOffsets, this.tags);
        }
    }
}
