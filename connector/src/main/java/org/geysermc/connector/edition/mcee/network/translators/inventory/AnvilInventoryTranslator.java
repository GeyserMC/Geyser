/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.edition.mcee.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.edition.mcee.network.translators.inventory.action.InventoryActionDataTranslator;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BlockInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;

import java.util.ArrayList;
import java.util.List;

public class AnvilInventoryTranslator extends BlockInventoryTranslator {

    private final InventoryActionDataTranslator actionTranslator;

    public AnvilInventoryTranslator(InventoryActionDataTranslator actionTranslator) {
        super(3, "minecraft:anvil[facing=north]", ContainerType.ANVIL, new ContainerInventoryUpdater());
        this.actionTranslator = actionTranslator;
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        int slotnum = action.getSlot();
        switch (action.getSource().getContainerId()) {
            case ContainerId.CONTAINER_INPUT:
            case ContainerId.ANVIL_MATERIAL:
                return slotnum;
            case ContainerId.ANVIL_RESULT:
                return 2;
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot < size) {
            return slot;
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
        List<InventoryActionData> fromActions = new ArrayList<>();
        List<InventoryActionData> toActions = new ArrayList<>();
        InventoryActionData anvilResult = null;

        for(InventoryActionData action : actions) {
            switch(action.getSource().getType()) {
                case UNTRACKED_INTERACTION_UI:
                case NON_IMPLEMENTED_TODO:
                case CONTAINER:
                case WORLD_INTERACTION:
                    switch(action.getSource().getContainerId()) {
                        case ContainerId.ANVIL_RESULT:
                            anvilResult = action;
                        // Container, Inventory, Crafting Input, Crafting Output
                        case ContainerId.CURSOR:
                        case ContainerId.INVENTORY:
                        case ContainerId.CRAFTING_ADD_INGREDIENT:
                        case ContainerId.CRAFTING_RESULT:
                        case ContainerId.CONTAINER_INPUT:
                        case ContainerId.NONE:
                            if (action.getFromItem().getCount() > action.getToItem().getCount()) {
                                fromActions.add(action);
                            } else {
                                toActions.add(action);
                            }
                            break;

                        // We are not interested in these
                        case ContainerId.CRAFTING_USE_INGREDIENT:
                            return;
                        default:
                            GeyserConnector.getInstance().getLogger().warning("Unknown ContainerID: " + action.getSource().getContainerId());
                    }
                    break;
            }
        }

        // Rename item
        if (anvilResult != null) {
            ItemData item = anvilResult.getFromItem();
            com.nukkitx.nbt.tag.CompoundTag tag = item.getTag();
            String rename = tag != null ? tag.getCompound("display").getString("Name") : "";
            ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
            session.sendDownstreamPacket(renameItemPacket);
        }

        if (!fromActions.isEmpty() && !toActions.isEmpty()) {
            actionTranslator.execute(this, session, inventory, fromActions, toActions);
        }
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
                session.sendDownstreamPacket(renameItemPacket);
            }
        }
        super.updateSlot(session, inventory, slot);
    }
}
