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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
public class Team {
    private final Scoreboard scoreboard;
    private final String id;

    private UpdateType updateType = UpdateType.ADD;
    private String name;
    private String prefix;
    private String suffix;
    private Set<String> entities = new HashSet<>();


    public Team(Scoreboard scoreboard, String id) {
        this.scoreboard = scoreboard;
        this.id = id;
    }

    public void addEntities(String... names) {
        List<String> added = new ArrayList<>();
        for (String name : names) {
            if (!entities.contains(name)) {
                entities.add(name);
                added.add(name);
            }
        }
        setUpdateType(UpdateType.UPDATE);
        for (Objective objective : scoreboard.getObjectives().values()) {
            for (Score score : objective.getScores().values()) {
                if (added.contains(score.getName())) {
                    score.setTeam(this);
                }
            }
        }
    }

    public void removeEntities(String... names) {
        for (String name : names) {
            entities.remove(name);
        }
        setUpdateType(UpdateType.UPDATE);
    }
}
