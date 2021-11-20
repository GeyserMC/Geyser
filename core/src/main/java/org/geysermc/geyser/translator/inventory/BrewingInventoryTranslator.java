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

package org.geysermc.geyser.translator.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater;

public class BrewingInventoryTranslator extends AbstractBlockInventoryTranslator {
    public BrewingInventoryTranslator() {
        super(5, "minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]", ContainerType.BREWING_STAND, ContainerInventoryUpdater.INSTANCE);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        super.openInventory(session, inventory);
        ContainerSetDataPacket dataPacket = new ContainerSetDataPacket();
        dataPacket.setWindowId((byte) inventory.getId());
        dataPacket.setProperty(ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL);
        dataPacket.setValue(20);
        session.sendUpstreamPacket(dataPacket);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        ContainerSetDataPacket dataPacket = new ContainerSetDataPacket();
        dataPacket.setWindowId((byte) inventory.getId());
        switch (key) {
            case 0:
                dataPacket.setProperty(ContainerSetDataPacket.BREWING_STAND_BREW_TIME);
                break;
            case 1:
                dataPacket.setProperty(ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT);
                break;
            default:
                return;
        }
        dataPacket.setValue(value);
        session.sendUpstreamPacket(dataPacket);
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        if (slotInfoData.getContainer() == ContainerSlotType.BREWING_INPUT) {
            // Ingredient
            return 3;
        }
        if (slotInfoData.getContainer() == ContainerSlotType.BREWING_RESULT) {
            // Potions
            return slotInfoData.getSlot() - 1;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 0;
            case 4 -> 4;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0, 1, 2 -> new BedrockContainerSlot(ContainerSlotType.BREWING_RESULT, javaSlotToBedrock(slot));
            case 3 -> new BedrockContainerSlot(ContainerSlotType.BREWING_INPUT, 0);
            case 4 -> new BedrockContainerSlot(ContainerSlotType.BREWING_FUEL, 4);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }
}
