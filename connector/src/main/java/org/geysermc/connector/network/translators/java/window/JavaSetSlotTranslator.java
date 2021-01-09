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

package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.CraftingInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.PlayerInventoryTranslator;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Translator(packet = ServerSetSlotPacket.class)
public class JavaSetSlotTranslator extends PacketTranslator<ServerSetSlotPacket> {

    @Override
    public void translate(ServerSetSlotPacket packet, GeyserSession session) {
        System.out.println(packet.toString());
        session.addInventoryTask(() -> {
            if (packet.getWindowId() == 255) { //cursor
                GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
                session.getPlayerInventory().setCursor(newItem, session);
                InventoryUtils.updateCursor(session);
                return;
            }

            //TODO: support window id -2, should update player inventory
            Inventory inventory = InventoryUtils.getInventory(session, packet.getWindowId());
            if (inventory == null)
                return;

            InventoryTranslator translator = session.getInventoryTranslator();
            if (translator != null) {
                updateCraftingGrid(session, packet, inventory, translator);

                GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
                inventory.setItem(packet.getSlot(), newItem, session);
                translator.updateSlot(session, inventory, packet.getSlot());
            }
        });
    }

    private void updateCraftingGrid(GeyserSession session, ServerSetSlotPacket packet, Inventory inventory, InventoryTranslator translator) {
        if (packet.getSlot() == 0) {
            int gridSize;
            if (translator instanceof PlayerInventoryTranslator) {
                gridSize = 4;
            } else if (translator instanceof CraftingInventoryTranslator) {
                gridSize = 9;
            } else {
                return;
            }

            if (packet.getItem() == null || packet.getItem().getId() == 0) {
                return;
            }

            int offset = gridSize == 4 ? 28 : 32;
            int gridDimensions = gridSize == 4 ? 2 : 3;
            int itemsStart = 0;
            for (int i = 1; i < inventory.getSize(); i++) { // Slot 0 is, well, the output, so we ignore that
                if (!inventory.getItem(i).isEmpty()) {
                    System.out.println(inventory.getItem(i).getItemStack().toString());
                    itemsStart = i;
                    break;
                }
            }

            System.out.println("Items start: " + itemsStart);

            //TODO
            recipes:
            for (Recipe recipe : session.getCraftingRecipes().values()) {
                if (recipe.getType() == RecipeType.CRAFTING_SHAPED) {
                    ShapedRecipeData data = (ShapedRecipeData) recipe.getData();
                    if (!data.getResult().equals(packet.getItem())) {
                        continue;
                    }
                    int height = 1;
                    int width = 1;
                    for (int i = 0; i < data.getIngredients().length; i++) {
                        System.out.println(height);
                        System.out.println(width);
                        System.out.println(data.getHeight());
                        System.out.println(data.getWidth());
                        System.out.println(Arrays.toString(data.getIngredients()));
                        Ingredient ingredient = data.getIngredients()[i];
                        GeyserItemStack geyserItemStack = inventory.getItem(itemsStart + (width - 1) + ((data.getWidth() - 1) * (gridDimensions - data.getWidth() + height)));
                        System.out.println(itemsStart + (width - 1) + ((data.getWidth() - 1) * (gridDimensions - data.getWidth() + height)));
                        boolean inventoryHasItem = false;
                        for (ItemStack itemStack : ingredient.getOptions()) {
                            if (geyserItemStack.isEmpty()) {
                                inventoryHasItem = itemStack == null || itemStack.getId() == 0;
                                if (inventoryHasItem) {
                                    break;
                                }
                            } else if (itemStack.equals(geyserItemStack.getItemStack())) {
                                inventoryHasItem = true;
                                break;
                            }
                        }
                        if (!inventoryHasItem) {
                            break recipes;
                        }
                        width++;
                        if (width > data.getWidth()) {
                            width = 1;
                            height++;
                        }
                    }
                    // Recipe is had, don't sent packet
                    return;
                } else if (recipe.getType() == RecipeType.CRAFTING_SHAPELESS) {
                    ShapelessRecipeData data = (ShapelessRecipeData) recipe.getData();
                    if (!data.getResult().equals(packet.getItem())) {
                        continue;
                    }
                    for (int i = 0; i < data.getIngredients().length; i++) {
                        Ingredient ingredient = data.getIngredients()[i];
                        for (ItemStack itemStack : ingredient.getOptions()) {
                            boolean inventoryHasItem = false;
                            for (int j = 0; j < inventory.getSize(); j++) {
                                GeyserItemStack geyserItemStack = inventory.getItem(j);
                                if (geyserItemStack.isEmpty()) {
                                    inventoryHasItem = itemStack == null || itemStack.getId() == 0;
                                    if (inventoryHasItem) {
                                        break;
                                    }
                                } else if (itemStack.equals(geyserItemStack.getItemStack())) {
                                    inventoryHasItem = true;
                                    break;
                                }
                            }
                            if (!inventoryHasItem) {
                                continue recipes;
                            }
                        }
                    }
                    // Recipe is had, don't sent packet
                    return;
                }
            }
            System.out.println("Sending packet!");

            ItemData[] ingredients = new ItemData[gridSize];
            //construct ingredient list and clear slots on client
            for (int i = 0; i < gridSize; i++) {
                ingredients[i] = inventory.getItem(i + 1).getItemData(session);

                InventorySlotPacket slotPacket = new InventorySlotPacket();
                slotPacket.setContainerId(ContainerId.UI);
                slotPacket.setSlot(i + offset);
                slotPacket.setItem(ItemData.AIR);
                session.sendUpstreamPacket(slotPacket);
            }

            CraftingDataPacket craftPacket = new CraftingDataPacket();
            UUID uuid = UUID.randomUUID();
            craftPacket.getCraftingData().add(CraftingData.fromShaped(
                    uuid.toString(),
                    gridDimensions,
                    gridDimensions,
                    Arrays.asList(ingredients),
                    Collections.singletonList(ItemTranslator.translateToBedrock(session, packet.getItem())),
                    uuid,
                    "crafting_table",
                    0,
                    session.getLastRecipeNetId().incrementAndGet()
            ));
            craftPacket.setCleanRecipes(false);
            session.sendUpstreamPacket(craftPacket);

            //restore cleared slots
            for (int i = 0; i < gridSize; i++) {
                InventorySlotPacket slotPacket = new InventorySlotPacket();
                slotPacket.setContainerId(ContainerId.UI);
                slotPacket.setSlot(i + offset);
                slotPacket.setItem(ingredients[i]);
                session.sendUpstreamPacket(slotPacket);
            }
        }
    }
}
