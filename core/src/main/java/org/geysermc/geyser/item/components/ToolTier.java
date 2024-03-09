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

package org.geysermc.geyser.item.components;

import com.google.common.base.Suppliers;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public enum ToolTier {
    WOODEN(2, () -> Set.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS, Items.MANGROVE_PLANKS)), // PLANKS tag // TODO ?
    STONE(4, () -> Set.of(Items.COBBLESTONE, Items.BLACKSTONE, Items.COBBLED_DEEPSLATE)), // STONE_TOOL_MATERIALS tag
    IRON(6, () -> Collections.singleton(Items.IRON_INGOT)),
    GOLDEN(12, () -> Collections.singleton(Items.GOLD_INGOT)),
    DIAMOND(8, () -> Collections.singleton(Items.DIAMOND)),
    NETHERITE(9, () -> Collections.singleton(Items.NETHERITE_INGOT));

    private static final ToolTier[] VALUES = values();

    private final int speed;
    private final Supplier<Set<Item>> repairIngredients;

    ToolTier(int speed, Supplier<Set<Item>> repairIngredients) {
        this.speed = speed;
        // Lazily initialize as this will likely be called as items are loading
        this.repairIngredients = Suppliers.memoize(repairIngredients::get);
    }

    public int getSpeed() {
        return speed;
    }

    public Set<Item> getRepairIngredients() {
        return repairIngredients.get();
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
