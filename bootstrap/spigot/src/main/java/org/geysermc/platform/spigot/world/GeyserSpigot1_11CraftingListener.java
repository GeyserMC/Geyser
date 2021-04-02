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

package org.geysermc.platform.spigot.world;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.RecipeRegistry;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.data.MappingData;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;

import java.util.*;

/**
 * Used to send all available recipes from the server to the client, as a valid recipe book packet won't be sent by the server.
 * Requires ViaVersion.
 */
public class GeyserSpigot1_11CraftingListener implements Listener {

    private final GeyserConnector connector;
    /**
     * Specific mapping data for 1.12 to 1.13. Used to convert the 1.12 item into 1.13.
     */
    private final MappingData mappingData1_12to1_13;
    /**
     * The list of all protocols from the client's version to 1.13.
     */
    private final List<Pair<Integer, Protocol>> protocolList;

    public GeyserSpigot1_11CraftingListener(GeyserConnector connector) {
        this.connector = connector;
        this.mappingData1_12to1_13 = ProtocolRegistry.getProtocol(Protocol1_13To1_12_2.class).getMappingData();
        this.protocolList = ProtocolRegistry.getProtocolPath(MinecraftConstants.PROTOCOL_VERSION,
                ProtocolVersion.v1_13.getVersion());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GeyserSession session = null;
        for (GeyserSession otherSession : connector.getPlayers()) {
            if (otherSession.getName().equals(event.getPlayer().getName())) {
                session = otherSession;
                break;
            }
        }
        if (session == null) {
            return;
        }

        sendServerRecipes(session);
    }

    public void sendServerRecipes(GeyserSession session) {
        int netId = RecipeRegistry.LAST_RECIPE_NET_ID;

        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);

        Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();

            Pair<ItemStack, ItemData> outputs = translateToBedrock(session, recipe.getResult());
            ItemStack javaOutput = outputs.getKey();
            ItemData output = outputs.getValue();
            if (output.getId() == 0) continue; // If items make air we don't want that

            boolean isNotAllAir = false; // Check for all-air recipes
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                int size = shapedRecipe.getShape().length * shapedRecipe.getShape()[0].length();
                Ingredient[] ingredients = new Ingredient[size];
                ItemData[] input = new ItemData[size];
                for (int i = 0; i < input.length; i++) {
                    // Index is converting char to integer, adding i then converting back to char based on ASCII code
                    Pair<ItemStack, ItemData> result = translateToBedrock(session, shapedRecipe.getIngredientMap().get((char) ('a' + i)));
                    ingredients[i] = new Ingredient(new ItemStack[]{result.getKey()});
                    input[i] = result.getValue();
                    isNotAllAir |= input[i].getId() != 0;
                }

                if (!isNotAllAir) continue;
                UUID uuid = UUID.randomUUID();
                // Add recipe to our internal cache
                ShapedRecipeData data = new ShapedRecipeData(shapedRecipe.getShape()[0].length(), shapedRecipe.getShape().length,
                        "", ingredients, javaOutput);
                session.getCraftingRecipes().put(netId,
                        new com.github.steveice10.mc.protocol.data.game.recipe.Recipe(RecipeType.CRAFTING_SHAPED, uuid.toString(), data));

                // Add recipe for Bedrock
                craftingDataPacket.getCraftingData().add(CraftingData.fromShaped(uuid.toString(),
                        shapedRecipe.getShape()[0].length(), shapedRecipe.getShape().length, Arrays.asList(input),
                        Collections.singletonList(output), uuid, "crafting_table", 0, netId++));
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                Ingredient[] ingredients = new Ingredient[shapelessRecipe.getIngredientList().size()];
                ItemData[] input = new ItemData[shapelessRecipe.getIngredientList().size()];

                for (int i = 0; i < input.length; i++) {
                    Pair<ItemStack, ItemData> result = translateToBedrock(session, shapelessRecipe.getIngredientList().get(i));
                    ingredients[i] = new Ingredient(new ItemStack[]{result.getKey()});
                    input[i] = result.getValue();
                    isNotAllAir |= input[i].getId() != 0;
                }

                if (!isNotAllAir) continue;
                UUID uuid = UUID.randomUUID();
                // Add recipe to our internal cache
                ShapelessRecipeData data = new ShapelessRecipeData("", ingredients, javaOutput);
                session.getCraftingRecipes().put(netId,
                        new com.github.steveice10.mc.protocol.data.game.recipe.Recipe(RecipeType.CRAFTING_SHAPELESS, uuid.toString(), data));

                // Add recipe for Bedrock
                craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                        Arrays.asList(input), Collections.singletonList(output), uuid, "crafting_table", 0, netId++));
            }
        }

        session.sendUpstreamPacket(craftingDataPacket);
    }

    @SuppressWarnings("deprecation")
    private Pair<ItemStack, ItemData> translateToBedrock(GeyserSession session, org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack != null && itemStack.getData() != null) {
            if (itemStack.getType().getId() == 0) {
                return new Pair<>(null, ItemData.AIR);
            }

            int legacyId = (itemStack.getType().getId() << 4) | (itemStack.getData().getData() & 0xFFFF);

            if (itemStack.getType().getId() == 355 && itemStack.getData().getData() == (byte) 0) { // Handle bed color since the server will always be pre-1.12
                legacyId = (itemStack.getType().getId() << 4) | ((byte) 14 & 0xFFFF);
            }

            // old version -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16 and so on
            int itemId;
            if (mappingData1_12to1_13.getItemMappings().containsKey(legacyId)) {
                itemId = mappingData1_12to1_13.getNewItemId(legacyId);
            } else if (mappingData1_12to1_13.getItemMappings().containsKey((itemStack.getType().getId() << 4) | (0))) {
                itemId = mappingData1_12to1_13.getNewItemId((itemStack.getType().getId() << 4) | (0));
            } else {
                // No ID found, just send back air
                return new Pair<>(null, ItemData.AIR);
            }

            for (int i = protocolList.size() - 1; i >= 0; i--) {
                MappingData mappingData = protocolList.get(i).getValue().getMappingData();
                if (mappingData != null) {
                    itemId = mappingData.getNewItemId(itemId);
                }
            }

            ItemStack mcItemStack = new ItemStack(itemId, itemStack.getAmount());
            ItemData finalData = ItemTranslator.translateToBedrock(session, mcItemStack);
            return new Pair<>(mcItemStack, finalData);
        }

        // Empty slot, most likely
        return new Pair<>(null, ItemData.AIR);
    }

}
