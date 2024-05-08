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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.event.Cancellable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.network.RemoteServer;

import java.util.Map;
import java.util.Objects;

/**
 * Called when a session has logged in, and is about to connect to a remote java server.
 * This event is cancellable, and can be used to prevent the player from connecting to the remote server.
 */
public final class SessionLoginEvent extends ConnectionEvent implements Cancellable {
    private RemoteServer remoteServer;
    private boolean cancelled;
    private String disconnectReason;
    private Map<String, byte[]> cookies;
    private boolean transferring;

    public SessionLoginEvent(@NonNull GeyserConnection connection,
                             @NonNull RemoteServer remoteServer,
                             @NonNull Map<String, byte[]> cookies) {
        super(connection);
        this.remoteServer = remoteServer;
        this.cookies = cookies;
        this.transferring = false;
    }

    /**
     * Returns whether the event is cancelled.
     *
     * @return The cancel status of the event.
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Cancels the login event, and disconnects the player.
     * If cancelled, the player disconnects without connecting to the remote server.
     * This method will use a default disconnect reason. To specify one, use {@link #setCancelled(boolean, String)}.
     *
     * @param cancelled If the login event should be cancelled.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Cancels the login event, and disconnects the player with the specified reason.
     * If cancelled, the player disconnects without connecting to the remote server.
     *
     * @param cancelled If the login event should be cancelled.
     * @param disconnectReason The reason for the cancellation.
     */
    public void setCancelled(boolean cancelled, @NonNull String disconnectReason) {
        this.cancelled = cancelled;
        this.disconnectReason = disconnectReason;
    }

    /**
     * Returns the reason for the cancellation, or null if there is no reason given.
     *
     * @return The reason for the cancellation.
     */
    public @Nullable String disconnectReason() {
        return this.disconnectReason;
    }

    /**
     * Gets the {@link RemoteServer} the section will attempt to connect to.
     *
     * @return the {@link RemoteServer} the section will attempt to connect to.
     */
    public @NonNull RemoteServer remoteServer() {
        return this.remoteServer;
    }

    /**
     * Sets the {@link RemoteServer} to connect the session to.
     *
     * @param remoteServer Sets the {@link RemoteServer} to connect to.
     */
    public void remoteServer(@NonNull RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }

    /**
     * Sets a map of cookies from a possible previous session. The Java server can send and request these
     * to store information on the client across server transfers.
     */
    public void cookies(@NonNull Map<String, byte[]> cookies) {
        Objects.requireNonNull(cookies);
        this.cookies = cookies;
    }

    /**
     * Gets a map of the sessions cookies, if set.
     * @return the connections cookies
     */
    public @NonNull Map<String, byte[]> cookies() {
        return cookies;
    }

    /**
     * Determines the connection intent of the connection
     */
    public void transferring(boolean transferring) {
        this.transferring = transferring;
    }

    /**
     * Gets whether this login attempt to the Java server
     * has the transfer intent
     */
    public boolean transferring() {
        return this.transferring;
    }
}
