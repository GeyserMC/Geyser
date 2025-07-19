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
 * A codec for encoding and decoding messages.
 * 
 * @param <T> the type of {@link MessageBuffer} used for encoding and decoding
 * @since 2.8.2
 */
public interface MessageCodec<T extends MessageBuffer> {

    /**
     * Reads a boolean value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the boolean value read
     */
    boolean readBoolean(@NonNull T buffer);

    /**
     * Reads a byte value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the byte value read
     */
    byte readByte(@NonNull T buffer);

    /**
     * Reads a short value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the short value read
     */
    short readShort(@NonNull T buffer);

    /**
     * Reads an integer value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the integer value read
     */
    int readInt(@NonNull T buffer);

    /**
     * Reads a float value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the float value read
     */
    float readFloat(@NonNull T buffer);

    /**
     * Reads a double value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the double value read
     */
    double readDouble(@NonNull T buffer);

    /**
     * Reads a long value from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the long value read
     */
    long readLong(@NonNull T buffer);

    /**
     * Reads a variable-length integer from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the variable-length integer read
     */
    int readVarInt(@NonNull T buffer);

    /**
     * Reads an unsigned variable-length integer from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the unsigned variable-length integer read
     */
    int readUnsignedVarInt(@NonNull T buffer);

    /**
     * Reads a variable-length long from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the variable-length long read
     */
    long readVarLong(@NonNull T buffer);
    
    /**
     * Reads an unsigned variable-length long from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the unsigned variable-length long read
     */
    long readUnsignedVarLong(@NonNull T buffer);

    /**
     * Reads a string from the {@link T buffer}.
     *
     * @param buffer the buffer to read from
     * @return the string read
     */
    @NonNull
    String readString(@NonNull T buffer);

    /**
     * Writes a boolean value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the boolean value to write
     */
    void writeBoolean(@NonNull T buffer, boolean value);

    /**
     * Writes a byte value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the byte value to write
     */
    void writeByte(@NonNull T buffer, byte value);

    /**
     * Writes a short value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the short value to write
     */
    void writeShort(@NonNull T buffer, short value);

    /**
     * Writes an integer value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the integer value to write
     */
    void writeInt(@NonNull T buffer, int value);

    /**
     * Writes a float value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the float value to write
     */
    void writeFloat(@NonNull T buffer, float value);

    /**
     * Writes a double value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the double value to write
     */
    void writeDouble(@NonNull T buffer, double value);

    /**
     * Writes a long value to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the long value to write
     */
    void writeLong(@NonNull T buffer, long value);

    /**
     * Writes a variable-length integer to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the variable-length integer to write
     */
    void writeVarInt(@NonNull T buffer, int value);

    /**
     * Writes an unsigned variable-length integer to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the unsigned variable-length integer to write
     */
    void writeUnsignedVarInt(@NonNull T buffer, int value);

    /**
     * Writes a variable-length long to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the variable-length long to write
     */
    void writeVarLong(@NonNull T buffer, long value);

    /**
     * Writes an unsigned variable-length long to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the unsigned variable-length long to write
     */
    void writeUnsignedVarLong(@NonNull T buffer, long value);

    /**
     * Writes a string to the {@link T buffer}.
     *
     * @param buffer the buffer to write to
     * @param value  the string to write
     */
    void writeString(@NonNull T buffer, @NonNull String value);

    /**
     * Creates a new {@link T buffer} instance.
     *
     * @return a new instance of {@link MessageBuffer}
     */
    @NonNull
    T createBuffer();

    /**
     * Creates a new {@link T buffer} instance with the given data.
     *
     * @param data the byte array to initialize the buffer with
     * @return a new instance of {@link MessageBuffer} initialized with the provided data
     */
    @NonNull
    T createBuffer(byte @NonNull [] data);
}
