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

package org.geysermc.geyser.translator.item;

import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.List;
import java.util.OptionalInt;

/**
 * This is only a separate class for testing purposes so we don't have to load in GeyserImpl in ItemTranslator.
 */
public final class CustomItemTranslator {

    @Nullable
    public static ItemDefinition getCustomItem(DataComponents components, ItemMapping mapping) {
        if (components == null) {
            return null;
        }
        List<Pair<CustomItemOptions, ItemDefinition>> customMappings = mapping.getCustomItemOptions();
        if (customMappings.isEmpty()) {
            return null;
        }

        // TODO 1.21.4
        float customModelDataInt = 0;
        CustomModelData customModelData = components.get(DataComponentType.CUSTOM_MODEL_DATA);
        if (customModelData != null) {
            if (!customModelData.floats().isEmpty()) {
                customModelDataInt = customModelData.floats().get(0);
            }
        }

        boolean checkDamage = mapping.getJavaItem().defaultMaxDamage() > 0;
        int damage = !checkDamage ? 0 : components.getOrDefault(DataComponentType.DAMAGE, 0);
        boolean unbreakable = checkDamage && !isDamaged(components, damage);

        for (Pair<CustomItemOptions, ItemDefinition> mappingTypes : customMappings) {
            CustomItemOptions options = mappingTypes.key();

            // Code note: there may be two or more conditions that a custom item must follow, hence the "continues"
            // here with the return at the end.

            // Implementation details: Java's predicate system works exclusively on comparing float numbers.
            // A value doesn't necessarily have to match 100%; it just has to be the first to meet all predicate conditions.
            // This is also why the order of iteration is important as the first to match will be the chosen display item.
            // For example, if CustomModelData is set to 2f as the requirement, then the NBT can be any number greater or equal (2, 3, 4...)
            // The same behavior exists for Damage (in fraction form instead of whole numbers),
            // and Damaged/Unbreakable handles no damage as 0f and damaged as 1f.

            if (checkDamage) {
                if (unbreakable && options.unbreakable() == TriState.FALSE) {
                    continue;
                }

                OptionalInt damagePredicate = options.damagePredicate();
                if (damagePredicate.isPresent() && damage < damagePredicate.getAsInt()) {
                    continue;
                }
            } else {
                if (options.unbreakable() != TriState.NOT_SET || options.damagePredicate().isPresent()) {
                    // These will never match on this item. 1.19.2 behavior
                    // Maybe move this to CustomItemRegistryPopulator since it'll be the same for every item? If so, add a test.
                    continue;
                }
            }

            OptionalInt customModelDataOption = options.customModelData();
            if (customModelDataOption.isPresent() && customModelDataInt < customModelDataOption.getAsInt()) {
                continue;
            }

            if (options.defaultItem()) {
                return null;
            }

            return mappingTypes.value();
        }

        return null;
    }

    /* These two functions are based off their Mojmap equivalents from 1.19.2 */

    private static boolean isDamaged(DataComponents components, int damage) {
        return isDamagableItem(components) && damage > 0;
    }

    private static boolean isDamagableItem(DataComponents components) {
        // mapping.getMaxDamage > 0 should also be checked (return false if not true) but we already check prior to this function
        Boolean unbreakable = components.get(DataComponentType.UNBREAKABLE);
        // Tag must either not be present or be set to false
        return unbreakable == null || !unbreakable;
    }

    private CustomItemTranslator() {
    }
}
