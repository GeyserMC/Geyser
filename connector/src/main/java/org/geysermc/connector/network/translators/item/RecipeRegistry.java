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
import java.util.List;
import java.util.UUID;

/**
 * Manages any recipe-related storing
 */
public class RecipeRegistry {

    /**
     * A list of all possible leather armor dyeing recipes.
     * Created manually.
     */
    public static List<CraftingData> LEATHER_DYEING_RECIPES = new ObjectArrayList<>();
    /**
     * A list of all possible firework rocket recipes, including the base rocket.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static List<CraftingData> FIREWORK_ROCKET_RECIPES = new ObjectArrayList<>(21);
    /**
     * A list of all possible firework star recipes.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static List<CraftingData> FIREWORK_STAR_RECIPES = new ObjectArrayList<>(40);
    /**
     * A list of all possible shulker box dyeing options.
     * Obtained from a ProxyPass dump of protocol v407
     */
    public static List<CraftingData> SHULKER_BOX_DYEING_RECIPES = new ObjectArrayList<>();

    static {
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
    }

    /**
     * Computes a Bedrock crafting recipe from the given JSON data.
     * @param node the JSON data to compute
     * @return the {@link CraftingData} to send to the Bedrock client.
     */
    private static CraftingData getCraftingDataFromJsonNode(JsonNode node) {
        ItemData output = ItemRegistry.getBedrockItemFromJson(node.get("output").get(0));
        List<ItemData> inputs = new ObjectArrayList<>();
        for (JsonNode entry : node.get("input")) {
            inputs.add(ItemRegistry.getBedrockItemFromJson(entry));
        }
        UUID uuid = UUID.randomUUID();
        if (node.get("type").asInt() == 5) {
            // Shulker box
            return CraftingData.fromShulkerBox(uuid.toString(),
                    inputs.toArray(new ItemData[0]), new ItemData[]{output}, uuid, "crafting_table", 0);
        }
        return CraftingData.fromShapeless(uuid.toString(),
                inputs.toArray(new ItemData[0]), new ItemData[]{output}, uuid, "crafting_table", 0);
    }

    public static void init() {
        // no-op
    }
}
