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
import org.geysermc.geyser.registry.type.ItemMapping;

import javax.annotation.Nonnull;
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
    private final int bowl;
    private final int chest;
    private final ItemMapping compass;
    private final ItemMapping crossbow;
    private final ItemMapping enchantedBook;
    private final ItemMapping fishingRod;
    private final int flintAndSteel;
    private final int goldenApple;
    private final int goldIngot;
    private final int ironIngot;
    private final int lead;
    private final ItemMapping lodestoneCompass;
    private final ItemMapping milkBucket;
    private final int nameTag;
    private final ItemMapping powderSnowBucket;
    private final ItemMapping playerHead;
    private final ItemMapping egg;
    private final int saddle;
    private final int shears;
    private final ItemMapping shield;
    private final int waterBucket;
    private final ItemMapping wheat;
    private final ItemMapping writableBook;

    public StoredItemMappings(Map<String, ItemMapping> itemMappings) {
        this.bamboo = load(itemMappings, "bamboo");
        this.banner = load(itemMappings, "white_banner"); // As of 1.17.10, all banners have the same Bedrock ID
        this.barrier = load(itemMappings, "barrier");
        this.bowl = load(itemMappings, "bowl").getJavaId();
        this.chest = load(itemMappings, "chest").getJavaId();
        this.compass = load(itemMappings, "compass");
        this.crossbow = load(itemMappings, "crossbow");
        this.enchantedBook = load(itemMappings, "enchanted_book");
        this.fishingRod = load(itemMappings, "fishing_rod");
        this.flintAndSteel = load(itemMappings, "flint_and_steel").getJavaId();
        this.goldenApple = load(itemMappings, "golden_apple").getJavaId();
        this.goldIngot = load(itemMappings, "gold_ingot").getJavaId();
        this.ironIngot = load(itemMappings, "iron_ingot").getJavaId();
        this.lead = load(itemMappings, "lead").getJavaId();
        this.lodestoneCompass = load(itemMappings, "lodestone_compass");
        this.milkBucket = load(itemMappings, "milk_bucket");
        this.nameTag = load(itemMappings, "name_tag").getJavaId();
        this.powderSnowBucket = load(itemMappings, "powder_snow_bucket");
        this.playerHead = load(itemMappings, "player_head");
        this.egg = load(itemMappings, "egg");
        this.saddle = load(itemMappings, "saddle").getJavaId();
        this.shears = load(itemMappings, "shears").getJavaId();
        this.shield = load(itemMappings, "shield");
        this.waterBucket = load(itemMappings, "water_bucket").getJavaId();
        this.wheat = load(itemMappings, "wheat");
        this.writableBook = load(itemMappings, "writable_book");
    }

    @Nonnull
    private ItemMapping load(Map<String, ItemMapping> itemMappings, String cleanIdentifier) {
        ItemMapping mapping = itemMappings.get("minecraft:" + cleanIdentifier);
        if (mapping == null) {
            throw new RuntimeException("Could not find item " + cleanIdentifier);
        }

        return mapping;
    }
}
