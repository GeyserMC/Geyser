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

package org.geysermc.geyser.translator.inventory.item;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.List;
import java.util.OptionalInt;

/**
 * This is only a separate class for testing purposes so we don't have to load in GeyserImpl in ItemTranslator.
 */
final class CustomItemTranslator {

    static int getCustomItem(CompoundTag nbt, ItemMapping mapping) {
        if (nbt == null) {
            return -1;
        }
        List<ObjectIntPair<CustomItemOptions>> customMappings = mapping.getCustomItemOptions();
        if (customMappings.isEmpty()) {
            return -1;
        }

        int customModelData = nbt.get("CustomModelData") instanceof IntTag customModelDataTag ? customModelDataTag.getValue() : 0;
        boolean checkDamage = mapping.getMaxDamage() > 0;
        int damage = !checkDamage ? 0 : nbt.get("Damage") instanceof IntTag damageTag ? damageTag.getValue() : 0;
        boolean unbreakable = checkDamage && !isDamaged(nbt, damage);

        for (ObjectIntPair<CustomItemOptions> mappingTypes : customMappings) {
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
            if (customModelDataOption.isPresent() && customModelData < customModelDataOption.getAsInt()) {
                continue;
            }

            return mappingTypes.valueInt();
        }
        return -1;
    }

    /* These two functions are based off their Mojmap equivalents from 1.19.2 */

    private static boolean isDamaged(CompoundTag nbt, int damage) {
        return isDamagableItem(nbt) && damage > 0;
    }

    private static boolean isDamagableItem(CompoundTag nbt) {
        // mapping.getMaxDamage > 0 should also be checked (return false if not true) but we already check prior to this function
        Tag unbreakableTag = nbt.get("Unbreakable");
        // Tag must either not be present or be set to false
        return unbreakableTag == null || !(unbreakableTag.getValue() instanceof Number number) || number.byteValue() == 0;
    }

    private CustomItemTranslator() {
    }
}
