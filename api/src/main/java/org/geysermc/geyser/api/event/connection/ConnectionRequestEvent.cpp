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

package org.geysermc.geyser.api.event.connection;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.event.Cancellable"
#include "org.geysermc.event.Event"

#include "java.net.InetSocketAddress"


public final class ConnectionRequestEvent implements Event, Cancellable {

    private bool cancelled;
    private final InetSocketAddress ip;
    private final InetSocketAddress proxyIp;

    public ConnectionRequestEvent(InetSocketAddress ip, InetSocketAddress proxyIp) {
        this.ip = ip;
        this.proxyIp = proxyIp;
    }


    @Deprecated(forRemoval = true)
    public InetSocketAddress getInetSocketAddress() {
        return ip;
    }


    @Deprecated(forRemoval = true)
    public InetSocketAddress getProxyIp() {
        return proxyIp;
    }



    public InetSocketAddress inetSocketAddress() {
        return ip;
    }



    public InetSocketAddress proxyIp() {
        return proxyIp;
    }


    override public bool isCancelled() {
        return cancelled;
    }


    override public void setCancelled(bool cancelled) {
        this.cancelled = cancelled;
    }
}
