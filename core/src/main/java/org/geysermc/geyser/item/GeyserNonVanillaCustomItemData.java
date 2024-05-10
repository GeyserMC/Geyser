/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@ToString
public final class GeyserNonVanillaCustomItemData extends GeyserCustomItemData implements NonVanillaCustomItemData {
    private final String identifier;
    private final int javaId;
    private final String armorType;
    private final Set<String> repairMaterials;
    private final boolean isHat;
    private final boolean isChargeable;
    private final String block;

    public GeyserNonVanillaCustomItemData(Builder builder) {
        super(builder);

        this.identifier = builder.identifier;
        this.javaId = builder.javaId;
        this.armorType = builder.armorType;
        this.repairMaterials = builder.repairMaterials;
        this.isHat = builder.hat;
        this.isChargeable = builder.chargeable;
        this.block = builder.block;
    }

    @Override
    public @NonNull String identifier() {
        return identifier;
    }

    @Override
    public int javaId() {
        return javaId;
    }

    @Override
    public @Nullable String armorType() {
        return armorType;
    }

    @Override
    public Set<String> repairMaterials() {
        return repairMaterials;
    }

    @Override
    public boolean isHat() {
        return isHat;
    }

    @Override
    public boolean isChargeable() {
        return isChargeable;
    }

    @Override
    public String block() {
        return block;
    }

    public static class Builder extends GeyserCustomItemData.Builder implements NonVanillaCustomItemData.Builder {
        private String identifier = null;
        private int javaId = -1;
        private String armorType = null;
        private Set<String> repairMaterials;
        private boolean hat = false;
        private boolean chargeable = false;
        private String block = null;

        @Override
        public Builder name(@NonNull String name) {
            return (Builder) super.name(name);
        }

        @Override
        public Builder customItemOptions(@NonNull CustomItemOptions customItemOptions) {
            //Do nothing, as that value won't be read
            return this;
        }

        @Override
        public Builder allowOffhand(boolean allowOffhand) {
            return (Builder) super.allowOffhand(allowOffhand);
        }

        @Override
        public Builder displayHandheld(boolean displayHandheld) {
            return (Builder) super.displayHandheld(displayHandheld);
        }

        @Override
        public Builder displayName(@NonNull String displayName) {
            return (Builder) super.displayName(displayName);
        }

        @Override
        public Builder icon(@NonNull String icon) {
            return (Builder) super.icon(icon);
        }

        @Override
        public Builder textureSize(int textureSize) {
            return (Builder) super.textureSize(textureSize);
        }

        @Override
        public Builder renderOffsets(CustomRenderOffsets renderOffsets) {
            return (Builder) super.renderOffsets(renderOffsets);
        }

        @Override
        public Builder tags(@Nullable Set<String> tags) {
            return (Builder) super.tags(tags);
        }

        @Override
        public Builder identifier(@NonNull String identifier) {
            this.identifier = identifier;
            return this;
        }

        @Override
        public Builder javaId(int javaId) {
            this.javaId = javaId;
            return this;
        }

        @Override
        public Builder armorType(@Nullable String armorType) {
            this.armorType = armorType;
            return this;
        }

        @Override
        public Builder repairMaterials(@Nullable Set<String> repairMaterials) {
            this.repairMaterials = repairMaterials;
            return this;
        }

        @Override
        public Builder creativeCategory(int creativeCategory) {
            return (Builder) super.creativeCategory(creativeCategory);
        }

        @Override
        public Builder creativeGroup(@Nullable String creativeGroup) {
            return (Builder) super.creativeGroup(creativeGroup);
        }

        @Override
        public Builder hat(boolean isHat) {
            this.hat = isHat;
            return this;
        }

        @Override
        public Builder chargeable(boolean isChargeable) {
            this.chargeable = isChargeable;
            return this;
        }

        @Override
        public Builder block(String block) {
            this.block = block;
            return this;
        }

        @Override
        public NonVanillaCustomItemData build() {
            if (identifier == null || javaId == -1) {
                throw new IllegalArgumentException("Identifier and javaId must be set");
            }

            super.customItemOptions(CustomItemOptions.builder().build());
            super.build();
            return new GeyserNonVanillaCustomItemData(this);
        }
    }
}
