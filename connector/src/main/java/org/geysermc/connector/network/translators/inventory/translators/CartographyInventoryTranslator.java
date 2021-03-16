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

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.geysermc.connector.inventory.CartographyContainer;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.updater.UIInventoryUpdater;

public class CartographyInventoryTranslator extends AbstractBlockInventoryTranslator {
    public CartographyInventoryTranslator() {
        super(3, "minecraft:cartography_table", ContainerType.CARTOGRAPHY, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public boolean shouldRejectItemPlace(GeyserSession session, Inventory inventory, ContainerSlotType bedrockSourceContainer,
                                         int javaSourceSlot, ContainerSlotType bedrockDestinationContainer, int javaDestinationSlot) {
        if (javaDestinationSlot == 0) {
            // Bedrock Edition can use paper or an empty map in slot 0
            GeyserItemStack itemStack = javaSourceSlot == -1 ? session.getPlayerInventory().getCursor() : inventory.getItem(javaSourceSlot);
            return itemStack.getItemEntry().getJavaIdentifier().equals("minecraft:paper") || itemStack.getItemEntry().getJavaIdentifier().equals("minecraft:map");
        } else if (javaDestinationSlot == 1) {
            // Bedrock Edition can use a compass to create locator maps, or use a filled map, in the ADDITIONAL slot
            GeyserItemStack itemStack = javaSourceSlot == -1 ? session.getPlayerInventory().getCursor() : inventory.getItem(javaSourceSlot);
            return itemStack.getItemEntry().getJavaIdentifier().equals("minecraft:compass") || itemStack.getItemEntry().getJavaIdentifier().equals("minecraft:filled_map");
        }
        return false;
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        switch (slotInfoData.getContainer()) {
            case CARTOGRAPHY_INPUT:
                return 0;
            case CARTOGRAPHY_ADDITIONAL:
                return 1;
            case CARTOGRAPHY_RESULT:
            case CREATIVE_OUTPUT:
                return 2;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        switch (slot) {
            case 0:
                return new BedrockContainerSlot(ContainerSlotType.CARTOGRAPHY_INPUT, 12);
            case 1:
                return new BedrockContainerSlot(ContainerSlotType.CARTOGRAPHY_ADDITIONAL, 13);
            case 2:
                return new BedrockContainerSlot(ContainerSlotType.CARTOGRAPHY_RESULT, 50);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 12;
            case 1:
                return 13;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public Inventory createInventory(String name, int windowId, WindowType windowType, PlayerInventory playerInventory) {
        return new CartographyContainer(name, windowId, this.size, windowType, playerInventory);
    }
}
