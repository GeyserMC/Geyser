/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.updater;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.CrafterInventoryTranslator;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;

import java.util.Arrays;

/**
 * Read {@link CrafterInventoryTranslator} for context on the complete custom implementation here
 */
public class CrafterInventoryUpdater extends InventoryUpdater {

    public static final CrafterInventoryUpdater INSTANCE = new CrafterInventoryUpdater();

    @Override
    public void updateInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        ItemData[] bedrockItems;
        InventoryContentPacket contentPacket;

        // crafter grid - but excluding the result slot
        bedrockItems = new ItemData[CrafterInventoryTranslator.GRID_SIZE];
        for (int i = 0; i < bedrockItems.length; i++) {
            bedrockItems[translator.javaSlotToBedrock(i)] = inventory.getItem(i).getItemData(session);
        }
        contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(inventory.getBedrockId());
        contentPacket.setContents(Arrays.asList(bedrockItems));
        session.sendUpstreamPacket(contentPacket);

        // inventory and hotbar
        bedrockItems = new ItemData[36];
        for (int i = 0; i < 36; i++) {
            final int offset = i < 9 ? 27 : -9;
            bedrockItems[i] = inventory.getItem(CrafterInventoryTranslator.GRID_SIZE + i + offset).getItemData(session);
        }
        contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(ContainerId.INVENTORY);
        contentPacket.setContents(Arrays.asList(bedrockItems));
        session.sendUpstreamPacket(contentPacket);

        // Crafter result - it doesn't come after the grid, as explained elsewhere.
        updateSlot(translator, session, inventory, CrafterInventoryTranslator.JAVA_RESULT_SLOT);
    }

    @Override
    public boolean updateSlot(InventoryTranslator translator, GeyserSession session, Inventory inventory, int javaSlot) {
        int containerId;
        if (javaSlot < CrafterInventoryTranslator.GRID_SIZE || javaSlot == CrafterInventoryTranslator.JAVA_RESULT_SLOT) {
            // Parts of the Crafter UI
            // It doesn't seem like BDS sends the result slot, but sending it as slot 50 does actually work (it doesn't seem to show otherwise)
            containerId = inventory.getBedrockId();
        } else {
            containerId = ContainerId.INVENTORY;
        }

        InventorySlotPacket packet = new InventorySlotPacket();
        packet.setContainerId(containerId);
        packet.setSlot(translator.javaSlotToBedrock(javaSlot));
        packet.setItem(inventory.getItem(javaSlot).getItemData(session));
        session.sendUpstreamPacket(packet);
        return true;
    }
}
