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

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.Generic9X3Container;
import org.geysermc.geyser.inventory.holder.BlockInventoryHolder;
import org.geysermc.geyser.inventory.holder.InventoryHolder;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.ChestType;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.ChestBlock;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;

public class SingleChestInventoryTranslator extends ChestInventoryTranslator<Generic9X3Container> {
    private final InventoryHolder holder;

    public SingleChestInventoryTranslator(int size) {
        super(size, 27);
        this.holder = new BlockInventoryHolder(Blocks.CHEST.defaultBlockState().withValue(Properties.CHEST_TYPE, ChestType.SINGLE),
            ChestBlock.class, ContainerType.CONTAINER, Blocks.ENDER_CHEST, Blocks.BARREL) {
            @Override
            protected boolean isValidBlock(GeyserSession session, Vector3i position, BlockState blockState) {
                if (blockState.is(Blocks.ENDER_CHEST) || blockState.is(Blocks.BARREL)) {
                    // Can't have double ender chests or barrels
                    return true;
                }

                if (!super.isValidBlock(session, position, blockState)) {
                    return false;
                } else if (blockState.getValue(Properties.CHEST_TYPE) != ChestType.SINGLE) {
                    // Add provision to ensure this isn't a double chest
                    return false;
                } else {
                    // On 1.21.110 and above the client likes to merge single chests next to each other, even when we
                    // tell the client not to
                    // So, check for chests left and right of this chest. If there is a chest facing the same way,
                    // there is a chance the client has merged them, and we can't use this block
                    Direction facing = blockState.getValue(Properties.HORIZONTAL_FACING);
                    Vector3i left = position.add((facing.getAxis() == Axis.X ? Direction.SOUTH : Direction.WEST).getUnitVector());
                    Vector3i right = position.add((facing.getAxis() == Axis.X ? Direction.NORTH : Direction.EAST).getUnitVector());

                    BlockState leftState = BlockState.of(GeyserImpl.getInstance().getWorldManager().getBlockAt(session, left));
                    BlockState rightState = BlockState.of(GeyserImpl.getInstance().getWorldManager().getBlockAt(session, right));

                    return (!leftState.is(blockState.block()) || leftState.getValue(Properties.HORIZONTAL_FACING) != facing)
                        && (!rightState.is(blockState.block()) || rightState.getValue(Properties.HORIZONTAL_FACING) != facing);
                }
            }
        };
    }

    @Override
    public boolean prepareInventory(GeyserSession session, Generic9X3Container container) {
        return holder.prepareInventory(session, container);
    }

    @Override
    public void openInventory(GeyserSession session, Generic9X3Container container) {
        holder.openInventory(session, container);
    }

    @Override
    public void closeInventory(GeyserSession session, Generic9X3Container container, boolean force) {
        holder.closeInventory(session, container, ContainerType.CONTAINER);
    }

    @Override
    public Generic9X3Container createInventory(GeyserSession session, String name, int windowId, org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType containerType) {
        return new Generic9X3Container(session, name, windowId, this.size, containerType);
    }

    @Override
    protected ContainerSlotType slotType(Generic9X3Container generic9X3Container) {
        if (generic9X3Container.isBarrel()) {
            return ContainerSlotType.BARREL;
        }
        return super.slotType(generic9X3Container);
    }
}
