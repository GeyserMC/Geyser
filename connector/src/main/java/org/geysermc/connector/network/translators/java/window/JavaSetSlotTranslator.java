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
                if (packet.getSlot() == 0) {
                    int gridSize = -1;
                    if (translator instanceof PlayerInventoryTranslator) {
                        gridSize = 4;
                    }
                    if (translator instanceof CraftingInventoryTranslator) {
                        gridSize = 9;
                    }
                    if (gridSize != -1) {
                        int offset = gridSize == 4 ? 28 : 32;
                        int gridWidth = gridSize == 4 ? 2 : 3;
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
                        UUID uuid = UUID.fromString("e0a4971a-698c-40fb-95dd-afc8ed16e108");
                        craftPacket.getCraftingData().add(CraftingData.fromShaped(
                                uuid.toString(),
                                gridWidth,
                                gridWidth,
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

                GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
                inventory.setItem(packet.getSlot(), newItem, session);
                translator.updateSlot(session, inventory, packet.getSlot());
            }
        });
    }
}
