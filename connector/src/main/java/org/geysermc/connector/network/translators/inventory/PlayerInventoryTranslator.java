/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.inventory;

import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;

public class PlayerInventoryTranslator extends InventoryTranslator {
    public PlayerInventoryTranslator() {
        super(46);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
        inventoryContentPacket.setContainerId(ContainerId.INVENTORY);

        ItemData[] contents = new ItemData[36];
        // Inventory
        for (int i = 9; i < 36; i++) {
            contents[i] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(i));
        }

        // Hotbar
        for (int i = 36; i < 45; i++) {
            contents[i - 36] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(i));
        }

        inventoryContentPacket.setContents(contents);
        session.getUpstream().sendPacket(inventoryContentPacket);

        // Armor
        InventoryContentPacket armorContentPacket = new InventoryContentPacket();
        armorContentPacket.setContainerId(ContainerId.ARMOR);
        contents = new ItemData[4];
        for (int i = 5; i < 9; i++) {
            contents[i - 5] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(i));
        }
        armorContentPacket.setContents(contents);
        session.getUpstream().sendPacket(armorContentPacket);

        // Offhand
        InventoryContentPacket offhandPacket = new InventoryContentPacket();
        offhandPacket.setContainerId(ContainerId.OFFHAND);
        offhandPacket.setContents(new ItemData[]{TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(45))});
        session.getUpstream().sendPacket(offhandPacket);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        if (slot >= 5 && slot <= 44) {
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            if (slot >= 9) {
                slotPacket.setContainerId(ContainerId.INVENTORY);
                if (slot >= 36) {
                    slotPacket.setInventorySlot(slot - 36);
                } else {
                    slotPacket.setInventorySlot(slot);
                }
            } else {
                slotPacket.setContainerId(ContainerId.ARMOR);
                slotPacket.setInventorySlot(slot - 5);
            }
            slotPacket.setSlot(TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(slot)));
            session.getUpstream().sendPacket(slotPacket);
        } else if (slot == 45) {
            InventoryContentPacket offhandPacket = new InventoryContentPacket();
            offhandPacket.setContainerId(ContainerId.OFFHAND);
            offhandPacket.setContents(new ItemData[]{TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(slot))});
            session.getUpstream().sendPacket(offhandPacket);
        }
    }

    @Override
    public int bedrockSlotToJava(InventoryAction action) {
        int slotnum = action.getSlot();
        switch (action.getSource().getContainerId()) {
            case ContainerId.INVENTORY:
                // Inventory
                if (slotnum >= 9 && slotnum <= 35) {
                    return slotnum;
                }
                // Hotbar
                if (slotnum >= 0 && slotnum <= 8) {
                    return slotnum + 36;
                }
                break;
            case ContainerId.ARMOR:
                if (slotnum >= 0 && slotnum <= 3) {
                    return slotnum + 5;
                }
                break;
            case ContainerId.OFFHAND:
                return 45;
            case ContainerId.CURSOR:
                if (slotnum >= 28 && 31 >= slotnum) {
                    return slotnum - 27;
                } else if (slotnum == 50) {
                    return 0;
                }
                break;
        }
        return slotnum;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return slot;
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 0)
            return SlotType.OUTPUT;
        return SlotType.NORMAL;
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
