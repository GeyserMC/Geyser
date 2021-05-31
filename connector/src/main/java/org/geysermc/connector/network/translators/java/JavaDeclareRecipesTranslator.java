/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.StoneCuttingRecipeData;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareRecipesPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import it.unimi.dsi.fastutil.ints.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Used to send all valid recipes from Java to Bedrock.
 *
 * Bedrock REQUIRES a CraftingDataPacket to be sent in order to craft anything.
 */
@Translator(packet = ServerDeclareRecipesPacket.class)
public class JavaDeclareRecipesTranslator extends PacketTranslator<ServerDeclareRecipesPacket> {

    @Override
    public void translate(ServerDeclareRecipesPacket packet, GeyserSession session) {
        // Get the last known network ID (first used for the pregenerated recipes) and increment from there.
        int netId = RecipeRegistry.LAST_RECIPE_NET_ID + 1;
        Int2ObjectMap<Recipe> recipeMap = new Int2ObjectOpenHashMap<>(RecipeRegistry.ALL_CRAFTING_RECIPES);
        Int2ObjectMap<List<StoneCuttingRecipeData>> unsortedStonecutterData = new Int2ObjectOpenHashMap<>();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        for (Recipe recipe : packet.getRecipes()) {
            switch (recipe.getType()) {
                case CRAFTING_SHAPELESS: {
                    ShapelessRecipeData shapelessRecipeData = (ShapelessRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapelessRecipeData.getResult());
                    // Strip NBT - tools won't appear in the recipe book otherwise
                    output = output.toBuilder().tag(null).build();
                    ItemData[][] inputCombinations = combinations(session, shapelessRecipeData.getIngredients());
                    for (ItemData[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                                Arrays.asList(inputs), Collections.singletonList(output), uuid, "crafting_table", 0, netId));
                        recipeMap.put(netId++, recipe);
                    }
                    break;
                }
                case CRAFTING_SHAPED: {
                    ShapedRecipeData shapedRecipeData = (ShapedRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapedRecipeData.getResult());
                    // See above
                    output = output.toBuilder().tag(null).build();
                    ItemData[][] inputCombinations = combinations(session, shapedRecipeData.getIngredients());
                    for (ItemData[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShaped(uuid.toString(),
                                shapedRecipeData.getWidth(), shapedRecipeData.getHeight(), Arrays.asList(inputs),
                                        Collections.singletonList(output), uuid, "crafting_table", 0, netId));
                        recipeMap.put(netId++, recipe);
                    }
                    break;
                }

                // These recipes are enabled by sending a special recipe
                case CRAFTING_SPECIAL_BOOKCLONING: {
                    craftingDataPacket.getCraftingData().add(RecipeRegistry.BOOK_CLONING_RECIPE_DATA);
                    break;
                }
                case CRAFTING_SPECIAL_REPAIRITEM: {
                    craftingDataPacket.getCraftingData().add(RecipeRegistry.TOOL_REPAIRING_RECIPE_DATA);
                    break;
                }
                case CRAFTING_SPECIAL_MAPCLONING: {
                    craftingDataPacket.getCraftingData().add(RecipeRegistry.MAP_CLONING_RECIPE_DATA);
                    break;
                }
                case CRAFTING_SPECIAL_MAPEXTENDING: {
                    craftingDataPacket.getCraftingData().add(RecipeRegistry.MAP_EXTENDING_RECIPE_DATA);
                    break;
                }
                case CRAFTING_SPECIAL_BANNERDUPLICATE: {
                    craftingDataPacket.getCraftingData().add(RecipeRegistry.BANNER_DUPLICATING_RECIPE_DATA);
                    break;
                }

                // Java doesn't actually tell us the recipes so we need to calculate this ahead of time.
                case CRAFTING_SPECIAL_FIREWORK_ROCKET: {
                    craftingDataPacket.getCraftingData().addAll(RecipeRegistry.FIREWORK_ROCKET_RECIPES);
                    break;
                }
                case CRAFTING_SPECIAL_FIREWORK_STAR: {
                    craftingDataPacket.getCraftingData().addAll(RecipeRegistry.FIREWORK_STAR_RECIPES);
                    break;
                }
                case CRAFTING_SPECIAL_SHULKERBOXCOLORING: {
                    craftingDataPacket.getCraftingData().addAll(RecipeRegistry.SHULKER_BOX_DYEING_RECIPES);
                    break;
                }
                case CRAFTING_SPECIAL_SUSPICIOUSSTEW: {
                    craftingDataPacket.getCraftingData().addAll(RecipeRegistry.SUSPICIOUS_STEW_RECIPES);
                    break;
                }
                case CRAFTING_SPECIAL_TIPPEDARROW: {
                    craftingDataPacket.getCraftingData().addAll(RecipeRegistry.TIPPED_ARROW_RECIPES);
                    break;
                }
                case CRAFTING_SPECIAL_ARMORDYE: {
                    // This one's even worse since it's not actually on Bedrock, but it still works!
                    craftingDataPacket.getCraftingData().addAll(RecipeRegistry.LEATHER_DYEING_RECIPES);
                    break;
                }
                case STONECUTTING: {
                    StoneCuttingRecipeData stoneCuttingData = (StoneCuttingRecipeData) recipe.getData();
                    ItemStack ingredient = stoneCuttingData.getIngredient().getOptions()[0];
                    List<StoneCuttingRecipeData> data = unsortedStonecutterData.get(ingredient.getId());
                    if (data == null) {
                        data = new ArrayList<>();
                        unsortedStonecutterData.put(ingredient.getId(), data);
                    }
                    data.add(stoneCuttingData);
                    // Save for processing after all recipes have been received
                }
            }
        }
        // Add all cartography table recipe UUIDs, so we can use the cartography table
        craftingDataPacket.getCraftingData().addAll(RecipeRegistry.CARTOGRAPHY_RECIPE_DATA);

        craftingDataPacket.getPotionMixData().addAll(PotionMixRegistry.POTION_MIXES);

        Int2ObjectMap<IntList> stonecutterRecipeMap = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<List<StoneCuttingRecipeData>> data : unsortedStonecutterData.int2ObjectEntrySet()) {
            // Sort the list by each output item's Java identifier - this is how it's sorted on Java, and therefore
            // We can get the correct order for button pressing
            data.getValue().sort(Comparator.comparing((stoneCuttingRecipeData ->
                    ItemRegistry.getItem(stoneCuttingRecipeData.getResult()).getJavaIdentifier())));

            // Now that it's sorted, let's translate these recipes
            for (StoneCuttingRecipeData stoneCuttingData : data.getValue()) {
                // As of 1.16.4, all stonecutter recipes have one ingredient option
                ItemStack ingredient = stoneCuttingData.getIngredient().getOptions()[0];
                ItemData input = ItemTranslator.translateToBedrock(session, ingredient);
                ItemData output = ItemTranslator.translateToBedrock(session, stoneCuttingData.getResult());
                UUID uuid = UUID.randomUUID();

                // We need to register stonecutting recipes so they show up on Bedrock
                craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                        Collections.singletonList(input), Collections.singletonList(output), uuid, "stonecutter", 0, netId++));

                // Save the recipe list for reference when crafting
                IntList outputs = stonecutterRecipeMap.get(ingredient.getId());
                if (outputs == null) {
                    outputs = new IntArrayList();
                    // Add the ingredient as the key and all possible values as the value
                    stonecutterRecipeMap.put(ingredient.getId(), outputs);
                }
                outputs.add(stoneCuttingData.getResult().getId());
            }
        }

        session.sendUpstreamPacket(craftingDataPacket);
        session.setCraftingRecipes(recipeMap);
        session.getUnlockedRecipes().clear();
        session.setStonecutterRecipes(stonecutterRecipeMap);
        session.getLastRecipeNetId().set(netId);
    }

    //TODO: rewrite
    /**
     * The Java server sends an array of items for each ingredient you can use per slot in the crafting grid.
     * Bedrock recipes take only one ingredient per crafting grid slot.
     *
     * @return the Java ingredient list as an array that Bedrock can understand
     */
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
                        optionSet.add(ItemData.builder()
                                .id(groupedItem.id)
                                .damage(Short.MAX_VALUE)
                                .count(groupedItem.count)
                                .tag(groupedItem.tag).build());
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
        NbtMap tag;
    }
}
