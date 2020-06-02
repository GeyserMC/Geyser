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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.window.*;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.*;

public class InventoryActionDataTranslator {
    public static void translate(InventoryTranslator translator, GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        if (actions.size() != 2)
            return;

        InventoryActionData worldAction = null;
        InventoryActionData cursorAction = null;
        InventoryActionData containerAction = null;
        boolean refresh = false;
        for (InventoryActionData action : actions) {
            if (action.getSource().getContainerId() == ContainerId.CRAFTING_USE_INGREDIENT || action.getSource().getContainerId() == ContainerId.CRAFTING_RESULT) {
                return;
            } else if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION) {
                worldAction = action;
            } else if (action.getSource().getContainerId() == ContainerId.CURSOR && action.getSlot() == 0) {
                cursorAction = action;
                ItemData translatedCursor = ItemTranslator.translateToBedrock(session, session.getInventory().getCursor());
                if (!translatedCursor.equals(action.getFromItem())) {
                    refresh = true;
                }
            } else {
                containerAction = action;
                ItemData translatedItem = ItemTranslator.translateToBedrock(session, inventory.getItem(translator.bedrockSlotToJava(action)));
                if (!translatedItem.equals(action.getFromItem())) {
                    refresh = true;
                }
            }
        }

        final int craftSlot = session.getCraftSlot();
        session.setCraftSlot(0);

        if (worldAction != null) {
            InventoryActionData sourceAction;
            if (cursorAction != null) {
                sourceAction = cursorAction;
            } else {
                sourceAction = containerAction;
            }

            if (sourceAction != null) {
                if (worldAction.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                    //quick dropping from hotbar?
                    if (session.getInventoryCache().getOpenInventory() == null && sourceAction.getSource().getContainerId() == ContainerId.INVENTORY) {
                        int heldSlot = session.getInventory().getHeldItemSlot();
                        if (sourceAction.getSlot() == heldSlot) {
                            ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(
                                    sourceAction.getToItem().getCount() == 0 ? PlayerAction.DROP_ITEM_STACK : PlayerAction.DROP_ITEM,
                                    new Position(0, 0, 0), BlockFace.DOWN);
                            session.sendDownstreamPacket(actionPacket);
                            ItemStack item = session.getInventory().getItem(heldSlot);
                            if (item != null) {
                                session.getInventory().setItem(heldSlot, new ItemStack(item.getId(), item.getAmount() - 1, item.getNbt()));
                            }
                            return;
                        }
                    }
                    int dropAmount = sourceAction.getFromItem().getCount() - sourceAction.getToItem().getCount();
                    if (sourceAction != cursorAction) { //dropping directly from inventory
                        int javaSlot = translator.bedrockSlotToJava(sourceAction);
                        if (dropAmount == sourceAction.getFromItem().getCount()) {
                            ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(),
                                    inventory.getTransactionId().getAndIncrement(),
                                    javaSlot, null, WindowAction.DROP_ITEM,
                                    DropItemParam.DROP_SELECTED_STACK);
                            session.sendDownstreamPacket(dropPacket);
                        } else {
                            for (int i = 0; i < dropAmount; i++) {
                                ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(),
                                        inventory.getTransactionId().getAndIncrement(),
                                        javaSlot, null, WindowAction.DROP_ITEM,
                                        DropItemParam.DROP_FROM_SELECTED);
                                session.sendDownstreamPacket(dropPacket);
                            }
                        }
                        ItemStack item = session.getInventory().getItem(javaSlot);
                        if (item != null) {
                            session.getInventory().setItem(javaSlot, new ItemStack(item.getId(), item.getAmount() - dropAmount, item.getNbt()));
                        }
                        return;
                    } else { //clicking outside of inventory
                        ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                -999, null, WindowAction.CLICK_ITEM,
                                dropAmount > 1 ? ClickItemParam.LEFT_CLICK : ClickItemParam.RIGHT_CLICK);
                        session.sendDownstreamPacket(dropPacket);
                        ItemStack cursor = session.getInventory().getCursor();
                        if (cursor != null) {
                            session.getInventory().setCursor(new ItemStack(cursor.getId(), dropAmount > 1 ? 0 : cursor.getAmount() - 1, cursor.getNbt()));
                        }
                        return;
                    }
                }
            }
        } else if (cursorAction != null && containerAction != null) {
            //left/right click
            ClickPlan plan = new ClickPlan();
            int javaSlot = translator.bedrockSlotToJava(containerAction);
            if (cursorAction.getFromItem().equals(containerAction.getToItem())
                    && containerAction.getFromItem().equals(cursorAction.getToItem())
                    && !InventoryUtils.canStack(cursorAction.getFromItem(), containerAction.getFromItem())) { //simple swap
                plan.add(Click.LEFT, javaSlot);
            } else if (cursorAction.getFromItem().getCount() > cursorAction.getToItem().getCount()) { //release
                if (cursorAction.getToItem().getCount() == 0) {
                    plan.add(Click.LEFT, javaSlot);
                } else {
                    int difference = cursorAction.getFromItem().getCount() - cursorAction.getToItem().getCount();
                    for (int i = 0; i < difference; i++) {
                        plan.add(Click.RIGHT, javaSlot);
                    }
                }
            } else { //pickup
                if (cursorAction.getFromItem().getCount() == 0) {
                    if (containerAction.getToItem().getCount() == 0) { //pickup all
                        plan.add(Click.LEFT, javaSlot);
                    } else { //pickup some
                        if (translator.getSlotType(javaSlot) == SlotType.FURNACE_OUTPUT
                                || containerAction.getToItem().getCount() == containerAction.getFromItem().getCount() / 2) { //right click
                            plan.add(Click.RIGHT, javaSlot);
                        } else {
                            plan.add(Click.LEFT, javaSlot);
                            int difference = containerAction.getFromItem().getCount() - cursorAction.getToItem().getCount();
                            for (int i = 0; i < difference; i++) {
                                plan.add(Click.RIGHT, javaSlot);
                            }
                        }
                    }
                } else { //pickup into non-empty cursor
                    if (translator.getSlotType(javaSlot) == SlotType.FURNACE_OUTPUT) {
                        if (containerAction.getToItem().getCount() == 0) {
                            plan.add(Click.LEFT, javaSlot);
                        } else {
                            ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(inventory.getId(),
                                    inventory.getTransactionId().getAndIncrement(),
                                    javaSlot, InventoryUtils.REFRESH_ITEM, WindowAction.SHIFT_CLICK_ITEM,
                                    ShiftClickItemParam.LEFT_CLICK);
                            session.sendDownstreamPacket(shiftClickPacket);
                            translator.updateInventory(session, inventory);
                            return;
                        }
                    } else if (translator.getSlotType(javaSlot) == SlotType.OUTPUT) {
                        plan.add(Click.LEFT, javaSlot);
                    } else {
                        int cursorSlot = findTempSlot(inventory, session.getInventory().getCursor(), Collections.singletonList(javaSlot), false);
                        if (cursorSlot != -1) {
                            plan.add(Click.LEFT, cursorSlot);
                        } else {
                            translator.updateInventory(session, inventory);
                            InventoryUtils.updateCursor(session);
                            return;
                        }
                        plan.add(Click.LEFT, javaSlot);
                        int difference = cursorAction.getToItem().getCount() - cursorAction.getFromItem().getCount();
                        for (int i = 0; i < difference; i++) {
                            plan.add(Click.RIGHT, cursorSlot);
                        }
                        plan.add(Click.LEFT, javaSlot);
                        plan.add(Click.LEFT, cursorSlot);
                    }
                }
            }
            plan.execute(session, translator, inventory, refresh);
            return;
        } else {
            ClickPlan plan = new ClickPlan();
            InventoryActionData fromAction;
            InventoryActionData toAction;
            if (actions.get(0).getFromItem().getCount() >= actions.get(0).getToItem().getCount()) {
                fromAction = actions.get(0);
                toAction = actions.get(1);
            } else {
                fromAction = actions.get(1);
                toAction = actions.get(0);
            }
            int fromSlot = translator.bedrockSlotToJava(fromAction);
            int toSlot = translator.bedrockSlotToJava(toAction);

            if (translator.getSlotType(fromSlot) == SlotType.OUTPUT) {
                if ((craftSlot != 0 && craftSlot != -2) && (inventory.getItem(toSlot) == null
                        || InventoryUtils.canStack(session.getInventory().getCursor(), inventory.getItem(toSlot)))) {
                    if (fromAction.getToItem().getCount() == 0) {
                        refresh = true;
                        plan.add(Click.LEFT, toSlot);
                        if (craftSlot != -1) {
                            plan.add(Click.LEFT, craftSlot);
                        }
                    } else {
                        int difference = toAction.getToItem().getCount() - toAction.getFromItem().getCount();
                        for (int i = 0; i < difference; i++) {
                            plan.add(Click.RIGHT, toSlot);
                        }
                        session.setCraftSlot(craftSlot);
                    }
                    plan.execute(session, translator, inventory, refresh);
                    return;
                } else {
                    session.setCraftSlot(-2);
                }
            }

            int cursorSlot = -1;
            if (session.getInventory().getCursor() != null) { //move cursor contents to a temporary slot
                cursorSlot = findTempSlot(inventory,
                        session.getInventory().getCursor(),
                        Arrays.asList(fromSlot, toSlot),
                        translator.getSlotType(fromSlot) == SlotType.OUTPUT);
                if (cursorSlot != -1) {
                    plan.add(Click.LEFT, cursorSlot);
                } else {
                    translator.updateInventory(session, inventory);
                    InventoryUtils.updateCursor(session);
                    return;
                }
            }
            if ((fromAction.getFromItem().equals(toAction.getToItem()) && !InventoryUtils.canStack(fromAction.getFromItem(), toAction.getFromItem()))
                    || fromAction.getToItem().getId() == 0) { //slot swap
                plan.add(Click.LEFT, fromSlot);
                plan.add(Click.LEFT, toSlot);
                if (fromAction.getToItem().getId() != 0) {
                    plan.add(Click.LEFT, fromSlot);
                }
            } else if (InventoryUtils.canStack(fromAction.getFromItem(), toAction.getToItem())) { //partial item move
                if (translator.getSlotType(fromSlot) == SlotType.FURNACE_OUTPUT) {
                    ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(inventory.getId(),
                            inventory.getTransactionId().getAndIncrement(),
                            fromSlot, InventoryUtils.REFRESH_ITEM, WindowAction.SHIFT_CLICK_ITEM,
                            ShiftClickItemParam.LEFT_CLICK);
                    session.sendDownstreamPacket(shiftClickPacket);
                    translator.updateInventory(session, inventory);
                    return;
                } else if (translator.getSlotType(fromSlot) == SlotType.OUTPUT) {
                    session.setCraftSlot(cursorSlot);
                    plan.add(Click.LEFT, fromSlot);
                    int difference = toAction.getToItem().getCount() - toAction.getFromItem().getCount();
                    for (int i = 0; i < difference; i++) {
                        plan.add(Click.RIGHT, toSlot);
                    }
                    //client will send additional packets later to finish transferring crafting output
                    //translator will know how to handle this using the craftSlot variable
                } else {
                    plan.add(Click.LEFT, fromSlot);
                    int difference = toAction.getToItem().getCount() - toAction.getFromItem().getCount();
                    for (int i = 0; i < difference; i++) {
                        plan.add(Click.RIGHT, toSlot);
                    }
                    plan.add(Click.LEFT, fromSlot);
                }
            }
            if (cursorSlot != -1) {
                plan.add(Click.LEFT, cursorSlot);
            }
            plan.execute(session, translator, inventory, refresh);
            return;
        }

        translator.updateInventory(session, inventory);
        InventoryUtils.updateCursor(session);
    }

    private static int findTempSlot(Inventory inventory, ItemStack item, List<Integer> slotBlacklist, boolean emptyOnly) {
        /*try and find a slot that can temporarily store the given item
        only look in the main inventory and hotbar
        only slots that are empty or contain a different type of item are valid*/
        int offset = inventory.getId() == 0 ? 1 : 0; //offhand is not a viable slot (some servers disable it)
        List<ItemStack> itemBlacklist = new ArrayList<>(slotBlacklist.size() + 1);
        itemBlacklist.add(item);
        for (int slot : slotBlacklist) {
            ItemStack blacklistItem = inventory.getItem(slot);
            if (blacklistItem != null)
                itemBlacklist.add(blacklistItem);
        }
        for (int i = inventory.getSize() - (36 + offset); i < inventory.getSize() - offset; i++) {
            ItemStack testItem = inventory.getItem(i);
            boolean acceptable = true;
            if (testItem != null) {
                if (emptyOnly) {
                    continue;
                }
                for (ItemStack blacklistItem : itemBlacklist) {
                    if (InventoryUtils.canStack(testItem, blacklistItem)) {
                        acceptable = false;
                        break;
                    }
                }
            }
            if (acceptable && !slotBlacklist.contains(i))
                return i;
        }
        //could not find a viable temp slot
        return -1;
    }
}
