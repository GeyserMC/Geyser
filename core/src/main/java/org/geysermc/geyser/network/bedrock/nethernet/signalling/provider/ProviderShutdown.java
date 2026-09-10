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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Keeps the JVM alive briefly while the provider sends its best-effort drain.
 */
public final class ProviderShutdown implements AutoCloseable {
    private final Supplier<? extends CompletionStage<Void>> stop;
    private final Runnable cleanup;
    private final CompletableFuture<Void> stopped = new CompletableFuture<>();
    private final AtomicBoolean requested = new AtomicBoolean();
    private final Thread hook;

    public ProviderShutdown(Supplier<? extends CompletionStage<Void>> stop, Consumer<String> diagnostics) {
        this(stop, () -> {
        }, diagnostics);
    }

    public ProviderShutdown(Supplier<? extends CompletionStage<Void>> stop, Runnable cleanup, Consumer<String> diagnostics) {
        this.stop = stop;
        this.cleanup = cleanup;
        hook = new Thread(() -> {
            close();
            try {
                stopped.get(20, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception failure) {
                diagnostics.accept("Provider shutdown did not complete within its shutdown window.");
            }
        }, "nethernet-provider-shutdown");
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * Normal extension shutdown stays asynchronous, including on a Netty event loop.
     */
    @Override
    public void close() {
        if (!requested.compareAndSet(false, true)) return;
        try {
            stop.get().whenComplete((ignored, failure) -> finish(failure));
        } catch (RuntimeException failure) {
            finish(failure);
        }
    }

    private void finish(Throwable failure) {
        // The transport's signed drain owns the endpoint until stop completes.
        try {
            cleanup.run();
        } catch (RuntimeException cleanupFailure) {
            if (failure == null) failure = cleanupFailure;
            else failure.addSuppressed(cleanupFailure);
        }
        if (failure == null) stopped.complete(null);
        else stopped.completeExceptionally(failure);
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException shuttingDown) { /* The JVM is already awaiting this hook. */ }
    }
}
