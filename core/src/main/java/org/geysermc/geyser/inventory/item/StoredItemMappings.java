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
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMapping;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A class to have easy access to specific item mappings per-version.
 */
@Getter
@Accessors(fluent = true)
public class StoredItemMappings {
    private final ItemDefinition amethystShard;
    private final ItemMapping bamboo;
    private final ItemMapping banner;
    private final ItemMapping barrier;
    private final ItemDefinition bowl;
    private final ItemDefinition bucket;
    private final ItemDefinition chest;
    private final ItemMapping compass;
    private final ItemMapping crossbow;
    private final ItemMapping enchantedBook;
    private final ItemMapping fishingRod;
    private final ItemDefinition flintAndSteel;
    private final ItemDefinition frogspawn;
    private final ItemDefinition goatHorn;
    private final ItemDefinition glassBottle;
    private final ItemDefinition goldenApple;
    private final ItemDefinition goldIngot;
    private final ItemDefinition ironIngot;
    private final ItemDefinition lead;
    private final ItemDefinition lilyPad;
    private final ItemMapping milkBucket;
    private final ItemDefinition nameTag;
    private final ItemMapping powderSnowBucket;
    private final ItemMapping playerHead;
    private final ItemMapping egg;
    private final ItemDefinition saddle;
    private final ItemDefinition shears;
    private final ItemMapping shield;
    private final ItemDefinition slimeBall;
    private final ItemDefinition waterBucket;
    private final ItemMapping wheat;
    private final ItemMapping writableBook;

    public StoredItemMappings(Map<String, ItemMapping> itemMappings) {
        this.amethystShard = load(itemMappings, "amethyst_shard").getBedrockDefinition();
        this.bamboo = load(itemMappings, "bamboo");
        this.banner = load(itemMappings, "white_banner"); // As of 1.17.10, all banners have the same Bedrock ID
        this.barrier = load(itemMappings, "barrier");
        this.bowl = load(itemMappings, "bowl").getBedrockDefinition();
        this.bucket = load(itemMappings, "bucket").getBedrockDefinition();
        this.chest = load(itemMappings, "chest").getBedrockDefinition();
        this.compass = load(itemMappings, "compass");
        this.crossbow = load(itemMappings, "crossbow");
        this.enchantedBook = load(itemMappings, "enchanted_book");
        this.fishingRod = load(itemMappings, "fishing_rod");
        this.flintAndSteel = load(itemMappings, "flint_and_steel").getBedrockDefinition();
        this.frogspawn = load(itemMappings, "frogspawn").getBedrockDefinition();
        this.goatHorn = load(itemMappings, "goat_horn").getBedrockDefinition();
        this.glassBottle = load(itemMappings, "glass_bottle").getBedrockDefinition();
        this.goldenApple = load(itemMappings, "golden_apple").getBedrockDefinition();
        this.goldIngot = load(itemMappings, "gold_ingot").getBedrockDefinition();
        this.ironIngot = load(itemMappings, "iron_ingot").getBedrockDefinition();
        this.lead = load(itemMappings, "lead").getBedrockDefinition();
        this.lilyPad = load(itemMappings, "lily_pad").getBedrockDefinition();
        this.milkBucket = load(itemMappings, "milk_bucket");
        this.nameTag = load(itemMappings, "name_tag").getBedrockDefinition();
        this.powderSnowBucket = load(itemMappings, "powder_snow_bucket");
        this.playerHead = load(itemMappings, "player_head");
        this.egg = load(itemMappings, "egg");
        this.saddle = load(itemMappings, "saddle").getBedrockDefinition();
        this.shears = load(itemMappings, "shears").getBedrockDefinition();
        this.shield = load(itemMappings, "shield");
        this.slimeBall = load(itemMappings, "slime_ball").getBedrockDefinition();
        this.waterBucket = load(itemMappings, "water_bucket").getBedrockDefinition();
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
