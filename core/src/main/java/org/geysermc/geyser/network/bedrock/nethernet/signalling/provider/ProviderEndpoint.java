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

package org.geysermc.geyser.network.bedrock.nethernet.signalling.provider;

import org.cloudburstmc.netty.signalling.admission.EndpointAddress;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Bound addresses plus operator-provisioned forwarding endpoints. Discovery never guesses NAT mappings.
 */
public record ProviderEndpoint(InetSocketAddress bind, List<InetSocketAddress> advertised) {
    public static ProviderEndpoint resolve(InetSocketAddress bind, List<InetSocketAddress> external, boolean localDevelopment) throws IOException {
        if (bind.isUnresolved()) throw new IOException("Provider bind-address could not be resolved");
        return resolve(bind, external, localDevelopment, bind.getAddress().isAnyLocalAddress() ? interfaces() : List.of());
    }

    static ProviderEndpoint resolve(InetSocketAddress bind, List<InetSocketAddress> external, boolean localDevelopment,
                                    List<InetAddress> interfaces) throws IOException {
        if (bind.isUnresolved() || bind.getPort() < 1 || bind.getAddress().isMulticastAddress())
            throw new IOException("Provider bind-address must resolve to a local unicast or wildcard address and fixed UDP port");
        Set<InetSocketAddress> endpoints = new LinkedHashSet<>();
        // Configured mappings are explicit assertions about the forwarder, which may translate IP families.
        for (InetSocketAddress endpoint : external) {
            if (endpoint.isUnresolved() || endpoint.getPort() < 1 || !EndpointAddress.advertisable(endpoint.getAddress(), localDevelopment))
                throw new IOException("Advertised endpoint must be a usable numeric unicast IP and UDP port");
            endpoints.add(endpoint);
        }
        if (!bind.getAddress().isAnyLocalAddress()) {
            // A local proxy may forward a configured external endpoint to a loopback listener.
            if (EndpointAddress.advertisable(bind.getAddress(), localDevelopment)) {
                InetAddress unscoped = InetAddress.getByAddress(bind.getAddress().getAddress());
                endpoints.add(new InetSocketAddress(unscoped, bind.getPort()));
            }
        } else {
            for (InetAddress address : interfaces) {
                // The pinned native listener uses IPV6_V6ONLY=0 for ::. A 0.0.0.0 socket is IPv4 only.
                if (!address.isLoopbackAddress() && EndpointAddress.advertisable(address, localDevelopment)
                        && (!(bind.getAddress() instanceof Inet4Address) || address instanceof Inet4Address)) {
                    InetAddress unscoped = InetAddress.getByAddress(address.getAddress());
                    endpoints.add(new InetSocketAddress(unscoped, bind.getPort()));
                }
            }
        }
        if (endpoints.isEmpty())
            throw new IOException("No usable UDP endpoints; configure nxs.advertise-addresses for external forwarding");
        if (endpoints.size() > 32)
            throw new IOException("More than 32 UDP endpoints; bind to a specific address to limit interface discovery");
        List<InetSocketAddress> sorted = endpoints.stream().sorted(Comparator
                .comparingInt(ProviderEndpoint::rank)
                .thenComparing(endpoint -> endpoint.getAddress().getHostAddress())
                .thenComparingInt(InetSocketAddress::getPort)).toList();
        return new ProviderEndpoint(bind, sorted);
    }

    public List<String> warnings() {
        List<String> messages = new ArrayList<>();
        if (advertised.stream().noneMatch(endpoint -> EndpointAddress.scope(endpoint.getAddress()) == EndpointAddress.Scope.PUBLIC))
            messages.add("Only private/shared or local addresses are advertised. Clients need LAN, VPN or routed connectivity to these endpoints. External signalling providers may advertise them; Warden does not relay game traffic.");
        if (advertised.stream().allMatch(endpoint -> endpoint.getAddress() instanceof Inet6Address))
            messages.add("Only IPv6 endpoints are advertised. Clients without working IPv6 connectivity to this host cannot join. Configure a reachable IPv4 endpoint for IPv4 clients.");
        return List.copyOf(messages);
    }

    private static int rank(InetSocketAddress endpoint) {
        return (EndpointAddress.scope(endpoint.getAddress()) == EndpointAddress.Scope.PUBLIC ? 0 : 2)
                + (endpoint.getAddress() instanceof Inet6Address ? 0 : 1);
    }

    private static List<InetAddress> interfaces() throws IOException {
        List<InetAddress> addresses = new ArrayList<>();
        var interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) return addresses;
        for (NetworkInterface network : Collections.list(interfaces))
            if (network.isUp() && !network.isLoopback()) addresses.addAll(Collections.list(network.getInetAddresses()));
        return addresses;
    }
}
