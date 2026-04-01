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

package org.geysermc.geyser.translator.inventory;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.AutoCraftRecipeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ConsumeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftResultsDeprecatedAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackResponsePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.CartographyContainer;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.inventory.click.Click;
import org.geysermc.geyser.inventory.click.ClickPlan;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.FakeHeadProvider;
import org.geysermc.geyser.translator.inventory.chest.DoubleChestInventoryTranslator;
import org.geysermc.geyser.translator.inventory.chest.SingleChestInventoryTranslator;
import org.geysermc.geyser.translator.inventory.furnace.BlastFurnaceInventoryTranslator;
import org.geysermc.geyser.translator.inventory.furnace.FurnaceInventoryTranslator;
import org.geysermc.geyser.translator.inventory.furnace.SmokerInventoryTranslator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.ItemUtils;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.geysermc.geyser.translator.inventory.BundleInventoryTranslator.isBundle;

@AllArgsConstructor
public abstract class InventoryTranslator<Type extends Inventory> {

    public static final InventoryTranslator<PlayerInventory> PLAYER_INVENTORY_TRANSLATOR = new PlayerInventoryTranslator();
    private static final Map<ContainerType, InventoryTranslator<? extends Inventory>> INVENTORY_TRANSLATORS = new EnumMap<>(ContainerType.class) {
        {
            /* Chest UIs */
            put(ContainerType.GENERIC_9X1, new SingleChestInventoryTranslator(9));
            put(ContainerType.GENERIC_9X2, new SingleChestInventoryTranslator(18));
            put(ContainerType.GENERIC_9X3, new SingleChestInventoryTranslator(27));
            put(ContainerType.GENERIC_9X4, new DoubleChestInventoryTranslator(36));
            put(ContainerType.GENERIC_9X5, new DoubleChestInventoryTranslator(45));
            put(ContainerType.GENERIC_9X6, new DoubleChestInventoryTranslator(54));

            /* Furnaces */
            put(ContainerType.FURNACE, new FurnaceInventoryTranslator());
            put(ContainerType.BLAST_FURNACE, new BlastFurnaceInventoryTranslator());
            put(ContainerType.SMOKER, new SmokerInventoryTranslator());

            /* Specific Inventories */
            put(ContainerType.ANVIL, new AnvilInventoryTranslator());
            put(ContainerType.BEACON, new BeaconInventoryTranslator());
            put(ContainerType.BREWING_STAND, new BrewingInventoryTranslator());
            put(ContainerType.CARTOGRAPHY, new CartographyInventoryTranslator());
            put(ContainerType.CRAFTER_3x3, new CrafterInventoryTranslator());
            put(ContainerType.CRAFTING, new CraftingInventoryTranslator());
            put(ContainerType.ENCHANTMENT, new EnchantingInventoryTranslator());
            put(ContainerType.HOPPER, new HopperInventoryTranslator());
            put(ContainerType.GENERIC_3X3, new Generic3X3InventoryTranslator());
            put(ContainerType.GRINDSTONE, new GrindstoneInventoryTranslator());
            put(ContainerType.LOOM, new LoomInventoryTranslator());
            put(ContainerType.MERCHANT, new MerchantInventoryTranslator());
            put(ContainerType.SHULKER_BOX, new ShulkerInventoryTranslator());
            put(ContainerType.SMITHING, new SmithingInventoryTranslator());
            put(ContainerType.STONECUTTER, new StonecutterInventoryTranslator());

            /* Lectern */
            put(ContainerType.LECTERN, new LecternInventoryTranslator());
        }
    };

    public static final int PLAYER_INVENTORY_SIZE = 36;
    public static final int PLAYER_INVENTORY_OFFSET = 9;

    public final int size;

    
    public boolean requiresOpeningDelay(GeyserSession session, Type inventory) {
        return false;
    }

    
    public boolean canReuseInventory(GeyserSession session, @NonNull Inventory inventory, @NonNull Inventory previous) {
        
        if (inventory.getContainerType() == null || previous.getContainerType() == null ||
            !Objects.equals(inventory.getContainerType(), previous.getContainerType()) ||
            !Objects.equals(inventory.getTitle(), previous.getTitle()) ||
            inventory.getSize() != previous.getSize()
        ) {
            return false;
        }

        
        return previous.getHolderId() != -1 || previous.getHolderPosition() != Vector3i.ZERO;
    }

    
    public abstract boolean prepareInventory(GeyserSession session, Type inventory);

    
    public abstract void openInventory(GeyserSession session, Type inventory);

    
    public abstract void closeInventory(GeyserSession session, Type inventory, boolean force);

    
    public abstract void updateProperty(GeyserSession session, Type inventory, int key, int value);

    
    public abstract void updateInventory(GeyserSession session, Type inventory);

    
    public abstract void updateSlot(GeyserSession session, Type inventory, int slot);

    
    public abstract int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData);

    
    public abstract int javaSlotToBedrock(int javaSlot);

    /**
     * Converts a Java slot to the corresponding Bedrock container and slot
     */
    public abstract BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot, Type inventory);

    /**
     * Returns the slot type for a Java slot id
     */
    public abstract SlotType getSlotType(int javaSlot);

    /**
     * Creates a new inventory.
     */
    public abstract Type createInventory(GeyserSession session, String name, int windowId, ContainerType containerType);

    /**
     * Used for crafting-related transactions. Will override in PlayerInventoryTranslator and CraftingInventoryTranslator.
     */
    public int getGridSize() {
        return -1;
    }

    /**
     * Should be overwritten in cases where specific inventories should reject an item being in a specific spot.
     * For examples, looms use this to reject items that are dyes in Bedrock but not in Java.
     * <p>
     * The source/destination slot will be -1 if the cursor is the slot
     *
     * @return true if this transfer should be rejected
     */
    protected boolean shouldRejectItemPlace(GeyserSession session, Type inventory, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        return false;
    }

    /**
     * Should be overrided if this request matches a certain criteria and shouldn't be treated normally.
     * E.G. anvil renaming or enchanting
     */
    protected boolean shouldHandleRequestFirst(ItemStackRequestAction action, Type inventory) {
        return false;
    }

    /**
     * If {@link #shouldHandleRequestFirst(ItemStackRequestAction, Inventory)} returns true, this will be called
     */
    protected ItemStackResponse translateSpecialRequest(GeyserSession session, Type inventory, ItemStackRequest request) {
        return rejectRequest(request);
    }

    public final void translateRequests(GeyserSession session, Type inventory, List<ItemStackRequest> requests) {
        boolean refresh = false;
        ItemStackResponsePacket responsePacket = new ItemStackResponsePacket();
        for (ItemStackRequest request : requests) {
            ItemStackResponse response;
            if (request.getActions().length > 0) {
                ItemStackRequestAction firstAction = request.getActions()[0];
                if (shouldHandleRequestFirst(firstAction, inventory)) {
                    
                    response = translateSpecialRequest(session, inventory, request);
                } else {
                    response = switch (firstAction.getType()) {
                        case CRAFT_RECIPE -> translateCraftingRequest(session, inventory, request);
                        case CRAFT_RECIPE_AUTO -> translateAutoCraftingRequest(session, inventory, request);
                        case CRAFT_CREATIVE ->
                                
                                translateCreativeRequest(session, inventory, request);
                        default -> translateRequest(session, inventory, request);
                    };
                }
            } else {
                response = rejectRequest(request);
            }

            if (response.getResult() != ItemStackResponseStatus.OK) {
                
                refresh = true;
            }

            responsePacket.getEntries().add(response);
        }
        session.sendUpstreamPacket(responsePacket);

        if (refresh) {
            InventoryUtils.updateCursor(session);
            updateInventory(session, inventory);
        }

        
        inventory.resetNextStateId();
    }

    public ItemStackResponse translateRequest(GeyserSession session, Type inventory, ItemStackRequest request) {
        ClickPlan plan = new ClickPlan(session, this, inventory);
        IntSet affectedSlots = new IntOpenHashSet();
        int pendingOutput = 0;
        int savedTempSlot = -1;

        for (ItemStackRequestAction action : request.getActions()) {
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();
            switch (action.getType()) {
                case TAKE:
                case PLACE: {
                    TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                    if (!(checkNetId(session, inventory, transferAction.getSource()) && checkNetId(session, inventory, transferAction.getDestination()))) {
                        if (session.getGeyser().config().debugMode()) {
                            session.getGeyser().getLogger().error("DEBUG: About to reject TAKE/PLACE request made by " + session.bedrockUsername());
                            dumpStackRequestDetails(session, inventory, transferAction.getSource(), transferAction.getDestination());
                        }
                        return rejectRequest(request);
                    }

                    
                    ItemStackResponse bundleResponse = BundleInventoryTranslator.handleBundle(session, this, inventory, request, false);
                    if (bundleResponse != null) {
                        
                        return bundleResponse;
                    }

                    int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                    int destSlot = bedrockSlotToJava(transferAction.getDestination());
                    boolean isSourceCursor = isCursor(transferAction.getSource());
                    boolean isDestCursor = isCursor(transferAction.getDestination());

                    if (this instanceof PlayerInventoryTranslator) {
                        if (destSlot == 5) {
                            
                            GeyserItemStack javaItem = inventory.getItem(sourceSlot);
                            if (javaItem.is(Items.PLAYER_HEAD) && javaItem.hasNonBaseComponents()) {
                                FakeHeadProvider.setHead(session, session.getPlayerEntity(), javaItem.getComponent(DataComponentTypes.PROFILE));
                            }
                        } else if (sourceSlot == 5) {
                            
                            FakeHeadProvider.restoreOriginalSkin(session, session.getPlayerEntity());
                        }
                    }

                    if (shouldRejectItemPlace(session, inventory, transferAction.getSource().getContainerName().getContainer(),
                            isSourceCursor ? -1 : sourceSlot,
                            transferAction.getDestination().getContainerName().getContainer(), isDestCursor ? -1 : destSlot)) {
                        
                        return rejectRequest(request, false);
                    }

                    
                    if (pendingOutput == 0 && !isSourceCursor && getSlotType(sourceSlot) == SlotType.OUTPUT
                        && transferAction.getCount() < plan.getItem(sourceSlot).getAmount()) {
                        
                        if (isDestCursor) {
                            return rejectRequest(request);
                        }

                        if (!plan.getCursor().isEmpty()) {
                            savedTempSlot = findTempSlot(plan, plan.getCursor(), true);
                            if (savedTempSlot == -1) {
                                return rejectRequest(request);
                            }
                            plan.add(Click.LEFT, savedTempSlot);
                        }

                        
                        pendingOutput = plan.getItem(sourceSlot).getAmount();
                        plan.add(Click.LEFT, sourceSlot);
                    }

                    
                    if (pendingOutput > 0) {
                        if (isSourceCursor || getSlotType(sourceSlot) != SlotType.OUTPUT
                            || transferAction.getCount() > pendingOutput
                            || destSlot == savedTempSlot
                            || isDestCursor) {
                            return rejectRequest(request);
                        }

                        
                        GeyserItemStack destItem = plan.getItem(destSlot);
                        if (!destItem.isEmpty() && !InventoryUtils.canStack(destItem, plan.getCursor())) {
                            return rejectRequest(request);
                        }

                        
                        if (pendingOutput == transferAction.getCount()) {
                            plan.add(Click.LEFT, destSlot);
                        } else {
                            for (int i = 0; i < transferAction.getCount(); i++) {
                                plan.add(Click.RIGHT, destSlot);
                            }
                        }

                        pendingOutput -= transferAction.getCount();
                        if (pendingOutput != plan.getCursor().getAmount()) {
                            return rejectRequest(request);
                        }

                        if (pendingOutput == 0 && savedTempSlot != -1) {
                            plan.add(Click.LEFT, savedTempSlot);
                            savedTempSlot = -1;
                        }

                        
                        continue;
                    }

                    if (isSourceCursor && isDestCursor) { //???
                        return rejectRequest(request);
                    } else if (isSourceCursor) { //releasing cursor
                        int sourceAmount = cursor.getAmount();
                        if (transferAction.getCount() == sourceAmount) { //release all
                            plan.add(Click.LEFT, destSlot);
                        } else { //release some
                            for (int i = 0; i < transferAction.getCount(); i++) {
                                plan.add(Click.RIGHT, destSlot);
                            }
                        }
                    } else if (isDestCursor) { //picking up into cursor
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
                            if (transferAction.getCount() != sourceAmount) {
                                int tempSlot = findTempSlot(plan, cursor, false, sourceSlot);
                                if (tempSlot == -1) {
                                    return rejectRequest(request);
                                }
                                plan.add(Click.LEFT, tempSlot); //place cursor into temp slot
                                plan.add(Click.LEFT, sourceSlot); //pickup source items into cursor
                                for (int i = 0; i < transferAction.getCount(); i++) {
                                    plan.add(Click.RIGHT, tempSlot); //partially transfer source items into temp slot (original cursor)
                                }
                                plan.add(Click.LEFT, sourceSlot); //return remaining source items
                                plan.add(Click.LEFT, tempSlot); //retrieve original cursor items from temp slot
                            } else {
                                if (getSlotType(sourceSlot).equals(SlotType.NORMAL)) {
                                    plan.add(Click.LEFT, sourceSlot); //release cursor onto source slot
                                }
                                plan.add(Click.LEFT, sourceSlot); //pickup combined cursor and source
                            }
                        }
                    } else { //transfer from one slot to another
                        int tempSlot = -1;
                        if (!plan.getCursor().isEmpty()) {
                            tempSlot = findTempSlot(plan, cursor, getSlotType(sourceSlot) != SlotType.NORMAL, sourceSlot, destSlot);
                            if (tempSlot == -1) {
                                return rejectRequest(request);
                            }
                            plan.add(Click.LEFT, tempSlot); //place cursor into temp slot
                        }

                        transferSlot(plan, sourceSlot, destSlot, transferAction.getCount());

                        if (tempSlot != -1) {
                            plan.add(Click.LEFT, tempSlot); //retrieve original cursor
                        }
                    }
                    break;
                }
                case SWAP: {
                    
                    SwapAction swapAction = (SwapAction) action;
                    ItemStackRequestSlotData source = swapAction.getSource();
                    ItemStackRequestSlotData destination = swapAction.getDestination();

                    if (!(checkNetId(session, inventory, source) && checkNetId(session, inventory, destination))) {
                        if (session.getGeyser().config().debugMode()) {
                            session.getGeyser().getLogger().error("DEBUG: About to reject SWAP request made by " + session.bedrockUsername());
                            dumpStackRequestDetails(session, inventory, source, destination);
                        }
                        return rejectRequest(request);
                    }

                    int sourceSlot = bedrockSlotToJava(source);
                    int destSlot = bedrockSlotToJava(destination);
                    boolean isSourceCursor = isCursor(source);
                    boolean isDestCursor = isCursor(destination);

                    if (shouldRejectItemPlace(session, inventory, source.getContainerName().getContainer(),
                            isSourceCursor ? -1 : sourceSlot,
                            destination.getContainerName().getContainer(), isDestCursor ? -1 : destSlot)) {
                        
                        return rejectRequest(request, false);
                    }

                    if (!isSourceCursor && destination.getContainerName().getContainer() == ContainerSlotType.HOTBAR || destination.getContainerName().getContainer() == ContainerSlotType.HOTBAR_AND_INVENTORY) {
                        
                        Click click = InventoryUtils.getClickForHotbarSwap(destination.getSlot());
                        if (click != null) {
                            plan.add(click, sourceSlot);
                            break;
                        }
                    }

                    
                    
                    
                    
                    

                    if (isSourceCursor && isDestCursor) { //???
                        return rejectRequest(request);
                    } else if (isSourceCursor) { //swap cursor
                        if (InventoryUtils.canStack(cursor, plan.getItem(destSlot))) { 
                            return rejectRequest(request);
                        }
                        plan.add(isBundle(plan, destSlot) || isBundle(cursor) ? Click.RIGHT : Click.LEFT, destSlot);
                    } else if (isDestCursor) { //swap cursor
                        if (InventoryUtils.canStack(cursor, plan.getItem(sourceSlot))) { 
                            return rejectRequest(request);
                        }
                        plan.add(isBundle(plan, sourceSlot) || isBundle(cursor) ? Click.RIGHT : Click.LEFT, sourceSlot);
                    } else {
                        if (!cursor.isEmpty()) { 
                            return rejectRequest(request);
                        }
                        if (sourceSlot == destSlot) { //doesn't make sense
                            return rejectRequest(request);
                        }
                        if (InventoryUtils.canStack(plan.getItem(sourceSlot), plan.getItem(destSlot))) { 
                            return rejectRequest(request);
                        }
                        plan.add(Click.LEFT, sourceSlot); //pickup source into cursor
                        plan.add(isBundle(plan, sourceSlot) || isBundle(plan, destSlot) ? Click.RIGHT : Click.LEFT, destSlot); //swap cursor with dest slot
                        plan.add(Click.LEFT, sourceSlot); //release cursor onto source
                    }
                    break;
                }
                case DROP: {
                    DropAction dropAction = (DropAction) action;
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
                case CONSUME: { 
                    if (inventory instanceof CartographyContainer) {
                        
                        ConsumeAction consumeData = (ConsumeAction) action;

                        int sourceSlot = bedrockSlotToJava(consumeData.getSource());
                        if ((sourceSlot == 0 && inventory.getItem(1).isEmpty()) || (sourceSlot == 1 && inventory.getItem(0).isEmpty())) {
                            
                            
                            return rejectRequest(request, false);
                        }

                        if (sourceSlot == 1) {
                            
                            
                            GeyserItemStack item = inventory.getItem(sourceSlot);
                            item.setAmount(item.getAmount() - consumeData.getCount());
                            if (item.isEmpty()) {
                                inventory.setItem(sourceSlot, GeyserItemStack.EMPTY, session);
                            }

                            GeyserItemStack itemZero = inventory.getItem(0);
                            itemZero.setAmount(itemZero.getAmount() - consumeData.getCount());
                            if (itemZero.isEmpty()) {
                                inventory.setItem(0, GeyserItemStack.EMPTY, session);
                            }
                        }
                        affectedSlots.add(sourceSlot);
                    }
                    break;
                }
                case CRAFT_RECIPE: 
                case CRAFT_RECIPE_AUTO: 
                case CRAFT_NON_IMPLEMENTED_DEPRECATED: 
                case CRAFT_RESULTS_DEPRECATED: 
                case CRAFT_RECIPE_OPTIONAL: 
                case CRAFT_LOOM: 
                case CRAFT_REPAIR_AND_DISENCHANT: 
                case MINE_BLOCK: { 
                    break;
                }
                default:
                    return rejectRequest(request);
            }
        }

        if (pendingOutput != 0) {
            return rejectRequest(request);
        }

        plan.execute(false);
        affectedSlots.addAll(plan.getAffectedSlots());
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }
    
    public ItemStackResponse translateCraftingRequest(GeyserSession session, Type inventory, ItemStackRequest request) {
        int resultSize = 0;
        int timesCrafted;
        CraftState craftState = CraftState.START;

        int leftover = 0;
        ClickPlan plan = new ClickPlan(session, this, inventory);
        
        IntSet affectedSlots = new IntOpenHashSet();
        for (ItemStackRequestAction action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_RECIPE: {
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;
                    break;
                }
                case CRAFT_RESULTS_DEPRECATED: {
                    CraftResultsDeprecatedAction deprecatedCraftAction = (CraftResultsDeprecatedAction) action;
                    if (craftState != CraftState.RECIPE_ID) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.DEPRECATED;

                    if (deprecatedCraftAction.getResultItems().length != 1) {
                        return rejectRequest(request);
                    }
                    resultSize = deprecatedCraftAction.getResultItems()[0].getCount();
                    timesCrafted = deprecatedCraftAction.getTimesCrafted();
                    if (resultSize <= 0 || timesCrafted <= 0) {
                        return rejectRequest(request);
                    }
                    break;
                }
                case CONSUME: {
                    if (craftState != CraftState.DEPRECATED && craftState != CraftState.INGREDIENTS) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.INGREDIENTS;
                    affectedSlots.add(bedrockSlotToJava(((ConsumeAction) action).getSource()));
                    break;
                }
                case TAKE:
                case PLACE: {
                    TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                    if (craftState != CraftState.INGREDIENTS && craftState != CraftState.TRANSFER) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.TRANSFER;

                    if (transferAction.getSource().getContainerName().getContainer() != ContainerSlotType.CREATED_OUTPUT) {
                        return rejectRequest(request);
                    }
                    if (transferAction.getCount() <= 0) {
                        return rejectRequest(request);
                    }

                    int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                    int destSlot = bedrockSlotToJava(transferAction.getDestination());

                    if (isCursor(transferAction.getDestination())) {
                        plan.add(Click.LEFT, sourceSlot);
                        craftState = CraftState.DONE;
                    } else {
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

                        if (plan.getCursor().isEmpty()) {
                            
                            for (int i = 0; i < timesToCraft; i++) {
                                plan.add(Click.LEFT, sourceSlot);
                                plan.add(Click.LEFT, destSlot);
                            }
                        } else {
                            GeyserItemStack cursor = session.getPlayerInventory().getCursor();
                            int tempSlot = findTempSlot(plan, cursor, true, sourceSlot, destSlot);
                            if (tempSlot == -1) {
                                return rejectRequest(request);
                            }

                            plan.add(Click.LEFT, tempSlot); //place cursor into temp slot
                            for (int i = 0; i < timesToCraft; i++) {
                                plan.add(Click.LEFT, sourceSlot); //pick up source item
                                plan.add(Click.LEFT, destSlot); //place source item into dest slot
                            }
                            plan.add(Click.LEFT, tempSlot); //pick up original item
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
        affectedSlots.addAll(plan.getAffectedSlots());
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }

    public ItemStackResponse translateAutoCraftingRequest(GeyserSession session, Type inventory, ItemStackRequest request) {
        final int gridSize = getGridSize();
        if (gridSize == -1) {
            return rejectRequest(request);
        }
        int gridDimensions = gridSize == 4 ? 2 : 3;

        List<SlotDisplay> ingredients = Collections.emptyList();
        SlotDisplay output = null;
        int recipeWidth = 0;
        int ingRemaining = 0;
        int ingredientIndex = -1;

        Int2IntMap consumedSlots = new Int2IntOpenHashMap();
        int prioritySlot = -1;
        int tempSlot;

        int resultSize;
        int timesCrafted = 0;
        Int2ObjectMap<Int2IntMap> ingredientMap = new Int2ObjectOpenHashMap<>();
        CraftState craftState = CraftState.START;

        ClickPlan plan = new ClickPlan(session, this, inventory);
        requestLoop:
        for (ItemStackRequestAction action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_RECIPE_AUTO: {
                    AutoCraftRecipeAction autoCraftAction = (AutoCraftRecipeAction) action;
                    
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;

                    int recipeId = autoCraftAction.getRecipeNetworkId();
                    GeyserRecipe recipe = session.getCraftingRecipes().get(recipeId);
                    if (recipe == null) {
                        return rejectRequest(request);
                    }
                    if (!plan.getCursor().isEmpty()) {
                        return rejectRequest(request);
                    }
                    //reject if crafting grid is not clear
                    for (int i = 1; i <= gridSize; i++) {
                        if (!inventory.getItem(i).isEmpty()) {
                            return rejectRequest(request);
                        }
                    }

                    if (recipe.isShaped()) {
                        GeyserShapedRecipe shapedRecipe = (GeyserShapedRecipe) recipe;
                        ingredients = shapedRecipe.ingredients();
                        recipeWidth = shapedRecipe.width();
                        output = shapedRecipe.result();
                        if (recipeWidth > gridDimensions || shapedRecipe.height() > gridDimensions) {
                            return rejectRequest(request);
                        }
                    } else {
                        GeyserShapelessRecipe shapelessRecipe = (GeyserShapelessRecipe) recipe;
                        ingredients = shapelessRecipe.ingredients();
                        recipeWidth = gridDimensions;
                        output = shapelessRecipe.result();
                        if (ingredients.size() > gridSize) {
                            return rejectRequest(request);
                        }
                    }
                    break;
                }
                case CRAFT_RESULTS_DEPRECATED: {
                    CraftResultsDeprecatedAction deprecatedCraftAction = (CraftResultsDeprecatedAction) action;
                    if (craftState != CraftState.RECIPE_ID) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.DEPRECATED;

                    if (deprecatedCraftAction.getResultItems().length != 1) {
                        return rejectRequest(request);
                    }
                    resultSize = deprecatedCraftAction.getResultItems()[0].getCount();
                    timesCrafted = deprecatedCraftAction.getTimesCrafted();
                    if (resultSize <= 0 || timesCrafted <= 0) {
                        return rejectRequest(request);
                    }
                    break;
                }
                case CONSUME: {
                    ConsumeAction consumeAction = (ConsumeAction) action;
                    if (craftState != CraftState.DEPRECATED && craftState != CraftState.INGREDIENTS) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.INGREDIENTS;

                    if (ingRemaining == 0) {
                        while (++ingredientIndex < ingredients.size()) {
                            if (!(ingredients.get(ingredientIndex) instanceof EmptySlotDisplay)) { 
                                ingRemaining = timesCrafted;
                                break;
                            }
                        }
                    }

                    ingRemaining -= consumeAction.getCount();
                    if (ingRemaining < 0)
                        return rejectRequest(request);

                    int javaSlot = bedrockSlotToJava(consumeAction.getSource());
                    consumedSlots.merge(javaSlot, consumeAction.getCount(), Integer::sum);

                    int gridSlot = 1 + ingredientIndex + ((ingredientIndex / recipeWidth) * (gridDimensions - recipeWidth));
                    Int2IntMap sources = ingredientMap.computeIfAbsent(gridSlot, k -> new Int2IntOpenHashMap());
                    sources.put(javaSlot, consumeAction.getCount());
                    break;
                }
                case TAKE:
                case PLACE: {
                    TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                    if (craftState != CraftState.INGREDIENTS && craftState != CraftState.TRANSFER) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.TRANSFER;

                    if (transferAction.getSource().getContainerName().getContainer() != ContainerSlotType.CREATED_OUTPUT) {
                        return rejectRequest(request);
                    }
                    if (transferAction.getCount() <= 0) {
                        return rejectRequest(request);
                    }

                    int javaSlot = bedrockSlotToJava(transferAction.getDestination());
                    if (isCursor(transferAction.getDestination())) { 
                        if (timesCrafted > 1) {
                            tempSlot = findTempSlot(plan, GeyserItemStack.from(session, output), true);
                            if (tempSlot == -1) {
                                return rejectRequest(request);
                            }
                        }
                        break requestLoop;
                    } else if (inventory.getItem(javaSlot).getAmount() == consumedSlots.get(javaSlot)) {
                        prioritySlot = bedrockSlotToJava(transferAction.getDestination());
                        break requestLoop;
                    }
                    break;
                }
                default:
                    return rejectRequest(request);
            }
        }

        final int maxLoops = Math.min(64, timesCrafted);
        for (int loops = 0; loops < maxLoops; loops++) {
            boolean done = true;
            for (Int2ObjectMap.Entry<Int2IntMap> entry : ingredientMap.int2ObjectEntrySet()) {
                Int2IntMap sources = entry.getValue();
                if (sources.isEmpty())
                    continue;

                done = false;
                int gridSlot = entry.getIntKey();
                if (!plan.getItem(gridSlot).isEmpty())
                    continue;

                int sourceSlot;
                if (loops == 0 && sources.containsKey(prioritySlot)) {
                    sourceSlot = prioritySlot;
                } else {
                    sourceSlot = sources.keySet().iterator().nextInt();
                }
                int transferAmount = sources.remove(sourceSlot);
                transferSlot(plan, sourceSlot, gridSlot, transferAmount);
            }

            if (!done) {
                
                plan.add(Click.LEFT_SHIFT, 0, true);
            } else {
                break;
            }
        }

        inventory.setItem(0, GeyserItemStack.from(session, output), session);
        plan.execute(true);
        return acceptRequest(request, makeContainerEntries(session, inventory, plan.getAffectedSlots()));
    }

    /**
     * Handled in {@link PlayerInventoryTranslator}
     */
    protected ItemStackResponse translateCreativeRequest(GeyserSession session, Type inventory, ItemStackRequest request) {
        return rejectRequest(request);
    }

    private void transferSlot(ClickPlan plan, int sourceSlot, int destSlot, int transferAmount) {
        boolean tempSwap = !plan.getCursor().isEmpty();
        int sourceAmount = plan.getItem(sourceSlot).getAmount();
        if (transferAmount == sourceAmount) { //transfer all
            plan.add(Click.LEFT, sourceSlot); //pickup source
            plan.add(Click.LEFT, destSlot); //let go of all items and done
        } else { //transfer some
            //try to transfer items with least clicks possible
            int halfSource = sourceAmount - (sourceAmount / 2); //larger half
            int holding;
            if (!tempSwap && transferAmount <= halfSource) { //faster to take only half. CURSOR MUST BE EMPTY
                plan.add(Click.RIGHT, sourceSlot);
                holding = halfSource;
            } else { //need all
                plan.add(Click.LEFT, sourceSlot);
                holding = sourceAmount;
            }
            if (!tempSwap && transferAmount > holding / 2) { //faster to release extra items onto source or dest slot?
                for (int i = 0; i < holding - transferAmount; i++) {
                    plan.add(Click.RIGHT, sourceSlot); //prepare cursor
                }
                plan.add(Click.LEFT, destSlot); //release cursor onto dest slot
            } else {
                for (int i = 0; i < transferAmount; i++) {
                    plan.add(Click.RIGHT, destSlot); //right click until transfer goal is met
                }
                plan.add(Click.LEFT, sourceSlot); //return extra items to source slot
            }
        }
    }

    protected static ItemStackResponse acceptRequest(ItemStackRequest request, List<ItemStackResponseContainer> containerEntries) {
        return new ItemStackResponse(ItemStackResponseStatus.OK, request.getRequestId(), containerEntries);
    }

    /**
     * Reject an incorrect ItemStackRequest.
     */
    protected static ItemStackResponse rejectRequest(ItemStackRequest request) {
        return rejectRequest(request, true);
    }

    /**
     * Reject an incorrect ItemStackRequest.
     *
     * @param throwError whether this request was truly erroneous (true), or known as an outcome and should not be treated
     *                   as bad (false).
     */
    protected static ItemStackResponse rejectRequest(ItemStackRequest request, boolean throwError) {
        if (throwError && GeyserImpl.getInstance().config().debugMode()) {
            new Throwable("DEBUGGING: ItemStackRequest rejected " + request.toString()).printStackTrace();
        }
        return new ItemStackResponse(ItemStackResponseStatus.ERROR, request.getRequestId(), Collections.emptyList());
    }

    /**
     * Print out the contents of an ItemStackRequest, should the net ID check fail.
     */
    protected void dumpStackRequestDetails(GeyserSession session, Type inventory, ItemStackRequestSlotData source, ItemStackRequestSlotData destination) {
        session.getGeyser().getLogger().error("Source: " + source.toString() + " Result: " + checkNetId(session, inventory, source));
        session.getGeyser().getLogger().error("Destination: " + destination.toString() + " Result: " + checkNetId(session, inventory, destination));
        session.getGeyser().getLogger().error("Geyser's record of source slot: " + inventory.getItem(bedrockSlotToJava(source)));
        session.getGeyser().getLogger().error("Geyser's record of destination slot: " + inventory.getItem(bedrockSlotToJava(destination)));
    }

    public boolean checkNetId(GeyserSession session, Type inventory, ItemStackRequestSlotData slotInfoData) {
        if (BundleInventoryTranslator.isBundle(slotInfoData)) {
            
            return true;
        }

        int netId = slotInfoData.getStackNetworkId();
        
        
        if (netId < 0 || netId == 1)
            return true;

        GeyserItemStack currentItem = isCursor(slotInfoData) ? session.getPlayerInventory().getCursor() : inventory.getItem(bedrockSlotToJava(slotInfoData));
        return currentItem.getNetId() == netId;
    }

    /**
     * Try to find a slot that is preferably empty, or does not stack with a given item.
     * Only looks in the main inventory and hotbar (excluding offhand).
     * <p>
     * Slots are searched in the reverse order that the bedrock client uses for quick moving.
     *
     * @param plan used to check the simulated inventory
     * @param item the item to temporarily store
     * @param emptyOnly if only empty slots should be considered
     * @param slotBlacklist list of slots to exclude; the items contained in these slots will also be checked for stacking
     * @return the temp slot, or -1 if no suitable slot was found
     */
    private static int findTempSlot(ClickPlan plan, GeyserItemStack item, boolean emptyOnly, int... slotBlacklist) {
        IntSortedSet potentialSlots = new IntLinkedOpenHashSet(PLAYER_INVENTORY_SIZE);
        int hotbarOffset = plan.getInventory().getOffsetForHotbar(0);

        
        for (int i = hotbarOffset - 1; i >= hotbarOffset - 27; i--) {
            potentialSlots.add(i);
        }

        
        for (int i = hotbarOffset + 8; i >= hotbarOffset; i--) {
            potentialSlots.add(i);
        }

        for (int i : slotBlacklist) {
            potentialSlots.remove(i);
        }

        
        IntIterator it = potentialSlots.iterator();
        while (it.hasNext()) {
            int slot = it.nextInt();
            if (plan.isEmpty(slot)) {
                return slot;
            }
        }

        if (emptyOnly) {
            return -1;
        }

        
        it = potentialSlots.iterator();

        outer:
        while (it.hasNext()) {
            int slot = it.nextInt();
            if (plan.canStack(slot, item)) {
                continue;
            }

            for (int blacklistedSlot : slotBlacklist) {
                GeyserItemStack blacklistedItem = plan.getItem(blacklistedSlot);
                if (plan.canStack(slot, blacklistedItem)) {
                    continue outer;
                }
            }

            return slot;
        }

        return -1;
    }

    protected final List<ItemStackResponseContainer> makeContainerEntries(GeyserSession session, Type inventory, IntSet affectedSlots) {
        Map<ContainerSlotType, List<ItemStackResponseSlot>> containerMap = new HashMap<>();
        
        IntIterator it = affectedSlots.iterator();
        while (it.hasNext()) {
            int slot = it.nextInt();
            BedrockContainerSlot bedrockSlot = javaSlotToBedrockContainer(slot, inventory);
            List<ItemStackResponseSlot> list = containerMap.computeIfAbsent(bedrockSlot.container(), k -> new ArrayList<>());
            list.add(makeItemEntry(bedrockSlot.slot(), inventory.getItem(slot)));
        }

        List<ItemStackResponseContainer> containerEntries = new ArrayList<>();
        for (Map.Entry<ContainerSlotType, List<ItemStackResponseSlot>> entry : containerMap.entrySet()) {
            containerEntries.add(new ItemStackResponseContainer(entry.getKey(), entry.getValue(), new FullContainerName(entry.getKey(), null)));
        }

        ItemStackResponseSlot cursorEntry = makeItemEntry(0, session.getPlayerInventory().getCursor());
        containerEntries.add(new ItemStackResponseContainer(ContainerSlotType.CURSOR, Collections.singletonList(cursorEntry), new FullContainerName(ContainerSlotType.CURSOR, null)));

        return containerEntries;
    }

    private static ItemStackResponseSlot makeItemEntry(int bedrockSlot, GeyserItemStack itemStack) {
        ItemStackResponseSlot itemEntry;
        if (!itemStack.isEmpty()) {
            
            
            int durability = 0;
            Integer damage = itemStack.getComponent(DataComponentTypes.DAMAGE);
            if (damage != null) {
                durability = ItemUtils.getCorrectBedrockDurability(itemStack.asItem(), damage);
            }

            itemEntry = new ItemStackResponseSlot((byte) bedrockSlot, (byte) bedrockSlot, (byte) itemStack.getAmount(), itemStack.getNetId(), "", durability, "");
        } else {
            itemEntry = new ItemStackResponseSlot((byte) bedrockSlot, (byte) bedrockSlot, (byte) 0, 0, "", 0, "");
        }
        return itemEntry;
    }

    protected static boolean isCursor(ItemStackRequestSlotData slotInfoData) {
        return slotInfoData.getContainerName().getContainer() == ContainerSlotType.CURSOR;
    }

    /**
     * Gets the {@link InventoryTranslator} for the given {@link ContainerType}.
     * Returns {@link #PLAYER_INVENTORY_TRANSLATOR} if type is null.
     *
     * @param type the type
     * @return the InventoryType for the given ContainerType.
     */
    @Nullable
    public static InventoryTranslator<? extends Inventory> inventoryTranslator(@Nullable ContainerType type) {
        if (type == null) {
            return PLAYER_INVENTORY_TRANSLATOR;
        }

        return INVENTORY_TRANSLATORS.get(type);
    }

    protected enum CraftState {
        START,
        RECIPE_ID,
        DEPRECATED,
        INGREDIENTS,
        TRANSFER,
        DONE
    }
}
