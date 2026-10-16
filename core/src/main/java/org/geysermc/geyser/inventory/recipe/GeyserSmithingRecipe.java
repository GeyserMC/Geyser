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
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

public interface GeyserSmithingRecipe<T extends RecipeData> extends GeyserRecipe<T> {

    @Override
    default boolean isShaped() {
        return false;
    }

    SlotDisplay template();

    SlotDisplay base();

    SlotDisplay addition();

    List<T> getCraftingDataList(CraftingDataPacket craftingDataPacket);

    default int addToPacket(GeyserSession session, CraftingDataPacket craftingDataPacket) {
        List<T> recipeData = asRecipeData(session);
        if (GameProtocol.is26_40orHigher(session.protocolVersion())) {
            getCraftingDataList(craftingDataPacket).addAll(recipeData);
        } else {
            craftingDataPacket.getCraftingData().addAll(recipeData);
        }
        return recipeData.size();
    }

    default boolean matches(GeyserSession session, GeyserItemStack template, GeyserItemStack input, GeyserItemStack material) {
        return InventoryUtils.acceptsAsInput(session, base(), input)
            && InventoryUtils.acceptsAsInput(session, addition(), material)
            && InventoryUtils.acceptsAsInput(session, template(), template);
    }

    default List<T> asRecipeData(GeyserSession session, RecipeDataConstructor<T> constructor) {
        List<ItemDescriptorWithCount> bases = RecipeUtil.translateToInput(session, base());
        List<ItemDescriptorWithCount> templates = RecipeUtil.translateToInput(session, template());
        List<ItemDescriptorWithCount> additions = RecipeUtil.translateToInput(session, addition());
        if (bases == null || templates == null || additions == null) {
            return List.of();
        }

        List<T> recipeData = new ArrayList<>();
        int i = 0;
        for (ItemDescriptorWithCount template : templates) {
            for (ItemDescriptorWithCount base : bases) {
                for (ItemDescriptorWithCount addition : additions) {
                    // Note: vanilla inputs use aux value of Short.MAX_VALUE
                    recipeData.add(constructor.construct(template, base, addition, i));
                    i++;
                }
            }
        }
        return recipeData;
    }

    @FunctionalInterface
    interface RecipeDataConstructor<T extends RecipeData> {

        T construct(ItemDescriptorWithCount template, ItemDescriptorWithCount base, ItemDescriptorWithCount addition, int index);
    }

    record Transform(int id,
                    int netId,
                    SlotDisplay template,
                    SlotDisplay base,
                    SlotDisplay addition,
                    SlotDisplay result) implements GeyserSmithingRecipe<SmithingTransformRecipeData> {

        public static Transform of(int id, int netId, SmithingRecipeDisplay display) {
            return new Transform(id, netId, display.template(), display.base(), display.addition(), display.result());
        }

        @Override
        public List<SmithingTransformRecipeData> getCraftingDataList(CraftingDataPacket craftingDataPacket) {
            return craftingDataPacket.getSmithingTransformData();
        }

        @Override
        public List<SmithingTransformRecipeData> asRecipeData(GeyserSession session) {
            Pair<Item, ItemData> output = RecipeUtil.translateToOutput(session, result);
            if (output == null) {
                return List.of();
            }

            return asRecipeData(session, (template, base, addition, index)
                -> SmithingTransformRecipeData.of(id + "_" + index, template, base, addition, output.right(), "smithing_table", netId + index));
        }
    }

    record Trim(int id,
               int netId,
               SlotDisplay template,
               SlotDisplay base,
               SlotDisplay addition) implements GeyserSmithingRecipe<SmithingTrimRecipeData> {

        public static Trim of(int id, int netId, SmithingRecipeDisplay display) {
            return new Trim(id, netId, display.template(), display.base(), display.addition());
        }

        @Override
        public List<SmithingTrimRecipeData> getCraftingDataList(CraftingDataPacket craftingDataPacket) {
            return craftingDataPacket.getSmithingTrimData();
        }

        @Override
        public SlotDisplay result() {
            throw new UnsupportedOperationException("Result of trimming recipe should not be used");
        }

        @Override
        public List<SmithingTrimRecipeData> asRecipeData(GeyserSession session) {
            return asRecipeData(session, (template, base, addition, index)
                -> SmithingTrimRecipeData.of(id + "_" + index, base, addition, template, "smithing_table", netId + index));
        }
    }
}
