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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.network.message.DataType;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageCodec;

public record ByteBufMessageBuffer(MessageCodec<ByteBufMessageBuffer> codec, ByteBuf buffer) implements MessageBuffer {

    public ByteBufMessageBuffer(MessageCodec<ByteBufMessageBuffer> codec) {
        this(codec, Unpooled.buffer());
    }

    @Override
    public <T> @NonNull T read(@NonNull DataType<T> type) {
        return type.read(this.codec, this);
    }

    @Override
    public <T> void write(@NonNull DataType<T> type, @NonNull T value) {
        type.write(this.codec, this, value);
    }

    @Override
    public byte @NonNull [] serialize() {
        byte[] bytes = new byte[this.buffer.readableBytes()];
        this.buffer.readBytes(bytes);
        this.buffer.clear(); // Clear the buffer after serialization
        return bytes;
    }

    @Override
    public int length() {
        return this.buffer.readableBytes();
    }
}
