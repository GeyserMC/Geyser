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

package org.geysermc.connector.network.translators.inventory.updater;

import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import lombok.AllArgsConstructor;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ChestInventoryUpdater extends InventoryUpdater {
    private static final ItemData UNUSUABLE_SPACE_BLOCK = InventoryUtils.createUnusableSpaceBlock(LanguageUtils.getLocaleStringLog("geyser.inventory.unusable_item.slot"));

    private final int paddedSize;

    @Override
    public void updateInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        super.updateInventory(translator, session, inventory);

        List<ItemData> bedrockItems = new ArrayList<>(paddedSize);
        for (int i = 0; i < paddedSize; i++) {
            if (i < translator.size) {
                bedrockItems.add(inventory.getItem(i).getItemData(session));
            } else {
                bedrockItems.add(UNUSUABLE_SPACE_BLOCK);
            }
        }

        InventoryContentPacket contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(inventory.getId());
        contentPacket.setContents(bedrockItems);
        session.sendUpstreamPacket(contentPacket);
    }

    @Override
    public boolean updateSlot(InventoryTranslator translator, GeyserSession session, Inventory inventory, int javaSlot) {
        if (super.updateSlot(translator, session, inventory, javaSlot))
            return true;

        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(inventory.getId());
        slotPacket.setSlot(translator.javaSlotToBedrock(javaSlot));
        slotPacket.setItem(inventory.getItem(javaSlot).getItemData(session));
        session.sendUpstreamPacket(slotPacket);
        return true;
    }
}
