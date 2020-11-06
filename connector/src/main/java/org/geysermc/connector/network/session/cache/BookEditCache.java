/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientEditBookPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;

/**
 * Manages updating the current writable book.
 *
 * Java sends book updates less frequently than Bedrock, and this can cause issues with servers that rate limit
 * book packets. Because of this, we need to ensure packets are only send every second or so at maximum.
 */
public class BookEditCache {
    private final GeyserSession session;
    private ClientEditBookPacket packet;
    /**
     * If false, we are expecting to send a book edit packet
     */
    private boolean noBookToSend;
    /**
     * Stores the last time a book update packet was sent to the server.
     */
    private long lastBookUpdate;

    public BookEditCache(GeyserSession session) {
        this.session = session;
        this.noBookToSend = true;
    }

    /**
     * Check to see if there is a book edit update to send, and if so, send it.
     */
    public void checkForSend() {
        if (noBookToSend) {
            // No new packet has to be sent
            return;
        }
        // Prevent kicks due to rate limiting - specifically on Spigot servers
        if ((System.currentTimeMillis() - lastBookUpdate) < 1000) {
            return;
        }
        // Don't send the update if the player isn't not holding a book, shouldn't happen if we catch all interactions
        ItemStack itemStack = session.getInventory().getItemInHand();
        if (itemStack == null || itemStack.getId() != ItemRegistry.WRITABLE_BOOK.getJavaId()) {
            noBookToSend = true;
            return;
        }
        session.getDownstream().getSession().send(packet);
        noBookToSend = true;
        lastBookUpdate = System.currentTimeMillis();
    }

    /**
     * Set a new {@link ClientEditBookPacket} to be sent to the server when ready
     *
     * @param packet the new packet with book information to send to the server
     */
    public void setPacket(ClientEditBookPacket packet) {
        this.packet = packet;
        noBookToSend = false;
    }
}
