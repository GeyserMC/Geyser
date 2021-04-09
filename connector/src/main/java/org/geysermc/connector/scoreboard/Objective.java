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
        switch (displaySlot) {
            case BELOW_NAME:
                return "belowname";
            case PLAYER_LIST:
                return "list";
            default:
                return "sidebar";
        }
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

    public void deactivate() {
        active = false;
    }

    public ScoreboardPosition getPositionCategory() {
        switch (displaySlot) {
            case PLAYER_LIST:
                return ScoreboardPosition.PLAYER_LIST;
            case BELOW_NAME:
                return ScoreboardPosition.BELOW_NAME;
            default:
                return ScoreboardPosition.SIDEBAR;
        }
    }

    public boolean hasTeamColor() {
        return displaySlot != ScoreboardPosition.PLAYER_LIST &&
                displaySlot != ScoreboardPosition.BELOW_NAME &&
                displaySlot != ScoreboardPosition.SIDEBAR;
    }

    public TeamColor getTeamColor() {
        switch (displaySlot) {
            case SIDEBAR_TEAM_RED:
                return TeamColor.RED;
            case SIDEBAR_TEAM_AQUA:
                return TeamColor.AQUA;
            case SIDEBAR_TEAM_BLUE:
                return TeamColor.BLUE;
            case SIDEBAR_TEAM_GOLD:
                return TeamColor.GOLD;
            case SIDEBAR_TEAM_GRAY:
                return TeamColor.GRAY;
            case SIDEBAR_TEAM_BLACK:
                return TeamColor.BLACK;
            case SIDEBAR_TEAM_GREEN:
                return TeamColor.GREEN;
            case SIDEBAR_TEAM_WHITE:
                return TeamColor.WHITE;
            case SIDEBAR_TEAM_YELLOW:
                return TeamColor.YELLOW;
            case SIDEBAR_TEAM_DARK_RED:
                return TeamColor.DARK_RED;
            case SIDEBAR_TEAM_DARK_AQUA:
                return TeamColor.DARK_AQUA;
            case SIDEBAR_TEAM_DARK_BLUE:
                return TeamColor.DARK_BLUE;
            case SIDEBAR_TEAM_DARK_GRAY:
                return TeamColor.DARK_GRAY;
            case SIDEBAR_TEAM_DARK_GREEN:
                return TeamColor.DARK_GREEN;
            case SIDEBAR_TEAM_DARK_PURPLE:
                return TeamColor.DARK_PURPLE;
            case SIDEBAR_TEAM_LIGHT_PURPLE:
                return TeamColor.LIGHT_PURPLE;
            default:
                return null;
        }
    }

    public void removed() {
        active = false;
        updateType = UpdateType.REMOVE;
        scores = null;
    }
}
