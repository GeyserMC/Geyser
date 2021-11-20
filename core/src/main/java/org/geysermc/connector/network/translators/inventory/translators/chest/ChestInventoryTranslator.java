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

package org.geysermc.connector.network.translators.inventory.translators.chest;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.translators.BaseInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.updater.ChestInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

public abstract class ChestInventoryTranslator extends BaseInventoryTranslator {
    private final InventoryUpdater updater;

    public ChestInventoryTranslator(int size, int paddedSize) {
        super(size);
        this.updater = new ChestInventoryUpdater(paddedSize);
    }

    @Override
    public boolean shouldRejectItemPlace(GeyserSession session, Inventory inventory, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        // Reject any item placements that occur in the unusable inventory space
        if (bedrockSourceContainer == ContainerSlotType.CONTAINER && javaSourceSlot >= this.size) {
            return true;
        }
        return bedrockDestinationContainer == ContainerSlotType.CONTAINER && javaDestinationSlot >= this.size;
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updater.updateInventory(this, session, inventory);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        updater.updateSlot(this, session, inventory, slot);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(ContainerSlotType.CONTAINER, javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot);
    }
}
