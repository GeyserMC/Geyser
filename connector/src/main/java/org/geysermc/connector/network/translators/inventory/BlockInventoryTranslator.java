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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.inventory.holder.BlockInventoryHolder;
import org.geysermc.connector.network.translators.inventory.holder.InventoryHolder;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

public class BlockInventoryTranslator extends BaseInventoryTranslator {
    private final InventoryHolder holder;
    private final InventoryUpdater updater;

    public BlockInventoryTranslator(int size, String javaBlockIdentifier, ContainerType containerType, InventoryUpdater updater) {
        super(size);
        BlockState javaBlockState = BlockTranslator.getJavaBlockState(javaBlockIdentifier);
        int blockId = BlockTranslator.getBedrockBlockId(javaBlockState);
        this.holder = new BlockInventoryHolder(blockId, containerType);
        this.updater = updater;
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        holder.prepareInventory(this, session, inventory);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        holder.openInventory(this, session, inventory);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        holder.closeInventory(this, session, inventory);
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
