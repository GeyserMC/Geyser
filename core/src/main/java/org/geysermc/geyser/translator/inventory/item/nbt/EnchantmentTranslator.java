/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.inventory.item.nbt;

import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.inventory.item.Enchantment;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ItemRemapper
public class EnchantmentTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        List<Tag> newTags = new ArrayList<>();
        Tag enchantmentTag = itemTag.get("Enchantments");
        if (enchantmentTag instanceof ListTag listTag) {
            for (Tag tag : listTag.getValue()) {
                if (!(tag instanceof CompoundTag)) continue;

                CompoundTag bedrockTag = remapEnchantment((CompoundTag) tag);
                newTags.add(bedrockTag);
            }
            itemTag.remove("Enchantments");
        }
        enchantmentTag = itemTag.get("StoredEnchantments");
        if (enchantmentTag instanceof ListTag listTag) {
            for (Tag tag : listTag.getValue()) {
                if (!(tag instanceof CompoundTag)) continue;

                CompoundTag bedrockTag = remapEnchantment((CompoundTag) tag);
                if (bedrockTag != null) {
                    bedrockTag.put(new ShortTag("GeyserStoredEnchantment", (short) 0));
                    newTags.add(bedrockTag);
                }
            }
            itemTag.remove("StoredEnchantments");
        }

        if (!newTags.isEmpty()) {
            itemTag.put(new ListTag("ench", newTags));
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        if (!itemTag.contains("ench")) {
            return;
        }

        ListTag enchantmentTag = itemTag.get("ench");
        List<Tag> enchantments = new ArrayList<>();
        List<Tag> storedEnchantments = new ArrayList<>();
        for (Tag value : enchantmentTag.getValue()) {
            if (!(value instanceof CompoundTag tagValue))
                continue;

            ShortTag bedrockId = tagValue.get("id");
            if (bedrockId == null) continue;

            ShortTag geyserStoredEnchantmentTag = tagValue.get("GeyserStoredEnchantment");

            Enchantment enchantment = Enchantment.getByBedrockId(bedrockId.getValue());
            if (enchantment != null) {
                CompoundTag javaTag = new CompoundTag("");
                Map<String, Tag> javaValue = javaTag.getValue();
                javaValue.put("id", new StringTag("id", enchantment.getJavaIdentifier()));
                ShortTag levelTag = tagValue.get("lvl");
                javaValue.put("lvl", new IntTag("lvl", levelTag != null ? levelTag.getValue() : 1));
                javaTag.setValue(javaValue);


                if (geyserStoredEnchantmentTag != null) {
                    tagValue.remove("GeyserStoredEnchantment");
                    storedEnchantments.add(javaTag);
                } else {
                    enchantments.add(javaTag);
                }
            } else {
                GeyserImpl.getInstance().getLogger().debug("Unknown bedrock enchantment: " + bedrockId);
            }
        }
        if (!enchantments.isEmpty()) {
            itemTag.put(new ListTag("Enchantments", enchantments));
        }
        if (!storedEnchantments.isEmpty()) {
            itemTag.put(new ListTag("StoredEnchantments", storedEnchantments));
        }
        itemTag.remove("ench");
    }


    private CompoundTag remapEnchantment(CompoundTag tag) {
        Tag javaEnchLvl = tag.get("lvl");
        if (!(javaEnchLvl instanceof ShortTag || javaEnchLvl instanceof IntTag))
            return null;

        Tag javaEnchId = tag.get("id");
        if (!(javaEnchId instanceof StringTag))
            return null;

        Enchantment enchantment = Enchantment.getByJavaIdentifier(((StringTag) javaEnchId).getValue());
        if (enchantment == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown java enchantment: " + javaEnchId.getValue());
            return null;
        }

        CompoundTag bedrockTag = new CompoundTag("");
        bedrockTag.put(new ShortTag("id", (short) enchantment.ordinal()));
        bedrockTag.put(new ShortTag("lvl", ((Number) javaEnchLvl.getValue()).shortValue()));
        return bedrockTag;
    }

}
