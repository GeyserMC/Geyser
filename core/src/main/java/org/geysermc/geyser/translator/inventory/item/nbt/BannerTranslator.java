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

import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ItemRemapper
public class BannerTranslator extends NbtItemStackTranslator {
    /**
     * Holds what a Java ominous banner pattern looks like.
     *
     * Translating the patterns over to Bedrock does not work effectively, but Bedrock has a dedicated type for
     * ominous banners that we set instead. This variable is used to detect Java ominous banner patterns, and apply
     * the correct ominous banner pattern if Bedrock pulls the item from creative.
     */
    public static final ListTag OMINOUS_BANNER_PATTERN;

    private final List<ItemMapping> appliedItems;

    static {
        OMINOUS_BANNER_PATTERN = new ListTag("Patterns");
        // Construct what an ominous banner is supposed to look like
        OMINOUS_BANNER_PATTERN.add(getPatternTag("mr", 9));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("bs", 8));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("cs", 7));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("bo", 8));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("ms", 15));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("hh", 8));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("mc", 8));
        OMINOUS_BANNER_PATTERN.add(getPatternTag("bo", 15));
    }

    private static CompoundTag getPatternTag(String pattern, int color) {
        StringTag patternType = new StringTag("Pattern", pattern);
        IntTag colorTag = new IntTag("Color", color);
        CompoundTag tag = new CompoundTag("");
        tag.put(patternType);
        tag.put(colorTag);
        return tag;
    }

    public BannerTranslator() {
        appliedItems = Registries.ITEMS.forVersion(MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion())
                .getItems()
                .values()
                .stream()
                .filter(entry -> entry.getJavaIdentifier().endsWith("banner"))
                .collect(Collectors.toList());
    }

    /**
     * Convert a list of patterns from Java nbt to Bedrock nbt
     *
     * @param patterns The patterns to convert
     * @return The new converted patterns
     */
    public static NbtList<NbtMap> convertBannerPattern(ListTag patterns) {
        List<NbtMap> tagsList = new ArrayList<>();
        for (Tag patternTag : patterns.getValue()) {
            tagsList.add(getBedrockBannerPattern((CompoundTag) patternTag));
        }

        return new NbtList<>(NbtType.COMPOUND, tagsList);
    }

    /**
     * Convert the Java edition banner pattern nbt to Bedrock edition, null if the pattern doesn't exist
     *
     * @param pattern Java edition pattern nbt
     * @return The Bedrock edition format pattern nbt
     */
    @Nonnull
    private static NbtMap getBedrockBannerPattern(CompoundTag pattern) {
        return NbtMap.builder()
                .putInt("Color", 15 - (int) pattern.get("Color").getValue())
                .putString("Pattern", (String) pattern.get("Pattern").getValue())
                .build();
    }

    /**
     * Convert the Bedrock edition banner pattern nbt to Java edition
     *
     * @param pattern Bedrock edition pattern nbt
     * @return The Java edition format pattern nbt
     */
    public static CompoundTag getJavaBannerPattern(NbtMap pattern) {
        Map<String, Tag> tags = new HashMap<>();
        tags.put("Color", new IntTag("Color", 15 - pattern.getInt("Color")));
        tags.put("Pattern", new StringTag("Pattern", pattern.getString("Pattern")));

        return new CompoundTag("", tags);
    }

    /**
     * Convert a list of patterns from Java nbt to Bedrock nbt, or vice versa (we just need to invert the color)
     *
     * @param patterns The patterns to convert
     */
    private void invertBannerColors(ListTag patterns) {
        for (Tag patternTag : patterns.getValue()) {
            IntTag color = ((CompoundTag) patternTag).get("Color");
            color.setValue(15 - color.getValue());
        }
    }

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        CompoundTag blockEntityTag = itemTag.get("BlockEntityTag");
        if (blockEntityTag != null && blockEntityTag.get("Patterns") instanceof ListTag patterns) {
            if (patterns.equals(OMINOUS_BANNER_PATTERN)) {
                // Remove the current patterns and set the ominous banner type
                itemTag.put(new IntTag("Type", 1));
            } else {
                invertBannerColors(patterns);
                itemTag.put(patterns);
            }
            itemTag.remove("BlockEntityTag");
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        if (itemTag.get("Type") instanceof IntTag type && type.getValue() == 1) {
            // Ominous banner pattern
            itemTag.remove("Type");
            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            blockEntityTag.put(OMINOUS_BANNER_PATTERN);

            itemTag.put(blockEntityTag);
        } else if (itemTag.get("Patterns") instanceof ListTag patterns) {
            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            invertBannerColors(patterns);
            blockEntityTag.put(patterns);

            itemTag.put(blockEntityTag);
            itemTag.remove("Patterns"); // Remove the old Bedrock patterns list
        }
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return appliedItems.contains(mapping);
    }
}
