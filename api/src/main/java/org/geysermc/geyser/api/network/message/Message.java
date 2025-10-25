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

package org.geysermc.geyser.api.network.message;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a message that can be sent over the network.
 * @since 2.8.2
 */
public interface Message<T extends MessageBuffer> {

    /**
     * Reads the message from the provided buffer.
     *
     * @param buffer the buffer to read from
     */
    void encode(@NonNull T buffer);

    /**
     * Represents a simple message using the built-in {@link MessageBuffer} implementation.
     */
    interface Simple extends Message<MessageBuffer> {
    }

    /**
     * Represents a packet message with an unknown packet ID.
     *
     * @param <T> the type of message buffer
     */
    interface PacketBase<T extends MessageBuffer> extends Message<T> {
    }

    /**
     * Represents a packet message that includes a packet ID.
     */
    interface Packet extends PacketBase<MessageBuffer> {

        /**
         * Creates a new packet message from the given packet object and direction.
         *
         * @param packet the packet object to create the message from
         * @return a new packet message
         */
        static <T extends MessageBuffer> PacketWrapped<T> of(@NonNull Object packet) {
            return GeyserApi.api().provider(PacketWrapped.class, packet);
        }

        /**
         * Creates a new packet message from the given packet object and direction.
         *
         * @param packetSupplier a supplier that provides the packet object to create the message from
         * @return a new packet message
         */
        @NonNull
        static <T extends MessageBuffer> MessageFactory<T> of(@NonNull Supplier<Object> packetSupplier) {
            return buffer -> of(packetSupplier.get());
        }

        /**
         * Creates a new packet message from the given substitutor and packet supplier.
         *
         * @param substitutor a function that applies to the buffer to get the packet object
         * @param packetSupplier a function that provides the packet object
         * @return a new packet message factory
         */
        @NonNull
        static <T extends MessageBuffer, V> MessageFactory<T> of(@NonNull Function<T, V> substitutor, @NonNull Function<V, Object> packetSupplier) {
            return buffer -> of(packetSupplier.apply(substitutor.apply(buffer)));
        }
    }

    /**
     * Represents a packet message that wraps an underlying packet object.
     *
     * @param <T> the type of message buffer
     */
    interface PacketWrapped<T extends MessageBuffer> extends PacketBase<T> {

        /**
         * Gets the packet associated with this message.
         *
         * @return the packet
         */
        @NonNull
        Object packet();
    }
}
