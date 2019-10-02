/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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
import org.geysermc.api.Geyser;
import org.geysermc.connector.console.GeyserLogger;
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

    public Objective registerNewObjective(String objectiveId, ScoreboardPosition displaySlot) {
        Objective objective = new Objective(this, objectiveId, displaySlot, "unknown", 0);
        if (objectives.containsKey(objectiveId)) despawnObjective(objectives.get(objectiveId));
        objectives.put(objectiveId, objective);
        return objective;
    }

    public Team registerNewTeam(String teamName, Set<String> players) {
        if (teams.containsKey(teamName)) {
            Geyser.getLogger().info("Ignoring team " + teamName + ". It overrides without removing old team.");
            return getTeam(teamName);
        }

        Team team = new Team(this, teamName).setEntities(players);
        teams.put(teamName, team);

        for (Objective objective : objectives.values()) {
            for (Score score : objective.getScores().values()) {
                if (players.contains(score.getName())) {
                    score.setTeam(team).setUpdateType(ADD);
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
        if (teams.remove(teamName) != null) {
            for (Objective objective : objectives.values()) {
                for (Score score : objective.getScores().values()) {
                    if (score.getName().equals(teamName)) {
                        score.setTeam(null).setUpdateType(ADD);
                    }
                }
            }
        }
    }

    public void onUpdate() {
        Set<Objective> changedObjectives = new HashSet<>();
        List<ScoreInfo> addScores = new ArrayList<>();
        List<ScoreInfo> removeScores = new ArrayList<>();

        for (String objectiveId : new ArrayList<>(objectives.keySet())) {
            Objective objective = objectives.get(objectiveId);
            if (objective.getUpdateType() != NOTHING) changedObjectives.add(objective);

            for (String identifier : new HashSet<>(objective.getScores().keySet())) {
                Score score = objective.getScores().get(identifier);

                boolean add = (objective.getUpdateType() != NOTHING && objective.getUpdateType() != REMOVE) && score.getUpdateType() != REMOVE || score.getUpdateType() == ADD;
                boolean remove = (add && score.getUpdateType() != ADD && objective.getUpdateType() != ADD) || objective.getUpdateType() == REMOVE || score.getUpdateType() == REMOVE;

                ScoreInfo info = new ScoreInfo(score.getId(), score.getObjective().getObjectiveName(), score.getScore(), score.getDisplayName());
                if (add || (score.getTeam() != null && (score.getTeam().getUpdateType() == ADD || score.getTeam().getUpdateType() == UPDATE))) addScores.add(info);
                if (remove || (score.getTeam() != null && score.getTeam().getUpdateType() != NOTHING)) removeScores.add(info);

                if (score.getUpdateType() == REMOVE) {
                    objective.removeScore(score.getName());
                }

                if (addScores.contains(info) || removeScores.contains(info)) changedObjectives.add(objective);
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
