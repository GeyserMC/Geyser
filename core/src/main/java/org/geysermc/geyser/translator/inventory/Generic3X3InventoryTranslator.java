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

import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.geysermc.geyser.inventory.Generic3X3Container;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater;

/**
 * Droppers and dispensers
 */
public class Generic3X3InventoryTranslator extends AbstractBlockInventoryTranslator {
    public Generic3X3InventoryTranslator() {
        super(9, "minecraft:dispenser[facing=north,triggered=false]", com.nukkitx.protocol.bedrock.data.inventory.ContainerType.DISPENSER, ContainerInventoryUpdater.INSTANCE,
                "minecraft:dropper");
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new Generic3X3Container(name, windowId, this.size, containerType, playerInventory);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) inventory.getId());
        // Required for opening the real block - otherwise, if the container type is incorrect, it refuses to open
        containerOpenPacket.setType(((Generic3X3Container) inventory).isDropper() ? com.nukkitx.protocol.bedrock.data.inventory.ContainerType.DROPPER : com.nukkitx.protocol.bedrock.data.inventory.ContainerType.DISPENSER);
        containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(ContainerSlotType.CONTAINER, javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot);
    }
}
