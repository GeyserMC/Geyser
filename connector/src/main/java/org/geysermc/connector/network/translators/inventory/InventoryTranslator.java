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
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.*;
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.AllArgsConstructor;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.click.Click;
import org.geysermc.connector.network.translators.inventory.click.ClickPlan;
import org.geysermc.connector.network.translators.inventory.translators.CraftingInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.MerchantInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.PlayerInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.chest.DoubleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.chest.SingleChestInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.furnace.BlastFurnaceInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.furnace.FurnaceInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.translators.furnace.SmokerInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.*;

@AllArgsConstructor
public abstract class InventoryTranslator {

    public static final Map<WindowType, InventoryTranslator> INVENTORY_TRANSLATORS = new HashMap<WindowType, InventoryTranslator>() {
        {
            put(null, new PlayerInventoryTranslator()); //player inventory
            put(WindowType.GENERIC_9X1, new SingleChestInventoryTranslator(9));
            put(WindowType.GENERIC_9X2, new SingleChestInventoryTranslator(18));
            put(WindowType.GENERIC_9X3, new SingleChestInventoryTranslator(27));
            put(WindowType.GENERIC_9X4, new DoubleChestInventoryTranslator(36));
            put(WindowType.GENERIC_9X5, new DoubleChestInventoryTranslator(45));
            put(WindowType.GENERIC_9X6, new DoubleChestInventoryTranslator(54));
            put(WindowType.CRAFTING, new CraftingInventoryTranslator());
            /*put(WindowType.BREWING_STAND, new BrewingInventoryTranslator());
            put(WindowType.ANVIL, new AnvilInventoryTranslator());
            put(WindowType.GRINDSTONE, new GrindstoneInventoryTranslator());*/
            put(WindowType.MERCHANT, new MerchantInventoryTranslator());
            //put(WindowType.SMITHING, new SmithingInventoryTranslator());
            //put(WindowType.ENCHANTMENT, new EnchantmentInventoryTranslator()); //TODO

            put(WindowType.FURNACE, new FurnaceInventoryTranslator());
            put(WindowType.BLAST_FURNACE, new BlastFurnaceInventoryTranslator());
            put(WindowType.SMOKER, new SmokerInventoryTranslator());

            InventoryUpdater containerUpdater = new ContainerInventoryUpdater();
            //put(WindowType.GENERIC_3X3, new AbstractBlockInventoryTranslator(9, "minecraft:dispenser[facing=north,triggered=false]", ContainerType.DISPENSER, containerUpdater));
            //put(WindowType.HOPPER, new AbstractBlockInventoryTranslator(5, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER, containerUpdater));
            //put(WindowType.SHULKER_BOX, new AbstractBlockInventoryTranslator(27, "minecraft:shulker_box[facing=north]", ContainerType.CONTAINER, containerUpdater));
            //put(WindowType.BEACON, new AbstractBlockInventoryTranslator(1, "minecraft:beacon", ContainerType.BEACON)); //TODO*/
        }
    };

    public static final int PLAYER_INVENTORY_SIZE = 36;
    public static final int PLAYER_INVENTORY_OFFSET = 9;
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

    public void translateRequests(GeyserSession session, Inventory inventory, List<ItemStackRequestPacket.Request> requests) {
        ItemStackResponsePacket responsePacket = new ItemStackResponsePacket();
        for (ItemStackRequestPacket.Request request : requests) {
            if (request.getActions().length > 0) {
                StackRequestActionData firstAction = request.getActions()[0];
                if (firstAction.getType() == StackRequestActionType.CRAFT_RECIPE || firstAction.getType() == StackRequestActionType.CRAFT_RECIPE_AUTO) {
                    responsePacket.getEntries().add(translateCraftingRequest(session, inventory, request));
                } else if (firstAction.getType() == StackRequestActionType.CRAFT_CREATIVE) {
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
        for (StackRequestActionData action : request.getActions()) {
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();
            switch (action.getType()) {
                case TAKE:
                case PLACE: {
                    TransferStackRequestActionData transferAction = (TransferStackRequestActionData) action;
                    if (!(checkNetId(session, inventory, transferAction.getSource()) && checkNetId(session, inventory, transferAction.getDestination())))
                        return rejectRequest(request);

                    if (isCursor(transferAction.getSource()) && isCursor(transferAction.getDestination())) { //???
                        return rejectRequest(request);
                    } else if (isCursor(transferAction.getSource())) { //releasing cursor
                        int sourceAmount = cursor.getAmount();
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
                        if (transferAction.getCount() == sourceAmount) { //release all
                            plan.add(Click.LEFT, destSlot);
                        } else { //release some
                            for (int i = 0; i < transferAction.getCount(); i++) {
                                plan.add(Click.RIGHT, destSlot);
                            }
                        }
                    } else if (isCursor(transferAction.getDestination())) { //picking up into cursor
                        int sourceSlot = bedrockSlotToJava(transferAction.getSource());
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
                            plan.add(Click.LEFT, sourceSlot); //release cursor onto source slot
                            plan.add(Click.LEFT, sourceSlot); //pickup combined cursor and source
                        }
                    } else { //transfer from one slot to another
                        if (!cursor.isEmpty()) { //TODO: handle slot transfer when cursor is already in use (temp slot)
                            return rejectRequest(request);
                        }
                        int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                        int sourceAmount = plan.getItem(sourceSlot).getAmount();
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
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

                    if (isCursor(swapAction.getSource()) && isCursor(swapAction.getDestination())) { //???
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
                        int sourceAmount = plan.getCursor().getAmount();
                        if (dropAction.getCount() == sourceAmount) { //drop all
                            plan.add(Click.LEFT_OUTSIDE, Click.OUTSIDE_SLOT);
                        } else { //drop some
                            for (int i = 0; i < dropAction.getCount(); i++) {
                                plan.add(Click.RIGHT_OUTSIDE, Click.OUTSIDE_SLOT); //drop one until goal is met
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
                }
                default:
                    return rejectRequest(request);
            }
        }
        plan.execute(false);
        return acceptRequest(request, makeContainerEntries(session, inventory, plan.getAffectedSlots()));
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
                    if (isCursor(transferAction.getDestination())) {
                        session.getPlayerInventory().setCursor(GeyserItemStack.from(ItemTranslator.translateToJava(ItemRegistry.CREATIVE_ITEMS[creativeId]), session.getItemNetId().getAndIncrement())); //TODO
                        return acceptRequest(request, makeContainerEntries(session, inventory, Collections.emptySet()));
                    } else {
                        int javaSlot = bedrockSlotToJava(transferAction.getDestination());
                        ItemStack javaItem = ItemTranslator.translateToJava(ItemRegistry.CREATIVE_ITEMS[creativeId - 1]); //TODO
                        inventory.setItem(javaSlot, GeyserItemStack.from(javaItem, session.getItemNetId().getAndIncrement()));
                        ClientCreativeInventoryActionPacket creativeActionPacket = new ClientCreativeInventoryActionPacket(
                                javaSlot,
                                javaItem
                        );
                        session.sendDownstreamPacket(creativeActionPacket);
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
        new Throwable("DEBUGGING: ItemStackRequest rejected").printStackTrace(); //TODO: temporary debugging
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
            int newNetId = session.getItemNetId().getAndIncrement();
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
