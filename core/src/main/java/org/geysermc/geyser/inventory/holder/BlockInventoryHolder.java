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

import org.checkerframework.checker.nullness.qual.Nullable;
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


public class BlockInventoryHolder extends InventoryHolder {
    
    private final BlockState defaultJavaBlockState;
    private final ContainerType containerType;
    private final Set<Block> validBlocks;
    private final @Nullable Class<? extends Block> validBlockClass;

    public BlockInventoryHolder(Block defaultJavaBlock, ContainerType containerType, Block... validBlocks) {
        this(defaultJavaBlock.defaultBlockState(), null, containerType, validBlocks);
    }

    public BlockInventoryHolder(BlockState defaultJavaBlockState, ContainerType containerType, Block... validBlocks) {
        this(defaultJavaBlockState, null, containerType, validBlocks);
    }

    public BlockInventoryHolder(BlockState defaultJavaBlockState, @Nullable Class<? extends Block> validBlockClass, ContainerType containerType, Block... validBlocks) {
        this.defaultJavaBlockState = defaultJavaBlockState;
        this.validBlockClass = validBlockClass;
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
        

        
        
        
        
        
        
        if (previous.isUsingRealBlock()) {
            return false;
        }

        
        Vector3i position = InventoryUtils.findAvailableWorldSpace(session);
        if (Objects.equals(position, previous.getHolderPosition())) {
            return true;
        } else {
            GeyserImpl.getInstance().getLogger().debug(session, "Not reusing inventory due to virtual block holder changing (%s -> %s)!",
                previous.getHolderPosition(), position);
            return false;
        }
    }

    @Override
    public boolean prepareInventory(GeyserSession session, Container container) {
        if (canUseRealBlock(session, container)) {
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
        container.setHolderPosition(position);

        setCustomName(session, position, container, defaultJavaBlockState);

        return true;
    }

    protected boolean canUseRealBlock(GeyserSession session, Container container) {
        
        
        
        if (checkInteractionPosition(session)) {
            
            
            BlockState state = session.getGeyser().getWorldManager().blockAt(session, session.getLastInteractionBlockPosition());
            if (!BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get().containsKey(state.javaId())) {
                if (isValidBlock(session, session.getLastInteractionBlockPosition(), state)) {
                    
                    container.setHolderPosition(session.getLastInteractionBlockPosition());
                    container.setUsingRealBlock(true, state.block());
                    setCustomName(session, session.getLastInteractionBlockPosition(), container, state);

                    return true;
                }
            }
        }

        return false;
    }

    
    protected boolean checkInteractionPosition(GeyserSession session) {
        return session.getLastInteractionPlayerPosition().distance(session.getPlayerEntity().position()) < 2;
    }

    
    protected boolean isValidBlock(GeyserSession session, Vector3i position, BlockState blockState) {
        if (this.validBlockClass != null && this.validBlockClass.isInstance(blockState.block())) {
            return true;
        }
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
            
            
            ContainerClosePacket packet = new ContainerClosePacket();
            packet.setId((byte) container.getBedrockId());
            packet.setServerInitiated(true);
            packet.setType(type != null ? type : containerType);
            session.sendUpstreamPacket(packet);

            if (container.isUsingRealBlock()) {
                
                
                if (type == null) {
                    Vector3i holderPos = container.getHolderPosition();
                    UpdateBlockPacket blockPacket = new UpdateBlockPacket();
                    blockPacket.setDataLayer(0);
                    blockPacket.setBlockPosition(holderPos);
                    blockPacket.setDefinition(session.getBlockMappings().getBedrockAir());
                    blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
                    session.sendUpstreamPacket(blockPacket);
                } else {
                    
                    
                    return;
                }
            }
        }

        
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
