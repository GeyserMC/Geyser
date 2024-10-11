/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.util.MinecraftKey;

/**
 * Lists vanilla enchantment tags.
 */
@SuppressWarnings("unused")
public final class EnchantmentTag {
    public static final Tag<Enchantment> TOOLTIP_ORDER = register("tooltip_order");
    public static final Tag<Enchantment> EXCLUSIVE_SET_ARMOR = register("exclusive_set/armor");
    public static final Tag<Enchantment> EXCLUSIVE_SET_BOOTS = register("exclusive_set/boots");
    public static final Tag<Enchantment> EXCLUSIVE_SET_BOW = register("exclusive_set/bow");
    public static final Tag<Enchantment> EXCLUSIVE_SET_CROSSBOW = register("exclusive_set/crossbow");
    public static final Tag<Enchantment> EXCLUSIVE_SET_DAMAGE = register("exclusive_set/damage");
    public static final Tag<Enchantment> EXCLUSIVE_SET_MINING = register("exclusive_set/mining");
    public static final Tag<Enchantment> EXCLUSIVE_SET_RIPTIDE = register("exclusive_set/riptide");
    public static final Tag<Enchantment> TRADEABLE = register("tradeable");
    public static final Tag<Enchantment> DOUBLE_TRADE_PRICE = register("double_trade_price");
    public static final Tag<Enchantment> IN_ENCHANTING_TABLE = register("in_enchanting_table");
    public static final Tag<Enchantment> ON_MOB_SPAWN_EQUIPMENT = register("on_mob_spawn_equipment");
    public static final Tag<Enchantment> ON_TRADED_EQUIPMENT = register("on_traded_equipment");
    public static final Tag<Enchantment> ON_RANDOM_LOOT = register("on_random_loot");
    public static final Tag<Enchantment> CURSE = register("curse");
    public static final Tag<Enchantment> SMELTS_LOOT = register("smelts_loot");
    public static final Tag<Enchantment> PREVENTS_BEE_SPAWNS_WHEN_MINING = register("prevents_bee_spawns_when_mining");
    public static final Tag<Enchantment> PREVENTS_DECORATED_POT_SHATTERING = register("prevents_decorated_pot_shattering");
    public static final Tag<Enchantment> PREVENTS_ICE_MELTING = register("prevents_ice_melting");
    public static final Tag<Enchantment> PREVENTS_INFESTED_SPAWNS = register("prevents_infested_spawns");
    public static final Tag<Enchantment> TREASURE = register("treasure");
    public static final Tag<Enchantment> NON_TREASURE = register("non_treasure");
    public static final Tag<Enchantment> TRADES_DESERT_COMMON = register("trades/desert_common");
    public static final Tag<Enchantment> TRADES_JUNGLE_COMMON = register("trades/jungle_common");
    public static final Tag<Enchantment> TRADES_PLAINS_COMMON = register("trades/plains_common");
    public static final Tag<Enchantment> TRADES_SAVANNA_COMMON = register("trades/savanna_common");
    public static final Tag<Enchantment> TRADES_SNOW_COMMON = register("trades/snow_common");
    public static final Tag<Enchantment> TRADES_SWAMP_COMMON = register("trades/swamp_common");
    public static final Tag<Enchantment> TRADES_TAIGA_COMMON = register("trades/taiga_common");
    public static final Tag<Enchantment> TRADES_DESERT_SPECIAL = register("trades/desert_special");
    public static final Tag<Enchantment> TRADES_JUNGLE_SPECIAL = register("trades/jungle_special");
    public static final Tag<Enchantment> TRADES_PLAINS_SPECIAL = register("trades/plains_special");
    public static final Tag<Enchantment> TRADES_SAVANNA_SPECIAL = register("trades/savanna_special");
    public static final Tag<Enchantment> TRADES_SNOW_SPECIAL = register("trades/snow_special");
    public static final Tag<Enchantment> TRADES_SWAMP_SPECIAL = register("trades/swamp_special");
    public static final Tag<Enchantment> TRADES_TAIGA_SPECIAL = register("trades/taiga_special");

    private EnchantmentTag() {}

    private static Tag<Enchantment> register(String name) {
        return JavaRegistries.ENCHANTMENT.registerVanillaTag(MinecraftKey.key(name));
    }

    public static void init() {
        // no-op
    }
}
