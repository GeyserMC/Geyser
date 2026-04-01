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
package org.geysermc.geyser.api.event.java

import org.checkerframework.common.value.qual.IntRange
import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.event.connection.ConnectionEvent

/**
 * Fired when the Java server sends a transfer request to a different Java server.
 * Geyser Extensions can listen to this event and set a target server ip/port for Bedrock players to be transferred to.
 */
class ServerTransferEvent(
    connection: GeyserConnection,
    private val host: String, private val port: Int, private val cookies: MutableMap<String?, ByteArray?>
) : ConnectionEvent(connection) {
    private var bedrockHost: String? = null
    private var bedrockPort: Int

    init {
        this.bedrockPort = -1
    }

    /**
     * The host that the Java server requests a transfer to.
     * 
     * @return the host
     */
    fun host(): String {
        return this.host
    }

    /**
     * The port that the Java server requests a transfer to.
     * 
     * @return the port
     */
    fun port(): Int {
        return this.port
    }

    /**
     * The host that the Bedrock player should try and connect to.
     * If this is not set, the Bedrock player will just be disconnected.
     * 
     * @return the host where the Bedrock client will be transferred to, or null if not set.
     */
    fun bedrockHost(): String? {
        return this.bedrockHost
    }

    /**
     * The port that the Bedrock player should try and connect to.
     * If this is not set, the Bedrock player will just be disconnected.
     * 
     * @return the port where the Bedrock client will be transferred to, or -1 if not set.
     */
    fun bedrockPort(): Int {
        return this.bedrockPort
    }

    /**
     * Sets the host for the Bedrock player to be transferred to
     */
    fun bedrockHost(host: String) {
        require(!(host == null || host.isBlank())) { "Server address cannot be null or blank" }
        this.bedrockHost = host
    }

    /**
     * Sets the port for the Bedrock player to be transferred to
     */
    fun bedrockPort(port: @IntRange(from = 0, to = 65535) Int) {
        require(!(port < 0 || port > 65535)) { "Server port must be between 0 and 65535, was " + port }
        this.bedrockPort = port
    }

    /**
     * Gets a map of the sessions current cookies.
     * 
     * @return the connections cookies
     */
    fun cookies(): MutableMap<String?, ByteArray?> {
        return cookies
    }
}
