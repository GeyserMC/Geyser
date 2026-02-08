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

package org.geysermc.geyser.translator.inventory.chest;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.holder.BlockInventoryHolder;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.ChestType;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.ChestBlock;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.DoubleChestBlockEntityTranslator;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Objects;

public class DoubleChestInventoryTranslator extends ChestInventoryTranslator<Container> {
    private final int defaultJavaBlockState;

    public DoubleChestInventoryTranslator(int size) {
        super(size, 54);
        this.defaultJavaBlockState = Blocks.CHEST.defaultBlockState()
                .withValue(Properties.HORIZONTAL_FACING, Direction.NORTH)
                .withValue(Properties.CHEST_TYPE, ChestType.SINGLE)
                .javaId();
    }

    /**
     * Additional checks to verify that we can re-use the block inventory holder.
     * Mirrors {@link BlockInventoryHolder#canReuseContainer(GeyserSession, Container, Container)}
     */
    @Override
    public boolean canReuseInventory(GeyserSession session, @NonNull Inventory newInventory, @NonNull Inventory oldInventory) {
        if (!super.canReuseInventory(session, newInventory, oldInventory) ||
            !(newInventory instanceof Container) ||
            !(oldInventory instanceof Container previous)
        ) {
            return false;
        }

        // While we could reuse real blocks for virtual inventories,
        // it can result in unpleasant visual artifacts with specific plugins.
        // Specifically - a few plugins send multiple ClientboundOpenScreen packets
        // with different titles; where Geyser needs to re-open the menu fully in order to get
        // the correct title to appear. The additional delay added by using virtual blocks masks
        // the quick closing of the first packet.
        if (previous.isUsingRealBlock()) {
            return false;
        }

        // Check if we'd be using the same virtual inventory position.
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

        Vector3i pairPosition = position.add(Vector3i.UNIT_X);
        BlockDefinition definition = session.getBlockMappings().getVanillaBedrockBlock(defaultJavaBlockState);

        UpdateBlockPacket blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(position);
        blockPacket.setDefinition(definition);
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);

        NbtMapBuilder tag = BlockEntityTranslator.getConstantBedrockTag("Chest", position)
                .putInt("pairx", pairPosition.getX())
                .putInt("pairz", pairPosition.getZ())
                .putString("CustomName", container.getTitle())
                .putBoolean("pairlead", false);

        BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag.build());
        dataPacket.setBlockPosition(position);
        session.sendUpstreamPacket(dataPacket);

        blockPacket = new UpdateBlockPacket();
        blockPacket.setDataLayer(0);
        blockPacket.setBlockPosition(pairPosition);
        blockPacket.setDefinition(definition);
        blockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(blockPacket);

        tag = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", pairPosition.getX())
                .putInt("y", pairPosition.getY())
                .putInt("z", pairPosition.getZ())
                .putInt("pairx", position.getX())
                .putInt("pairz", position.getZ())
                .putString("CustomName", container.getTitle())
                .putBoolean("pairlead", true);

        dataPacket = new BlockEntityDataPacket();
        dataPacket.setData(tag.build());
        dataPacket.setBlockPosition(pairPosition);
        session.sendUpstreamPacket(dataPacket);

        container.setHolderPosition(position);

        return true;
    }

    @Override
    public void openInventory(GeyserSession session, Container container) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) container.getBedrockId());
        containerOpenPacket.setType(ContainerType.CONTAINER);
        containerOpenPacket.setBlockPosition(container.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(container.getHolderId());
        session.sendUpstreamPacket(containerOpenPacket);

        GeyserImpl.getInstance().getLogger().debug(session, containerOpenPacket.toString());
    }

    @Override
    public void closeInventory(GeyserSession session, Container container, boolean force) {
        // No need to reset a block since we didn't change any blocks
        // But send a container close packet because we aren't destroying the original.
        if (container.isDisplayed()) {
            ContainerClosePacket packet = new ContainerClosePacket();
            packet.setId((byte) container.getBedrockId());
            packet.setServerInitiated(true);
            packet.setType(ContainerType.CONTAINER);
            session.sendUpstreamPacket(packet);
        }

        if (!container.isUsingRealBlock()) {
            Vector3i holderPos = container.getHolderPosition();
            int realBlock = session.getGeyser().getWorldManager().getBlockAt(session, holderPos);
            UpdateBlockPacket blockPacket = new UpdateBlockPacket();
            blockPacket.setDataLayer(0);
            blockPacket.setBlockPosition(holderPos);
            blockPacket.setDefinition(session.getBlockMappings().getBedrockBlock(realBlock));
            session.sendUpstreamPacket(blockPacket);

            holderPos = holderPos.add(Vector3i.UNIT_X);
            realBlock = session.getGeyser().getWorldManager().getBlockAt(session, holderPos);
            blockPacket = new UpdateBlockPacket();
            blockPacket.setDataLayer(0);
            blockPacket.setBlockPosition(holderPos);
            blockPacket.setDefinition(session.getBlockMappings().getBedrockBlock(realBlock));
            session.sendUpstreamPacket(blockPacket);
        }
    }

    private boolean canUseRealBlock(GeyserSession session, Container container) {
        // See BlockInventoryHolder - same concept there except we're also dealing with a specific block state
        if (session.getLastInteractionPlayerPosition().distance(session.getPlayerEntity().getPosition()) < 2) {
            BlockState state = session.getGeyser().getWorldManager().blockAt(session, session.getLastInteractionBlockPosition());
            if (!BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get().containsKey(state.javaId())) {
                if (state.block() instanceof ChestBlock && state.getValue(Properties.CHEST_TYPE) != ChestType.SINGLE) {
                    container.setHolderPosition(session.getLastInteractionBlockPosition());
                    container.setUsingRealBlock(true, state.block());

                    NbtMapBuilder tag = BlockEntityTranslator.getConstantBedrockTag("Chest", session.getLastInteractionBlockPosition())
                        .putString("CustomName", container.getTitle());

                    DoubleChestBlockEntityTranslator.translateChestValue(tag, state,
                        session.getLastInteractionBlockPosition().getX(), session.getLastInteractionBlockPosition().getZ());

                    BlockEntityDataPacket dataPacket = new BlockEntityDataPacket();
                    dataPacket.setData(tag.build());
                    dataPacket.setBlockPosition(session.getLastInteractionBlockPosition());
                    session.sendUpstreamPacket(dataPacket);

                    return true;
                }
            }
        }
        return false;
    }
}
