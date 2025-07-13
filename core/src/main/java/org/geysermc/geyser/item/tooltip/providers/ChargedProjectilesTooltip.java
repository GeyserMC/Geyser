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
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.geyser.item.tooltip.TooltipProviders;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class ChargedProjectilesTooltip implements ComponentTooltipProvider<List<ItemStack>> {

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull List<ItemStack> projectiles) {
        ItemStack currentStack = null;
        int count = 0;

        for (ItemStack stack : projectiles) {
            if (currentStack == null) {
                currentStack = stack;
                count = 1;
            } else if (currentStack.equals(stack)) {
                count++;
            } else {
                addProjectile(context, currentStack, count, adder);
                currentStack = stack;
                count = 1;
            }
        }

        if (currentStack != null) {
            addProjectile(context, currentStack, count, adder);
        }
    }

    private static void addProjectile(TooltipContext context, ItemStack stack, int count, Consumer<Component> adder) {
        if (count == 1) {
            // TODO stack name
            adder.accept(Component.translatable("item.minecraft.crossbow.projectile.single", Component.text(stack.toString())));
        } else {
            adder.accept(Component.translatable("item.minecraft.crossbow.projectile.multiple", Component.text(count), Component.text(stack.toString())));
        }

        // TODO default components?
        TooltipProviders.addTooltips(context.withComponents(stack.getDataComponentsPatch()).withFlags(false, false),
            tooltip -> adder.accept(Component.space().append(tooltip).color(NamedTextColor.GRAY)));
    }
}
