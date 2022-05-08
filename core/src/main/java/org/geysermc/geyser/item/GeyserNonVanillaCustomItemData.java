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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;

import java.util.OptionalInt;
import java.util.Set;

public record GeyserNonVanillaCustomItemData(CustomItemData customItemData,
                                             String identifier,
                                             int javaId,
                                             int stackSize,
                                             int maxDamage,
                                             String toolType,
                                             String toolTier,
                                             String armorType,
                                             String armorTier,
                                             int protectionValue,
                                             String translationString,
                                             Set<String> repairMaterials,
                                             OptionalInt creativeCategory,
                                             String creativeGroup,
                                             boolean isHat,
                                             boolean isTool) implements NonVanillaCustomItemData {
    @Override
    public @NonNull String name() {
        return this.customItemData.name();
    }

    @Override
    public CustomItemOptions customItemOptions() {
        return this.customItemData.customItemOptions();
    }

    @Override
    public @NonNull String displayName() {
        return this.customItemData.displayName();
    }

    @Override
    public @NonNull String icon() {
        return this.customItemData.icon();
    }

    @Override
    public boolean allowOffhand() {
        return this.customItemData.allowOffhand();
    }

    @Override
    public int textureSize() {
        return this.customItemData.textureSize();
    }

    @Override
    public @Nullable CustomRenderOffsets renderOffsets() {
        return this.customItemData.renderOffsets();
    }

    public static class NonVanillaCustomItemDataBuilder implements NonVanillaCustomItemData.Builder {
        private CustomItemData.Builder customItemData = CustomItemData.builder();

        private String identifier = null;
        private int javaId = -1;

        private int stackSize = 64;

        private int maxDamage = 0;

        private String toolType = null;
        private String toolTier = null;

        private String armorType = null;
        private String armorTier = null;
        private int protectionValue = 0;

        private String translationString;

        private Set<String> repairMaterials;

        private OptionalInt creativeCategory = OptionalInt.empty();
        private String creativeGroup = null;

        private boolean isHat = false;
        private boolean isTool = false;

        @Override
        public NonVanillaCustomItemData.Builder name(@NonNull String name) {
            this.customItemData.name(name);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder identifier(@NonNull String identifier) {
            this.identifier = identifier;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder javaId(int javaId) {
            this.javaId = javaId;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder maxDamage(int maxDamage) {
            this.maxDamage = maxDamage;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder toolType(@Nullable String toolType) {
            this.toolType = toolType;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder toolTier(@Nullable String toolTier) {
            this.toolTier = toolTier;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder armorType(@Nullable String armorType) {
            this.armorType = armorType;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder armorTier(@Nullable String armorTier) {
            this.armorTier = armorTier;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder protectionValue(int protectionValue) {
            this.protectionValue = protectionValue;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder translationString(@Nullable String translationString) {
            this.translationString = translationString;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder repairMaterials(@Nullable Set<String> repairMaterials) {
            this.repairMaterials = repairMaterials;
            return this;
        }

        @Override
        public Builder creativeCategory(int creativeCategory) {
            this.creativeCategory = OptionalInt.of(creativeCategory);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder creativeGroup(@Nullable String creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder isHat(boolean isHat) {
            this.isHat = isHat;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder isTool(boolean isTool) {
            this.isTool = isTool;
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder customItemOptions(@NonNull CustomItemOptions customItemOptions) {
            this.customItemData.customItemOptions(customItemOptions);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder displayName(@NonNull String displayName) {
            this.customItemData.displayName(displayName);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder icon(@NonNull String icon) {
            this.customItemData.icon(icon);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder allowOffhand(boolean allowOffhand) {
            this.customItemData.allowOffhand(allowOffhand);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder textureSize(int textureSize) {
            this.customItemData.textureSize(textureSize);
            return this;
        }

        @Override
        public NonVanillaCustomItemData.Builder renderOffsets(@Nullable CustomRenderOffsets renderOffsets) {
            this.customItemData.renderOffsets(renderOffsets);
            return this;
        }

        @Override
        public NonVanillaCustomItemData build() {
            if (identifier == null || javaId == -1) {
                throw new IllegalArgumentException("Identifier and javaId must be set");
            }
            return new GeyserNonVanillaCustomItemData(customItemData.build(), identifier, javaId, stackSize, maxDamage, toolType, toolTier, armorType, armorTier, protectionValue, translationString, repairMaterials, creativeCategory, creativeGroup, isHat, isTool);
        }
    }
}
