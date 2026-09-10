/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.netty.transport;

import org.geysermc.geyser.network.netty.GeyserServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * A way for Bedrock clients (or a proxy acting on their behalf) to reach Geyser. Geyser ships with a RakNet
 * transport; extensions can add others (e.g. TCP/QUIC for a Bedrock proxy) via
 * {@link GeyserDefineBedrockTransportsEvent}.
 * <p>
 * A transport creates and binds its own Netty server, builds the per-connection pipeline that yields a
 * {@link org.cloudburstmc.protocol.bedrock.BedrockServerSession}, and hands it to Geyser through
 * {@link GeyserServer#getSessionInitializer()}. It is bound once at startup and shut down once at stop.
 */
public interface GeyserBedrockTransport {

    /** A short, unique identifier, e.g. {@code raknet}. Used for logging and to identify transports to remove. */
    String id();

    /**
     * Binds this transport. The built-in RakNet transport uses {@code defaultBindAddress}; others may bind to
     * their own configured address. Returns a future that completes once bound (or exceptionally on failure).
     */
    CompletableFuture<Void> bind(GeyserServer server, InetSocketAddress defaultBindAddress);

    /** Releases all resources held by this transport. Called on shutdown. */
    void shutdown();
}
