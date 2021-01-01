/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.inventory;

import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.Arrays;

public class Inventory {

    @Getter
    protected int id;

    @Getter
    protected final int size;

    @Getter
    @Setter
    protected String title;

    protected GeyserItemStack[] items;

    /**
     * The location of the inventory block. Will either be a fake block above the player's head, or the actual block location
     */
    @Getter
    @Setter
    protected Vector3i holderPosition = Vector3i.ZERO;

    @Getter
    @Setter
    protected long holderId = -1;

    @Getter
    protected short transactionId = 0;

    protected Inventory(int id, int size) {
        this("Inventory", id, size);
    }

    protected Inventory(String title, int id, int size) {
        this.title = title;
        this.id = id;
        this.size = size;
        this.items = new GeyserItemStack[size];
        Arrays.fill(items, GeyserItemStack.EMPTY);
    }

    public GeyserItemStack getItem(int slot) {
        return items[slot];
    }

    public void setItem(int slot, @NonNull GeyserItemStack newItem, GeyserSession session) {
        GeyserItemStack oldItem = items[slot];
        updateItemNetId(oldItem, newItem, session);
        items[slot] = newItem;
    }

    protected static void updateItemNetId(GeyserItemStack oldItem, GeyserItemStack newItem, GeyserSession session) {
        if (!newItem.isEmpty()) {
            if (newItem.getItemData(session).equals(oldItem.getItemData(session), false, false, false)) {
                newItem.setNetId(oldItem.getNetId());
            } else {
                newItem.setNetId(session.getNextItemNetId());
            }
        }
    }

    public short getNextTransactionId() {
        return ++transactionId;
    }
}
