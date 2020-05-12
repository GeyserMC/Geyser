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
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.inventory.updater.ChestInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

public class DoubleChestInventoryTranslator extends BaseInventoryTranslator {
    private final InventoryUpdater updater;

    public DoubleChestInventoryTranslator(int size) {
        super(size);
        this.updater = new ChestInventoryUpdater(54);
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        Vector3i position = session.getLastInteractionPosition();
        Vector3i pairPosition = Vector3i.ZERO;
        boolean isCreatingNewBlock = false;
        int newBlockId = 0;
        String javaBlockId;
        if (session.getConnector().getConfig().isCacheChunks()) {
            // More reliable
            if (position != null) {
                javaBlockId = BlockTranslator.getJavaIdBlockMap().inverse().get(session.getConnector().getWorldManager().getBlockAt(session, position.getX(), position.getY(), position.getZ()));
            } else {
                javaBlockId = "minecraft:air";
                isCreatingNewBlock = true;
            }
        } else {
            javaBlockId = session.getLastBlockPlacedId();
        }
        String blockId = "minecraft:chest[facing=north,type=single,waterlogged=false]";
        String otherPairBlockId = blockId;
        if (javaBlockId != null && !isCreatingNewBlock) {
            String isolatedBlockId = javaBlockId.split("\\[")[0];
            String thisBlockId = blockId.split("\\[")[0];
            if (isolatedBlockId.equals(thisBlockId)) {
                newBlockId = BlockTranslator.getBedrockBlockId(BlockTranslator.getJavaBlockState(javaBlockId));
                if (javaBlockId.contains("type=left")) {
                    if (javaBlockId.contains("facing=north")) {
                        pairPosition = position.add(Vector3i.UNIT_X);
                    } else if (javaBlockId.contains("facing=south")) {
                        pairPosition = position.sub(Vector3i.UNIT_X);
                    } else if (javaBlockId.contains("facing=east")) {
                        pairPosition = position.add(Vector3i.UNIT_Z);
                    } else if (javaBlockId.contains("facing=west")) {
                        pairPosition = position.sub(Vector3i.UNIT_Z);
                    }
                    otherPairBlockId = javaBlockId.replace("type=left", "type=right");
                } else if (javaBlockId.contains("type=right")) {
                    if (javaBlockId.contains("facing=north")) {
                        pairPosition = position.sub(Vector3i.UNIT_X);
                    } else if (javaBlockId.contains("facing=south")) {
                        pairPosition = position.add(Vector3i.UNIT_X);
                    } else if (javaBlockId.contains("facing=east")) {
                        pairPosition = position.sub(Vector3i.UNIT_Z);
                    } else if (javaBlockId.contains("facing=west")) {
                        pairPosition = position.add(Vector3i.UNIT_Z);
                    }
                    otherPairBlockId = javaBlockId.replace("type=right", "type=left");
                } else {
                    // Chest type is single
                    isCreatingNewBlock = true;
                }
            } else {
                System.out.println(isolatedBlockId + " is not equal at all to " + thisBlockId);
                isCreatingNewBlock = true;
            }
        } else {
            isCreatingNewBlock = true;
        }

        if (isCreatingNewBlock) {
            // Reset position to player because this is the wrong block
            position = session.getPlayerEntity().getPosition().toInt().add(Vector3i.UP);
            pairPosition = position.add(Vector3i.UNIT_X);
            otherPairBlockId = blockId;
        }

        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setRuntimeId(isCreatingNewBlock ? BlockTranslator.getBedrockBlockId(BlockTranslator.getJavaBlockState(blockId)) : newBlockId);
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);

        if (isCreatingNewBlock) {
            CompoundTag tag = CompoundTag.builder()
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
            session.sendUpstreamPacket(dataPacket);
        }

        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(pairPosition);
        blockPacket.setRuntimeId(BlockTranslator.getBedrockBlockId(BlockTranslator.getJavaBlockState(otherPairBlockId)));
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);

        CompoundTag tag = CompoundTag.builder()
                .stringTag("id", "Chest")
                .intTag("x", pairPosition.getX())
                .intTag("y", pairPosition.getY())
                .intTag("z", pairPosition.getZ())
                .intTag("pairx", position.getX())
                .intTag("pairz", position.getZ())
                .stringTag("CustomName", inventory.getTitle()).buildRootTag();
        BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(pairPosition);
        session.sendUpstreamPacket(dataPacket);

        inventory.setHolderPosition(position);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setWindowId((byte) inventory.getId());
        containerOpenPacket.setType((byte) ContainerType.CONTAINER.id());
        containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        Vector3i holderPos = inventory.getHolderPosition();
        Position pos = new Position(holderPos.getX(), holderPos.getY(), holderPos.getZ());
        BlockState realBlock = session.getConnector().getWorldManager().getBlockAt(session, pos.getX(), pos.getY(), pos.getZ());
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

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updater.updateInventory(this, session, inventory);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        updater.updateSlot(this, session, inventory, slot);
    }
}
