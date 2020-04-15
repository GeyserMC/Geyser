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
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;

import java.util.List;

public class AnvilInventoryTranslator extends BlockInventoryTranslator {
    public AnvilInventoryTranslator() {
        super(3, "minecraft:anvil[facing=north]", ContainerType.ANVIL, new CursorInventoryUpdater());
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        if (action.getSource().getContainerId() == ContainerId.CURSOR) {
            switch (action.getSlot()) {
                case 1:
                    return 0;
                case 2:
                    return 1;
                case 50:
                    return 2;
            }
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2)
            return SlotType.OUTPUT;
        return SlotType.NORMAL;
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        InventoryActionData anvilResult = null;
        InventoryActionData anvilInput = null;
        for (InventoryActionData action : actions) {
            if (action.getSource().getContainerId() == ContainerId.ANVIL_MATERIAL) {
                //useless packet
                return;
            } else if (action.getSource().getContainerId() == ContainerId.ANVIL_RESULT) {
                anvilResult = action;
            } else if (bedrockSlotToJava(action) == 0) {
                anvilInput = action;
            }
        }
        ItemData itemName = null;
        if (anvilResult != null) {
            itemName = anvilResult.getFromItem();
        } else if (anvilInput != null) {
            itemName = anvilInput.getToItem();
        }
        if (itemName != null) {
            String rename;
            com.nukkitx.nbt.tag.CompoundTag tag = itemName.getTag();
            if (tag != null) {
                rename = tag.getCompound("display").getString("Name");
            } else {
                rename = "";
            }
            ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
            session.getDownstream().getSession().send(renameItemPacket);
        }
        if (anvilResult != null) {
            //client will send another packet to grab anvil output
            return;
        }

        super.translateActions(session, inventory, actions);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        if (slot >= 0 && slot <= 2) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                String rename;
                CompoundTag tag = item.getNbt();
                if (tag != null) {
                    CompoundTag displayTag = tag.get("display");
                    if (displayTag != null) {
                        String itemName = displayTag.get("Name").getValue().toString();
                        Message message = Message.fromString(itemName);
                        rename = message.getText();
                    } else {
                        rename = "";
                    }
                } else {
                    rename = "";
                }
                ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
                session.getDownstream().getSession().send(renameItemPacket);
            }
        }
        super.updateSlot(session, inventory, slot);
    }
}
