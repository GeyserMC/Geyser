/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.scoreboard;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.WorldCache;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class ScoreboardUpdater extends Thread {
    public static final int FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD = 10;
    public static final int SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD = 250;

    private static final int FIRST_MILLIS_BETWEEN_UPDATES = 1000; // 1 update per second
    private static final int SECOND_MILLIS_BETWEEN_UPDATES = 1000 * 3; // 1 update per 3 seconds

    private final WorldCache worldCache;
    private final GeyserSession session;

    private int millisBetweenUpdates = FIRST_MILLIS_BETWEEN_UPDATES;
    private long lastUpdate = System.currentTimeMillis();

    // If the Score Packets Per Second dropped below the lowest threshold once since the lastLog
    private boolean failedBetweenLastLog = true;
    private long lastLog = -1;

    private long lastPacketsPerSecondUpdate = System.currentTimeMillis();
    private final AtomicInteger packetsPerSecond = new AtomicInteger(0);
    private final AtomicInteger pendingPacketsPerSecond = new AtomicInteger(0);

    public ScoreboardUpdater(WorldCache worldCache) {
        super("Scoreboard Updater");
        this.worldCache = worldCache;
        session = worldCache.getSession();
    }

    @Override
    public void run() {
        while (!session.isClosed()) {
            long currentTime = System.currentTimeMillis();

            // reset score-packets per second every second
            if (currentTime - lastPacketsPerSecondUpdate > 1000) {
                lastPacketsPerSecondUpdate = currentTime;
                packetsPerSecond.set(pendingPacketsPerSecond.get());
                pendingPacketsPerSecond.set(0);
            }

            if (currentTime - lastUpdate > millisBetweenUpdates) {
                lastUpdate = currentTime;

                int pps = packetsPerSecond.get();
                if (pps >= FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
                    boolean reachedSecondThreshold = pps >= SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD;
                    if (reachedSecondThreshold) {
                        millisBetweenUpdates = SECOND_MILLIS_BETWEEN_UPDATES;
                    } else {
                        millisBetweenUpdates = FIRST_MILLIS_BETWEEN_UPDATES;
                    }

                    worldCache.getScoreboard().onUpdate(true);

                    if (failedBetweenLastLog && currentTime - lastLog > 60000) { // one minute
                        int threshold = reachedSecondThreshold ?
                                SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD :
                                FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD;

                        GeyserConnector.getInstance().getLogger().info(
                                LanguageUtils.getLocaleStringLog("geyser.scoreboard.updater.threshold_reached.log", session.getName(), threshold, pps) +
                                LanguageUtils.getLocaleStringLog("geyser.scoreboard.updater.threshold_reached", (millisBetweenUpdates / 1000.0))
                        );

                        String languageCode = session.getClientData().getLanguageCode();
                        session.sendMessage(
                                LanguageUtils.getPlayerLocaleString("geyser.scoreboard.updater.threshold_reached.player", languageCode, threshold, pps) + ' ' +
                                LanguageUtils.getPlayerLocaleString("geyser.scoreboard.updater.threshold_reached", languageCode, (millisBetweenUpdates / 1000.0))
                        );

                        lastLog = currentTime;
                    }
                    continue;
                }
                failedBetweenLastLog = true;
            }
        }
    }

    public int getPacketsPerSecond() {
        return packetsPerSecond.get();
    }

    /**
     * Increase the Scoreboard Packets Per Second and return the updated value
     */
    public int incrementAndGetPacketsPerSecond() {
        return pendingPacketsPerSecond.incrementAndGet();
    }
}
