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

package org.geysermc.connector.edition.mcee.network.translators.inventory.action;

import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.action.Click;
import org.geysermc.connector.network.translators.inventory.action.ClickPlan;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides an MCEE implementation of InventoryActionDataTranslator
 */
public class InventoryActionDataTranslator {
    public InventoryActionDataTranslator() {

    }

    @Data
    @AllArgsConstructor
    @ToString
    static class InventoryData {
        private InventoryActionData action;
        private int fromCount;
        private int toCount;
    }

    /**
     * Resolve the from and to actions
     * @return boolean true if execution occurred
     */
    public void execute(InventoryTranslator translator, GeyserSession session, Inventory inventory, List<InventoryActionData> fromActions, List<InventoryActionData> toActions) {
        // Generate InventoryData
        List<InventoryData> fromData = fromActions.stream()
                .map(a -> new InventoryData(a, a.getFromItem().getCount(), a.getToItem().getCount()))
                .collect(Collectors.toList());
        List<InventoryData> toData = toActions.stream()
                .map(a -> new InventoryData(a, a.getFromItem().getCount(), a.getToItem().getCount()))
                .collect(Collectors.toList());

        ClickPlan plan = new ClickPlan();

        for (InventoryData from : fromData) {
            int fromSlot = translator.bedrockSlotToJava(from.action);

            // Pick up items
            plan.add(Click.LEFT, fromSlot);

            // Drop back difference if its the same
            if (from.action.getToItem().getId() == from.action.getFromItem().getId()) {
                while (from.fromCount > 0 && from.toCount > 0) {
                    plan.add(Click.RIGHT, fromSlot);
                    from.fromCount--;
                    from.toCount--;
                }
            }

            for (InventoryData to : toData) {
                int toSlot = translator.bedrockSlotToJava(to.action);

                // Items left to drop?
                if (from.fromCount > 0) {
                    // Can they be dropped here?
                    if (InventoryUtils.canStack(from.action.getFromItem(), to.action.getToItem())) {
                        // Can we drop everything?
                        if (from.fromCount <= to.toCount) {
                            to.toCount-=from.fromCount;
                            from.fromCount=0;

                            // Check if we are dropping to the world
                            if (to.action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                                plan.add(Click.DROP_STACK, fromSlot);
                            } else {
                                plan.add(Click.LEFT, toSlot);
                            }
                            continue;
                        }

                        // Drop individually
                        while (from.fromCount > 0 && to.toCount > 0) {
                            from.fromCount--;
                            to.toCount--;

                            // Check if we are dropping to the world
                            if (to.action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                                plan.add(Click.DROP_ITEM, fromSlot);
                            } else {
                                plan.add(Click.RIGHT, toSlot);
                            }
                        }
                    }
                }
            }

            if (from.fromCount > 0 || from.toCount > 0) {
                GeyserConnector.getInstance().getLogger().warning("From was not resolved: " + from);
            }
        }
        plan.execute(session, translator, inventory, false);
    }
}
