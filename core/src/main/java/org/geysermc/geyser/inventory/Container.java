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

package org.geysermc.geyser.inventory;

import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.jetbrains.annotations.Range;

/**
 * Combination of {@link Inventory} and {@link PlayerInventory}
 */
@Getter
public class Container extends Inventory {
    private final PlayerInventory playerInventory;
    private final int containerSize;

    /**
     * Whether we are using a real block when opening this inventory.
     */
    private boolean isUsingRealBlock = false;

    public Container(String title, int id, int size, ContainerType containerType, PlayerInventory playerInventory) {
        super(title, id, size, containerType);
        this.playerInventory = playerInventory;
        this.containerSize = this.size + InventoryTranslator.PLAYER_INVENTORY_SIZE;
    }

    @Override
    public GeyserItemStack getItem(int slot) {
        if (slot < this.size) {
            return super.getItem(slot);
        } else {
            return playerInventory.getItem(slot - this.size + InventoryTranslator.PLAYER_INVENTORY_OFFSET);
        }
    }

    @Override
    public int getOffsetForHotbar(@Range(from = 0, to = 8) int slot) {
        return playerInventory.getOffsetForHotbar(slot) - InventoryTranslator.PLAYER_INVENTORY_OFFSET + this.size;
    }

    @Override
    public void setItem(int slot, @NonNull GeyserItemStack newItem, GeyserSession session) {
        if (slot < this.size) {
            super.setItem(slot, newItem, session);
        } else {
            playerInventory.setItem(slot - this.size + InventoryTranslator.PLAYER_INVENTORY_OFFSET, newItem, session);
        }
    }

    @Override
    public int getSize() {
        return this.containerSize;
    }

    /**
     * Will be overwritten for droppers.
     *
     * @param usingRealBlock whether this container is using a real container or not
     * @param javaBlockId the Java block string of the block, if real
     */
    public void setUsingRealBlock(boolean usingRealBlock, String javaBlockId) {
        isUsingRealBlock = usingRealBlock;
    }
}
