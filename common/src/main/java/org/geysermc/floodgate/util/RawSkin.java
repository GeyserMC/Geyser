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

package org.geysermc.floodgate.util;

import lombok.AllArgsConstructor;

import java.nio.ByteBuffer;
import java.util.Base64;

import static java.lang.String.format;

@AllArgsConstructor
public final class RawSkin {
    public int width;
    public int height;
    public byte[] data;
    public boolean alex;

    private RawSkin() {
    }

    public static RawSkin decode(byte[] data, int offset) throws InvalidFormatException {
        if (data == null || offset < 0 || data.length <= offset) {
            return null;
        }
        if (offset == 0) {
            return decode(data);
        }

        byte[] rawSkin = new byte[data.length - offset];
        System.arraycopy(data, offset, rawSkin, 0, rawSkin.length);
        return decode(rawSkin);
    }

    public static RawSkin decode(byte[] data) throws InvalidFormatException {
        // offset is an amount of bytes before the Base64 starts
        if (data == null) {
            return null;
        }

        int maxEncodedLength = Base64Utils.getEncodedLength(64 * 64 * 4 + 9);
        // if the RawSkin is longer then the max Java Edition skin length
        if (data.length > maxEncodedLength) {
            throw new InvalidFormatException(format(
                    "Encoded data cannot be longer then %s bytes! Got %s",
                    maxEncodedLength, data.length
            ));
        }

        // if the encoded data doesn't even contain the width, height (8 bytes, 2 ints) and isAlex
        if (data.length < Base64Utils.getEncodedLength(9)) {
            throw new InvalidFormatException("Encoded data must be at least 16 bytes long!");
        }

        data = Base64.getDecoder().decode(data);

        ByteBuffer buffer = ByteBuffer.wrap(data);

        RawSkin skin = new RawSkin();
        skin.width = buffer.getInt();
        skin.height = buffer.getInt();
        if (buffer.remaining() - 1 != (skin.width * skin.height * 4)) {
            throw new InvalidFormatException(format(
                    "Expected skin length to be %s, got %s",
                    (skin.width * skin.height * 4), buffer.remaining()
            ));
        }
        skin.data = new byte[buffer.remaining() - 1];
        buffer.get(skin.data);
        skin.alex = buffer.get() == 1;
        return skin;
    }

    public byte[] encode() {
        // 2 x int + 1 = 9 bytes
        ByteBuffer buffer = ByteBuffer.allocate(9 + data.length);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put(data);
        buffer.put((byte) (alex ? 1 : 0));
        return Base64.getEncoder().encode(buffer.array());
    }
}
