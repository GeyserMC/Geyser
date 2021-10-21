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
import org.geysermc.connector.inventory.AnvilContainer;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.updater.AnvilInventoryUpdater;

public class AnvilInventoryTranslator extends AbstractBlockInventoryTranslator {
    public AnvilInventoryTranslator() {
        super(3, "minecraft:anvil[facing=north]", ContainerType.ANVIL, AnvilInventoryUpdater.INSTANCE,
                "minecraft:chipped_anvil", "minecraft:damaged_anvil");
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        return switch (slotInfoData.getContainer()) {
            case ANVIL_INPUT -> 0;
            case ANVIL_MATERIAL -> 1;
            case ANVIL_RESULT, CREATIVE_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_INPUT, 1);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_MATERIAL, 2);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.ANVIL_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public Inventory createInventory(String name, int windowId, WindowType windowType, PlayerInventory playerInventory) {
        return new AnvilContainer(name, windowId, this.size, windowType, playerInventory);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        if (key == 0) {
            AnvilContainer anvilContainer = (AnvilContainer) inventory;
            anvilContainer.setJavaLevelCost(value);
            updateInventory(session, inventory); // TODO maybe just update the input slot?
        }
    }
}
