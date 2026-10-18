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

import java.net.InetSocketAddress;
import java.util.Locale;

/**
 * The hostname and port a Bedrock player used to reach this server.
 * <p>
 * RakNet clients send {@code host:port}, while NetherNet clients send the signaling URL they used,
 * e.g. {@code http://example.com:19132}, and pings over built-in signaling carry an HTTP Host header.
 *
 * @param host the hostname or IP address, without IPv6 brackets
 * @param port the port, or the default port when none was given
 */
public record JoinAddress(String host, int port) {

    /**
     * @param address the address to parse, with or without a scheme, port or path
     * @param defaultPort the port to use when the address has none and its scheme does not imply one
     */
    public static JoinAddress parse(String address, int defaultPort) {
        String remaining = address.trim();

        int schemeEnd = remaining.indexOf("://");
        if (schemeEnd != -1) {
            String scheme = remaining.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
            if (scheme.equals("https")) {
                defaultPort = 443;
            } else if (scheme.equals("http")) {
                defaultPort = 80;
            }
            remaining = remaining.substring(schemeEnd + 3);
        }

        // Drop any path, query or fragment, then any user info
        for (int i = 0; i < remaining.length(); i++) {
            char c = remaining.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                remaining = remaining.substring(0, i);
                break;
            }
        }
        remaining = remaining.substring(remaining.lastIndexOf('@') + 1);

        if (remaining.startsWith("[")) {
            int end = remaining.indexOf(']');
            if (end != -1) {
                String rest = remaining.substring(end + 1);
                int port = rest.startsWith(":") ? parsePort(rest.substring(1), defaultPort) : defaultPort;
                return new JoinAddress(remaining.substring(1, end), port);
            }
        }

        int colon = remaining.lastIndexOf(':');
        if (colon == -1) {
            return new JoinAddress(remaining, defaultPort);
        }
        // Unbracketed IPv6 from RakNet clients also ends in the port
        return new JoinAddress(remaining.substring(0, colon), parsePort(remaining.substring(colon + 1), defaultPort));
    }

    public InetSocketAddress toUnresolved() {
        return InetSocketAddress.createUnresolved(host, port);
    }

    private static int parsePort(String port, int defaultPort) {
        try {
            int parsed = Integer.parseInt(port);
            return parsed > 0 && parsed <= 65535 ? parsed : defaultPort;
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }
}
