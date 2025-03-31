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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.LecternContainer;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the fake block we implement for each inventory, should we need to.
 * This class will attempt to use a real block first, if possible.
 */
public class BlockInventoryHolder extends InventoryHolder {
    /**
     * The default Java block ID to translate as a fake block
     */
    private final BlockState defaultJavaBlockState;
    private final ContainerType containerType;
    private final Set<Block> validBlocks;

    public BlockInventoryHolder(Block defaultJavaBlock, ContainerType containerType, Block... validBlocks) {
        this(defaultJavaBlock.defaultBlockState(), containerType, validBlocks);
    }

    public BlockInventoryHolder(BlockState defaultJavaBlockState, ContainerType containerType, Block... validBlocks) {
        this.defaultJavaBlockState = defaultJavaBlockState;
        this.containerType = containerType;
        if (validBlocks != null) {
            Set<Block> validBlocksTemp = new HashSet<>(validBlocks.length + 1);
            Collections.addAll(validBlocksTemp, validBlocks);
            validBlocksTemp.add(defaultJavaBlockState.block());
            this.validBlocks = Set.copyOf(validBlocksTemp);
        } else {
            this.validBlocks = Collections.singleton(defaultJavaBlockState.block());
        }
    }

    @Override
    public boolean canReuseContainer(GeyserSession session, Container container, Container previous) {
        // We already ensured that the inventories are using the same type, size, and title

        // TODO this would currently break, so we're not reusing this
        if (previous.isUsingRealBlock()) {
            return false;
        }

        // Check if we'd be using the same virtual inventory position.
        Vector3i position = InventoryUtils.findAvailableWorldSpace(session);
        if (Objects.equals(position, previous.getHolderPosition())) {
            return true;
        } else {
            GeyserImpl.getInstance().getLogger().debug(session, "Not reusing inventory (%s) due to virtual block holder changing (%s -> %s)!",
                InventoryUtils.debugInventory(container), previous.getHolderPosition(), position);
            return false;
        }
    }

    @Override
    public boolean prepareInventory(GeyserSession session, Container inventory) {
        if (canUseRealBlock(session, inventory)) {
            return true;
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

    protected boolean canUseRealBlock(GeyserSession session, Container inventory) {
        // Check to see if there is an existing block we can use that the player just selected.
        // First, verify that the player's position has not changed, so we don't try to select a block wildly out of range.
        // (This could be a virtual inventory that the player is opening)
        if (checkInteractionPosition(session)) {
            // Then, check to see if the interacted block is valid for this inventory by ensuring the block state identifier is valid
            // and the bedrock block is vanilla
            BlockState state = session.getGeyser().getWorldManager().blockAt(session, session.getLastInteractionBlockPosition());
            if (!BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get().containsKey(state.javaId())) {
                if (isValidBlock(state)) {
                    // We can safely use this block
                    inventory.setHolderPosition(session.getLastInteractionBlockPosition());
                    inventory.setUsingRealBlock(true, state.block());
                    setCustomName(session, session.getLastInteractionBlockPosition(), inventory, state);

                    return true;
                }
            }
        }

        return false;
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
    protected boolean isValidBlock(BlockState blockState) {
        return this.validBlocks.contains(blockState.block());
    }

    protected void setCustomName(GeyserSession session, Vector3i position, Inventory inventory, BlockState javaBlockState) {
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
    public void openInventory(GeyserSession session, Container container) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) container.getBedrockId());
        containerOpenPacket.setType(containerType);
        containerOpenPacket.setBlockPosition(container.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(container.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);

        GeyserImpl.getInstance().getLogger().debug(session, containerOpenPacket.toString());
    }

    @Override
    public void closeInventory(GeyserSession session, Container container, ContainerType type) {
        if (container.isDisplayed() && !(container instanceof LecternContainer)) {
            // No need to reset a block since we didn't change any blocks
            // But send a container close packet because we aren't destroying the original.
            ContainerClosePacket packet = new ContainerClosePacket();
            packet.setId((byte) container.getBedrockId());
            packet.setServerInitiated(true);
            packet.setType(type != null ? type : containerType);
            session.sendUpstreamPacket(packet);

            if (container.isUsingRealBlock()) {
                // Type being null indicates that the ContainerClosePacket is not effective.
                // So we yeet away the block!
                if (type == null) {
                    Vector3i holderPos = container.getHolderPosition();
                    UpdateBlockPacket blockPacket = new UpdateBlockPacket();
                    blockPacket.setDataLayer(0);
                    blockPacket.setBlockPosition(holderPos);
                    blockPacket.setDefinition(session.getBlockMappings().getBedrockAir());
                    blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
                    session.sendUpstreamPacket(blockPacket);
                } else {
                    // We're using a real block and are able to close the block without destroying it,
                    // so we can don't need to reset it below.
                    return;
                }
            }
        }

        // Reset to correct block
        Vector3i holderPos = container.getHolderPosition();
        int realBlock = session.getGeyser().getWorldManager().getBlockAt(session, holderPos.getX(), holderPos.getY(), holderPos.getZ());
        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(holderPos);
        blockPacket.setDefinition(session.getBlockMappings().getBedrockBlock(realBlock));
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);
    }
}
