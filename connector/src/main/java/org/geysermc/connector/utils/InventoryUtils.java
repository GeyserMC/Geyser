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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class InventoryUtils {

    public static void refreshPlayerInventory(GeyserSession session, Inventory inventory) {
        InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
        inventoryContentPacket.setContainerId(ContainerId.INVENTORY);

        ItemData[] contents = new ItemData[40];
        // Inventory
        for (int i = 9; i < 36; i++) {
            contents[i] = Translators.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        // Hotbar
        for (int i = 36; i < 45; i++) {
            contents[i - 36] = Translators.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        // Armor
        for (int i = 5; i < 9; i++) {
            contents[i + 31] = Translators.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        inventoryContentPacket.setContents(contents);
        session.getUpstream().sendPacket(inventoryContentPacket);
    }

    public static void openInventory(GeyserSession session, ServerOpenWindowPacket packet) {
        Inventory inventory = new Inventory(packet.getWindowId(), packet.getType(), 45); // TODO: Find a way to set this value
        session.getInventoryCache().getInventories().put(packet.getWindowId(), inventory);
        session.getInventoryCache().setOpenInventory(inventory);

        InventoryTranslator translator = Translators.getInventoryTranslator();
        translator.prepareInventory(session, inventory);
        GeyserConnector.getInstance().getGeneralThreadPool().schedule(() -> {
            List<Packet> packets = session.getInventoryCache().getCachedPackets().get(inventory.getId());
            packets.forEach(itemPacket -> {
                if (itemPacket != null) {
                    if (ServerWindowItemsPacket.class.isAssignableFrom(itemPacket.getClass())) {
                        updateInventory(session, (ServerWindowItemsPacket) itemPacket);
                    }
                }
            });
        }, 200, TimeUnit.MILLISECONDS);
    }

    public static void updateInventory(GeyserSession session, ServerWindowItemsPacket packet) {
        if (packet.getWindowId() == 0)
            return;

        if (session.getInventoryCache().getOpenInventory() == null || !session.getInventoryCache().getInventories().containsKey(packet.getWindowId()))
            return;

        Inventory openInventory = session.getInventoryCache().getOpenInventory();
        if (packet.getWindowId() != openInventory.getId())
            return;

        InventoryTranslator translator = Translators.getInventoryTranslator();
        if (translator == null) {
            session.getDownstream().getSession().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }

        openInventory.setItems(packet.getItems());
        translator.updateInventory(session, openInventory);
    }

    public static void updateSlot(GeyserSession session, ServerSetSlotPacket packet) {
        if (packet.getWindowId() == 0)
            return;

        if (session.getInventoryCache().getOpenInventory() == null || !session.getInventoryCache().getInventories().containsKey(packet.getWindowId()))
            return;

        Inventory openInventory = session.getInventoryCache().getOpenInventory();
        if (packet.getWindowId() != openInventory.getId())
            return;

        InventoryTranslator translator = Translators.getInventoryTranslator();
        if (translator == null) {
            session.getDownstream().getSession().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }

        if (packet.getSlot() >= openInventory.getSize()) {
            session.getDownstream().getSession().send(new ClientCloseWindowPacket(packet.getWindowId()));
            return;
        }

        ItemStack[] items = openInventory.getItems();
        items[packet.getSlot()] = packet.getItem();
        translator.updateSlot(session, openInventory, packet.getSlot());
    }
}
