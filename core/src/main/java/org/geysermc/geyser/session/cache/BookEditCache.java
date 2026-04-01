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

package org.geysermc.geyser.session.cache;

import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundEditBookPacket;
import lombok.Setter;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;


public class BookEditCache {
    private final GeyserSession session;
    @Setter
    private ServerboundEditBookPacket packet;
    
    private long lastBookUpdate;

    public BookEditCache(GeyserSession session) {
        this.session = session;
    }

    
    public void checkForSend() {
        if (packet == null) {
            
            return;
        }
        
        if ((System.currentTimeMillis() - lastBookUpdate) < 1000) {
            return;
        }
        
        GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand();
        if (itemStack == null || !itemStack.is(Items.WRITABLE_BOOK)) {
            packet = null;
            return;
        }
        session.sendDownstreamGamePacket(packet);
        packet = null;
        lastBookUpdate = System.currentTimeMillis();
    }
}
