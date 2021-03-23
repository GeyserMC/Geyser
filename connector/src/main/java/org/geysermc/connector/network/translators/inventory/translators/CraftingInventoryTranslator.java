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

package org.geysermc.connector.network.translators.inventory.translators;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.network.translators.inventory.updater.UIInventoryUpdater;

public class CraftingInventoryTranslator extends AbstractBlockInventoryTranslator {
    public CraftingInventoryTranslator() {
        super(10, "minecraft:crafting_table", ContainerType.WORKBENCH, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 0) {
            return SlotType.OUTPUT;
        }
        return SlotType.NORMAL;
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot >= 1 && slot <= 9) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_INPUT, slot + 31);
        }
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTING_OUTPUT, 0);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        if (slotInfoData.getContainer() == ContainerSlotType.CRAFTING_INPUT) {
            // Java goes from 1 - 9, left to right then up to down
            // Bedrock is the same, but it starts from 32.
            return slotInfoData.getSlot() - 31;
        }
        if (slotInfoData.getContainer() == ContainerSlotType.CRAFTING_OUTPUT || slotInfoData.getContainer() == ContainerSlotType.CREATIVE_OUTPUT) {
            return 0;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot < size) {
            return slot == 0 ? 50 : slot + 31;
        }
        return super.javaSlotToBedrock(slot);
    }
}
