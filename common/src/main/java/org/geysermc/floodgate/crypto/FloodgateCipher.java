/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.crypto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.geysermc.floodgate.util.InvalidFormatException;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Responsible for both encrypting and decrypting data
 */
public interface FloodgateCipher {
    // use invalid username characters at the beginning and the end of the identifier,
    // to make sure that it doesn't get messed up with usernames
    byte[] IDENTIFIER = "^Floodgate^".getBytes(StandardCharsets.UTF_8);
    int HEADER_LENGTH = IDENTIFIER.length;

    static boolean hasHeader(String data) {
        if (data.length() < IDENTIFIER.length) {
            return false;
        }

        for (int i = 0; i < IDENTIFIER.length; i++) {
            if (IDENTIFIER[i] != data.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Initializes the instance by giving it the key it needs to encrypt or decrypt data
     *
     * @param key the key used to encrypt and decrypt data
     */
    void init(Key key);

    /**
     * Encrypts the given data using the Key provided in {@link #init(Key)}
     *
     * @param data the data to encrypt
     * @return the encrypted data
     * @throws Exception when the encryption failed
     */
    byte[] encrypt(byte[] data) throws Exception;

    /**
     * Encrypts data from a String.<br> This method internally calls {@link #encrypt(byte[])}
     *
     * @param data the data to encrypt
     * @return the encrypted data
     * @throws Exception when the encryption failed
     */
    default byte[] encryptFromString(String data) throws Exception {
        return encrypt(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts the given data using the Key provided in {@link #init(Key)}
     *
     * @param data the data to decrypt
     * @return the decrypted data
     * @throws Exception when the decrypting failed
     */
    byte[] decrypt(byte[] data) throws Exception;

    /**
     * Decrypts a byte[] and turn it into a String.<br> This method internally calls {@link
     * #decrypt(byte[])} and converts the returned byte[] into a String.
     *
     * @param data the data to encrypt
     * @return the decrypted data in a UTF-8 String
     * @throws Exception when the decrypting failed
     */
    default String decryptToString(byte[] data) throws Exception {
        byte[] decrypted = decrypt(data);
        if (decrypted == null) {
            return null;
        }
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Decrypts a String.<br> This method internally calls {@link #decrypt(byte[])} by converting
     * the UTF-8 String into a byte[]
     *
     * @param data the data to decrypt
     * @return the decrypted data in a byte[]
     * @throws Exception when the decrypting failed
     */
    default byte[] decryptFromString(String data) throws Exception {
        return decrypt(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Checks if the header is valid. This method will throw an InvalidFormatException when the
     * header is invalid.
     *
     * @param data the data to check
     * @throws InvalidFormatException when the header is invalid
     */
    default void checkHeader(byte[] data) throws InvalidFormatException {
        final int identifierLength = IDENTIFIER.length;

        if (data.length <= HEADER_LENGTH) {
            throw new InvalidFormatException("Data length is smaller then header." +
                    "Needed " + HEADER_LENGTH + ", got " + data.length,
                    true
            );
        }

        for (int i = 0; i < identifierLength; i++) {
            if (IDENTIFIER[i] != data[i]) {
                StringBuilder receivedIdentifier = new StringBuilder();
                for (byte b : IDENTIFIER) {
                    receivedIdentifier.append(b);
                }

                throw new InvalidFormatException(
                        String.format("Expected identifier %s, got %s",
                                new String(IDENTIFIER, StandardCharsets.UTF_8),
                                receivedIdentifier.toString()
                        ),
                        true
                );
            }
        }
    }

    @Data
    @AllArgsConstructor
    class HeaderResult {
        private int version;
        private int startIndex;
    }
}
