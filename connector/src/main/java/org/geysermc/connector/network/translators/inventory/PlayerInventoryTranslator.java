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

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.action.InventoryActionDataTranslator;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.List;

public class PlayerInventoryTranslator extends InventoryTranslator {
    private static final ItemData UNUSUABLE_CRAFTING_SPACE_BLOCK = InventoryUtils.createUnusableSpaceBlock(
            "The creative crafting grid is\nunavailable in Java Edition");

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
            contents[i] = ItemTranslator.translateToBedrock(session, inventory.getItem(i));
        }
        // Hotbar
        for (int i = 36; i < 45; i++) {
            contents[i - 36] = ItemTranslator.translateToBedrock(session, inventory.getItem(i));
        }
        inventoryContentPacket.setContents(contents);
        session.sendUpstreamPacket(inventoryContentPacket);

        // Armor
        InventoryContentPacket armorContentPacket = new InventoryContentPacket();
        armorContentPacket.setContainerId(ContainerId.ARMOR);
        contents = new ItemData[4];
        for (int i = 5; i < 9; i++) {
            contents[i - 5] = ItemTranslator.translateToBedrock(session, inventory.getItem(i));
        }
        armorContentPacket.setContents(contents);
        session.sendUpstreamPacket(armorContentPacket);

        // Offhand
        InventoryContentPacket offhandPacket = new InventoryContentPacket();
        offhandPacket.setContainerId(ContainerId.OFFHAND);
        offhandPacket.setContents(new ItemData[]{ItemTranslator.translateToBedrock(session, inventory.getItem(45))});
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
            slotPacket.setContainerId(ContainerId.CURSOR);
            slotPacket.setSlot(i + 27);

            if (session.getGameMode() == GameMode.CREATIVE) {
                slotPacket.setItem(UNUSUABLE_CRAFTING_SPACE_BLOCK);
            }else{
                slotPacket.setItem(ItemTranslator.translateToBedrock(session, inventory.getItem(i)));
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
                slotPacket.setContainerId(ContainerId.CURSOR);
                slotPacket.setSlot(slot + 27);
            }
            slotPacket.setItem(ItemTranslator.translateToBedrock(session, inventory.getItem(slot)));
            session.sendUpstreamPacket(slotPacket);
        } else if (slot == 45) {
            InventoryContentPacket offhandPacket = new InventoryContentPacket();
            offhandPacket.setContainerId(ContainerId.OFFHAND);
            offhandPacket.setContents(new ItemData[]{ItemTranslator.translateToBedrock(session, inventory.getItem(slot))});
            session.sendUpstreamPacket(offhandPacket);
        }
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
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
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        if (session.getGameMode() == GameMode.CREATIVE) {
            //crafting grid is not visible in creative mode in java edition
            for (InventoryActionData action : actions) {
                if (action.getSource().getContainerId() == ContainerId.CURSOR && (action.getSlot() >= 28 && 31 >= action.getSlot())) {
                    updateInventory(session, inventory);
                    InventoryUtils.updateCursor(session);
                    return;
                }
            }

            ItemStack javaItem;
            for (InventoryActionData action : actions) {
                switch (action.getSource().getContainerId()) {
                    case ContainerId.INVENTORY:
                    case ContainerId.ARMOR:
                    case ContainerId.OFFHAND:
                        int javaSlot = bedrockSlotToJava(action);
                        if (action.getToItem().getId() == 0) {
                            javaItem = new ItemStack(-1, 0, null);
                        } else {
                            javaItem = ItemTranslator.translateToJava(action.getToItem());
                        }
                        ClientCreativeInventoryActionPacket creativePacket = new ClientCreativeInventoryActionPacket(javaSlot, javaItem);
                        session.sendDownstreamPacket(creativePacket);
                        inventory.setItem(javaSlot, javaItem);
                        break;
                    case ContainerId.CURSOR:
                        if (action.getSlot() == 0) {
                            session.getInventory().setCursor(ItemTranslator.translateToJava(action.getToItem()));
                        }
                        break;
                    case ContainerId.NONE:
                        if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION
                                && action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                            javaItem = ItemTranslator.translateToJava(action.getToItem());
                            ClientCreativeInventoryActionPacket creativeDropPacket = new ClientCreativeInventoryActionPacket(-1, javaItem);
                            session.sendDownstreamPacket(creativeDropPacket);
                        }
                        break;
                }
            }
            return;
        }

        InventoryActionDataTranslator.translate(this, session, inventory, actions);
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
