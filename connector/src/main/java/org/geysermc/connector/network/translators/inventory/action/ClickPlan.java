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

package org.geysermc.connector.network.translators.inventory.action;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

class ClickPlan {
    private final List<ClickAction> plan = new ArrayList<>();

    public void add(Click click, int slot) {
        plan.add(new ClickAction(click, slot));
    }

    public void execute(GeyserSession session, InventoryTranslator translator, Inventory inventory, boolean refresh) {
        PlayerInventory playerInventory = session.getInventory();
        ListIterator<ClickAction> planIter = plan.listIterator();
        while (planIter.hasNext()) {
            final ClickAction action = planIter.next();
            final ItemStack cursorItem = playerInventory.getCursor();
            final ItemStack clickedItem = inventory.getItem(action.slot);
            final short actionId = (short) inventory.getTransactionId().getAndIncrement();

            //TODO: stop relying on refreshing the inventory for crafting to work properly
            if (translator.getSlotType(action.slot) != SlotType.NORMAL)
                refresh = true;

            ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(inventory.getId(),
                    actionId, action.slot, !planIter.hasNext() && refresh ? InventoryUtils.REFRESH_ITEM : clickedItem,
                    WindowAction.CLICK_ITEM, action.click.actionParam);

            if (translator.getSlotType(action.slot) == SlotType.OUTPUT) {
                if (cursorItem == null && clickedItem != null) {
                    playerInventory.setCursor(clickedItem);
                } else if (InventoryUtils.canStack(cursorItem, clickedItem)) {
                    playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                            cursorItem.getAmount() + clickedItem.getAmount(), cursorItem.getNbt()));
                }
            } else {
                switch (action.click) {
                    case LEFT:
                        if (!InventoryUtils.canStack(cursorItem, clickedItem)) {
                            playerInventory.setCursor(clickedItem);
                            inventory.setItem(action.slot, cursorItem);
                        } else {
                            playerInventory.setCursor(null);
                            inventory.setItem(action.slot, new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() + cursorItem.getAmount(), clickedItem.getNbt()));
                        }
                        break;
                    case RIGHT:
                        if (cursorItem == null && clickedItem != null) {
                            ItemStack halfItem = new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() / 2, clickedItem.getNbt());
                            inventory.setItem(action.slot, halfItem);
                            playerInventory.setCursor(new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() - halfItem.getAmount(), clickedItem.getNbt()));
                        } else if (cursorItem != null && clickedItem == null) {
                            playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                                    cursorItem.getAmount() - 1, cursorItem.getNbt()));
                            inventory.setItem(action.slot, new ItemStack(cursorItem.getId(),
                                    1, cursorItem.getNbt()));
                        } else if (InventoryUtils.canStack(cursorItem, clickedItem)) {
                            playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                                    cursorItem.getAmount() - 1, cursorItem.getNbt()));
                            inventory.setItem(action.slot, new ItemStack(clickedItem.getId(),
                                    clickedItem.getAmount() + 1, clickedItem.getNbt()));
                        }
                        break;
                }
            }
            session.getDownstream().getSession().send(clickPacket);
            session.getDownstream().getSession().send(new ClientConfirmTransactionPacket(inventory.getId(), actionId, true));
        }

        /*if (refresh) {
            translator.updateInventory(session, inventory);
            InventoryUtils.updateCursor(session);
        }*/
    }

    private static class ClickAction {
        final Click click;
        final int slot;
        ClickAction(Click click, int slot) {
            this.click = click;
            this.slot = slot;
        }
    }
}
