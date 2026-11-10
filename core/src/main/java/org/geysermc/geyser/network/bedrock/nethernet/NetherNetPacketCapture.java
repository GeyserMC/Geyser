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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.geysermc.geyser.session.GeyserSession;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Writes every packet of one player's session, Java and Bedrock in both directions, with its contents, to a file for
 * a few seconds, for the nethernet packets command. Contents come from each packet's toString, cut at
 * {@link #MAX_CONTENT_CHARS}. They include chat, names and skins, so a capture should only be shared privately.
 */
public final class NetherNetPacketCapture {
    public static final int MAX_CONTENT_CHARS = 4000;
    /** Stops a capture before it fills a disk, whatever its length */
    private static final int MAX_LINES = 500_000;
    private static final Map<Channel, NetherNetPacketCapture> ACTIVE = new ConcurrentHashMap<>();
    private static final String END = "";

    public enum Direction {
        JAVA_IN, JAVA_OUT, BEDROCK_IN, BEDROCK_OUT
    }

    private final Channel channel;
    private final Path file;
    private final long startNanos = System.nanoTime();
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Map<Direction, Map<String, Integer>> counts = new ConcurrentHashMap<>();
    private final Consumer<String> onDone;
    private volatile int written;

    private NetherNetPacketCapture(Channel channel, Path file, Consumer<String> onDone) {
        this.channel = channel;
        this.file = file;
        this.onDone = onDone;
    }

    /**
     * @param channel the session's upstream channel
     * @return false if this session is already being captured
     */
    public static boolean start(Channel channel, Path file, List<String> header, int seconds, Consumer<String> onDone) throws IOException {
        NetherNetPacketCapture capture = new NetherNetPacketCapture(channel, file, onDone);
        if (ACTIVE.putIfAbsent(channel, capture) != null) {
            return false;
        }
        BufferedWriter writer;
        try {
            Files.createDirectories(file.getParent());
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
            for (String line : header) {
                writer.write("# " + line);
                writer.newLine();
            }
            writer.write("t_ms\tdirection\tpacket\tcontents");
            writer.newLine();
        } catch (IOException | RuntimeException e) {
            ACTIVE.remove(channel);
            throw e;
        }
        Thread thread = new Thread(() -> capture.write(writer), "Geyser NetherNet packet capture");
        thread.setDaemon(true);
        thread.start();
        channel.eventLoop().schedule(capture::stop, seconds, TimeUnit.SECONDS);
        channel.closeFuture().addListener(future -> capture.stop());
        return true;
    }

    /** Java packets, from the session's downstream handlers. */
    public static void java(GeyserSession session, Direction direction, Object packet) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        NetherNetPacketCapture capture = ACTIVE.get(session.getUpstream().getSession().getPeer().getChannel());
        if (capture != null) {
            capture.add(direction, packet.getClass().getSimpleName(), packet);
        }
    }

    /** A Bedrock packet the client sent, as it is translated. */
    public static void bedrockIn(GeyserSession session, Object packet) {
        java(session, Direction.BEDROCK_IN, packet);
    }

    /** A Bedrock packet about to be batched and sent; its object is gone if it was sent pre-encoded. */
    static void bedrockOut(Channel channel, BedrockPacketWrapper wrapper) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        NetherNetPacketCapture capture = ACTIVE.get(channel);
        if (capture != null) {
            Object packet = wrapper.getPacket();
            capture.add(Direction.BEDROCK_OUT, packet == null ? "id " + wrapper.getPacketId() : packet.getClass().getSimpleName(),
                packet == null ? "(pre-encoded, " + (wrapper.getPacketBuffer() == null ? "?" : wrapper.getPacketBuffer().readableBytes()) + " bytes)" : packet);
        }
    }

    private void add(Direction direction, String type, @Nullable Object packet) {
        if (stopped.get()) {
            return;
        }
        counts.computeIfAbsent(direction, key -> new ConcurrentHashMap<>()).merge(type, 1, Integer::sum);
        String contents;
        try {
            // Taken now: packets are pooled and reused once handled
            contents = String.valueOf(packet);
        } catch (RuntimeException e) {
            contents = "(toString failed: " + e + ")";
        }
        if (contents.length() > MAX_CONTENT_CHARS) {
            contents = contents.substring(0, MAX_CONTENT_CHARS) + "... (" + contents.length() + " chars)";
        }
        long t = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        lines.add(t + "\t" + direction + "\t" + type + "\t" + contents.replace('\n', ' ').replace('\t', ' '));
    }

    private void stop() {
        if (stopped.compareAndSet(false, true)) {
            ACTIVE.remove(channel, this);
            lines.add(END);
        }
    }

    private void write(BufferedWriter writer) {
        String reason = null;
        try (writer) {
            while (true) {
                String line = lines.take();
                if (line == END) {
                    break;
                }
                if (written >= MAX_LINES) {
                    reason = "stopped after " + MAX_LINES + " packets";
                    stop();
                    continue;
                }
                writer.write(line);
                writer.newLine();
                written++;
            }
        } catch (IOException e) {
            reason = "writing failed: " + e.getMessage();
            stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
        onDone.accept("Captured " + written + " packets to " + file + (reason == null ? "" : " (" + reason + ")") + ": " + summary());
    }

    /** Packets per second in each direction, with the most frequent types. */
    String summary() {
        double seconds = Math.max(1, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)) / 1000.0;
        StringBuilder builder = new StringBuilder();
        for (Direction direction : Direction.values()) {
            Map<String, Integer> byType = counts.getOrDefault(direction, new HashMap<>());
            int total = byType.values().stream().mapToInt(Integer::intValue).sum();
            String top = byType.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(String.format(Locale.ROOT, "%s %.0f/s", direction, total / seconds));
            if (!top.isEmpty()) {
                builder.append(" (").append(top).append(")");
            }
        }
        return builder.toString();
    }
}
