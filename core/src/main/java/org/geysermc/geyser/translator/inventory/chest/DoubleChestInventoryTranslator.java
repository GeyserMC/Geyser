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

package org.geysermc.geyser.translator.inventory.chest;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.DoubleChestValue;
import org.geysermc.geyser.translator.level.block.entity.DoubleChestBlockEntityTranslator;
import org.geysermc.geyser.registry.BlockRegistries;

public class DoubleChestInventoryTranslator extends ChestInventoryTranslator {
    private final int defaultJavaBlockState;

    public DoubleChestInventoryTranslator(int size) {
        super(size, 54);
        this.defaultJavaBlockState = BlockRegistries.JAVA_IDENTIFIERS.get("minecraft:chest[facing=north,type=single,waterlogged=false]");
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        // See BlockInventoryHolder - same concept there except we're also dealing with a specific block state
        if (session.getLastInteractionPlayerPosition().equals(session.getPlayerEntity().getPosition())) {
            int javaBlockId = session.getGeyser().getWorldManager().getBlockAt(session, session.getLastInteractionBlockPosition());
            String[] javaBlockString = BlockRegistries.JAVA_IDENTIFIERS.get().getOrDefault(javaBlockId, "minecraft:air").split("\\[");
            if (javaBlockString.length > 1 && (javaBlockString[0].equals("minecraft:chest") || javaBlockString[0].equals("minecraft:trapped_chest"))
                    && !javaBlockString[1].contains("type=single")) {
                inventory.setHolderPosition(session.getLastInteractionBlockPosition());
                ((Container) inventory).setUsingRealBlock(true, javaBlockString[0]);

                NbtMapBuilder tag = NbtMap.builder()
                        .putString("id", "Chest")
                        .putInt("x", session.getLastInteractionBlockPosition().getX())
                        .putInt("y", session.getLastInteractionBlockPosition().getY())
                        .putInt("z", session.getLastInteractionBlockPosition().getZ())
                        .putString("CustomName", inventory.getTitle())
                        .putString("id", "Chest");

                DoubleChestValue chestValue = BlockStateValues.getDoubleChestValues().get(javaBlockId);
                DoubleChestBlockEntityTranslator.translateChestValue(tag, chestValue,
                        session.getLastInteractionBlockPosition().getX(), session.getLastInteractionBlockPosition().getZ());

                BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
                dataPacket.setData(tag.build());
                dataPacket.setBlockPosition(session.getLastInteractionBlockPosition());
                session.sendUpstreamPacket(dataPacket);
                return;
            }
        }

        Vector3i position = session.getPlayerEntity().getPosition().toInt().add(Vector3i.UP);
        Vector3i pairPosition = position.add(Vector3i.UNIT_X);
        int bedrockBlockId = session.getBlockMappings().getBedrockBlockId(defaultJavaBlockState);

        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setRuntimeId(bedrockBlockId);
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
        blockPacket.setRuntimeId(bedrockBlockId);
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
        if (((Container) inventory).isUsingRealBlock()) {
            // No need to reset a block since we didn't change any blocks
            // But send a container close packet because we aren't destroying the original.
            ContainerClosePacket packet = new ContainerClosePacket();
            packet.setId((byte) inventory.getId());
            packet.setUnknownBool0(true); //TODO needs to be changed in Protocol to "server-side" or something
            session.sendUpstreamPacket(packet);
            return;
        }

        Vector3i holderPos = inventory.getHolderPosition();
        int realBlock = session.getGeyser().getWorldManager().getBlockAt(session, holderPos);
        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setRuntimeId(session.getBlockMappings().getBedrockBlockId(realBlock));
        session.sendUpstreamPacket(blockPacket);

        holderPos = holderPos.add(Vector3i.UNIT_X);
        realBlock = session.getGeyser().getWorldManager().getBlockAt(session, holderPos);
        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setRuntimeId(session.getBlockMappings().getBedrockBlockId(realBlock));
        session.sendUpstreamPacket(blockPacket);
    }
}
