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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.components.FireworkExplosionShape;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;

import java.util.function.Consumer;

public class FireworksTooltip implements ComponentTooltipProvider<Fireworks> {

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull Fireworks fireworks) {
        if (fireworks.getFlightDuration() > 0) {
            adder.accept(Component.translatable("item.minecraft.firework_rocket.flight")
                .append(Component.space())
                .append(Component.text(fireworks.getFlightDuration()))
                .color(NamedTextColor.GRAY));
        }

        Fireworks.FireworkExplosion current = null;
        int count = 0;

        for (Fireworks.FireworkExplosion explosion : fireworks.getExplosions()) {
            if (current == null) {
                current = explosion;
                count = 1;
            } else if (current.equals(explosion)) {
                count++;
            } else {
                addExplosion(current, count, adder);
                current = explosion;
                count = 1;
            }
        }

        if (current != null) {
            addExplosion(current, count, adder);
        }
    }

    private static void addExplosion(Fireworks.FireworkExplosion explosion, int count, Consumer<Component> adder) {
        Component name = FireworkExplosionShape.values()[explosion.getShapeId()].displayName();
        if (count == 1) {
            adder.accept(Component.translatable("item.minecraft.firework_rocket.single_star", name).color(NamedTextColor.GRAY));
        } else {
            adder.accept(Component.translatable("item.minecraft.firework_rocket.multiple_stars", Component.text(count), name).color(NamedTextColor.GRAY));
        }

        FireworkExplosionTooltip.addDetails(explosion, detail -> adder.accept(Component.space().append(detail)));
    }
}
