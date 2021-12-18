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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater.ScoreboardSession;

@Getter
public class WorldCache {
    private final GeyserSession session;
    private final ScoreboardSession scoreboardSession;
    private Scoreboard scoreboard;
    @Setter
    private Difficulty difficulty = Difficulty.EASY;

    public WorldCache(GeyserSession session) {
        this.session = session;
        this.scoreboard = new Scoreboard(session);
        scoreboardSession = new ScoreboardSession(session);
    }

    public void removeScoreboard() {
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
            scoreboard = new Scoreboard(session);
        }
    }

    public int increaseAndGetScoreboardPacketsPerSecond() {
        int pendingPps = scoreboardSession.getPendingPacketsPerSecond().incrementAndGet();
        int pps = scoreboardSession.getPacketsPerSecond();
        return Math.max(pps, pendingPps);
    }
}