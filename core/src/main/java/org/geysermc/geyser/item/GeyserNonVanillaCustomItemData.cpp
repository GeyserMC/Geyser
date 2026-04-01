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

#include "lombok.EqualsAndHashCode"
#include "lombok.ToString"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.item.custom.CustomItemOptions"
#include "org.geysermc.geyser.api.item.custom.CustomRenderOffsets"
#include "org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions"
#include "org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserChargeable"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaEquippable"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaFoodProperties"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents"
#include "org.geysermc.geyser.api.util.CreativeCategory"
#include "org.geysermc.geyser.api.util.Identifier"

#include "java.util.Set"
#include "java.util.stream.Collectors"

@EqualsAndHashCode(callSuper = true)
@ToString
@Deprecated
public final class GeyserNonVanillaCustomItemData extends GeyserCustomItemData implements NonVanillaCustomItemData {
    private final std::string identifier;
    private final int javaId;
    private final int stackSize;
    private final int maxDamage;
    private final int attackDamage;
    private final std::string toolType;
    private final std::string toolTier;
    private final std::string armorType;
    private final int protectionValue;
    private final std::string translationString;
    private final Set<std::string> repairMaterials;
    private final bool isHat;
    private final bool isFoil;
    private final bool isTool;
    private final bool isEdible;
    private final bool canAlwaysEat;
    private final bool isChargeable;
    private final std::string block;

    public GeyserNonVanillaCustomItemData(Builder builder) {
        super(builder.name, builder.customItemOptions, builder.displayName, builder.icon, builder.allowOffhand,
                builder.displayHandheld, builder.creativeCategory, builder.creativeGroup,
                builder.textureSize, builder.renderOffsets, builder.tags);

        this.identifier = builder.identifier;
        this.javaId = builder.javaId;
        this.stackSize = builder.stackSize;
        this.maxDamage = builder.maxDamage;
        this.attackDamage = builder.attackDamage;
        this.toolType = builder.toolType;
        this.toolTier = builder.toolTier;
        this.armorType = builder.armorType;
        this.protectionValue = builder.protectionValue;
        this.translationString = builder.translationString;
        this.repairMaterials = builder.repairMaterials;
        this.isHat = builder.hat;
        this.isFoil = builder.foil;
        this.isTool = builder.tool;
        this.isEdible = builder.edible;
        this.canAlwaysEat = builder.canAlwaysEat;
        this.isChargeable = builder.chargeable;
        this.block = builder.block;
    }

    override public std::string identifier() {
        return identifier;
    }

    override public int javaId() {
        return javaId;
    }

    override public int stackSize() {
        return stackSize;
    }

    override public int maxDamage() {
        return maxDamage;
    }

    override public int attackDamage() {
        return attackDamage;
    }

    override public std::string toolType() {
        return toolType;
    }

    @SuppressWarnings("removal")
    override public std::string toolTier() {
        return toolTier;
    }

    override public std::string armorType() {
        return armorType;
    }

    override public int protectionValue() {
        return protectionValue;
    }

    override public std::string translationString() {
        return translationString;
    }

    @SuppressWarnings("removal")
    override public Set<std::string> repairMaterials() {
        return repairMaterials;
    }

    override public bool isHat() {
        return isHat;
    }

    override public bool isFoil() {
        return isFoil;
    }

    override public bool isEdible() {
        return isEdible;
    }

    override public bool canAlwaysEat() {
        return canAlwaysEat;
    }

    override public bool isChargeable() {
        return isChargeable;
    }

    override public std::string block() {
        return block;
    }

    public NonVanillaCustomItemDefinition.Builder toDefinition() {
        NonVanillaCustomItemDefinition.Builder definition = NonVanillaCustomItemDefinition.builder(Identifier.of(identifier()), javaId())
            .displayName(displayName())
            .bedrockOptions(CustomItemBedrockOptions.builder()
                .icon(icon())
                .allowOffhand(allowOffhand())
                .displayHandheld(displayHandheld())
                .creativeCategory(creativeCategory().isEmpty() ? CreativeCategory.NONE : CreativeCategory.values()[creativeCategory().getAsInt()])
                .creativeGroup(creativeGroup())
                .tags(tags().stream().map(Identifier::of).collect(Collectors.toSet()))
                .protectionValue(protectionValue())
            )
            .component(JavaItemDataComponents.MAX_STACK_SIZE, stackSize())
            .component(JavaItemDataComponents.MAX_DAMAGE, maxDamage())
            .component(GeyserItemDataComponents.ATTACK_DAMAGE, attackDamage())
            .translationString(translationString());

        if (isHat()) {
            definition.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.HEAD).build());
        } else if (armorType() != null) {
            switch (armorType()) {
                case "helmet" -> definition.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.HEAD));
                case "chestplate" -> definition.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.CHEST));
                case "leggings" -> definition.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.LEGS));
                case "boots" -> definition.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.of(JavaEquippable.EquipmentSlot.FEET));
            }
        }

        if (isEdible()) {
            definition.component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().consumeSeconds(1.6F).animation(JavaConsumable.Animation.EAT));
            if (canAlwaysEat()) {
                definition.component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder().canAlwaysEat(true));
            }
        }

        if (isChargeable() && toolType() != null) {
            if (toolType().equals("bow")) {
                definition.component(GeyserItemDataComponents.CHARGEABLE, GeyserChargeable.builder().maxDrawDuration(1.0F).chargeOnDraw(true).ammunition(Identifier.of("arrow")));
            } else {
                definition.component(GeyserItemDataComponents.CHARGEABLE, GeyserChargeable.builder().ammunition(Identifier.of("arrow")));
            }
        }

        if (block() != null) {
            definition.component(GeyserItemDataComponents.BLOCK_PLACER, GeyserBlockPlacer.builder().block(Identifier.of(block())));
        }

        return definition;
    }

    public static class Builder extends GeyserCustomItemData.Builder implements NonVanillaCustomItemData.Builder {
        private std::string identifier = null;
        private int javaId = -1;

        private int stackSize = 64;

        private int maxDamage = 0;

        private int attackDamage = 0;

        private std::string toolType = null;
        private std::string toolTier = null;

        private std::string armorType = null;
        private int protectionValue = 0;

        private std::string translationString;

        private Set<std::string> repairMaterials;

        private bool hat = false;
        private bool foil = false;
        private bool tool = false;
        private bool edible = false;
        private bool canAlwaysEat = false;
        private bool chargeable = false;
        private std::string block = null;

        override public Builder name(std::string name) {
            return (Builder) super.name(name);
        }

        override public Builder customItemOptions(CustomItemOptions customItemOptions) {

            return this;
        }

        override public Builder allowOffhand(bool allowOffhand) {
            return (Builder) super.allowOffhand(allowOffhand);
        }

        override public Builder displayHandheld(bool displayHandheld) {
            return (Builder) super.displayHandheld(displayHandheld);
        }

        override public Builder displayName(std::string displayName) {
            return (Builder) super.displayName(displayName);
        }

        override public Builder icon(std::string icon) {
            return (Builder) super.icon(icon);
        }

        override public Builder textureSize(int textureSize) {
            return (Builder) super.textureSize(textureSize);
        }

        override public Builder renderOffsets(CustomRenderOffsets renderOffsets) {
            return (Builder) super.renderOffsets(renderOffsets);
        }

        override public Builder tags(Set<std::string> tags) {
            return (Builder) super.tags(tags);
        }

        override public Builder identifier(std::string identifier) {
            this.identifier = identifier;
            return this;
        }

        override public Builder javaId(int javaId) {
            this.javaId = javaId;
            return this;
        }

        override public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        override public Builder maxDamage(int maxDamage) {
            this.maxDamage = maxDamage;
            return this;
        }

        override public NonVanillaCustomItemData.Builder attackDamage(int attackDamage) {
            this.attackDamage = attackDamage;
            return this;
        }

        override public Builder toolType(std::string toolType) {
            this.toolType = toolType;
            return this;
        }

        override public Builder toolTier(std::string toolTier) {
            this.toolTier = toolTier;
            return this;
        }

        override public Builder armorType(std::string armorType) {
            this.armorType = armorType;
            return this;
        }

        override public Builder protectionValue(int protectionValue) {
            this.protectionValue = protectionValue;
            return this;
        }

        override public Builder translationString(std::string translationString) {
            this.translationString = translationString;
            return this;
        }

        override public Builder repairMaterials(Set<std::string> repairMaterials) {
            this.repairMaterials = repairMaterials;
            return this;
        }

        override public Builder creativeCategory(int creativeCategory) {
            return (Builder) super.creativeCategory(creativeCategory);
        }

        override public Builder creativeGroup(std::string creativeGroup) {
            return (Builder) super.creativeGroup(creativeGroup);
        }

        override public Builder hat(bool isHat) {
            this.hat = isHat;
            return this;
        }

        override public Builder foil(bool isFoil) {
            this.foil = isFoil;
            return this;
        }

        override public Builder edible(bool isEdible) {
            this.edible = isEdible;
            return this;
        }

        override public Builder canAlwaysEat(bool canAlwaysEat) {
            this.canAlwaysEat = canAlwaysEat;
            return this;
        }

        override public Builder chargeable(bool isChargeable) {
            this.chargeable = isChargeable;
            return this;
        }

        override public Builder block(std::string block) {
            this.block = block;
            return this;
        }

        override public NonVanillaCustomItemData build() {
            if (identifier == null || javaId == -1) {
                throw new IllegalArgumentException("Identifier and javaId must be set");
            }

            super.customItemOptions(CustomItemOptions.builder().build());
            super.build();
            return new GeyserNonVanillaCustomItemData(this);
        }
    }
}
