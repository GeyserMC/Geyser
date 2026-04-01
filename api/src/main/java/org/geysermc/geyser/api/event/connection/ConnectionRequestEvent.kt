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
package org.geysermc.geyser.api.event.connection

import org.geysermc.event.Cancellable
import org.geysermc.event.Event
import java.net.InetSocketAddress

/**
 * Called whenever a client attempts to connect to the server, before the connection is accepted.
 */
class ConnectionRequestEvent(
    /**
     * The IP address of the client attempting to connect
     * 
     * @return the IP address of the client attempting to connect
     */
    @get:Deprecated("Use {@link #inetSocketAddress()} instead") val inetSocketAddress: InetSocketAddress,
    /**
     * The IP address of the proxy handling the connection. It will return null if there is no proxy.
     * 
     * @return the IP address of the proxy handling the connection
     */
    @get:Deprecated("Use {@link #proxyIp()} instead") val proxyIp: InetSocketAddress?
) : Event, Cancellable {
    private var cancelled = false

    /**
     * The IP address of the client attempting to connect
     * 
     * @return the IP address of the client attempting to connect
     */
    fun inetSocketAddress(): InetSocketAddress {
        return this.inetSocketAddress
    }

    /**
     * The IP address of the proxy handling the connection. It will return null if there is no proxy.
     * 
     * @return the IP address of the proxy handling the connection
     */
    fun proxyIp(): InetSocketAddress? {
        return proxyIp
    }

    /**
     * The cancel status of this event. If this event is cancelled, the connection will be rejected.
     * 
     * @return the cancel status of this event
     */
    override fun isCancelled(): Boolean {
        return cancelled
    }

    /**
     * Sets the cancel status of this event. If this event is canceled, the connection will be rejected.
     * 
     * @param cancelled the cancel status of this event.
     */
    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}
