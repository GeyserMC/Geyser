/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.chunk;

import org.cloudburstmc.math.GenericMath;

/**
 * This class is adopted from that of the vanilla client.
 */
public class ChunkBatchSizeCalculator {

    private static final int MAX_OLD_SAMPLES_WEIGHT = 49;
    private static final int CLAMP_COEFFICIENT = 3;

    private double aggregatedNanosPerChunk = 2000000.0;
    private int oldSamplesWeight = 1;
    private volatile long chunkBatchStartTime = getNanos();

    public void onBatchStart() {
        this.chunkBatchStartTime = getNanos();
    }

    public void onBatchFinished(int batchSize) {
        if (batchSize > 0) {
            double batchPeriod = getNanos() - this.chunkBatchStartTime;
            double nanosPerChunk = batchPeriod / batchSize;

            nanosPerChunk = GenericMath.clamp(nanosPerChunk,
                this.aggregatedNanosPerChunk / CLAMP_COEFFICIENT,
                this.aggregatedNanosPerChunk * CLAMP_COEFFICIENT);

            this.aggregatedNanosPerChunk = (this.aggregatedNanosPerChunk * this.oldSamplesWeight + nanosPerChunk) / (this.oldSamplesWeight + 1);

            this.oldSamplesWeight = Math.min(MAX_OLD_SAMPLES_WEIGHT, this.oldSamplesWeight + 1);
        }
    }

    public float getDesiredChunksPerTick() {
        return (float) (7000000.0 / this.aggregatedNanosPerChunk);
    }

    private static long getNanos() {
        return System.nanoTime();
    }
}
