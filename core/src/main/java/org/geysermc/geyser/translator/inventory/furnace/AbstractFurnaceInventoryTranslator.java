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

package org.geysermc.geyser.translator.inventory.furnace;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.AbstractBlockInventoryTranslator;

public abstract class AbstractFurnaceInventoryTranslator extends AbstractBlockInventoryTranslator {
    AbstractFurnaceInventoryTranslator(Block javaBlock, ContainerType containerType) {
        super(3, javaBlock.defaultBlockState().withValue(Properties.LIT, false), containerType, ContainerInventoryUpdater.INSTANCE);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        ContainerSetDataPacket dataPacket = new ContainerSetDataPacket();
        dataPacket.setWindowId((byte) inventory.getBedrockId());
        switch (key) {
            case 0:
                dataPacket.setProperty(ContainerSetDataPacket.FURNACE_LIT_TIME);
                break;
            case 1:
                dataPacket.setProperty(ContainerSetDataPacket.FURNACE_LIT_DURATION);
                break;
            case 2:
                dataPacket.setProperty(ContainerSetDataPacket.FURNACE_TICK_COUNT);
                break;
            default:
                return;
        }
        dataPacket.setValue(value);
        session.sendUpstreamPacket(dataPacket);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2)
            return SlotType.FURNACE_OUTPUT;
        return SlotType.NORMAL;
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot == 1) {
            return new BedrockContainerSlot(ContainerSlotType.FURNACE_FUEL, javaSlotToBedrock(slot));
        }
        if (slot == 2) {
            return new BedrockContainerSlot(ContainerSlotType.FURNACE_RESULT, javaSlotToBedrock(slot));
        }
        return super.javaSlotToBedrockContainer(slot);
    }
}
