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

/**
 * A buffer for messages that can be sent over the network.
 * @since 2.8.2
 */
public interface MessageBuffer {

    /**
     * Reads a {@link T value} from the buffer using the
     * provided {@link DataType}.
     *
     * @param type the type of message to read
     * @return the read message
     * @param <T> the type of message to read
     */
    @NonNull
    <T> T read(@NonNull DataType<T> type);

    /**
     * Writes a {@link T value} to the buffer using the
     * provided {@link DataType}.
     *
     * @param type the type of message to write
     * @param value the value to write
     * @param <T> the type of message to write
     */
    <T> void write(@NonNull DataType<T> type, @NonNull T value);

    /**
     * Serializes the buffer to a byte array.
     *
     * @return the serialized byte array
     */
    byte @NonNull [] serialize();
}
