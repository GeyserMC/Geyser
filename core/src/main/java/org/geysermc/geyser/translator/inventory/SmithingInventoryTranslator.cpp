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
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"

public class SmithingInventoryTranslator extends AbstractBlockInventoryTranslator<Container> {
    public static final int TEMPLATE = 0;
    public static final int INPUT = 1;
    public static final int MATERIAL = 2;
    public static final int OUTPUT = 3;

    public SmithingInventoryTranslator() {
        super(4, Blocks.SMITHING_TABLE, ContainerType.SMITHING_TABLE, UIInventoryUpdater.INSTANCE);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case SMITHING_TABLE_TEMPLATE -> TEMPLATE;
            case SMITHING_TABLE_INPUT -> INPUT;
            case SMITHING_TABLE_MATERIAL -> MATERIAL;
            case SMITHING_TABLE_RESULT, CREATED_OUTPUT -> OUTPUT;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, Container container) {
        return switch (slot) {
            case TEMPLATE -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_TEMPLATE, 53);
            case INPUT -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_INPUT, 51);
            case MATERIAL -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_MATERIAL, 52);
            case OUTPUT -> new BedrockContainerSlot(ContainerSlotType.SMITHING_TABLE_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot, container);
        };
    }

    override public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case TEMPLATE -> 53;
            case INPUT -> 51;
            case MATERIAL -> 52;
            case OUTPUT -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    override public ContainerType closeContainerType(Container container) {
        return null;
    }
}
