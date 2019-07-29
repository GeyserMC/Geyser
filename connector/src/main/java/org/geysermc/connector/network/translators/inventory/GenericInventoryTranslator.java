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

import com.flowpowered.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.utils.InventoryUtils;

public class GenericInventoryTranslator extends InventoryTranslator {

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        // TODO: Add code here
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setWindowId((byte) inventory.getId());
        containerOpenPacket.setType((byte) 0);
        containerOpenPacket.setBlockPosition(new Vector3i(0, 0, 0));
        session.getUpstream().sendPacket(containerOpenPacket);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        ContainerId containerId = InventoryUtils.getContainerId(inventory.getId());
        if (containerId == null)
            return;

        ItemData[] bedrockItems = new ItemData[inventory.getItems().length];
        for (int i = 0; i < bedrockItems.length; i++) {
            bedrockItems[i] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItems()[i]);
        }

        InventoryContentPacket contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(containerId);
        contentPacket.setContents(bedrockItems);
        session.getUpstream().sendPacket(contentPacket);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        ContainerId containerId = InventoryUtils.getContainerId(inventory.getId());
        if (containerId == null)
            return;

        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(containerId);
        slotPacket.setSlot(TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItems()[slot]));
        slotPacket.setInventorySlot(slot);
        session.getUpstream().sendPacket(slotPacket);
    }
}
