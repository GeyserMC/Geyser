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

package org.geysermc.geyser.network.message;

import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.geysermc.geyser.api.network.message.MessageCodec;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class ByteBufCodecLE implements MessageCodec<ByteBufMessageBuffer> {

    ByteBufCodecLE() {
    }

    @Override
    public boolean readBoolean(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readBoolean();
    }

    @Override
    public byte readByte(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readByte();
    }

    @Override
    public short readShort(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readShortLE();
    }

    @Override
    public int readInt(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readIntLE();
    }

    @Override
    public float readFloat(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readFloatLE();
    }

    @Override
    public double readDouble(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readDoubleLE();
    }

    @Override
    public long readLong(@NotNull ByteBufMessageBuffer buffer) {
        return buffer.buffer().readLongLE();
    }

    @Override
    public int readVarInt(@NotNull ByteBufMessageBuffer buffer) {
        return VarInts.readInt(buffer.buffer());
    }

    @Override
    public int readUnsignedVarInt(@NotNull ByteBufMessageBuffer buffer) {
        return VarInts.readUnsignedInt(buffer.buffer());
    }

    @Override
    public long readVarLong(@NotNull ByteBufMessageBuffer buffer) {
        return VarInts.readLong(buffer.buffer());
    }

    @Override
    public long readUnsignedVarLong(@NotNull ByteBufMessageBuffer buffer) {
        return VarInts.readUnsignedLong(buffer.buffer());
    }

    @Override
    public @NotNull String readString(@NotNull ByteBufMessageBuffer buffer) {
        int size = VarInts.readUnsignedInt(buffer.buffer());
        byte[] bytes = new byte[size];
        buffer.buffer().readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void writeBoolean(@NotNull ByteBufMessageBuffer buffer, boolean value) {
        buffer.buffer().writeBoolean(value);
    }

    @Override
    public void writeByte(@NotNull ByteBufMessageBuffer buffer, byte value) {
        buffer.buffer().writeByte(value);
    }

    @Override
    public void writeShort(@NotNull ByteBufMessageBuffer buffer, short value) {
        buffer.buffer().writeShortLE(value);
    }

    @Override
    public void writeInt(@NotNull ByteBufMessageBuffer buffer, int value) {
        buffer.buffer().writeIntLE(value);
    }

    @Override
    public void writeFloat(@NotNull ByteBufMessageBuffer buffer, float value) {
        buffer.buffer().writeFloatLE(value);
    }

    @Override
    public void writeDouble(@NotNull ByteBufMessageBuffer buffer, double value) {
        buffer.buffer().writeDoubleLE(value);
    }

    @Override
    public void writeLong(@NotNull ByteBufMessageBuffer buffer, long value) {
        buffer.buffer().writeLongLE(value);
    }

    @Override
    public void writeVarInt(@NotNull ByteBufMessageBuffer buffer, int value) {
        VarInts.writeInt(buffer.buffer(), value);
    }

    @Override
    public void writeUnsignedVarInt(@NotNull ByteBufMessageBuffer buffer, int value) {
        VarInts.writeUnsignedInt(buffer.buffer(), value);
    }

    @Override
    public void writeVarLong(@NotNull ByteBufMessageBuffer buffer, long value) {
        VarInts.writeLong(buffer.buffer(), value);
    }

    @Override
    public void writeUnsignedVarLong(@NotNull ByteBufMessageBuffer buffer, long value) {
        VarInts.writeUnsignedLong(buffer.buffer(), value);
    }

    @Override
    public void writeString(@NotNull ByteBufMessageBuffer buffer, @NonNull String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        VarInts.writeUnsignedInt(buffer.buffer(), bytes.length);
        buffer.buffer().writeBytes(bytes);
    }

    @Override
    public @NotNull ByteBufMessageBuffer createBuffer() {
        return new ByteBufMessageBuffer(this);
    }

    @Override
    public @NotNull ByteBufMessageBuffer createBuffer(byte @NotNull [] data) {
        return new ByteBufMessageBuffer(this, Unpooled.wrappedBuffer(data));
    }
}
