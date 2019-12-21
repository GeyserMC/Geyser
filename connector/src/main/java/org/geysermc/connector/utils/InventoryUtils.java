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
import org.geysermc.connector.network.translators.TranslatorsInit;
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
            contents[i] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        // Hotbar
        for (int i = 36; i < 45; i++) {
            contents[i - 36] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        // Armor
        for (int i = 5; i < 9; i++) {
            contents[i + 31] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        inventoryContentPacket.setContents(contents);
        session.getUpstream().sendPacket(inventoryContentPacket);
    }

    public static void openInventory(GeyserSession session, ServerOpenWindowPacket packet) {
        Inventory inventory = new Inventory(packet.getWindowId(), packet.getType(), 45); // TODO: Find a way to set this value
        session.getInventoryCache().getInventories().put(packet.getWindowId(), inventory);
        session.getInventoryCache().setOpenInventory(inventory);

        InventoryTranslator translator = TranslatorsInit.getInventoryTranslator();
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

        InventoryTranslator translator = TranslatorsInit.getInventoryTranslator();
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

        InventoryTranslator translator = TranslatorsInit.getInventoryTranslator();
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
