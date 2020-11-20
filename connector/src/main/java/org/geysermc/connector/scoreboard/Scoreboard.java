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
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.packet.RemoveObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetDisplayObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.geysermc.connector.scoreboard.UpdateType.*;

@Getter
public final class Scoreboard {
    private final GeyserSession session;
    private final GeyserLogger logger;
    private final AtomicLong nextId = new AtomicLong(0);

    private final Map<String, Objective> objectives = new ConcurrentHashMap<>();
    private final Map<String, Team> teams = new HashMap<>();

    private int lastAddScoreCount = 0;
    private int lastRemoveScoreCount = 0;

    public Scoreboard(GeyserSession session) {
        this.session = session;
        this.logger = GeyserConnector.getInstance().getLogger();
    }

    public Objective registerNewObjective(String objectiveId, boolean active) {
        Objective objective = objectives.get(objectiveId);
        if (active || objective != null) {
            return objective;
        }
        objective = new Objective(this, objectiveId);
        objectives.put(objectiveId, objective);
        return objective;
    }

    public Objective displayObjective(String objectiveId, ScoreboardPosition displaySlot) {
        Objective objective = objectives.get(objectiveId);
        if (objective != null) {
            if (!objective.isActive()) {
                objective.setActive(displaySlot);
                removeOldObjectives(objective);
                return objective;
            }
            despawnObjective(objective);
        }

        objective = new Objective(this, objectiveId, displaySlot, "unknown", 0);
        objectives.put(objectiveId, objective);
        removeOldObjectives(objective);
        return objective;
    }

    private void removeOldObjectives(Objective newObjective) {
        for (Objective next : objectives.values()) {
            if (next.getId() == newObjective.getId()) {
                continue;
            }
            if (next.getDisplaySlot() == newObjective.getDisplaySlot()) {
                next.setUpdateType(REMOVE);
            }
        }
    }

    public Team registerNewTeam(String teamName, Set<String> players) {
        Team team = teams.get(teamName);
        if (team != null) {
            logger.info(LanguageUtils.getLocaleStringLog("geyser.network.translator.team.failed_overrides", teamName));
            return team;
        }

        team = new Team(this, teamName).addEntities(players);
        teams.put(teamName, team);
        return team;
    }

    public Objective getObjective(String objectiveName) {
        return objectives.get(objectiveName);
    }

    public Team getTeam(String teamName) {
        return teams.get(teamName);
    }

    public void unregisterObjective(String objectiveName) {
        Objective objective = getObjective(objectiveName);
        if (objective != null) {
            objective.setUpdateType(REMOVE);
        }
    }

    public void removeTeam(String teamName) {
        Team remove = teams.remove(teamName);
        if (remove != null) {
            remove.setUpdateType(REMOVE);
        }
    }

    public void onUpdate() {
        List<ScoreInfo> addScores = new ArrayList<>(getLastAddScoreCount());
        List<ScoreInfo> removeScores = new ArrayList<>(getLastRemoveScoreCount());
        List<Objective> removedObjectives = new ArrayList<>();

        for (Objective objective : objectives.values()) {
            if (!objective.isActive()) {
                logger.debug("Ignoring non-active Scoreboard Objective '" + objective.getObjectiveName() + '\'');
                continue;
            }

            // hearts can't hold teams, so we treat them differently
            if (objective.getType() == 1) {
                for (Score score : objective.getScores().values()) {
                    boolean update = score.shouldUpdate();

                    if (update) {
                        score.update(objective.getObjectiveName());
                    }

                    if (score.getUpdateType() != REMOVE && update) {
                        addScores.add(score.getCachedInfo());
                    }
                    if (score.getUpdateType() != ADD && update) {
                        removeScores.add(score.getCachedInfo());
                    }
                }
                continue;
            }

            boolean objectiveUpdate = objective.getUpdateType() == UPDATE;
            boolean objectiveAdd = objective.getUpdateType() == ADD;
            boolean objectiveRemove = objective.getUpdateType() == REMOVE;

            for (Score score : objective.getScores().values()) {
                Team team = score.getTeam();

                boolean add = objectiveAdd || objectiveUpdate;
                boolean remove = false;
                if (team != null) {
                    if (team.getUpdateType() == REMOVE || !team.hasEntity(score.getName())) {
                        score.setTeam(null);
                        add = true;
                        remove = true;
                    }
                }

                add |= score.shouldUpdate();
                remove |= score.shouldUpdate();

                if (score.getUpdateType() == REMOVE || objectiveRemove) {
                    add = false;
                }

                if (score.getUpdateType() == ADD || objectiveRemove) {
                    remove = false;
                }

                if (score.shouldUpdate()) {
                    score.update(objective.getObjectiveName());
                }

                if (add) {
                    addScores.add(score.getCachedInfo());
                }

                if (remove) {
                    removeScores.add(score.getCachedInfo());
                }

                // score is pending to be removed, so we can remove it from the objective
                if (score.getUpdateType() == REMOVE) {
                    objective.removeScore0(score.getName());
                }

                score.setUpdateType(NOTHING);
            }

            if (objectiveRemove) {
                removedObjectives.add(objective);
            }

            if (objectiveUpdate) {
                RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
                removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
                session.sendUpstreamPacket(removeObjectivePacket);
            }

            if ((objectiveAdd || objectiveUpdate) && !objectiveRemove) {
                SetDisplayObjectivePacket displayObjectivePacket = new SetDisplayObjectivePacket();
                displayObjectivePacket.setObjectiveId(objective.getObjectiveName());
                displayObjectivePacket.setDisplayName(objective.getDisplayName());
                displayObjectivePacket.setCriteria("dummy");
                displayObjectivePacket.setDisplaySlot(objective.getDisplaySlotName());
                displayObjectivePacket.setSortOrder(1); // ??
                session.sendUpstreamPacket(displayObjectivePacket);
            }

            objective.setUpdateType(NOTHING);
        }

        Iterator<Team> teamIterator = teams.values().iterator();
        while (teamIterator.hasNext()) {
            Team current = teamIterator.next();

            switch (current.getUpdateType()) {
                case ADD:
                case UPDATE:
                    current.markUpdated();
                    break;
                case REMOVE:
                    teamIterator.remove();
            }
        }

        if (!removeScores.isEmpty()) {
            SetScorePacket setScorePacket = new SetScorePacket();
            setScorePacket.setAction(SetScorePacket.Action.REMOVE);
            setScorePacket.setInfos(removeScores);
            session.sendUpstreamPacket(setScorePacket);
        }

        if (!addScores.isEmpty()) {
            SetScorePacket setScorePacket = new SetScorePacket();
            setScorePacket.setAction(SetScorePacket.Action.SET);
            setScorePacket.setInfos(addScores);
            session.sendUpstreamPacket(setScorePacket);
        }

        // prevents crashes in some cases
        for (Objective objective : removedObjectives) {
            despawnObjective(objective);
        }

        lastAddScoreCount = addScores.size();
        lastRemoveScoreCount = removeScores.size();
    }

    public void despawnObjective(Objective objective) {
        objectives.remove(objective.getObjectiveName());
        objective.removed();

        RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
        removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
        session.sendUpstreamPacket(removeObjectivePacket);
    }

    public Team getTeamFor(String entity) {
        for (Team team : teams.values()) {
            if (team.hasEntity(entity)) {
                return team;
            }
        }
        return null;
    }
}
