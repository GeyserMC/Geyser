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
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.components.FireworkExplosionShape;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.geyser.level.FireworkColor;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;

import java.util.Locale;
import java.util.function.Consumer;

public class FireworkExplosionTooltip implements ComponentTooltipProvider<Fireworks.FireworkExplosion> {
    private static final Component CUSTOM_COLOR = Component.translatable("item.minecraft.firework_star.custom_color");

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, Fireworks.@NonNull FireworkExplosion component) {
        adder.accept(FireworkExplosionShape.values()[component.getShapeId()].displayName().color(NamedTextColor.GRAY));

        if (component.getColors().length > 0) {
            adder.accept(writeColors(Component.translatable(), component.getColors()).color(NamedTextColor.GRAY));
        }
        if (component.getFadeColors().length > 0) {
            adder.accept(writeColors(Component.translatable().key("item.minecraft.firework_star.fade_to"), component.getFadeColors()).color(NamedTextColor.GRAY));
        }
        if (component.isHasTrail()) {
            adder.accept(Component.translatable("item.minecraft.firework_star.trail").color(NamedTextColor.GRAY));
        }
        if (component.isHasTwinkle()) {
            adder.accept(Component.translatable("item.minecraft.firework_star.flicker").color(NamedTextColor.GRAY));
        }
    }

    private static Component writeColors(TranslatableComponent.Builder builder, int[] colors) {
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                builder.append(Component.text(", "));
            }

            int color = colors[i];
            builder.append(getFireworkColorName(color));
        }

        return builder.build();
    }

    private static Component getFireworkColorName(int rgb) {
        FireworkColor builtin = FireworkColor.fromJavaRGB(rgb);
        if (builtin == null) {
            return CUSTOM_COLOR;
        }
        return Component.translatable("item.minecraft.firework_star." + builtin.name().toLowerCase(Locale.ROOT));
    }
}
