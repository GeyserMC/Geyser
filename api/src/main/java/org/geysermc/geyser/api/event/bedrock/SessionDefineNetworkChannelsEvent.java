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

package org.geysermc.geyser.api.event.bedrock;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageCodec;
import org.geysermc.geyser.api.network.message.MessageFactory;

/**
 * Called whenever Geyser is registering network channels.
 * @since 2.8.2
 */
public abstract class SessionDefineNetworkChannelsEvent extends ConnectionEvent {

    public SessionDefineNetworkChannelsEvent(@NonNull GeyserConnection connection) {
        super(connection);
    }

    /**
     * Registers a new network channel with a message factory.
     *
     * @param channel the channel to register
     * @param messageFactory the factory to create messages from the buffer
     */
    public abstract void register(@NonNull NetworkChannel channel, @NonNull MessageFactory<MessageBuffer> messageFactory);

    /**
     * Registers a new network channel with a message factory.
     *
     * @param channel the channel to register
     * @param messageFactory the factory to create messages from the buffer
     */
    public abstract <T extends MessageBuffer> void register(@NonNull NetworkChannel channel, @NonNull MessageCodec<T> codec, @NonNull MessageFactory<T> messageFactory);
}
