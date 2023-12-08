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
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.GeyserSession;

public class StonecutterContainer extends Container {
    /**
     * The button that has currently been pressed Java-side
     */
    @Getter
    @Setter
    private int stonecutterButton = -1;

    public StonecutterContainer(String title, int id, int size, ContainerType containerType, PlayerInventory playerInventory) {
        super(title, id, size, containerType, playerInventory);
    }

    @Override
    public void setItem(int slot, @NonNull GeyserItemStack newItem, GeyserSession session) {
        if (slot == 0 && newItem.getJavaId() != items[slot].getJavaId()) {
            // The pressed stonecutter button output resets whenever the input item changes
            this.stonecutterButton = -1;
        }
        super.setItem(slot, newItem, session);
    }
}
