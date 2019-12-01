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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.window.*;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.InventoryAction;
import com.nukkitx.protocol.bedrock.data.InventorySource;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;

import java.util.*;

public class BedrockInventoryTransactionTranslator extends PacketTranslator<InventoryTransactionPacket> {
    private final ItemStack refreshItem = new ItemStack(1, 127, new CompoundTag(""));

    @Override
    public void translate(InventoryTransactionPacket packet, GeyserSession session) {
        switch (packet.getTransactionType()) {
            case NORMAL:
                for (InventoryAction action : packet.getActions()) {
                    if (action.getSource().getContainerId() == ContainerId.CRAFTING_USE_INGREDIENT ||
                            action.getSource().getContainerId() == ContainerId.CRAFTING_RESULT) {
                        return;
                    }
                }

                Inventory inventory = session.getInventoryCache().getOpenInventory();
                if (inventory == null)
                    inventory = session.getInventory();
                InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());

                int craftSlot = session.getCraftSlot();
                session.setCraftSlot(0);

                if (session.getGameMode() == GameMode.CREATIVE && inventory.getId() == 0) {
                    ItemStack javaItem;
                    for (InventoryAction action : packet.getActions()) {
                        switch (action.getSource().getContainerId()) {
                            case ContainerId.INVENTORY:
                            case ContainerId.ARMOR:
                            case ContainerId.OFFHAND:
                                int javaSlot = translator.bedrockSlotToJava(action);
                                if (action.getToItem().getId() == 0) {
                                    javaItem = new ItemStack(-1, 0, null);
                                } else {
                                    javaItem = TranslatorsInit.getItemTranslator().translateToJava(action.getToItem());
                                    if (javaItem.getId() == 0) { //item missing mapping
                                        translator.updateInventory(session, inventory);
                                        break;
                                    }
                                }
                                ClientCreativeInventoryActionPacket creativePacket = new ClientCreativeInventoryActionPacket(javaSlot, fixStack(javaItem));
                                session.getDownstream().getSession().send(creativePacket);
                                inventory.setItem(javaSlot, javaItem);
                                break;
                            case ContainerId.NONE:
                                if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION &&
                                        action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                                    javaItem = TranslatorsInit.getItemTranslator().translateToJava(action.getToItem());
                                    if (javaItem.getId() == 0) { //item missing mapping
                                        break;
                                    }
                                    ClientCreativeInventoryActionPacket creativeDropPacket = new ClientCreativeInventoryActionPacket(-1, fixStack(javaItem));
                                    session.getDownstream().getSession().send(creativeDropPacket);
                                }
                                break;
                        }
                    }
                    return;
                }

                InventoryAction worldAction = null;
                InventoryAction cursorAction = null;
                for (InventoryAction action : packet.getActions()) {
                    if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION) {
                        worldAction = action;
                    } else if (action.getSource().getContainerId() == ContainerId.CURSOR && action.getSlot() == 0) {
                        cursorAction = action;
                    }
                }
                List<InventoryAction> actions = packet.getActions();
                if (inventory.getWindowType() == WindowType.ANVIL) {
                    InventoryAction anvilResult = null;
                    InventoryAction anvilInput = null;
                    for (InventoryAction action : packet.getActions()) {
                        if (action.getSource().getContainerId() == ContainerId.ANVIL_MATERIAL) {
                            //useless packet
                            return;
                        } else if (action.getSource().getContainerId() == ContainerId.ANVIL_RESULT) {
                            anvilResult = action;
                        } else if (translator.bedrockSlotToJava(action) == 0) {
                            anvilInput = action;
                        }
                    }
                    ItemData itemName = null;
                    if (anvilResult != null) {
                        itemName = anvilResult.getFromItem();
                    } else if (anvilInput != null) {
                        itemName = anvilInput.getToItem();
                    }
                    if (itemName != null) {
                        String rename;
                        com.nukkitx.nbt.tag.CompoundTag tag = itemName.getTag();
                        if (tag != null) {
                            rename = tag.getAsCompound("display").getAsString("Name");
                        } else {
                            rename = "";
                        }
                        ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(rename);
                        session.getDownstream().getSession().send(renameItemPacket);
                    }
                    if (anvilResult != null) {
                        //client will send another packet to grab anvil output
                        //this packet was only used to send rename packet
                        return;
                    }
                }

                if (actions.size() == 2) {
                    if (worldAction != null) {
                        //find container action
                        InventoryAction containerAction = null;
                        for (InventoryAction action : actions) {
                            if (action != worldAction) {
                                containerAction = action;
                                break;
                            }
                        }
                        if (containerAction != null && worldAction.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                            //quick dropping from hotbar?
                            if (session.getInventoryCache().getOpenInventory() == null && containerAction.getSource().getContainerId() == ContainerId.INVENTORY) {
                                int heldSlot = session.getInventory().getHeldItemSlot();
                                if (containerAction.getSlot() == heldSlot) {
                                    ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(
                                            containerAction.getToItem().getCount() == 0 ? PlayerAction.DROP_ITEM_STACK : PlayerAction.DROP_ITEM,
                                            new Position(0, 0, 0), BlockFace.DOWN);
                                    session.getDownstream().getSession().send(actionPacket);
                                    ItemStack item = session.getInventory().getItem(heldSlot);
                                    if (item != null) {
                                        session.getInventory().setItem(heldSlot, new ItemStack(item.getId(), item.getAmount() - 1, item.getNbt()));
                                    }
                                    return;
                                }
                            }
                            int dropAmount = containerAction.getFromItem().getCount() - containerAction.getToItem().getCount();
                            if (containerAction != cursorAction) { //dropping directly from inventory
                                int javaSlot = translator.bedrockSlotToJava(containerAction);
                                if (dropAmount == containerAction.getFromItem().getCount()) {
                                    ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(),
                                            inventory.getTransactionId().getAndIncrement(),
                                            javaSlot, null, WindowAction.DROP_ITEM,
                                            DropItemParam.DROP_SELECTED_STACK);
                                    session.getDownstream().getSession().send(dropPacket);
                                } else {
                                    for (int i = 0; i < dropAmount; i++) {
                                        ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(),
                                                inventory.getTransactionId().getAndIncrement(),
                                                javaSlot, null, WindowAction.DROP_ITEM,
                                                DropItemParam.DROP_FROM_SELECTED);
                                        session.getDownstream().getSession().send(dropPacket);
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
                                session.getDownstream().getSession().send(dropPacket);
                                ItemStack cursor = session.getInventory().getCursor();
                                if (cursor != null) {
                                    session.getInventory().setCursor(new ItemStack(cursor.getId(), dropAmount > 1 ? 0 : cursor.getAmount() - 1, cursor.getNbt()));
                                }
                                return;
                            }
                        }
                    } else if (cursorAction != null) {
                        //find container action
                        InventoryAction containerAction = null;
                        for (InventoryAction action : actions) {
                            if (action != cursorAction) {
                                containerAction = action;
                                break;
                            }
                        }
                        if (containerAction != null) {
                            //left/right click
                            List<ClickAction> plan = new ArrayList<>();
                            ItemStack translatedCursor = cursorAction.getFromItem().isValid() ?
                                    TranslatorsInit.getItemTranslator().translateToJava(cursorAction.getFromItem()) : null;
                            ItemStack currentCursor = session.getInventory().getCursor();
                            boolean refresh = false;
                            if (currentCursor != null) {
                                if (translatedCursor != null) {
                                    refresh = !(currentCursor.getId() == translatedCursor.getId() &&
                                            currentCursor.getAmount() == translatedCursor.getAmount());
                                } else {
                                    refresh = true;
                                }
                            }

                            int javaSlot = translator.bedrockSlotToJava(containerAction);
                            if (cursorAction.getFromItem().equals(containerAction.getToItem()) &&
                                    containerAction.getFromItem().equals(cursorAction.getToItem()) &&
                                    !canStack(cursorAction.getFromItem(), containerAction.getFromItem())) { //simple swap
                                Click.LEFT.onSlot(javaSlot, plan);
                            } else if (cursorAction.getFromItem().getCount() > cursorAction.getToItem().getCount()) { //release
                                if (cursorAction.getToItem().getCount() == 0) {
                                    Click.LEFT.onSlot(javaSlot, plan);
                                } else {
                                    int difference = cursorAction.getFromItem().getCount() - cursorAction.getToItem().getCount();
                                    for (int i = 0; i < difference; i++) {
                                        Click.RIGHT.onSlot(javaSlot, plan);
                                    }
                                }
                            } else { //pickup
                                if (cursorAction.getFromItem().getCount() == 0) {
                                    if (containerAction.getToItem().getCount() == 0) { //pickup all
                                        Click.LEFT.onSlot(javaSlot, plan);
                                    } else { //pickup some
                                        if (translator.getSlotType(javaSlot) == SlotType.FURNACE_OUTPUT ||
                                                containerAction.getToItem().getCount() == containerAction.getFromItem().getCount() / 2) { //right click
                                            Click.RIGHT.onSlot(javaSlot, plan);
                                        } else {
                                            Click.LEFT.onSlot(javaSlot, plan);
                                            int difference = containerAction.getFromItem().getCount() - cursorAction.getToItem().getCount();
                                            for (int i = 0; i < difference; i++) {
                                                Click.RIGHT.onSlot(javaSlot, plan);
                                            }
                                        }
                                    }
                                } else { //pickup into non-empty cursor
                                    if (translator.getSlotType(javaSlot) == SlotType.FURNACE_OUTPUT) {
                                        if (containerAction.getToItem().getCount() == 0) {
                                            Click.LEFT.onSlot(javaSlot, plan);
                                        } else {
                                            ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(inventory.getId(),
                                                    inventory.getTransactionId().getAndIncrement(),
                                                    javaSlot, refreshItem, WindowAction.SHIFT_CLICK_ITEM,
                                                    ShiftClickItemParam.LEFT_CLICK);
                                            session.getDownstream().getSession().send(shiftClickPacket);
                                            translator.updateInventory(session, inventory);
                                            return;
                                        }
                                    } else if (translator.getSlotType(javaSlot) == SlotType.OUTPUT) {
                                        Click.LEFT.onSlot(javaSlot, plan);
                                    } else {
                                        int cursorSlot = findTempSlot(inventory, session.getInventory().getCursor(), Collections.singletonList(javaSlot));
                                        if (cursorSlot != -1) {
                                            Click.LEFT.onSlot(cursorSlot, plan);
                                        } else {
                                            translator.updateInventory(session, inventory);
                                            return;
                                        }
                                        Click.LEFT.onSlot(javaSlot, plan);
                                        int difference = cursorAction.getToItem().getCount() - cursorAction.getFromItem().getCount();
                                        for (int i = 0; i < difference; i++) {
                                            Click.RIGHT.onSlot(cursorSlot, plan);
                                        }
                                        Click.LEFT.onSlot(javaSlot, plan);
                                        Click.LEFT.onSlot(cursorSlot, plan);
                                    }
                                }
                            }
                            executePlan(session, inventory, translator, plan, refresh);
                            return;
                        }
                    } else {
                        List<ClickAction> plan = new ArrayList<>();
                        InventoryAction fromAction;
                        InventoryAction toAction;
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
                            if ((craftSlot != 0 && craftSlot != -2) && (inventory.getItem(toSlot) == null ||
                                    canStack(session.getInventory().getCursor(), inventory.getItem(toSlot)))) {
                                boolean refresh = false;
                                if (fromAction.getToItem().getCount() == 0) {
                                    refresh = true;
                                    Click.LEFT.onSlot(toSlot, plan);
                                    if (craftSlot != -1) {
                                        Click.LEFT.onSlot(craftSlot, plan);
                                    }
                                } else {
                                    int difference = toAction.getToItem().getCount() - toAction.getFromItem().getCount();
                                    for (int i = 0; i < difference; i++) {
                                        Click.RIGHT.onSlot(toSlot, plan);
                                    }
                                    session.setCraftSlot(craftSlot);
                                }
                                executePlan(session, inventory, translator, plan, refresh);
                                return;
                            } else {
                                session.setCraftSlot(-2);
                            }
                        }

                        int cursorSlot = -1;
                        if (session.getInventory().getCursor() != null) { //move cursor contents to a temporary slot
                            cursorSlot = findTempSlot(inventory, session.getInventory().getCursor(), Arrays.asList(fromSlot, toSlot));
                            if (cursorSlot != -1) {
                                Click.LEFT.onSlot(cursorSlot, plan);
                            } else {
                                translator.updateInventory(session, inventory);
                                return;
                            }
                        }
                        if ((fromAction.getFromItem().equals(toAction.getToItem()) && !canStack(fromAction.getFromItem(), toAction.getFromItem())) || fromAction.getToItem().getId() == 0) { //slot swap
                            Click.LEFT.onSlot(fromSlot, plan);
                            Click.LEFT.onSlot(toSlot, plan);
                            if (fromAction.getToItem().getId() != 0) {
                                Click.LEFT.onSlot(fromSlot, plan);
                            }
                        } else if (canStack(fromAction.getFromItem(), toAction.getToItem())) { //partial item move
                            if (translator.getSlotType(fromSlot) == SlotType.FURNACE_OUTPUT) {
                                ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(inventory.getId(),
                                        inventory.getTransactionId().getAndIncrement(),
                                        fromSlot, refreshItem, WindowAction.SHIFT_CLICK_ITEM,
                                        ShiftClickItemParam.LEFT_CLICK);
                                session.getDownstream().getSession().send(shiftClickPacket);
                                translator.updateInventory(session, inventory);
                                return;
                            } else if (translator.getSlotType(fromSlot) == SlotType.OUTPUT) {
                                session.setCraftSlot(cursorSlot);
                                Click.LEFT.onSlot(fromSlot, plan);
                                int difference = toAction.getToItem().getCount() - toAction.getFromItem().getCount();
                                for (int i = 0; i < difference; i++) {
                                    Click.RIGHT.onSlot(toSlot, plan);
                                }
                                //client will send additional packets later to finish transferring crafting output
                                //translator will know how to handle this using the craftSlot variable
                            } else {
                                Click.LEFT.onSlot(fromSlot, plan);
                                int difference = toAction.getToItem().getCount() - toAction.getFromItem().getCount();
                                for (int i = 0; i < difference; i++) {
                                    Click.RIGHT.onSlot(toSlot, plan);
                                }
                                Click.LEFT.onSlot(fromSlot, plan);
                            }
                        }
                        if (cursorSlot != -1) {
                            Click.LEFT.onSlot(cursorSlot, plan);
                        }
                        executePlan(session, inventory, translator, plan, false);
                        return;
                    }
                }
                translator.updateInventory(session, inventory);
                break;
            case INVENTORY_MISMATCH:
                InventorySlotPacket cursorPacket = new InventorySlotPacket();
                cursorPacket.setContainerId(ContainerId.CURSOR);
                cursorPacket.setSlot(TranslatorsInit.getItemTranslator().translateToBedrock(session.getInventory().getCursor()));
                //session.getUpstream().sendPacket(cursorPacket);

                Inventory inv = session.getInventoryCache().getOpenInventory();
                if (inv == null)
                    inv = session.getInventory();
                TranslatorsInit.getInventoryTranslators().get(inv.getWindowType()).updateInventory(session, inv);
                break;
            case ITEM_USE:
                if (packet.getActionType() == 1) {
                    ClientPlayerUseItemPacket useItemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                    session.getDownstream().getSession().send(useItemPacket);
                } else if (packet.getActionType() == 2) {
                    PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
                    Position pos = new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ());
                    ClientPlayerActionPacket breakPacket = new ClientPlayerActionPacket(action, pos, BlockFace.values()[packet.getFace()]);
                    session.getDownstream().getSession().send(breakPacket);
                }
                break;
            case ITEM_RELEASE:
                if (packet.getActionType() == 0) {
                    ClientPlayerActionPacket releaseItemPacket = new ClientPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, new Position(0, 0, 0), BlockFace.DOWN);
                    session.getDownstream().getSession().send(releaseItemPacket);
                }
                break;
            case ITEM_USE_ON_ENTITY:
                Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                if (entity == null)
                    return;

                Vector3f vector = packet.getClickPosition();
                ClientPlayerInteractEntityPacket entityPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.values()[packet.getActionType()], vector.getX(), vector.getY(), vector.getZ(), Hand.MAIN_HAND);

                session.getDownstream().getSession().send(entityPacket);
                break;
        }
    }

    private int findTempSlot(Inventory inventory, ItemStack item, List<Integer> slotBlacklist) {
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
                for (ItemStack blacklistItem : itemBlacklist) {
                    if (canStack(testItem, blacklistItem)) {
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

    //NPE if compound tag is null
    private ItemStack fixStack(ItemStack stack) {
        if (stack == null || stack.getId() == 0)
            return null;
        return new ItemStack(stack.getId(), stack.getAmount(), stack.getNbt() == null ? new CompoundTag("") : stack.getNbt());
    }

    private boolean canStack(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null)
            return false;
        return item1.getId() == item2.getId() && Objects.equals(item1.getNbt(), item2.getNbt());
    }

    private boolean canStack(ItemData item1, ItemData item2) {
        if (item1 == null || item2 == null)
            return false;
        return item1.equals(item2, false, true, true);
    }

    private void executePlan(GeyserSession session, Inventory inventory, InventoryTranslator translator, List<ClickAction> plan, boolean refresh) {
        PlayerInventory playerInventory = session.getInventory();
        ListIterator<ClickAction> planIter = plan.listIterator();
        while (planIter.hasNext()) {
            ClickAction action = planIter.next();
            ItemStack cursorItem = playerInventory.getCursor();
            ItemStack clickedItem = inventory.getItem(action.slot);
            short actionId = (short) inventory.getTransactionId().getAndIncrement();
            boolean isOutput = translator.getSlotType(action.slot) == SlotType.OUTPUT;

            if (isOutput || translator.getSlotType(action.slot) == SlotType.FURNACE_OUTPUT)
                refresh = true;
            ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(inventory.getId(),
                    actionId, action.slot, !planIter.hasNext() && refresh ? refreshItem : fixStack(clickedItem),
                    WindowAction.CLICK_ITEM, action.click.actionParam);

            if (isOutput) {
                if (cursorItem == null && clickedItem != null) {
                    playerInventory.setCursor(clickedItem);
                } else if (canStack(cursorItem, clickedItem)) {
                    playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                            cursorItem.getAmount() + clickedItem.getAmount(), cursorItem.getNbt()));
                }
            } else {
                switch (action.click) {
                    case LEFT:
                        if (!canStack(cursorItem, clickedItem)) {
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
                        } else if (canStack(cursorItem, clickedItem)) {
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
    }

    private enum Click {
        LEFT(ClickItemParam.LEFT_CLICK),
        RIGHT(ClickItemParam.RIGHT_CLICK);

        final WindowActionParam actionParam;
        Click(WindowActionParam actionParam) {
            this.actionParam = actionParam;
        }
        void onSlot(int slot, List<ClickAction> plan) {
            plan.add(new ClickAction(slot, this));
        }
    }

    private static class ClickAction {
        final int slot;
        final Click click;
        ClickAction(int slot, Click click) {
            this.slot = slot;
            this.click = click;
        }
    }
}
