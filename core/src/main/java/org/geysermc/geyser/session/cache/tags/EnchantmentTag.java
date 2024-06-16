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

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.util.Ordered;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class EnchantmentTag implements Ordered {
    public static final Map<Key, EnchantmentTag> ALL_ENCHANTMENT_TAGS = new HashMap<>();

    public static final EnchantmentTag TOOLTIP_ORDER = new EnchantmentTag("tooltip_order");
    public static final EnchantmentTag EXCLUSIVE_SET_ARMOR = new EnchantmentTag("exclusive_set/armor");
    public static final EnchantmentTag EXCLUSIVE_SET_BOOTS = new EnchantmentTag("exclusive_set/boots");
    public static final EnchantmentTag EXCLUSIVE_SET_BOW = new EnchantmentTag("exclusive_set/bow");
    public static final EnchantmentTag EXCLUSIVE_SET_CROSSBOW = new EnchantmentTag("exclusive_set/crossbow");
    public static final EnchantmentTag EXCLUSIVE_SET_DAMAGE = new EnchantmentTag("exclusive_set/damage");
    public static final EnchantmentTag EXCLUSIVE_SET_MINING = new EnchantmentTag("exclusive_set/mining");
    public static final EnchantmentTag EXCLUSIVE_SET_RIPTIDE = new EnchantmentTag("exclusive_set/riptide");
    public static final EnchantmentTag TRADEABLE = new EnchantmentTag("tradeable");
    public static final EnchantmentTag DOUBLE_TRADE_PRICE = new EnchantmentTag("double_trade_price");
    public static final EnchantmentTag IN_ENCHANTING_TABLE = new EnchantmentTag("in_enchanting_table");
    public static final EnchantmentTag ON_MOB_SPAWN_EQUIPMENT = new EnchantmentTag("on_mob_spawn_equipment");
    public static final EnchantmentTag ON_TRADED_EQUIPMENT = new EnchantmentTag("on_traded_equipment");
    public static final EnchantmentTag ON_RANDOM_LOOT = new EnchantmentTag("on_random_loot");
    public static final EnchantmentTag CURSE = new EnchantmentTag("curse");
    public static final EnchantmentTag SMELTS_LOOT = new EnchantmentTag("smelts_loot");
    public static final EnchantmentTag PREVENTS_BEE_SPAWNS_WHEN_MINING = new EnchantmentTag("prevents_bee_spawns_when_mining");
    public static final EnchantmentTag PREVENTS_DECORATED_POT_SHATTERING = new EnchantmentTag("prevents_decorated_pot_shattering");
    public static final EnchantmentTag PREVENTS_ICE_MELTING = new EnchantmentTag("prevents_ice_melting");
    public static final EnchantmentTag PREVENTS_INFESTED_SPAWNS = new EnchantmentTag("prevents_infested_spawns");
    public static final EnchantmentTag TREASURE = new EnchantmentTag("treasure");
    public static final EnchantmentTag NON_TREASURE = new EnchantmentTag("non_treasure");
    public static final EnchantmentTag TRADES_DESERT_COMMON = new EnchantmentTag("trades/desert_common");
    public static final EnchantmentTag TRADES_JUNGLE_COMMON = new EnchantmentTag("trades/jungle_common");
    public static final EnchantmentTag TRADES_PLAINS_COMMON = new EnchantmentTag("trades/plains_common");
    public static final EnchantmentTag TRADES_SAVANNA_COMMON = new EnchantmentTag("trades/savanna_common");
    public static final EnchantmentTag TRADES_SNOW_COMMON = new EnchantmentTag("trades/snow_common");
    public static final EnchantmentTag TRADES_SWAMP_COMMON = new EnchantmentTag("trades/swamp_common");
    public static final EnchantmentTag TRADES_TAIGA_COMMON = new EnchantmentTag("trades/taiga_common");
    public static final EnchantmentTag TRADES_DESERT_SPECIAL = new EnchantmentTag("trades/desert_special");
    public static final EnchantmentTag TRADES_JUNGLE_SPECIAL = new EnchantmentTag("trades/jungle_special");
    public static final EnchantmentTag TRADES_PLAINS_SPECIAL = new EnchantmentTag("trades/plains_special");
    public static final EnchantmentTag TRADES_SAVANNA_SPECIAL = new EnchantmentTag("trades/savanna_special");
    public static final EnchantmentTag TRADES_SNOW_SPECIAL = new EnchantmentTag("trades/snow_special");
    public static final EnchantmentTag TRADES_SWAMP_SPECIAL = new EnchantmentTag("trades/swamp_special");
    public static final EnchantmentTag TRADES_TAIGA_SPECIAL = new EnchantmentTag("trades/taiga_special");

    private final int id;

    private EnchantmentTag(String identifier) {
        this.id = ALL_ENCHANTMENT_TAGS.size();
        register(identifier, this);
    }

    @Override
    public int ordinal() {
        return id;
    }

    private static void register(String name, EnchantmentTag tag) {
        ALL_ENCHANTMENT_TAGS.put(MinecraftKey.key(name), tag);
    }
}
