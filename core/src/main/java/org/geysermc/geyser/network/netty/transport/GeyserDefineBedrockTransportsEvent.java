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

import org.geysermc.event.Event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Fired before Geyser binds its Bedrock listeners. The transport list starts with Geyser's built-in RakNet
 * transport; extensions may add, remove, or replace entries to change how Bedrock clients reach Geyser.
 * <p>
 * Lives in core rather than the API module because a transport necessarily deals with Netty channels.
 */
public final class GeyserDefineBedrockTransportsEvent implements Event {
    private final List<GeyserBedrockTransport> transports;

    public GeyserDefineBedrockTransportsEvent(List<GeyserBedrockTransport> transports) {
        this.transports = transports;
    }

    /** Adds a transport to bind. */
    public void register(GeyserBedrockTransport transport) {
        this.transports.add(Objects.requireNonNull(transport, "transport"));
    }

    /** Removes transports matching the filter, e.g. {@code removeIf(t -> t.id().equals("raknet"))}. */
    public boolean removeIf(Predicate<? super GeyserBedrockTransport> filter) {
        return this.transports.removeIf(filter);
    }

    /** The transports currently scheduled to bind, in order. */
    public List<GeyserBedrockTransport> transports() {
        return Collections.unmodifiableList(this.transports);
    }
}