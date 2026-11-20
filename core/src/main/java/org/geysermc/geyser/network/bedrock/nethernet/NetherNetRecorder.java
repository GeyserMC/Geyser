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

import io.netty.channel.Channel;
import io.netty.util.concurrent.ScheduledFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import tel.schich.libdatachannel.SctpStats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Samples one connection's SCTP state every {@link #INTERVAL_MILLIS} ms into a CSV file, for the nethernet record
 * command.
 * <p>
 * Timeouts that come evenly spaced while data stays in flight, with no loss, point at the sender's retransmission
 * timer rather than the network. game_rtt_ms is the last NetworkStackLatency round trip (see
 * {@link NetherNetStats.Snapshot#gameRoundTripMillis()}): when it grows far past rtt_ms while unacked chunks stay low,
 * the backlog is in the client, not the network.
 */
public final class NetherNetRecorder {
    public static final long INTERVAL_MILLIS = 20;
    static final String HEADER = "t_ms,rtt_ms,rto_ms,cwnd,unacked_chunks,timeouts,game_rtt_ms";
    private static final Set<Channel> RECORDING = ConcurrentHashMap.newKeySet();

    private final NetherNetChannel channel;
    private final BufferedWriter writer;
    private final Path file;
    private final long startNanos = System.nanoTime();
    private final long endNanos;
    private final Consumer<String> onDone;
    private final Summary summary = new Summary();
    // Only touched on the channel's event loop
    private @Nullable ScheduledFuture<?> task;
    private boolean finished;

    private NetherNetRecorder(NetherNetChannel channel, Path file, BufferedWriter writer, int seconds, Consumer<String> onDone) {
        this.channel = channel;
        this.file = file;
        this.writer = writer;
        this.endNanos = startNanos + TimeUnit.SECONDS.toNanos(seconds);
        this.onDone = onDone;
    }

    /**
     * @param header comment lines written ahead of the CSV header, without the leading #
     * @param onDone receives a one-line summary once the recording ends
     * @return false if this connection is already being recorded
     */
    public static boolean start(NetherNetChannel channel, Path file, List<String> header, int seconds,
                                Consumer<String> onDone) throws IOException {
        if (!RECORDING.add(channel)) {
            return false;
        }
        BufferedWriter writer;
        try {
            writer = NetherNetRecordFiles.open(file, header, HEADER);
        } catch (IOException | RuntimeException e) {
            RECORDING.remove(channel);
            throw e;
        }
        NetherNetRecorder recorder = new NetherNetRecorder(channel, file, writer, seconds, onDone);
        channel.eventLoop().execute(() -> {
            recorder.task = channel.eventLoop().scheduleWithFixedDelay(recorder::sample, 0, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            // Channel listeners run on the event loop
            channel.closeFuture().addListener(future -> recorder.finish("the connection closed"));
        });
        return true;
    }

    private void sample() {
        if (finished) {
            return;
        }
        long now = System.nanoTime();
        if (now >= endNanos) {
            finish(null);
            return;
        }
        @Nullable SctpStats sctp = channel.sctpStats();
        if (sctp == null) {
            finish("SCTP stats became unavailable");
            return;
        }
        long t = TimeUnit.NANOSECONDS.toMillis(now - startNanos);
        NetherNetStats stats = channel.attr(NetherNetStats.ATTRIBUTE).get();
        long gameRoundTrip = stats == null ? -1 : stats.lastGameRoundTripMillis();
        try {
            writer.write(t + "," + sctp.rtt().toMillis() + "," + sctp.rto().toMillis() + "," + sctp.congestionWindow()
                + "," + sctp.unackedChunks() + "," + sctp.dataTimeouts() + "," + gameRoundTrip);
            writer.newLine();
        } catch (IOException e) {
            finish("writing failed: " + e.getMessage());
            return;
        }
        summary.add(t, sctp.unackedChunks(), sctp.dataTimeouts(), sctp.congestionWindow());
    }

    private void finish(@Nullable String reason) {
        if (finished) {
            return;
        }
        finished = true;
        if (task != null) {
            task.cancel(false);
        }
        try {
            writer.close();
        } catch (IOException ignored) {
            // Nothing more can be written, and the summary below still reaches the sender
        }
        RECORDING.remove(channel);
        onDone.accept("Recorded " + file + (reason == null ? "" : " (stopped early: " + reason + ")") + ": " + summary);
    }

    /**
     * What a recording shows at a glance: how often timeouts came, how far apart, and whether data was in flight
     * all along.
     */
    static final class Summary {
        // One MTU is about 1.2 KB: a window that falls under 2 KB from above twice that was cut by a timeout
        private static final long CUT_FROM_ABOVE = 2400;
        private static final long CUT_BELOW = 2000;

        private long samples;
        private long inFlightSamples;
        private long firstTimeouts = -1;
        private long lastTimeouts;
        private long lastTimeoutAt = -1;
        private final List<Long> spacings = new ArrayList<>();
        private long collapses;
        private long lastCwnd = -1;
        private long lastMillis;

        void add(long millis, long unackedChunks, long dataTimeouts, long congestionWindow) {
            samples++;
            lastMillis = millis;
            if (unackedChunks > 0) {
                inFlightSamples++;
            }
            if (firstTimeouts < 0) {
                firstTimeouts = dataTimeouts;
                lastTimeouts = firstTimeouts;
            } else if (dataTimeouts > lastTimeouts) {
                if (lastTimeoutAt >= 0) {
                    spacings.add(millis - lastTimeoutAt);
                }
                lastTimeoutAt = millis;
                lastTimeouts = dataTimeouts;
            }
            // A cut that recovers within one sample on a fast link is missed
            if (lastCwnd > CUT_FROM_ABOVE && congestionWindow < CUT_BELOW) {
                collapses++;
            }
            lastCwnd = congestionWindow;
        }

        @Override
        public String toString() {
            long timeouts = firstTimeouts < 0 ? 0 : lastTimeouts - firstTimeouts;
            String spacing = "n/a";
            if (!spacings.isEmpty()) {
                List<Long> sorted = spacings.stream().sorted().toList();
                spacing = sorted.get(sorted.size() / 2) + " ms";
            }
            double seconds = Math.max(lastMillis, 1) / 1000.0;
            return String.format(Locale.ROOT,
                "%d samples over %.1f s, %d timeouts (%.2f/s, median %s apart), cwnd cut to one MTU %d times, data in flight %.0f%% of the time",
                samples, seconds, timeouts, timeouts / seconds, spacing, collapses,
                samples == 0 ? 0.0 : 100.0 * inFlightSamples / samples);
        }
    }
}
