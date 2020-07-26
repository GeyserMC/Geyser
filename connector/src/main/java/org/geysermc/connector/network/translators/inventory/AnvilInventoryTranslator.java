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
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;

import java.util.List;
import java.util.stream.Collectors;

public class AnvilInventoryTranslator extends BlockInventoryTranslator {
    public AnvilInventoryTranslator() {
        super(3, "minecraft:anvil[facing=north]", ContainerType.ANVIL, new CursorInventoryUpdater());
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        if (action.getSource().getContainerId() == ContainerId.UI) {
            switch (action.getSlot()) {
                case 1:
                    return 0;
                case 2:
                    return 1;
                case 50:
                    return 2;
            }
        }
        if (action.getSource().getContainerId() == ContainerId.ANVIL_RESULT) {
            return 2;
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
            NbtMap tag = itemName.getTag();
            if (tag != null) {
                String name = tag.getCompound("display").getString("Name");
                Component component = GsonComponentSerializer.gson().deserialize(name);
                rename = LegacyComponentSerializer.legacySection().serialize(component);
            } else {
                rename = "";
            }
            ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
            session.sendDownstreamPacket(renameItemPacket);
        }
        if (anvilResult != null) {
            //Strip unnecessary actions
            List<InventoryActionData> strippedActions = actions.stream()
                    .filter(action -> action.getSource().getContainerId() == ContainerId.ANVIL_RESULT
                            || (action.getSource().getType() == InventorySource.Type.CONTAINER
                            && !(action.getSource().getContainerId() == ContainerId.UI && action.getSlot() != 0)))
                    .collect(Collectors.toList());
            super.translateActions(session, inventory, strippedActions);
            return;
        }

        super.translateActions(session, inventory, actions);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        if (slot == 0) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                String rename;
                CompoundTag tag = item.getNbt();
                if (tag != null) {
                    CompoundTag displayTag = tag.get("display");
                    if (displayTag != null && displayTag.contains("Name")) {
                        String itemName = displayTag.get("Name").getValue().toString();
                        Component component = GsonComponentSerializer.gson().deserialize(itemName);
                        rename = LegacyComponentSerializer.legacySection().serialize(component);
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
