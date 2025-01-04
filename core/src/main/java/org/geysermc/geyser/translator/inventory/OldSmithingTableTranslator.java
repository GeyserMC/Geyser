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

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TakeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.function.IntFunction;

/**
 * Translator for smithing tables for pre-1.20 servers.
 * This adapts ViaVersion's furnace ui to the 1.20+ smithing table; with the addition of a fake smithing template so Bedrock clients can use it.
 */
public class OldSmithingTableTranslator extends AbstractBlockInventoryTranslator {

    public static final OldSmithingTableTranslator INSTANCE = new OldSmithingTableTranslator();

    private static final IntFunction<ItemData> UPGRADE_TEMPLATE = InventoryUtils.getUpgradeTemplate();

    private OldSmithingTableTranslator() {
        super(3, Blocks.SMITHING_TABLE, ContainerType.SMITHING_TABLE, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case SMITHING_TABLE_INPUT -> 0;
            case SMITHING_TABLE_MATERIAL -> 1;
            case SMITHING_TABLE_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_INPUT, 51);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_MATERIAL, 52);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 51;
            case 1 -> 52;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public boolean shouldHandleRequestFirst(ItemStackRequestAction action, Inventory inventory) {
        return true;
    }

    @Override
    protected ItemStackResponse translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
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
        // Allow everything else that doesn't involve the fake template
        return super.translateRequest(session, inventory, request);
    }

    private boolean isInvalidAction(ItemStackRequestSlotData slotData) {
        return slotData.getContainerName().getContainer().equals(ContainerSlotType.SMITHING_TABLE_TEMPLATE);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        super.openInventory(session, inventory);

        // pre-1.20 server has no concept of templates, but we are working with a 1.20 client
        // put a fake netherite upgrade template in the template slot otherwise the client doesn't recognize a valid recipe
        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(ContainerId.UI);
        slotPacket.setSlot(53);
        slotPacket.setItem(UPGRADE_TEMPLATE.apply(session.getUpstream().getProtocolVersion()));
        session.sendUpstreamPacket(slotPacket);
    }
}
