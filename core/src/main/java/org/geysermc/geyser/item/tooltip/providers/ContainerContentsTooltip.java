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
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class ContainerContentsTooltip implements ComponentTooltipProvider<List<ItemStack>> {

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull List<ItemStack> stacks) {
        int lines = 0;
        int total = 0;

        for (ItemStack stack : stacks) {
            GeyserItemStack itemStack = GeyserItemStack.from(stack);
            if (itemStack.isEmpty()) {
                continue;
            }
            total++;
            if (lines < 5) {
                lines++;
                // TODO itemStack name properly
                adder.accept(Component.translatable("item.container.item_count", Component.translatable(itemStack.asItem().translationKey()), Component.text(itemStack.getAmount())));
            }
        }

        if (total - lines > 0) {
            adder.accept(Component.translatable("item.container.more_items", Component.text(total - lines)).decorate(TextDecoration.ITALIC));
        }
    }
}
