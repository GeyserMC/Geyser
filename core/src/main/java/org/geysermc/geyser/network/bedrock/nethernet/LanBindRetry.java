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

import java.net.BindException;
import java.util.concurrent.Callable;

/**
 * Bounds the transient bind race while Cloudburst's void close() releases discovery asynchronously.
 * Used only on the same startup thread that already performs the blocking channel bind, never on a packet callback.
 * A library close-completion API would let a future caller avoid this retry entirely.
 */
final class LanBindRetry {
    private LanBindRetry() {
    }

    static <T> T bind(Callable<T> attempt, Runnable cleanup) throws Exception {
        return bind(attempt, cleanup, Thread::sleep, 30, 100);
    }

    static <T> T bind(Callable<T> attempt, Runnable cleanup, Sleeper sleeper, int attempts, long delayMillis) throws Exception {
        if (attempts < 1 || delayMillis < 0) throw new IllegalArgumentException("Invalid bind retry bound");
        for (int remaining = attempts; ; remaining--) {
            try {
                return attempt.call();
            } catch (Exception failure) {
                cleanup.run();
                if (failure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw failure;
                }
                if (remaining == 1 || !isBindFailure(failure)) throw failure;
                try {
                    sleeper.sleep(delayMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw interrupted;
                }
            }
        }
    }

    private static boolean isBindFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof BindException) return true;
        }
        return false;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
