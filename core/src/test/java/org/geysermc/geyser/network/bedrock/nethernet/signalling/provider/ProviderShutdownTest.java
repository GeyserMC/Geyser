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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProviderShutdownTest {
    @TempDir Path directory;

    @Test void finishesDaemonDrainWhenTheProcessReceivesSigterm() throws Exception { runChild("signal"); }
    @Test void finishesDrainAlreadyStartedByExtensionShutdown() throws Exception { runChild("exit"); }

    @Test void leavesEndpointAliveUntilDrainCompletesAndCleansExactlyOnce() {
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
            assertEquals(0, cleanups.get(), "Endpoint/event loop must remain available during signed drain");
            drain.complete(null);
            shutdown.close();
            assertEquals(1, cleanups.get());
        } finally { drain.complete(null); }
    }

    @Test void failedDrainStillReleasesEndpointResources() {
        CompletableFuture<Void> drain = new CompletableFuture<>();
        AtomicInteger cleanups = new AtomicInteger();
        ProviderShutdown shutdown = new ProviderShutdown(() -> drain, cleanups::incrementAndGet, ignored -> {});
        shutdown.close();
        assertEquals(0, cleanups.get());
        drain.completeExceptionally(new java.io.IOException("Control plane unavailable"));
        assertEquals(1, cleanups.get());
    }

    private void runChild(String mode) throws Exception {
        Path marker = directory.resolve(mode);
        String classes = Path.of(Child.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            + java.io.File.pathSeparator + Path.of(ProviderShutdown.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", classes, Child.class.getName(), mode, marker.toString()).redirectErrorStream(true).start();
        try {
            var reader = child.inputReader();
            String ready = CompletableFuture.supplyAsync(() -> {
                try { return reader.readLine(); } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
            }).get(10, TimeUnit.SECONDS);
            assertEquals("READY", ready);
            if (mode.equals("signal")) child.destroy();
            assertTrue(child.waitFor(10, TimeUnit.SECONDS), "Shutdown exceeded its bounded drain window");
            assertEquals("drained", Files.readString(marker));
        } finally { if (child.isAlive()) child.destroyForcibly().waitFor(); }
    }

    public static class Child {
        public static void main(String[] args) throws Exception {
            ProviderShutdown shutdown = new ProviderShutdown(() -> {
                CompletableFuture<Void> drained = new CompletableFuture<>();
                Thread worker = new Thread(() -> {
                    try {
                        // Daemon cleanup would otherwise be abandoned at JVM exit.
                        Thread.sleep(250);
                        Files.writeString(Path.of(args[1]), "drained");
                        drained.complete(null);
                    } catch (Exception failure) { drained.completeExceptionally(failure); }
                });
                worker.setDaemon(true);
                worker.start();
                return drained;
            }, System.err::println);
            System.out.println("READY");
            if (args[0].equals("exit")) { shutdown.close(); System.exit(0); }
            Thread.sleep(60_000);
        }
    }
}
