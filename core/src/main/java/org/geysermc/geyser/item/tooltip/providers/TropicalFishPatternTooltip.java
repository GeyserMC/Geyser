/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.tooltip.providers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.entity.type.living.animal.TropicalFishEntity;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.function.Consumer;

public class TropicalFishPatternTooltip implements ComponentTooltipProvider<Integer> {
    private static final Style STYLE = Style.style(NamedTextColor.GRAY, TextDecoration.ITALIC);

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull Integer pattern) {
        int baseColor = context.components().getOrDefault(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, 0);
        int patternColor = context.components().getOrDefault(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, 0);

        int packedVariant = TropicalFishEntity.getPackedVariant(pattern, baseColor, patternColor);

        int predefinedVariantId = TropicalFishEntity.getPredefinedId(packedVariant);
        if (predefinedVariantId != -1) {
            adder.accept(Component.translatable("entity.minecraft.tropical_fish.predefined." + predefinedVariantId, STYLE));
        } else {
            adder.accept(Component.translatable("entity.minecraft.tropical_fish.type." + TropicalFishEntity.getVariantName(packedVariant), STYLE));

            Component colorTooltip = Component.translatable("color.minecraft." + TropicalFishEntity.getColorName((byte) baseColor));
            if (baseColor != patternColor) {
                colorTooltip = colorTooltip.append(Component.text(", "))
                    .append(Component.translatable("color.minecraft." + TropicalFishEntity.getColorName((byte) patternColor)));
            }
            adder.accept(colorTooltip.style(STYLE));
        }
    }
}
