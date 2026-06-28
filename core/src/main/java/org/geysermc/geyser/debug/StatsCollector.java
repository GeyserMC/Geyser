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

package org.geysermc.geyser.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLongArray;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.display.slot.DisplaySlot;
import org.geysermc.geyser.session.GeyserSession;
import org.openjdk.jol.info.GraphLayout;

public class StatsCollector {
    private static final boolean ENABLED = System.getenv("STATS_COLLECTOR_ENABLED") != null;

    private static final Queue<String> allocationLinesToWrite = new ConcurrentLinkedQueue<>();
    private static volatile long allocationLastSafeNanos = System.nanoTime();

    private static volatile long count;
    private static volatile long totalComponentSize;
    private static volatile long totalStringSize;
    private static final Object lock = new Object();

    private static final AtomicLongArray packetCounts = new AtomicLongArray(ScoreboardPacketType.length);

    public static void addAllocStats(Component component, String string) {
        if (!ENABLED) {
            return;
        }

        final long componentSize = GraphLayout.parseInstance(component).totalSize();
        final long stringSize = GraphLayout.parseInstance(string).totalSize();

        synchronized (lock) {
            if (totalComponentSize + componentSize < 0 || totalStringSize + stringSize < 0) {
                // Overflow protection, we have to write to disk now.

                double percentage = 100.0D - (double) totalStringSize / totalComponentSize * 100.0;
                allocationLinesToWrite.add(String.format("%-15s %-10s %-20s %-20s %-20s %-5.2f (overflow)%n", System.currentTimeMillis(), count, totalComponentSize, totalStringSize, totalComponentSize - totalStringSize, percentage));

                allocationLastSafeNanos = System.nanoTime();
                count = 0;
                totalComponentSize = 0;
                totalStringSize = 0;
            }

            count += 1;
            totalComponentSize += componentSize;
            totalStringSize += stringSize;

            long now = System.nanoTime();
            if (now - allocationLastSafeNanos > 180_000_000_000L) { // Every 30m
                double percentage = 100.0D - (double) totalStringSize / totalComponentSize * 100.0;
                allocationLinesToWrite.add(String.format("%-15s %-10s %-20s %-20s %-20s %.2f%n", System.currentTimeMillis(), count, totalComponentSize, totalStringSize, totalComponentSize - totalStringSize, percentage));

                allocationLastSafeNanos = now;
                count = 0;
                totalComponentSize = 0;
                totalStringSize = 0;
            }
        }
    }

    public static void addPacketCount(ScoreboardPacketType packetType) {
        if (!ENABLED) {
            return;
        }

        packetCounts.incrementAndGet(packetType.ordinal());
    }

    static {
        if (ENABLED) {
            GeyserImpl instance = GeyserImpl.getInstance();
            GeyserLogger logger = instance.getLogger();

            long ctm = System.currentTimeMillis();
            Path allocationStatsPath = instance.configDirectory().resolve("memdiff-alloc-stats-" + ctm + ".txt");
            logger.info("Writing allocation stats to " + allocationStatsPath);
            Path scoreStatsPath = instance.configDirectory().resolve("score-stats-" + ctm + ".txt");
            logger.info("Writing score stats to " + scoreStatsPath);
            Path packetStatsPath = instance.configDirectory().resolve("packet-stats-" + ctm + ".txt");
            logger.info("Writing packet stats to " + packetStatsPath);

            try {
                // Just to be sure, attempt to write to it to see if we have perms. Otherwise, the while loop will just fail indefinitely
                Files.writeString(allocationStatsPath, "", StandardOpenOption.CREATE);
                Files.writeString(scoreStatsPath, "", StandardOpenOption.CREATE);
                Files.writeString(packetStatsPath, "", StandardOpenOption.CREATE);
            } catch (IOException e) {
                logger.error("Failed to ensure that the stats files were created!", e);
                throw new RuntimeException(e);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Write remaining available stats immediately

                String remainingStats;
                synchronized (lock) {
                    double percentage = 100.0D - (double) totalStringSize / totalComponentSize * 100.0;
                    remainingStats = String.format("%-15s %-10s %-20s %-20s %-20s %-5.2f (shutdown)%n", System.currentTimeMillis(), count, totalComponentSize, totalStringSize, totalComponentSize - totalStringSize, percentage);
                    allocationLinesToWrite.add(remainingStats);
                }

                try {
                    writeToDisk(instance, allocationStatsPath, scoreStatsPath, packetStatsPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));

            new Thread(() -> {
                while (!instance.isShuttingDown()) {
                    try {
                        Thread.sleep(60_000);
                        writeToDisk(instance, allocationStatsPath, scoreStatsPath, packetStatsPath);
                    } catch (Throwable ignored) {
                    }
                }
            }, "Geyser - DebugStatsWriter").start();
        }
    }

    private static void writeToDisk(GeyserImpl impl, Path allocationStatsPath, Path scoreStatsPath, Path packetStatsPath) throws IOException {
        writeAllocLines(allocationStatsPath);
        writeScoreStats(impl, scoreStatsPath);
        writePacketStats(packetStatsPath);
    }

    private static void writeAllocLines(Path path) throws IOException {
        String line;
        while ((line = allocationLinesToWrite.poll()) != null) {
            Files.writeString(path, line, StandardOpenOption.APPEND);
        }
    }

    private static void writeScoreStats(GeyserImpl geyser, Path path) throws IOException {
        long playerCount = 0;
        long belowNameScoreCount = 0;
        long sidebarScoreCount = 0;
        long tabListScoreCount = 0;
        long unregisteredScoreCount = 0;
        long teamCount = 0;

        for (GeyserSession connection : geyser.onlineConnections()) {
            Scoreboard scoreboard = connection.getWorldCache().getScoreboard();

            playerCount += 1;

            for (Objective objective : scoreboard.objectives.values()) {
                boolean registered = false;

                for (DisplaySlot activeSlot : new ArrayList<>(objective.activeSlots)) {
                    registered = true;
                    switch (DisplaySlot.slotCategory(activeSlot.position())) {
                        case SIDEBAR -> sidebarScoreCount += objective.getScores().size();
                        case BELOW_NAME -> belowNameScoreCount += objective.getScores().size();
                        case PLAYER_LIST ->  tabListScoreCount += objective.getScores().size();
                    }
                }

                if (!registered) {
                    unregisteredScoreCount += objective.getScores().size();
                }
            }
            teamCount += scoreboard.teams.size();
        }

        Files.writeString(path, String.format("%-5s %-7s %-7s %-7s %-7s %-7s%n", playerCount, belowNameScoreCount, sidebarScoreCount, tabListScoreCount, unregisteredScoreCount, teamCount), StandardOpenOption.APPEND);
    }

    private static void writePacketStats(Path path) throws IOException {
        Long[] stats = new Long[ScoreboardPacketType.length];
        for (int i = 0; i < ScoreboardPacketType.length; i++) {
            stats[i] = packetCounts.getAndSet(i, 0L);
        }
        Files.writeString(path, String.format("%-7s".repeat(stats.length) + "%n", (Object[]) stats), StandardOpenOption.APPEND);
    }
}
