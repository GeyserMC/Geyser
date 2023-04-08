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

import com.github.steveice10.opennbt.tag.builtin.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.entity.type.living.animal.TropicalFishEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.ArrayList;
import java.util.List;

public class TropicalFishBucketItem extends Item {
    private static final Style LORE_STYLE = Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC);

    public TropicalFishBucketItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        // Prevent name from appearing as "Bucket of"
        tag.put(new ByteTag("AppendCustomName", (byte) 1));
        tag.put(new StringTag("CustomName", MinecraftLocale.getLocaleString("entity.minecraft.tropical_fish", session.locale())));
        // Add Java's client side lore tag
        Tag bucketVariantTag = tag.get("BucketVariantTag");
        if (bucketVariantTag instanceof IntTag) {
            CompoundTag displayTag = tag.get("display");
            if (displayTag == null) {
                displayTag = new CompoundTag("display");
                tag.put(displayTag);
            }

            List<Tag> lore = new ArrayList<>();

            int varNumber = ((IntTag) bucketVariantTag).getValue();
            int predefinedVariantId = TropicalFishEntity.getPredefinedId(varNumber);
            if (predefinedVariantId != -1) {
                Component tooltip = Component.translatable("entity.minecraft.tropical_fish.predefined." + predefinedVariantId, LORE_STYLE);
                lore.add(0, new StringTag("", MessageTranslator.convertMessage(tooltip, session.locale())));
            } else {
                Component typeTooltip = Component.translatable("entity.minecraft.tropical_fish.type." + TropicalFishEntity.getVariantName(varNumber), LORE_STYLE);
                lore.add(0, new StringTag("", MessageTranslator.convertMessage(typeTooltip, session.locale())));

                byte baseColor = TropicalFishEntity.getBaseColor(varNumber);
                byte patternColor = TropicalFishEntity.getPatternColor(varNumber);
                Component colorTooltip = Component.translatable("color.minecraft." + TropicalFishEntity.getColorName(baseColor), LORE_STYLE);
                if (baseColor != patternColor) {
                    colorTooltip = colorTooltip.append(Component.text(", ", LORE_STYLE))
                            .append(Component.translatable("color.minecraft." + TropicalFishEntity.getColorName(patternColor), LORE_STYLE));
                }
                lore.add(1, new StringTag("", MessageTranslator.convertMessage(colorTooltip, session.locale())));
            }

            ListTag loreTag = displayTag.get("Lore");
            if (loreTag != null) {
                lore.addAll(loreTag.getValue());
            }
            displayTag.put(new ListTag("Lore", lore));
        }
    }
}
