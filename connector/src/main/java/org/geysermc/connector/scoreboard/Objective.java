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

import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardPosition;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Objective {
    private Scoreboard scoreboard;
    private long id;
    private boolean temp;

    @Setter
    private UpdateType updateType = UpdateType.ADD;
    private String objectiveName;
    private String displaySlot;
    private String displayName = "unknown";
    private int type = 0; // 0 = integer, 1 = heart

    private Map<String, Score> scores = new HashMap<>();

    private Objective(Scoreboard scoreboard) {
        this.id = scoreboard.getNextId().getAndIncrement();
        this.scoreboard = scoreboard;
    }

    /**
     * /!\ This method is made for temporary objectives until the real objective is received
     * @param scoreboard the scoreboard
     * @param objectiveName the name of the objective
     */
    public Objective(Scoreboard scoreboard, String objectiveName) {
        this(scoreboard);
        this.objectiveName = objectiveName;
        this.temp = true;
    }

    public Objective(Scoreboard scoreboard, String objectiveName, ScoreboardPosition displaySlot, String displayName, int type) {
        this(scoreboard, objectiveName, displaySlot.name().toLowerCase(), displayName, type);
    }

    public Objective(Scoreboard scoreboard, String objectiveName, String displaySlot, String displayName, int type) {
        this(scoreboard);
        this.objectiveName = objectiveName;
        this.displaySlot = displaySlot;
        this.displayName = displayName;
        this.type = type;
    }

    public void registerScore(String id, int score) {
        if (!scores.containsKey(id)) {
            Score score1 = new Score(this, id)
                    .setScore(score)
                    .setTeam(scoreboard.getTeamFor(id));
            scores.put(id, score1);
        }
    }

    public void setScore(String id, int score) {
        if (scores.containsKey(id)) {
            scores.get(id).setScore(score).setUpdateType(UpdateType.ADD);
        } else {
            registerScore(id, score);
        }
    }

    public void setScoreText(String oldText, String newText) {
        if (!scores.containsKey(oldText) || oldText.equals(newText)) return;
        Score oldScore = scores.get(oldText);

        Score newScore = new Score(this, newText)
                .setScore(oldScore.getScore())
                .setTeam(scoreboard.getTeamFor(newText));

        scores.put(newText, newScore);
        oldScore.setUpdateType(UpdateType.REMOVE);
    }

    public int getScore(String id) {
        if (scores.containsKey(id)) {
            return scores.get(id).getScore();
        }
        return 0;
    }

    public Score getScore(int line) {
        for (Score score : scores.values()) {
            if (score.getScore() == line) return score;
        }
        return null;
    }

    public void resetScore(String id) {
        if (scores.containsKey(id)) {
            scores.get(id).setUpdateType(UpdateType.REMOVE);
        }
    }

    public void removeScore(String id) {
        scores.remove(id);
    }

    public Objective setDisplayName(String displayName) {
        this.displayName = displayName;
        if (updateType == UpdateType.NOTHING) updateType = UpdateType.UPDATE;
        return this;
    }

    public Objective setType(int type) {
        this.type = type;
        if (updateType == UpdateType.NOTHING) updateType = UpdateType.UPDATE;
        return this;
    }

    public void removeTemp(ScoreboardPosition displaySlot) {
        if (temp) {
            temp = false;
            this.displaySlot = displaySlot.name().toLowerCase();
        }
    }
}
