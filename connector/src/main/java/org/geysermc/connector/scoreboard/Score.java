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

import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain = true)
public class Score {
    private final Objective objective;
    private ScoreInfo cachedInfo;
    private final long id;

    @Setter
    private UpdateType updateType = UpdateType.ADD;
    private final String name;
    private Team team;
    private int score;
    @Setter
    private int oldScore = Integer.MIN_VALUE;

    public Score(Objective objective, String name) {
        this.id = objective.getScoreboard().getNextId().getAndIncrement();
        this.objective = objective;
        this.name = name;
    }

    public String getDisplayName() {
        if (team != null) {
            return team.getPrefix() + name + team.getSuffix();
        }
        return name;
    }

    public Score setScore(int score) {
        this.score = score;
        updateType = UpdateType.UPDATE;
        return this;
    }

    public Score setTeam(Team team) {
        if (this.team != null && team != null) {
            if (!this.team.equals(team)) {
                this.team = team;
                updateType = UpdateType.UPDATE;
            }
            return this;
        }
        // simplified from (this.team != null && team == null) || (this.team == null && team != null)
        if (this.team != null || team != null) {
            this.team = team;
            updateType = UpdateType.UPDATE;
        }
        return this;
    }

    public void update() {
        cachedInfo = new ScoreInfo(id, objective.getObjectiveName(), score, getDisplayName());
    }
}
