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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.network.RemoteServer;

/**
 * Called when a session has logged in, and is about to connect to a remote java server.
 */
public final class SessionLoginEvent extends ConnectionEvent implements Cancellable {
    private RemoteServer remoteServer;
    private boolean cancelled;

    /**
     * @param connection The connection that is logging in.
     * @param remoteServer The {@link RemoteServer} the section will try to connect to.
     */
    public SessionLoginEvent(@NonNull GeyserConnection connection, RemoteServer remoteServer) {
        super(connection);
        this.remoteServer = remoteServer;
    }

    /**
     * @return The cancel status of the event.
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * @param cancelled If the login event should be cancelled.
     * If cancelled, the player disconnects without connecting to the remote server.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * @return The {@link RemoteServer} the section will connect to.
     */
    public RemoteServer remoteServer() {
        return this.remoteServer;
    }

    /**
     * @param remoteServer Sets the {@link RemoteServer}  to connect to.
     */
    public void remoteServer(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }
}
