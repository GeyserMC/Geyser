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

package org.geysermc.connector.network.translators.inventory;

import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

import java.util.List;

public class MerchantInventoryTranslator extends BaseInventoryTranslator {

    private final InventoryUpdater updater;

    public MerchantInventoryTranslator() {
        super(3);
        this.updater = new CursorInventoryUpdater();
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 4;
            case 1:
                return 5;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        if (action.getSource().getContainerId() == ContainerId.CURSOR) {
            switch (action.getSlot()) {
                case 4:
                    return 0;
                case 5:
                    return 1;
                case 50:
                    return 2;
            }
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2) {
            return SlotType.OUTPUT;
        }
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
        session.setLastInteractedVillagerEid(-1);
        session.setVillagerTrades(null);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updater.updateInventory(this, session, inventory);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        updater.updateSlot(this, session, inventory, slot);
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        for (InventoryActionData action : actions) {
            if (action.getSource().getType() == InventorySource.Type.NON_IMPLEMENTED_TODO) {
                return;
            }
        }

        super.translateActions(session, inventory, actions);
    }
}
