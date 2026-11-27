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

package org.geysermc.geyser.network.bedrock.nethernet;

import org.junit.jupiter.api.Test;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetDiscovery;
import java.util.concurrent.atomic.AtomicReference;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LanBindRetryTest {
    @Test
    void actualDiscoveryCanRebindAfterAsynchronousClose() throws Exception {
        int port;
        try (var reservation = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0))) {
            port = reservation.getLocalPort();
        }
        var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        var current = new AtomicReference<NetherNetDiscovery>();
        Runnable close = () -> {
            var discovery = current.getAndSet(null);
            if (discovery != null) discovery.close();
        };
        try {
            for (int reload = 0; reload < 5; reload++) {
                close.run();
                LanBindRetry.bind(() -> {
                    var discovery = new NetherNetDiscovery(1234);
                    current.set(discovery);
                    discovery.bind(address);
                    assertTrue(discovery.isActive());
                    return discovery;
                }, close);
            }
        } finally {
            close.run();
        }
    }

    @Test
    void releasesOldDatagramSocketBeforeRetryWithoutChangingPort() throws Exception {
        var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        try (var oldSocket = new DatagramSocket(address)) {
            var occupied = new InetSocketAddress(InetAddress.getLoopbackAddress(), oldSocket.getLocalPort());
            var cleanups = new AtomicInteger();
            try (var replacement = LanBindRetry.bind(() -> new DatagramSocket(occupied), cleanups::incrementAndGet,
                    delay -> oldSocket.close(), 3, 0)) {
                assertEquals(occupied.getPort(), replacement.getLocalPort());
                assertEquals(1, cleanups.get());
            }
        }
    }

    @Test
    void permanentConflictStopsAtBoundAndCleansEveryAttempt() {
        var attempts = new AtomicInteger();
        var cleanups = new AtomicInteger();
        var waits = new AtomicInteger();
        assertThrows(BindException.class, () -> LanBindRetry.bind(() -> {
            attempts.incrementAndGet();
            throw new BindException("still owned");
        }, cleanups::incrementAndGet, ignored -> waits.incrementAndGet(), 3, 100));
        assertEquals(3, attempts.get());
        assertEquals(3, cleanups.get());
        assertEquals(2, waits.get());
    }

    @Test
    void unrelatedFailureDoesNotRetry() {
        var attempts = new AtomicInteger();
        var cleanups = new AtomicInteger();
        assertThrows(IOException.class, () -> LanBindRetry.bind(() -> {
            attempts.incrementAndGet();
            throw new IOException("invalid identity");
        }, cleanups::incrementAndGet, ignored -> fail("must not retry"), 3, 100));
        assertEquals(1, attempts.get());
        assertEquals(1, cleanups.get());
    }

    @Test
    void wrappedBindFailureCanRetry() throws Exception {
        var attempts = new AtomicInteger();
        var result = LanBindRetry.bind(() -> {
            if (attempts.incrementAndGet() == 1) throw new IOException(new BindException("closing"));
            return "ready";
        }, () -> {}, ignored -> {}, 3, 100);
        assertEquals("ready", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void interruptedWaitPreservesCancellationAndNeverStartsAnotherAttempt() {
        var attempts = new AtomicInteger();
        try {
            assertThrows(InterruptedException.class, () -> LanBindRetry.bind(() -> {
                attempts.incrementAndGet();
                throw new BindException("closing");
            }, () -> {}, ignored -> { throw new InterruptedException(); }, 3, 100));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, attempts.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void distinctValidIcePortRequired() {
        assertEquals(19134, NetherNetServer.lanIcePort(19134, 19133));
        for (int port : new int[]{0, -1, 65536, 7551, 19133}) {
            assertThrows(IllegalArgumentException.class, () -> NetherNetServer.lanIcePort(port, 19133));
        }
    }
}
