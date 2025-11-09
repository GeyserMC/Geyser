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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a data type that can be sent or received over the network.
 *
 * @param <T> the type
 * @since 2.9.1
 */
public final class DataType<T> {
    /**
     * A DataType for reading and writing boolean values.
     */
    public static final DataType<Boolean> BOOLEAN = of(MessageCodec::readBoolean, MessageCodec::writeBoolean);
    /**
     * A DataType for reading and writing byte values.
     */
    public static final DataType<Byte> BYTE = of(MessageCodec::readByte, MessageCodec::writeByte);
    /**
     * A DataType for reading and writing short values.
     */
    public static final DataType<Short> SHORT = of(MessageCodec::readShort, MessageCodec::writeShort);
    /**
     * A DataType for reading and writing integer values.
     */
    public static final DataType<Integer> INT = of(MessageCodec::readInt, MessageCodec::writeInt);
    /**
     * A DataType for reading and writing float values.
     */
    public static final DataType<Float> FLOAT = of(MessageCodec::readFloat, MessageCodec::writeFloat);
    /**
     * A DataType for reading and writing double values.
     */
    public static final DataType<Double> DOUBLE = of(MessageCodec::readDouble, MessageCodec::writeDouble);
    /**
     * A DataType for reading and writing long values.
     */
    public static final DataType<Long> LONG = of(MessageCodec::readLong, MessageCodec::writeLong);
    /**
     * A DataType for reading and writing variable-length integers.
     */
    public static final DataType<Integer> VAR_INT = of(MessageCodec::readVarInt, MessageCodec::writeVarInt);
    /**
     * A DataType for reading and writing unsigned variable-length integers.
     */
    public static final DataType<Integer> UNSIGNED_VAR_INT = of(MessageCodec::readUnsignedVarInt, MessageCodec::writeUnsignedVarInt);
    /**
     * A DataType for reading and writing variable-length long values.
     */
    public static final DataType<Long> VAR_LONG = of(MessageCodec::readVarLong, MessageCodec::writeVarLong);
    /**
     * A DataType for reading and writing unsigned variable-length long values.
     */
    public static final DataType<Long> UNSIGNED_VAR_LONG = of(MessageCodec::readUnsignedVarLong, MessageCodec::writeUnsignedVarLong);
    /**
     * A DataType for reading and writing strings.
     * <p>
     * Note: Strings are encoded in UTF-8 format.
     */
    public static final DataType<String> STRING = of(MessageCodec::readString, MessageCodec::writeString);

    private final Reader<T> reader;
    private final Writer<T> writer;

    private DataType(@NonNull Reader<T> reader, @NonNull Writer<T> writer) {
        this.reader = reader;
        this.writer = writer;
    }

    /**
     * Reads a value of this data type from the given buffer using the specified codec.
     *
     * @param codec the codec to use for reading
     * @param buffer the buffer to read from
     * @return the read value
     */
    @NonNull
    public <B extends MessageBuffer, C extends MessageCodec<B>> T read(@NonNull C codec, @NonNull B buffer) {
        return this.reader.read(codec, buffer);
    }

    /**
     * Writes a value of this data type to the given buffer using the specified codec.
     *
     * @param codec the codec to use for writing
     * @param buffer the buffer to write to
     * @param value the value to write
     */
    public <B extends MessageBuffer, C extends MessageCodec<B>> void write(@NonNull C codec, @NonNull B buffer, @NonNull T value) {
        this.writer.write(codec, buffer, value);
    }

    /**
     * Creates a new DataType with the specified reader and writer.
     *
     * @param reader the reader to use for reading values of this type
     * @param writer the writer to use for writing values of this type
     * @param <T> the type of values this DataType handles
     * @return a new DataType instance
     */
    @NonNull
    public static <T> DataType<T> of(@NonNull Reader<T> reader, @NonNull Writer<T> writer) {
        return new DataType<>(reader, writer);
    }

    /**
     * Creates an optional DataType based on the provided type.
     *
     * @param type the underlying data type to be wrapped in an Optional
     * @return a DataType that reads and writes Optional values
     * @param <T> the type of the value contained in the Optional
     */
    @NonNull
    public static <T> DataType<Optional<T>> optional(@NonNull DataType<T> type) {
        return new DataType<>(new Reader<>() {

            @Override
            @NonNull
            public <B extends MessageBuffer, C extends MessageCodec<B>> Optional<T> read(@NonNull C codec, @NonNull B buffer) {
                if (codec.readBoolean(buffer)) {
                    return Optional.of(type.read(codec, buffer));
                } else {
                    return Optional.empty();
                }
            }
        }, new Writer<>() {

            @Override
            public <B extends MessageBuffer, C extends MessageCodec<B>> void write(@NonNull C codec, @NonNull B buffer, @NonNull Optional<T> value) {
                codec.writeBoolean(buffer, value.isPresent());
                value.ifPresent(t -> type.write(codec, buffer, t));
            }
        });
    }

    /**
     * Creates a DataType that represents a list of values of the specified type.
     *
     * @param type the underlying data type of the list elements
     * @return a DataType that reads and writes lists of the specified type
     * @param <T> the type of the elements in the list
     */
    @NonNull
    public static <T> DataType<List<T>> list(@NonNull DataType<T> type) {
        return new DataType<>(new Reader<>() {

            @Override
            @NonNull
            public <B extends MessageBuffer, C extends MessageCodec<B>> List<T> read(@NonNull C codec, @NonNull B buffer) {
                int size = codec.readUnsignedVarInt(buffer);
                List<T> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(type.read(codec, buffer));
                }

                return list;
            }
        }, new Writer<>() {

            @Override
            public <B extends MessageBuffer, C extends MessageCodec<B>> void write(@NonNull C codec, @NonNull B buffer, @NonNull List<T> value) {
                codec.writeUnsignedVarInt(buffer, value.size());
                for (T element : value) {
                    type.write(codec, buffer, element);
                }
            }
        });
    }

    /**
     * Represents a reader that can read values of a {@link DataType} from a buffer.
     *
     * @param <T> the type of value to read
     */
    public interface Reader<T> {

        /**
         * Reads a value of type {@link T} from the given buffer using the specified {@link C codec}.
         *
         * @param codec the codec to use for reading
         * @param buffer the buffer to read from
         * @return the read value
         */
        @NonNull
        <B extends MessageBuffer, C extends MessageCodec<B>> T read(@NonNull C codec, @NonNull B buffer);
    }

    /**
     * Represents a writer that can write values of a {@link DataType} to a buffer.
     *
     * @param <T> the type of value to write
     */
    public interface Writer<T> {

        /**
         * Writes a value of type {@link T} to the given buffer using the specified {@link C codec}.
         *
         * @param codec the codec to use for writing
         * @param buffer the buffer to write to
         * @param value the value to write
         */
        <B extends MessageBuffer, C extends MessageCodec<B>> void write(@NonNull C codec, @NonNull B buffer, @NonNull T value);
    }
}
