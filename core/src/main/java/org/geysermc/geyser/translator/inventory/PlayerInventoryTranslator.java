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

package org.geysermc.geyser.translator.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.inventory.*;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.*;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.geysermc.geyser.inventory.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.FakeHeadProvider;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.IntFunction;

public class PlayerInventoryTranslator extends InventoryTranslator {
    private static final IntFunction<ItemData> UNUSUABLE_CRAFTING_SPACE_BLOCK = InventoryUtils.createUnusableSpaceBlock(GeyserLocale.getLocaleStringLog("geyser.inventory.unusable_item.creative"));

    public PlayerInventoryTranslator() {
        super(46);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updateCraftingGrid(session, inventory);

        InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
        inventoryContentPacket.setContainerId(ContainerId.INVENTORY);
        ItemData[] contents = new ItemData[36];
        // Inventory
        for (int i = 9; i < 36; i++) {
            contents[i] = inventory.getItem(i).getItemData(session);
        }
        // Hotbar
        for (int i = 36; i < 45; i++) {
            contents[i - 36] = inventory.getItem(i).getItemData(session);
        }
        inventoryContentPacket.setContents(Arrays.asList(contents));
        session.sendUpstreamPacket(inventoryContentPacket);

        // Armor
        InventoryContentPacket armorContentPacket = new InventoryContentPacket();
        armorContentPacket.setContainerId(ContainerId.ARMOR);
        contents = new ItemData[4];
        for (int i = 5; i < 9; i++) {
            contents[i - 5] = inventory.getItem(i).getItemData(session);
        }
        armorContentPacket.setContents(Arrays.asList(contents));
        session.sendUpstreamPacket(armorContentPacket);

        // Offhand
        InventoryContentPacket offhandPacket = new InventoryContentPacket();
        offhandPacket.setContainerId(ContainerId.OFFHAND);
        offhandPacket.setContents(Collections.singletonList(inventory.getItem(45).getItemData(session)));
        session.sendUpstreamPacket(offhandPacket);
    }

    /**
     * Update the crafting grid for the player to hide/show the barriers in the creative inventory
     * @param session Connection of the player
     * @param inventory Inventory of the player
     */
    public static void updateCraftingGrid(GeyserSession session, Inventory inventory) {
        // Crafting grid
        for (int i = 1; i < 5; i++) {
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(i + 27);

            if (session.getGameMode() == GameMode.CREATIVE) {
                slotPacket.setItem(UNUSUABLE_CRAFTING_SPACE_BLOCK.apply(session.getUpstream().getProtocolVersion()));
            } else {
                slotPacket.setItem(ItemTranslator.translateToBedrock(session, inventory.getItem(i).getItemStack()));
            }

            session.sendUpstreamPacket(slotPacket);
        }
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        GeyserItemStack javaItem = inventory.getItem(slot);
        ItemData bedrockItem = javaItem.getItemData(session);

        if (slot == 5) {
            // Check for custom skull
            if (javaItem.getJavaId() == session.getItemMappings().getStoredItems().playerHead().getJavaId()
                    && javaItem.getNbt() != null
                    && javaItem.getNbt().get("SkullOwner") instanceof CompoundTag profile) {
                FakeHeadProvider.setHead(session, session.getPlayerEntity(), profile);
            } else {
                FakeHeadProvider.restoreOriginalSkin(session, session.getPlayerEntity());
            }
        }

        if (slot >= 1 && slot <= 44) {
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            if (slot >= 9) {
                slotPacket.setContainerId(ContainerId.INVENTORY);
                if (slot >= 36) {
                    slotPacket.setSlot(slot - 36);
                } else {
                    slotPacket.setSlot(slot);
                }
            } else if (slot >= 5) {
                slotPacket.setContainerId(ContainerId.ARMOR);
                slotPacket.setSlot(slot - 5);
            } else {
                slotPacket.setContainerId(ContainerId.UI);
                slotPacket.setSlot(slot + 27);
            }
            slotPacket.setItem(bedrockItem);
            session.sendUpstreamPacket(slotPacket);
        } else if (slot == 45) {
            InventoryContentPacket offhandPacket = new InventoryContentPacket();
            offhandPacket.setContainerId(ContainerId.OFFHAND);
            offhandPacket.setContents(Collections.singletonList(bedrockItem));
            session.sendUpstreamPacket(offhandPacket);
        }
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        int slotnum = slotInfoData.getSlot();
        switch (slotInfoData.getContainer()) {
            case HOTBAR_AND_INVENTORY:
            case HOTBAR:
            case INVENTORY:
                // Inventory
                if (slotnum >= 9 && slotnum <= 35) {
                    return slotnum;
                }
                // Hotbar
                if (slotnum >= 0 && slotnum <= 8) {
                    return slotnum + 36;
                }
                break;
            case ARMOR:
                if (slotnum >= 0 && slotnum <= 3) {
                    return slotnum + 5;
                }
                break;
            case OFFHAND:
                return 45;
            case CRAFTING_INPUT:
                if (slotnum >= 28 && 31 >= slotnum) {
                    return slotnum - 27;
                }
                break;
            case CREATIVE_OUTPUT:
                return 0;
        }
        return slotnum;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return -1;
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot >= 36 && slot <= 44) {
            return new BedrockContainerSlot(ContainerSlotType.HOTBAR, slot - 36);
        } else if (slot >= 9 && slot <= 35) {
            return new BedrockContainerSlot(ContainerSlotType.INVENTORY, slot);
        } else if (slot >= 5 && slot <= 8) {
            return new BedrockContainerSlot(ContainerSlotType.ARMOR, slot - 5);
        } else if (slot == 45) {
            return new BedrockContainerSlot(ContainerSlotType.OFFHAND, 1);
        } else if (slot >= 1 && slot <= 4) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_INPUT, slot + 27);
        } else if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_OUTPUT, 0);
        } else {
            throw new IllegalArgumentException("Unknown bedrock slot");
        }
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 0)
            return SlotType.OUTPUT;
        return SlotType.NORMAL;
    }

    @Override
    public ItemStackResponsePacket.Response translateRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        if (session.getGameMode() != GameMode.CREATIVE) {
            return super.translateRequest(session, inventory, request);
        }

        PlayerInventory playerInv = session.getPlayerInventory();
        IntSet affectedSlots = new IntOpenHashSet();
        for (StackRequestActionData action : request.getActions()) {
            switch (action.getType()) {
                case TAKE, PLACE -> {
                    TransferStackRequestActionData transferAction = (TransferStackRequestActionData) action;
                    if (!(checkNetId(session, inventory, transferAction.getSource()) && checkNetId(session, inventory, transferAction.getDestination()))) {
                        return rejectRequest(request);
                    }
                    if (isCraftingGrid(transferAction.getSource()) || isCraftingGrid(transferAction.getDestination())) {
                        return rejectRequest(request, false);
                    }

                    int transferAmount = transferAction.getCount();
                    if (isCursor(transferAction.getDestination())) {
                        int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                        GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                        if (playerInv.getCursor().isEmpty()) {
                            playerInv.setCursor(sourceItem.copy(0), session);
                        }

                        playerInv.getCursor().add(transferAmount);
                        sourceItem.sub(transferAmount);

                        affectedSlots.add(sourceSlot);
                    } else if (isCursor(transferAction.getSource())) {
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
                        GeyserItemStack sourceItem = playerInv.getCursor();
                        if (inventory.getItem(destSlot).isEmpty()) {
                            inventory.setItem(destSlot, sourceItem.copy(0), session);
                        }

                        inventory.getItem(destSlot).add(transferAmount);
                        sourceItem.sub(transferAmount);

                        affectedSlots.add(destSlot);
                    } else {
                        int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
                        GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                        if (inventory.getItem(destSlot).isEmpty()) {
                            inventory.setItem(destSlot, sourceItem.copy(0), session);
                        }

                        inventory.getItem(destSlot).add(transferAmount);
                        sourceItem.sub(transferAmount);

                        affectedSlots.add(sourceSlot);
                        affectedSlots.add(destSlot);
                    }
                }
                case SWAP -> {
                    SwapStackRequestActionData swapAction = (SwapStackRequestActionData) action;
                    if (!(checkNetId(session, inventory, swapAction.getSource()) && checkNetId(session, inventory, swapAction.getDestination()))) {
                        return rejectRequest(request);
                    }
                    if (isCraftingGrid(swapAction.getSource()) || isCraftingGrid(swapAction.getDestination())) {
                        return rejectRequest(request, false);
                    }

                    if (isCursor(swapAction.getDestination())) {
                        int sourceSlot = bedrockSlotToJava(swapAction.getSource());
                        GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                        GeyserItemStack destItem = playerInv.getCursor();

                        playerInv.setCursor(sourceItem, session);
                        inventory.setItem(sourceSlot, destItem, session);

                        affectedSlots.add(sourceSlot);
                    } else if (isCursor(swapAction.getSource())) {
                        int destSlot = bedrockSlotToJava(swapAction.getDestination());
                        GeyserItemStack sourceItem = playerInv.getCursor();
                        GeyserItemStack destItem = inventory.getItem(destSlot);

                        inventory.setItem(destSlot, sourceItem, session);
                        playerInv.setCursor(destItem, session);

                        affectedSlots.add(destSlot);
                    } else {
                        int sourceSlot = bedrockSlotToJava(swapAction.getSource());
                        int destSlot = bedrockSlotToJava(swapAction.getDestination());
                        GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                        GeyserItemStack destItem = inventory.getItem(destSlot);

                        inventory.setItem(destSlot, sourceItem, session);
                        inventory.setItem(sourceSlot, destItem, session);

                        affectedSlots.add(sourceSlot);
                        affectedSlots.add(destSlot);
                    }
                }
                case DROP -> {
                    DropStackRequestActionData dropAction = (DropStackRequestActionData) action;
                    if (!checkNetId(session, inventory, dropAction.getSource())) {
                        return rejectRequest(request);
                    }
                    if (isCraftingGrid(dropAction.getSource())) {
                        return rejectRequest(request, false);
                    }

                    GeyserItemStack sourceItem;
                    if (isCursor(dropAction.getSource())) {
                        sourceItem = playerInv.getCursor();
                    } else {
                        int sourceSlot = bedrockSlotToJava(dropAction.getSource());
                        sourceItem = inventory.getItem(sourceSlot);
                        affectedSlots.add(sourceSlot);
                    }

                    if (sourceItem.isEmpty()) {
                        return rejectRequest(request);
                    }

                    ServerboundSetCreativeModeSlotPacket creativeDropPacket = new ServerboundSetCreativeModeSlotPacket(-1, sourceItem.getItemStack(dropAction.getCount()));
                    session.sendDownstreamPacket(creativeDropPacket);

                    sourceItem.sub(dropAction.getCount());
                }
                case DESTROY -> {
                    // Only called when a creative client wants to destroy an item... I think - Camotoy
                    DestroyStackRequestActionData destroyAction = (DestroyStackRequestActionData) action;
                    if (!checkNetId(session, inventory, destroyAction.getSource())) {
                        return rejectRequest(request);
                    }
                    if (isCraftingGrid(destroyAction.getSource())) {
                        return rejectRequest(request, false);
                    }

                    if (!isCursor(destroyAction.getSource())) {
                        // Item exists; let's remove it from the inventory
                        int javaSlot = bedrockSlotToJava(destroyAction.getSource());
                        GeyserItemStack existingItem = inventory.getItem(javaSlot);
                        existingItem.sub(destroyAction.getCount());
                        affectedSlots.add(javaSlot);
                    } else {
                        // Just sync up the item on our end, since the server doesn't care what's in our cursor
                        playerInv.getCursor().sub(destroyAction.getCount());
                    }
                }
                default -> {
                    session.getGeyser().getLogger().error("Unknown crafting state induced by " + session.name());
                    return rejectRequest(request);
                }
            }
        }
        for (int slot : affectedSlots) {
            sendCreativeAction(session, inventory, slot);
        }
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }

    @Override
    public ItemStackResponsePacket.Response translateCreativeRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        ItemStack javaCreativeItem = null;
        IntSet affectedSlots = new IntOpenHashSet();
        CraftState craftState = CraftState.START;
        for (StackRequestActionData action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_CREATIVE: {
                    CraftCreativeStackRequestActionData creativeAction = (CraftCreativeStackRequestActionData) action;
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;

                    int creativeId = creativeAction.getCreativeItemNetworkId() - 1;
                    ItemData[] creativeItems = session.getItemMappings().getCreativeItems();
                    if (creativeId < 0 || creativeId >= creativeItems.length) {
                        return rejectRequest(request);
                    }
                    // Reference the creative items list we send to the client to know what it's asking of us
                    ItemData creativeItem = creativeItems[creativeId];
                    javaCreativeItem = ItemTranslator.translateToJava(creativeItem, session.getItemMappings());
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
                case DESTROY: {
                    DestroyStackRequestActionData destroyAction = (DestroyStackRequestActionData) action;
                    if (craftState != CraftState.DEPRECATED) {
                        return rejectRequest(request);
                    }

                    int sourceSlot = bedrockSlotToJava(destroyAction.getSource());
                    inventory.setItem(sourceSlot, GeyserItemStack.EMPTY, session); //assume all creative destroy requests will empty the slot
                    affectedSlots.add(sourceSlot);
                    break;
                }
                case TAKE:
                case PLACE: {
                    TransferStackRequestActionData transferAction = (TransferStackRequestActionData) action;
                    if (!(craftState == CraftState.DEPRECATED || craftState == CraftState.TRANSFER)) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.TRANSFER;

                    if (transferAction.getSource().getContainer() != ContainerSlotType.CREATIVE_OUTPUT) {
                        return rejectRequest(request);
                    }

                    if (isCursor(transferAction.getDestination())) {
                        if (session.getPlayerInventory().getCursor().isEmpty()) {
                            GeyserItemStack newItemStack = GeyserItemStack.from(javaCreativeItem);
                            newItemStack.setAmount(transferAction.getCount());
                            session.getPlayerInventory().setCursor(newItemStack, session);
                        } else {
                            session.getPlayerInventory().getCursor().add(transferAction.getCount());
                        }
                        //cursor is always included in response
                    } else {
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
                        if (inventory.getItem(destSlot).isEmpty()) {
                            GeyserItemStack newItemStack = GeyserItemStack.from(javaCreativeItem);
                            newItemStack.setAmount(transferAction.getCount());
                            inventory.setItem(destSlot, newItemStack, session);
                        } else {
                            inventory.getItem(destSlot).add(transferAction.getCount());
                        }
                        affectedSlots.add(destSlot);
                    }
                    break;
                }
                default:
                    return rejectRequest(request);
            }
        }
        for (int slot : affectedSlots) {
            sendCreativeAction(session, inventory, slot);
        }
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }

    private static void sendCreativeAction(GeyserSession session, Inventory inventory, int slot) {
        GeyserItemStack item = inventory.getItem(slot);
        ItemStack itemStack = item.isEmpty() ? new ItemStack(-1, 0, null) : item.getItemStack();

        ServerboundSetCreativeModeSlotPacket creativePacket = new ServerboundSetCreativeModeSlotPacket(slot, itemStack);
        session.sendDownstreamPacket(creativePacket);
    }

    private static boolean isCraftingGrid(StackRequestSlotInfoData slotInfoData) {
        return slotInfoData.getContainer() == ContainerSlotType.CRAFTING_INPUT;
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
    }
}
