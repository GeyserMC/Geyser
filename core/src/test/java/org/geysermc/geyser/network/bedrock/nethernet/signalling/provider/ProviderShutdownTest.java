/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock.nethernet.signalling.provider;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderShutdownTest {
    @Test
    void keepsEndpointUntilDrainCompletesAndCleansUpOnce() {
        CompletableFuture<Void> drain = new CompletableFuture<>();
        AtomicInteger stops = new AtomicInteger(), cleanups = new AtomicInteger();
        ProviderShutdown shutdown = new ProviderShutdown(() -> {
            stops.incrementAndGet();
            return drain;
        }, cleanups::incrementAndGet, ignored -> {});
        try {
            shutdown.close();
            shutdown.close();
            assertEquals(1, stops.get());
            // The provider drain is sent over the endpoint, so it must stay open until the drain completes
            assertEquals(0, cleanups.get());
            drain.complete(null);
            shutdown.close();
            assertEquals(1, cleanups.get());
        } finally {
            drain.complete(null);
        }
    }

    @Test
    void failedDrainStillReleasesEndpoint() {
        CompletableFuture<Void> drain = new CompletableFuture<>();
        AtomicInteger cleanups = new AtomicInteger();
        ProviderShutdown shutdown = new ProviderShutdown(() -> drain, cleanups::incrementAndGet, ignored -> {});
        shutdown.close();
        assertEquals(0, cleanups.get());
        drain.completeExceptionally(new IOException("Provider unavailable"));
        assertEquals(1, cleanups.get());
    }
}
