/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ItemRemapper
public class BannerTranslator extends ItemTranslator {

    private List<ItemEntry> appliedItems;

    public BannerTranslator() {
    }

    @Override
    public ItemData translateToBedrock(ItemStack itemStack, ItemEntry itemEntry) {
        if (itemStack.getNbt() == null) return super.translateToBedrock(itemStack, itemEntry);

        ItemData itemData = super.translateToBedrock(itemStack, itemEntry);

        CompoundTag blockEntityTag = itemStack.getNbt().get("BlockEntityTag");
        if (blockEntityTag.contains("Patterns")) {
            ListTag patterns = blockEntityTag.get("Patterns");

            NbtMapBuilder builder = itemData.getTag().toBuilder();
            builder.put("Patterns", convertBannerPattern(patterns));

            itemData = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount(), builder.build());
        }

        return itemData;
    }

    @Override
    public ItemStack translateToJava(ItemData itemData, ItemEntry itemEntry) {
        if (itemData.getTag() == null) return super.translateToJava(itemData, itemEntry);

        ItemStack itemStack = super.translateToJava(itemData, itemEntry);

        NbtMap nbtTag = itemData.getTag();
        if (nbtTag.containsKey("Patterns", NbtType.COMPOUND)) {
            List<NbtMap> patterns = nbtTag.getList("Patterns", NbtType.COMPOUND);

            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            blockEntityTag.put(convertBannerPattern(patterns));

            itemStack.getNbt().put(blockEntityTag);
        }

        return itemStack;
    }

    @Override
    public List<ItemEntry> getAppliedItems() {
        if (appliedItems == null) {
            appliedItems = ItemRegistry.ITEM_ENTRIES.values().stream().filter(entry -> entry.getJavaIdentifier().endsWith("banner")).collect(Collectors.toList());
        }
        return appliedItems;
    }

    /**
     * Convert a list of patterns from Java nbt to Bedrock nbt
     *
     * @param patterns The patterns to convert
     * @return The new converted patterns
     */
    public static NbtList<NbtMap> convertBannerPattern(ListTag patterns) {
        List<NbtMap> tagsList = new ArrayList<>();
        for (com.github.steveice10.opennbt.tag.builtin.Tag patternTag : patterns.getValue()) {
            NbtMap newPatternTag = getBedrockBannerPattern((CompoundTag) patternTag);
            if (newPatternTag != null) {
                tagsList.add(newPatternTag);
            }
        }

        return new NbtList<>(NbtType.COMPOUND, tagsList);
    }

    /**
     * Convert the Java edition banner pattern nbt to Bedrock edition, null if the pattern doesn't exist
     *
     * @param pattern Java edition pattern nbt
     * @return The Bedrock edition format pattern nbt
     */
    public static NbtMap getBedrockBannerPattern(CompoundTag pattern) {
        String patternName = (String) pattern.get("Pattern").getValue();

        // Return null if its the globe pattern as it doesn't exist on bedrock
        if (patternName.equals("glb")) {
            return null;
        }

        return NbtMap.builder()
                .putInt("Color", 15 - (int) pattern.get("Color").getValue())
                .putString("Pattern", (String) pattern.get("Pattern").getValue())
                .putString("Pattern", patternName)
                .build();
    }

    /**
     * Convert a list of patterns from Bedrock nbt to Java nbt
     *
     * @param patterns The patterns to convert
     * @return The new converted patterns
     */
    public static ListTag convertBannerPattern(List<NbtMap> patterns) {
        List<Tag> tagsList = new ArrayList<>();
        for (Object patternTag : patterns) {
            CompoundTag newPatternTag = getJavaBannerPattern((NbtMap) patternTag);
            tagsList.add(newPatternTag);
        }

        return new ListTag("Patterns", tagsList);
    }

    /**
     * Convert the Bedrock edition banner pattern nbt to Java edition
     *
     * @param pattern Bedorck edition pattern nbt
     * @return The Java edition format pattern nbt
     */
    public static CompoundTag getJavaBannerPattern(NbtMap pattern) {
        Map<String, Tag> tags = new HashMap<>();
        tags.put("Color", new IntTag("Color", 15 - pattern.getInt("Color")));
        tags.put("Pattern", new StringTag("Pattern", pattern.getString("Pattern")));

        return new CompoundTag("", tags);
    }
}
