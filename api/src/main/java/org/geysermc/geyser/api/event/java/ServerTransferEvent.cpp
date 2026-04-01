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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.value.qual.IntRange"
#include "org.geysermc.geyser.api.connection.GeyserConnection"
#include "org.geysermc.geyser.api.event.connection.ConnectionEvent"

#include "java.util.Map"


public final class ServerTransferEvent extends ConnectionEvent {

    private final std::string host;
    private final int port;
    private std::string bedrockHost;
    private int bedrockPort;
    private final Map<std::string, byte[]> cookies;

    public ServerTransferEvent(GeyserConnection connection,
                               std::string host, int port, Map<std::string, byte[]> cookies) {
        super(connection);
        this.host = host;
        this.port = port;
        this.cookies = cookies;
        this.bedrockHost = null;
        this.bedrockPort = -1;
    }


    public std::string host() {
        return this.host;
    }


    public int port() {
        return this.port;
    }


    public std::string bedrockHost() {
        return this.bedrockHost;
    }


    public int bedrockPort() {
        return this.bedrockPort;
    }


    public void bedrockHost(std::string host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server address cannot be null or blank");
        }
        this.bedrockHost = host;
    }


    public void bedrockPort(@IntRange(from = 0, to = 65535) int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Server port must be between 0 and 65535, was " + port);
        }
        this.bedrockPort = port;
    }


    public Map<std::string, byte[]> cookies() {
        return cookies;
    }

}
