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

package org.geysermc.geyser.registry.populator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.geysermc.geyser.util.InventoryUtils.LAST_RECIPE_NET_ID;

/**
 * Populates the recipe registry.
 */
public class RecipeRegistryPopulator {

    public static void populate() {
        JsonNode items;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/recipes.json")) {
            items = GeyserImpl.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.toolbox.fail.runtime_java"), e);
        }

        int currentRecipeId = LAST_RECIPE_NET_ID;
        for (Int2ObjectMap.Entry<ItemMappings> version : Registries.ITEMS.get().int2ObjectEntrySet()) {
            // Make a bit of an assumption here that the last recipe net ID will be equivalent between all versions
            LAST_RECIPE_NET_ID = currentRecipeId;
            Map<RecipeType, List<RecipeData>> craftingData = new EnumMap<>(RecipeType.class);
            Int2ObjectMap<GeyserRecipe> recipes = new Int2ObjectOpenHashMap<>();

            craftingData.put(RecipeType.CRAFTING_SPECIAL_BOOKCLONING,
                    Collections.singletonList(MultiRecipeData.of(UUID.fromString("d1ca6b84-338e-4f2f-9c6b-76cc8b4bd98d"), ++LAST_RECIPE_NET_ID)));
            craftingData.put(RecipeType.CRAFTING_SPECIAL_REPAIRITEM,
                    Collections.singletonList(MultiRecipeData.of(UUID.fromString("00000000-0000-0000-0000-000000000001"), ++LAST_RECIPE_NET_ID)));
            craftingData.put(RecipeType.CRAFTING_SPECIAL_MAPEXTENDING,
                    Collections.singletonList(MultiRecipeData.of(UUID.fromString("d392b075-4ba1-40ae-8789-af868d56f6ce"), ++LAST_RECIPE_NET_ID)));
            craftingData.put(RecipeType.CRAFTING_SPECIAL_MAPCLONING,
                    Collections.singletonList(MultiRecipeData.of(UUID.fromString("85939755-ba10-4d9d-a4cc-efb7a8e943c4"), ++LAST_RECIPE_NET_ID)));

            // https://github.com/pmmp/PocketMine-MP/blob/stable/src/pocketmine/inventory/MultiRecipe.php

            for (JsonNode entry : items.get("leather_armor")) {
                // This won't be perfect, as we can't possibly send every leather input for every kind of color
                // But it does display the correct output from a base leather armor, and besides visuals everything works fine
                craftingData.computeIfAbsent(RecipeType.CRAFTING_SPECIAL_ARMORDYE,
                        c -> new ObjectArrayList<>()).add(getCraftingDataFromJsonNode(entry, recipes, version.getValue()));
            }
            for (JsonNode entry : items.get("firework_rockets")) {
                craftingData.computeIfAbsent(RecipeType.CRAFTING_SPECIAL_FIREWORK_ROCKET,
                        c -> new ObjectArrayList<>()).add(getCraftingDataFromJsonNode(entry, recipes, version.getValue()));
            }
            for (JsonNode entry : items.get("firework_stars")) {
                craftingData.computeIfAbsent(RecipeType.CRAFTING_SPECIAL_FIREWORK_STAR,
                        c -> new ObjectArrayList<>()).add(getCraftingDataFromJsonNode(entry, recipes, version.getValue()));
            }
            for (JsonNode entry : items.get("shulker_boxes")) {
                craftingData.computeIfAbsent(RecipeType.CRAFTING_SPECIAL_SHULKERBOXCOLORING,
                        c -> new ObjectArrayList<>()).add(getCraftingDataFromJsonNode(entry, recipes, version.getValue()));
            }
            for (JsonNode entry : items.get("suspicious_stew")) {
                craftingData.computeIfAbsent(RecipeType.CRAFTING_SPECIAL_SUSPICIOUSSTEW,
                        c -> new ObjectArrayList<>()).add(getCraftingDataFromJsonNode(entry, recipes, version.getValue()));
            }
            for (JsonNode entry : items.get("tipped_arrows")) {
                craftingData.computeIfAbsent(RecipeType.CRAFTING_SPECIAL_TIPPEDARROW,
                        c -> new ObjectArrayList<>()).add(getCraftingDataFromJsonNode(entry, recipes, version.getValue()));
            }

            Registries.CRAFTING_DATA.register(version.getIntKey(), craftingData);
            Registries.RECIPES.register(version.getIntKey(), recipes);
        }
    }

    /**
     * Computes a Bedrock crafting recipe from the given JSON data.
     * @param node the JSON data to compute
     * @param recipes a list of all the recipes
     * @return the {@link RecipeData} to send to the Bedrock client.
     */
    private static RecipeData getCraftingDataFromJsonNode(JsonNode node, Int2ObjectMap<GeyserRecipe> recipes, ItemMappings mappings) {
        int netId = ++LAST_RECIPE_NET_ID;
        int type = node.get("bedrockRecipeType").asInt();
        JsonNode outputNode = node.get("output");
        ItemMapping outputEntry = mappings.getMapping(outputNode.get("identifier").asText());
        ItemData output = getBedrockItemFromIdentifierJson(outputEntry, outputNode);
        UUID uuid = UUID.randomUUID();
        if (type == 1) {
            // Shaped recipe
            List<String> shape = new ArrayList<>();
            // Get the shape of the recipe
            for (JsonNode chars : node.get("shape")) {
                shape.add(chars.asText());
            }

            // In recipes.json each recipe is mapped by a letter
            Map<String, ItemData> letterToRecipe = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.get("inputs").fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                JsonNode inputNode = entry.getValue();
                ItemMapping inputEntry = mappings.getMapping(inputNode.get("identifier").asText());
                letterToRecipe.put(entry.getKey(), getBedrockItemFromIdentifierJson(inputEntry, inputNode));
            }

            List<ItemData> inputs = new ArrayList<>(shape.size() * shape.get(0).length());
            int i = 0;
            // Create a linear array of items from the "cube" of the shape
            for (int j = 0; i < shape.size() * shape.get(0).length(); j++) {
                for (char c : shape.get(j).toCharArray()) {
                    ItemData data = letterToRecipe.getOrDefault(String.valueOf(c), ItemData.AIR);
                    inputs.add(data);
                    i++;
                }
            }

            /* Convert into a Java recipe class for autocrafting */
            List<Ingredient> ingredients = new ArrayList<>();
            for (ItemData input : inputs) {
                ingredients.add(new Ingredient(new ItemStack[]{ItemTranslator.translateToJava(input, mappings)}));
            }
            GeyserRecipe recipe = new GeyserShapedRecipe(shape.get(0).length(), shape.size(),
                    ingredients.toArray(new Ingredient[0]), ItemTranslator.translateToJava(output, mappings));
            recipes.put(netId, recipe);
            /* Convert end */

            return ShapedRecipeData.shaped(uuid.toString(), shape.get(0).length(), shape.size(),
                    inputs.stream().map(ItemDescriptorWithCount::fromItem).toList(), Collections.singletonList(output), uuid, "crafting_table", 0, netId);
        }
        List<ItemData> inputs = new ObjectArrayList<>();
        for (JsonNode entry : node.get("inputs")) {
            ItemMapping inputEntry = mappings.getMapping(entry.get("identifier").asText());
            inputs.add(getBedrockItemFromIdentifierJson(inputEntry, entry));
        }

        /* Convert into a Java Recipe class for autocrafting */
        List<Ingredient> ingredients = new ArrayList<>();
        for (ItemData input : inputs) {
            ingredients.add(new Ingredient(new ItemStack[]{ItemTranslator.translateToJava(input, mappings)}));
        }
        GeyserRecipe recipe = new GeyserShapelessRecipe(ingredients.toArray(new Ingredient[0]), ItemTranslator.translateToJava(output, mappings));
        recipes.put(netId, recipe);
        /* Convert end */

        if (type == 5) {
            // Shulker box
            return ShapelessRecipeData.shulkerBox(uuid.toString(),
                    inputs.stream().map(ItemDescriptorWithCount::fromItem).toList(), Collections.singletonList(output), uuid, "crafting_table", 0, netId);
        }
        return ShapelessRecipeData.shapeless(uuid.toString(),
                inputs.stream().map(ItemDescriptorWithCount::fromItem).toList(), Collections.singletonList(output), uuid, "crafting_table", 0, netId);
    }

    private static ItemData getBedrockItemFromIdentifierJson(ItemMapping mapping, JsonNode itemNode) {
        int count = 1;
        short damage = 0;
        NbtMap tag = null;
        JsonNode damageNode = itemNode.get("bedrockDamage");
        if (damageNode != null) {
            damage = damageNode.numberValue().shortValue();
        }
        JsonNode countNode = itemNode.get("count");
        if (countNode != null) {
            count = countNode.asInt();
        }
        JsonNode nbtNode = itemNode.get("bedrockNbt");
        if (nbtNode != null) {
            byte[] bytes = Base64.getDecoder().decode(nbtNode.asText());
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                tag = (NbtMap) NbtUtils.createReaderLE(bais).readTag();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ItemData.builder()
                .definition(mapping.getBedrockDefinition())
                .damage(damage)
                .count(count)
                .blockDefinition(mapping.getBedrockBlockDefinition())
                .tag(tag)
                .build();
    }
}
