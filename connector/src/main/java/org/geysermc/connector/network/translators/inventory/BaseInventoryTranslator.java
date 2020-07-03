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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.NonNull;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.action.Click;
import org.geysermc.connector.network.translators.inventory.action.Execute;
import org.geysermc.connector.network.translators.inventory.action.Refresh;
import org.geysermc.connector.network.translators.inventory.action.Transaction;
import org.geysermc.connector.network.translators.inventory.action.Drop;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseInventoryTranslator extends InventoryTranslator{
    BaseInventoryTranslator(int size) {
        super(size);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        //
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        int slotnum = action.getSlot();
        switch (action.getSource().getContainerId()) {
            case ContainerId.INVENTORY:
                //hotbar
                if (slotnum >= 9) {
                    return slotnum + this.size - 9;
                }
                return slotnum + this.size + 27;
            case ContainerId.UI:
                if (action.getSlot() == 0) {
                    return -1;
                }
                break;
        }

        return slotnum;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot >= this.size) {
            final int tmp = slot - this.size;
            if (tmp < 27) {
                return tmp + 9;
            } else {
                return tmp - 27;
            }
        }
        return slot;
    }

    /**
     * Return true if the action represents the temporary cursor slot
     * @return boolean true if is the cursor
     */
    @Override
    public boolean isCursor(InventoryActionData action) {
        return (action.getSource().getContainerId() == ContainerId.UI && action.getSlot() == 0);
    }

    /**
     * Return true if action represents an output slot
     * @return boolean true if an output slot
     */
    @Override
    public boolean isOutput(InventoryActionData action) {
        return false;
    }


    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        Transaction transaction = Transaction.of(session, BaseInventoryTranslator.this, inventory);
        transaction.add(new Execute(() -> {
            List<ActionData> actionDataList = new ArrayList<>();
            ActionData cursor = null;

            for (InventoryActionData action : actions) {
                ActionData actionData = new ActionData(BaseInventoryTranslator.this, action);

                if (isCursor(action)) {
                    cursor = actionData;
                }
                actionDataList.add(actionData);
            }

            if (cursor == null) {
                // Create a fake cursor action based upon current known cursor
                ItemStack playerCursor = session.getInventory().getCursor();
                if (playerCursor != null) {
                    cursor = new ActionData(BaseInventoryTranslator.this, new InventoryActionData(
                            InventorySource.fromContainerWindowId(124),
                            -1,
                            ItemTranslator.translateToBedrock(session, playerCursor),
                            ItemTranslator.translateToBedrock(session, playerCursor)
                    ));
                } else {
                    cursor = new ActionData(BaseInventoryTranslator.this, new InventoryActionData(
                            InventorySource.fromContainerWindowId(124),
                            -1,
                            ItemData.AIR,
                            ItemData.AIR
                    ));
                }
                actionDataList.add(cursor);
            }

            while (actionDataList.size() > 0) {
                ActionData a1 = actionDataList.remove(0);

                for (ActionData a2 : actionDataList) {

                    // Check if a1 is already fulfilled
                    if (a1.isResolved() || a2.isResolved()) {
                        continue;
                    }

                    // Directions have to be opposite or equal
                    if ((a1.currentCount > a1.toCount && a2.currentCount > a2.toCount)
                            || (a1.currentCount < a1.toCount && a2.currentCount < a2.toCount)) {
                        continue;
                    }

                    // Work out direction
                    ActionData from;
                    ActionData to;
                    if (a1.currentCount > a1.toCount) {
                        from = a1;
                        to = a2;
                    } else {
                        from = a2;
                        to = a1;
                    }

                    // Process
                    processAction(transaction, cursor, from, to);
                }

                // Log unresolved for the moment
                if (a1.remaining() > 0) {
                    GeyserConnector.getInstance().getLogger().warning("Inventory Items Unresolved: " + a1);
                    transaction.add(new Refresh());
                }
            }
        }));

        Transaction.execute();
    }

    protected void processAction(Transaction transaction, ActionData cursor, ActionData from, ActionData to) {
        // Dropping to the world?
        if (to.action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {

            // Is it dropped without a window?
            if (transaction.getSession().getInventoryCache().getOpenInventory() == null
                    && from.action.getSource().getContainerId() == ContainerId.INVENTORY
                    && from.action.getSlot() == transaction.getSession().getInventory().getHeldItemSlot()) {

                // Dropping everything?
                if (from.toCount == 0 && from.currentCount <= to.remaining()) {
                    to.currentCount = from.currentCount;
                    from.currentCount =0;
                    transaction.add(new Drop(Drop.Type.DROP_STACK_HOTBAR, from.javaSlot));
                } else {
                    while (from.remaining() > 0 && to.remaining() > 0) {
                        to.currentCount++;
                        from.currentCount--;
                        transaction.add(new Drop(Drop.Type.DROP_ITEM_HOTBAR, from.javaSlot));
                    }
                }
            } else {

                // Dropping everything?
                if (from.toCount == 0 && from.currentCount <= to.remaining()) {
                    to.currentCount += from.currentCount;
                    from.currentCount = 0;
                    transaction.add(new Drop(Drop.Type.DROP_STACK, from.javaSlot));
                } else {
                    while (from.remaining() > 0 && to.remaining() > 0) {
                        to.currentCount++;
                        from.currentCount--;
                        transaction.add(new Drop(Drop.Type.DROP_ITEM, from.javaSlot));
                    }
                }
            }
            return;
        }

        // Can we swap the contents of to and from? Only applicable if either is the cursor or the cursor is empty
        if ((cursor.currentCount == 0 || cursor == from  || cursor == to)
            && (from.getCurrentItem().equals(to.getToItem())
                    && to.getCurrentItem().equals(from.getToItem())
                    && from.currentCount == to.toCount
                    && !from.getCurrentItem().equals(to.getCurrentItem()))) {

            if (from != cursor && to != cursor) {
                transaction.add(new Click(Click.Type.LEFT, from.javaSlot));
                transaction.add(new Click(Click.Type.LEFT, to.javaSlot));
                if (to.currentCount != 0) {
                    transaction.add(new Click(Click.Type.LEFT, from.javaSlot));
                }
            } else {
                transaction.add(new Click(Click.Type.LEFT, from == cursor ? to.javaSlot : from.javaSlot));
            }

            int currentCount = from.currentCount;
            ItemData currentItem = from.getCurrentItem();

            from.currentCount = to.currentCount;
            from.currentItem = to.getCurrentItem();
            to.currentCount = currentCount;
            to.currentItem = currentItem;
            return;
        }

        // Incompatible Items?
        if (!from.getCurrentItem().equals(to.getCurrentItem())
                && !from.getCurrentItem().equals(ItemData.AIR)
                && !to.getCurrentItem().equals(ItemData.AIR)) {
            return;
        }

        // Can we drop anything from cursor onto to?
        if (cursor != to && to.remaining() > 0 && cursor.currentCount > 0 && cursor.getCurrentItem().equals(to.getToItem())
                && (to.getCurrentItem().equals(ItemData.AIR) || to.getCurrentItem().equals(to.getToItem()))) {

            to.currentItem = cursor.getCurrentItem();
            while (cursor.currentCount > 0 && to.remaining() > 0) {
                transaction.add(new Click(Click.Type.RIGHT, to.javaSlot));
                cursor.currentCount--;
                to.currentCount++;
            }
        }

        // If from is not the cursor and the cursor is empty or is to we can pick up from from and drop onto to
        if (from != cursor && (cursor.currentCount == 0 || to == cursor)) {
            if (from.isResolved()) {
                return;
            }

            // If cursor is to and not empty we will have to use a spot slot if possible
            int spareSlot = -1;
            int spareCount = 0;
            if (to == cursor && cursor.currentCount > 0) {
                spareSlot = findTempSlot(transaction.getInventory(), transaction.getSession().getInventory().getCursor(), new ArrayList<>(), true);
                if (spareSlot == -1) {
                    // Failed, so we abort which will force a refresh if a mismatch occurs
                    return;
                }
                transaction.add(new Click(Click.Type.LEFT, spareSlot));
                spareCount = cursor.currentCount;
                cursor.currentCount = 0;
            }

            // Pick up everything
            transaction.add(new Click(Click.Type.LEFT, from.javaSlot));
            cursor.currentCount += from.currentCount;
            cursor.currentItem = from.getCurrentItem();
            from.currentCount = 0;

            // Drop what we don't need if not an output - NOTE This has the chance of leaking items to the cursor
            // due to the fact bedrock allows arbitrary pickup amounts.
            int leak = 0;
            while (from.remaining() > 0) {
                if (!isOutput(from.action)) {
                    transaction.add(new Click(Click.Type.RIGHT, from.javaSlot));
                    cursor.currentCount--;
                    from.currentCount++;
                } else {
                    leak++;
                    from.toCount--;
                }
            }

            // Drop onto to if not the cursor
            if (to != cursor) {
                to.currentItem = cursor.getCurrentItem();
                while (to.remaining() > 0 && cursor.currentCount > 0) {
                    transaction.add(new Click(Click.Type.RIGHT, to.javaSlot));
                    cursor.currentCount--;
                    to.currentCount++;
                }

                // If we have leaks we try drop everything else onto to
                if (leak > 0) {
                    transaction.add(new Click(Click.Type.LEFT, to.javaSlot));
                    to.toCount += leak;
                    to.currentCount += leak;
                    cursor.currentCount -= leak;
                    transaction.add(new Refresh());
                }
            }

            // Pick up spare if needed
            if (spareSlot != -1) {
                if (cursor.currentCount > 0) {
                    // place first
                    transaction.add(new Click(Click.Type.LEFT, spareSlot));
                }

                transaction.add(new Click(Click.Type.LEFT, spareSlot));
                cursor.currentCount += spareCount;
            }
        } else {
            // From is the cursor, so we can assume to is not
            if (to.isResolved()) {
                return;
            }

            // Can we drop everything onto to?
            if (cursor.toCount == 0 && cursor.remaining() > 0 && cursor.remaining() <= to.remaining()) {
                to.currentCount += cursor.currentCount;
                to.currentItem = cursor.getCurrentItem();
                cursor.currentCount = 0;

                transaction.add(new Click(Click.Type.LEFT, to.javaSlot));
            } else {
                // Drop what we need onto to
                to.currentItem = cursor.getCurrentItem();
                while (cursor.remaining() > 0 && to.remaining() > 0) {
                    cursor.currentCount--;
                    to.currentCount++;

                    transaction.add(new Click(Click.Type.RIGHT, to.javaSlot));
                }
            }
        }

        // Can we drop anything from cursor onto to?
        // @TODO: Is this needed still?
        if (cursor != to && to.remaining() > 0 && cursor.currentCount > 0 && cursor.getCurrentItem().equals(to.getToItem())
                && (to.getCurrentItem().equals(ItemData.AIR) || to.getCurrentItem().equals(to.getToItem()))) {

            GeyserConnector.getInstance().getLogger().warning("If no other errors above then the cursor drop is still needed.");

            to.currentItem = cursor.getCurrentItem();

            while (cursor.currentCount > 0 && to.remaining() > 0) {
                transaction.add(new Click(Click.Type.RIGHT, to.javaSlot));
                cursor.currentCount--;
                to.currentCount++;
            }
        }
    }

    private int findTempSlot(Inventory inventory, ItemStack item, List<Integer> slotBlacklist, boolean emptyOnly) {
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

    @ToString
    public static class ActionData {
        @ToString.Exclude
        public final InventoryTranslator translator;
        @ToString.Exclude
        public final InventoryActionData action;

        @ToString.Exclude
        public ItemData currentItem;
        @ToString.Exclude
        public final ItemData toItem;

        public int currentCount;
        public int toCount;
        public final int javaSlot;

        public ActionData(InventoryTranslator translator, @NonNull InventoryActionData action) {
            this.translator = translator;
            this.action = action;

            this.toItem = ItemData.of(
                    action.getToItem().getId(),
                    action.getToItem().getDamage(),
                    0,
                    action.getToItem().getTag(),
                    action.getToItem().getCanPlace(),
                    action.getToItem().getCanBreak(),
                    action.getToItem().getBlockingTicks()
            );
            this.currentItem = ItemData.of(
                    action.getFromItem().getId(),
                    action.getFromItem().getDamage(),
                    0,
                    action.getFromItem().getTag(),
                    action.getFromItem().getCanPlace(),
                    action.getFromItem().getCanBreak(),
                    action.getFromItem().getBlockingTicks()
            );

            this.toCount = action.getToItem().getCount();
            this.currentCount = action.getFromItem().getCount();
            this.javaSlot = translator.bedrockSlotToJava(action);
        }

        public ItemData getCurrentItem() {
            return currentCount == 0 ? ItemData.AIR : currentItem;
        }

        public ItemData getToItem() {
            return toCount == 0 ? ItemData.AIR : toItem;
        }

        public int remaining() {
            return Math.abs(toCount - currentCount);
        }

        public boolean isResolved() {
            return remaining() == 0 && getCurrentItem().equals(getToItem());
        }

    }
}
