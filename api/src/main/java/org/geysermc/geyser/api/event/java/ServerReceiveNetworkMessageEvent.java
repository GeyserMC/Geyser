/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.java;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.network.MessageDirection;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.message.Message;

/**
 * Called when Geyser receives a network message from the server.
 * @since 2.8.2
 */
public final class ServerReceiveNetworkMessageEvent extends ConnectionEvent implements Cancellable {
    private final NetworkChannel channel;
    private final Message<?> message;
    private final MessageDirection direction;
    private boolean cancelled = false;

    public ServerReceiveNetworkMessageEvent(@NonNull GeyserConnection connection, @NonNull NetworkChannel channel, @NonNull Message<?> message, @NonNull MessageDirection direction) {
        super(connection);

        this.channel = channel;
        this.message = message;
        this.direction = direction;
    }

    /**
     * Gets the channel that received the message.
     * <p>
     * See {@link NetworkChannel} for more information.
     *
     * @return the channel that received the message
     */
    @NonNull
    public NetworkChannel channel() {
        return this.channel;
    }

    /**
     * Gets the message that was received.
     *
     * @return the received message
     */
    @NonNull
    public Message<?> message() {
        return this.message;
    }

    /**
     * Gets the direction of the message.
     *
     * @return the direction of the message
     */
    @NonNull
    public MessageDirection direction() {
        return this.direction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
