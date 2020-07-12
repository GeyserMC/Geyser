/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.spigot.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import us.myles.ViaVersion.protocols.protocol1_13_1to1_13.packets.InventoryPackets;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData;

import java.util.Iterator;
import java.util.UUID;

public class GeyserSpigotLegacyCraftingTranslator {

    public static void sendAllRecipes(GeyserSession session) {
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            ItemData output = translateToBedrock(session, recipe.getResult());
            output = ItemData.of(output.getId(), output.getDamage(), output.getCount(), null);
            if (output.getId() == 0) continue; // If items make air we don't want that
            boolean isNotAllAir = false; // Check for all-air recipes
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                ItemData[] input = new ItemData[9];
                for (int i = 0; i < input.length; i++) {
                    // Index is converting char to integer, adding i then converting back to char based on ASCII code
                    ItemData itemData = translateToBedrock(session, shapedRecipe.getIngredientMap().get((char) ('a' + i)));
                    input[i] = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount());
                    isNotAllAir = isNotAllAir || input[i].getId() != 0;
                }
                if (!isNotAllAir) continue;
                UUID uuid = UUID.randomUUID();
                craftingDataPacket.getCraftingData().add(CraftingData.fromShaped(uuid.toString(),
                        shapedRecipe.getShape()[0].length(), shapedRecipe.getShape().length, input,
                        new ItemData[]{output}, uuid, "crafting_table", 0));
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                ItemData[] input = new ItemData[shapelessRecipe.getIngredientList().size()];
                for (int i = 0; i < input.length; i++) {
                    input[i] = translateToBedrock(session, shapelessRecipe.getIngredientList().get(i));
                    isNotAllAir = isNotAllAir || input[i].getId() != 0;
                }
                if (!isNotAllAir) continue;
                UUID uuid = UUID.randomUUID();
                craftingDataPacket.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(),
                        input, new ItemData[]{output}, uuid, "crafting_table", 0));
            }
        }
        session.sendUpstreamPacket(craftingDataPacket);
    }

    @SuppressWarnings("deprecation")
    private static ItemData translateToBedrock(GeyserSession session, org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack != null && itemStack.getData() != null) {
            if (itemStack.getType().getId() == 0) {
                return ItemData.AIR;
            }
            int legacyId = (itemStack.getType().getId() << 4) | (itemStack.getData().getData() & 0xFFFF);
            if (itemStack.getType().getId() == 355 && itemStack.getData().getData() == (byte) 0) { // Handle bed color since the server will always be pre-1.12
                legacyId = (itemStack.getType().getId() << 4) | ((byte) 14 & 0xFFFF);
            }
            // old version -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16
            int thirteenId;
            if (MappingData.oldToNewItems.containsKey(legacyId)) {
                thirteenId = MappingData.oldToNewItems.get(legacyId);
            } else if (MappingData.oldToNewItems.containsKey((itemStack.getType().getId() << 4) | (0))) {
                thirteenId = MappingData.oldToNewItems.get((itemStack.getType().getId() << 4) | (0));
            } else {
                // No ID found, just send back air
                return ItemData.AIR;
            }
            int thirteenPointOneId = InventoryPackets.getNewItemId(thirteenId);
            int fourteenId = us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData.oldToNewItems.get(thirteenPointOneId);
            int fifteenId = us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData.oldToNewItems.get(fourteenId);
            int sixteenId = us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData.oldToNewItems.get(fifteenId);
            ItemStack mcItemStack = new ItemStack(sixteenId, itemStack.getAmount());
            ItemData finalData = ItemTranslator.translateToBedrock(session, mcItemStack);
            return ItemData.of(finalData.getId(), finalData.getDamage(), finalData.getCount(), null);
        }
        // Empty slot, most likely
        return ItemData.AIR;
    }
}
