/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinAddressTest {
    private static final int DEFAULT_PORT = 19132;

    private static void assertParsed(String input, String host, int port) {
        assertEquals(new JoinAddress(host, port), JoinAddress.parse(input, DEFAULT_PORT), input);
    }

    @Test
    public void raknetAddresses() {
        assertParsed("example.com:19133", "example.com", 19133);
        assertParsed("127.0.0.1:19132", "127.0.0.1", 19132);
        assertParsed("example.com", "example.com", DEFAULT_PORT);
        // Unbracketed IPv6 keeps the old behaviour: the last segment is the port
        assertParsed("2001:db8::1:19132", "2001:db8::1", 19132);
    }

    @Test
    public void netherNetSignalingUrls() {
        // https://github.com/GeyserMC/Geyser/issues/6710
        assertParsed("http://example.com:19132", "example.com", 19132);
        assertParsed("https://example.com:19133/", "example.com", 19133);
        assertParsed("HTTPS://example.com", "example.com", 443);
        assertParsed("http://example.com/v1/join?x=1", "example.com", 80);
        assertParsed("http://[2001:db8::1]:19132", "2001:db8::1", 19132);
        assertParsed("https://[2001:db8::1]", "2001:db8::1", 443);
    }

    @Test
    public void invalidPortsFallBack() {
        assertParsed("example.com:notaport", "example.com", DEFAULT_PORT);
        assertParsed("example.com:0", "example.com", DEFAULT_PORT);
        assertParsed("example.com:70000", "example.com", DEFAULT_PORT);
    }
}
