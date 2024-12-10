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
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeOptionalAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.geysermc.geyser.inventory.AnvilContainer;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.updater.AnvilInventoryUpdater;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

import java.util.Objects;

public class AnvilInventoryTranslator extends AbstractBlockInventoryTranslator {
    public AnvilInventoryTranslator() {
        super(3, Blocks.ANVIL, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.ANVIL, AnvilInventoryUpdater.INSTANCE,
                Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
    }

    @Override
    protected boolean shouldHandleRequestFirst(ItemStackRequestAction action, Inventory inventory) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE_OPTIONAL;
    }

    @Override
    protected ItemStackResponse translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        // Guarded by shouldHandleRequestFirst check
        CraftRecipeOptionalAction data = (CraftRecipeOptionalAction) request.getActions()[0];
        AnvilContainer container = (AnvilContainer) inventory;

        if (request.getFilterStrings().length != 0) {
            // Required as of 1.18.30 - FilterTextPackets no longer appear to be sent
            String name = request.getFilterStrings()[data.getFilteredStringIndex()];
            if (!Objects.equals(name, container.getNewName())) { // TODO is this still necessary after pre-1.19.50 support is dropped?
                container.checkForRename(session, name);
            }
        }

        return super.translateRequest(session, inventory, request);
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case ANVIL_INPUT -> 0;
            case ANVIL_MATERIAL -> 1;
            case ANVIL_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_INPUT, 1);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_MATERIAL, 2);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new AnvilContainer(name, windowId, this.size, containerType, playerInventory);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        // The only property sent by Java is key 0 which is the level cost
        if (key != 0) return;
        AnvilContainer anvilContainer = (AnvilContainer) inventory;
        anvilContainer.setJavaLevelCost(value);
        anvilContainer.setUseJavaLevelCost(true);
        updateSlot(session, anvilContainer, 1);
    }
}
