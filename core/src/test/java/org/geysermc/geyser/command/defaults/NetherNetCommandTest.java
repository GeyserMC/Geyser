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

package org.geysermc.geyser.command.defaults;

import org.cloudburstmc.netty.signaling.ProviderClient;
import org.geysermc.geyser.network.bedrock.nethernet.NetherNetServer.Advertised.Builtin;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetherNetCommandTest {
    private static final InetSocketAddress IPV4 = new InetSocketAddress("198.51.100.1", 19132);
    private static final InetSocketAddress IPV6 = new InetSocketAddress("2001:db8::1", 19133);

    @Test
    void builtinListsTheConfiguredAddresses() {
        assertEquals("Built-in signaling advertises 198.51.100.1:19132, [2001:db8::1]:19133.",
            NetherNetCommand.builtinAdvertised(new Builtin(List.of(IPV4, IPV6), false, List.of())));
    }

    @Test
    void builtinWithoutConfiguredAddressesSaysWhatStunFound() {
        assertEquals("Built-in signaling advertises this machine's own addresses, and 198.51.100.1:19132 found with STUN.",
            NetherNetCommand.builtinAdvertised(new Builtin(List.of(), true, List.of(IPV4))));
        assertEquals("Built-in signaling advertises this machine's own addresses. STUN discovery has not found a public address yet.",
            NetherNetCommand.builtinAdvertised(new Builtin(List.of(), true, List.of())));
        assertEquals("Built-in signaling advertises this machine's own addresses. STUN discovery is off.",
            NetherNetCommand.builtinAdvertised(new Builtin(List.of(), false, List.of())));
    }

    @Test
    void nxsListsWhatTheServiceConfirmed() {
        assertEquals("External signaling publishes 198.51.100.1:19132, [2001:db8::1]:19133. Assisted joins are offered over IPv4 and IPv6.",
            NetherNetCommand.nxsPublished(new ProviderClient.PublishedEndpoints(List.of(IPV4, IPV6), Set.of(6, 4))));
        assertEquals("External signaling publishes 198.51.100.1:19132.",
            NetherNetCommand.nxsPublished(new ProviderClient.PublishedEndpoints(List.of(IPV4), Set.of())));
        assertEquals("External signaling publishes no addresses.",
            NetherNetCommand.nxsPublished(new ProviderClient.PublishedEndpoints(List.of(), Set.of())));
        assertEquals("External signaling has no addresses confirmed by the service right now.",
            NetherNetCommand.nxsPublished(null));
    }
}
