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

package org.geysermc.geyser.network.bedrock.nethernet;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * How {@link NetherNetServer#ADDITIONAL_HEADERS_ENV} is read. Network checks the names and values themselves.
 */
class AdditionalHeadersTest {

    @Test
    void readsEachEntry() {
        assertEquals(Map.of("X-A", "1", "X-B", "a:b"), NetherNetServer.responseHeaders(" X-A: 1 , X-B:a:b"));
    }

    @Test
    void keepsACommaInsideAValue() {
        assertEquals(Map.of("Cache-Control", "no-cache, no-store", "X-A", "1"),
            NetherNetServer.responseHeaders("Cache-Control: no-cache, no-store, X-A: 1"));
    }

    @Test
    void keepsAColonInsideAValue() {
        assertEquals(Map.of("Alt-Svc", "h3=\":443\", h2=\":443\""),
            NetherNetServer.responseHeaders("Alt-Svc: h3=\":443\", h2=\":443\""));
    }

    @Test
    void joinsANameGivenTwice() {
        assertEquals(Map.of("X-A", "1, 2"), NetherNetServer.responseHeaders("X-A: 1, X-A: 2"));
    }

    @Test
    void readsNothingFromABlankValue() {
        assertEquals(Map.of(), NetherNetServer.responseHeaders(" , "));
    }

    @Test
    void refusesAValueWithNoNameBeforeIt() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
            () -> NetherNetServer.responseHeaders("no-store, X-A: 1"));
        assertEquals("\"no-store\" is not \"Name: value\"", refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> NetherNetServer.responseHeaders(": 1"));
    }
}
