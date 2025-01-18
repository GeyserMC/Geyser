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
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.geysermc.geyser.inventory.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

public abstract class BaseInventoryTranslator extends InventoryTranslator {
    public BaseInventoryTranslator(int size) {
        super(size);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        //
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        int slotnum = slotInfoData.getSlot();
        switch (slotInfoData.getContainerName().getContainer()) {
            case HOTBAR_AND_INVENTORY:
            case HOTBAR:
            case INVENTORY:
                //hotbar
                if (slotnum >= 9) {
                    return slotnum + this.size - 9;
                } else {
                    return slotnum + this.size + 27;
                }
        }
        return slotnum;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot >= this.size) {
            final int tmp = slot - this.size;
            if (tmp < 27) {
                return tmp + 9;
            } else {
                return tmp - 27;
            }
        }
        return slot;
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot >= this.size) {
            final int tmp = slot - this.size;
            if (tmp < 27) {
                return new BedrockContainerSlot(ContainerSlotType.INVENTORY, tmp + 9);
            } else {
                return new BedrockContainerSlot(ContainerSlotType.HOTBAR, tmp - 27);
            }
        }
        throw new IllegalArgumentException("Unknown bedrock slot");
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        return SlotType.NORMAL;
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new Container(name, windowId, this.size, containerType, playerInventory);
    }
}
