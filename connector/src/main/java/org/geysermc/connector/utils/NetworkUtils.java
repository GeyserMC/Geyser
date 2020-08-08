/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class NetworkUtils {

    public static String readString(ByteBuf buf) {
        return new String(readByteArray(buf), StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String string) {
        writeByteArray(buf, string.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] readByteArray(ByteBuf buf) {
        byte[] bytes = new byte[readUnsignedVarInt(buf)];
        buf.readBytes(bytes);
        return bytes;
    }

    public static void writeByteArray(ByteBuf buf, byte[] bytes) {
        writeUnsignedVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static int readVarInt(ByteBuf buf) {
        int i = 0, j = 0;
        while (true) {
            int k = buf.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt was too big!");
            }
            if ((k & 0x80) != 128) {
                break;
            }
        }
        return i;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                buf.writeByte(value);
                return;
            }

            buf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
    }

    public static int readUnsignedVarInt(ByteBuf buf) {
        int i = 0;
        for (int j = 0; j < 64; j += 7) {
            final byte b = buf.readByte();
            i |= (long) (b & 0x7F) << j;
            if ((b & 0x80) == 0) {
                return i;
            }
        }
        throw new RuntimeException("Varint was too big!");
    }

    public static void writeUnsignedVarInt(ByteBuf buf, int value) {
        long i = value & 0xFFFFFFFFL;
        while (true) {
            if ((i & ~0x7FL) == 0) {
                buf.writeByte((int) i);
                return;
            } else {
                buf.writeByte((byte) ((i & 0x7F) | 0x80));
                i >>>= 7;
            }
        }
    }
}
