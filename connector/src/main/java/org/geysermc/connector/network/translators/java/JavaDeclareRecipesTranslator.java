/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.nukkitx.protocol.bedrock.data.PotionMixData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;

import java.util.*;
import java.util.stream.Collectors;

@Translator(packet = ServerDeclareRecipesPacket.class)
public class JavaDeclareRecipesTranslator extends PacketTranslator<ServerDeclareRecipesPacket> {
    private static final Collection<PotionMixData> POTION_MIXES =
            Arrays.stream(new int[]{372, 331, 348, 376, 289, 437, 353, 414, 382, 375, 462, 378, 396, 377, 370, 469, 470})
            .mapToObj(ingredient -> new PotionMixData(0, ingredient, 0))
            .collect(Collectors.toList());

    @Override
    public void translate(ServerDeclareRecipesPacket packet, GeyserSession session) {
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        for (Recipe recipe : packet.getRecipes()) {
            switch (recipe.getType()) {
                case CRAFTING_SHAPELESS: {
                    ShapelessRecipeData shapelessRecipeData = (ShapelessRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapelessRecipeData.getResult());
                    output = ItemData.of(output.getId(), output.getDamage(), output.getCount()); //strip NBT
                    ItemData[][] inputCombinations = combinations(session, shapelessRecipeData.getIngredients());
                    for (ItemData[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                                inputs, new ItemData[]{output}, uuid, "crafting_table", 0));
                    }
                    break;
                }
                case CRAFTING_SHAPED: {
                    ShapedRecipeData shapedRecipeData = (ShapedRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapedRecipeData.getResult());
                    output = ItemData.of(output.getId(), output.getDamage(), output.getCount()); //strip NBT
                    ItemData[][] inputCombinations = combinations(session, shapedRecipeData.getIngredients());
                    for (ItemData[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShaped(uuid.toString(),
                                shapedRecipeData.getWidth(), shapedRecipeData.getHeight(), inputs,
                                new ItemData[]{output}, uuid, "crafting_table", 0));
                    }
                    break;
                }
            }
        }
        craftingDataPacket.getPotionMixData().addAll(POTION_MIXES);
        session.sendUpstreamPacket(craftingDataPacket);
    }

    //TODO: rewrite
    private ItemData[][] combinations(GeyserSession session, Ingredient[] ingredients) {
        Map<Set<ItemData>, IntSet> squashedOptions = new HashMap<>();
        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].getOptions().length == 0) {
                squashedOptions.computeIfAbsent(Collections.singleton(ItemData.AIR), k -> new IntOpenHashSet()).add(i);
                continue;
            }
            Ingredient ingredient = ingredients[i];
            Map<GroupedItem, List<ItemData>> groupedByIds = Arrays.stream(ingredient.getOptions())
                    .map(item -> ItemTranslator.translateToBedrock(session, item))
                    .collect(Collectors.groupingBy(item -> new GroupedItem(item.getId(), item.getCount(), item.getTag())));
            Set<ItemData> optionSet = new HashSet<>(groupedByIds.size());
            for (Map.Entry<GroupedItem, List<ItemData>> entry : groupedByIds.entrySet()) {
                if (entry.getValue().size() > 1) {
                    GroupedItem groupedItem = entry.getKey();
                    int idCount = 0;
                    //not optimal
                    for (ItemEntry itemEntry : ItemRegistry.ITEM_ENTRIES.values()) {
                        if (itemEntry.getBedrockId() == groupedItem.id) {
                            idCount++;
                        }
                    }
                    if (entry.getValue().size() < idCount) {
                        optionSet.addAll(entry.getValue());
                    } else {
                        optionSet.add(ItemData.of(groupedItem.id, (short) -1, groupedItem.count, groupedItem.tag));
                    }
                } else {
                    ItemData item = entry.getValue().get(0);
                    optionSet.add(item);
                }
            }
            squashedOptions.computeIfAbsent(optionSet, k -> new IntOpenHashSet()).add(i);
        }
        int totalCombinations = 1;
        for (Set optionSet : squashedOptions.keySet()) {
            totalCombinations *= optionSet.size();
        }
        if (totalCombinations > 500) {
            ItemData[] translatedItems = new ItemData[ingredients.length];
            for (int i = 0; i < ingredients.length; i++) {
                if (ingredients[i].getOptions().length > 0) {
                    translatedItems[i] = ItemTranslator.translateToBedrock(session, ingredients[i].getOptions()[0]);
                } else {
                    translatedItems[i] = ItemData.AIR;
                }
            }
            return new ItemData[][]{translatedItems};
        }
        List<Set<ItemData>> sortedSets = new ArrayList<>(squashedOptions.keySet());
        sortedSets.sort(Comparator.comparing(Set::size, Comparator.reverseOrder()));
        ItemData[][] combinations = new ItemData[totalCombinations][ingredients.length];
        int x = 1;
        for (Set<ItemData> set : sortedSets) {
            IntSet slotSet = squashedOptions.get(set);
            int i = 0;
            for (ItemData item : set) {
                for (int j = 0; j < totalCombinations / set.size(); j++) {
                    final int comboIndex = (i * x) + (j % x) + ((j / x) * set.size() * x);
                    for (int slot : slotSet) {
                        combinations[comboIndex][slot] = item;
                    }
                }
                i++;
            }
            x *= set.size();
        }
        return combinations;
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private static class GroupedItem {
        int id;
        int count;
        CompoundTag tag;
    }
}
