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

package org.geysermc.geyser.inventory.holder;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the fake block we implement for each inventory, should we need to.
 * This class will attempt to use a real block first, if possible.
 */
public class BlockInventoryHolder extends InventoryHolder {
    /**
     * The default Java block ID to translate as a fake block
     */
    private final int defaultJavaBlockState;
    private final ContainerType containerType;
    private final Set<String> validBlocks;

    public BlockInventoryHolder(String javaBlockIdentifier, ContainerType containerType, String... validBlocks) {
        this.defaultJavaBlockState = BlockRegistries.JAVA_IDENTIFIER_TO_ID.get().getInt(javaBlockIdentifier);
        this.containerType = containerType;
        if (validBlocks != null) {
            Set<String> validBlocksTemp = new HashSet<>(validBlocks.length + 1);
            Collections.addAll(validBlocksTemp, validBlocks);
            validBlocksTemp.add(BlockUtils.getCleanIdentifier(javaBlockIdentifier));
            this.validBlocks = Set.copyOf(validBlocksTemp);
        } else {
            this.validBlocks = Collections.singleton(BlockUtils.getCleanIdentifier(javaBlockIdentifier));
        }
    }

    @Override
    public boolean prepareInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        // Check to see if there is an existing block we can use that the player just selected.
        // First, verify that the player's position has not changed, so we don't try to select a block wildly out of range.
        // (This could be a virtual inventory that the player is opening)
        if (checkInteractionPosition(session)) {
            // Then, check to see if the interacted block is valid for this inventory by ensuring the block state identifier is valid
            // and the bedrock block is vanilla
            int javaBlockId = session.getGeyser().getWorldManager().getBlockAt(session, session.getLastInteractionBlockPosition());
            if (!BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get().containsKey(javaBlockId)) {
                String[] javaBlockString = BlockRegistries.JAVA_BLOCKS.getOrDefault(javaBlockId, BlockMapping.DEFAULT).getJavaIdentifier().split("\\[");
                if (isValidBlock(javaBlockString)) {
                    // We can safely use this block
                    inventory.setHolderPosition(session.getLastInteractionBlockPosition());
                    ((Container) inventory).setUsingRealBlock(true, javaBlockString[0]);
                    setCustomName(session, session.getLastInteractionBlockPosition(), inventory, javaBlockId);

                    return true;
                }
            }
        }

        Vector3i position = InventoryUtils.findAvailableWorldSpace(session);
        if (position == null) {
            return false;
        }

        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setDefinition(session.getBlockMappings().getVanillaBedrockBlock(defaultJavaBlockState));
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);
        inventory.setHolderPosition(position);

        setCustomName(session, position, inventory, defaultJavaBlockState);

        return true;
    }

    /**
     * Will be overwritten in the beacon inventory translator to remove the check, since virtual inventories can't exist.
     *
     * @return if the player's last interaction position and current position match. Used to ensure that we don't select
     * a block to hold the inventory that's wildly out of range.
     */
    protected boolean checkInteractionPosition(GeyserSession session) {
        return session.getLastInteractionPlayerPosition().equals(session.getPlayerEntity().getPosition());
    }

    /**
     * @return true if this Java block ID can be used for player inventory.
     */
    protected boolean isValidBlock(String[] javaBlockString) {
        return this.validBlocks.contains(javaBlockString[0]);
    }

    protected void setCustomName(GeyserSession session, Vector3i position, Inventory inventory, int javaBlockState) {
        NbtMap tag = NbtMap.builder()
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ())
                .putString("CustomName", inventory.getTitle()).build();
        BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag);
        dataPacket.setBlockPosition(position);
        session.sendUpstreamPacket(dataPacket);
    }

    @Override
    public void openInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) inventory.getBedrockId());
        containerOpenPacket.setType(containerType);
        containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);
    }

    @Override
    public void closeInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        if (((Container) inventory).isUsingRealBlock()) {
            // No need to reset a block since we didn't change any blocks
            // But send a container close packet because we aren't destroying the original.
            ContainerClosePacket packet = new ContainerClosePacket();
            packet.setId((byte) inventory.getBedrockId());
            packet.setServerInitiated(true);
            session.sendUpstreamPacket(packet);
            return;
        }

        Vector3i holderPos = inventory.getHolderPosition();
        int realBlock = session.getGeyser().getWorldManager().getBlockAt(session, holderPos.getX(), holderPos.getY(), holderPos.getZ());
        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setDefinition(session.getBlockMappings().getBedrockBlock(realBlock));
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);
    }
}
