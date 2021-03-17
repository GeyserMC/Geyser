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

package org.geysermc.connector.network.translators.inventory.click;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Value;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.network.translators.inventory.translators.CraftingInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.PlayerInventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.*;

public class ClickPlan {
    private final List<ClickAction> plan = new ArrayList<>();
    private final Int2ObjectMap<GeyserItemStack> simulatedItems;
    private GeyserItemStack simulatedCursor;
    private boolean simulating;

    private final GeyserSession session;
    private final InventoryTranslator translator;
    private final Inventory inventory;
    private final int gridSize;

    public ClickPlan(GeyserSession session, InventoryTranslator translator, Inventory inventory) {
        this.session = session;
        this.translator = translator;
        this.inventory = inventory;

        this.simulatedItems = new Int2ObjectOpenHashMap<>(inventory.getSize());
        this.simulatedCursor = session.getPlayerInventory().getCursor().copy();
        this.simulating = true;

        if (translator instanceof PlayerInventoryTranslator) {
            gridSize = 4;
        } else if (translator instanceof CraftingInventoryTranslator) {
            gridSize = 9;
        } else {
            gridSize = -1;
        }
    }

    private void resetSimulation() {
        this.simulatedItems.clear();
        this.simulatedCursor = session.getPlayerInventory().getCursor().copy();
    }

    public void add(Click click, int slot) {
        add(click, slot, false);
    }

    public void add(Click click, int slot, boolean force) {
        if (!simulating)
            throw new UnsupportedOperationException("ClickPlan already executed");

        if (click == Click.LEFT_OUTSIDE || click == Click.RIGHT_OUTSIDE) {
            slot = Click.OUTSIDE_SLOT;
        }

        ClickAction action = new ClickAction(click, slot, force);
        plan.add(action);
        simulateAction(action);
    }

    public void execute(boolean refresh) {
        //update geyser inventory after simulation to avoid net id desync
        resetSimulation();
        ListIterator<ClickAction> planIter = plan.listIterator();
        while (planIter.hasNext()) {
            ClickAction action = planIter.next();

            if (action.slot != Click.OUTSIDE_SLOT && translator.getSlotType(action.slot) != SlotType.NORMAL) {
                refresh = true;
            }

            ItemStack clickedItemStack;
            if (!planIter.hasNext() && refresh) {
                clickedItemStack = InventoryUtils.REFRESH_ITEM;
            } else if (action.click.windowAction == WindowAction.DROP_ITEM || action.slot == Click.OUTSIDE_SLOT) {
                clickedItemStack = null;
            } else {
                clickedItemStack = getItem(action.slot).getItemStack();
            }

            short actionId = inventory.getNextTransactionId();
            ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(
                    inventory.getId(),
                    actionId,
                    action.slot,
                    clickedItemStack,
                    action.click.windowAction,
                    action.click.actionParam
            );

            simulateAction(action);

            session.sendDownstreamPacket(clickPacket);
            if (clickedItemStack == InventoryUtils.REFRESH_ITEM || action.force) {
                session.sendDownstreamPacket(new ClientConfirmTransactionPacket(inventory.getId(), actionId, true));
            }
        }

        session.getPlayerInventory().setCursor(simulatedCursor, session);
        for (Int2ObjectMap.Entry<GeyserItemStack> simulatedSlot : simulatedItems.int2ObjectEntrySet()) {
            inventory.setItem(simulatedSlot.getIntKey(), simulatedSlot.getValue(), session);
        }
        simulating = false;
    }

    public GeyserItemStack getItem(int slot) {
        return getItem(slot, true);
    }

    public GeyserItemStack getItem(int slot, boolean generate) {
        if (generate) {
            return simulatedItems.computeIfAbsent(slot, k -> inventory.getItem(slot).copy());
        } else {
            return simulatedItems.getOrDefault(slot, inventory.getItem(slot));
        }
    }

    public GeyserItemStack getCursor() {
        return simulatedCursor;
    }

    private void setItem(int slot, GeyserItemStack item) {
        if (simulating) {
            simulatedItems.put(slot, item);
        } else {
            inventory.setItem(slot, item, session);
        }
    }

    private void setCursor(GeyserItemStack item) {
        if (simulating) {
            simulatedCursor = item;
        } else {
            session.getPlayerInventory().setCursor(item, session);
        }
    }

    private void simulateAction(ClickAction action) {
        GeyserItemStack cursor = simulating ? getCursor() : session.getPlayerInventory().getCursor();
        switch (action.click) {
            case LEFT_OUTSIDE:
                setCursor(GeyserItemStack.EMPTY);
                return;
            case RIGHT_OUTSIDE:
                if (!cursor.isEmpty()) {
                    cursor.sub(1);
                }
                return;
        }

        GeyserItemStack clicked = simulating ? getItem(action.slot) : inventory.getItem(action.slot);
        if (translator.getSlotType(action.slot) == SlotType.OUTPUT) {
            switch (action.click) {
                case LEFT:
                case RIGHT:
                    if (cursor.isEmpty() && !clicked.isEmpty()) {
                        setCursor(clicked.copy());
                    } else if (InventoryUtils.canStack(cursor, clicked)) {
                        cursor.add(clicked.getAmount());
                    }
                    reduceCraftingGrid(false);
                    break;
                case LEFT_SHIFT:
                    reduceCraftingGrid(true);
                    break;
            }
        } else {
            switch (action.click) {
                case LEFT:
                    if (!InventoryUtils.canStack(cursor, clicked)) {
                        setCursor(clicked);
                        setItem(action.slot, cursor);
                    } else {
                        setCursor(GeyserItemStack.EMPTY);
                        clicked.add(cursor.getAmount());
                    }
                    break;
                case RIGHT:
                    if (cursor.isEmpty() && !clicked.isEmpty()) {
                        int half = clicked.getAmount() / 2; //smaller half
                        setCursor(clicked.copy(clicked.getAmount() - half)); //larger half
                        clicked.setAmount(half);
                    } else if (!cursor.isEmpty() && clicked.isEmpty()) {
                        cursor.sub(1);
                        setItem(action.slot, cursor.copy(1));
                    } else if (InventoryUtils.canStack(cursor, clicked)) {
                        cursor.sub(1);
                        clicked.add(1);
                    }
                    break;
                case LEFT_SHIFT:
                    //TODO
                    break;
                case DROP_ONE:
                    if (!clicked.isEmpty()) {
                        clicked.sub(1);
                    }
                    break;
                case DROP_ALL:
                    setItem(action.slot, GeyserItemStack.EMPTY);
                    break;
            }
        }
    }

    //TODO
    private void reduceCraftingGrid(boolean makeAll) {
        if (gridSize == -1)
            return;

        int crafted;
        if (!makeAll) {
            crafted = 1;
        } else {
            crafted = 0;
            for (int i = 0; i < gridSize; i++) {
                GeyserItemStack item = getItem(i + 1);
                if (!item.isEmpty()) {
                    if (crafted == 0) {
                        crafted = item.getAmount();
                    }
                    crafted = Math.min(crafted, item.getAmount());
                }
            }
        }

        for (int i = 0; i < gridSize; i++) {
            GeyserItemStack item = getItem(i + 1);
            if (!item.isEmpty())
                item.sub(crafted);
        }
    }

    /**
     * @return a new set of all affected slots. This isn't a constant variable; it's newly generated each time it is run.
     */
    public IntSet getAffectedSlots() {
        IntSet affectedSlots = new IntOpenHashSet();
        for (ClickAction action : plan) {
            if (translator.getSlotType(action.slot) == SlotType.NORMAL && action.slot != Click.OUTSIDE_SLOT) {
                affectedSlots.add(action.slot);
            }
        }
        return affectedSlots;
    }

    @Value
    private static class ClickAction {
        Click click;
        /**
         * Java slot
         */
        int slot;
        boolean force;
    }
}
