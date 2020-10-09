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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Objective {
    private final Scoreboard scoreboard;
    private final long id;
    private boolean active = true;

    @Setter
    private UpdateType updateType = UpdateType.ADD;
    private String objectiveName;
    private String displaySlotName;
    private String displayName = "unknown";
    private int type = 0; // 0 = integer, 1 = heart

    private Map<String, Score> scores = new ConcurrentHashMap<>();

    private Objective(Scoreboard scoreboard) {
        this.id = scoreboard.getNextId().getAndIncrement();
        this.scoreboard = scoreboard;
    }

    /**
     * /!\ This method is made for temporary objectives until the real objective is received
     *
     * @param scoreboard    the scoreboard
     * @param objectiveName the name of the objective
     */
    public Objective(Scoreboard scoreboard, String objectiveName) {
        this(scoreboard);
        this.objectiveName = objectiveName;
        this.active = false;
    }

    public Objective(Scoreboard scoreboard, String objectiveName, ScoreboardPosition displaySlot, String displayName, int type) {
        this(scoreboard);
        this.objectiveName = objectiveName;
        this.displaySlotName = translateDisplaySlot(displaySlot);
        this.displayName = displayName;
        this.type = type;
    }

    public void registerScore(String id, int score) {
        if (!scores.containsKey(id)) {
            Score score1 = new Score(this, id)
                    .setScore(score)
                    .setTeam(scoreboard.getTeamFor(id))
                    .setUpdateType(UpdateType.ADD);
            scores.put(id, score1);
        }
    }

    public void setScore(String id, int score) {
        if (scores.containsKey(id)) {
            scores.get(id).setScore(score);
            return;
        }
        registerScore(id, score);
    }

    public int getScore(String id) {
        if (scores.containsKey(id)) {
            return scores.get(id).getScore();
        }
        return 0;
    }

    public void removeScore(String id) {
        if (scores.containsKey(id)) {
            scores.get(id).setUpdateType(UpdateType.REMOVE);
        }
    }

    /**
     * Used internally to remove a score from the score map
     */
    public void removeScore0(String id) {
        scores.remove(id);
    }

    public Objective setDisplayName(String displayName) {
        this.displayName = displayName;
        if (updateType == UpdateType.NOTHING) {
            updateType = UpdateType.UPDATE;
        }
        return this;
    }

    public Objective setType(int type) {
        this.type = type;
        if (updateType == UpdateType.NOTHING) {
            updateType = UpdateType.UPDATE;
        }
        return this;
    }

    public void setActive(ScoreboardPosition displaySlot) {
        if (!active) {
            active = true;
            displaySlotName = translateDisplaySlot(displaySlot);
        }
    }

    public void removed() {
        scores = null;
    }

    private static String translateDisplaySlot(ScoreboardPosition displaySlot) {
        switch (displaySlot) {
            case BELOW_NAME:
                return "belowname";
            case PLAYER_LIST:
                return "list";
            default:
                return "sidebar";
        }
    }
}
