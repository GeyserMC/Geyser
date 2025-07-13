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

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.extension.Extension;

/**
 * Represents a channel used for network communication.
 * @since 2.8.2
 */
public interface NetworkChannel {

    /**
     * Gets the key that owns this channel.
     *
     * @return the key that owns this channel
     */
    @NonNull
    String key();

    /**
     * Gets the name of the channel.
     *
     * @return the channel name
     */
    @NonNull
    String channel();

    /**
     * Checks if this channel is a packet channel.
     *
     * @return true if this channel is a packet channel, false otherwise
     */
    boolean isPacket();

    /**
     * Creates a new {@link NetworkChannel} instance.
     *
     * @param extension the extension that registered this channel
     * @param channel the name of the channel
     * @return a new {@link NetworkChannel} instance
     */
    @NonNull
    static NetworkChannel of(@NonNull Extension extension, @NonNull String channel) {
        return new ExtensionNetworkChannel(extension, channel);
    }

    /**
     * Creates a new {@link NetworkChannel} instance.
     *
     * @param id the channel id
     * @param channel the name of the channel
     * @return a new {@link NetworkChannel} instance
     */
    @NonNull
    static NetworkChannel of(@NonNull String id, @NonNull String channel) {
        return new ExternalNetworkChannel(id, channel);
    }

    /**
     * Creates a new {@link PacketChannel} instance for a packet channel.
     *
     * @param key the packet key
     * @param packetId the packet ID
     * @param packetType the type of the packet
     * @return a new {@link PacketChannel} instance for a packet channel
     */
    static NetworkChannel packet(@NonNull String key, @NonNegative int packetId, @NonNull Class<?> packetType) {
        return new PacketChannel(key, packetId, packetType);
    }
}
