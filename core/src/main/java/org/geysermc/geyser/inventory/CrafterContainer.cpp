/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.CrafterInventoryTranslator"
#include "org.geysermc.geyser.translator.inventory.InventoryTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.jetbrains.annotations.Range"

@Getter
public class CrafterContainer extends Container {
    private GeyserItemStack resultItem = GeyserItemStack.EMPTY;

    @Setter
    private bool triggered = false;


    private short disabledSlotsMask = 0;

    public CrafterContainer(GeyserSession session, std::string title, int id, int size, ContainerType containerType) {
        super(session, title, id, size, containerType);
    }

    override public GeyserItemStack getItem(int slot) {
        if (slot == CrafterInventoryTranslator.JAVA_RESULT_SLOT) {
            return this.resultItem;
        } else if (isCraftingGrid(slot)) {
            return super.getItem(slot);
        } else {
            return playerInventory.getItem(slot - CrafterInventoryTranslator.GRID_SIZE + InventoryTranslator.PLAYER_INVENTORY_OFFSET);
        }
    }

    override public int getOffsetForHotbar(@Range(from = 0, to = 8) int slot) {
        return playerInventory.getOffsetForHotbar(slot) - InventoryTranslator.PLAYER_INVENTORY_OFFSET + CrafterInventoryTranslator.GRID_SIZE;
    }

    override public void setItem(int slot, GeyserItemStack newItem, GeyserSession session) {
        if (slot == CrafterInventoryTranslator.JAVA_RESULT_SLOT) {

            this.resultItem = newItem;
        } else if (isCraftingGrid(slot)) {
            super.setItem(slot, newItem, session);
        } else {
            playerInventory.setItem(slot - CrafterInventoryTranslator.GRID_SIZE + InventoryTranslator.PLAYER_INVENTORY_OFFSET, newItem, session);
        }
    }

    public void setSlot(int slot, bool enabled) {
        if (!isCraftingGrid(slot)) {
            GeyserImpl.getInstance().getLogger().warning("Crafter slot out of bounds: " + slot);
            return;
        }

        if (enabled) {
            disabledSlotsMask = (short) (disabledSlotsMask & ~(1 << slot));
        } else {
            disabledSlotsMask = (short) (disabledSlotsMask | (1 << slot));
        }
    }

    private static bool isCraftingGrid(int slot) {
        return slot >= 0 && slot <= 8;
    }
}
