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

package org.geysermc.connector.network.translators.inventory.translators;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.nukkitx.protocol.bedrock.data.inventory.*;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayerInventoryTranslator extends InventoryTranslator {
    private static final ItemData UNUSUABLE_CRAFTING_SPACE_BLOCK = InventoryUtils.createUnusableSpaceBlock(LanguageUtils.getLocaleStringLog("geyser.inventory.unusable_item.creative"));

    public PlayerInventoryTranslator() {
        super(46);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updateCraftingGrid(session, inventory);

        InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
        inventoryContentPacket.setContainerId(ContainerId.INVENTORY);
        ItemData[] contents = new ItemData[36];
        // Inventory
        for (int i = 9; i < 36; i++) {
            contents[i] = inventory.getItem(i).getItemData(session);
        }
        // Hotbar
        for (int i = 36; i < 45; i++) {
            contents[i - 36] = inventory.getItem(i).getItemData(session);
        }
        inventoryContentPacket.setContents(Arrays.asList(contents));
        session.sendUpstreamPacket(inventoryContentPacket);

        // Armor
        InventoryContentPacket armorContentPacket = new InventoryContentPacket();
        armorContentPacket.setContainerId(ContainerId.ARMOR);
        contents = new ItemData[4];
        for (int i = 5; i < 9; i++) {
            contents[i - 5] = inventory.getItem(i).getItemData(session);
        }
        armorContentPacket.setContents(Arrays.asList(contents));
        session.sendUpstreamPacket(armorContentPacket);

        // Offhand
        InventoryContentPacket offhandPacket = new InventoryContentPacket();
        offhandPacket.setContainerId(ContainerId.OFFHAND);
        offhandPacket.setContents(Collections.singletonList(inventory.getItem(45).getItemData(session)));
        session.sendUpstreamPacket(offhandPacket);
    }

    /**
     * Update the crafting grid for the player to hide/show the barriers in the creative inventory
     * @param session Session of the player
     * @param inventory Inventory of the player
     */
    public static void updateCraftingGrid(GeyserSession session, Inventory inventory) {
        // Crafting grid
        for (int i = 1; i < 5; i++) {
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(i + 27);

            if (session.getGameMode() == GameMode.CREATIVE) {
                slotPacket.setItem(UNUSUABLE_CRAFTING_SPACE_BLOCK);
            }else{
                slotPacket.setItem(ItemTranslator.translateToBedrock(session, inventory.getItem(i).getItemStack()));
            }

            session.sendUpstreamPacket(slotPacket);
        }
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        if (slot >= 1 && slot <= 44) {
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            if (slot >= 9) {
                slotPacket.setContainerId(ContainerId.INVENTORY);
                if (slot >= 36) {
                    slotPacket.setSlot(slot - 36);
                } else {
                    slotPacket.setSlot(slot);
                }
            } else if (slot >= 5) {
                slotPacket.setContainerId(ContainerId.ARMOR);
                slotPacket.setSlot(slot - 5);
            } else {
                slotPacket.setContainerId(ContainerId.UI);
                slotPacket.setSlot(slot + 27);
            }
            slotPacket.setItem(inventory.getItem(slot).getItemData(session));
            session.sendUpstreamPacket(slotPacket);
        } else if (slot == 45) {
            InventoryContentPacket offhandPacket = new InventoryContentPacket();
            offhandPacket.setContainerId(ContainerId.OFFHAND);
            offhandPacket.setContents(Collections.singletonList(inventory.getItem(slot).getItemData(session)));
            session.sendUpstreamPacket(offhandPacket);
        }
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        int slotnum = slotInfoData.getSlot();
        switch (slotInfoData.getContainer()) {
            case HOTBAR_AND_INVENTORY:
            case HOTBAR:
            case INVENTORY:
                // Inventory
                if (slotnum >= 9 && slotnum <= 35) {
                    return slotnum;
                }
                // Hotbar
                if (slotnum >= 0 && slotnum <= 8) {
                    return slotnum + 36;
                }
                break;
            case ARMOR:
                if (slotnum >= 0 && slotnum <= 3) {
                    return slotnum + 5;
                }
                break;
            case OFFHAND:
                return 45;
            case CRAFTING_INPUT:
                if (slotnum >= 28 && 31 >= slotnum) {
                    return slotnum - 27;
                }
                break;
            case CREATIVE_OUTPUT:
                return 0;
        }
        return slotnum;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return -1;
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot >= 36 && slot <= 44) {
            return new BedrockContainerSlot(ContainerSlotType.HOTBAR, slot - 36);
        } else if (slot >= 9 && slot <= 35) {
            return new BedrockContainerSlot(ContainerSlotType.INVENTORY, slot);
        } else if (slot >= 5 && slot <= 8) {
            return new BedrockContainerSlot(ContainerSlotType.ARMOR, slot - 5);
        } else if (slot == 45) {
            return new BedrockContainerSlot(ContainerSlotType.OFFHAND, 1);
        } else if (slot >= 1 && slot <= 4) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_INPUT, slot + 27);
        } else if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_OUTPUT, 0);
        } else {
            throw new IllegalArgumentException("Unknown bedrock slot");
        }
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 0)
            return SlotType.OUTPUT;
        return SlotType.NORMAL;
    }

    @Override
    public void translateRequests(GeyserSession session, Inventory inventory, List<ItemStackRequestPacket.Request> requests) {
        super.translateRequests(session, inventory, requests);
    }

    @Override
    public Inventory createInventory(String name, int windowId, WindowType windowType, PlayerInventory playerInventory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
    }
}
