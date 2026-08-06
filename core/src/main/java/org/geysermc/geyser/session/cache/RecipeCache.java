/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.CraftingRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserSmithingRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.inventory.recipe.TrimRecipes;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.FurnaceRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayEntry;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapedCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapelessCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SmithingTrimDemoSlotDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.geysermc.geyser.inventory.recipe.RecipeUtil.CARTOGRAPHY_RECIPES;

@Getter
public class RecipeCache {
    private final GeyserSession session;
    /**
     * Stores all Java recipes by ID, and matches them to all possible Bedrock recipe identifiers.
     */
    private final Int2ObjectMap<List<String>> javaToBedrockRecipeIds = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectMap<GeyserRecipe<?>> craftingRecipes = new Int2ObjectOpenHashMap<>();
    @Setter
    private Pair<CraftingRecipeData, GeyserRecipe<?>> lastCreatedRecipe = null; // TODO try to prevent sending duplicate recipes
    private final AtomicInteger lastRecipeNetId = new AtomicInteger(InventoryUtils.LAST_RECIPE_NET_ID + 1);

    /**
     * Used to minimize the amount of recipes sent to the client, as the packet is quite heavy
     * when sent in rapid succession
     */
    @Setter
    private boolean cleanRecipesRequired = true;

    /**
     * Saves a list of all stonecutter recipes, for use in a stonecutter inventory.
     * The key is the Bedrock recipe net ID; the values are their respective output and button ID.
     */
    @Setter
    private Int2ObjectMap<GeyserStonecutterData> stonecutterRecipes = Int2ObjectMaps.emptyMap();
    private final List<GeyserSmithingRecipe> smithingRecipes = new ArrayList<>();
    private final TrimRecipes trimRecipes = new TrimRecipes();

    public RecipeCache(GeyserSession session) {
        this.session = session;
    }

    public void readdAllRecipes(CraftingDataPacket craftingDataPacket) {
        craftingDataPacket.setCleanRecipes(true);
        this.addRecipesToPacket(craftingDataPacket, CARTOGRAPHY_RECIPES);
        craftingDataPacket.getPotionMixData().addAll(Registries.POTION_MIXES.forVersion(session.getUpstream().getProtocolVersion()));

        for (GeyserRecipe<?> recipe : craftingRecipes.values()) {
            if (GameProtocol.is26_40orHigher(session.protocolVersion())) {
                if (recipe instanceof GeyserShapedRecipe shapedRecipe) {
                    craftingDataPacket.getShapedData().addAll(shapedRecipe.asRecipeData(session));
                } else if (recipe instanceof GeyserShapelessRecipe shapelessRecipe) {
                    craftingDataPacket.getShapelessData().addAll(shapelessRecipe.asRecipeData(session));
                }
            } else {
                craftingDataPacket.getCraftingData().addAll(recipe.asRecipeData(session));
            }
        }

        for (GeyserSmithingRecipe recipe : smithingRecipes) {
            if (GameProtocol.is26_40orHigher(session.protocolVersion())) {
                craftingDataPacket.getSmithingTransformData().addAll(recipe.asRecipeData(session));
            } else {
                craftingDataPacket.getCraftingData().addAll(recipe.asRecipeData(session));
            }
        }
    }

    public void addRecipesToPacket(CraftingDataPacket craftingDataPacket, List<? extends RecipeData> data) {
        // TODO Remove when 26.40 is the lowest supported version
        if (!GameProtocol.is26_40orHigher(session.protocolVersion())) {
            craftingDataPacket.getCraftingData().addAll(data);
            return;
        }
        for (RecipeData recipeData : data) {
            addRecipeToPacket(craftingDataPacket, recipeData);
        }
    }

    public void addRecipeToPacket(CraftingDataPacket craftingDataPacket, RecipeData recipeData) {
        // TODO Remove when 26.40 is the lowest supported version
        if (!GameProtocol.is26_40orHigher(session.protocolVersion())) {
            craftingDataPacket.getCraftingData().add(recipeData);
            return;
        }
        switch (recipeData) {
            case ShapedRecipeData shaped -> {
                craftingDataPacket.getShapedData().add(shaped);
            }
            case ShapelessRecipeData shapeless -> {
                craftingDataPacket.getShapelessData().add(shapeless);
            }
            case MultiRecipeData multi -> {
                craftingDataPacket.getMultiData().add(multi);
            }
            case SmithingTransformRecipeData smithingTransform -> {
                craftingDataPacket.getSmithingTransformData().add(smithingTransform);
            }
            case SmithingTrimRecipeData smithingTrim -> {
                craftingDataPacket.getSmithingTrimData().add(smithingTrim);
            }
            default -> {} // TODO figure out a good way to handle this
        }
    }
}
