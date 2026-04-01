/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TakeAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.InventoryUtils"

#include "java.util.function.IntFunction"


public class OldSmithingTableTranslator extends AbstractBlockInventoryTranslator<Container> {

    public static final OldSmithingTableTranslator INSTANCE = new OldSmithingTableTranslator();

    private static final IntFunction<ItemData> UPGRADE_TEMPLATE = InventoryUtils.getUpgradeTemplate();

    private OldSmithingTableTranslator() {
        super(3, Blocks.SMITHING_TABLE, ContainerType.SMITHING_TABLE, UIInventoryUpdater.INSTANCE);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case SMITHING_TABLE_INPUT -> 0;
            case SMITHING_TABLE_MATERIAL -> 1;
            case SMITHING_TABLE_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, Container container) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_INPUT, 51);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_MATERIAL, 52);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot, container);
        };
    }

    override public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 51;
            case 1 -> 52;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    override public bool shouldHandleRequestFirst(ItemStackRequestAction action, Container container) {
        return true;
    }

    override protected ItemStackResponse translateSpecialRequest(GeyserSession session, Container container, ItemStackRequest request) {
        for (var action: request.getActions()) {
            switch (action.getType()) {
                case DROP -> {
                   if (isInvalidAction(((DropAction) action).getSource())) {
                       return rejectRequest(request, false);
                   }
                }
                case TAKE -> {
                    if (isInvalidAction(((TakeAction) action).getSource()) ||
                            isInvalidAction(((TakeAction) action).getDestination())) {
                        return rejectRequest(request, false);
                    }
                }
                case SWAP -> {
                    if (isInvalidAction(((SwapAction) action).getSource()) ||
                            isInvalidAction(((SwapAction) action).getDestination())) {
                        return rejectRequest(request, false);
                    }
                }
                case PLACE -> {
                    if (isInvalidAction(((PlaceAction) action).getSource()) ||
                            isInvalidAction(((PlaceAction) action).getDestination())) {
                        return rejectRequest(request, false);
                    }
                }
            }
        }

        return super.translateRequest(session, container, request);
    }

    private bool isInvalidAction(ItemStackRequestSlotData slotData) {
        return slotData.getContainerName().getContainer().equals(ContainerSlotType.SMITHING_TABLE_TEMPLATE);
    }

    override public void openInventory(GeyserSession session, Container container) {
        super.openInventory(session, container);



        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(ContainerId.UI);
        slotPacket.setSlot(53);
        slotPacket.setItem(UPGRADE_TEMPLATE.apply(session.getUpstream().getProtocolVersion()));
        session.sendUpstreamPacket(slotPacket);
    }

    override public ContainerType closeContainerType(Container container) {
        return null;
    }
}
