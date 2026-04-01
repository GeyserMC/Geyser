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
package org.geysermc.geyser.api.event.bedrock

import org.geysermc.event.Cancellable
import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.event.connection.ConnectionEvent
import org.geysermc.geyser.api.network.RemoteServer
import java.util.*

/**
 * Called when a session has logged in, and is about to connect to a remote Java server.
 * This event is cancellable, and can be used to prevent the player from connecting to the remote server.
 */
class SessionLoginEvent(
    connection: GeyserConnection,
    private var remoteServer: RemoteServer,
    private var cookies: MutableMap<String?, ByteArray?>
) : ConnectionEvent(connection), Cancellable {
    private var cancelled = false
    private var disconnectReason: String? = null
    private var transferring = false

    /**
     * Returns whether the event is cancelled.
     * 
     * @return The cancel status of the event.
     */
    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    /**
     * Cancels the login event, and disconnects the player.
     * If cancelled, the player disconnects without connecting to the remote server.
     * This method will use a default disconnect reason. To specify one, use [.setCancelled].
     * 
     * @param cancelled If the login event should be cancelled.
     */
    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

    /**
     * Cancels the login event, and disconnects the player with the specified reason.
     * If cancelled, the player disconnects without connecting to the remote server.
     * 
     * @param cancelled If the login event should be cancelled.
     * @param disconnectReason The reason for the cancellation.
     */
    fun setCancelled(cancelled: Boolean, disconnectReason: String) {
        this.cancelled = cancelled
        this.disconnectReason = disconnectReason
    }

    /**
     * Returns the reason for the cancellation, or null if there is no reason given.
     * 
     * @return The reason for the cancellation.
     */
    fun disconnectReason(): String? {
        return this.disconnectReason
    }

    /**
     * Gets the [RemoteServer] the session will attempt to connect to.
     * 
     * @return the [RemoteServer] the session will attempt to connect to.
     */
    fun remoteServer(): RemoteServer {
        return this.remoteServer
    }

    /**
     * Sets the [RemoteServer] to connect the session to.
     * This method will only work as expected on [PlatformType.STANDALONE],
     * as on other Geyser platforms, the remote server is not determined by Geyser.
     * 
     * @param remoteServer Sets the [RemoteServer] to connect to.
     */
    fun remoteServer(remoteServer: RemoteServer) {
        this.remoteServer = remoteServer
    }

    /**
     * Sets a map of cookies from a possible previous session. The Java server can send and request these
     * to store information on the client across server transfers.
     */
    fun cookies(cookies: MutableMap<String?, ByteArray?>) {
        Objects.requireNonNull<MutableMap<String?, ByteArray?>>(cookies)
        this.cookies = cookies
    }

    /**
     * Gets a map of the sessions cookies, if set.
     * @return the connections cookies
     */
    fun cookies(): MutableMap<String?, ByteArray?> {
        return cookies
    }

    /**
     * Determines the connection intent of the connection
     */
    fun transferring(transferring: Boolean) {
        this.transferring = transferring
    }

    /**
     * Gets whether this login attempt to the Java server
     * has the transfer intent
     */
    fun transferring(): Boolean {
        return this.transferring
    }
}
