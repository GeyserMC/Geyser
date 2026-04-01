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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeOptionalAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.geysermc.geyser.inventory.AnvilContainer"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.updater.AnvilInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"

#include "java.util.Objects"

public class AnvilInventoryTranslator extends AbstractBlockInventoryTranslator<AnvilContainer> {
    public AnvilInventoryTranslator() {
        super(3, Blocks.ANVIL, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.ANVIL, AnvilInventoryUpdater.INSTANCE,
                Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
    }

    override protected bool shouldHandleRequestFirst(ItemStackRequestAction action, AnvilContainer container) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE_OPTIONAL;
    }

    override protected ItemStackResponse translateSpecialRequest(GeyserSession session, AnvilContainer container, ItemStackRequest request) {

        CraftRecipeOptionalAction data = (CraftRecipeOptionalAction) request.getActions()[0];

        if (request.getFilterStrings().length != 0) {

            std::string name = request.getFilterStrings()[data.getFilteredStringIndex()];
            if (!Objects.equals(name, container.getNewName())) {
                container.checkForRename(session, name);
            }
        }

        return super.translateRequest(session, container, request);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case ANVIL_INPUT -> 0;
            case ANVIL_MATERIAL -> 1;
            case ANVIL_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, AnvilContainer container) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_INPUT, 1);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_MATERIAL, 2);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot, container);
        };
    }

    override public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    override public AnvilContainer createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new AnvilContainer(session, name, windowId, this.size, containerType);
    }

    override public void updateProperty(GeyserSession session, AnvilContainer container, int key, int value) {

        if (key != 0) return;
        container.setJavaLevelCost(value);
        container.setUseJavaLevelCost(true);
        updateSlot(session, container, 1);
    }

    override public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(AnvilContainer container) {
        return null;
    }
}
