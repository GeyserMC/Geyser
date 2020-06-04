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
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.common.ChatColor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.DoubleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class InventoryUtils {
    public static final ItemStack REFRESH_ITEM = new ItemStack(1, 127, new CompoundTag("")); //TODO: stop using this

    public static void openInventory(GeyserSession session, Inventory inventory) {
        InventoryTranslator translator = InventoryTranslator.INVENTORY_TRANSLATORS.get(inventory.getWindowType());
        if (translator != null) {
            session.getInventoryCache().setOpenInventory(inventory);
            translator.prepareInventory(session, inventory);
            //Ensure at least half a second passes between closing and opening a new window
            //The client will not open the new window if it is still closing the old one
            long delay = 500 - (System.currentTimeMillis() - session.getLastWindowCloseTime());
            //TODO: find better way to handle double chest delay
            if (translator instanceof DoubleChestInventoryTranslator) {
                delay = Math.max(delay, 200);
            }
            if (delay > 0) {
                GeyserConnector.getInstance().getGeneralThreadPool().schedule(() -> {
                    translator.openInventory(session, inventory);
                    translator.updateInventory(session, inventory);
                }, delay, TimeUnit.MILLISECONDS);
            } else {
                translator.openInventory(session, inventory);
                translator.updateInventory(session, inventory);
            }
        }
    }

    public static void closeInventory(GeyserSession session, int windowId) {
        if (windowId != 0) {
            Inventory inventory = session.getInventoryCache().getInventories().get(windowId);
            Inventory openInventory = session.getInventoryCache().getOpenInventory();
            session.getInventoryCache().uncacheInventory(windowId);
            if (inventory != null && openInventory != null && inventory.getId() == openInventory.getId()) {
                InventoryTranslator translator = InventoryTranslator.INVENTORY_TRANSLATORS.get(inventory.getWindowType());
                translator.closeInventory(session, inventory);
                session.getInventoryCache().setOpenInventory(null);
            } else {
                return;
            }
        } else {
            Inventory inventory = session.getInventory();
            InventoryTranslator translator = InventoryTranslator.INVENTORY_TRANSLATORS.get(inventory.getWindowType());
            translator.updateInventory(session, inventory);
        }

        session.setCraftSlot(0);
        session.getInventory().setCursor(null);
        updateCursor(session);
    }

    public static void closeWindow(GeyserSession session, int windowId) {
        //Spamming close window packets can bug the client
        if (System.currentTimeMillis() - session.getLastWindowCloseTime() > 500) {
            ContainerClosePacket closePacket = new ContainerClosePacket();
            closePacket.setWindowId((byte) windowId);
            session.sendUpstreamPacket(closePacket);
            session.setLastWindowCloseTime(System.currentTimeMillis());
        }
    }

    public static void updateCursor(GeyserSession session) {
        InventorySlotPacket cursorPacket = new InventorySlotPacket();
        cursorPacket.setContainerId(ContainerId.CURSOR);
        cursorPacket.setSlot(0);
        cursorPacket.setItem(ItemTranslator.translateToBedrock(session, session.getInventory().getCursor()));
        session.sendUpstreamPacket(cursorPacket);
    }

    public static boolean canStack(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null)
            return false;
        return item1.getId() == item2.getId() && Objects.equals(item1.getNbt(), item2.getNbt());
    }

    public static boolean canStack(ItemData item1, ItemData item2) {
        if (item1 == null || item2 == null)
            return false;
        return item1.equals(item2, false, true, true);
    }

    /**
     * Returns a barrier block with custom name and lore to explain why
     * part of the inventory is unusable.
     */
    public static ItemData createUnusableSpaceBlock(String description) {
        CompoundTagBuilder root = CompoundTagBuilder.builder();
        CompoundTagBuilder display = CompoundTagBuilder.builder();

        display.stringTag("Name", ChatColor.RESET + "Unusable inventory space");
        display.listTag("Lore", StringTag.class, Collections.singletonList(new StringTag("", ChatColor.RESET + ChatColor.DARK_PURPLE + description)));

        root.tag(display.build("display"));
        return ItemData.of(ItemRegistry.ITEM_ENTRIES.get(ItemRegistry.BARRIER_INDEX).getBedrockId(), (short) 0, 1, root.buildRootTag());
    }
}
