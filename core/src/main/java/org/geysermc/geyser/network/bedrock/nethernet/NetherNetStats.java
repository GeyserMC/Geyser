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

import io.netty.util.AttributeKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelMetrics;
import org.cloudburstmc.netty.channel.nethernet.config.NetherConnectionFailure;
import tel.schich.libdatachannel.IceState;
import tel.schich.libdatachannel.PeerState;
import tel.schich.libdatachannel.SctpStats;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Per-connection NetherNet counters, to tell a send backlog apart from packet loss or plain distance.
 * The channel reports wire traffic from its event loop and native threads; Geyser's pipeline reports batches.
 * Everything here is safe to read from any thread.
 */
public final class NetherNetStats implements NetherChannelMetrics {
    public static final AttributeKey<NetherNetStats> ATTRIBUTE = AttributeKey.valueOf("geyser-nethernet-stats");

    private final long createdNanos = System.nanoTime();

    private final RollingCounter bytesIn = new RollingCounter();
    private final RollingCounter bytesOut = new RollingCounter();
    private final RollingCounter messagesIn = new RollingCounter();
    private final RollingCounter messagesOut = new RollingCounter();
    private final LongAdder fragmentsIn = new LongAdder();
    private final LongAdder fragmentsOut = new LongAdder();
    private final LongAdder decodeFailures = new LongAdder();

    private final RollingCounter packetsOut = new RollingCounter();
    private final LongAdder batchesOut = new LongAdder();
    private final LongAdder uncompressedOut = new LongAdder();
    private final LongAdder compressedOut = new LongAdder();

    private final LongAdder unwritableEvents = new LongAdder();
    private volatile long unwritableSinceNanos;
    private volatile long lastReceivedNanos = createdNanos;
    private volatile String peerState = "NEW";
    private volatile String iceState = "NEW";
    private volatile @Nullable String failure;

    @Override
    public void bytesIn(int bytes) {
        bytesIn.add(bytes);
        lastReceivedNanos = System.nanoTime();
    }

    @Override
    public void bytesOut(int bytes) {
        bytesOut.add(bytes);
    }

    @Override
    public void messagesIn(int messages) {
        messagesIn.add(messages);
    }

    @Override
    public void messagesOut(int messages) {
        messagesOut.add(messages);
    }

    @Override
    public void fragmentsIn(int fragments) {
        fragmentsIn.add(fragments);
    }

    @Override
    public void fragmentsOut(int fragments) {
        fragmentsOut.add(fragments);
    }

    @Override
    public void decodeFail(int bytes) {
        decodeFailures.increment();
    }

    @Override
    public void peerStateChange(PeerState state) {
        peerState = state.name();
    }

    @Override
    public void iceStateChange(IceState state) {
        iceState = state.name();
    }

    @Override
    public void connectionFailed(NetherConnectionFailure failure) {
        this.failure = String.valueOf(failure);
    }

    /**
     * A compressed batch about to be sent, with how many Bedrock packets it holds.
     */
    void batchOut(int packets, int uncompressedBytes) {
        batchesOut.increment();
        packetsOut.add(packets);
        uncompressedOut.add(uncompressedBytes);
    }

    void compressedOut(int bytes) {
        compressedOut.add(bytes);
    }

    void writabilityChanged(boolean writable) {
        if (writable) {
            unwritableSinceNanos = 0;
        } else {
            unwritableEvents.increment();
            unwritableSinceNanos = System.nanoTime();
        }
    }

    /**
     * Reads the counters and the channel's live SCTP state.
     *
     * @param includeAddresses whether to include the addresses of the selected ICE pair, not just their types
     */
    public Snapshot snapshot(NetherNetChannel channel, boolean includeAddresses) {
        long now = System.nanoTime();
        @Nullable SctpStats sctp = channel.isOpen() ? channel.sctpStats() : null;
        NetherNetChannel.@Nullable Path path = channel.isOpen() ? channel.selectedPath() : null;

        long batches = batchesOut.sum();
        long uncompressed = uncompressedOut.sum();
        long compressed = compressedOut.sum();
        long unwritableSince = unwritableSinceNanos;

        String pathDescription = null;
        if (path != null) {
            pathDescription = includeAddresses
                ? path.localType() + " " + path.local() + " -> " + path.remoteType() + " " + path.remote()
                : path.localType() + " -> " + path.remoteType();
        }

        return new Snapshot(
            TimeUnit.NANOSECONDS.toSeconds(now - createdNanos),
            peerState,
            iceState,
            failure,
            pathDescription,
            channel.getPing(),
            sctp == null ? -1 : sctp.rtt().toMillis(),
            sctp == null ? -1 : sctp.rto().toMillis(),
            sctp == null ? -1 : sctp.congestionWindow(),
            sctp == null ? -1 : sctp.peerReceiveWindow(),
            sctp == null ? -1 : sctp.unackedChunks(),
            sctp == null ? -1 : sctp.dataTimeouts(),
            NetherNetQueue.queuedBytes(channel),
            sctp == null ? -1 : sctp.pendingChunks(),
            channel.isWritable(),
            unwritableEvents.sum(),
            unwritableSince == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(now - unwritableSince),
            TimeUnit.NANOSECONDS.toMillis(now - lastReceivedNanos),
            bytesOut.rate(), messagesOut.rate(), packetsOut.rate(),
            bytesIn.rate(), messagesIn.rate(),
            bytesOut.total(), messagesOut.total(), fragmentsOut.sum(), packetsOut.total(),
            bytesIn.total(), messagesIn.total(), fragmentsIn.sum(), decodeFailures.sum(),
            batches == 0 ? 0 : (double) packetsOut.total() / batches,
            uncompressed == 0 ? 0 : (double) compressed / uncompressed
        );
    }

    /**
     * @param ping the peer connection's round trip time in milliseconds, 0 if unknown
     * @param sctpRtt SCTP's smoothed round trip time in milliseconds, -1 if unknown (as for all SCTP fields)
     * @param congestionWindow bytes SCTP may have in flight; one that stays near its initial size caps throughput
     * @param unackedChunks chunks sent but not yet acknowledged, so at most about a congestion window's worth
     * @param dataTimeouts retransmission timeouts so far, which indicate loss
     * @param queuedBytes bytes waiting in the data channels for SCTP to take them, -1 if unknown. This only grows once
     *                    SCTP's own send buffer (1 MiB) is full, and what that buffer holds can't be read yet
     * @param receiveQueueChunks chunks received from the client that SCTP holds for reassembly or until they are read
     *                           (usrsctp's sstat_penddata). Not the send backlog
     * @param unwritableMillis how long the channel has been over its write buffer high watermark, 0 if it is not
     * @param averageBatchSize Bedrock packets per NetherNet message
     * @param compressionRatio compressed size over uncompressed size of what was sent
     */
    public record Snapshot(
        long ageSeconds,
        String peerState,
        String iceState,
        @Nullable String failure,
        @Nullable String path,
        long ping,
        long sctpRtt,
        long sctpRto,
        long congestionWindow,
        long peerReceiveWindow,
        long unackedChunks,
        long dataTimeouts,
        long queuedBytes,
        long receiveQueueChunks,
        boolean writable,
        long unwritableEvents,
        long unwritableMillis,
        long lastReceivedMillis,
        long bytesOutPerSecond,
        long messagesOutPerSecond,
        long packetsOutPerSecond,
        long bytesInPerSecond,
        long messagesInPerSecond,
        long bytesOut,
        long messagesOut,
        long fragmentsOut,
        long packetsOut,
        long bytesIn,
        long messagesIn,
        long fragmentsIn,
        long decodeFailures,
        double averageBatchSize,
        double compressionRatio
    ) {
    }

    /**
     * A total plus per-second buckets for the last few seconds, so rates need no scheduler.
     */
    static final class RollingCounter {
        private static final int BUCKETS = 8;
        /** Full seconds averaged; the current, partial second is left out */
        private static final int WINDOW = 5;

        private final LongAdder total = new LongAdder();
        private final long[] counts = new long[BUCKETS];
        private final long[] seconds = new long[BUCKETS];

        void add(long amount) {
            total.add(amount);
            long second = currentSecond();
            int slot = (int) Math.floorMod(second, BUCKETS);
            synchronized (this) {
                if (seconds[slot] != second) {
                    seconds[slot] = second;
                    counts[slot] = 0;
                }
                counts[slot] += amount;
            }
        }

        long total() {
            return total.sum();
        }

        synchronized long rate() {
            long now = currentSecond();
            long sum = 0;
            for (int i = 1; i <= WINDOW; i++) {
                long second = now - i;
                int slot = (int) Math.floorMod(second, BUCKETS);
                if (seconds[slot] == second) {
                    sum += counts[slot];
                }
            }
            return sum / WINDOW;
        }

        private static long currentSecond() {
            return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
        }
    }
}
