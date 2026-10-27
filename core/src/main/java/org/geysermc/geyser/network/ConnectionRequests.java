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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;
import org.geysermc.geyser.text.GeyserLocale;

import java.net.InetSocketAddress;

/**
 * The pre-authentication gate every inbound connection passes, whichever transport carried it.
 */
public final class ConnectionRequests {

    private ConnectionRequests() {
    }

    /**
     * Fires {@link ConnectionRequestEvent} and reports whether the connection may proceed.
     * <p>
     * Extensions use this to refuse a peer by address, before any session exists, so every transport
     * has to ask before it creates one.
     *
     * @param geyser        The running instance
     * @param clientAddress The address the connection came from
     * @param proxyAddress  The proxy that forwarded it, or null when the transport has no proxy support
     * @return true when the connection may proceed
     */
    public static boolean accept(GeyserImpl geyser, InetSocketAddress clientAddress,
                                 @Nullable InetSocketAddress proxyAddress) {
        String ip = geyser.config().logPlayerIpAddresses() ? String.valueOf(clientAddress) : "<IP address withheld>";

        ConnectionRequestEvent requestEvent = new ConnectionRequestEvent(clientAddress, proxyAddress);
        geyser.eventBus().fire(requestEvent);
        if (requestEvent.isCancelled()) {
            geyser.getLogger().debug("Connection request from " + ip + " was cancelled using the API!");
            return false;
        }

        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.network.attempt_connect", ip));
        return true;
    }
}
