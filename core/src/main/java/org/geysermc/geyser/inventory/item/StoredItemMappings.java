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

package org.geysermc.geyser.inventory.item;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Map;

/**
 * A class to have easy access to specific item mappings per-version.
 */
@Getter
@Accessors(fluent = true)
public class StoredItemMappings {
    private final ItemMapping bamboo;
    private final ItemMapping banner;
    private final ItemMapping barrier;
    private final ItemMapping compass;
    private final ItemMapping crossbow;
    private final ItemMapping egg;
    private final ItemMapping glassBottle;
    private final ItemMapping milkBucket;
    private final ItemMapping powderSnowBucket;
    private final ItemMapping shield;
    private final ItemMapping upgradeTemplate;
    private final ItemMapping wheat;
    private final ItemMapping writableBook;

    public StoredItemMappings(Map<Item, ItemMapping> itemMappings) {
        this.bamboo = load(itemMappings, Items.BAMBOO);
        this.banner = load(itemMappings, Items.WHITE_BANNER); // As of 1.17.10, all banners have the same Bedrock ID
        this.barrier = load(itemMappings, Items.BARRIER);
        this.compass = load(itemMappings, Items.COMPASS);
        this.crossbow = load(itemMappings, Items.CROSSBOW);
        this.egg = load(itemMappings, Items.EGG);
        this.glassBottle = load(itemMappings, Items.GLASS_BOTTLE);
        this.milkBucket = load(itemMappings, Items.MILK_BUCKET);
        this.powderSnowBucket = load(itemMappings, Items.POWDER_SNOW_BUCKET);
        this.shield = load(itemMappings, Items.SHIELD);
        this.upgradeTemplate = load(itemMappings, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        this.wheat = load(itemMappings, Items.WHEAT);
        this.writableBook = load(itemMappings, Items.WRITABLE_BOOK);
    }

    @NonNull
    private ItemMapping load(Map<Item, ItemMapping> itemMappings, Item item) {
        ItemMapping mapping = itemMappings.get(item);
        if (mapping == null) {
            throw new RuntimeException("Could not find item " + item.javaIdentifier());
        }

        return mapping;
    }
}
