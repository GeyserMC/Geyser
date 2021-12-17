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

import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public final class Score {
    private final long id;
    private final String name;
    private ScoreInfo cachedInfo;

    /**
     * Changes that have been made since the last cached data.
     */
    private final Score.ScoreData currentData;
    /**
     * The data that is currently displayed to the Bedrock client.
     */
    private Score.ScoreData cachedData;

    public Score(long id, String name) {
        this.id = id;
        this.name = name;
        this.currentData = new ScoreData();
    }

    public String getDisplayName() {
        Team team = cachedData.team;
        if (team != null) {
            return team.getDisplayName(name);
        }
        return name;
    }

    public int getScore() {
        return currentData.getScore();
    }

    public Score setScore(int score) {
        currentData.score = score;
        return this;
    }

    public Team getTeam() {
        return currentData.team;
    }

    public Score setTeam(Team team) {
        if (currentData.team != null && team != null) {
            if (!currentData.team.equals(team)) {
                currentData.team = team;
                setUpdateType(UpdateType.UPDATE);
            }
            return this;
        }
        // simplified from (this.team != null && team == null) || (this.team == null && team != null)
        if (currentData.team != null || team != null) {
            currentData.team = team;
            setUpdateType(UpdateType.UPDATE);
        }
        return this;
    }

    public UpdateType getUpdateType() {
        return currentData.updateType;
    }

    public Score setUpdateType(UpdateType updateType) {
        if (updateType != UpdateType.NOTHING) {
            currentData.changed = true;
        }
        currentData.updateType = updateType;
        return this;
    }

    public boolean shouldUpdate() {
        return cachedData == null || currentData.changed ||
                (currentData.team != null && currentData.team.shouldUpdate());
    }

    public void update(String objectiveName) {
        if (cachedData == null) {
            cachedData = new ScoreData();
            cachedData.updateType = UpdateType.ADD;
            if (currentData.updateType == UpdateType.REMOVE) {
                cachedData.updateType = UpdateType.REMOVE;
            }
        } else {
            cachedData.updateType = currentData.updateType;
        }

        currentData.changed = false;
        cachedData.team = currentData.team;
        cachedData.score = currentData.score;

        String name = this.name;
        if (cachedData.team != null) {
            cachedData.team.prepareUpdate();
            name = cachedData.team.getDisplayName(name);
        }
        cachedInfo = new ScoreInfo(id, objectiveName, cachedData.score, name);
    }

    @Getter
    public static final class ScoreData {
        private UpdateType updateType;
        private boolean changed;

        private Team team;
        private int score;

        private ScoreData() {
            updateType = UpdateType.ADD;
        }
    }
}
