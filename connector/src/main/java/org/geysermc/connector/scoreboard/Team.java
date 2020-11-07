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

import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Accessors(chain = true)
public final class Team {
    private final Scoreboard scoreboard;
    private final String id;

    @Getter(AccessLevel.NONE)
    private final Set<String> entities;
    @Setter private NameTagVisibility nameTagVisibility;
    @Setter private TeamColor color;

    private TeamData currentData;
    private TeamData cachedData;

    private boolean updating;

    public Team(Scoreboard scoreboard, String id) {
        this.scoreboard = scoreboard;
        this.id = id;
        currentData = new TeamData();
        entities = new ObjectOpenHashSet<>();
    }

    private void checkAddedEntities(List<String> added) {
        if (added.size() == 0) {
            return;
        }
        // we don't have to change the updateType,
        // because the scores itself need updating, not the team
        for (Objective objective : scoreboard.getObjectives().values()) {
            for (String addedEntity : added) {
                Score score = objective.getScores().get(addedEntity);
                if (score != null) {
                    score.setTeam(this);
                }
            }
        }
    }

    public Team addEntities(String... names) {
        List<String> added = new ArrayList<>();
        for (String name : names) {
            if (entities.add(name)) {
                added.add(name);
            }
        }
        checkAddedEntities(added);
        return this;
    }

    public Team addEntities(Set<String> names) {
        List<String> added = new ArrayList<>();
        for (String name : names) {
            if (entities.add(name)) {
                added.add(name);
            }
        }
        checkAddedEntities(added);
        return this;
    }

    public void removeEntities(String... names) {
        for (String name : names) {
            entities.remove(name);
        }
    }

    public boolean hasEntity(String name) {
        return entities.contains(name);
    }

    public Team setName(String name) {
        currentData.name = name;
        return this;
    }

    public Team setPrefix(String prefix) {
        // replace "null" to an empty string,
        // we do this here to improve the performance of Score#getDisplayName
        if (prefix.length() == 4 && "null".equals(prefix)) {
            currentData.prefix = "";
            return this;
        }
        currentData.prefix = prefix;
        return this;
    }

    public Team setSuffix(String suffix) {
        // replace "null" to an empty string,
        // we do this here to improve the performance of Score#getDisplayName
        if (suffix.length() == 4 && "null".equals(suffix)) {
            currentData.suffix = "";
            return this;
        }
        currentData.suffix = suffix;
        return this;
    }

    public String getDisplayName(String score) {
        return cachedData != null ?
                cachedData.getDisplayName(score) :
                currentData.getDisplayName(score);
    }

    public void markUpdated() {
        updating = false;
    }

    public boolean shouldUpdate() {
        return updating || cachedData == null || currentData.updateTime > cachedData.updateTime;
    }

    public void prepareUpdate() {
        if (updating) {
            return;
        }
        updating = true;

        if (cachedData == null) {
            cachedData = new TeamData();
            cachedData.updateType = currentData.updateType != UpdateType.REMOVE ? UpdateType.ADD : UpdateType.REMOVE;
        } else {
            cachedData.updateType = currentData.updateType;
        }

        cachedData.updateTime = currentData.updateTime;
        cachedData.name = currentData.name;
        cachedData.prefix = currentData.prefix;
        cachedData.suffix = currentData.suffix;
    }

    public UpdateType getUpdateType() {
        return cachedData != null ? cachedData.updateType : currentData.updateType;
    }

    public Team setUpdateType(UpdateType updateType) {
        if (updateType != UpdateType.NOTHING) {
            currentData.updateTime = System.currentTimeMillis();
        }
        currentData.updateType = updateType;
        return this;
    }

    public boolean isVisibleFor(String entity) {
        switch (nameTagVisibility) {
            case HIDE_FOR_OTHER_TEAMS:
                return hasEntity(entity);
            case HIDE_FOR_OWN_TEAM:
                return !hasEntity(entity);
            case ALWAYS:
                return true;
            case NEVER:
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Getter
    public static final class TeamData {
        protected UpdateType updateType;
        protected long updateTime;

        protected String name;
        protected String prefix;
        protected String suffix;

        protected TeamData() {
            updateType = UpdateType.ADD;
        }

        public String getDisplayName(String score) {
            return prefix + score + suffix;
        }
    }
}
