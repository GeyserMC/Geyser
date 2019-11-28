/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareRecipesPacket;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.CraftingData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;

import java.util.*;
import java.util.stream.Collectors;

public class JavaDeclareRecipesTranslator extends PacketTranslator<ServerDeclareRecipesPacket> {

    @Override
    public void translate(ServerDeclareRecipesPacket packet, GeyserSession session) {
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        for (Recipe recipe : packet.getRecipes()) {
            switch (recipe.getType()) {
                case CRAFTING_SHAPELESS: {
                    ShapelessRecipeData shapelessRecipeData = (ShapelessRecipeData) recipe.getData();
                    ItemData output = TranslatorsInit.getItemTranslator().translateToBedrock(shapelessRecipeData.getResult());
                    List<ItemData[]> inputList = combinations(shapelessRecipeData.getIngredients());
                    for (ItemData[] inputs : inputList) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                                inputs, new ItemData[]{output}, uuid, "crafting_table", 0));
                    }
                    break;
                }
                case CRAFTING_SHAPED: {
                    ShapedRecipeData shapedRecipeData = (ShapedRecipeData) recipe.getData();
                    ItemData output = TranslatorsInit.getItemTranslator().translateToBedrock(shapedRecipeData.getResult());
                    List<ItemData[]> inputList = combinations(shapedRecipeData.getIngredients());
                    for (ItemData[] inputs : inputList) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShaped(uuid.toString(),
                                shapedRecipeData.getWidth(), shapedRecipeData.getHeight(), inputs,
                                new ItemData[]{output}, uuid, "crafting_table", 0));
                    }
                    break;
                }
            }
        }
        session.getUpstream().sendPacket(craftingDataPacket);
    }

    private List<ItemData[]> combinations(Ingredient[] ingredients) {
        ItemData[][] squashed = new ItemData[ingredients.length][];
        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].getOptions().length == 0) {
                squashed[i] = new ItemData[]{ItemData.AIR};
                continue;
            }
            Ingredient ingredient = ingredients[i];
            Map<GroupedItem, List<ItemData>> groupedByIds = Arrays.stream(ingredient.getOptions())
                    .map(item -> TranslatorsInit.getItemTranslator().translateToBedrock(item))
                    .collect(Collectors.groupingBy(item -> new GroupedItem(item.getId(), item.getCount(), item.getTag())));
            squashed[i] = new ItemData[groupedByIds.size()];
            int index = 0;
            for (Map.Entry<GroupedItem, List<ItemData>> entry : groupedByIds.entrySet()) {
                if (entry.getValue().size() > 1) {
                    GroupedItem groupedItem = entry.getKey();
                    squashed[i][index++] = ItemData.of(groupedItem.id, (short) -1, groupedItem.count, groupedItem.tag);
                } else {
                    ItemData item = entry.getValue().get(0);
                    squashed[i][index++] = item;
                }
            }
        }
        int[] sizeArray = new int[squashed.length];
        int[] counterArray = new int[squashed.length];
        int totalCombinationCount = 1;
        for(int i = 0; i < squashed.length; i++) {
            sizeArray[i] = squashed[i].length;
            totalCombinationCount *= squashed[i].length;
        }
        if (totalCombinationCount > 10000) {
            ItemData[] translatedItems = new ItemData[ingredients.length];
            for (int i = 0; i < ingredients.length; i++) {
                if (ingredients[i].getOptions().length > 0) {
                    translatedItems[i] = TranslatorsInit.getItemTranslator().translateToBedrock(ingredients[i].getOptions()[0]);
                } else {
                    translatedItems[i] = ItemData.AIR;
                }
            }
            return Collections.singletonList(translatedItems);
        }
        List<ItemData[]> combinationList = new ArrayList<>(totalCombinationCount);
        for (int countdown = totalCombinationCount; countdown > 0; --countdown) {
            ItemData[] translatedItems = new ItemData[squashed.length];
            for(int i = 0; i < squashed.length; ++i) {
                if (squashed[i].length > 0)
                    translatedItems[i] = squashed[i][counterArray[i]];
            }
            combinationList.add(translatedItems);
            for(int incIndex = squashed.length - 1; incIndex >= 0; --incIndex) {
                if(counterArray[incIndex] + 1 < sizeArray[incIndex]) {
                    ++counterArray[incIndex];
                    break;
                }
                counterArray[incIndex] = 0;
            }
        }
        return combinationList;
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private static class GroupedItem {
        int id;
        int count;
        CompoundTag tag;
    }
}
