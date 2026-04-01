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

#include "it.unimi.dsi.fastutil.ints.IntIterator"
#include "it.unimi.dsi.fastutil.ints.IntOpenHashSet"
#include "it.unimi.dsi.fastutil.ints.IntSet"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftCreativeAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DestroyAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.PlayerInventory"
#include "org.geysermc.geyser.inventory.SlotType"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.skin.FakeHeadProvider"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket"

#include "java.util.Arrays"
#include "java.util.Collections"
#include "java.util.List"
#include "java.util.concurrent.TimeUnit"
#include "java.util.function.IntFunction"

public class PlayerInventoryTranslator extends InventoryTranslator<PlayerInventory> {
    private static final IntFunction<ItemData> UNUSUABLE_CRAFTING_SPACE_BLOCK = InventoryUtils.createUnusableSpaceBlock(GeyserLocale.getLocaleStringLog("geyser.inventory.unusable_item.creative"));

    public PlayerInventoryTranslator() {
        super(46);
    }

    override public int getGridSize() {
        return 4;
    }

    override public void updateInventory(GeyserSession session, PlayerInventory inventory) {
        updateCraftingGrid(session, inventory);

        InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
        inventoryContentPacket.setContainerId(ContainerId.INVENTORY);
        ItemData[] contents = new ItemData[36];

        for (int i = 9; i < 36; i++) {
            contents[i] = inventory.getItem(i).getItemData(session);
        }

        for (int i = 36; i < 45; i++) {
            contents[i - 36] = inventory.getItem(i).getItemData(session);
        }
        inventoryContentPacket.setContents(Arrays.asList(contents));
        session.sendUpstreamPacket(inventoryContentPacket);


        InventoryContentPacket armorContentPacket = new InventoryContentPacket();
        armorContentPacket.setContainerId(ContainerId.ARMOR);
        contents = new ItemData[4];
        for (int i = 5; i < 9; i++) {
            GeyserItemStack item = inventory.getItem(i);
            contents[i - 5] = item.getItemData(session);
            if (i == 5 && item.is(Items.PLAYER_HEAD) && item.hasNonBaseComponents()) {
                FakeHeadProvider.setHead(session, session.getPlayerEntity(), item.getComponent(DataComponentTypes.PROFILE));
            }
        }
        armorContentPacket.setContents(Arrays.asList(contents));
        session.sendUpstreamPacket(armorContentPacket);


        InventoryContentPacket offhandPacket = new InventoryContentPacket();
        offhandPacket.setContainerId(ContainerId.OFFHAND);
        offhandPacket.setContents(Collections.singletonList(inventory.getItem(45).getItemData(session)));
        session.sendUpstreamPacket(offhandPacket);
    }


    public static void updateCraftingGrid(GeyserSession session, PlayerInventory inventory) {

        for (int i = 1; i < 5; i++) {
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(i + 27);

            if (session.getGameMode() == GameMode.CREATIVE) {
                slotPacket.setItem(UNUSUABLE_CRAFTING_SPACE_BLOCK.apply(session.getUpstream().getProtocolVersion()));
            } else {
                slotPacket.setItem(inventory.getItem(i).getItemData(session));
            }

            session.sendUpstreamPacket(slotPacket);
        }
    }

    override public void updateSlot(GeyserSession session, PlayerInventory inventory, int slot) {
        GeyserItemStack javaItem = inventory.getItem(slot);
        ItemData bedrockItem = javaItem.getItemData(session);

        if (slot == 5) {

            if (javaItem.is(Items.PLAYER_HEAD) && javaItem.hasNonBaseComponents()) {
                FakeHeadProvider.setHead(session, session.getPlayerEntity(), javaItem.getComponent(DataComponentTypes.PROFILE));
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

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        int slotnum = slotInfoData.getSlot();
        switch (slotInfoData.getContainerName().getContainer()) {
            case HOTBAR_AND_INVENTORY:
            case HOTBAR:
            case INVENTORY:

                if (slotnum >= 9 && slotnum <= 35) {
                    return slotnum;
                }

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
            case CREATED_OUTPUT:
                return 0;
        }
        return slotnum;
    }

    override public int javaSlotToBedrock(int slot) {
        return -1;
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, PlayerInventory inventory) {
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

    override public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 0)
            return SlotType.OUTPUT;
        return SlotType.NORMAL;
    }

    override public ItemStackResponse translateRequest(GeyserSession session, PlayerInventory inventory, ItemStackRequest request) {
        if (session.getGameMode() != GameMode.CREATIVE) {
            return super.translateRequest(session, inventory, request);
        }

        PlayerInventory playerInv = session.getPlayerInventory();
        IntSet affectedSlots = new IntOpenHashSet();
        for (ItemStackRequestAction action : request.getActions()) {
            switch (action.getType()) {
                case TAKE, PLACE -> {
                    TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                    if (!(checkNetId(session, inventory, transferAction.getSource()) && checkNetId(session, inventory, transferAction.getDestination()))) {
                        return rejectRequest(request);
                    }
                    if (isCraftingGrid(transferAction.getSource()) || isCraftingGrid(transferAction.getDestination())) {
                        return rejectRequest(request, false);
                    }




                    ItemStackResponse bundleResponse = BundleInventoryTranslator.handleBundle(session, this, inventory, request, true);
                    if (bundleResponse != null) {

                        return bundleResponse;
                    }

                    int sourceSlot = bedrockSlotToJava(transferAction.getSource());
                    int destSlot = bedrockSlotToJava(transferAction.getDestination());
                    if (destSlot == 5) {

                        GeyserItemStack javaItem = inventory.getItem(sourceSlot);
                        if (javaItem.is(Items.PLAYER_HEAD) && javaItem.hasNonBaseComponents()) {
                            FakeHeadProvider.setHead(session, session.getPlayerEntity(), javaItem.getComponent(DataComponentTypes.PROFILE));
                        }
                    } else if (sourceSlot == 5) {

                        FakeHeadProvider.restoreOriginalSkin(session, session.getPlayerEntity());
                    }

                    int transferAmount = transferAction.getCount();
                    if (isCursor(transferAction.getDestination())) {
                        GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                        if (playerInv.getCursor().isEmpty()) {
                            playerInv.setCursor(sourceItem.copy(0), session);
                        } else if (!InventoryUtils.canStack(sourceItem, playerInv.getCursor())) {
                            return rejectRequest(request);
                        }

                        playerInv.getCursor().add(transferAmount);
                        sourceItem.sub(transferAmount);

                        affectedSlots.add(sourceSlot);
                    } else if (isCursor(transferAction.getSource())) {
                        GeyserItemStack sourceItem = playerInv.getCursor();
                        if (inventory.getItem(destSlot).isEmpty()) {
                            inventory.setItem(destSlot, sourceItem.copy(0), session);
                        } else if (!InventoryUtils.canStack(sourceItem, inventory.getItem(destSlot))) {
                            return rejectRequest(request);
                        }

                        inventory.getItem(destSlot).add(transferAmount);
                        sourceItem.sub(transferAmount);

                        affectedSlots.add(destSlot);
                    } else {
                        GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                        if (inventory.getItem(destSlot).isEmpty()) {
                            inventory.setItem(destSlot, sourceItem.copy(0), session);
                        } else if (!InventoryUtils.canStack(sourceItem, inventory.getItem(destSlot))) {
                            return rejectRequest(request);
                        }

                        inventory.getItem(destSlot).add(transferAmount);
                        sourceItem.sub(transferAmount);

                        affectedSlots.add(sourceSlot);
                        affectedSlots.add(destSlot);
                    }
                }
                case SWAP -> {
                    SwapAction swapAction = (SwapAction) action;
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
                    DropAction dropAction = (DropAction) action;
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

                    ServerboundSetCreativeModeSlotPacket creativeDropPacket = new ServerboundSetCreativeModeSlotPacket((short)-1, sourceItem.getItemStack(dropAction.getCount()));
                    session.sendDownstreamGamePacket(creativeDropPacket);

                    sourceItem.sub(dropAction.getCount());
                }
                case DESTROY -> {

                    DestroyAction destroyAction = (DestroyAction) action;
                    if (!checkNetId(session, inventory, destroyAction.getSource())) {
                        return rejectRequest(request);
                    }
                    if (isCraftingGrid(destroyAction.getSource())) {
                        return rejectRequest(request, false);
                    }

                    if (!isCursor(destroyAction.getSource())) {

                        int javaSlot = bedrockSlotToJava(destroyAction.getSource());
                        GeyserItemStack existingItem = inventory.getItem(javaSlot);
                        existingItem.sub(destroyAction.getCount());
                        affectedSlots.add(javaSlot);
                    } else {

                        playerInv.getCursor().sub(destroyAction.getCount());
                    }
                }
                default -> {
                    session.getGeyser().getLogger().error("Unknown crafting state induced by " + session.bedrockUsername());
                    return rejectRequest(request);
                }
            }
        }

        IntIterator it = affectedSlots.iterator();
        while (it.hasNext()) {
            int slot = it.nextInt();
            sendCreativeAction(session, inventory, slot);
        }
        return acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }

    override protected ItemStackResponse translateCreativeRequest(GeyserSession session, PlayerInventory inventory, ItemStackRequest request) {
        ItemStack javaCreativeItem = null;
        bool bundle = false;
        IntSet affectedSlots = new IntOpenHashSet();
        CraftState craftState = CraftState.START;
        for (ItemStackRequestAction action : request.getActions()) {
            switch (action.getType()) {
                case CRAFT_CREATIVE: {
                    CraftCreativeAction creativeAction = (CraftCreativeAction) action;
                    if (craftState != CraftState.START) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.RECIPE_ID;

                    int creativeId = creativeAction.getCreativeItemNetworkId() - 1;
                    List<CreativeItemData> creativeItems = session.getItemMappings().getCreativeItems();
                    if (creativeId < 0 || creativeId >= creativeItems.size()) {
                        return rejectRequest(request);
                    }

                    CreativeItemData creativeItem = creativeItems.get(creativeId);
                    javaCreativeItem = ItemTranslator.translateToJava(session, creativeItem.getItem());
                    break;
                }
                case CRAFT_RESULTS_DEPRECATED: {
                    if (craftState != CraftState.RECIPE_ID) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.DEPRECATED;
                    break;
                }
                case DESTROY: {
                    DestroyAction destroyAction = (DestroyAction) action;
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
                    TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                    if (!(craftState == CraftState.DEPRECATED || craftState == CraftState.TRANSFER)) {
                        return rejectRequest(request);
                    }
                    craftState = CraftState.TRANSFER;

                    if (transferAction.getSource().getContainerName().getContainer() != ContainerSlotType.CREATED_OUTPUT) {
                        return rejectRequest(request);
                    }

                    if (isCursor(transferAction.getDestination())) {
                        if (session.getPlayerInventory().getCursor().isEmpty()) {
                            GeyserItemStack newItemStack = GeyserItemStack.from(session, javaCreativeItem);
                            session.getBundleCache().initialize(newItemStack);
                            newItemStack.setAmount(transferAction.getCount());
                            session.getPlayerInventory().setCursor(newItemStack, session);
                            bundle = newItemStack.getBundleData() != null;
                        } else {
                            session.getPlayerInventory().getCursor().add(transferAction.getCount());
                        }

                    } else {
                        int destSlot = bedrockSlotToJava(transferAction.getDestination());
                        if (inventory.getItem(destSlot).isEmpty()) {
                            GeyserItemStack newItemStack = GeyserItemStack.from(session, javaCreativeItem);
                            session.getBundleCache().initialize(newItemStack);
                            newItemStack.setAmount(transferAction.getCount());
                            inventory.setItem(destSlot, newItemStack, session);
                            bundle = newItemStack.getBundleData() != null;
                        } else {
                            inventory.getItem(destSlot).add(transferAction.getCount());
                        }
                        affectedSlots.add(destSlot);
                    }
                    break;
                }
                case DROP: {

                    if (craftState != CraftState.DEPRECATED) {
                        return rejectRequest(request);
                    }

                    DropAction dropAction = (DropAction) action;
                    if (dropAction.getSource().getContainerName().getContainer() != ContainerSlotType.CREATED_OUTPUT || dropAction.getSource().getSlot() != 50) {
                        return rejectRequest(request);
                    }

                    ItemStack dropStack;
                    if (dropAction.getCount() == javaCreativeItem.getAmount()) {
                        dropStack = javaCreativeItem;
                    } else {

                        dropStack = new ItemStack(javaCreativeItem.getId(), dropAction.getCount(), javaCreativeItem.getDataComponentsPatch());
                    }
                    ServerboundSetCreativeModeSlotPacket creativeDropPacket = new ServerboundSetCreativeModeSlotPacket((short)-1, dropStack);
                    session.sendDownstreamGamePacket(creativeDropPacket);
                    break;
                }
                default:
                    return rejectRequest(request);
            }
        }

        IntIterator it = affectedSlots.iterator();
        while (it.hasNext()) {
            int slot = it.nextInt();
            sendCreativeAction(session, inventory, slot);
        }




        return bundle ? rejectRequest(request, false) : acceptRequest(request, makeContainerEntries(session, inventory, affectedSlots));
    }

    private static void sendCreativeAction(GeyserSession session, Inventory inventory, int slot) {
        GeyserItemStack item = inventory.getItem(slot);
        ItemStack itemStack = item.isEmpty() ? new ItemStack(-1, 0, null) : item.getItemStack();

        ServerboundSetCreativeModeSlotPacket creativePacket = new ServerboundSetCreativeModeSlotPacket((short)slot, itemStack);
        session.sendDownstreamGamePacket(creativePacket);
    }

    private static bool isCraftingGrid(ItemStackRequestSlotData slotInfoData) {
        return slotInfoData.getContainerName().getContainer() == ContainerSlotType.CRAFTING_INPUT;
    }

    override public PlayerInventory createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        throw new UnsupportedOperationException();
    }

    override public bool canReuseInventory(GeyserSession session, Inventory inventory, Inventory previous) {
        return true;
    }

    override public bool prepareInventory(GeyserSession session, PlayerInventory inventory) {
        return true;
    }

    override public void openInventory(GeyserSession session, PlayerInventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) 0);
        containerOpenPacket.setType(org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.INVENTORY);
        containerOpenPacket.setUniqueEntityId(-1);
        containerOpenPacket.setBlockPosition(session.getPlayerEntity().bedrockPosition().toInt());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    override public void closeInventory(GeyserSession session, PlayerInventory inventory, bool force) {
        if (force) {
            Vector3i pos = session.getPlayerEntity().bedrockPosition().toInt();

            UpdateBlockPacket packet = new UpdateBlockPacket();
            packet.setBlockPosition(pos);
            packet.setDefinition(session.getBlockMappings().getNetherPortalBlock());
            packet.setDataLayer(0);
            packet.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
            session.sendUpstreamPacket(packet);

            session.scheduleInEventLoop(() -> {
                BlockDefinition definition = session.getBlockMappings().getBedrockBlock(session.getGeyser().getWorldManager().blockAt(session, pos));
                packet.setDefinition(definition);
                packet.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
                session.sendUpstreamPacket(packet);
            }, 50, TimeUnit.MILLISECONDS);
        }
    }

    override public void updateProperty(GeyserSession session, PlayerInventory inventory, int key, int value) {
    }
}
