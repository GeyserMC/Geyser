/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.bedrock;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.event.Cancellable"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.connection.ConnectionEvent"
#include "org.geysermc.geyser.api.network.RemoteServer"
#include "org.geysermc.geyser.api.util.PlatformType"

#include "java.util.Map"
#include "java.util.Objects"


public final class SessionLoginEvent extends ConnectionEvent implements Cancellable {
    private RemoteServer remoteServer;
    private bool cancelled;
    private std::string disconnectReason;
    private Map<std::string, byte[]> cookies;
    private bool transferring;

    public SessionLoginEvent(GeyserConnection connection,
                             RemoteServer remoteServer,
                             Map<std::string, byte[]> cookies) {
        super(connection);
        this.remoteServer = remoteServer;
        this.cookies = cookies;
        this.transferring = false;
    }


    override public bool isCancelled() {
        return this.cancelled;
    }


    override public void setCancelled(bool cancelled) {
        this.cancelled = cancelled;
    }


    public void setCancelled(bool cancelled, std::string disconnectReason) {
        this.cancelled = cancelled;
        this.disconnectReason = disconnectReason;
    }


    public std::string disconnectReason() {
        return this.disconnectReason;
    }


    public RemoteServer remoteServer() {
        return this.remoteServer;
    }


    public void remoteServer(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }


    public void cookies(Map<std::string, byte[]> cookies) {
        Objects.requireNonNull(cookies);
        this.cookies = cookies;
    }


    public Map<std::string, byte[]> cookies() {
        return cookies;
    }


    public void transferring(bool transferring) {
        this.transferring = transferring;
    }


    public bool transferring() {
        return this.transferring;
    }
}
