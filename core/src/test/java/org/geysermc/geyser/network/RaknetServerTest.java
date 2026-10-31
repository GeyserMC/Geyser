/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.netty.util.nethernet.IpRangeSet;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaknetServerTest {
    private static final InetAddress PROXY = address(10, 0, 0, 1);
    private static final InetAddress PLAYER = address(203, 0, 113, 7);

    @Test
    void listedProxiesAloneAdmit() {
        IpRangeSet trusted = IpRangeSet.parse(List.of("10.0.0.0/8"));
        assertTrue(RaknetServer.admitsSender(true, trusted, PROXY));
        assertFalse(RaknetServer.admitsSender(true, trusted, PLAYER));
    }

    @Test
    void noListAdmitsEveryone() {
        assertFalse(RaknetServer.listsProxies(List.of()));
        assertFalse(RaknetServer.listsProxies(Arrays.asList(" ", null)));
        assertFalse(RaknetServer.listsProxies(null));
        assertTrue(RaknetServer.admitsSender(false, IpRangeSet.empty(), PLAYER));
    }

    @Test
    void aListThatParsedToNothingRefusesEveryone() {
        // An invalid prefix is skipped without a DNS lookup
        List<String> listed = List.of("10.0.0.0/99");
        IpRangeSet trusted = IpRangeSet.parse(listed);
        assertTrue(trusted.isEmpty());
        assertTrue(RaknetServer.listsProxies(listed));
        assertFalse(RaknetServer.admitsSender(true, trusted, PROXY));
        assertFalse(RaknetServer.admitsSender(true, trusted, PLAYER));
    }

    private static InetAddress address(int a, int b, int c, int d) {
        try {
            return InetAddress.getByAddress(new byte[] {(byte) a, (byte) b, (byte) c, (byte) d});
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
