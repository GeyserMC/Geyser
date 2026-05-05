/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.FurnaceRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapelessCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record GeyserShapelessRecipe(int id,
                                    int netId,
                                    List<SlotDisplay> ingredients,
                                    SlotDisplay result,
                                    String tag) implements GeyserRecipe {

    public GeyserShapelessRecipe(int id, int netId, ShapelessCraftingRecipeDisplay data) {
        this(id, netId, data.ingredients(), data.result(), "crafting_table");
    }

    public GeyserShapelessRecipe(int id, int netId, FurnaceRecipeDisplay data, int category) {
        this(id, netId, List.of(data.ingredient()), data.result(), FurnaceRecipeType.fromCategory(category).tag);
    }

    @Override
    public boolean isShaped() {
        return false;
    }

    @Override
    public List<RecipeData> asRecipeData(GeyserSession session) {
        var bedrockRecipes = RecipeUtil.combinations(session, result, ingredients);
        if (bedrockRecipes == null) {
            return List.of();
        }

        List<RecipeData> recipeData = new ArrayList<>();
        ItemData output = bedrockRecipes.right();
        List<List<ItemDescriptorWithCount>> left = bedrockRecipes.left();
        int i = 0;
        for (List<ItemDescriptorWithCount> inputs : left) {
            recipeData.add(ShapelessRecipeData.shapeless(id + "_" + i, inputs,
                    Collections.singletonList(output), UUID.randomUUID(), tag, 0,
                    netId + i, RecipeUnlockingRequirement.INVALID));
            i++;
        }
        return recipeData;
    }

    public enum FurnaceRecipeType {
        FURNACE("furnace", 4, 5, 6), // furnace
        BLAST_FURNACE("blast_furnace", 7, 8), // blast_furnace_blocks, blast_furnace_misc
        SMOKER("smoker", 9, 12); // smoker_food, campfire

        private final String tag;
        @Getter
        @Accessors(fluent = true)
        private final IntList categories;

        FurnaceRecipeType(String tag, int... categories) {
            this.tag = tag;
            this.categories = IntList.of(categories);
        }

        public static FurnaceRecipeType fromCategory(int category) {
            for (FurnaceRecipeType type : values()) {
                if (type.categories.contains(category)) {
                    return type;
                }
            }

            return FURNACE; // /shrug
        }
    }
}
