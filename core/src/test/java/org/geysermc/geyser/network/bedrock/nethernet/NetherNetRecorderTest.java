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

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetherNetRecorderTest {

    @Test
    void summaryShowsTimeoutsWhileDataStaysInFlight() {
        // What usrsctp does with data in flight all the time: a timeout every 800 ms, some cutting cwnd to one MTU
        NetherNetRecorder.Summary summary = new NetherNetRecorder.Summary();
        long timeouts = 10;
        for (long t = 0; t <= 4000; t += 20) {
            if (t > 0 && t % 800 == 0) {
                timeouts++;
            }
            long cwnd = t % 1600 == 0 && t > 0 ? 1184 : 11720;
            summary.add(t, 5, timeouts, cwnd);
        }
        assertEquals("201 samples over 4.0 s, 5 timeouts (1.25/s, median 800 ms apart), cwnd cut to one MTU 2 times, "
            + "data in flight 100% of the time", summary.toString());
    }
}
