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

package org.geysermc.geyser.item.type;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.GeyserSession;

public class ChestItem extends BlockItem {

    public ChestItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        // Strip the BlockEntityTag from the chests contents
        // sent to the client. The client does not parse this
        // or use it for anything, as this tag is fully
        // server-side, so we remove it to reduce bandwidth and
        // solve potential issues with very large tags.

        // There was a problem in the past where this would strip
        // NBT data in creative mode, however with the new server
        // authoritative inventories, this is no longer a concern.
        tag.remove("BlockEntityTag");
    }
}
