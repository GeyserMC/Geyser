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

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Range;

public class PlayerInventory extends Inventory {
    /**
     * Stores the held item slot, starting at index 0.
     * Add 36 in order to get the network item slot.
     */
    @Getter
    @Setter
    private int heldItemSlot;

    @Getter
    @NonNull
    private GeyserItemStack cursor = GeyserItemStack.EMPTY;

    public PlayerInventory() {
        super(0, 46, null);
        heldItemSlot = 0;
    }

    @Override
    public int getOffsetForHotbar(@Range(from = 0, to = 8) int slot) {
        return slot + 36;
    }

    public void setCursor(@NonNull GeyserItemStack newCursor, GeyserSession session) {
        updateItemNetId(cursor, newCursor, session);
        cursor = newCursor;
    }

    public GeyserItemStack getItemInHand(@NonNull Hand hand) {
        return hand == Hand.OFF_HAND ? getOffhand() : getItemInHand();
    }

    public GeyserItemStack getItemInHand() {
        if (36 + heldItemSlot > this.size) {
            GeyserImpl.getInstance().getLogger().debug("Held item slot was larger than expected!");
            return GeyserItemStack.EMPTY;
        }
        return items[36 + heldItemSlot];
    }

    public void setItemInHand(@NonNull GeyserItemStack item) {
        if (36 + heldItemSlot > this.size) {
            GeyserImpl.getInstance().getLogger().debug("Held item slot was larger than expected!");
            return;
        }
        items[36 + heldItemSlot] = item;
    }

    public GeyserItemStack getOffhand() {
        return items[45];
    }
}
