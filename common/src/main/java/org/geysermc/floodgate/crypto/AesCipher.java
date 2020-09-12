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

import org.geysermc.floodgate.util.InvalidHeaderException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.SecureRandom;

public final class AesCipher implements FloodgateCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BIT_LENGTH = 128;
    private static final String CIPHER_NAME = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public void init(Key key) {
        if (!"AES".equals(key.getAlgorithm())) {
            throw new RuntimeException(
                    "Algorithm was expected to be AES, but got " + key.getAlgorithm()
            );
        }
        secretKey = (SecretKey) key;
    }

    public byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_NAME);

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        byte[] cipherText = cipher.doFinal(data);

        return ByteBuffer.allocate(iv.length + cipherText.length + HEADER_LENGTH)
                .put(IDENTIFIER).put(VERSION) // header
                .put(iv)
                .put(cipherText)
                .array();
    }

    public byte[] decrypt(byte[] cipherTextWithIv) throws Exception {
        HeaderResult pair = checkHeader(cipherTextWithIv);
        if (pair.getVersion() != VERSION) {
            throw new InvalidHeaderException(
                    "Expected version " + VERSION + ", got " + pair.getVersion()
            );
        }

        Cipher cipher = Cipher.getInstance(CIPHER_NAME);

        int bufferLength = cipherTextWithIv.length - HEADER_LENGTH;
        ByteBuffer buffer = ByteBuffer.wrap(cipherTextWithIv, HEADER_LENGTH, bufferLength);

        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);

        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return cipher.doFinal(cipherText);
    }
}
