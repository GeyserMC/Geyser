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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.SmithingTransformRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.StoneCuttingRecipeData;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import it.unimi.dsi.fastutil.ints.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.inventory.recipe.TrimRecipe;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
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
    private static final List<RecipeData> CARTOGRAPHY_RECIPES = List.of(
            MultiRecipeData.of(UUID.fromString("8b36268c-1829-483c-a0f1-993b7156a8f2"), ++LAST_RECIPE_NET_ID), // Map extending
            MultiRecipeData.of(UUID.fromString("442d85ed-8272-4543-a6f1-418f90ded05d"), ++LAST_RECIPE_NET_ID), // Map cloning
            MultiRecipeData.of(UUID.fromString("98c84b38-1085-46bd-b1ce-dd38c159e6cc"), ++LAST_RECIPE_NET_ID), // Map upgrading
            MultiRecipeData.of(UUID.fromString("602234e4-cac1-4353-8bb7-b1ebff70024b"), ++LAST_RECIPE_NET_ID) // Map locking
    );

    private static final List<String> NETHERITE_UPGRADES = List.of(
            "minecraft:netherite_sword",
            "minecraft:netherite_shovel",
            "minecraft:netherite_pickaxe",
            "minecraft:netherite_axe",
            "minecraft:netherite_hoe",
            "minecraft:netherite_helmet",
            "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings",
            "minecraft:netherite_boots"
    );

    @Override
    public void translate(GeyserSession session, ClientboundUpdateRecipesPacket packet) {
        Map<RecipeType, List<RecipeData>> recipeTypes = Registries.CRAFTING_DATA.forVersion(session.getUpstream().getProtocolVersion());
        // Get the last known network ID (first used for the pregenerated recipes) and increment from there.
        int netId = InventoryUtils.LAST_RECIPE_NET_ID + 1;
        boolean sendTrimRecipes = false;

        Int2ObjectMap<GeyserRecipe> recipeMap = new Int2ObjectOpenHashMap<>(Registries.RECIPES.forVersion(session.getUpstream().getProtocolVersion()));
        Int2ObjectMap<List<StoneCuttingRecipeData>> unsortedStonecutterData = new Int2ObjectOpenHashMap<>();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        for (Recipe recipe : packet.getRecipes()) {
            switch (recipe.getType()) {
                case CRAFTING_SHAPELESS -> {
                    ShapelessRecipeData shapelessRecipeData = (ShapelessRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapelessRecipeData.getResult());
                    if (!output.isValid()) {
                        // Likely modded item that Bedrock will complain about if it persists
                        continue;
                    }
                    // Strip NBT - tools won't appear in the recipe book otherwise
                    output = output.toBuilder().tag(null).build();
                    ItemDescriptorWithCount[][] inputCombinations = combinations(session, shapelessRecipeData.getIngredients());
                    if (inputCombinations == null) {
                        continue;
                    }

                    for (ItemDescriptorWithCount[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData.shapeless(uuid.toString(),
                                Arrays.asList(inputs), Collections.singletonList(output), uuid, "crafting_table", 0, netId));
                        recipeMap.put(netId++, new GeyserShapelessRecipe(shapelessRecipeData));
                    }
                }
                case CRAFTING_SHAPED -> {
                    ShapedRecipeData shapedRecipeData = (ShapedRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, shapedRecipeData.getResult());
                    if (!output.isValid()) {
                        // Likely modded item that Bedrock will complain about if it persists
                        continue;
                    }
                    // See above
                    output = output.toBuilder().tag(null).build();
                    ItemDescriptorWithCount[][] inputCombinations = combinations(session, shapedRecipeData.getIngredients());
                    if (inputCombinations == null) {
                        continue;
                    }
                    for (ItemDescriptorWithCount[] inputs : inputCombinations) {
                        UUID uuid = UUID.randomUUID();
                        craftingDataPacket.getCraftingData().add(org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData.shaped(uuid.toString(),
                                shapedRecipeData.getWidth(), shapedRecipeData.getHeight(), Arrays.asList(inputs),
                                Collections.singletonList(output), uuid, "crafting_table", 0, netId));
                        recipeMap.put(netId++, new GeyserShapedRecipe(shapedRecipeData));
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
                case SMITHING_TRANSFORM -> {
                    SmithingTransformRecipeData data = (SmithingTransformRecipeData) recipe.getData();
                    ItemData output = ItemTranslator.translateToBedrock(session, data.getResult());

                    for (ItemStack template : data.getTemplate().getOptions()) {
                        ItemDescriptorWithCount bedrockTemplate = ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, template));

                        for (ItemStack base : data.getBase().getOptions()) {
                            ItemDescriptorWithCount bedrockBase = ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, base));

                            for (ItemStack addition : data.getAddition().getOptions()) {
                                ItemDescriptorWithCount bedrockAddition = ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, addition));

                                // Note: vanilla inputs use aux value of Short.MAX_VALUE
                                craftingDataPacket.getCraftingData().add(org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData.of(recipe.getIdentifier(),
                                    bedrockTemplate, bedrockBase, bedrockAddition, output, "smithing_table", netId++));
                            }
                        }
                    }

                }
                case SMITHING_TRIM -> {
                    sendTrimRecipes = true;
                    // ignored currently - see below
                }
                default -> {
                    List<RecipeData> craftingData = recipeTypes.get(recipe.getType());
                    if (craftingData != null) {
                        craftingDataPacket.getCraftingData().addAll(craftingData);
                    }
                }
            }
        }
        craftingDataPacket.getCraftingData().addAll(CARTOGRAPHY_RECIPES);
        craftingDataPacket.getPotionMixData().addAll(Registries.POTION_MIXES.forVersion(session.getUpstream().getProtocolVersion()));

        Int2ObjectMap<GeyserStonecutterData> stonecutterRecipeMap = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<List<StoneCuttingRecipeData>> data : unsortedStonecutterData.int2ObjectEntrySet()) {
            // Sort the list by each output item's Java identifier - this is how it's sorted on Java, and therefore
            // We can get the correct order for button pressing
            data.getValue().sort(Comparator.comparing((stoneCuttingRecipeData ->
                    Registries.JAVA_ITEMS.get().get(stoneCuttingRecipeData.getResult().getId())
                            .javaIdentifier())));

            // Now that it's sorted, let's translate these recipes
            int buttonId = 0;
            for (StoneCuttingRecipeData stoneCuttingData : data.getValue()) {
                // As of 1.16.4, all stonecutter recipes have one ingredient option
                ItemStack ingredient = stoneCuttingData.getIngredient().getOptions()[0];
                ItemData input = ItemTranslator.translateToBedrock(session, ingredient);
                ItemDescriptorWithCount descriptor = ItemDescriptorWithCount.fromItem(input);
                ItemStack javaOutput = stoneCuttingData.getResult();
                ItemData output = ItemTranslator.translateToBedrock(session, javaOutput);
                if (!input.isValid() || !output.isValid()) {
                    // Probably modded items
                    continue;
                }
                UUID uuid = UUID.randomUUID();

                // We need to register stonecutting recipes so they show up on Bedrock
                craftingDataPacket.getCraftingData().add(org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData.shapeless(uuid.toString(),
                        Collections.singletonList(descriptor), Collections.singletonList(output), uuid, "stonecutter", 0, netId));

                // Save the recipe list for reference when crafting
                // Add the net ID as the key and the button required + output for the value
                stonecutterRecipeMap.put(netId++, new GeyserStonecutterData(buttonId++, javaOutput));
            }
        }

        session.getLastRecipeNetId().set(netId);

        // Only send smithing trim recipes if Java/ViaVersion sends them.
        if (sendTrimRecipes) {
            // BDS sends armor trim templates and materials before the CraftingDataPacket
            TrimDataPacket trimDataPacket = new TrimDataPacket();
            trimDataPacket.getPatterns().addAll(TrimRecipe.PATTERNS);
            trimDataPacket.getMaterials().addAll(TrimRecipe.MATERIALS);
            session.sendUpstreamPacket(trimDataPacket);

            // Identical smithing_trim recipe sent by BDS that uses tag-descriptors, as the client seems to ignore the
            // approach of using many default-descriptors (which we do for smithing_transform)
            craftingDataPacket.getCraftingData().add(SmithingTrimRecipeData.of(TrimRecipe.ID,
                    TrimRecipe.BASE, TrimRecipe.ADDITION, TrimRecipe.TEMPLATE, "smithing_table", session.getLastRecipeNetId().getAndIncrement()));
        } else {
            // manually add recipes for the upgrade template (workaround), since Java pre-1.20 doesn't
            craftingDataPacket.getCraftingData().addAll(getSmithingTransformRecipes(session));
        }
        session.setOldSmithingTable(!sendTrimRecipes);
        session.sendUpstreamPacket(craftingDataPacket);
        session.setCraftingRecipes(recipeMap);
        session.setStonecutterRecipes(stonecutterRecipeMap);
    }

    //TODO: rewrite
    /**
     * The Java server sends an array of items for each ingredient you can use per slot in the crafting grid.
     * Bedrock recipes take only one ingredient per crafting grid slot.
     *
     * @return the Java ingredient list as an array that Bedrock can understand
     */
    private ItemDescriptorWithCount[][] combinations(GeyserSession session, Ingredient[] ingredients) {
        boolean empty = true;
        Map<Set<ItemDescriptorWithCount>, IntSet> squashedOptions = new HashMap<>();
        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].getOptions().length == 0) {
                squashedOptions.computeIfAbsent(Collections.singleton(ItemDescriptorWithCount.EMPTY), k -> new IntOpenHashSet()).add(i);
                continue;
            }
            empty = false;
            Ingredient ingredient = ingredients[i];
            Map<GroupedItem, List<ItemDescriptorWithCount>> groupedByIds = Arrays.stream(ingredient.getOptions())
                    .map(item -> ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, item)))
                    .collect(Collectors.groupingBy(item -> item == ItemDescriptorWithCount.EMPTY ? new GroupedItem(ItemDefinition.AIR, 0) : new GroupedItem(((DefaultDescriptor) item.getDescriptor()).getItemId(), item.getCount())));
            Set<ItemDescriptorWithCount> optionSet = new HashSet<>(groupedByIds.size());
            for (Map.Entry<GroupedItem, List<ItemDescriptorWithCount>> entry : groupedByIds.entrySet()) {
                if (entry.getValue().size() > 1) {
                    GroupedItem groupedItem = entry.getKey();
                    int idCount = 0;
                    //not optimal
                    for (ItemMapping mapping : session.getItemMappings().getItems()) {
                        if (mapping.getBedrockDefinition() == groupedItem.id) {
                            idCount++;
                        }
                    }
                    if (entry.getValue().size() < idCount) {
                        optionSet.addAll(entry.getValue());
                    } else {
                        optionSet.add(groupedItem.id == ItemDefinition.AIR ? ItemDescriptorWithCount.EMPTY : new ItemDescriptorWithCount(new DefaultDescriptor(groupedItem.id, Short.MAX_VALUE), groupedItem.count));
                    }
                } else {
                    ItemDescriptorWithCount item = entry.getValue().get(0);
                    optionSet.add(item);
                }
            }
            squashedOptions.computeIfAbsent(optionSet, k -> new IntOpenHashSet()).add(i);
        }
        if (empty) {
            // Crashes Bedrock 1.19.70 otherwise
            // Fixes https://github.com/GeyserMC/Geyser/issues/3549
            return null;
        }
        int totalCombinations = 1;
        for (Set<ItemDescriptorWithCount> optionSet : squashedOptions.keySet()) {
            totalCombinations *= optionSet.size();
        }
        if (totalCombinations > 500) {
            ItemDescriptorWithCount[] translatedItems = new ItemDescriptorWithCount[ingredients.length];
            for (int i = 0; i < ingredients.length; i++) {
                if (ingredients[i].getOptions().length > 0) {
                    translatedItems[i] = ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, ingredients[i].getOptions()[0]));
                } else {
                    translatedItems[i] = ItemDescriptorWithCount.EMPTY;
                }
            }
            return new ItemDescriptorWithCount[][]{translatedItems};
        }
        List<Set<ItemDescriptorWithCount>> sortedSets = new ArrayList<>(squashedOptions.keySet());
        sortedSets.sort(Comparator.comparing(Set::size, Comparator.reverseOrder()));
        ItemDescriptorWithCount[][] combinations = new ItemDescriptorWithCount[totalCombinations][ingredients.length];
        int x = 1;
        for (Set<ItemDescriptorWithCount> set : sortedSets) {
            IntSet slotSet = squashedOptions.get(set);
            int i = 0;
            for (ItemDescriptorWithCount item : set) {
                for (int j = 0; j < totalCombinations / set.size(); j++) {
                    final int comboIndex = (i * x) + (j % x) + ((j / x) * set.size() * x);
                    for (IntIterator it = slotSet.iterator(); it.hasNext(); ) {
                        combinations[comboIndex][it.nextInt()] = item;
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
        ItemDefinition id;
        int count;
    }

    private List<RecipeData> getSmithingTransformRecipes(GeyserSession session) {
        List<RecipeData> recipes = new ArrayList<>();
        ItemMapping template = session.getItemMappings().getStoredItems().upgradeTemplate();

        for (String identifier : NETHERITE_UPGRADES) {
            recipes.add(org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData.of(identifier + "_smithing",
                    getDescriptorFromId(session, template.getBedrockIdentifier()),
                    getDescriptorFromId(session, identifier.replace("netherite", "diamond")),
                    getDescriptorFromId(session, "minecraft:netherite_ingot"),
                    ItemData.builder().definition(Objects.requireNonNull(session.getItemMappings().getDefinition(identifier))).count(1).build(),
                    "smithing_table",
                    session.getLastRecipeNetId().getAndIncrement()));
        }
        return recipes;
    }

    private ItemDescriptorWithCount getDescriptorFromId(GeyserSession session, String bedrockId) {
        ItemDefinition bedrockDefinition = session.getItemMappings().getDefinition(bedrockId);
        if (bedrockDefinition != null) {
            return ItemDescriptorWithCount.fromItem(ItemData.builder().definition(bedrockDefinition).count(1).build());
        }
        GeyserImpl.getInstance().getLogger().debug("Unable to find item with identifier " + bedrockId);
        return ItemDescriptorWithCount.EMPTY;
    }
}
