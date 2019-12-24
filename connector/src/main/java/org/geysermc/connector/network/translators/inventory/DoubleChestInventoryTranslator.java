/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;

public class DoubleChestInventoryTranslator extends BlockInventoryTranslator {
    public DoubleChestInventoryTranslator(int size) {
        super(size, "minecraft:chest[facing=north,type=single,waterlogged=false]", ContainerType.CONTAINER);
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        Vector3i position = session.getPlayerEntity().getPosition().toInt().add(Vector3i.UP);
        Vector3i pairPosition = position.add(Vector3i.UNIT_X);
        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setRuntimeId(blockId);
        blockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        session.getUpstream().sendPacket(blockPacket);

        CompoundTag tag = CompoundTag.EMPTY.toBuilder()
                .stringTag("id", "Chest")
                .intTag("x", position.getX())
                .intTag("y", position.getY())
                .intTag("z", position.getZ())
                .intTag("pairx", pairPosition.getX())
                .intTag("pairz", pairPosition.getZ())
                .stringTag("CustomName", inventory.getTitle()).buildRootTag();
        BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(position);
        session.getUpstream().sendPacket(dataPacket);

        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(pairPosition);
        blockPacket.setRuntimeId(blockId);
        blockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        session.getUpstream().sendPacket(blockPacket);

        tag = CompoundTag.EMPTY.toBuilder()
                .stringTag("id", "Chest")
                .intTag("x", pairPosition.getX())
                .intTag("y", pairPosition.getY())
                .intTag("z", pairPosition.getZ())
                .intTag("pairx", position.getX())
                .intTag("pairz", position.getZ())
                .stringTag("CustomName", inventory.getTitle()).buildRootTag();
        dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(pairPosition);
        session.getUpstream().sendPacket(dataPacket);

        inventory.setHolderPosition(position);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        Vector3i holderPos = inventory.getHolderPosition();
        Position pos = new Position(holderPos.getX(), holderPos.getY(), holderPos.getZ());
        BlockEntry realBlock = session.getChunkCache().getBlockAt(pos);
        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setRuntimeId(realBlock.getBedrockRuntimeId());
        session.getUpstream().sendPacket(blockPacket);

        holderPos = holderPos.add(Vector3i.UNIT_X);
        pos = new Position(holderPos.getX(), holderPos.getY(), holderPos.getZ());
        realBlock = session.getChunkCache().getBlockAt(pos);
        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setRuntimeId(realBlock.getBedrockRuntimeId());
        session.getUpstream().sendPacket(blockPacket);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        //need to pad empty slots for 4x9 and 5x9
        ItemData[] bedrockItems = new ItemData[54];
        for (int i = 0; i < bedrockItems.length; i++) {
            if (i <= this.size) {
                bedrockItems[i] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(i));
            } else {
                bedrockItems[i] = ItemData.AIR;
            }
        }
        InventoryContentPacket contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(inventory.getId());
        contentPacket.setContents(bedrockItems);
        session.getUpstream().sendPacket(contentPacket);

        Inventory playerInventory = session.getInventory();
        for (int i = 0; i < 36; i++) {
            playerInventory.setItem(i + 9, inventory.getItem(i + this.size));
        }
        TranslatorsInit.getInventoryTranslators().get(playerInventory.getWindowType()).updateInventory(session, playerInventory);
    }
}
