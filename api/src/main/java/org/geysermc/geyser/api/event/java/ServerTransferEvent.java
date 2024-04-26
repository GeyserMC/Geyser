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

package org.geysermc.geyser.api.event.java;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;

import java.util.Map;

public class ServerTransferEvent extends ConnectionEvent {

    private final String host;
    private final int port;
    private final Map<String, byte[]> cookies;

    public ServerTransferEvent(@NonNull GeyserConnection connection, String host, int port, Map<String, byte[]> cookies) {
        super(connection);
        this.host = host;
        this.port = port;
        this.cookies = cookies;
    }

    /**
     * The host that the Java server requests a transfer to.
     * @return the host
     */
    public String host() {
        return this.host;
    }

    /**
     * The port that the Java server requests a transfer to.
     * @return the port
     */
    public int port() {
        return this.port;
    }

    /**
     * Gets a map of the sessions current cookies.
     * @return the connections cookies
     */
    public @NonNull Map<String, byte[]> cookies() {
        return cookies;
    }

}
