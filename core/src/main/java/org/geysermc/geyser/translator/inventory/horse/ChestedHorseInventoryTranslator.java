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

package org.geysermc.geyser.translator.inventory.horse;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Arrays;

public abstract class ChestedHorseInventoryTranslator extends AbstractHorseInventoryTranslator {
    private final int chestSize;
    private final int equipSlot;

    /**
     * @param size the total Java size of the inventory
     * @param equipSlot the Java equipment slot. Java always has two slots - one for armor and one for saddle. Chested horses
     *                  on Bedrock only acknowledge one slot.
     */
    public ChestedHorseInventoryTranslator(int size, int equipSlot) {
        super(size);
        this.chestSize = size - 2;
        this.equipSlot = equipSlot;
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.HORSE_EQUIP) {
            return this.equipSlot;
        }
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.LEVEL_ENTITY) {
            return slotInfoData.getSlot() + 1;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot == this.equipSlot) {
            return new BedrockContainerSlot(ContainerSlotType.HORSE_EQUIP, 0);
        }
        if (slot <= this.size - 1) { // Accommodate for the lack of one slot (saddle or armor)
            return new BedrockContainerSlot(ContainerSlotType.LEVEL_ENTITY, slot - 1);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot == 0 && this.equipSlot == 0) {
            return 0;
        }
        if (slot <= this.size - 1) {
            return slot - 1;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        ItemData[] bedrockItems = new ItemData[36];
        for (int i = 0; i < 36; i++) {
            final int offset = i < 9 ? 27 : -9;
            bedrockItems[i] = inventory.getItem(this.size + i + offset).getItemData(session);
        }
        InventoryContentPacket contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(ContainerId.INVENTORY);
        contentPacket.setContents(Arrays.asList(bedrockItems));
        session.sendUpstreamPacket(contentPacket);

        ItemData[] horseItems = new ItemData[chestSize + 1];
        // Manually specify the first slot - Java always has two slots (armor and saddle) and one is invisible.
        // Bedrock doesn't have this invisible slot.
        horseItems[0] = inventory.getItem(this.equipSlot).getItemData(session);
        for (int i = 1; i < horseItems.length; i++) {
            horseItems[i] = inventory.getItem(i + 1).getItemData(session);
        }

        InventoryContentPacket horseContentsPacket = new InventoryContentPacket();
        horseContentsPacket.setContainerId(inventory.getBedrockId());
        horseContentsPacket.setContents(Arrays.asList(horseItems));
        session.sendUpstreamPacket(horseContentsPacket);
    }
}
