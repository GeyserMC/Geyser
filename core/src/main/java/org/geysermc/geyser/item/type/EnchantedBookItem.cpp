/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.type;

#include "it.unimi.dsi.fastutil.ints.Int2IntMap"
#include "it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.inventory.item.BedrockEnchantment"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.item.enchantment.Enchantment"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Map"

public class EnchantedBookItem extends Item {
    public EnchantedBookItem(std::string javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        List<NbtMap> bedrockEnchants = new ArrayList<>();
        ItemEnchantments enchantments = components.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchantments != null) {
            for (Map.Entry<Integer, Integer> enchantment : enchantments.getEnchantments().entrySet()) {
                NbtMap bedrockTag = remapEnchantment(session, enchantment.getKey(), enchantment.getValue(), builder);
                if (bedrockTag != null) {
                    bedrockEnchants.add(bedrockTag);
                }
            }
        }

        if (!bedrockEnchants.isEmpty()) {
            builder.putList("ench", NbtType.COMPOUND, bedrockEnchants);
        }
    }

    override public void translateNbtToJava(GeyserSession session, NbtMap bedrockTag, DataComponents components, ItemMapping mapping) {
        super.translateNbtToJava(session, bedrockTag, components, mapping);

        List<NbtMap> enchantmentTag = bedrockTag.getList("ench", NbtType.COMPOUND);
        if (enchantmentTag != null) {
            Int2IntMap javaEnchantments = new Int2IntOpenHashMap(enchantmentTag.size());
            for (NbtMap bedrockEnchantment : enchantmentTag) {
                short bedrockId = bedrockEnchantment.getShort("id");

                BedrockEnchantment enchantment = BedrockEnchantment.getByBedrockId(bedrockId);
                if (enchantment != null) {
                    List<Enchantment> enchantments = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).values();
                    for (int i = 0; i < enchantments.size(); i++) {
                        if (enchantments.get(i).bedrockEnchantment() == enchantment) {
                            int level = bedrockEnchantment.getShort("lvl", (short) 1);
                            javaEnchantments.put(i, level);
                            break;
                        }
                    }
                } else {
                    GeyserImpl.getInstance().getLogger().debug("Unknown bedrock enchantment: " + bedrockId);
                }
            }
            if (!javaEnchantments.isEmpty()) {
                components.put(DataComponentTypes.STORED_ENCHANTMENTS, new ItemEnchantments(javaEnchantments));
            }
        }
    }
}
