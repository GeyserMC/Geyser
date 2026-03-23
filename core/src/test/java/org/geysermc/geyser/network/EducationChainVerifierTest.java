/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EducationChainVerifierTest {

    // ---- padBase64 ----

    @Test
    void padBase64_noPaddingNeeded() {
        // Length divisible by 4 — no padding added
        String input = "abcd"; // length 4
        assertEquals("abcd", EducationChainVerifier.padBase64(input));
    }

    @Test
    void padBase64_onePadding() {
        // Length % 4 == 3 — needs 1 pad
        String input = "abc"; // length 3
        assertEquals("abc=", EducationChainVerifier.padBase64(input));
    }

    @Test
    void padBase64_twoPadding() {
        // Length % 4 == 2 — needs 2 pads
        String input = "ab"; // length 2
        assertEquals("ab==", EducationChainVerifier.padBase64(input));
    }

    @Test
    void padBase64_threePadding() {
        // Length % 4 == 1 — needs 3 pads
        String input = "a"; // length 1
        assertEquals("a===", EducationChainVerifier.padBase64(input));
    }

    @Test
    void padBase64_longerString() {
        // 5 chars: 5 % 4 == 1, needs 3 pads
        assertEquals("abcde===", EducationChainVerifier.padBase64("abcde"));
        // 6 chars: 6 % 4 == 2, needs 2 pads
        assertEquals("abcdef==", EducationChainVerifier.padBase64("abcdef"));
        // 7 chars: 7 % 4 == 3, needs 1 pad
        assertEquals("abcdefg=", EducationChainVerifier.padBase64("abcdefg"));
        // 8 chars: 8 % 4 == 0, no padding
        assertEquals("abcdefgh", EducationChainVerifier.padBase64("abcdefgh"));
    }

    @Test
    void padBase64_realBase64UrlDecodes() {
        // Ensure padded output is valid for Base64URL decoding
        String original = "Hello, World!";
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(original.getBytes(StandardCharsets.UTF_8));
        String padded = EducationChainVerifier.padBase64(encoded);
        byte[] decoded = Base64.getUrlDecoder().decode(padded);
        assertEquals(original, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    void padBase64_emptyString() {
        // Empty string: 0 % 4 == 0, no padding
        assertEquals("", EducationChainVerifier.padBase64(""));
    }
}
