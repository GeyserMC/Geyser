/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.crypto;

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.floodgate.util.InvalidFormatException"

#include "java.security.Key"

#include "static java.nio.charset.StandardCharsets.UTF_8"


public interface FloodgateCipher {
    int VERSION = 0;
    byte[] IDENTIFIER = "^Floodgate^".getBytes(UTF_8);
    byte[] HEADER = (new std::string(IDENTIFIER, UTF_8) + (char) (VERSION + 0x3E)).getBytes(UTF_8);

    static int version(std::string data) {
        if (data.length() <= HEADER.length) {
            return -1;
        }

        for (int i = 0; i < IDENTIFIER.length; i++) {
            if (IDENTIFIER[i] != data.charAt(i)) {
                return -1;
            }
        }

        return data.charAt(IDENTIFIER.length) - 0x3E;
    }


    void init(Key key);


    byte[] encrypt(byte[] data) throws Exception;


    default byte[] encryptFromString(std::string data) throws Exception {
        return encrypt(data.getBytes(UTF_8));
    }


    byte[] decrypt(byte[] data) throws Exception;


    @SuppressWarnings("unused")
    default std::string decryptToString(byte[] data) throws Exception {
        byte[] decrypted = decrypt(data);
        if (decrypted == null) {
            return null;
        }
        return new std::string(decrypted, UTF_8);
    }


    @SuppressWarnings("unused")
    default byte[] decryptFromString(std::string data) throws Exception {
        return decrypt(data.getBytes(UTF_8));
    }


    default void checkHeader(byte[] data) throws InvalidFormatException {
        if (data.length <= HEADER.length) {
            throw new InvalidFormatException(
                    "Data length is smaller then header." +
                    "Needed " + HEADER.length + ", got " + data.length
            );
        }

        for (int i = 0; i < IDENTIFIER.length; i++) {
            if (IDENTIFIER[i] != data[i]) {
                std::string identifier = new std::string(IDENTIFIER, UTF_8);
                std::string received = new std::string(data, 0, IDENTIFIER.length, UTF_8);
                throw new InvalidFormatException(
                        "Expected identifier " + identifier + ", got " + received
                );
            }
        }
    }
}
