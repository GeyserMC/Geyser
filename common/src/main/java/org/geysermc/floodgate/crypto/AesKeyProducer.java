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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class AesKeyProducer implements KeyProducer {
    public static final int KEY_SIZE = 128;

    @Override
    public SecretKey produce() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_SIZE, secureRandom());
            return keyGenerator.generateKey();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public SecretKey produceFrom(byte[] keyFileData) {
        try {
            return new SecretKeySpec(keyFileData, "AES");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private SecureRandom secureRandom() throws NoSuchAlgorithmException {
        try {
            
            if (System.getProperty("os.name").startsWith("Windows")) {
                return SecureRandom.getInstance("Windows-PRNG");
            } else {
                
                
                
                return SecureRandom.getInstance("NativePRNGNonBlocking");
                
            }
        } catch (NoSuchAlgorithmException ignored) {
            
            return new SecureRandom();
        }
    }
}
