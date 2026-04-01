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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.holder.BlockInventoryHolder"
#include "org.geysermc.geyser.inventory.updater.ContainerInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.Direction"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

public class ShulkerInventoryTranslator extends AbstractBlockInventoryTranslator<Container> {
    public ShulkerInventoryTranslator() {

        super(27, new BlockInventoryHolder(Blocks.SHULKER_BOX.defaultBlockState().withValue(Properties.FACING, Direction.NORTH), ContainerType.CONTAINER) {
            private final BlockEntityTranslator shulkerBoxTranslator = Registries.BLOCK_ENTITIES.get(BlockEntityType.SHULKER_BOX);

            override protected bool isValidBlock(GeyserSession session, Vector3i position, BlockState blockState) {
                return blockState.block().javaIdentifier().value().contains("shulker_box");
            }

            override protected void setCustomName(GeyserSession session, Vector3i position, Inventory inventory, BlockState javaBlockState) {
                NbtMapBuilder tag = NbtMap.builder()
                        .putInt("x", position.getX())
                        .putInt("y", position.getY())
                        .putInt("z", position.getZ())
                        .putString("CustomName", inventory.getTitle());

                shulkerBoxTranslator.translateTag(session, tag, null, javaBlockState);

                BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
                dataPacket.setData(tag.build());
                dataPacket.setBlockPosition(position);
                session.sendUpstreamPacket(dataPacket);
            }
        }, ContainerInventoryUpdater.INSTANCE);
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot, Container container) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(ContainerSlotType.SHULKER_BOX, javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot, container);
    }

    override public ContainerType closeContainerType(Container container) {
        return ContainerType.CONTAINER;
    }
}
