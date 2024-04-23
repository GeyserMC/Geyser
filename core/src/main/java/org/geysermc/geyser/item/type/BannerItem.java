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

import com.github.steveice10.mc.protocol.data.game.item.component.BannerPatternLayer;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentType;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponents;
import com.github.steveice10.opennbt.tag.builtin.*;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.inventory.item.DyeColor;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;

import java.util.ArrayList;
import java.util.List;

public class BannerItem extends BlockItem {
    /**
     * Holds what a Java ominous banner pattern looks like.
     * <p>
     * Translating the patterns over to Bedrock does not work effectively, but Bedrock has a dedicated type for
     * ominous banners that we set instead. This variable is used to detect Java ominous banner patterns, and apply
     * the correct ominous banner pattern if Bedrock pulls the item from creative.
     */
    private static final List<Pair<BannerPattern, DyeColor>> OMINOUS_BANNER_PATTERN;
    private static final ListTag OMINOUS_BANNER_PATTERN_BLOCK;

    static {
        // Construct what an ominous banner is supposed to look like
        OMINOUS_BANNER_PATTERN = List.of(
                Pair.of(BannerPattern.RHOMBUS, DyeColor.CYAN),
                Pair.of(BannerPattern.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.STRIPE_CENTER, DyeColor.GRAY),
                Pair.of(BannerPattern.BORDER, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.STRIPE_MIDDLE, DyeColor.BLACK),
                Pair.of(BannerPattern.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.CIRCLE, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.BORDER, DyeColor.BLACK)
        );

        OMINOUS_BANNER_PATTERN_BLOCK = new ListTag("patterns");
        for (Pair<BannerPattern, DyeColor> pair : OMINOUS_BANNER_PATTERN) {
            OMINOUS_BANNER_PATTERN_BLOCK.add(getJavaBannerPatternTag(pair.left(), pair.right()));
        }
    }

    public static boolean isOminous(GeyserSession session, List<BannerPatternLayer> patternLayers) {
        if (OMINOUS_BANNER_PATTERN.size() != patternLayers.size()) {
            return false;
        }
        for (int i = 0; i < OMINOUS_BANNER_PATTERN.size(); i++) {
            BannerPatternLayer patternLayer = patternLayers.get(i);
            Pair<BannerPattern, DyeColor> pair = OMINOUS_BANNER_PATTERN.get(i);
            if (!patternLayer.getPattern().isId() || patternLayer.getColorId() != pair.right().ordinal()) {
                return false;
            }
            BannerPattern bannerPattern = session.getRegistryCache().bannerPatterns().get(patternLayer.getPattern().id());
            if (bannerPattern != pair.left()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOminous(ListTag blockEntityPatterns) {
        return OMINOUS_BANNER_PATTERN_BLOCK.equals(blockEntityPatterns);
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
            NbtMap bedrockBannerPattern = getBedrockBannerPattern((CompoundTag) patternTag);
            if (bedrockBannerPattern != null) {
                tagsList.add(bedrockBannerPattern);
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
    private static NbtMap getBedrockBannerPattern(CompoundTag pattern) {
        BannerPattern bannerPattern = BannerPattern.getByJavaIdentifier((String) pattern.get("pattern").getValue());
        DyeColor dyeColor = DyeColor.getByJavaIdentifier((String) pattern.get("color").getValue());
        if (bannerPattern == null || dyeColor == null) {
            return null;
        }

        return NbtMap.builder()
                .putString("Pattern", bannerPattern.getBedrockIdentifier())
                .putInt("Color", 15 - dyeColor.ordinal())
                .build();
    }

    public static CompoundTag getJavaBannerPatternTag(BannerPattern bannerPattern, DyeColor dyeColor) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new StringTag("pattern", bannerPattern.getJavaIdentifier()));
        tag.put(new StringTag("color", dyeColor.getJavaIdentifier()));
        return tag;
    }

    /**
     * Convert the Bedrock edition banner pattern nbt to Java edition
     *
     * @param pattern Bedrock edition pattern nbt
     * @return The Java edition format pattern layer
     */
    public static BannerPatternLayer getJavaBannerPattern(GeyserSession session, NbtMap pattern) {
        return null; // TODO
        /*Int2ObjectBiMap<BannerPattern> registry = session.getRegistryCache().bannerPatterns();
        BannerPattern bannerPattern = BannerPattern.getByBedrockIdentifier(pattern.getString("Pattern"));
        DyeColor dyeColor = DyeColor.getById(15 - pattern.getInt("Color"));
        if (bannerPattern != null && dyeColor != null && registry.containsValue(bannerPattern)) {
            return new BannerPatternLayer(Holder.ofId(registry.get(bannerPattern)), dyeColor.ordinal());
        }
        return null;*/
    }

    public BannerItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        List<BannerPatternLayer> patterns = components.get(DataComponentType.BANNER_PATTERNS);
        if (patterns != null) {
            if (isOminous(session, patterns)) {
                // Remove the current patterns and set the ominous banner type
                builder.putInt("Type", 1);
            } else {
                List<NbtMap> patternList = new ArrayList<>(patterns.size());
                for (BannerPatternLayer patternLayer : patterns) {
                    patternLayer.getPattern().ifId(holder -> {
                        BannerPattern bannerPattern = session.getRegistryCache().bannerPatterns().get(holder.id());
                        if (bannerPattern != null) {
                            NbtMap tag = NbtMap.builder()
                                    .putString("Pattern", bannerPattern.getBedrockIdentifier())
                                    .putInt("Color", 15 - patternLayer.getColorId())
                                    .build();
                            patternList.add(tag);
                        }
                    });
                }
                builder.putList("Patterns", NbtType.COMPOUND, patternList);
            }
        }
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) { // TODO
        super.translateNbtToJava(tag, mapping);

        if (tag.get("Type") instanceof IntTag type && type.getValue() == 1) {
            // Ominous banner pattern
            tag.remove("Type");
            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            blockEntityTag.put(OMINOUS_BANNER_PATTERN_BLOCK);

            tag.put(blockEntityTag);
        } else if (tag.get("Patterns") instanceof ListTag patterns && patterns.getElementType() == CompoundTag.class) {
            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");

            ListTag javaPatterns = new ListTag("patterns");
            for (Tag pattern : patterns.getValue()) {
                BannerPattern bannerPattern = BannerPattern.getByBedrockIdentifier((String) ((CompoundTag) pattern).get("Pattern").getValue());
                DyeColor dyeColor = DyeColor.getById((int) ((CompoundTag) pattern).get("Color").getValue());
                if (bannerPattern != null && dyeColor != null) {
                    javaPatterns.add(getJavaBannerPatternTag(bannerPattern, dyeColor));
                }
            }
            blockEntityTag.put(javaPatterns);

            tag.put(blockEntityTag);
            tag.remove("Patterns"); // Remove the old Bedrock patterns list
        }
    }
}
