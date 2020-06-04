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

package org.geysermc.connector.network.translators.inventory.action;

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.nukkitx.protocol.bedrock.data.CraftingData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;

import java.util.Arrays;
import java.util.UUID;

// Bedrock requires the CraftingDataPacket in order to know what recipes are available on the server
// On versions where that is not sent, we send a new CraftingDataPacket every time a valid recipe is created so Bedrock knows it's a valid packet
public class LegacyCraftingTranslator {

    public static void addCraftingRecipe(GeyserSession session, ServerSetSlotPacket packet, Inventory inventory) {
        if (!session.isSentDeclareRecipesPacket() && packet.getSlot() == 0 && (packet.getWindowId() == 0 || inventory.getWindowType() == WindowType.CRAFTING) // if legacy crafting is enabled, the output slot was modified, and the window is either the player's or the crafting table
                && packet.getItem() != null && packet.getItem().getId() != 0) { // if the item isn't air nor null
            boolean fullCraftingTable = (inventory.getWindowType() == WindowType.CRAFTING);
            ItemData[] input = (fullCraftingTable ? new ItemData[9] : new ItemData[4]);
            boolean isNotAllAir = false;
            CraftingDataPacket dataPacket = new CraftingDataPacket();
            dataPacket.setCleanRecipes(false);
            for (int i = 1; i < input.length + 1; i++) {
                ItemData tempInput =  ItemTranslator.translateToBedrock(session, fullCraftingTable ? inventory.getItem(i) : session.getInventory().getItem(i));
                input[i - 1] = ItemData.of(tempInput.getId(), tempInput.getDamage(), 1);
                isNotAllAir = isNotAllAir || input[i - 1].getId() != 0;
            }
            if (isNotAllAir) { // So we don't make a recipe that is air only
                ItemData output = ItemTranslator.translateToBedrock(session, inventory.getItem(0));
                UUID uuid = UUID.randomUUID();
                int size = fullCraftingTable ? 3 : 2;
                // Add new crafting data that lets Bedrock see the recipe is valid
                CraftingData craftingData = CraftingData.fromShaped(uuid.toString(), size, size, input, new ItemData[]{output}, uuid, "crafting_table", 0);
                if (alreadySent(session, craftingData)) {
                    return;
                } else {
                    session.getKnownRecipes().add(craftingData);
                }
                dataPacket.getCraftingData().add(craftingData);
                session.sendUpstreamPacket(dataPacket);
            }
        }
    }

    private static boolean alreadySent(GeyserSession session, CraftingData craftingData) {
        for (CraftingData otherData : session.getKnownRecipes()) {
            if (!craftingData.getOutputs()[0].equals(otherData.getOutputs()[0])) continue;
            if (!Arrays.deepEquals(craftingData.getInputs(), otherData.getInputs())) continue;
            if (!(craftingData.getHeight() == otherData.getHeight())) continue;
            return true;
        }
        return false;
    }

}
