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
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Represents a channel used for network communication.
 * <p>
 * A network channel can either be an external channel or a
 * packet channel. External channels are identified by a unique
 * key and are used for custom payloads over the network. Packet
 * channels will represent data from packets, identified by a packet
 * ID and type.
 * <p>
 * For constructing an external NetworkChannel, the following
 * can be done:
 *
 * <pre>
 * {@code
 *     private final NetworkChannel myChannel = NetworkChannel.of("example", "my_channel", MyMessage.class);
 * }
 * </pre>
 * Or when inside an extension, with 'this' being the extension instance:
 * <pre>
 * {@code
 *     private final NetworkChannel myChannel = NetworkChannel.of(this, "my_channel", MyMessage.class);
 * }
 * </pre>
 *
 * <p>
 * For packet channels, it can get slightly more complex as you need to
 * know the packet ID, understand the constructed message type and have an extension
 * available. The following example demonstrates this with the animate packet,
 * assuming the AnimateMessage class represents the correct packet structure:
 * <pre>
 * {@code
 *    private final NetworkChannel animateChannel = PacketChannel.bedrock(this, 44, AnimateMessage.class);
 * }
 * </pre>
 *
 * <p>
 * Packet channels can also be registered against packet objects from
 * external protocol libraries, such as the ones provided in Geyser. For
 * an example on how to do this, please see the
 * <a href="https://geysermc.org/wiki/geyser/networking-api">Networking API documentation</a>.
 *
 * @since 2.9.2
 */
public interface NetworkChannel {

    /**
     * Gets the identifier that owns this channel.
     *
     * @return the identifier that owns this channel
     */
    @NonNull
    Identifier identifier();

    /**
     * Checks if this channel is a packet channel.
     *
     * @return true if this channel is a packet channel, false otherwise
     */
    boolean isPacket();

    /**
     * Creates a new external {@link NetworkChannel} instance.
     * <p>
     * Extensions should use this method to register
     * their own channels for more robust identification.
     *
     * @param extension the extension that registered this channel
     * @param channel the name of the channel
     * @param messageType the type of the message sent over this channel
     * @return a new external {@link NetworkChannel} instance
     */
    @NonNull
    static NetworkChannel of(@NonNull Extension extension, @NonNull String channel, @NonNull Class<?> messageType) {
        return GeyserApi.api().provider(NetworkChannel.class, extension, channel, messageType);
    }

    /**
     * Creates a new external {@link NetworkChannel} instance.
     * <p>
     * This method is used for external channels provided
     * by third parties, such as plugins or mods.
     *
     * @param id the channel id
     * @param channel the name of the channel
     * @param messageType the type of the message sent over this channel
     * @return a new external {@link NetworkChannel} instance
     */
    @NonNull
    static NetworkChannel of(@NonNull String id, @NonNull String channel, @NonNull Class<?> messageType) {
        return of(Identifier.of(id, channel), messageType);
    }

    /**
     * Creates a new external {@link NetworkChannel} instance.
     * <p>
     * This method is used for external channels provided
     * by third parties, such as plugins or mods.
     *
     * @param identifier the {@link Identifier} of the channel
     * @param messageType the type of the message sent over this channel
     * @return a new external {@link NetworkChannel} instance
     */
    @NonNull
    static NetworkChannel of(@NonNull Identifier identifier, @NonNull Class<?> messageType) {
        return GeyserApi.api().provider(NetworkChannel.class, identifier, messageType);
    }
}
