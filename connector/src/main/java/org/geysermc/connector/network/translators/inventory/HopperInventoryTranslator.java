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

import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.action.InventoryActionDataTranslator;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

import java.util.List;

public class HopperInventoryTranslator extends BlockInventoryTranslator {

    public HopperInventoryTranslator(InventoryUpdater updater) {
        super(5, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER, updater);
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        if (inventory.getId() == 127) {
            // The armor gui
            GeyserConnector.getInstance().getLogger().debug("Armor gui event");
            int i = 0;
            for (InventoryActionData action : actions) {
                if (action.getSource().getContainerId() == 127) {
                    // Just as a fallback we set it to the current slot
                    int newSlot;

                    switch (action.getSlot()) {
                        case 0:
                            newSlot = 5;
                            break;
                        case 1:
                            newSlot = 6;
                            break;
                        case 2:
                            newSlot = 7;
                            break;
                        case 3:
                            newSlot = 8;
                            break;
                        case 4:
                            newSlot = 45;
                            break;
                        default:
                            newSlot = action.getSlot() + 4;
                            break;
                    }

                    GeyserConnector.getInstance().getLogger().debug("New slot: " + newSlot);

                    // This doesnt work but might be close
                    InventorySource newSource = InventorySource.fromContainerWindowId(session.getInventoryCache().getPlayerInventory().getId());
                    InventoryActionData newData = new InventoryActionData(newSource, newSlot, action.getFromItem(), action.getToItem());
                    actions.set(i, newData);
                }
                i++;
            }
        }

        InventoryActionDataTranslator.translate(this, session, session.getInventoryCache().getPlayerInventory(), actions);
    }
}
