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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherNetHistoryTest {

    private static NetherNetStats.Snapshot snapshot(long ageSeconds) {
        return new NetherNetStats.Snapshot(ageSeconds, "CONNECTED", "COMPLETED", null, null,
            14, 14, 10_000, 1184, 1_048_576, 40, 27, 1_429_738, 1_000_000, 1 << 20, 0, 0, true,
            0, 0, 2_500, -1, -1, -1,
            6795, 3, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0.0, 0.0, List.of());
    }

    @Test
    void rowHasAColumnForEveryHeaderColumn() {
        String row = NetherNetHistory.row(snapshot(64));
        assertEquals("64,14,10000,1184,40,27,1000000,1429738,6795,0,2500,-1,COMPLETED,true", row);
        assertEquals(NetherNetHistory.HEADER.split(",").length, row.split(",").length);
    }

    @Test
    void keepsOnlyTheLastRows() {
        NetherNetHistory history = new NetherNetHistory();
        for (int second = 1; second <= NetherNetHistory.CAPACITY + 5; second++) {
            history.add(snapshot(second));
        }
        List<String> rows = history.rows();
        assertEquals(NetherNetHistory.CAPACITY, rows.size());
        assertTrue(rows.get(0).startsWith("6,"));
        assertTrue(rows.get(rows.size() - 1).startsWith((NetherNetHistory.CAPACITY + 5) + ","));
    }

    @Test
    void cwndBasedMaxBurstIsOnlyOverriddenWhenSet() {
        assertNull(NetherNetSctp.parse(null));
        assertNull(NetherNetSctp.parse(" "));
        assertFalse(NetherNetSctp.parse("false"));
        assertTrue(NetherNetSctp.parse(" true "));
    }
}
