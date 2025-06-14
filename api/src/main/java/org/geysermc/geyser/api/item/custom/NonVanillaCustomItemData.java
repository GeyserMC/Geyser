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

package org.geysermc.geyser.api.item.custom;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.BlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.Chargeable;
import org.geysermc.geyser.api.item.custom.v2.component.Consumable;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.Equippable;
import org.geysermc.geyser.api.item.custom.v2.component.FoodProperties;
import org.geysermc.geyser.api.item.custom.v2.component.GeyserDataComponent;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a completely custom item that is not based on an existing vanilla Minecraft item.
 *
 * @deprecated use the new {@link org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition}
 */
@Deprecated
public interface NonVanillaCustomItemData extends CustomItemData {
    /**
     * Gets the java identifier for this item.
     *
     * @return The java identifier for this item.
     */
    @NonNull String identifier();

    /**
     * Gets the java item id of the item.
     *
     * @return the java item id of the item
     */
    @NonNegative int javaId();

    /**
     * Gets the stack size of the item.
     *
     * @return the stack size of the item
     */
    @NonNegative int stackSize();

    /**
     * Gets the max damage of the item.
     *
     * @return the max damage of the item
     */
    int maxDamage();

    /**
     * Gets the attack damage of the item.
     * This is purely visual, and only applied to tools
     *
     * @return the attack damage of the item
     */
    int attackDamage();

    /**
     * Gets the tool type of the item.
     *
     * @return the tool type of the item
     */
    @Nullable String toolType();

    /**
     * @deprecated no longer used
     */
    @Deprecated(forRemoval = true)
    @Nullable String toolTier();

    /**
     * Gets the armor type of the item.
     *
     * @return the armor type of the item
     */
    @Nullable String armorType();

    /**
     * Gets the armor protection value of the item.
     *
     * @return the armor protection value of the item
     */
    int protectionValue();

    /**
     * Gets the item's translation string.
     *
     * @return the item's translation string
     */
    @Nullable String translationString();

    /**
     * @deprecated No longer used.
     */
    @Deprecated(forRemoval = true)
    @Nullable Set<String> repairMaterials();

    /**
     * Gets if the item is a hat. This is used to determine if the item should be rendered on the player's head, and
     * normally allow the player to equip it. This is not meant for armor.
     *
     * @return if the item is a hat
     */
    boolean isHat();

    /**
     * Gets if the item is a foil. This is used to determine if the item should be rendered with an enchantment glint effect.
     *
     * @return if the item is a foil
     */
    boolean isFoil();

    /**
     * Gets if the item is edible.
     *
     * @return if the item is edible
     */
    boolean isEdible();

    /**
     * Gets if the food item can always be eaten.
     *
     * @return if the item is allowed to be eaten all the time
     */
    boolean canAlwaysEat();

    /**
     * Gets if the item is chargable, like a bow.
     *
     * @return if the item should act like a chargable item
     */
    boolean isChargeable();

    /**
     * @deprecated Use {@link #displayHandheld()} instead.
     * Gets if the item is a tool. This is used to set the render type of the item, if the item is handheld.
     *
     * @return if the item is a tool
     */
    @Deprecated
    default boolean isTool() {
        return displayHandheld();
    }

    /**
     * Gets the block the item places.
     *
     * @return the block the item places
     */
    String block();

    static NonVanillaCustomItemData.Builder builder() {
        return GeyserApi.api().provider(NonVanillaCustomItemData.Builder.class);
    }

    @Override
    default CustomItemDefinition.Builder toDefinition(Identifier javaItem) {
        throw new UnsupportedOperationException("Use toDefinition()");
    }

    default NonVanillaCustomItemDefinition.Builder toDefinition() {
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
            .component(DataComponent.MAX_STACK_SIZE, stackSize())
            .component(DataComponent.MAX_DAMAGE, maxDamage())
            .component(GeyserDataComponent.ATTACK_DAMAGE, attackDamage())
            .translationString(translationString());

        if (isHat()) {
            definition.component(DataComponent.EQUIPPABLE, new Equippable(Equippable.EquipmentSlot.HEAD));
        } else if (armorType() != null) {
            switch (armorType()) {
                case "helmet" -> definition.component(DataComponent.EQUIPPABLE, new Equippable(Equippable.EquipmentSlot.HEAD));
                case "chestplate" -> definition.component(DataComponent.EQUIPPABLE, new Equippable(Equippable.EquipmentSlot.CHEST));
                case "leggings" -> definition.component(DataComponent.EQUIPPABLE, new Equippable(Equippable.EquipmentSlot.LEGS));
                case "boots" -> definition.component(DataComponent.EQUIPPABLE, new Equippable(Equippable.EquipmentSlot.FEET));
            }
        }

        if (isEdible()) {
            definition.component(DataComponent.CONSUMABLE, new Consumable(1.6F, Consumable.Animation.EAT)); // Default values
            if (canAlwaysEat()) {
                definition.component(DataComponent.FOOD, new FoodProperties(0, 0, true));
            }
        }

        if (isChargeable() && toolType() != null) {
            if (toolType().equals("bow")) {
                definition.component(GeyserDataComponent.CHARGEABLE, new Chargeable(1.0F, true, Identifier.of("arrow")));
            } else {
                definition.component(GeyserDataComponent.CHARGEABLE, new Chargeable(0.0F, false, Identifier.of("arrow")));
            }
        }

        if (block() != null) {
            definition.component(GeyserDataComponent.BLOCK_PLACER, new BlockPlacer(Identifier.of(block()), false));
        }

        return definition;
    }

    interface Builder extends CustomItemData.Builder {
        @Override
        Builder name(@NonNull String name);

        Builder identifier(@NonNull String identifier);

        Builder javaId(@NonNegative int javaId);

        Builder stackSize(@NonNegative int stackSize);

        Builder maxDamage(int maxDamage);

        Builder attackDamage(int attackDamage);

        Builder toolType(@Nullable String toolType);

        Builder toolTier(@Nullable String toolTier);

        Builder armorType(@Nullable String armorType);

        Builder protectionValue(int protectionValue);

        Builder translationString(@Nullable String translationString);

        Builder repairMaterials(@Nullable Set<String> repairMaterials);

        Builder hat(boolean isHat);

        Builder foil(boolean isFoil);

        Builder edible(boolean isEdible);

        Builder canAlwaysEat(boolean canAlwaysEat);

        Builder chargeable(boolean isChargeable);

        Builder block(String block);

        /**
         * @deprecated Use {@link #displayHandheld(boolean)} instead.
         */
        @Deprecated
        default Builder tool(boolean isTool) {
            return displayHandheld(isTool);
        }

        @Override
        Builder creativeCategory(int creativeCategory);

        @Override
        Builder creativeGroup(@Nullable String creativeGroup);

        @Override
        Builder customItemOptions(@NonNull CustomItemOptions customItemOptions);

        @Override
        Builder displayName(@NonNull String displayName);

        @Override
        Builder icon(@NonNull String icon);

        @Override
        Builder allowOffhand(boolean allowOffhand);

        @Override
        Builder displayHandheld(boolean displayHandheld);

        @Override
        Builder textureSize(int textureSize);

        @Override
        Builder renderOffsets(@Nullable CustomRenderOffsets renderOffsets);

        @Override
        Builder tags(@Nullable Set<String> tags);

        NonVanillaCustomItemData build();
    }
}
