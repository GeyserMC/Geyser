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

package org.geysermc.geyser.hybrid;

import org.geysermc.floodgate.crypto.AesCipher;
import org.geysermc.floodgate.crypto.AesKeyProducer;
import org.geysermc.floodgate.crypto.Base64Topping;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;

public interface HybridProvider {
    void onSkinUpload(GeyserSession session, String value, String signature);

    FloodgateCipher getCipher();

    static FloodgateCipher getOrCreateKey(GeyserImpl geyser) {
        GeyserLogger logger = geyser.getLogger();
        GeyserConfiguration config = geyser.getConfig();
        try {
            // TODO make this common code with Floodgate. Like, make sure Geyser's core and Floodgate's core points to the same thing
            FloodgateCipher cipher = new AesCipher(new Base64Topping());

            Path keyPath = config.getFloodgateKeyPath();
            if (!Files.exists(keyPath)) {
                generateFloodgateKey(cipher, keyPath); // Should also init the cipher for us.
                // TODO good?
                logger.info("We just created a Floodgate key at " + keyPath + ". You will need to copy this file into " +
                        "your Floodgate config folder(s).");
            } else {
                Key key = new AesKeyProducer().produceFrom(keyPath);
                cipher.init(key);
            }
            logger.debug(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.loaded_key"));
            return cipher;
        } catch (Exception exception) {
            logger.severe(GeyserLocale.getLocaleStringLog("geyser.auth.floodgate.bad_key"), exception);
            return null;
        }
    }

    static void generateFloodgateKey(FloodgateCipher cipher, Path keyPath) throws Exception {
        Key key = new AesKeyProducer().produce();
        cipher.init(key);

        String test = "abcdefghijklmnopqrstuvwxyz0123456789";
        byte[] encrypted = cipher.encryptFromString(test);
        String decrypted = cipher.decryptToString(encrypted);

        if (!test.equals(decrypted)) {
            throw new RuntimeException("Failed to decrypt test message.\n" +
                    "Original message: " + test + "." +
                    "Decrypted message: " + decrypted + ".\n" +
                    "The encrypted message itself: " + new String(encrypted)
            );
        }

        Files.write(keyPath, key.getEncoded());
    }
}
