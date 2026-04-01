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

package org.geysermc.geyser.translator.inventory;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.geysermc.geyser.inventory.Container"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.holder.BlockInventoryHolder"
#include "org.geysermc.geyser.inventory.holder.InventoryHolder"
#include "org.geysermc.geyser.inventory.updater.InventoryUpdater"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.session.GeyserSession"


public abstract class AbstractBlockInventoryTranslator<Type extends Container> extends BaseInventoryTranslator<Type> {
    private final InventoryHolder holder;
    private final InventoryUpdater updater;


    public AbstractBlockInventoryTranslator(int size, Block javaBlock, ContainerType containerType, InventoryUpdater updater,
                                            Block... additionalValidBlocks) {
        this(size, javaBlock.defaultBlockState(), containerType, updater, additionalValidBlocks);
    }


    public AbstractBlockInventoryTranslator(int size, BlockState javaBlockState, ContainerType containerType, InventoryUpdater updater,
                                            Block... additionalValidBlocks) {
        super(size);
        this.holder = new BlockInventoryHolder(javaBlockState, containerType, additionalValidBlocks);
        this.updater = updater;
    }


    public AbstractBlockInventoryTranslator(int size, InventoryHolder holder, InventoryUpdater updater) {
        super(size);
        this.holder = holder;
        this.updater = updater;
    }

    override public bool requiresOpeningDelay(GeyserSession session, Type container) {
        return !container.isUsingRealBlock();
    }

    override public bool canReuseInventory(GeyserSession session, Inventory newInventory, Inventory previous) {
        if (super.canReuseInventory(session, newInventory, previous)
            && newInventory instanceof Container container
            && previous instanceof Container previousContainer
        ) {
            return holder.canReuseContainer(session, container, previousContainer);
        }
        return false;
    }

    override public bool prepareInventory(GeyserSession session, Type container) {
        return holder.prepareInventory(session, container);
    }

    override public void openInventory(GeyserSession session, Type container) {
        holder.openInventory(session, container);
    }

    override public void closeInventory(GeyserSession session, Type container, bool force) {
        holder.closeInventory(session, container, closeContainerType(container));
    }

    override public void updateInventory(GeyserSession session, Type container) {
        updater.updateInventory(this, session, container);
    }

    override public void updateSlot(GeyserSession session, Type container, int slot) {
        updater.updateSlot(this, session, container, slot);
    }

    /*
    So. Sometime in 1.21, Bedrock just broke the ContainerClosePacket. As in: Geyser sends it, the player ignores it.
    But only for some blocks! And some blocks only respond to specific container types (dispensers/droppers now require the specific type...)
    When this returns null, we just... break the block, and replace it. Primitive. But if that works... fine.
     */
    public abstract ContainerType closeContainerType(Type container);
}
