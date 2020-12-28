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
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.*;
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AllArgsConstructor;
import org.geysermc.connector.inventory.CartographyContainer;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.click.Click;
import org.geysermc.connector.network.translators.inventory.click.ClickPlan;
import org.geysermc.connector.network.translators.inventory.translators.*;
import org.geysermc.connector.network.translators.inventory.translators.chest.DoubleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.chest.SingleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.furnace.BlastFurnaceInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.furnace.FurnaceInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.furnace.SmokerInventoryTranslator;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.*;

@AllArgsConstructor
public abstract class InventoryTranslator {

    public static final InventoryTranslator PLAYER_INVENTORY_TRANSLATOR = new PlayerInventoryTranslator();
    public static final Map<WindowType, InventoryTranslator> INVENTORY_TRANSLATORS = new HashMap<WindowType, InventoryTranslator>() {
        {
            /* Player Inventory */
            put(null, PLAYER_INVENTORY_TRANSLATOR);

            /* Chest UIs */
            put(WindowType.GENERIC_9X1, new SingleChestInventoryTranslator(9));
            put(WindowType.GENERIC_9X2, new SingleChestInventoryTranslator(18));
            put(WindowType.GENERIC_9X3, new SingleChestInventoryTranslator(27));
            put(WindowType.GENERIC_9X4, new DoubleChestInventoryTranslator(36));
            put(WindowType.GENERIC_9X5, new DoubleChestInventoryTranslator(45));
            put(WindowType.GENERIC_9X6, new DoubleChestInventoryTranslator(54));

            /* Furnaces */
            put(WindowType.FURNACE, new FurnaceInventoryTranslator());
            put(WindowType.BLAST_FURNACE, new BlastFurnaceInventoryTranslator());
            put(WindowType.SMOKER, new SmokerInventoryTranslator());

            /* Specific Inventories */
            put(WindowType.ANVIL, new AnvilInventoryTranslator());
            put(WindowType.BEACON, new BeaconInventoryTranslator());
            put(WindowType.BREWING_STAND, new BrewingInventoryTranslator());
            put(WindowType.CARTOGRAPHY, new CartographyInventoryTranslator());
            put(WindowType.CRAFTING, new CraftingInventoryTranslator());
            put(WindowType.ENCHANTMENT, new EnchantingInventoryTranslator());
            put(WindowType.GRINDSTONE, new GrindstoneInventoryTranslator());
            put(WindowType.LOOM, new LoomInventoryTranslator());
            put(WindowType.MERCHANT, new MerchantInventoryTranslator());
            put(WindowType.SHULKER_BOX, new ShulkerInventoryTranslator());
            put(WindowType.SMITHING, new SmithingInventoryTranslator());
            put(WindowType.STONECUTTER, new StonecutterInventoryTranslator());

            /* Generics */
            put(WindowType.GENERIC_3X3, new GenericBlockInventoryTranslator(9, "minecraft:dispenser[facing=north,triggered=false]", ContainerType.DISPENSER));
            put(WindowType.HOPPER, new GenericBlockInventoryTranslator(5, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER));

            /* Lectern */
            put(WindowType.LECTERN, new LecternInventoryTranslator());
        }
    };

    public static final int PLAYER_INVENTORY_SIZE = 36;
    public static final int PLAYER_INVENTORY_OFFSET = 9;
    private static final int MAX_ITEM_STACK_SIZE = 64;
    public final int size;

    public abstract void prepareInventory(GeyserSession session, Inventory inventory);
    public abstract void openInventory(GeyserSession session, Inventory inventory);
    public abstract void closeInventory(GeyserSession session, Inventory inventory);
    public abstract void updateProperty(GeyserSession session, Inventory inventory, int key, int value);
    public abstract void updateInventory(GeyserSession session, Inventory inventory);
    public abstract void updateSlot(GeyserSession session, Inventory inventory, int slot);
    public abstract int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData);
    public abstract int javaSlotToBedrock(int javaSlot); //TODO
    public abstract BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot); //TODO
    public abstract SlotType getSlotType(int javaSlot);
    public abstract Inventory createInventory(String name, int windowId, WindowType windowType, PlayerInventory playerInventory);

    /**
     * Should be overwritten in cases where specific inventories should reject an item being in a specific spot.
     * For examples, looms use this to reject items that are dyes in Bedrock but not in Java.
     *
     * The source/destination slot will be -1 if the cursor is the slot
     *
     * @return true if this transfer should be rejected
     */
    public boolean shouldRejectItemPlace(GeyserSession session, Inventory inventory, int javaSourceSlot, int javaDestinationSlot) {
        return false;
    }

    /**
     * Should be overrided if this request matches a certain criteria and shouldn't be treated normally.
     * E.G. anvil renaming or enchanting
     */
    public boolean shouldHandleRequestFirst(StackRequestActionData action, Inventory inventory) {
        return false;
    }

    /**
     * If {@link #shouldHandleRequestFirst(StackRequestActionData, Inventory)} returns true, this will be called
     */
    public ItemStackResponsePacket.Response translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        return null;
    }

    public void translateRequests(GeyserSession session, Inventory inventory, List<ItemStackRequestPacket.Request> requests) {
        ItemStackResponsePacket responsePacket = new ItemStackResponsePacket();
        for (ItemStackRequestPacket.Request request : requests) {
            if (request.getActions().length > 0) {
                StackRequestActionData firstAction = request.getActions()[0];
                if (shouldHandleRequestFirst(firstAction, inventory)) {
                    // Some special request that shouldn't be processed normally
                    responsePacket.getEntries().add(translateSpecialRequest(session, inventory, request));
                } else if (firstAction.getType() == StackRequestActionType.CRAFT_RECIPE || firstAction.getType() == StackRequestActionType.CRAFT_RECIPE_AUTO) {
                    responsePacket.getEntries().add(translateCraftingRequest(session, inventory, request));
                } else if (firstAction.getType() == StackRequestActionType.CRAFT_CREATIVE) {
                    // This is also used for pulling items out of creative
                    responsePacket.getEntries().add(translateCreativeRequest(session, inventory, request));
                } else {
                    responsePacket.getEntries().add(translateRequest(session, inventory, request));
                }
            } else {
                responsePacket.getEntries().add(rejectRequest(request));
            }
        }
        session.sendUpstreamPacket(responsePacket);
        System.out.println(responsePacket);
    }

    public ItemStackResponsePacket.Response translateRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        System.out.println(request);
        ClickPlan plan = new ClickPlan(session, this, inventory);
        IntSet affectedSlots = new IntOpenHashSet();
        for (StackRequestActionData action : request.getActions()) {
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();
            switch (action.getType()) {
                case TAKE:
                case PLACE: {
                    TransferStackRequestActionData transferAction = (TransferStackRequestActionData) action;
                    if (!(checkNetId(session, inventory, transferAction.getSource()) && checkNetId(session, inventory, transferAction.getDestination()))) {
                        if (session.getGameMode().equals(GameMode.CREATIVE) && transferAction.getSource().getContainer() == ContainerSlotType.CRAFTING_INPUT &&
                                transferAction.getSource().getSlot() >= 28 && transferAction.getSource().getSlot() <= 31) {
                            return rejectRequest(request, false);
                        }
                        session.getConnector().getLogger().error("DEBUG: About to reject request.");
                        session.getConnector().getLogger().error("Source: " + transferAction.getSource().toString() + " Result: " + checkNetId(session, inventory, transferAction.getSource()));
                        session.getConnector().getLogger().error("Destination: " + transferAction.getDestination().toString() + " Result: " + checkNetId(session, inventory, transferAction.getDestination()));
                        return rejectRequest(request);
                    }

                    int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                    int destSlot = bedrockSlotToJava(transferAction.getDestination());

                    if (shouldRejectItemPlace(session, inventory, isCursor(transferAction.getSource()) ? -1 : sourceSlot,
                            isCursor(transferAction.getDestination()) ? -1 : destSlot)) {
                        // This item would not be here in Java
                        return rejectRequest(request, false);
                    }

                    if (isCursor(transferAction.getSource()) && isCursor(transferAction.getDestination())) { //???
                        return rejectRequest(request);
                    } else if (session.getGameMode().equals(GameMode.CREATIVE) && inventory instanceof PlayerInventory) { // TODO: does the Java server use this stuff all the time in creative?
                        // Creative acts a little differently because it just edits slots
                        boolean sourceIsCursor = isCursor(transferAction.getSource());
                        boolean destIsCursor = isCursor(transferAction.getDestination());

                        GeyserItemStack sourceItem = sourceIsCursor ? session.getPlayerInventory().getCursor() :
                                inventory.getItem(sourceSlot);
                        GeyserItemStack newItem = sourceItem.copy();
                        if (sourceIsCursor) {
                            GeyserItemStack destItem = inventory.getItem(destSlot);
                            if (destItem.getId() == sourceItem.getId()) {
                                // Combining items
                                int itemsLeftOver = destItem.getAmount() + transferAction.getCount();
                                if (itemsLeftOver > MAX_ITEM_STACK_SIZE) {
                                    // Items will remain in cursor because destination slot gets set to 64
                                    destItem.setAmount(MAX_ITEM_STACK_SIZE);
                                    sourceItem.setAmount(itemsLeftOver - MAX_ITEM_STACK_SIZE);
                                } else {
                                    // Cursor will be emptied
                                    destItem.setAmount(itemsLeftOver);
                                    session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY);
                                }
                                ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                        destSlot,
                                        destItem.getItemStack()
                                );
                                session.sendDownstreamPacket(creativeActionPacket);
                                affectedSlots.add(destSlot);
                                break;
                            }
                        } else {
                            // Delete the source since we're moving it
                            inventory.setItem(sourceSlot, GeyserItemStack.EMPTY);
                            ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                    sourceSlot,
                                    new ItemStack(0)
                            );
                            session.sendDownstreamPacket(creativeActionPacket);
                            affectedSlots.add(sourceSlot);
                        }
                        // Update the item count with however much the client took
                        newItem.setAmount(transferAction.getCount());
                        // Remove that amount from the existing item
                        sourceItem.setAmount(sourceItem.getAmount() - transferAction.getCount());
                        if (sourceItem.isEmpty()) {
                            // Item is basically deleted
                            if (sourceIsCursor) {
                                session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY);
                            } else {
                                inventory.setItem(sourceSlot, GeyserItemStack.EMPTY);
                            }
                        }
                        if (destIsCursor) {
                            session.getPlayerInventory().setCursor(newItem);
                        } else {
                            inventory.setItem(destSlot, newItem);
                        }
                        GeyserItemStack itemToUpdate = destIsCursor ? sourceItem : newItem;
                        // The Java server doesn't care about what's in the mouse in creative mode, so we just need to track
                        // which inventory slot the client modified
                        ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                destIsCursor ? sourceSlot : destSlot,
                                itemToUpdate.isEmpty() ? new ItemStack(0) : itemToUpdate.getItemStack()
                        );
                        session.sendDownstreamPacket(creativeActionPacket);
                        System.out.println(creativeActionPacket);

                        if (!sourceIsCursor) { // Cursor is always added for us as an affected slot
                            affectedSlots.add(sourceSlot);
                        }
                        if (!destIsCursor) {
                            affectedSlots.add(destSlot);
                        }

                    } else if (isCursor(transferAction.getSource())) { //releasing cursor
                        int sourceAmount = cursor.getAmount();
                        if (transferAction.getCount() == sourceAmount) { //release all
                            plan.add(Click.LEFT, destSlot);
                        } else { //release some
                            for (int i = 0; i < transferAction.getCount(); i++) {
                                plan.add(Click.RIGHT, destSlot);
                            }
                        }
                    } else if (isCursor(transferAction.getDestination())) { //picking up into cursor
                        GeyserItemStack sourceItem = plan.getItem(sourceSlot);
                        int sourceAmount = sourceItem.getAmount();
                        if (cursor.isEmpty()) { //picking up into empty cursor
                            if (transferAction.getCount() == sourceAmount) { //pickup all
                                plan.add(Click.LEFT, sourceSlot);
                            } else if (transferAction.getCount() == sourceAmount - (sourceAmount / 2)) { //larger half; simple right click
                                plan.add(Click.RIGHT, sourceSlot);
                            } else { //pickup some; not a simple right click
                                plan.add(Click.LEFT, sourceSlot); //first pickup all
                                for (int i = 0; i < sourceAmount - transferAction.getCount(); i++) {
                                    plan.add(Click.RIGHT, sourceSlot); //release extra items back into source slot
                                }
                            }
                        } else { //pickup into non-empty cursor
                            if (!InventoryUtils.canStack(cursor, plan.getItem(sourceSlot))) { //doesn't make sense, reject
                                return rejectRequest(request);
                            }
                            if (transferAction.getCount() != sourceAmount) { //TODO: handle partially picking up into non-empty cursor (temp slot)
                                return rejectRequest(request);
                            }
                            if (getSlotType(sourceSlot).equals(SlotType.NORMAL)) {
                                plan.add(Click.LEFT, sourceSlot); //release cursor onto source slot
                            }
                            plan.add(Click.LEFT, sourceSlot); //pickup combined cursor and source
                        }
                    } else { //transfer from one slot to another
                        if (!cursor.isEmpty()) { //TODO: handle slot transfer when cursor is already in use (temp slot)
                            return rejectRequest(request);
                        }
                        int sourceAmount = plan.getItem(sourceSlot).getAmount();
                        if (transferAction.getCount() == sourceAmount) { //transfer all
                            plan.add(Click.LEFT, sourceSlot); //pickup source
                            plan.add(Click.LEFT, destSlot); //let go of all items and done
                        } else { //transfer some
                            //try to transfer items with least clicks possible
                            int halfSource = sourceAmount / 2; //smaller half
                            int holding;
                            if (transferAction.getCount() <= halfSource) { //faster to take only half
                                plan.add(Click.RIGHT, sourceSlot);
                                holding = halfSource;
                            } else { //need all
                                plan.add(Click.LEFT, sourceSlot);
                                holding = sourceAmount;
                            }
                            if (transferAction.getCount() > holding / 2) { //faster to release extra items onto source or dest slot?
                                for (int i = 0; i < holding - transferAction.getCount(); i++) {
                                    plan.add(Click.RIGHT, sourceSlot); //prepare cursor
                                }
                                plan.add(Click.LEFT, destSlot); //release cursor onto dest slot
                            } else {
                                for (int i = 0; i < transferAction.getCount(); i++) {
                                    plan.add(Click.RIGHT, destSlot); //right click until transfer goal is met
                                }
                                plan.add(Click.LEFT, sourceSlot); //return extra items to source slot
                            }
                        }
                    }
                    break;
                }
                case SWAP: { //TODO
                    SwapStackRequestActionData swapAction = (SwapStackRequestActionData) action;
                    if (!(checkNetId(session, inventory, swapAction.getSource()) && checkNetId(session, inventory, swapAction.getDestination())))
                        return rejectRequest(request);

                    if (session.getGameMode().equals(GameMode.CREATIVE) && inventory instanceof PlayerInventory) {
                        int destSlot = bedrockSlotToJava(swapAction.getDestination());
                        GeyserItemStack oldSourceItem;
                        GeyserItemStack oldDestinationItem = inventory.getItem(destSlot);
                        if (isCursor(swapAction.getSource())) {
                            oldSourceItem = session.getPlayerInventory().getCursor();
                            session.getPlayerInventory().setCursor(oldDestinationItem);
                        } else {
                            int sourceSlot = bedrockSlotToJava(swapAction.getSource());
                            oldSourceItem = inventory.getItem(sourceSlot);
                            ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                    sourceSlot,
                                    oldDestinationItem.isEmpty() ? new ItemStack(0) : oldDestinationItem.getItemStack() // isEmpty check... just in case
                            );
                            System.out.println(creativeActionPacket);
                            session.sendDownstreamPacket(creativeActionPacket);
                            inventory.setItem(sourceSlot, oldDestinationItem);
                        }
                        if (isCursor(swapAction.getDestination())) {
                            session.getPlayerInventory().setCursor(oldSourceItem);
                        } else {
                            ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                    destSlot,
                                    oldSourceItem.isEmpty() ? new ItemStack(0) : oldSourceItem.getItemStack()
                            );
                            System.out.println(creativeActionPacket);
                            session.sendDownstreamPacket(creativeActionPacket);
                            inventory.setItem(destSlot, oldSourceItem);
                        }

                    } else if (isCursor(swapAction.getSource()) && isCursor(swapAction.getDestination())) { //???
                        return rejectRequest(request);
                    } else if (isCursor(swapAction.getSource())) { //swap cursor
                        int destSlot = bedrockSlotToJava(swapAction.getDestination());
                        if (InventoryUtils.canStack(cursor, plan.getItem(destSlot))) { //TODO: cannot simply swap if cursor stacks with slot (temp slot)
                            return rejectRequest(request);
                        }
                        plan.add(Click.LEFT, destSlot);
                    } else if (isCursor(swapAction.getDestination())) { //swap cursor
                        int sourceSlot = bedrockSlotToJava(swapAction.getSource());
                        if (InventoryUtils.canStack(cursor, plan.getItem(sourceSlot))) { //TODO
                            return rejectRequest(request);
                        }
                        plan.add(Click.LEFT, sourceSlot);
                    } else {
                        int sourceSlot = bedrockSlotToJava(swapAction.getSource());
                        int destSlot = bedrockSlotToJava(swapAction.getDestination());
                        if (!cursor.isEmpty()) { //TODO: (temp slot)
                            return rejectRequest(request);
                        }
                        if (sourceSlot == destSlot) { //doesn't make sense
                            return rejectRequest(request);
                        }
                        if (InventoryUtils.canStack(plan.getItem(sourceSlot), plan.getItem(destSlot))) { //TODO: (temp slot)
                            return rejectRequest(request);
                        }
                        plan.add(Click.LEFT, sourceSlot); //pickup source into cursor
                        plan.add(Click.LEFT, destSlot); //swap cursor with dest slot
                        plan.add(Click.LEFT, sourceSlot); //release cursor onto source
                    }
                    break;
                }
                case DROP: {
                    DropStackRequestActionData dropAction = (DropStackRequestActionData) action;
                    if (!checkNetId(session, inventory, dropAction.getSource()))
                        return rejectRequest(request);

                    if (isCursor(dropAction.getSource())) { //clicking outside of window
                        if (session.getGameMode() == GameMode.CREATIVE && inventory instanceof PlayerInventory) {
                            GeyserItemStack cursorItem = session.getPlayerInventory().getCursor();
                            GeyserItemStack droppingItem = cursorItem.copy();
                            // Subtract the cursor item by however much is being dropped
                            cursorItem.setAmount(cursorItem.getAmount() - dropAction.getCount());
                            if (cursorItem.isEmpty()) {
                                // Cursor item no longer exists
                                session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY);
                            }
                            droppingItem.setAmount(dropAction.getCount());
                            ClientCreativeInventoryActionPacket packet = new ClientCreativeInventoryActionPacket(
                                    Click.OUTSIDE_SLOT,
                                    droppingItem.getItemStack()
                            );
                            System.out.println(packet.toString());
                            session.sendDownstreamPacket(packet);
                        } else {
                            int sourceAmount = plan.getCursor().getAmount();
                            if (dropAction.getCount() == sourceAmount) { //drop all
                                plan.add(Click.LEFT_OUTSIDE, Click.OUTSIDE_SLOT);
                            } else { //drop some
                                for (int i = 0; i < dropAction.getCount(); i++) {
                                    plan.add(Click.RIGHT_OUTSIDE, Click.OUTSIDE_SLOT); //drop one until goal is met
                                }
                            }
                        }
                    } else { //dropping from inventory
                        int sourceSlot = bedrockSlotToJava(dropAction.getSource());
                        int sourceAmount = plan.getItem(sourceSlot).getAmount();
                        if (dropAction.getCount() == sourceAmount && sourceAmount > 1) { //dropping all? (prefer DROP_ONE if only one)
                            plan.add(Click.DROP_ALL, sourceSlot);
                        } else { //drop some
                            for (int i = 0; i < dropAction.getCount(); i++) {
                                plan.add(Click.DROP_ONE, sourceSlot); //drop one until goal is met
                            }
                        }
                    }
                    break;
                }
                case CRAFT_CREATIVE: {
                    CraftCreativeStackRequestActionData creativeAction = (CraftCreativeStackRequestActionData) action;
                    System.out.println(creativeAction.getCreativeItemNetworkId());
                    break;
                }
                case DESTROY: {
                    // Only called when a creative client wants to destroy an item... I think - Camotoy
                    //TODO there is a Count here we don't use
                    DestroyStackRequestActionData destroyAction = (DestroyStackRequestActionData) action;
                    if (!session.getGameMode().equals(GameMode.CREATIVE)) {
                        // If this happens, let's throw an error and figure out why.
                        return rejectRequest(request);
                    }
                    if (!isCursor(destroyAction.getSource())) {
                        // Item exists; let's remove it from the inventory
                        int javaSlot = bedrockSlotToJava(destroyAction.getSource());
                        ClientCreativeInventoryActionPacket destroyItemPacket = new ClientCreativeInventoryActionPacket(
                                javaSlot,
                                new ItemStack(0)
                        );
                        session.sendDownstreamPacket(destroyItemPacket);
                        System.out.println(destroyItemPacket);
                        inventory.setItem(javaSlot, GeyserItemStack.EMPTY);
                        affectedSlots.add(javaSlot);
                    } else {
                        // Just sync up the item on our end, since the server doesn't care what's in our cursor
                        session.getPlayerInventory().setCursor(GeyserItemStack.EMPTY);
                    }
                    break;
                }
                // The following three tend to be called for UI inventories
                case CONSUME: {
                    if (inventory instanceof CartographyContainer) {
                        // TODO add this for more inventories? Only seems to glitch out the cartography table, though.
                        ConsumeStackRequestActionData consumeData = (ConsumeStackRequestActionData) action;
                        int sourceSlot = bedrockSlotToJava(consumeData.getSource());
                        if (sourceSlot == 0 && inventory.getItem(1).isEmpty()) {
                            // Java doesn't allow an item to be renamed; this is why CARTOGRAPHY_ADDITIONAL could remain empty for Bedrock
                            // We check this during slot 0 since setting the inventory slots here messes up shouldRejectItemPlace
                            return rejectRequest(request, false);
                        }

                        GeyserItemStack item = inventory.getItem(sourceSlot);
                        item.setAmount(item.getAmount() - consumeData.getCount());
                        if (item.isEmpty()) {
                            inventory.setItem(sourceSlot, GeyserItemStack.EMPTY);
                        }
                        affectedSlots.add(sourceSlot);
                    }
                    break;
                }
                case CRAFT_NON_IMPLEMENTED_DEPRECATED: {
                    break;
                }
                case CRAFT_RESULTS_DEPRECATED: {
                    break;
                }
                case CRAFT_RECIPE_OPTIONAL: {
                    // Anvils and cartography tables will handle this
                    break;
                }
                default:
                    return rejectRequest(request);
            }
        }
        plan.execute(false);
        affectedSlots.addAll(plan.getAffectedSlots());
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }
    
    public ItemStackResponsePacket.Response translateCraftingRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        System.out.println(request);

        int recipeId = 0;
        int resultSize = 0;
        boolean autoCraft;
        CraftState craftState = CraftState.START;

        int leftover = 0;
        ClickPlan plan = new ClickPlan(session, this, inventory);
        for (StackRequestActionData action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_RECIPE: {
                    CraftRecipeStackRequestActionData craftAction = (CraftRecipeStackRequestActionData) action;
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;
                    recipeId = craftAction.getRecipeNetworkId();
                    //System.out.println(session.getCraftingRecipes().get(recipeId));
                    autoCraft = false;
                    break;
                }
//                case CRAFT_RECIPE_AUTO: {
//                    AutoCraftRecipeStackRequestActionData autoCraftAction = (AutoCraftRecipeStackRequestActionData) action;
//                    if (craftState != CraftState.START) {
//                        return rejectRequest(request);
//                    }
//                    craftState = CraftState.RECIPE_ID;
//                    recipeId = autoCraftAction.getRecipeNetworkId();
//                    Recipe recipe = session.getCraftingRecipes().get(recipeId);
//                    System.out.println(recipe);
//                    if (recipe == null) {
//                        return rejectRequest(request);
//                    }
////                    ClientPrepareCraftingGridPacket packet = new ClientPrepareCraftingGridPacket(session.getOpenInventory().getId(), recipe.getIdentifier(), true);
////                    session.sendDownstreamPacket(packet);
//                    autoCraft = true;
//                    //TODO: reject transaction if crafting grid is not clear
//                    break;
//                }
                case CRAFT_RESULTS_DEPRECATED: {
                    CraftResultsDeprecatedStackRequestActionData deprecatedCraftAction = (CraftResultsDeprecatedStackRequestActionData) action;
                    if (craftState != CraftState.RECIPE_ID) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.DEPRECATED;

                    if (deprecatedCraftAction.getResultItems().length != 1) {
                        return rejectRequest(request);
                    }
                    resultSize = deprecatedCraftAction.getResultItems()[0].getCount();
                    if (resultSize <= 0) {
                        return rejectRequest(request);
                    }
                    break;
                }
                case CONSUME: {
                    ConsumeStackRequestActionData consumeAction = (ConsumeStackRequestActionData) action;
                    if (craftState != CraftState.DEPRECATED && craftState != CraftState.INGREDIENTS) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.INGREDIENTS;
                    break;
                }
                case TAKE:
                case PLACE: {
                    TransferStackRequestActionData transferAction = (TransferStackRequestActionData) action;
                    if (craftState != CraftState.INGREDIENTS && craftState != CraftState.TRANSFER) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.TRANSFER;

                    if (transferAction.getSource().getContainer() != ContainerSlotType.CREATIVE_OUTPUT) {
                        return rejectRequest(request);
                    }
                    if (transferAction.getCount() <= 0) {
                        return rejectRequest(request);
                    }

                    int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                    if (isCursor(transferAction.getDestination())) {
                        plan.add(Click.LEFT, sourceSlot);
                        craftState = CraftState.DONE;
                    } else {
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
                        if (leftover != 0) {
                            if (transferAction.getCount() > leftover) {
                                return rejectRequest(request);
                            }
                            if (transferAction.getCount() == leftover) {
                                plan.add(Click.LEFT, destSlot);
                            } else {
                                for (int i = 0; i < transferAction.getCount(); i++) {
                                    plan.add(Click.RIGHT, destSlot);
                                }
                            }
                            leftover -= transferAction.getCount();
                            break;
                        }

                        int remainder = transferAction.getCount() % resultSize;
                        int timesToCraft = transferAction.getCount() / resultSize;
                        for (int i = 0; i < timesToCraft; i++) {
                            plan.add(Click.LEFT, sourceSlot);
                            plan.add(Click.LEFT, destSlot);
                        }
                        if (remainder > 0) {
                            plan.add(Click.LEFT, 0);
                            for (int i = 0; i < remainder; i++) {
                                plan.add(Click.RIGHT, destSlot);
                            }
                            leftover = resultSize - remainder;
                        }
                    }
                    break;
                }
                default:
                    return rejectRequest(request);
            }
        }
        plan.execute(false);
        Set<Integer> affectedSlots = plan.getAffectedSlots();
        affectedSlots.addAll(Arrays.asList(1, 2, 3, 4)); //TODO: crafting grid
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }

    public ItemStackResponsePacket.Response translateCreativeRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        int creativeId = 0;
        CraftState craftState = CraftState.START;
        for (StackRequestActionData action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_CREATIVE: {
                    CraftCreativeStackRequestActionData creativeAction = (CraftCreativeStackRequestActionData) action;
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;

                    creativeId = creativeAction.getCreativeItemNetworkId();
                    break;
                }
                case CRAFT_RESULTS_DEPRECATED: {
                    CraftResultsDeprecatedStackRequestActionData deprecatedCraftAction = (CraftResultsDeprecatedStackRequestActionData) action;
                    if (craftState != CraftState.RECIPE_ID) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.DEPRECATED;
                    break;
                }
                case TAKE:
                case PLACE: {
                    TransferStackRequestActionData transferAction = (TransferStackRequestActionData) action;
                    if (craftState != CraftState.DEPRECATED) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.TRANSFER;

                    if (transferAction.getSource().getContainer() != ContainerSlotType.CREATIVE_OUTPUT) {
                        return rejectRequest(request);
                    }
                    // Reference the creative items list we send to the client to know what it's asking of us
                    ItemData creativeItem = ItemRegistry.CREATIVE_ITEMS[creativeId - 1];
                    // Get the correct count
                    creativeItem = ItemData.of(creativeItem.getId(), creativeItem.getDamage(), transferAction.getCount(),  creativeItem.getTag());
                    ItemStack javaCreativeItem = ItemTranslator.translateToJava(creativeItem);

                    if (isCursor(transferAction.getDestination())) {
                        session.getPlayerInventory().setCursor(GeyserItemStack.from(javaCreativeItem, session.getNextItemNetId()));
                        return acceptRequest(request, Collections.singletonList(
                                new ItemStackResponsePacket.ContainerEntry(ContainerSlotType.CURSOR,
                                        Collections.singletonList(makeItemEntry(session, 0, session.getPlayerInventory().getCursor())))));
                    } else {
                        int javaSlot = bedrockSlotToJava(transferAction.getDestination());
                        GeyserItemStack existingItem = inventory.getItem(javaSlot);
                        if (existingItem.getId() == javaCreativeItem.getId()) {
                            // Adding more to an existing item
                            existingItem.setAmount(existingItem.getAmount() + transferAction.getCount());
                            javaCreativeItem = existingItem.getItemStack();
                        } else {
                            inventory.setItem(javaSlot, GeyserItemStack.from(javaCreativeItem, session.getNextItemNetId()));
                        }
                        ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                javaSlot,
                                javaCreativeItem
                        );
                        session.sendDownstreamPacket(creativeActionPacket);
                        System.out.println(creativeActionPacket);
                        Set<Integer> affectedSlots = Collections.singleton(javaSlot);
                        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
                    }
                }
                default:
                    return rejectRequest(request);
            }
        }
        return rejectRequest(request);
    }

    public static ItemStackResponsePacket.Response acceptRequest(ItemStackRequestPacket.Request request, List<ItemStackResponsePacket.ContainerEntry> containerEntries) {
        return new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.OK, request.getRequestId(), containerEntries);
    }

    public static ItemStackResponsePacket.Response rejectRequest(ItemStackRequestPacket.Request request) {
        return rejectRequest(request, true);
    }

    public static ItemStackResponsePacket.Response rejectRequest(ItemStackRequestPacket.Request request, boolean throwError) {
        if (throwError) {
            // Currently for debugging, but might be worth it to keep in the future if something goes terribly wrong.
            new Throwable("DEBUGGING: ItemStackRequest rejected").printStackTrace();
        }
        return new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, request.getRequestId(), Collections.emptyList());
    }

    public boolean checkNetId(GeyserSession session, Inventory inventory, StackRequestSlotInfoData slotInfoData) {
        if (slotInfoData.getStackNetworkId() < 0)
            return true;

        GeyserItemStack currentItem = isCursor(slotInfoData) ? session.getPlayerInventory().getCursor() : inventory.getItem(bedrockSlotToJava(slotInfoData));
        return currentItem.getNetId() == slotInfoData.getStackNetworkId();
    }

    public List<ItemStackResponsePacket.ContainerEntry> makeContainerEntries(GeyserSession session, Inventory inventory, Set<Integer> affectedSlots) {
        Map<ContainerSlotType, List<ItemStackResponsePacket.ItemEntry>> containerMap = new HashMap<>();
        for (int slot : affectedSlots) {
            BedrockContainerSlot bedrockSlot = javaSlotToBedrockContainer(slot);
            List<ItemStackResponsePacket.ItemEntry> list = containerMap.computeIfAbsent(bedrockSlot.getContainer(), k -> new ArrayList<>());
            list.add(makeItemEntry(session, bedrockSlot.getSlot(), inventory.getItem(slot)));
        }

        List<ItemStackResponsePacket.ContainerEntry> containerEntries = new ArrayList<>();
        for (Map.Entry<ContainerSlotType, List<ItemStackResponsePacket.ItemEntry>> entry : containerMap.entrySet()) {
            containerEntries.add(new ItemStackResponsePacket.ContainerEntry(entry.getKey(), entry.getValue()));
        }

        ItemStackResponsePacket.ItemEntry cursorEntry = makeItemEntry(session, 0, session.getPlayerInventory().getCursor());
        containerEntries.add(new ItemStackResponsePacket.ContainerEntry(ContainerSlotType.CURSOR, Collections.singletonList(cursorEntry)));

        return containerEntries;
    }

    public static ItemStackResponsePacket.ItemEntry makeItemEntry(GeyserSession session, int bedrockSlot, GeyserItemStack itemStack) {
        ItemStackResponsePacket.ItemEntry itemEntry;
        if (!itemStack.isEmpty()) {
            int newNetId = session.getNextItemNetId();
            itemStack.setNetId(newNetId);
            itemEntry = new ItemStackResponsePacket.ItemEntry((byte) bedrockSlot, (byte) bedrockSlot, (byte) itemStack.getAmount(), newNetId, "");
        } else {
            itemEntry = new ItemStackResponsePacket.ItemEntry((byte) bedrockSlot, (byte) bedrockSlot, (byte) 0, 0, "");
        }
        return itemEntry;
    }

    private static boolean isCursor(StackRequestSlotInfoData slotInfoData) {
        return slotInfoData.getContainer() == ContainerSlotType.CURSOR;
    }

    private enum CraftState {
        START,
        RECIPE_ID,
        DEPRECATED,
        INGREDIENTS,
        TRANSFER,
        DONE
    }
}
