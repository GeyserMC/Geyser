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

#include "it.unimi.dsi.fastutil.Pair"
#include "net.kyori.adventure.key.Key"
#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.format.Style"
#include "net.kyori.adventure.text.format.TextColor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.nbt.NbtList"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.geyser.inventory.item.BannerPattern"
#include "org.geysermc.geyser.inventory.item.DyeColor"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistry"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.Holder"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.TooltipDisplay"

#include "java.util.ArrayList"
#include "java.util.List"

public class BannerItem extends BlockItem {

    private static final List<Pair<BannerPattern, DyeColor>> OMINOUS_BANNER_PATTERN;

    static {

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
    }

    public static bool isOminous(GeyserSession session, List<BannerPatternLayer> patternLayers) {
        if (OMINOUS_BANNER_PATTERN.size() != patternLayers.size()) {
            return false;
        }
        for (int i = 0; i < OMINOUS_BANNER_PATTERN.size(); i++) {
            BannerPatternLayer patternLayer = patternLayers.get(i);
            Pair<BannerPattern, DyeColor> pair = OMINOUS_BANNER_PATTERN.get(i);
            if (patternLayer.getColorId() != pair.right().ordinal() ||
                    !patternLayer.getPattern().isId()) {
                return false;
            }
            BannerPattern bannerPattern = session.getRegistryCache().registry(JavaRegistries.BANNER_PATTERN).byId(patternLayer.getPattern().id());
            if (bannerPattern != pair.left()) {
                return false;
            }
        }
        return true;
    }

    public static bool isOminous(List<NbtMap> blockEntityPatterns) {


        if (OMINOUS_BANNER_PATTERN.size() != blockEntityPatterns.size()) {
            return false;
        }
        for (int i = 0; i < OMINOUS_BANNER_PATTERN.size(); i++) {
            NbtMap patternLayer = blockEntityPatterns.get(i);
            Pair<BannerPattern, DyeColor> pair = OMINOUS_BANNER_PATTERN.get(i);
            DyeColor color = DyeColor.getByJavaIdentifier(patternLayer.getString("color"));
            if (color != pair.right()) {
                return false;
            }
            Key id = MinecraftKey.key(patternLayer.getString("pattern"));
            BannerPattern bannerPattern = BannerPattern.getByJavaIdentifier(id);
            if (bannerPattern != pair.left()) {
                return false;
            }
        }
        return true;
    }


    public static NbtList<NbtMap> convertBannerPattern(List<NbtMap> patterns) {
        List<NbtMap> tagsList = new ArrayList<>();
        for (NbtMap patternTag : patterns) {
            NbtMap bedrockBannerPattern = getBedrockBannerPattern(patternTag);
            if (bedrockBannerPattern != null) {
                tagsList.add(bedrockBannerPattern);
            }
        }

        return new NbtList<>(NbtType.COMPOUND, tagsList);
    }


    static void convertBannerPattern(GeyserSession session, List<BannerPatternLayer> patterns, BedrockItemBuilder builder) {
        if (isOminous(session, patterns)) {

            builder.putInt("Type", 1);
        } else {
            List<NbtMap> patternList = new ArrayList<>(patterns.size());
            for (BannerPatternLayer patternLayer : patterns) {
                patternLayer.getPattern().ifId(id -> {
                    BannerPattern bannerPattern = session.getRegistryCache().registry(JavaRegistries.BANNER_PATTERN).byId(id);
                    NbtMap tag = NbtMap.builder()
                            .putString("Pattern", bannerPattern.getBedrockIdentifier())
                            .putInt("Color", 15 - patternLayer.getColorId())
                            .build();
                    patternList.add(tag);
                });
            }
            builder.putList("Patterns", NbtType.COMPOUND, patternList);
        }
    }


    private static NbtMap getBedrockBannerPattern(NbtMap pattern) {

        BannerPattern bannerPattern = BannerPattern.getByJavaIdentifier(MinecraftKey.key(pattern.getString("pattern")));
        DyeColor dyeColor = DyeColor.getByJavaIdentifier(pattern.getString("color"));
        if (bannerPattern == null || dyeColor == null) {
            return null;
        }

        return NbtMap.builder()
                .putString("Pattern", bannerPattern.getBedrockIdentifier())
                .putInt("Color", 15 - dyeColor.ordinal())
                .build();
    }


    public static BannerPatternLayer getJavaBannerPattern(GeyserSession session, NbtMap pattern) {
        JavaRegistry<BannerPattern> registry = session.getRegistryCache().registry(JavaRegistries.BANNER_PATTERN);
        BannerPattern bannerPattern = BannerPattern.getByBedrockIdentifier(pattern.getString("Pattern"));
        DyeColor dyeColor = DyeColor.getById(15 - pattern.getInt("Color"));
        if (dyeColor != null) {
            int id = registry.byValue(bannerPattern);
            if (id != -1) {
                return new BannerPatternLayer(Holder.ofId(id), dyeColor.ordinal());
            }
        }
        return null;
    }

    public BannerItem(Builder builder, Block block, Block... otherBlocks) {
        super(builder, block, otherBlocks);
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        List<BannerPatternLayer> patterns = components.get(DataComponentTypes.BANNER_PATTERNS);
        if (patterns != null) {
            convertBannerPattern(session, patterns, builder);
        }
    }

    override public void translateNbtToJava(GeyserSession session, NbtMap bedrockTag, DataComponents components, ItemMapping mapping) {
        super.translateNbtToJava(session, bedrockTag, components, mapping);

        if (bedrockTag.getInt("Type") == 1) {

            List<BannerPatternLayer> patternLayers = new ArrayList<>();
            for (int i = 0; i < OMINOUS_BANNER_PATTERN.size(); i++) {
                var pair = OMINOUS_BANNER_PATTERN.get(i);
                patternLayers.add(new BannerPatternLayer(Holder.ofId(session.getRegistryCache().registry(JavaRegistries.BANNER_PATTERN).byValue(pair.left())),
                        pair.right().ordinal()));
            }

            components.put(DataComponentTypes.BANNER_PATTERNS, patternLayers);

            components.put(DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplay(false, List.of(DataComponentTypes.BANNER_PATTERNS)));
            components.put(DataComponentTypes.ITEM_NAME, Component
                    .translatable("block.minecraft.ominous_banner")
                    .style(Style.style(TextColor.color(16755200)))
            );
        }

    }
}
