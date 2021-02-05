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

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.*;
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import it.unimi.dsi.fastutil.ints.*;
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
        boolean refresh = false;
        ItemStackResponsePacket responsePacket = new ItemStackResponsePacket();
        for (ItemStackRequestPacket.Request request : requests) {
            ItemStackResponsePacket.Response response;
            if (request.getActions().length > 0) {
                StackRequestActionData firstAction = request.getActions()[0];
                if (shouldHandleRequestFirst(firstAction, inventory)) {
                    // Some special request that shouldn't be processed normally
                    response = translateSpecialRequest(session, inventory, request);
                } else if (firstAction.getType() == StackRequestActionType.CRAFT_RECIPE) {
                    response = translateCraftingRequest(session, inventory, request);
                } else if (firstAction.getType() == StackRequestActionType.CRAFT_RECIPE_AUTO) {
                    response = translateAutoCraftingRequest(session, inventory, request);
                } else if (firstAction.getType() == StackRequestActionType.CRAFT_CREATIVE) {
                    // This is also used for pulling items out of creative
                    response = translateCreativeRequest(session, inventory, request);
                } else {
                    response = translateRequest(session, inventory, request);
                }
            } else {
                response = rejectRequest(request);
            }
            if (response.getResult() == ItemStackResponsePacket.ResponseStatus.ERROR) {
                refresh = true;
            }
            responsePacket.getEntries().add(response);
        }
        session.sendUpstreamPacket(responsePacket);

        if (refresh) {
            InventoryUtils.updateCursor(session);
            updateInventory(session, inventory);
        }
    }

    public ItemStackResponsePacket.Response translateRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
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
                        session.getConnector().getLogger().error("DEBUG: About to reject request made by " + session.getName());
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
                            if (transferAction.getCount() != sourceAmount) {
                                int tempSlot = findTempSlot(inventory, cursor, false, sourceSlot);
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
                            tempSlot = findTempSlot(inventory, cursor, false, sourceSlot, destSlot);
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
                            inventory.setItem(sourceSlot, GeyserItemStack.EMPTY, session);
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
        int recipeId = 0;
        int resultSize = 0;
        int timesCrafted = 0;
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
                    break;
                }
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
                    timesCrafted = deprecatedCraftAction.getTimesCrafted();
                    if (resultSize <= 0 || timesCrafted <= 0) {
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
        return acceptRequest(request, makeContainerEntries(session, inventory, plan.getAffectedSlots()));
    }

    public ItemStackResponsePacket.Response translateAutoCraftingRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        int gridSize;
        int gridDimensions;
        if (this instanceof PlayerInventoryTranslator) {
            gridSize = 4;
            gridDimensions = 2;
        } else if (this instanceof CraftingInventoryTranslator) {
            gridSize = 9;
            gridDimensions = 3;
        } else {
            return rejectRequest(request);
        }

        Recipe recipe;
        Ingredient[] ingredients = new Ingredient[0];
        ItemStack output = null;
        int recipeWidth = 0;
        int ingRemaining = 0;
        int ingredientIndex = -1;

        Int2IntMap consumedSlots = new Int2IntOpenHashMap();
        int prioritySlot = -1;
        int secondarySlot = -1;
        int tempSlot = -1;
        boolean intoCursor = false;

        int resultSize;
        int timesCrafted = 0;
        Int2ObjectMap<Int2IntMap> ingredientMap = new Int2ObjectOpenHashMap<>();
        CraftState craftState = CraftState.START;

        ClickPlan plan = new ClickPlan(session, this, inventory);
        requestLoop:
        for (StackRequestActionData action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_RECIPE_AUTO: {
                    AutoCraftRecipeStackRequestActionData autoCraftAction = (AutoCraftRecipeStackRequestActionData) action;
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;

                    int recipeId = autoCraftAction.getRecipeNetworkId();
                    recipe = session.getCraftingRecipes().get(recipeId);
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

                    switch (recipe.getType()) {
                        case CRAFTING_SHAPED:
                            ShapedRecipeData shapedData = (ShapedRecipeData) recipe.getData();
                            ingredients = shapedData.getIngredients();
                            recipeWidth = shapedData.getWidth();
                            output = shapedData.getResult();
                            if (shapedData.getWidth() > gridDimensions || shapedData.getHeight() > gridDimensions) {
                                return rejectRequest(request);
                            }
                            break;
                        case CRAFTING_SHAPELESS:
                            ShapelessRecipeData shapelessData = (ShapelessRecipeData) recipe.getData();
                            ingredients = shapelessData.getIngredients();
                            recipeWidth = gridDimensions;
                            output = shapelessData.getResult();
                            if (ingredients.length > gridSize) {
                                return rejectRequest(request);
                            }
                            break;
                    }
                    break;
                }
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
                    timesCrafted = deprecatedCraftAction.getTimesCrafted();
                    if (resultSize <= 0 || timesCrafted <= 0) {
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

                    if (ingRemaining == 0) {
                        while (++ingredientIndex < ingredients.length) {
                            if (ingredients[ingredientIndex].getOptions().length != 0) {
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

                    int javaSlot = bedrockSlotToJava(transferAction.getDestination());
                    if (isCursor(transferAction.getDestination())) { //TODO
                        intoCursor = true;
                        if (timesCrafted > 1) {
                            tempSlot = findTempSlot(inventory, GeyserItemStack.from(output), true);
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
                //TODO: sometimes the server does not agree on this slot?
                plan.add(Click.LEFT_SHIFT, 0, true);
            } else {
                break;
            }
        }

        inventory.setItem(0, GeyserItemStack.from(output), session);
        plan.execute(true);
        return acceptRequest(request, makeContainerEntries(session, inventory, plan.getAffectedSlots()));
    }

    public ItemStackResponsePacket.Response translateCreativeRequest(GeyserSession session, Inventory inventory, ItemStackRequestPacket.Request request) {
        // Handled in PlayerInventoryTranslator
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

    public static ItemStackResponsePacket.Response acceptRequest(ItemStackRequestPacket.Request request, List<ItemStackResponsePacket.ContainerEntry> containerEntries) {
        return new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.OK, request.getRequestId(), containerEntries);
    }

    public static ItemStackResponsePacket.Response rejectRequest(ItemStackRequestPacket.Request request) {
        return rejectRequest(request, true);
    }

    public static ItemStackResponsePacket.Response rejectRequest(ItemStackRequestPacket.Request request, boolean throwError) {
        if (throwError) {
            // Currently for debugging, but might be worth it to keep in the future if something goes terribly wrong.
            new Throwable("DEBUGGING: ItemStackRequest rejected " + request.toString()).printStackTrace();
        }
        return new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, request.getRequestId(), Collections.emptyList());
    }

    public boolean checkNetId(GeyserSession session, Inventory inventory, StackRequestSlotInfoData slotInfoData) {
        int netId = slotInfoData.getStackNetworkId();
        if (netId < 0 || netId == 1)
            return true;

        GeyserItemStack currentItem = isCursor(slotInfoData) ? session.getPlayerInventory().getCursor() : inventory.getItem(bedrockSlotToJava(slotInfoData));
        return currentItem.getNetId() == netId;
    }

    /**
     * Try to find a slot that can temporarily store the given item.
     * Only looks in the main inventory and hotbar (excluding offhand).
     * Only slots that are empty or contain a different type of item are valid.
     *
     * @return java id for the temporary slot, or -1 if no viable slot was found
     */
    //TODO: compatibility for simulated inventory (ClickPlan)
    private static int findTempSlot(Inventory inventory, GeyserItemStack item, boolean emptyOnly, int... slotBlacklist) {
        int offset = inventory.getId() == 0 ? 1 : 0; //offhand is not a viable temp slot
        HashSet<GeyserItemStack> itemBlacklist = new HashSet<>(slotBlacklist.length + 1);
        itemBlacklist.add(item);

        IntSet potentialSlots = new IntOpenHashSet(36);
        for (int i = inventory.getSize() - (36 + offset); i < inventory.getSize() - offset; i++) {
            potentialSlots.add(i);
        }
        for (int i : slotBlacklist) {
            potentialSlots.remove(i);
            GeyserItemStack blacklistedItem = inventory.getItem(i);
            if (!blacklistedItem.isEmpty()) {
                itemBlacklist.add(blacklistedItem);
            }
        }

        for (int i : potentialSlots) {
            GeyserItemStack testItem = inventory.getItem(i);
            if ((emptyOnly && !testItem.isEmpty())) {
                continue;
            }

            boolean viable = true;
            for (GeyserItemStack blacklistedItem : itemBlacklist) {
                if (InventoryUtils.canStack(testItem, blacklistedItem)) {
                    viable = false;
                    break;
                }
            }
            if (!viable) {
                continue;
            }
            return i;
        }
        //could not find a viable temp slot
        return -1;
    }

    public List<ItemStackResponsePacket.ContainerEntry> makeContainerEntries(GeyserSession session, Inventory inventory, Set<Integer> affectedSlots) {
        Map<ContainerSlotType, List<ItemStackResponsePacket.ItemEntry>> containerMap = new HashMap<>();
        for (int slot : affectedSlots) {
            BedrockContainerSlot bedrockSlot = javaSlotToBedrockContainer(slot);
            List<ItemStackResponsePacket.ItemEntry> list = containerMap.computeIfAbsent(bedrockSlot.getContainer(), k -> new ArrayList<>());
            list.add(makeItemEntry(bedrockSlot.getSlot(), inventory.getItem(slot)));
        }

        List<ItemStackResponsePacket.ContainerEntry> containerEntries = new ArrayList<>();
        for (Map.Entry<ContainerSlotType, List<ItemStackResponsePacket.ItemEntry>> entry : containerMap.entrySet()) {
            containerEntries.add(new ItemStackResponsePacket.ContainerEntry(entry.getKey(), entry.getValue()));
        }

        ItemStackResponsePacket.ItemEntry cursorEntry = makeItemEntry(0, session.getPlayerInventory().getCursor());
        containerEntries.add(new ItemStackResponsePacket.ContainerEntry(ContainerSlotType.CURSOR, Collections.singletonList(cursorEntry)));

        return containerEntries;
    }

    public static ItemStackResponsePacket.ItemEntry makeItemEntry(int bedrockSlot, GeyserItemStack itemStack) {
        ItemStackResponsePacket.ItemEntry itemEntry;
        if (!itemStack.isEmpty()) {
            itemEntry = new ItemStackResponsePacket.ItemEntry((byte) bedrockSlot, (byte) bedrockSlot, (byte) itemStack.getAmount(), itemStack.getNetId(), "");
        } else {
            itemEntry = new ItemStackResponsePacket.ItemEntry((byte) bedrockSlot, (byte) bedrockSlot, (byte) 0, 0, "");
        }
        return itemEntry;
    }

    protected static boolean isCursor(StackRequestSlotInfoData slotInfoData) {
        return slotInfoData.getContainer() == ContainerSlotType.CURSOR;
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
