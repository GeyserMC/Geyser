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

import org.geysermc.connector.network.session.GeyserSession;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.geysermc.connector.scoreboard.UpdateType.*;

@Getter
public class Scoreboard {
    private GeyserSession session;
    private AtomicLong nextId = new AtomicLong(0);

    private Map<String, Objective> objectives = new HashMap<>();
    private Map<String, Team> teams = new HashMap<>();

    public Scoreboard(GeyserSession session) {
        this.session = session;
    }

    public Objective registerNewObjective(String objectiveId, boolean temp) {
        if (!temp || objectives.containsKey(objectiveId)) return objectives.get(objectiveId);
        Objective objective = new Objective(this, objectiveId);
        objectives.put(objectiveId, objective);
        return objective;
    }

    public Objective registerNewObjective(String objectiveId, ScoreboardPosition displaySlot) {
        Objective objective = null;
        if (objectives.containsKey(objectiveId)) {
            objective = objectives.get(objectiveId);
            if (objective.isTemp()) objective.removeTemp(displaySlot);
            else {
                despawnObjective(objective);
                objective = null;
            }
        }
        if (objective == null) {
            objective = new Objective(this, objectiveId, displaySlot, "unknown", 0);
            objectives.put(objectiveId, objective);
        }
        return objective;
    }

    public Team registerNewTeam(String teamName, Set<String> players) {
        if (teams.containsKey(teamName)) {
            session.getConnector().getLogger().info("Ignoring team " + teamName + ". It overrides without removing old team.");
            return getTeam(teamName);
        }

        Team team = new Team(this, teamName).setEntities(players);
        teams.put(teamName, team);

        for (Objective objective : objectives.values()) {
            for (Score score : objective.getScores().values()) {
                if (players.contains(score.getName())) {
                    score.setTeam(team);
                }
            }
        }
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
        if (objective != null) objective.setUpdateType(REMOVE);
    }

    public void removeTeam(String teamName) {
        Team remove = teams.remove(teamName);
        if (remove != null) remove.setUpdateType(REMOVE);
    }

    public void onUpdate() {
        Set<Objective> changedObjectives = new HashSet<>();
        List<ScoreInfo> addScores = new ArrayList<>();
        List<ScoreInfo> removeScores = new ArrayList<>();

        for (String objectiveId : new ArrayList<>(objectives.keySet())) {
            Objective objective = objectives.get(objectiveId);
            if (objective.isTemp()) {
                session.getConnector().getLogger().debug("Ignoring temp Scoreboard Objective '"+ objectiveId +'\'');
                continue;
            }

            if (objective.getUpdateType() != NOTHING) changedObjectives.add(objective);

            boolean globalUpdate = objective.getUpdateType() == UPDATE;
            boolean globalAdd = objective.getUpdateType() == ADD || globalUpdate;
            boolean globalRemove = objective.getUpdateType() == REMOVE || globalUpdate;

            boolean hasUpdate = globalUpdate;

            List<Score> handledScores = new ArrayList<>();
            for (String identifier : new HashSet<>(objective.getScores().keySet())) {
                Score score = objective.getScores().get(identifier);
                Team team = score.getTeam();

                boolean inTeam = team != null && team.getEntities().contains(score.getName());

                boolean teamAdd = team != null && (team.getUpdateType() == ADD || team.getUpdateType() == UPDATE);
                boolean teamRemove = team != null && (team.getUpdateType() == REMOVE || team.getUpdateType() == UPDATE);

                if (team != null && (team.getUpdateType() == REMOVE || !inTeam)) score.setTeam(null);

                boolean add = (hasUpdate || globalAdd || teamAdd || teamRemove || score.getUpdateType() == ADD || score.getUpdateType() == UPDATE) && (score.getUpdateType() != REMOVE);
                boolean remove = hasUpdate || globalRemove || teamAdd || teamRemove || score.getUpdateType() == REMOVE || score.getUpdateType() == UPDATE;

                boolean updated = false;
                if (!hasUpdate) {
                    updated = hasUpdate = add;
                }

                if (updated) {
                    for (Score score1 : handledScores) {
                        ScoreInfo scoreInfo = new ScoreInfo(score1.getId(), score1.getObjective().getObjectiveName(), score1.getScore(), score1.getDisplayName());
                        addScores.add(scoreInfo);
                        removeScores.add(scoreInfo);
                    }
                }

                if (add) {
                    addScores.add(new ScoreInfo(score.getId(), score.getObjective().getObjectiveName(), score.getScore(), score.getDisplayName()));
                }
                if (remove) {
                    removeScores.add(new ScoreInfo(score.getId(), score.getObjective().getObjectiveName(), score.getOldScore(), score.getDisplayName()));
                }
                score.setOldScore(score.getScore());

                if (score.getUpdateType() == REMOVE) {
                    objective.removeScore(score.getName());
                }

                if (add || remove) {
                    changedObjectives.add(objective);
                } else { // stays the same like before
                    handledScores.add(score);
                }
                score.setUpdateType(NOTHING);
            }
        }

        for (Objective objective : changedObjectives) {
            boolean update = objective.getUpdateType() == NOTHING || objective.getUpdateType() == UPDATE;
            if (objective.getUpdateType() == REMOVE || update) {
                RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
                removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
                session.getUpstream().sendPacket(removeObjectivePacket);
                if (objective.getUpdateType() == REMOVE) {
                    objectives.remove(objective.getObjectiveName()); // now we can deregister
                }
            }
            if (objective.getUpdateType() == ADD || update) {
                SetDisplayObjectivePacket displayObjectivePacket = new SetDisplayObjectivePacket();
                displayObjectivePacket.setObjectiveId(objective.getObjectiveName());
                displayObjectivePacket.setDisplayName(objective.getDisplayName());
                displayObjectivePacket.setCriteria("dummy");
                displayObjectivePacket.setDisplaySlot(objective.getDisplaySlot());
                displayObjectivePacket.setSortOrder(1); // ??
                session.getUpstream().sendPacket(displayObjectivePacket);
            }
            objective.setUpdateType(NOTHING);
        }

        if (!removeScores.isEmpty()) {
            SetScorePacket setScorePacket = new SetScorePacket();
            setScorePacket.setAction(SetScorePacket.Action.REMOVE);
            setScorePacket.setInfos(removeScores);
            session.getUpstream().sendPacket(setScorePacket);
        }

        if (!addScores.isEmpty()) {
            SetScorePacket setScorePacket = new SetScorePacket();
            setScorePacket.setAction(SetScorePacket.Action.SET);
            setScorePacket.setInfos(addScores);
            session.getUpstream().sendPacket(setScorePacket);
        }
    }

    public void despawnObjective(Objective objective) {
        RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
        removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
        session.getUpstream().sendPacket(removeObjectivePacket);
        objectives.remove(objective.getDisplayName());

        List<ScoreInfo> toRemove = new ArrayList<>();
        for (String identifier : objective.getScores().keySet()) {
            Score score = objective.getScores().get(identifier);
            toRemove.add(new ScoreInfo(
                    score.getId(), score.getObjective().getObjectiveName(),
                    0, ""
            ));
        }

        if (!toRemove.isEmpty()) {
            SetScorePacket setScorePacket = new SetScorePacket();
            setScorePacket.setAction(SetScorePacket.Action.REMOVE);
            setScorePacket.setInfos(toRemove);
            session.getUpstream().sendPacket(setScorePacket);
        }
    }

    public Team getTeamFor(String entity) {
        for (Team team : teams.values()) {
            if (team.getEntities().contains(entity)) {
                return team;
            }
        }
        return null;
    }
}
