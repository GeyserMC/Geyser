/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

public class DoubleChestInventoryTranslator extends ChestInventoryTranslator {
    private Integer blockId;

    public DoubleChestInventoryTranslator(int size) {
        super(size, 54);
    }

    private int getBlockId() {
        if (blockId == null) {
            int javaBlockState = BlockTranslator.getJavaBlockState("minecraft:chest[facing=north,type=single,waterlogged=false]");
            blockId = BlockTranslator.getBedrockBlockId(javaBlockState);
        }
        return blockId;
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        Vector3i position = session.getPlayerEntity().getPosition().toInt().add(Vector3i.UP);
        Vector3i pairPosition = position.add(Vector3i.UNIT_X);

        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setRuntimeId(getBlockId());
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);

        NbtMap tag = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ())
                .putInt("pairx", pairPosition.getX())
                .putInt("pairz", pairPosition.getZ())
                .putString("CustomName", inventory.getTitle()).build();
        BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(position);
        session.sendUpstreamPacket(dataPacket);

        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(pairPosition);
        blockPacket.setRuntimeId(getBlockId());
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);

        tag = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", pairPosition.getX())
                .putInt("y", pairPosition.getY())
                .putInt("z", pairPosition.getZ())
                .putInt("pairx", position.getX())
                .putInt("pairz", position.getZ())
                .putString("CustomName", inventory.getTitle()).build();
        dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(pairPosition);
        session.sendUpstreamPacket(dataPacket);

        inventory.setHolderPosition(position);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) inventory.getId());
        containerOpenPacket.setType(ContainerType.CONTAINER);
        containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        Vector3i holderPos = inventory.getHolderPosition();
        Position pos = new Position(holderPos.getX(), holderPos.getY(), holderPos.getZ());
        int realBlock = session.getConnector().getWorldManager().getBlockAt(session, pos.getX(), pos.getY(), pos.getZ());
        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setRuntimeId(BlockTranslator.getBedrockBlockId(realBlock));
        session.sendUpstreamPacket(blockPacket);

        holderPos = holderPos.add(Vector3i.UNIT_X);
        pos = new Position(holderPos.getX(), holderPos.getY(), holderPos.getZ());
        realBlock = session.getConnector().getWorldManager().getBlockAt(session, pos.getX(), pos.getY(), pos.getZ());
        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setRuntimeId(BlockTranslator.getBedrockBlockId(realBlock));
        session.sendUpstreamPacket(blockPacket);
    }
}
