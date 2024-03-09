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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.List;

import static org.geysermc.erosion.util.BannerUtils.getJavaPatternTag;

public class BannerItem extends BlockItem {
    /**
     * Holds what a Java ominous banner pattern looks like.
     * <p>
     * Translating the patterns over to Bedrock does not work effectively, but Bedrock has a dedicated type for
     * ominous banners that we set instead. This variable is used to detect Java ominous banner patterns, and apply
     * the correct ominous banner pattern if Bedrock pulls the item from creative.
     */
    public static final ListTag OMINOUS_BANNER_PATTERN;

    static {
        OMINOUS_BANNER_PATTERN = new ListTag("Patterns");
        // Construct what an ominous banner is supposed to look like
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("mr", 9));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("bs", 8));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("cs", 7));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("bo", 8));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("ms", 15));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("hh", 8));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("mc", 8));
        OMINOUS_BANNER_PATTERN.add(getJavaPatternTag("bo", 15));
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
    @NonNull
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
        return getJavaPatternTag(pattern.getString("Pattern"), 15 - pattern.getInt("Color"));
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

    public BannerItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        CompoundTag blockEntityTag = tag.remove("BlockEntityTag");
        if (blockEntityTag != null && blockEntityTag.get("Patterns") instanceof ListTag patterns) {
            if (patterns.equals(OMINOUS_BANNER_PATTERN)) {
                // Remove the current patterns and set the ominous banner type
                tag.put(new IntTag("Type", 1));
            } else {
                invertBannerColors(patterns);
                tag.put(patterns);
            }
        }
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        if (tag.get("Type") instanceof IntTag type && type.getValue() == 1) {
            // Ominous banner pattern
            tag.remove("Type");
            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            blockEntityTag.put(OMINOUS_BANNER_PATTERN);

            tag.put(blockEntityTag);
        } else if (tag.get("Patterns") instanceof ListTag patterns) {
            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            invertBannerColors(patterns);
            blockEntityTag.put(patterns);

            tag.put(blockEntityTag);
            tag.remove("Patterns"); // Remove the old Bedrock patterns list
        }
    }
}
