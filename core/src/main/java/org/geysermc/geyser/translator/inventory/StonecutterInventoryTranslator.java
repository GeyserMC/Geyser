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

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.geysermc.geyser.inventory.*;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket;

public class StonecutterInventoryTranslator extends AbstractBlockInventoryTranslator {
    public StonecutterInventoryTranslator() {
        super(2, Blocks.STONECUTTER, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.STONECUTTER, UIInventoryUpdater.INSTANCE);
    }

    @Override
    protected boolean shouldHandleRequestFirst(ItemStackRequestAction action, Inventory inventory) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE;
    }

    @Override
    protected ItemStackResponse translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        // Guarded by shouldHandleRequestFirst
        CraftRecipeAction data = (CraftRecipeAction) request.getActions()[0];

        // Look up all possible options of cutting from this ID
        GeyserStonecutterData craftingData = session.getStonecutterRecipes().get(data.getRecipeNetworkId());
        if (craftingData == null) {
            return rejectRequest(request);
        }

        StonecutterContainer container = (StonecutterContainer) inventory;
        ItemStack javaOutput = craftingData.output();
        int button = craftingData.buttonId();

        // If we've already pressed the button with this item, no need to press it again!
        if (container.getStonecutterButton() != button) {
            // Getting the index of the item in the Java stonecutter list
            ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(inventory.getJavaId(), button);
            session.sendDownstreamGamePacket(packet);
            container.setStonecutterButton(button);
        }

        if (inventory.getItem(1).getJavaId() != javaOutput.getId()) {
            // We don't know there is an output here, so we tell ourselves that there is
            inventory.setItem(1, GeyserItemStack.from(javaOutput), session);
        }

        return translateRequest(session, inventory, request);
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case STONECUTTER_INPUT -> 0;
            case STONECUTTER_RESULT, CREATED_OUTPUT -> 1;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.STONECUTTER_INPUT, 3);
        }
        if (slot == 1) {
            return new BedrockContainerSlot(ContainerSlotType.STONECUTTER_RESULT, 50);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 3;
        }
        if (slot == 1) {
            return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 1) {
            return SlotType.OUTPUT;
        }
        return super.getSlotType(javaSlot);
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new StonecutterContainer(name, windowId, this.size, containerType, playerInventory);
    }
}
