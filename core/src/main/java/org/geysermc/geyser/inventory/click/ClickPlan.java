/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.click;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.BundleInventoryTranslator;
import org.geysermc.geyser.translator.inventory.CraftingInventoryTranslator;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.thirdparty.Fraction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSelectBundleItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class ClickPlan {
    private final List<ClickAction> plan = new ArrayList<>();
    private final Int2ObjectMap<GeyserItemStack> simulatedItems;
    
    private Int2ObjectMap<ItemStack> changedItems;
    private Int2ObjectMap<HashedStack> changedHashedItems;
    private GeyserItemStack simulatedCursor;
    private int desiredBundleSlot;
    private boolean executionBegan;

    private final GeyserSession session;
    private final InventoryTranslator<?> translator;
    private final Inventory inventory;
    private final int gridSize;

    public ClickPlan(GeyserSession session, InventoryTranslator<?> translator, Inventory inventory) {
        this.session = session;
        this.translator = translator;
        this.inventory = inventory;

        this.simulatedItems = new Int2ObjectOpenHashMap<>(inventory.getSize());
        this.changedItems = null;
        this.changedHashedItems = null;
        this.simulatedCursor = session.getPlayerInventory().getCursor().copy();
        this.executionBegan = false;

        gridSize = translator.getGridSize();
    }

    private void resetSimulation() {
        this.simulatedItems.clear();
        this.simulatedCursor = session.getPlayerInventory().getCursor().copy();
    }

    public void add(Click click, int slot) {
        add(click, slot, false);
    }

    public void add(Click click, int slot, boolean force) {
        if (executionBegan)
            throw new UnsupportedOperationException("ClickPlan already executed");

        if (click == Click.LEFT_OUTSIDE || click == Click.RIGHT_OUTSIDE) {
            slot = Click.OUTSIDE_SLOT;
        }

        ClickAction action = new ClickAction(click, slot, force);
        plan.add(action);
        
        
        simulateAction(action);
    }

    public void execute(boolean refresh) {
        executionBegan = true;
        //update geyser inventory after simulation to avoid net id desync
        resetSimulation();
        ListIterator<ClickAction> planIter = plan.listIterator();
        while (planIter.hasNext()) {
            ClickAction action = planIter.next();

            if (action.slot != Click.OUTSIDE_SLOT && translator.getSlotType(action.slot) != SlotType.NORMAL) {
                
                refresh = true;
            }

            changedItems = new Int2ObjectOpenHashMap<>();
            changedHashedItems = new Int2ObjectOpenHashMap<>();

            boolean emulatePost1_16Logic = session.isEmulatePost1_16Logic();

            int stateId;
            if (emulatePost1_16Logic) {
                stateId = stateIdHack(action);
                simulateAction(action);
            } else {
                stateId = inventory.getStateId();
            }

            ItemStack clickedItemStack;
            if (emulatePost1_16Logic) {
                
                clickedItemStack = simulatedCursor.getItemStack();
            } else {
                if (!planIter.hasNext() && refresh) {
                    
                    
                    clickedItemStack = InventoryUtils.REFRESH_ITEM;
                } else {
                    if (action.click.actionType == ContainerActionType.DROP_ITEM || action.slot == Click.OUTSIDE_SLOT) {
                        clickedItemStack = null;
                    } else {
                        clickedItemStack = getItem(action.slot).getItemStack();
                    }
                }
            }

            if (!emulatePost1_16Logic) {
                simulateAction(action);
            }

            ServerboundContainerClickPacket clickPacket = new ServerboundContainerClickPacket(
                inventory.getJavaId(),
                stateId,
                action.slot,
                action.click.actionType,
                action.click.action,
                DataComponentHashers.hashStack(session, clickedItemStack),
                changedHashedItems
            );

            session.sendDownstreamGamePacket(clickPacket);
        }

        session.getPlayerInventory().setCursor(simulatedCursor, session);
        for (Int2ObjectMap.Entry<GeyserItemStack> simulatedSlot : simulatedItems.int2ObjectEntrySet()) {
            inventory.setItem(simulatedSlot.getIntKey(), simulatedSlot.getValue(), session);
        }
    }

    public void executeForCreativeMode() {
        executionBegan = true;
        //update geyser inventory after simulation to avoid net id desync
        resetSimulation();
        changedItems = new Int2ObjectOpenHashMap<>();
        changedHashedItems = new Int2ObjectOpenHashMap<>();
        for (ClickAction action : plan) {
            simulateAction(action);
        }
        session.getPlayerInventory().setCursor(simulatedCursor, session);
        for (Int2ObjectMap.Entry<GeyserItemStack> simulatedSlot : simulatedItems.int2ObjectEntrySet()) {
            inventory.setItem(simulatedSlot.getIntKey(), simulatedSlot.getValue(), session);
        }
        for (Int2ObjectMap.Entry<ItemStack> changedSlot : changedItems.int2ObjectEntrySet()) {
            ItemStack value = changedSlot.getValue();
            ItemStack toSend = InventoryUtils.isEmpty(value) ? new ItemStack(-1, 0, null) : value;
            session.sendDownstreamGamePacket(
                new ServerboundSetCreativeModeSlotPacket((short) changedSlot.getIntKey(), toSend)
            );
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    
    public boolean canStack(int slot, GeyserItemStack item) {
        GeyserItemStack slotItem = simulatedItems.getOrDefault(slot, inventory.getItem(slot));
        return InventoryUtils.canStack(slotItem, item);
    }

    
    public boolean isEmpty(int slot) {
        return simulatedItems.getOrDefault(slot, inventory.getItem(slot)).isEmpty();
    }

    public GeyserItemStack getItem(int slot) {
        return simulatedItems.computeIfAbsent(slot, k -> inventory.getItem(slot).copy());
    }

    public void setDesiredBundleSlot(int desiredBundleSlot) {
        this.desiredBundleSlot = desiredBundleSlot;
    }

    public GeyserItemStack getCursor() {
        return simulatedCursor;
    }

    private void setItem(int slot, GeyserItemStack item) {
        simulatedItems.put(slot, item);
        onSlotItemChange(slot, item);
    }

    private void setCursor(GeyserItemStack item) {
        simulatedCursor = item;
    }

    private void add(int slot, GeyserItemStack itemStack, int amount) {
        itemStack.add(amount);
        onSlotItemChange(slot, itemStack);
    }

    private void sub(int slot, GeyserItemStack itemStack, int amount) {
        itemStack.sub(amount);
        onSlotItemChange(slot, itemStack);
    }

    private void setAmount(int slot, GeyserItemStack itemStack, int amount) {
        itemStack.setAmount(amount);
        onSlotItemChange(slot, itemStack);
    }

    
    private void onSlotItemChange(int slot, GeyserItemStack itemStack) {
        if (changedItems != null) {
            changedItems.put(slot, itemStack.getItemStack());
            changedHashedItems.put(slot, DataComponentHashers.hashStack(session, itemStack.getItemStack()));
        }
    }

    private void simulateAction(ClickAction action) {
        GeyserItemStack cursor = getCursor();
        switch (action.click) {
            case LEFT_OUTSIDE -> {
                setCursor(GeyserItemStack.EMPTY);
                return;
            }
            case RIGHT_OUTSIDE -> {
                if (!cursor.isEmpty()) {
                    cursor.sub(1);
                }
                return;
            }
        }

        GeyserItemStack clicked = getItem(action.slot);
        if (translator.getSlotType(action.slot) == SlotType.OUTPUT) {
            switch (action.click) {
                case LEFT, RIGHT -> {
                    if (cursor.isEmpty() && !clicked.isEmpty()) {
                        setCursor(clicked.copy());
                    } else if (InventoryUtils.canStack(cursor, clicked)) {
                        cursor.add(clicked.getAmount());
                    }
                    reduceCraftingGrid(false);
                    setItem(action.slot, GeyserItemStack.EMPTY); 
                }
                case LEFT_SHIFT -> reduceCraftingGrid(true);
            }
        } else {
            switch (action.click) {
                case LEFT:
                    if (!InventoryUtils.canStack(cursor, clicked)) {
                        setCursor(clicked);
                        setItem(action.slot, cursor);
                    } else {
                        setCursor(GeyserItemStack.EMPTY);
                        add(action.slot, clicked, cursor.getAmount());
                    }
                    break;
                case RIGHT:
                    if (cursor.isEmpty() && !clicked.isEmpty()) {
                        int half = clicked.getAmount() / 2; //smaller half
                        setCursor(clicked.copy(clicked.getAmount() - half)); //larger half
                        setAmount(action.slot, clicked, half);
                    } else if (!cursor.isEmpty() && clicked.isEmpty()) {
                        cursor.sub(1);
                        setItem(action.slot, cursor.copy(1));
                    } else if (InventoryUtils.canStack(cursor, clicked)) {
                        cursor.sub(1);
                        add(action.slot, clicked, 1);
                    } else {
                        
                        
                        setCursor(clicked);
                        setItem(action.slot, cursor);
                    }
                    break;
                case LEFT_BUNDLE:
                    Fraction bundleWeight = BundleInventoryTranslator.calculateBundleWeight(clicked.getBundleData().contents());
                    int amountToAddInBundle = Math.min(BundleInventoryTranslator.capacityForItemStack(bundleWeight, cursor), cursor.getAmount());
                    GeyserItemStack toInsertInBundle = cursor.copy(amountToAddInBundle);
                    if (executionBegan) {
                        clicked.getBundleData().contents().add(0, toInsertInBundle);
                        session.getBundleCache().onItemAdded(clicked); 
                    }
                    onSlotItemChange(action.slot, clicked);
                    cursor.sub(amountToAddInBundle);
                    break;
                case LEFT_BUNDLE_FROM_CURSOR:
                    List<GeyserItemStack> contents = cursor.getBundleData().contents();
                    bundleWeight = BundleInventoryTranslator.calculateBundleWeight(contents);
                    amountToAddInBundle = Math.min(BundleInventoryTranslator.capacityForItemStack(bundleWeight, clicked), clicked.getAmount());
                    toInsertInBundle = clicked.copy(amountToAddInBundle);
                    if (executionBegan) {
                        cursor.getBundleData().contents().add(0, toInsertInBundle);
                        session.getBundleCache().onItemAdded(cursor);
                    }
                    sub(action.slot, clicked, amountToAddInBundle);
                    break;
                case RIGHT_BUNDLE:
                    if (!cursor.isEmpty()) {
                        
                        GeyserItemStack itemStack = cursor.getBundleData()
                            .contents()
                            .remove(0);
                        if (executionBegan) {
                            session.getBundleCache().onItemRemoved(cursor, 0);
                        }
                        setItem(action.slot, itemStack);
                        break;
                    }

                    if (executionBegan) {
                        sendSelectedBundleSlot(action.slot);
                    }
                    GeyserItemStack itemStack = clicked.getBundleData()
                        .contents()
                        .remove(desiredBundleSlot);
                    if (executionBegan) {
                        session.getBundleCache().onItemRemoved(clicked, desiredBundleSlot);
                    }
                    onSlotItemChange(action.slot, clicked);
                    setCursor(itemStack);
                    break;
                case SWAP_TO_HOTBAR_1:
                    swap(action.slot, inventory.getOffsetForHotbar(0), clicked);
                    break;
                case SWAP_TO_HOTBAR_2:
                    swap(action.slot, inventory.getOffsetForHotbar(1), clicked);
                    break;
                case SWAP_TO_HOTBAR_3:
                    swap(action.slot, inventory.getOffsetForHotbar(2), clicked);
                    break;
                case SWAP_TO_HOTBAR_4:
                    swap(action.slot, inventory.getOffsetForHotbar(3), clicked);
                    break;
                case SWAP_TO_HOTBAR_5:
                    swap(action.slot, inventory.getOffsetForHotbar(4), clicked);
                    break;
                case SWAP_TO_HOTBAR_6:
                    swap(action.slot, inventory.getOffsetForHotbar(5), clicked);
                    break;
                case SWAP_TO_HOTBAR_7:
                    swap(action.slot, inventory.getOffsetForHotbar(6), clicked);
                    break;
                case SWAP_TO_HOTBAR_8:
                    swap(action.slot, inventory.getOffsetForHotbar(7), clicked);
                    break;
                case SWAP_TO_HOTBAR_9:
                    swap(action.slot, inventory.getOffsetForHotbar(8), clicked);
                    break;
                case LEFT_SHIFT:
                    if (clicked.isEmpty()) {
                        break;
                    }
                    
                    int remaining = clicked.getAmount();
                    
                    boolean sourceInPlayerInventory = action.slot >= translator.size;

                    if (!sourceInPlayerInventory) {
                        int hotbarOffset = inventory.getOffsetForHotbar(0);
                        int mainStart = hotbarOffset - 27;
                        int mainEnd = hotbarOffset - 1;
                        int hotbarEnd = hotbarOffset + 8;

                        
                        
                        
                        
                        
                        
                        
                        remaining = fillPartialStacks(action.slot, clicked, remaining, mainStart, mainEnd);
                        remaining = fillPartialStacks(action.slot, clicked, remaining, hotbarOffset, hotbarEnd);
                        
                        
                        remaining = fillEmptySlots(action.slot, clicked, remaining, mainStart, mainEnd);
                        fillEmptySlots(action.slot, clicked, remaining, hotbarOffset, hotbarEnd); 
                    } else { 
                        
                        
                        remaining = fillPartialStacks(action.slot, clicked, remaining, 0, translator.size - 1);
                        fillEmptySlots(action.slot, clicked, remaining, 0, translator.size - 1);
                    }
                    
                    break;
                case DROP_ONE:
                    if (!clicked.isEmpty()) {
                        sub(action.slot, clicked, 1);
                    }
                    break;
                case DROP_ALL:
                    setItem(action.slot, GeyserItemStack.EMPTY);
                    break;
            }
        }
    }

    private void sendSelectedBundleSlot(int slot) {
        
        session.sendDownstreamGamePacket(new ServerboundSelectBundleItemPacket(slot, desiredBundleSlot));
    }

    
    private void swap(int sourceSlot, int destSlot, GeyserItemStack sourceItem) {
        GeyserItemStack destinationItem = getItem(destSlot);
        setItem(sourceSlot, destinationItem);
        setItem(destSlot, sourceItem);
    }

    private int stateIdHack(ClickAction action) {
        int stateId;
        if (inventory.getNextStateId() != -1) {
            stateId = inventory.getNextStateId();
        } else {
            stateId = inventory.getStateId();
        }

        
        
        
        
        
        
        
        
        
        if (inventory.getContainerType() == ContainerType.CRAFTING && CraftingInventoryTranslator.isCraftingGrid(action.slot)) {
            
            
            int stateIdIncrements;
            GeyserItemStack clicked = getItem(action.slot);
            if (action.click == Click.LEFT) {
                if (!clicked.isEmpty() && !InventoryUtils.canStack(simulatedCursor, clicked)) {
                    
                    stateIdIncrements = 2;
                } else {
                    
                    stateIdIncrements = 1;
                }
            } else if (action.click == Click.RIGHT) {
                stateIdIncrements = 1;
            } else if (action.click.actionType == ContainerActionType.MOVE_TO_HOTBAR_SLOT) {
                stateIdIncrements = 1;
            } else {
                if (session.getGeyser().config().debugMode()) {
                    session.getGeyser().getLogger().debug("Not sure how to handle state ID hack in crafting table: " + plan);
                }
                stateIdIncrements = 1;
            }
            inventory.incrementStateId(stateIdIncrements);
        }

        return stateId;
    }

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
            final int slot = i + 1;
            GeyserItemStack item = getItem(slot);
            if (!item.isEmpty()) {
                
                sub(slot, item, crafted);
            }
        }
    }

    
    private int fillPartialStacks(int sourceSlot, GeyserItemStack source, int remaining, int rangeStart, int rangeEnd) {
        if (remaining <= 0) return 0; 
        for (int i = rangeStart; i <= rangeEnd && remaining > 0; i++) {
            if (!canStack(i, source)) continue;

            GeyserItemStack dest = getItem(i);
            int canAdd = Math.min(remaining, dest.getMaxStackSize() - dest.getAmount());
            if (canAdd <= 0) continue;

            add(i, dest, canAdd);
            sub(sourceSlot, source, canAdd);
            remaining -= canAdd;
        }
        return remaining;
    }

    
    private int fillEmptySlots(int sourceSlot, GeyserItemStack source, int remaining, int rangeStart, int rangeEnd) {
        if (remaining <= 0) return 0; 
        for (int i = rangeStart; i <= rangeEnd && remaining > 0; i++) {
            if (!isEmpty(i)) continue;

            int toMove = Math.min(remaining, source.getMaxStackSize());

            setItem(i, source.copy(toMove));
            sub(sourceSlot, source, toMove);
            remaining -= toMove;
        }
        return remaining;
    }

    
    @Contract("-> new")
    public IntSet getAffectedSlots() {
        IntSet affectedSlots = new IntOpenHashSet();
        for (ClickAction action : plan) {
            if (translator.getSlotType(action.slot) != SlotType.OUTPUT && action.slot != Click.OUTSIDE_SLOT) {
                affectedSlots.add(action.slot);
                if (action.click.actionType == ContainerActionType.MOVE_TO_HOTBAR_SLOT) {
                    
                    affectedSlots.add(inventory.getOffsetForHotbar(((MoveToHotbarAction) action.click.action).ordinal()));
                }
            }
        }
        return affectedSlots;
    }

    private record ClickAction(Click click, int slot, boolean force) {
    }
}
