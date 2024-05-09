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

package org.geysermc.geyser.session.cache.tags;

import org.geysermc.geyser.session.cache.TagCache;

public enum ItemTag {
    AXOLOTL_FOOD("axolotl_food"),
    CREEPER_IGNITERS("creeper_igniters"),
    FISHES("fishes"),
    FOX_FOOD("fox_food"),
    PIGLIN_LOVED("piglin_loved"),
    SMALL_FLOWERS("small_flowers"),
    SNIFFER_FOOD("sniffer_food"),
    PIGLIN_FOOD("piglin_food"),
    COW_FOOD("cow_food"),
    GOAT_FOOD("goat_food"),
    SHEEP_FOOD("sheep_food"),
    WOLF_FOOD("wolf_food"),
    CAT_FOOD("cat_food"),
    HORSE_FOOD("horse_food"),
    CAMEL_FOOD("camel_food"),
    ARMADILLO_FOOD("armadillo_food"),
    BEE_FOOD("bee_food"),
    CHICKEN_FOOD("chicken_food"),
    FROG_FOOD("frog_food"),
    HOGLIN_FOOD("hoglin_food"),
    LLAMA_FOOD("llama_food"),
    OCELOT_FOOD("ocelot_food"),
    PANDA_FOOD("panda_food"),
    PIG_FOOD("pig_food"),
    RABBIT_FOOD("rabbit_food"),
    STRIDER_FOOD("strider_food"),
    TURTLE_FOOD("turtle_food"),
    PARROT_FOOD("parrot_food"),
    PARROT_POISONOUS_FOOD("parrot_poisonous_food");
    
    ItemTag(String identifier) {
        register(identifier, this);
    }

    private static void register(String name, ItemTag tag) {
        TagCache.ALL_ITEM_TAGS.put("minecraft:" + name, tag);
    }
}
