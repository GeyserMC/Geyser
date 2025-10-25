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

package org.geysermc.geyser.api.network;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;

import java.util.Set;

/**
 * Represents the network manager responsible for handling network operations
 * for a {@link GeyserConnection}.
 *
 * @since 2.8.2
 */
public interface NetworkManager {

    /**
     * Gets the registered network channels.
     *
     * @return the registered network channels
     */
    @NonNull
    Set<NetworkChannel> registeredChannels();

    /**
     * Sends a message to this connection on the specified channel.
     *
     * @param channel the channel to send the message on
     * @param message the message to send
     * @param direction the direction of the message (clientbound or serverbound)
     */
    <T extends MessageBuffer> void send(@NonNull NetworkChannel channel, @NonNull Message<T> message, @NonNull MessageDirection direction);
}
