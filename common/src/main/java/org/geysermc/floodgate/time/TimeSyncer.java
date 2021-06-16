/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.time;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class TimeSyncer {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private long timeOffset = Long.MIN_VALUE; // value when it failed to get the offset

    public TimeSyncer(String timeServer) {
        executorService.scheduleWithFixedDelay(() -> {
            // 5 tries to get the time offset, since UDP doesn't guaranty a response
            for (int i = 0; i < 5; i++) {
                long offset = SntpClientUtils.requestTimeOffset(timeServer, 3000);
                if (offset != Long.MIN_VALUE) {
                    timeOffset = offset;
                    return;
                }
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public long getTimeOffset() {
        return timeOffset;
    }

    public long getRealMillis() {
        if (hasUsefulOffset()) {
            return System.currentTimeMillis() + getTimeOffset();
        }
        return System.currentTimeMillis();
    }

    public boolean hasUsefulOffset() {
        return timeOffset != Long.MIN_VALUE;
    }
}
