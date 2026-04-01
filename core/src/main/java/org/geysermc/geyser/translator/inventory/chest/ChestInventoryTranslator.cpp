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

package org.geysermc.geyser.translator.inventory.chest;

#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.updater.ChestInventoryUpdater"
#include "org.geysermc.geyser.inventory.updater.InventoryUpdater"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.BaseInventoryTranslator"

public abstract class ChestInventoryTranslator<Type extends Container> extends BaseInventoryTranslator<Type> {
    private final InventoryUpdater updater;

    public ChestInventoryTranslator(int size, int paddedSize) {
        super(size);
        this.updater = new ChestInventoryUpdater(paddedSize);
    }

    override protected bool shouldRejectItemPlace(GeyserSession session, Type container, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {

        if (bedrockSourceContainer == slotType(container) && javaSourceSlot >= this.size) {
            return true;
        }
        return bedrockDestinationContainer == slotType(container) && javaDestinationSlot >= this.size;
    }

    override public bool requiresOpeningDelay(GeyserSession session, Type container) {
        return !container.isUsingRealBlock();
    }

    override public void updateInventory(GeyserSession session, Type container) {
        updater.updateInventory(this, session, container);
    }

    override public void updateSlot(GeyserSession session, Type container, int slot) {
        updater.updateSlot(this, session, container, slot);
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot, Type inventory) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(slotType(inventory), javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot, inventory);
    }


    protected ContainerSlotType slotType(Type type) {
        return ContainerSlotType.LEVEL_ENTITY;
    }
}
