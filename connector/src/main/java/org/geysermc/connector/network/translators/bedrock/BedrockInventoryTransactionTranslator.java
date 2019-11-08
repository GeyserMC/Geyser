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
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BedrockInventoryTransactionTranslator extends PacketTranslator<InventoryTransactionPacket> {

    @Override
    public void translate(InventoryTransactionPacket packet, GeyserSession session) {
        switch (packet.getTransactionType()) {
            case NORMAL:
                Inventory inventory = session.getInventoryCache().getOpenInventory();
                if (inventory == null)
                    inventory = session.getInventory();
                InventoryTranslator translator = TranslatorsInit.getInventoryTranslators().get(inventory.getWindowType());

                InventoryAction worldAction = null;
                InventoryAction cursorAction = null;
                for (InventoryAction action : packet.getActions()) {
                    if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION) {
                        worldAction = action;
                    } else if (action.getSource().getContainerId() == ContainerId.CURSOR) {
                        cursorAction = action;
                    }
                }

                List<InventoryAction> actions = packet.getActions();
                if (inventory.getWindowType() == WindowType.ANVIL) {
                    InventoryAction anvilResult = null;
                    InventoryAction anvilInput = null;
                    for (InventoryAction action : packet.getActions()) {
                        if (action.getSource().getContainerId() == ContainerId.ANVIL_RESULT) {
                            anvilResult = action;
                        } else if (action.getSource().getContainerId() == ContainerId.CONTAINER_INPUT) {
                            anvilInput = action;
                        }
                    }
                    ItemData itemName = null;
                    if (anvilResult != null) {
                        itemName = anvilResult.getFromItem();
                        actions = new ArrayList<>(2);
                        for (InventoryAction action : packet.getActions()) { //packet sent by client when grabbing anvil output needs useless actions stripped
                            if (!(action.getSource().getContainerId() == ContainerId.CONTAINER_INPUT || action.getSource().getContainerId() == ContainerId.ANVIL_MATERIAL)) {
                                actions.add(action);
                            }
                        }
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
                }

                if (actions.size() == 2) {
                    if (worldAction != null && worldAction.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                        //find container action
                        InventoryAction containerAction = null;
                        for (InventoryAction action : actions) {
                            if (action != worldAction) {
                                containerAction = action;
                                break;
                            }
                        }
                        if (containerAction != null) {
                            //quick dropping from hotbar?
                            if (session.getInventoryCache().getOpenInventory() == null && containerAction.getSource().getContainerId() == ContainerId.INVENTORY) {
                                if (containerAction.getSlot() == session.getInventory().getHeldItemSlot()) {
                                    ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(
                                            containerAction.getToItem().getCount() == 0 ? PlayerAction.DROP_ITEM_STACK : PlayerAction.DROP_ITEM,
                                            new Position(0, 0, 0), BlockFace.DOWN);
                                    session.getDownstream().getSession().send(actionPacket);
                                    return;
                                }
                            }
                            boolean leftClick = containerAction.getToItem().getCount() == 0;
                            if (containerAction.getSource().getContainerId() != ContainerId.CURSOR) { //dropping directly from inventory
                                int javaSlot = translator.bedrockSlotToJava(containerAction);
                                ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                        javaSlot, null, WindowAction.DROP_ITEM,
                                        leftClick ? DropItemParam.DROP_SELECTED_STACK : DropItemParam.DROP_FROM_SELECTED);
                                session.getDownstream().getSession().send(dropPacket);
                                return;
                            } else { //clicking outside of inventory
                                ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                        -999, null, WindowAction.CLICK_ITEM,
                                        leftClick ? ClickItemParam.LEFT_CLICK : ClickItemParam.RIGHT_CLICK);
                                session.getDownstream().getSession().send(dropPacket);
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
                            if (InventoryUtils.canCombine(cursorAction.getFromItem(), cursorAction.getToItem())
                                    && cursorAction.getToItem().getCount() > cursorAction.getFromItem().getCount()) { //fill stack
                                int javaSlot = session.getLastClickedSlot();
                                ClientWindowActionPacket fillStackPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                        javaSlot, null, WindowAction.FILL_STACK, FillStackParam.FILL);
                                session.getDownstream().getSession().send(fillStackPacket);
                                translator.updateInventory(session, inventory); //bedrock fill stack can sometimes differ from java version, refresh and let server change slots
                                return;
                            } else {
                                //left/right click
                                int javaSlot = translator.bedrockSlotToJava(containerAction);
                                boolean rightClick;
                                if (cursorAction.getFromItem().getCount() == 0) { //picking up item
                                    rightClick = containerAction.getToItem().getCount() != 0;
                                } else { //releasing item
                                    rightClick = cursorAction.getToItem().getCount() != 0 && cursorAction.getFromItem().getCount() - cursorAction.getToItem().getCount() == 1;
                                }
                                ItemStack translatedCursor = TranslatorsInit.getItemTranslator().translateToJava(cursorAction.getFromItem());
                                boolean refresh = !Objects.equals(session.getInventory().getCursor(), translatedCursor.getId() == 0 ? null : translatedCursor); //refresh slot if there is a cursor mismatch
                                ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(inventory.getId(),
                                        inventory.getTransactionId().getAndIncrement(), javaSlot,
                                        refresh ? new ItemStack(1, 127, new CompoundTag("")) : InventoryUtils.fixStack(TranslatorsInit.getItemTranslator().translateToJava(containerAction.getFromItem())), //send invalid item stack to refresh slot
                                        WindowAction.CLICK_ITEM, rightClick ? ClickItemParam.RIGHT_CLICK : ClickItemParam.LEFT_CLICK);
                                session.getDownstream().getSession().send(clickPacket);
                                inventory.getItems()[javaSlot] = TranslatorsInit.getItemTranslator().translateToJava(containerAction.getToItem());
                                translator.updateSlot(session, inventory, javaSlot);
                                session.getInventory().setCursor(TranslatorsInit.getItemTranslator().translateToJava(cursorAction.getToItem()));
                                session.setLastClickedSlot(javaSlot);
                                return;
                            }
                        }
                    } else {
                        //either moving 1 item or swapping 2 slots (touchscreen or one slot shift click)
                        InventoryAction fromAction;
                        InventoryAction toAction;
                        //find source slot
                        if (actions.get(0).getFromItem().getCount() > actions.get(0).getToItem().getCount()) {
                            fromAction = actions.get(0);
                            toAction = actions.get(1);
                        } else {
                            fromAction = actions.get(1);
                            toAction = actions.get(0);
                        }
                        int fromSlot = translator.bedrockSlotToJava(fromAction);
                        int toSlot = translator.bedrockSlotToJava(toAction);

                        //check if dealing with output only slot like furnace. this is to handle a situation where the output slot was partially emptied without right clicking (touchscreen or full inventory)
                        //this is only possible by shift clicking
                        if (translator.isOutputSlot(fromAction) && fromAction.getToItem().getCount() != 0) {
                            ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                    fromSlot, InventoryUtils.fixStack(inventory.getItem(fromSlot)), WindowAction.SHIFT_CLICK_ITEM, ShiftClickItemParam.LEFT_CLICK);
                            session.getDownstream().getSession().send(shiftClickPacket);
                            inventory.getItems()[toSlot] = TranslatorsInit.getItemTranslator().translateToJava(toAction.getToItem());
                            inventory.getItems()[fromSlot] = TranslatorsInit.getItemTranslator().translateToJava(fromAction.getToItem());
                            return;
                        } else {
                            //pickup fromAction item
                            ClientWindowActionPacket leftClick1Packet = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                    fromSlot, InventoryUtils.fixStack(TranslatorsInit.getItemTranslator().translateToJava(fromAction.getFromItem())), WindowAction.CLICK_ITEM,
                                    ClickItemParam.LEFT_CLICK);
                            session.getDownstream().getSession().send(leftClick1Packet);
                            //release fromAction item into toAction slot
                            ClientWindowActionPacket leftClick2Packet = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                    toSlot, InventoryUtils.fixStack(TranslatorsInit.getItemTranslator().translateToJava(toAction.getFromItem())), WindowAction.CLICK_ITEM,
                                    ClickItemParam.LEFT_CLICK);
                            session.getDownstream().getSession().send(leftClick2Packet);
                            //test if swapping two items or moving one item
                            //if swapping then complete it
                            if (fromAction.getToItem().getId() != 0) {
                                ClientWindowActionPacket leftClick3Packet = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                        fromSlot, null, WindowAction.CLICK_ITEM,
                                        ClickItemParam.LEFT_CLICK);
                                session.getDownstream().getSession().send(leftClick3Packet);
                            }
                            inventory.getItems()[toSlot] = TranslatorsInit.getItemTranslator().translateToJava(toAction.getToItem());
                            inventory.getItems()[fromSlot] = TranslatorsInit.getItemTranslator().translateToJava(fromAction.getToItem());
                            return;
                        }
                    }
                } else if (actions.size() > 2) {
                    //shift click or fill stack?
                    ItemData firstItem;
                    if (actions.get(0).getFromItem().getId() != 0) {
                        firstItem = actions.get(0).getFromItem();
                    } else {
                        firstItem = actions.get(0).getToItem();
                    }
                    List<InventoryAction> sourceActions = new ArrayList<>(actions.size());
                    List<InventoryAction> destActions = new ArrayList<>(actions.size());
                    boolean sameItems = true;
                    for (InventoryAction action : actions) {
                        if (action.getFromItem().getCount() > action.getToItem().getCount()) {
                            if (!InventoryUtils.canCombine(action.getFromItem(), firstItem))
                                sameItems = false;
                            sourceActions.add(action);
                        } else {
                            if (!InventoryUtils.canCombine(action.getToItem(), firstItem))
                                sameItems = false;
                            destActions.add(action);
                        }
                    }
                    if (sameItems) {
                        if (sourceActions.size() == 1) { //shift click
                            InventoryAction sourceAction = sourceActions.get(0);
                            //in java edition, shift clicked item must move across hotbar and main inventory
                            if (sourceAction.getSource().getContainerId() == ContainerId.INVENTORY) {
                                for (InventoryAction action : actions) {
                                    if (action != sourceAction && action.getSource().getContainerId() == ContainerId.INVENTORY) {
                                        if ((sourceAction.getSlot() < 9 && action.getSlot() < 9) || (sourceAction.getSlot() >= 9 && action.getSlot() >= 9)) {
                                            //shift click not compatible with java edition. refresh inventory and abort
                                            translator.updateInventory(session, inventory);
                                            return;
                                        }
                                    }
                                }
                            }
                            int javaSlot = translator.bedrockSlotToJava(sourceAction);
                            ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                    javaSlot, InventoryUtils.fixStack(inventory.getItem(javaSlot)), WindowAction.SHIFT_CLICK_ITEM, ShiftClickItemParam.LEFT_CLICK);
                            session.getDownstream().getSession().send(shiftClickPacket);
                            return;
                        } else if (destActions.size() == 1) { //fill stack
                            InventoryAction destAction = destActions.get(0);
                            int javaSlot;
                            if (destAction != cursorAction) { //if touchscreen
                                javaSlot = translator.bedrockSlotToJava(destAction);
                                ClientWindowActionPacket leftClickPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                        javaSlot, InventoryUtils.fixStack(inventory.getItem(javaSlot)), WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
                                session.getDownstream().getSession().send(leftClickPacket);
                            } else {
                                javaSlot = session.getLastClickedSlot();
                            }
                            ClientWindowActionPacket fillStackPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                    javaSlot, null, WindowAction.FILL_STACK, FillStackParam.FILL);
                            session.getDownstream().getSession().send(fillStackPacket);
                            if (destAction != cursorAction) { //if touchscreen
                                ClientWindowActionPacket leftClickPacket = new ClientWindowActionPacket(inventory.getId(), inventory.getTransactionId().getAndIncrement(),
                                        javaSlot, null, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
                                session.getDownstream().getSession().send(leftClickPacket);
                                inventory.getItems()[javaSlot] = TranslatorsInit.getItemTranslator().translateToJava(destAction.getToItem());
                            }
                            translator.updateInventory(session, inventory);
                            return;
                        }
                    }
                }

                //refresh inventory, transaction was not translated
                translator.updateInventory(session, inventory);
                break;
            case INVENTORY_MISMATCH:
                InventorySlotPacket cursorPacket = new InventorySlotPacket();
                cursorPacket.setContainerId(ContainerId.CURSOR);
                cursorPacket.setSlot(TranslatorsInit.getItemTranslator().translateToBedrock(session.getInventory().getCursor()));
                session.getUpstream().sendPacket(cursorPacket);

                Inventory inv = session.getInventoryCache().getOpenInventory();
                if (inv == null)
                    inv = session.getInventory();
                TranslatorsInit.getInventoryTranslators().get(inv.getWindowType()).updateInventory(session, inv);
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
}
