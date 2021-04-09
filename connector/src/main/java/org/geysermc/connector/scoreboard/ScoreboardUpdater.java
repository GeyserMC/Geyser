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

package org.geysermc.connector.scoreboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.WorldCache;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScoreboardUpdater implements Runnable {
    public static final int FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD;
    public static final int SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD = 250;

    private static final int FIRST_MILLIS_BETWEEN_UPDATES = 250; // 4 updates per second
    private static final int SECOND_MILLIS_BETWEEN_UPDATES = 1000 * 3; // 1 update per second

    private static final boolean DEBUG_ENABLED;

    private static GeyserConnector connector;

    static {
        GeyserConfiguration config = GeyserConnector.getInstance().getConfig();
        FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD = Math.min(config.getScoreboardPacketThreshold(), SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD);
        DEBUG_ENABLED = config.isDebugMode();
    }

    private long lastUpdate = System.currentTimeMillis();
    private long lastPacketsPerSecondUpdate = System.currentTimeMillis();

    public static void init() {
        connector = GeyserConnector.getInstance();
        ScoreboardUpdater updater = new ScoreboardUpdater();
        updater.run();
    }

    public void run() {
        long timeTillAction = getTimeTillNextAction();
        if (timeTillAction > 0) {
            connector.getGeneralThreadPool().schedule(this, timeTillAction, TimeUnit.MILLISECONDS);
            return;
        }

        long currentTime = System.currentTimeMillis();

        // reset score-packets per second every second
        if (currentTime - lastPacketsPerSecondUpdate >= 1000) {
            lastPacketsPerSecondUpdate = currentTime;
            for (GeyserSession session : connector.getPlayers()) {
                ScoreboardSession scoreboardSession = session.getWorldCache().getScoreboardSession();

                int oldPps = scoreboardSession.getPacketsPerSecond();
                int newPps = scoreboardSession.getPendingPacketsPerSecond().get();

                scoreboardSession.packetsPerSecond = newPps;
                scoreboardSession.pendingPacketsPerSecond.set(0);

                // just making sure that all updates are pushed before giving up control
                if (oldPps >= FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD &&
                        newPps < FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
                    session.getWorldCache().getScoreboard().onUpdate();
                }
            }
        }

        if (currentTime - lastUpdate >= FIRST_MILLIS_BETWEEN_UPDATES) {
            lastUpdate = currentTime;

            for (GeyserSession session : connector.getPlayers()) {
                WorldCache worldCache = session.getWorldCache();
                ScoreboardSession scoreboardSession = worldCache.getScoreboardSession();

                int pps = scoreboardSession.getPacketsPerSecond();
                if (pps >= FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
                    boolean reachedSecondThreshold = pps >= SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD;

                    int millisBetweenUpdates = reachedSecondThreshold ?
                            SECOND_MILLIS_BETWEEN_UPDATES :
                            FIRST_MILLIS_BETWEEN_UPDATES;

                    if (currentTime - scoreboardSession.lastUpdate >= millisBetweenUpdates) {
                        worldCache.getScoreboard().onUpdate();
                        scoreboardSession.lastUpdate = currentTime;

                        if (DEBUG_ENABLED && (currentTime - scoreboardSession.lastLog >= 60000)) { // one minute
                            int threshold = reachedSecondThreshold ?
                                    SECOND_SCORE_PACKETS_PER_SECOND_THRESHOLD :
                                    FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD;

                            GeyserConnector.getInstance().getLogger().info(
                                    LanguageUtils.getLocaleStringLog("geyser.scoreboard.updater.threshold_reached.log", session.getName(), threshold, pps) +
                                            LanguageUtils.getLocaleStringLog("geyser.scoreboard.updater.threshold_reached", (millisBetweenUpdates / 1000.0))
                            );

                            scoreboardSession.lastLog = currentTime;
                        }
                    }
                }
            }
        }

        long timeTillNextAction = getTimeTillNextAction();
        connector.getGeneralThreadPool().schedule(this, timeTillNextAction, TimeUnit.MILLISECONDS);
    }

    private long getTimeTillNextAction() {
        long currentTime = System.currentTimeMillis();

        long timeUntilNextUpdate = FIRST_MILLIS_BETWEEN_UPDATES - (currentTime - lastUpdate);
        long timeUntilPacketReset = 1000 - (currentTime - lastPacketsPerSecondUpdate);

        return Math.min(timeUntilNextUpdate, timeUntilPacketReset);
    }

    @RequiredArgsConstructor
    @Getter
    public static final class ScoreboardSession {
        private final GeyserSession session;
        private final AtomicInteger pendingPacketsPerSecond = new AtomicInteger(0);
        private int packetsPerSecond;
        private long lastUpdate;
        private long lastLog;
    }
}
