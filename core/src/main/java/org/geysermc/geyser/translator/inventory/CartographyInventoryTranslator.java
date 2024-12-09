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
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

public class CartographyInventoryTranslator extends AbstractBlockInventoryTranslator {
    public CartographyInventoryTranslator() {
        super(3, Blocks.CARTOGRAPHY_TABLE, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.CARTOGRAPHY, UIInventoryUpdater.INSTANCE);
    }

    @Override
    protected boolean shouldRejectItemPlace(GeyserSession session, Inventory inventory, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        if (javaDestinationSlot == 0) {
            // Bedrock Edition can use paper or an empty map in slot 0
            GeyserItemStack itemStack = javaSourceSlot == -1 ? session.getPlayerInventory().getCursor() : inventory.getItem(javaSourceSlot);
            return itemStack.asItem() == Items.PAPER || itemStack.asItem() == Items.MAP;
        } else if (javaDestinationSlot == 1) {
            // Bedrock Edition can use a compass to create locator maps, or use a filled map, in the ADDITIONAL slot
            GeyserItemStack itemStack = javaSourceSlot == -1 ? session.getPlayerInventory().getCursor() : inventory.getItem(javaSourceSlot);
            return itemStack.asItem() == Items.COMPASS || itemStack.asItem() == Items.FILLED_MAP;
        }
        return false;
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case CARTOGRAPHY_INPUT -> 0;
            case CARTOGRAPHY_ADDITIONAL -> 1;
            case CARTOGRAPHY_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.CARTOGRAPHY_INPUT, 12);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.CARTOGRAPHY_ADDITIONAL, 13);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.CARTOGRAPHY_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 12;
            case 1 -> 13;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new CartographyContainer(name, windowId, this.size, containerType, playerInventory);
    }
}
