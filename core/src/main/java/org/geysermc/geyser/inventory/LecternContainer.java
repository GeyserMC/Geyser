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

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.java.inventory.JavaOpenBookTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

@Getter
public class LecternContainer extends Container {
    @Setter
    private int currentBedrockPage = 0;
    @Setter
    private NbtMap blockEntityTag;

    private boolean isBookInPlayerInventory = false;

    public LecternContainer(GeyserSession session, String title, int id, int size, ContainerType containerType) {
        super(session, title, id, size, containerType);
    }

    /**
     * This is used ONLY once to set the book of a fake lectern in {@link JavaOpenBookTranslator}.
     * See {@link LecternContainer#setItem(int, GeyserItemStack, GeyserSession)} as for why this is separate.
     */
    public void setVirtualLecternBook(GeyserItemStack book, GeyserSession session) {
        this.isBookInPlayerInventory = true;
        super.setItem(0, book, session);
    }

    @Override
    public boolean shouldConfirmContainerClose() {
        return !isBookInPlayerInventory;
    }
}
