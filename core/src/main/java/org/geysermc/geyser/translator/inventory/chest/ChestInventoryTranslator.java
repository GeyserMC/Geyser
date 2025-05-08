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

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.updater.ChestInventoryUpdater;
import org.geysermc.geyser.inventory.updater.InventoryUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.BaseInventoryTranslator;

public abstract class ChestInventoryTranslator<Type extends Container> extends BaseInventoryTranslator<Type> {
    private final InventoryUpdater updater;

    public ChestInventoryTranslator(int size, int paddedSize) {
        super(size);
        this.updater = new ChestInventoryUpdater(paddedSize);
    }

    @Override
    protected boolean shouldRejectItemPlace(GeyserSession session, Type container, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        // Reject any item placements that occur in the unusable inventory space
        if (bedrockSourceContainer == slotType(container) && javaSourceSlot >= this.size) {
            return true;
        }
        return bedrockDestinationContainer == slotType(container) && javaDestinationSlot >= this.size;
    }

    @Override
    public boolean requiresOpeningDelay(GeyserSession session, Type container) {
        return !container.isUsingRealBlock();
    }

    @Override
    public void updateInventory(GeyserSession session, Type container) {
        updater.updateInventory(this, session, container);
    }

    @Override
    public void updateSlot(GeyserSession session, Type container, int slot) {
        updater.updateSlot(this, session, container, slot);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot, Type inventory) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(slotType(inventory), javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot, inventory);
    }

    /**
     * Overridden by the SingleChestInventoryTranslator in case barrels are used.
     * Bedrock uses the ContainerSlotType.BARREL for those.
     */
    protected ContainerSlotType slotType(Type type) {
        return ContainerSlotType.LEVEL_ENTITY;
    }
}
