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

#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.SlotType"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"

public class CraftingInventoryTranslator extends AbstractBlockInventoryTranslator<Container> {
    public CraftingInventoryTranslator() {
        super(10, Blocks.CRAFTING_TABLE, ContainerType.WORKBENCH, UIInventoryUpdater.INSTANCE);
    }

    override public int getGridSize() {
        return 9;
    }

    override public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 0) {
            return SlotType.OUTPUT;
        }
        return SlotType.NORMAL;
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, Container container) {
        if (isCraftingGrid(slot)) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_INPUT, slot + 31);
        }
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_OUTPUT, 0);
        }
        return super.javaSlotToBedrockContainer(slot, container);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.CRAFTING_INPUT) {


            return slotInfoData.getSlot() - 31;
        }
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.CRAFTING_OUTPUT || slotInfoData.getContainerName().getContainer() == ContainerSlotType.CREATED_OUTPUT) {
            return 0;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    override public int javaSlotToBedrock(int slot) {
        if (slot < size) {
            return slot == 0 ? 50 : slot + 31;
        }
        return super.javaSlotToBedrock(slot);
    }

    public static bool isCraftingGrid(int slot) {
        return slot >= 1 && slot <= 9;
    }

    override public ContainerType closeContainerType(Container container) {
        return null;
    }
}
