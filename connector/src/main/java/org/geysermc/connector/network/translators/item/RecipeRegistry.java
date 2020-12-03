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

package org.geysermc.connector.network.translators.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;

import java.io.InputStream;
import java.util.*;

/**
 * Manages any recipe-related storing
 */
public class RecipeRegistry {

    /**
     * Stores the last used recipe network ID. Since 1.16.200 (and for server-authoritative inventories),
     * each recipe needs a unique network ID (or else in .200 the client crashes).
     */
    public static int LAST_RECIPE_NET_ID = 0;

    /**
     * A list of all possible leather armor dyeing recipes.
     * Created manually.
     */
    public static final List<CraftingData> LEATHER_DYEING_RECIPES = new ObjectArrayList<>();
    /**
     * A list of all possible firework rocket recipes, including the base rocket.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static final List<CraftingData> FIREWORK_ROCKET_RECIPES = new ObjectArrayList<>();
    /**
     * A list of all possible firework star recipes.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static final List<CraftingData> FIREWORK_STAR_RECIPES = new ObjectArrayList<>();
    /**
     * A list of all possible shulker box dyeing options.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static final List<CraftingData> SHULKER_BOX_DYEING_RECIPES = new ObjectArrayList<>();
    /**
     * A list of all possible suspicious stew recipes.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static final List<CraftingData> SUSPICIOUS_STEW_RECIPES = new ObjectArrayList<>();
    /**
     * A list of all possible tipped arrow recipes.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static final List<CraftingData> TIPPED_ARROW_RECIPES = new ObjectArrayList<>();

    // TODO: Confirm that these are the correct functions for the multi ids
    /**
     * Recipe data that, when sent to the client, enables book cloning
     */
    public static final CraftingData BOOK_CLONING_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables tool repairing in a crafting table
     */
    public static final CraftingData TOOL_REPAIRING_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map extending in a crafting table
     */
    public static final CraftingData MAP_EXTENDING_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map extending in a cartography table
     */
    public static final CraftingData MAP_EXTENDING_CARTOGRAPHY_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map cloning in a crafting table
     */
    public static final CraftingData MAP_CLONING_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map cloning in a cartography table
     */
    public static final CraftingData MAP_CLONING_CARTOGRAPHY_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map upgrading in a crafting table or anvil
     */
    public static final CraftingData MAP_UPGRADING_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map upgrading in a cartography table
     */
    public static final CraftingData MAP_UPGRADING_CARTOGRAPHY_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables banner duplicating
     */
    public static final CraftingData BANNER_DUPLICATING_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables banner pattern adding
     */
    public static final CraftingData BANNER_ADD_PATTERN_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, does something to do with fireworks probably idrk
     */
    public static final CraftingData FIREWORKS_RECIPE_DATA;
    /**
     * Recipe data that, when sent to the client, enables map locking in a cartography table
     */
    public static final CraftingData MAP_LOCKING_RECIPE_DATA;


    static {
        BOOK_CLONING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("d1ca6b84-338e-4f2f-9c6b-76cc8b4bd98d"), LAST_RECIPE_NET_ID++);
        TOOL_REPAIRING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("00000000-0000-0000-0000-000000000001"), LAST_RECIPE_NET_ID++);
        MAP_EXTENDING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("d392b075-4ba1-40ae-8789-af868d56f6ce"), LAST_RECIPE_NET_ID++);
        MAP_EXTENDING_CARTOGRAPHY_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("8b36268c-1829-483c-a0f1-993b7156a8f2"), LAST_RECIPE_NET_ID++);
        MAP_CLONING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("85939755-ba10-4d9d-a4cc-efb7a8e943c4"), LAST_RECIPE_NET_ID++);
        MAP_CLONING_CARTOGRAPHY_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("442d85ed-8272-4543-a6f1-418f90ded05d"), LAST_RECIPE_NET_ID++);
        MAP_UPGRADING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("aecd2294-4b94-434b-8667-4499bb2c9327"), LAST_RECIPE_NET_ID++);
        MAP_UPGRADING_CARTOGRAPHY_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("98c84b38-1085-46bd-b1ce-dd38c159e6cc"), LAST_RECIPE_NET_ID++);
        BANNER_DUPLICATING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("b5c5d105-75a2-4076-af2b-923ea2bf4bf0"), LAST_RECIPE_NET_ID++);
        BANNER_ADD_PATTERN_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("d81aaeaf-e172-4440-9225-868df030d27b"), LAST_RECIPE_NET_ID++);
        FIREWORKS_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("00000000-0000-0000-0000-000000000002"), LAST_RECIPE_NET_ID++);
        MAP_LOCKING_RECIPE_DATA = CraftingData.fromMulti(UUID.fromString("602234e4-cac1-4353-8bb7-b1ebff70024b"), LAST_RECIPE_NET_ID++);

        // Get all recipes that are not directly sent from a Java server
        InputStream stream = FileUtils.getResource("mappings/recipes.json");

        JsonNode items;
        try {
            items = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError(LanguageUtils.getLocaleStringLog("geyser.toolbox.fail.runtime_java"), e);
        }

        for (JsonNode entry: items.get("leather_armor")) {
            // This won't be perfect, as we can't possibly send every leather input for every kind of color
            // But it does display the correct output from a base leather armor, and besides visuals everything works fine
            LEATHER_DYEING_RECIPES.add(getCraftingDataFromJsonNode(entry));
        }
        for (JsonNode entry : items.get("firework_rockets")) {
            FIREWORK_ROCKET_RECIPES.add(getCraftingDataFromJsonNode(entry));
        }
        for (JsonNode entry : items.get("firework_stars")) {
            FIREWORK_STAR_RECIPES.add(getCraftingDataFromJsonNode(entry));
        }
        for (JsonNode entry : items.get("shulker_boxes")) {
            SHULKER_BOX_DYEING_RECIPES.add(getCraftingDataFromJsonNode(entry));
        }
        for (JsonNode entry : items.get("suspicious_stew")) {
            SUSPICIOUS_STEW_RECIPES.add(getCraftingDataFromJsonNode(entry));
        }
        for (JsonNode entry : items.get("tipped_arrows")) {
            TIPPED_ARROW_RECIPES.add(getCraftingDataFromJsonNode(entry));
        }
    }

    /**
     * Computes a Bedrock crafting recipe from the given JSON data.
     * @param node the JSON data to compute
     * @return the {@link CraftingData} to send to the Bedrock client.
     */
    private static CraftingData getCraftingDataFromJsonNode(JsonNode node) {
        ItemData output = ItemRegistry.getBedrockItemFromJson(node.get("output").get(0));
        UUID uuid = UUID.randomUUID();
        if (node.get("type").asInt() == 1) {
            // Shaped recipe
            List<String> shape = new ArrayList<>();
            // Get the shape of the recipe
            for (JsonNode chars : node.get("shape")) {
                shape.add(chars.asText());
            }

            // In recipes.json each recipe is mapped by a letter
            Map<String, ItemData> letterToRecipe = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.get("input").fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                letterToRecipe.put(entry.getKey(), ItemRegistry.getBedrockItemFromJson(entry.getValue()));
            }

            ItemData[] inputs = new ItemData[shape.size() * shape.get(0).length()];
            int i = 0;
            // Create a linear array of items from the "cube" of the shape
            for (int j = 0; i < shape.size() * shape.get(0).length(); j++) {
                for (char c : shape.get(j).toCharArray()) {
                    ItemData data = letterToRecipe.getOrDefault(String.valueOf(c), ItemData.AIR);
                    inputs[i] = data;
                    i++;
                }
            }

            return CraftingData.fromShaped(uuid.toString(), shape.get(0).length(), shape.size(),
                    inputs, new ItemData[]{output}, uuid, "crafting_table", 0, LAST_RECIPE_NET_ID++);
        }
        List<ItemData> inputs = new ObjectArrayList<>();
        for (JsonNode entry : node.get("input")) {
            inputs.add(ItemRegistry.getBedrockItemFromJson(entry));
        }
        if (node.get("type").asInt() == 5) {
            // Shulker box
            return CraftingData.fromShulkerBox(uuid.toString(),
                    inputs.toArray(new ItemData[0]), new ItemData[]{output}, uuid, "crafting_table", 0, LAST_RECIPE_NET_ID++);
        }
        return CraftingData.fromShapeless(uuid.toString(),
                inputs.toArray(new ItemData[0]), new ItemData[]{output}, uuid, "crafting_table", 0, LAST_RECIPE_NET_ID++);
    }

    public static void init() {
        // no-op
    }
}
