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

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionUtil {
    public static String encrypt(Key key, String data) throws IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        SecretKey secretKey = generator.generateKey();

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedText = cipher.doFinal(data.getBytes());

        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(key instanceof PublicKey ? Cipher.PUBLIC_KEY : Cipher.PRIVATE_KEY, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(secretKey.getEncoded())) + '\0' +
                Base64.getEncoder().encodeToString(encryptedText);
    }

    public static String encryptBedrockData(Key key, BedrockData data) throws IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return encrypt(key, data.toString());
    }

    public static byte[] decrypt(Key key, String encryptedData) throws IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        String[] split = encryptedData.split("\0");
        if (split.length != 2) {
            throw new IllegalArgumentException("Expected two arguments, got " + split.length);
        }

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(key instanceof PublicKey ? Cipher.PUBLIC_KEY : Cipher.PRIVATE_KEY, key);
        byte[] decryptedKey = cipher.doFinal(Base64.getDecoder().decode(split[0]));

        SecretKey secretKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(Base64.getDecoder().decode(split[1]));
    }

    public static BedrockData decryptBedrockData(Key key, String encryptedData) throws IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return BedrockData.fromRawData(decrypt(key, encryptedData));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Key> T getKeyFromFile(Path fileLocation, Class<T> keyType) throws
            IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        boolean isPublicKey = keyType == PublicKey.class;
        if (!isPublicKey && keyType != PrivateKey.class) {
            throw new RuntimeException("I can only read public and private keys!");
        }

        byte[] key = Files.readAllBytes(fileLocation);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec keySpec = isPublicKey ? new X509EncodedKeySpec(key) : new PKCS8EncodedKeySpec(key);
        return (T) (isPublicKey ?
                keyFactory.generatePublic(keySpec) :
                keyFactory.generatePrivate(keySpec)
        );
    }
}
