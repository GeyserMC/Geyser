/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.network;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus;

/**
 * Manages the Nethernet (WebRTC) transport server. Nethernet is Microsoft's
 * peer-to-peer transport used by Education Edition and Xbox for connection
 * ID based joins.
 *
 * Extensions use this to request that Geyser listen for incoming Nethernet
 * connections. Geyser owns the connection ID (persisted in config), handles
 * PlayFab authentication, signaling WebSocket management, and automatic
 * reconnection internally.
 */
@ApiStatus.NonExtendable
public interface NethernetManager {

    /**
     * Starts the Nethernet server. Geyser acquires an anonymous PlayFab
     * MCToken and opens a signaling WebSocket to accept WebRTC connections
     * on the configured connection ID.
     *
     * @return true if the server started successfully
     */
    boolean start();

    /**
     * Stops the Nethernet server and releases all resources.
     */
    void stop();

    /**
     * @return true if the Nethernet server is running and accepting connections
     */
    boolean isRunning();

    /**
     * @return true if the signaling WebSocket to Microsoft is alive
     */
    boolean isSignalingAlive();

    /**
     * Rebuilds the signaling WebSocket with a fresh MCToken, preserving the
     * same connection ID. Existing WebRTC peer connections are unaffected.
     *
     * @return true if the signaling reconnected successfully
     */
    boolean restartSignaling();

    /**
     * Gets the connection ID that Nethernet clients use to connect.
     * Persisted in {@code nethernet/connection-id.yml} and stable across restarts.
     *
     * @return the connection ID (10-18 decimal digits)
     */
    @NonNull
    String getConnectionId();

    /**
     * Gets the PlayFab Messaging ID (pmid) from the MCToken used for signaling.
     * Required for ConnectionType 7 (JSON-RPC) Xbox session discovery.
     *
     * @return the pmsgId, or null if the server hasn't authenticated yet
     */
    @Nullable
    String getPmsgId();
}
