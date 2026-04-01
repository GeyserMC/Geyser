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

#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Generic3X3Container"
#include "org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"


public class Generic3X3InventoryTranslator extends AbstractBlockInventoryTranslator<Generic3X3Container> {
    public Generic3X3InventoryTranslator() {
        super(9, Blocks.DISPENSER, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.DISPENSER, ContainerInventoryUpdater.INSTANCE,
                Blocks.DROPPER);
    }

    override public Generic3X3Container createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new Generic3X3Container(session, name, windowId, this.size, containerType);
    }

    override public void openInventory(GeyserSession session, Generic3X3Container container) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) container.getBedrockId());

        containerOpenPacket.setType(container.isDropper() ? org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.DROPPER : org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.DISPENSER);
        containerOpenPacket.setBlockPosition(container.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(container.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot, Generic3X3Container container) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(ContainerSlotType.LEVEL_ENTITY, javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot, container);
    }

    override public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(Generic3X3Container container) {
        return container.isDropper() ? org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.DROPPER :
            org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.DISPENSER;
    }
}
