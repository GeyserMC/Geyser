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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.StoneCuttingRecipeData;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import it.unimi.dsi.fastutil.ints.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.geysermc.geyser.util.InventoryUtils.LAST_RECIPE_NET_ID;

/**
 * Used to send all valid recipes from Java to Bedrock.
 *
 * Bedrock REQUIRES a CraftingDataPacket to be sent in order to craft anything.
 */
@Translator(packet = ClientboundUpdateRecipesPacket.class)
public class JavaUpdateRecipesTranslator extends PacketTranslator<ClientboundUpdateRecipesPacket> {
    /**
     * Required to use the specified cartography table recipes
     */
    private static final List<CraftingData> CARTOGRAPHY_RECIPES = Arrays.asList(
            CraftingData.fromMulti(UUID.fromString("8b36268c-1829-483c-a0f1-993b7156a8f2"), ++LAST_RECIPE_NET_ID), // Map extending
            CraftingData.fromMulti(UUID.fromString("442d85ed-8272-4543-a6f1-418f90ded05d"), ++LAST_RECIPE_NET_ID), // Map cloning
            CraftingData.fromMulti(UUID.fromString("98c84b38-1085-46bd-b1ce-dd38c159e6cc"), ++LAST_RECIPE_NET_ID), // Map upgrading
            CraftingData.fromMulti(UUID.fromString("602234e4-cac1-4353-8bb7-b1ebff70024b"), ++LAST_RECIPE_NET_ID) // Map locking
    );

    @Override
    public void translate(GeyserSession session, ClientboundUpdateRecipesPacket packet) {
        Map<RecipeType, List<CraftingData>> recipeTypes = Registries.CRAFTING_DATA.forVersion(session.getUpstream().getProtocolVersion());
        // Get the last known network ID (first used for the pregenerated recipes) and increment from there.
        int netId = InventoryUtils.LAST_RECIPE_NET_ID + 1;

        Int2ObjectMap<Recipe> recipeMap = new Int2ObjectOpenHashMap<>(Registries.RECIPES.forVersion(session.getUpstream().getProtocolVersion()));
        Int2ObjectMap<List<StoneCuttingRecipeData>> unsortedStonecutterData = new Int2ObjectOpenHashMap<>();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        for (Recipe recipe : packet.getRecipes()) {
            switch (recipe.getType()) {
                case CRAFTING_SHAPELESS -> {
                    ShapelessRecipeData shapelessRecipeData = (ShapelessRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapelessRecipeData.getResult());
                    if (output.equals(ItemData.AIR)) {
                        // Likely modded item that Bedrock will complain about if it persists
                        continue;
                    }
                    // Strip NBT - tools won't appear in the recipe book otherwise
                    output = output.toBuilder().tag(null).build();
                    ItemData[][] inputCombinations = combinations(session, shapelessRecipeData.getIngredients());
                    for (ItemData[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                                Arrays.asList(inputs), Collections.singletonList(output), uuid, "crafting_table", 0, netId));
                        recipeMap.put(netId++, recipe);
                    }
                }
                case CRAFTING_SHAPED -> {
                    ShapedRecipeData shapedRecipeData = (ShapedRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapedRecipeData.getResult());
                    if (output.equals(ItemData.AIR)) {
                        // Likely modded item that Bedrock will complain about if it persists
                        continue;
                    }
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
                }
                case STONECUTTING -> {
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
                default -> {
                    List<CraftingData> craftingData = recipeTypes.get(recipe.getType());
                    if (craftingData != null) {
                        craftingDataPacket.getCraftingData().addAll(craftingData);
                    }
                }
            }
        }
        craftingDataPacket.getCraftingData().addAll(CARTOGRAPHY_RECIPES);
        craftingDataPacket.getPotionMixData().addAll(Registries.POTION_MIXES.get());

        Int2ObjectMap<IntList> stonecutterRecipeMap = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<List<StoneCuttingRecipeData>> data : unsortedStonecutterData.int2ObjectEntrySet()) {
            // Sort the list by each output item's Java identifier - this is how it's sorted on Java, and therefore
            // We can get the correct order for button pressing
            data.getValue().sort(Comparator.comparing((stoneCuttingRecipeData ->
                    session.getItemMappings().getItems()
                            .getOrDefault(stoneCuttingRecipeData.getResult().getId(), ItemMapping.AIR)
                            .getJavaIdentifier())));

            // Now that it's sorted, let's translate these recipes
            for (StoneCuttingRecipeData stoneCuttingData : data.getValue()) {
                // As of 1.16.4, all stonecutter recipes have one ingredient option
                ItemStack ingredient = stoneCuttingData.getIngredient().getOptions()[0];
                ItemData input = ItemTranslator.translateToBedrock(session, ingredient);
                ItemData output = ItemTranslator.translateToBedrock(session, stoneCuttingData.getResult());
                if (input.equals(ItemData.AIR) || output.equals(ItemData.AIR)) {
                    // Probably modded items
                    continue;
                }
                UUID uuid = UUID.randomUUID();

                // We need to register stonecutting recipes so they show up on Bedrock
                craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                        Collections.singletonList(input), Collections.singletonList(output), uuid, "stonecutter", 0, netId++));

                // Save the recipe list for reference when crafting
                // Add the ingredient as the key and all possible values as the value
                IntList outputs = stonecutterRecipeMap.computeIfAbsent(ingredient.getId(), ($) -> new IntArrayList());
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
                    for (ItemMapping mapping : session.getItemMappings().getItems().values()) {
                        if (mapping.getBedrockId() == groupedItem.id) {
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
        for (Set<ItemData> optionSet : squashedOptions.keySet()) {
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
