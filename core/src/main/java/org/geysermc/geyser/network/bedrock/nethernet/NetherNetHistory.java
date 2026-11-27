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

import java.util.ArrayDeque;
import java.util.List;

/**
 * One connection's last {@link #CAPACITY} seconds of stats, one row a second, written out when the connection drops
 * so the minutes before an SCTP timeout or lost ICE connectivity can be read afterwards. Event loop only.
 * <p>
 * A loss collapse shows as timeouts climbing while cwnd sits at one MTU and rto doubles; a downlink that died shows
 * as last_received_ms growing while the server still sends; a backlog shows as send_queue_bytes and queued_bytes
 * growing faster than bytes_out_per_s drains them.
 */
final class NetherNetHistory {
    static final int CAPACITY = 120;
    static final String HEADER = "age_s,rtt_ms,rto_ms,cwnd,unacked_chunks,timeouts,send_queue_bytes,queued_bytes,"
        + "bytes_out_per_s,bytes_in_per_s,last_received_ms,game_rtt_ms,ice_state,writable";

    private final ArrayDeque<String> rows = new ArrayDeque<>(CAPACITY);

    void add(NetherNetStats.Snapshot snapshot) {
        if (rows.size() == CAPACITY) {
            rows.removeFirst();
        }
        rows.addLast(row(snapshot));
    }

    List<String> rows() {
        return List.copyOf(rows);
    }

    static String row(NetherNetStats.Snapshot s) {
        return s.ageSeconds() + "," + s.sctpRtt() + "," + s.sctpRto() + "," + s.congestionWindow() + ","
            + s.unackedChunks() + "," + s.dataTimeouts() + "," + s.sendQueueBytes() + "," + s.queuedBytes() + ","
            + s.bytesOutPerSecond() + "," + s.bytesInPerSecond() + "," + s.lastReceivedMillis() + ","
            + s.gameRoundTripMillis() + "," + s.iceState() + "," + s.writable();
    }
}
