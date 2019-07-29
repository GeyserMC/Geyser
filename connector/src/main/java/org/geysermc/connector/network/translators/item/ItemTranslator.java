/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.DoubleTag;
import com.github.steveice10.opennbt.tag.builtin.FloatTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.LongArrayTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.connector.utils.Toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemTranslator {

    private static Map<String, String> identifiers = new HashMap<String, String>();

    static {
        // Key: java translation
        // Value: bedrock translation
        identifiers.put("grass_block", "grass");
        identifiers.put("granite", "stone:1");
        identifiers.put("polished_granite", "stone:2");
        identifiers.put("diorite", "stone:3");
        identifiers.put("polished_diorite", "stone:4");
        identifiers.put("andesite", "stone:5");
        identifiers.put("polished_andesite", "stone:6");

        identifiers.put("oak_log", "log");
        identifiers.put("spruce_log", "log:1");
        identifiers.put("birch_log", "log:2");
        identifiers.put("jungle_log", "log:3");
        identifiers.put("acacia_log", "log:4");
        identifiers.put("dark_oak_log", "log:5");

        identifiers.put("oak_planks", "planks");
        identifiers.put("spruce_planks", "planks:1");
        identifiers.put("birch_planks", "planks:2");
        identifiers.put("jungle_planks", "planks:3");
        identifiers.put("acacia_planks", "planks:4");
        identifiers.put("dark_oak_planks", "planks:5");

        identifiers.put("white_wool", "wool");
        identifiers.put("orange_wool", "wool:1");
        identifiers.put("magenta_wool", "wool:2");
        identifiers.put("light_blue_wool", "wool:3");
        identifiers.put("yellow_wool", "wool:4");
        identifiers.put("lime_wool", "wool:5");
        identifiers.put("pink_wool", "wool:6");
        identifiers.put("gray_wool", "wool:7");
        identifiers.put("light_gray_wool", "wool:8");
        identifiers.put("cyan_wool", "wool:9");
        identifiers.put("purple_wool", "wool:10");
        identifiers.put("blue_wool", "wool:11");
        identifiers.put("brown_wool", "wool:12");
        identifiers.put("green_wool", "wool:13");
        identifiers.put("red_wool", "wool:14");
        identifiers.put("black_wool", "wool:15");

        identifiers.put("white_carpet", "carpet");
        identifiers.put("orange_carpet", "carpet:1");
        identifiers.put("magenta_carpet", "carpet:2");
        identifiers.put("light_blue_carpet", "carpet:3");
        identifiers.put("yellow_carpet", "carpet:4");
        identifiers.put("lime_carpet", "carpet:5");
        identifiers.put("pink_carpet", "carpet:6");
        identifiers.put("gray_carpet", "carpet:7");
        identifiers.put("light_gray_carpet", "carpet:8");
        identifiers.put("cyan_carpet", "carpet:9");
        identifiers.put("purple_carpet", "carpet:10");
        identifiers.put("blue_carpet", "carpet:11");
        identifiers.put("brown_carpet", "carpet:12");
        identifiers.put("green_carpet", "carpet:13");
        identifiers.put("red_carpet", "carpet:14");
        identifiers.put("black_carpet", "carpet:15");
    }

    public ItemStack translateToJava(ItemData data) {
        JavaItem javaItem = getJavaItem(data);

        if (data.getTag() == null) {
            return new ItemStack(javaItem.getId(), data.getCount());
        }
        return new ItemStack(javaItem.getId(), data.getCount(), translateToJavaNBT(data.getTag()));
    }

    public ItemData translateToBedrock(ItemStack stack) {
        // Most likely air if null
        if (stack == null) {
            return ItemData.AIR;
        }

        BedrockItem bedrockItem = getBedrockItem(stack);
        if (stack.getNBT() == null) {
            return ItemData.of(bedrockItem.getId(), (short) bedrockItem.getData(), stack.getAmount());
        }
        return ItemData.of(bedrockItem.getId(), (short) bedrockItem.getData(), stack.getAmount(), translateToBedrockNBT(stack.getNBT()));
    }

    public BedrockItem getBedrockItem(ItemStack stack) {
       for (Map.Entry<String, JavaItem> javaItems : Toolbox.JAVA_ITEMS.entrySet()) {
           if (javaItems.getValue().getId() != stack.getId())
               continue;

           JavaItem javaItem = javaItems.getValue();
           String identifier = getBedrockIdentifier(javaItem.getIdentifier().replace("minecraft:", ""));
           if (!Toolbox.BEDROCK_ITEMS.containsKey(identifier))
                continue;

           return Toolbox.BEDROCK_ITEMS.get(identifier);
       }

       return new BedrockItem("minecraft:air", 0, 0);
    }

    public JavaItem getJavaItem(ItemData data) {
        for (Map.Entry<String, BedrockItem> bedrockItems : Toolbox.BEDROCK_ITEMS.entrySet()) {
            if (bedrockItems.getValue().getId() != data.getId())
                continue;

            String identifier = getJavaIdentifier(bedrockItems.getKey().replace("minecraft:", ""));
            if (!Toolbox.JAVA_ITEMS.containsKey(identifier))
                continue;

            return Toolbox.JAVA_ITEMS.get(identifier);
        }

        return new JavaItem("minecraft:air", 0);
    }

    public String getBedrockIdentifier(String javaIdentifier) {
        if (!identifiers.containsKey(javaIdentifier))
            return "minecraft:" + javaIdentifier;

        return "minecraft:" + identifiers.get(javaIdentifier);
    }

    public String getJavaIdentifier(String bedrockIdentifier) {
        for (Map.Entry<String, String> entry : identifiers.entrySet()) {
            if (entry.getValue().equals(bedrockIdentifier)) {
                return "minecraft:" + entry.getKey();
            }
        }

        return "minecraft:" + bedrockIdentifier;
    }

    private CompoundTag translateToJavaNBT(com.nukkitx.nbt.tag.CompoundTag tag) {
        CompoundTag javaTag = new CompoundTag(tag.getName());
        Map<String, Tag> javaValue = javaTag.getValue();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                com.nukkitx.nbt.tag.Tag bedrockTag = tag.get(str);
                Tag translatedTag = translateToJavaNBT(bedrockTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(str, translatedTag);
            }
        }

        return javaTag;
    }

    private Tag translateToJavaNBT(com.nukkitx.nbt.tag.Tag tag) {
        if (tag instanceof com.nukkitx.nbt.tag.ByteArrayTag) {
            com.nukkitx.nbt.tag.ByteArrayTag byteArrayTag = (com.nukkitx.nbt.tag.ByteArrayTag) tag;
            return new ByteArrayTag(byteArrayTag.getName(), byteArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ByteTag) {
            com.nukkitx.nbt.tag.ByteTag byteTag = (com.nukkitx.nbt.tag.ByteTag) tag;
            return new ByteTag(byteTag.getName(), byteTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.DoubleTag) {
            com.nukkitx.nbt.tag.DoubleTag doubleTag = (com.nukkitx.nbt.tag.DoubleTag) tag;
            return new DoubleTag(doubleTag.getName(), doubleTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.FloatTag) {
            com.nukkitx.nbt.tag.FloatTag floatTag = (com.nukkitx.nbt.tag.FloatTag) tag;
            return new FloatTag(floatTag.getName(), floatTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.IntArrayTag) {
            com.nukkitx.nbt.tag.IntArrayTag intArrayTag = (com.nukkitx.nbt.tag.IntArrayTag) tag;
            return new IntArrayTag(intArrayTag.getName(), intArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.IntTag) {
            com.nukkitx.nbt.tag.IntTag intTag = (com.nukkitx.nbt.tag.IntTag) tag;
            return new IntTag(intTag.getName(), intTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.LongArrayTag) {
            com.nukkitx.nbt.tag.LongArrayTag longArrayTag = (com.nukkitx.nbt.tag.LongArrayTag) tag;
            return new LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.LongTag) {
            com.nukkitx.nbt.tag.LongTag longTag = (com.nukkitx.nbt.tag.LongTag) tag;
            return new LongTag(longTag.getName(), longTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ShortTag) {
            com.nukkitx.nbt.tag.ShortTag shortTag = (com.nukkitx.nbt.tag.ShortTag) tag;
            return new ShortTag(shortTag.getName(), shortTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.StringTag) {
            com.nukkitx.nbt.tag.StringTag stringTag = (com.nukkitx.nbt.tag.StringTag) tag;
            return new StringTag(stringTag.getName(), stringTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ListTag) {
            com.nukkitx.nbt.tag.ListTag listTag = (com.nukkitx.nbt.tag.ListTag) tag;

            List<Tag> tags = new ArrayList<Tag>();
            for (Object value : listTag.getValue()) {
                if (!(value instanceof com.nukkitx.nbt.tag.Tag))
                    continue;

                com.nukkitx.nbt.tag.Tag tagValue = (com.nukkitx.nbt.tag.Tag) value;
                Tag javaTag = translateToJavaNBT(tagValue);
                if (javaTag != null)
                    tags.add(javaTag);
            }
            return new ListTag(listTag.getName(), tags);
        }

        if (tag instanceof com.nukkitx.nbt.tag.CompoundTag) {
            return translateToJavaNBT((com.nukkitx.nbt.tag.CompoundTag) tag);
        }

        return null;
    }

    private com.nukkitx.nbt.tag.CompoundTag translateToBedrockNBT(CompoundTag tag) {
        Map<String, com.nukkitx.nbt.tag.Tag<?>> javaValue = new HashMap<String, com.nukkitx.nbt.tag.Tag<?>>();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                Tag javaTag = tag.get(str);
                com.nukkitx.nbt.tag.Tag translatedTag = translateToBedrockNBT(javaTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(str, translatedTag);
            }
        }

        com.nukkitx.nbt.tag.CompoundTag bedrockTag = new com.nukkitx.nbt.tag.CompoundTag(tag.getName(), javaValue);
        return bedrockTag;
    }

    private com.nukkitx.nbt.tag.Tag translateToBedrockNBT(Tag tag) {
        if (tag instanceof ByteArrayTag) {
            ByteArrayTag byteArrayTag = (ByteArrayTag) tag;
            return new com.nukkitx.nbt.tag.ByteArrayTag(byteArrayTag.getName(), byteArrayTag.getValue());
        }

        if (tag instanceof ByteTag) {
            ByteTag byteTag = (ByteTag) tag;
            return new com.nukkitx.nbt.tag.ByteTag(byteTag.getName(), byteTag.getValue());
        }

        if (tag instanceof DoubleTag) {
            DoubleTag doubleTag = (DoubleTag) tag;
            return new com.nukkitx.nbt.tag.DoubleTag(doubleTag.getName(), doubleTag.getValue());
        }

        if (tag instanceof FloatTag) {
            FloatTag floatTag = (FloatTag) tag;
            return new com.nukkitx.nbt.tag.FloatTag(floatTag.getName(), floatTag.getValue());
        }

        if (tag instanceof IntArrayTag) {
            IntArrayTag intArrayTag = (IntArrayTag) tag;
            return new com.nukkitx.nbt.tag.IntArrayTag(intArrayTag.getName(), intArrayTag.getValue());
        }

        if (tag instanceof IntTag) {
            IntTag intTag = (IntTag) tag;
            return new com.nukkitx.nbt.tag.IntTag(intTag.getName(), intTag.getValue());
        }

        if (tag instanceof LongArrayTag) {
            LongArrayTag longArrayTag = (LongArrayTag) tag;
            return new com.nukkitx.nbt.tag.LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
        }

        if (tag instanceof LongTag) {
            LongTag longTag = (LongTag) tag;
            return new com.nukkitx.nbt.tag.LongTag(longTag.getName(), longTag.getValue());
        }

        if (tag instanceof ShortTag) {
            ShortTag shortTag = (ShortTag) tag;
            return new com.nukkitx.nbt.tag.ShortTag(shortTag.getName(), shortTag.getValue());
        }

        if (tag instanceof StringTag) {
            StringTag stringTag = (StringTag) tag;
            return new com.nukkitx.nbt.tag.StringTag(stringTag.getName(), stringTag.getValue());
        }

        if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;

            List<com.nukkitx.nbt.tag.Tag> tags = new ArrayList<com.nukkitx.nbt.tag.Tag>();
            for (Object value : listTag.getValue()) {
                if (!(value instanceof Tag))
                    continue;

                Tag tagValue = (Tag) value;
                com.nukkitx.nbt.tag.Tag bedrockTag = translateToBedrockNBT(tagValue);
                if (bedrockTag != null)
                    tags.add(bedrockTag);
            }
            // TODO: Fix unchecked call here
            return new com.nukkitx.nbt.tag.ListTag(listTag.getName(), listTag.getElementType(), tags);
        }

        if (tag instanceof CompoundTag) {
            return translateToBedrockNBT((CompoundTag) tag);
        }

        return null;
    }
}
