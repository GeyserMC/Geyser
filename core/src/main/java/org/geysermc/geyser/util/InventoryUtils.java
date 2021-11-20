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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundPickItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerHotbarPacket;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.LecternInventoryTranslator;
import org.geysermc.geyser.translator.inventory.chest.DoubleChestInventoryTranslator;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

public class InventoryUtils {
    /**
     * Stores the last used recipe network ID. Since 1.16.200 (and for server-authoritative inventories),
     * each recipe needs a unique network ID (or else in .200 the client crashes).
     */
    public static int LAST_RECIPE_NET_ID;
    
    public static final ItemStack REFRESH_ITEM = new ItemStack(1, 127, new CompoundTag(""));

    public static void openInventory(GeyserSession session, Inventory inventory) {
        session.setOpenInventory(inventory);
        if (session.isClosingInventory()) {
            //Wait for close confirmation from client before opening the new inventory.
            //Handled in BedrockContainerCloseTranslator
            inventory.setPending(true);
            return;
        }
        displayInventory(session, inventory);
    }

    public static void displayInventory(GeyserSession session, Inventory inventory) {
        InventoryTranslator translator = session.getInventoryTranslator();
        if (translator != null) {
            translator.prepareInventory(session, inventory);
            if (translator instanceof DoubleChestInventoryTranslator && !((Container) inventory).isUsingRealBlock()) {
                session.scheduleInEventLoop(() -> {
                    Inventory openInv = session.getOpenInventory();
                    if (openInv != null && openInv.getId() == inventory.getId()) {
                        translator.openInventory(session, inventory);
                        translator.updateInventory(session, inventory);
                    } else if (openInv != null && openInv.isPending()) {
                        // Presumably, this inventory is no longer relevant, and the client doesn't care about it
                        displayInventory(session, openInv);
                    }
                }, 200, TimeUnit.MILLISECONDS);
            } else {
                translator.openInventory(session, inventory);
                translator.updateInventory(session, inventory);
            }
        } else {
            // Precaution - as of 1.16 every inventory should be translated so this shouldn't happen
            session.setOpenInventory(null);
        }
    }

    public static void closeInventory(GeyserSession session, int windowId, boolean confirm) {
        session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY, session);
        updateCursor(session);

        Inventory inventory = getInventory(session, windowId);
        if (inventory != null) {
            InventoryTranslator translator = session.getInventoryTranslator();
            translator.closeInventory(session, inventory);
            if (confirm && !inventory.isPending() && !(translator instanceof LecternInventoryTranslator)) {
                session.setClosingInventory(true);
            }
        }
        session.setInventoryTranslator(InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR);
        session.setOpenInventory(null);
    }

    public static Inventory getInventory(GeyserSession session, int windowId) {
        if (windowId == 0) {
            return session.getPlayerInventory();
        } else {
            Inventory openInventory = session.getOpenInventory();
            if (openInventory != null && windowId == openInventory.getId()) {
                return openInventory;
            }
            return null;
        }
    }

    public static void updateCursor(GeyserSession session) {
        InventorySlotPacket cursorPacket = new InventorySlotPacket();
        cursorPacket.setContainerId(ContainerId.UI);
        cursorPacket.setSlot(0);
        cursorPacket.setItem(session.getPlayerInventory().getCursor().getItemData(session));
        session.sendUpstreamPacket(cursorPacket);
    }

    public static boolean canStack(GeyserItemStack item1, GeyserItemStack item2) {
        if (item1.isEmpty() || item2.isEmpty())
            return false;
        return item1.getJavaId() == item2.getJavaId() && Objects.equals(item1.getNbt(), item2.getNbt());
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
     *
     * @param description the description
     * @return the unusable space block
     */
    public static IntFunction<ItemData> createUnusableSpaceBlock(String description) {
        NbtMapBuilder root = NbtMap.builder();
        NbtMapBuilder display = NbtMap.builder();

        // Not ideal to use log here but we dont get a session
        display.putString("Name", ChatColor.RESET + GeyserLocale.getLocaleStringLog("geyser.inventory.unusable_item.name"));
        display.putList("Lore", NbtType.STRING, Collections.singletonList(ChatColor.RESET + ChatColor.DARK_PURPLE + description));

        root.put("display", display.build());
        return protocolVersion -> ItemData.builder()
                .id(Registries.ITEMS.forVersion(protocolVersion).getStoredItems().barrier().getBedrockId())
                .count(1)
                .tag(root.build()).build();
    }

    /**
     * See {@link #findOrCreateItem(GeyserSession, String)}. This is for finding a specified {@link ItemStack}.
     *
     * @param session the Bedrock client's session
     * @param itemStack the item to try to find a match for. NBT will also be accounted for.
     */
    public static void findOrCreateItem(GeyserSession session, ItemStack itemStack) {
        PlayerInventory inventory = session.getPlayerInventory();

        if (itemStack == null || itemStack.getId() == 0) {
            return;
        }

        // Check hotbar for item
        for (int i = 36; i < 45; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this is the item we're looking for
            if (geyserItem.getJavaId() == itemStack.getId() && Objects.equals(geyserItem.getNbt(), itemStack.getNbt())) {
                setHotbarItem(session, i);
                // Don't check inventory if item was in hotbar
                return;
            }
        }

        // Check inventory for item
        for (int i = 9; i < 36; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this is the item we're looking for
            if (geyserItem.getJavaId() == itemStack.getId() && Objects.equals(geyserItem.getNbt(), itemStack.getNbt())) {
                ServerboundPickItemPacket packetToSend = new ServerboundPickItemPacket(i); // https://wiki.vg/Protocol#Pick_Item
                session.sendDownstreamPacket(packetToSend);
                return;
            }
        }

        // If we still have not found the item, and we're in creative, ask for the item from the server.
        if (session.getGameMode() == GameMode.CREATIVE) {
            int slot = findEmptyHotbarSlot(inventory);

            ServerboundSetCreativeModeSlotPacket actionPacket = new ServerboundSetCreativeModeSlotPacket(slot,
                    itemStack);
            if ((slot - 36) != inventory.getHeldItemSlot()) {
                setHotbarItem(session, slot);
            }
            session.sendDownstreamPacket(actionPacket);
        }
    }

    /**
     * Attempt to find the specified item name in the session's inventory.
     * If it is found and in the hotbar, set the user's held item to that slot.
     * If it is found in another part of the inventory, move it.
     * If it is not found and the user is in creative mode, create the item,
     * overriding the current item slot if no other hotbar slots are empty, or otherwise selecting the empty slot.
     *
     * This attempts to mimic Java Edition behavior as best as it can.
     * @param session the Bedrock client's session
     * @param itemName the Java identifier of the item to search/select
     */
    public static void findOrCreateItem(GeyserSession session, String itemName) {
        // Get the inventory to choose a slot to pick
        PlayerInventory inventory = session.getPlayerInventory();

        if (itemName.equals("minecraft:air")) {
            return;
        }

        // Check hotbar for item
        for (int i = 36; i < 45; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this isn't the item we're looking for
            if (!geyserItem.getMapping(session).getJavaIdentifier().equals(itemName)) {
                continue;
            }

            setHotbarItem(session, i);
            // Don't check inventory if item was in hotbar
            return;
        }

        // Check inventory for item
        for (int i = 9; i < 36; i++) {
            GeyserItemStack geyserItem = inventory.getItem(i);
            if (geyserItem.isEmpty()) {
                continue;
            }
            // If this isn't the item we're looking for
            if (!geyserItem.getMapping(session).getJavaIdentifier().equals(itemName)) {
                continue;
            }

            ServerboundPickItemPacket packetToSend = new ServerboundPickItemPacket(i); // https://wiki.vg/Protocol#Pick_Item
            session.sendDownstreamPacket(packetToSend);
            return;
        }

        // If we still have not found the item, and we're in creative, ask for the item from the server.
        if (session.getGameMode() == GameMode.CREATIVE) {
            int slot = findEmptyHotbarSlot(inventory);

            ItemMapping mapping = session.getItemMappings().getMapping(itemName);
            if (mapping != null) {
                ServerboundSetCreativeModeSlotPacket actionPacket = new ServerboundSetCreativeModeSlotPacket(slot,
                        new ItemStack(mapping.getJavaId()));
                if ((slot - 36) != inventory.getHeldItemSlot()) {
                    setHotbarItem(session, slot);
                }
                session.sendDownstreamPacket(actionPacket);
            } else {
                session.getGeyser().getLogger().debug("Cannot find item for block " + itemName);
            }
        }
    }

    /**
     * @return the first empty slot found in this inventory, or else the player's currently held slot.
     */
    private static int findEmptyHotbarSlot(PlayerInventory inventory) {
        int slot = inventory.getHeldItemSlot() + 36;
        if (!inventory.getItemInHand().isEmpty()) { // Otherwise we should just use the current slot
            for (int i = 36; i < 45; i++) {
                if (inventory.getItem(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
        }
        return slot;
    }

    /**
     * Changes the held item slot to the specified slot
     * @param session GeyserSession
     * @param slot inventory slot to be selected
     */
    private static void setHotbarItem(GeyserSession session, int slot) {
        PlayerHotbarPacket hotbarPacket = new PlayerHotbarPacket();
        hotbarPacket.setContainerId(0);
        // Java inventory slot to hotbar slot ID
        hotbarPacket.setSelectedHotbarSlot(slot - 36);
        hotbarPacket.setSelectHotbarSlot(true);
        session.sendUpstreamPacket(hotbarPacket);
        // No need to send a Java packet as Bedrock sends a confirmation packet back that we translate
    }
}
