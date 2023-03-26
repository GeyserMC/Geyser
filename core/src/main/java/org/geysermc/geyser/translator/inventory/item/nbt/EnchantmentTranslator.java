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

package org.geysermc.geyser.translator.inventory.item.nbt;

import com.github.steveice10.mc.protocol.data.game.Identifier;
import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.Enchantment;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ItemRemapper
public class EnchantmentTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        List<Tag> newTags = new ArrayList<>();
        Tag enchantmentTag = itemTag.remove("Enchantments");
        if (enchantmentTag instanceof ListTag listTag) {
            for (Tag tag : listTag.getValue()) {
                if (!(tag instanceof CompoundTag)) continue;
                CompoundTag bedrockTag = remapEnchantment(session, (CompoundTag) tag, itemTag);
                if (bedrockTag != null) {
                    newTags.add(bedrockTag);
                }
            }
        }

        // TODO consolidate this into EnchantedBookTranslator
        enchantmentTag = itemTag.remove("StoredEnchantments");
        if (enchantmentTag instanceof ListTag listTag) {
            for (Tag tag : listTag.getValue()) {
                if (!(tag instanceof CompoundTag)) continue;
                CompoundTag bedrockTag = remapEnchantment(session, (CompoundTag) tag, itemTag);
                if (bedrockTag != null) {
                    newTags.add(bedrockTag);
                }
            }
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


    private CompoundTag remapEnchantment(GeyserSession session, CompoundTag tag, CompoundTag rootTag) {
        Tag javaEnchId = tag.get("id");
        if (!(javaEnchId instanceof StringTag))
            return null;

        Enchantment enchantment = Enchantment.getByJavaIdentifier(((StringTag) javaEnchId).getValue());
        if (enchantment == null) {
            if (Identifier.formalize((String) javaEnchId.getValue()).equals("minecraft:sweeping")) {
                Tag javaEnchLvl = tag.get("lvl");
                int sweepingLvl = javaEnchLvl != null && javaEnchLvl.getValue() instanceof Number lvl ? lvl.intValue() : 0;

                addSweeping(session, rootTag, sweepingLvl);
                return null;
            }
            GeyserImpl.getInstance().getLogger().debug("Unknown Java enchantment while NBT item translating: " + javaEnchId.getValue());
            return null;
        }

        Tag javaEnchLvl = tag.get("lvl");

        CompoundTag bedrockTag = new CompoundTag("");
        bedrockTag.put(new ShortTag("id", (short) enchantment.ordinal()));
        // If the tag cannot parse, Java Edition 1.18.2 sets to 0
        bedrockTag.put(new ShortTag("lvl", javaEnchLvl != null && javaEnchLvl.getValue() instanceof Number lvl ? lvl.shortValue() : (short) 0));
        return bedrockTag;
    }

    private void addSweeping(GeyserSession session, CompoundTag itemTag, int level) {
        CompoundTag displayTag = itemTag.get("display");
        if (displayTag == null) {
            displayTag = new CompoundTag("display");
            itemTag.put(displayTag);
        }
        ListTag loreTag = displayTag.get("Lore");
        if (loreTag == null) {
            loreTag = new ListTag("Lore");
            displayTag.put(loreTag);
        }

        String sweepingTranslation = MinecraftLocale.getLocaleString("enchantment.minecraft.sweeping", session.locale());
        String lvlTranslation = MinecraftLocale.getLocaleString("enchantment.level." + level, session.locale());

        loreTag.add(new StringTag("", ChatColor.RESET + ChatColor.GRAY + sweepingTranslation + " " + lvlTranslation));
    }
}