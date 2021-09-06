/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handle null-terminated strings and string arrays
 */
public class StringByteUtil {

    /**
     * Convert string(s) to null terminated byte array
     *
     * @param strings string(s)
     * @return null terminated character array
     */
    public static byte[] stringToBytes(String... strings) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for (String s : strings) {
                byte[] bytes1 = s.getBytes(StandardCharsets.UTF_8);
                bytes.write(bytes1);
                bytes.write(0);
            }
            return bytes.toByteArray();
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Convert strings to null terminated byte array
     *
     * @param strings strings
     * @return null terminated character array
     */
    public static byte[] stringToBytes(Collection<String> strings) {
        return stringToBytes(strings.toArray(new String[0]));
    }

    /**
     * Convert byte array to strings
     *
     * @param bytes byte array
     * @return list of strings. should never be null
     */
    public static List<String> bytesToStrings(byte[] bytes) {
        int stringEnd = 0;
        int stringStart = 0;
        List<String> strings = new ArrayList<>();
        while (true) {

            while (stringEnd < bytes.length && bytes[stringEnd] != 0) {
                stringEnd++;
            }

            if (stringEnd > stringStart) {
                strings.add(new String(bytes, stringStart, stringEnd - stringStart, StandardCharsets.UTF_8));
            }
            stringStart = ++stringEnd; //stringEnd is on a null character. we'll start with a non-null (next)

            if (stringEnd >= bytes.length) return strings;
        }
    }
}
