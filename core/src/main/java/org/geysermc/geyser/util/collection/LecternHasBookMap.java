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

package org.geysermc.geyser.util.collection;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.LecternInventoryTranslator;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.util.BlockEntityUtils;

/**
 * Map that takes advantage of its internals for fast operations on block states to determine if they are lecterns.
 */
public class LecternHasBookMap extends FixedInt2BooleanMap {

    /**
     * Update a potential lectern within the world. This is a map method so it can use the internal fields to
     * optimize lectern determining.
     */
    public void handleBlockChange(GeyserSession session, int blockState, Vector3i position) {
        WorldManager worldManager = session.getGeyser().getWorldManager();

        int offset = blockState - this.start;
        if (offset < 0 || offset >= this.value.length) {
            // Block state is out of bounds of this map - lectern has been destroyed, if it existed
            if (!worldManager.shouldExpectLecternHandled()) {
                session.getLecternCache().remove(position);
            }
            return;
        }

        boolean newLecternHasBook;
        if (worldManager.shouldExpectLecternHandled()) {
            // As of right now, no tag can be added asynchronously
            worldManager.getLecternDataAt(session, position.getX(), position.getY(), position.getZ(), false);
        } else if ((newLecternHasBook = this.value[offset]) != this.get(worldManager.getBlockAt(session, position))) {
            // If the lectern block was updated, or it previously had a book
            NbtMap newLecternTag;
            // newLecternHasBook = the new lectern block state's "has book" toggle.
            if (newLecternHasBook) {
                newLecternTag = worldManager.getLecternDataAt(session, position.getX(), position.getY(), position.getZ(), false);
            } else {
                session.getLecternCache().remove(position);
                newLecternTag = LecternInventoryTranslator.getBaseLecternTag(position.getX(), position.getY(), position.getZ(), 0).build();
            }
            BlockEntityUtils.updateBlockEntity(session, newLecternTag, position);
        }
    }
}
