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

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.holder.BlockInventoryHolder;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;

public class ShulkerInventoryTranslator extends AbstractBlockInventoryTranslator {
    public ShulkerInventoryTranslator() {
        super(27, new BlockInventoryHolder("minecraft:shulker_box[facing=north]", ContainerType.CONTAINER) {
            private final BlockEntityTranslator shulkerBoxTranslator = BlockEntityTranslator.BLOCK_ENTITY_TRANSLATORS.get("ShulkerBox");

            @Override
            protected boolean isValidBlock(String[] javaBlockString) {
                return javaBlockString[0].contains("shulker_box");
            }

            @Override
            protected void setCustomName(GeyserSession session, Vector3i position, Inventory inventory, int javaBlockState) {
                NbtMapBuilder tag = NbtMap.builder()
                        .putInt("x", position.getX())
                        .putInt("y", position.getY())
                        .putInt("z", position.getZ())
                        .putString("CustomName", inventory.getTitle());
                // Don't reset facing property
                shulkerBoxTranslator.translateTag(tag, null, javaBlockState);

                BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
                dataPacket.setData(tag.build());
                dataPacket.setBlockPosition(position);
                session.sendUpstreamPacket(dataPacket);
            }
        }, ContainerInventoryUpdater.INSTANCE);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot) {
        if (javaSlot < this.size) {
            return new BedrockContainerSlot(ContainerSlotType.SHULKER, javaSlot);
        }
        return super.javaSlotToBedrockContainer(javaSlot);
    }
}
