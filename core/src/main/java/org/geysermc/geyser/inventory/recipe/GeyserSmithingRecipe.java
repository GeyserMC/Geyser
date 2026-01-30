/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.recipe;

import it.unimi.dsi.fastutil.Pair;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

public record GeyserSmithingRecipe(int id,
                                   SlotDisplay template,
                                   SlotDisplay base,
                                   SlotDisplay addition,
                                   SlotDisplay result) implements GeyserRecipe {
    public GeyserSmithingRecipe(int id, SmithingRecipeDisplay display) {
        this(id, display.template(), display.base(), display.addition(), display.result());
    }

    @Override
    public boolean isShaped() {
        return false;
    }

    @Override
    public List<RecipeData> asRecipeData(GeyserSession session) {
        Pair<Item, ItemData> output = RecipeUtil.translateToOutput(session, result);
        if (output == null) {
            return List.of();
        }

        List<ItemDescriptorWithCount> bases = RecipeUtil.translateToInput(session, base);
        List<ItemDescriptorWithCount> templates = RecipeUtil.translateToInput(session, template);
        List<ItemDescriptorWithCount> additions = RecipeUtil.translateToInput(session, addition);
        if (bases == null || templates == null || additions == null) {
            return List.of();
        }

        List<RecipeData> recipeData = new ArrayList<>();
        int i = 0;
        for (ItemDescriptorWithCount template : templates) {
            for (ItemDescriptorWithCount base : bases) {
                for (ItemDescriptorWithCount addition : additions) {
                    // Note: vanilla inputs use aux value of Short.MAX_VALUE
                    recipeData.add(SmithingTransformRecipeData.of(id + "_" + i++, template, base, addition,
                            output.right(), "smithing_table", session.getLastRecipeNetId().getAndIncrement()));
                }
            }
        }
        return recipeData;
    }
}
