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

package org.geysermc.geyser.translator.inventory.horse;

import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.updater.MountInventoryUpdater;
import org.geysermc.geyser.inventory.updater.InventoryUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.BaseInventoryTranslator;

public abstract class AbstractMountInventoryTranslator extends BaseInventoryTranslator<Container> {
    private final InventoryUpdater updater;

    public AbstractMountInventoryTranslator(int size) {
        super(size);
        this.updater = MountInventoryUpdater.INSTANCE;
    }

    @Override
    public boolean prepareInventory(GeyserSession session, Container container) {
        return true;
    }

    @Override
    public void openInventory(GeyserSession session, Container container) {
    }

    @Override
    public void closeInventory(GeyserSession session, Container container, boolean force) {
        // TODO find a way to implement
        // Can cause inventory de-sync if the Java server requests an inventory close
    }

    @Override
    public void updateInventory(GeyserSession session, Container container) {
        updater.updateInventory(this, session, container);
    }

    @Override
    public void updateSlot(GeyserSession session, Container container, int slot) {
        updater.updateSlot(this, session, container, slot);
    }
}
