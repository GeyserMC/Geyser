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

package org.geysermc.geyser.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardPosition;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class Objective {
    private final Scoreboard scoreboard;
    private final long id;
    private boolean active = true;

    @Setter
    private UpdateType updateType = UpdateType.ADD;

    private String objectiveName;
    private ScoreboardPosition displaySlot;
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
        this.displaySlot = displaySlot;
        this.displaySlotName = translateDisplaySlot(displaySlot);
        this.displayName = displayName;
        this.type = type;
    }

    private static String translateDisplaySlot(ScoreboardPosition displaySlot) {
        return switch (displaySlot) {
            case BELOW_NAME -> "belowname";
            case PLAYER_LIST -> "list";
            default -> "sidebar";
        };
    }

    public void registerScore(String id, int score) {
        if (!scores.containsKey(id)) {
            long scoreId = scoreboard.getNextId().getAndIncrement();
            Score scoreObject = new Score(scoreId, id)
                    .setScore(score)
                    .setTeam(scoreboard.getTeamFor(id))
                    .setUpdateType(UpdateType.ADD);
            scores.put(id, scoreObject);
        }
    }

    public void setScore(String id, int score) {
        Score stored = scores.get(id);
        if (stored != null) {
            stored.setScore(score)
                    .setUpdateType(UpdateType.UPDATE);
            return;
        }
        registerScore(id, score);
    }

    public void removeScore(String id) {
        Score stored = scores.get(id);
        if (stored != null) {
            stored.setUpdateType(UpdateType.REMOVE);
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
            this.displaySlot = displaySlot;
            displaySlotName = translateDisplaySlot(displaySlot);
        }
    }

    /**
     * The objective will be removed on the next update
     */
    public void pendingRemove() {
        updateType = UpdateType.REMOVE;
    }

    public TeamColor getTeamColor() {
        return switch (displaySlot) {
            case SIDEBAR_TEAM_RED -> TeamColor.RED;
            case SIDEBAR_TEAM_AQUA -> TeamColor.AQUA;
            case SIDEBAR_TEAM_BLUE -> TeamColor.BLUE;
            case SIDEBAR_TEAM_GOLD -> TeamColor.GOLD;
            case SIDEBAR_TEAM_GRAY -> TeamColor.GRAY;
            case SIDEBAR_TEAM_BLACK -> TeamColor.BLACK;
            case SIDEBAR_TEAM_GREEN -> TeamColor.GREEN;
            case SIDEBAR_TEAM_WHITE -> TeamColor.WHITE;
            case SIDEBAR_TEAM_YELLOW -> TeamColor.YELLOW;
            case SIDEBAR_TEAM_DARK_RED -> TeamColor.DARK_RED;
            case SIDEBAR_TEAM_DARK_AQUA -> TeamColor.DARK_AQUA;
            case SIDEBAR_TEAM_DARK_BLUE -> TeamColor.DARK_BLUE;
            case SIDEBAR_TEAM_DARK_GRAY -> TeamColor.DARK_GRAY;
            case SIDEBAR_TEAM_DARK_GREEN -> TeamColor.DARK_GREEN;
            case SIDEBAR_TEAM_DARK_PURPLE -> TeamColor.DARK_PURPLE;
            case SIDEBAR_TEAM_LIGHT_PURPLE -> TeamColor.LIGHT_PURPLE;
            default -> null;
        };
    }

    public void removed() {
        active = false;
        updateType = UpdateType.REMOVE;
        scores = null;
    }
}
