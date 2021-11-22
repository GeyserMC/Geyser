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
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.packet.RemoveObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetDisplayObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import lombok.Getter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.geysermc.geyser.scoreboard.UpdateType.*;

public final class Scoreboard {
    private final GeyserSession session;
    private final GeyserLogger logger;
    @Getter
    private final AtomicLong nextId = new AtomicLong(0);

    private final Map<String, Objective> objectives = new ConcurrentHashMap<>();
    @Getter
    private final Map<ScoreboardPosition, Objective> objectiveSlots = new EnumMap<>(ScoreboardPosition.class);
    private final Map<String, Team> teams = new ConcurrentHashMap<>(); // updated on multiple threads

    private int lastAddScoreCount = 0;
    private int lastRemoveScoreCount = 0;

    public Scoreboard(GeyserSession session) {
        this.session = session;
        this.logger = GeyserImpl.getInstance().getLogger();
    }

    public void removeScoreboard() {
        Iterator<Objective> iterator = objectives.values().iterator();
        while (iterator.hasNext()) {
            Objective objective = iterator.next();
            iterator.remove();

            deleteObjective(objective, false);
        }
    }

    public Objective registerNewObjective(String objectiveId) {
        Objective objective = objectives.get(objectiveId);
        if (objective != null) {
            // we have no other choice, or we have to make a new map?
            // if the objective hasn't been deleted, we have to force it
            if (objective.getUpdateType() != REMOVE) {
                return null;
            }
            deleteObjective(objective, true);
        }

        objective = new Objective(this, objectiveId);
        objectives.put(objectiveId, objective);
        return objective;
    }

    public void displayObjective(String objectiveId, ScoreboardPosition displaySlot) {
        Objective objective = objectives.get(objectiveId);
        if (objective == null) {
            return;
        }

        if (!objective.isActive()) {
            objective.setActive(displaySlot);
            // for reactivated objectives
            objective.setUpdateType(ADD);
        }

        Objective storedObjective = objectiveSlots.get(displaySlot);
        if (storedObjective != null && storedObjective != objective) {
            storedObjective.pendingRemove();
        }
        objectiveSlots.put(displaySlot, objective);

        if (displaySlot == ScoreboardPosition.BELOW_NAME) {
            // Display the below name score option to all players
            // Of note: unlike Bedrock, if there is an objective in the below name slot, everyone has a display
            for (PlayerEntity entity : session.getEntityCache().getAllPlayerEntities()) {
                if (!entity.isValid()) {
                    // Player hasn't spawned yet - don't bother, it'll be done then
                    continue;
                }

                entity.setBelowNameText(objective);
            }
        }
    }

    public Team registerNewTeam(String teamName, String[] players) {
        Team team = teams.get(teamName);
        if (team != null) {
            logger.info(GeyserLocale.getLocaleStringLog("geyser.network.translator.team.failed_overrides", teamName));
            return team;
        }

        team = new Team(this, teamName);
        team.addEntities(players);
        teams.put(teamName, team);
        return team;
    }

    public void onUpdate() {
        List<ScoreInfo> addScores = new ArrayList<>(lastAddScoreCount);
        List<ScoreInfo> removeScores = new ArrayList<>(lastRemoveScoreCount);
        List<Objective> removedObjectives = new ArrayList<>();

        Team playerTeam = getTeamFor(session.getPlayerEntity().getUsername());
        Objective correctSidebar = null;

        for (Objective objective : objectives.values()) {
            // objective has been deleted
            if (objective.getUpdateType() == REMOVE) {
                removedObjectives.add(objective);
                continue;
            }

            // there's nothing we can do with inactive objectives
            // after checking if the objective has been deleted,
            // except waiting for the objective to become activated (:
            if (!objective.isActive()) {
                continue;
            }

            if (playerTeam != null && playerTeam.getColor() == objective.getTeamColor()) {
                correctSidebar = objective;
            }
        }

        if (correctSidebar == null) {
            correctSidebar = objectiveSlots.get(ScoreboardPosition.SIDEBAR);
        }

        for (Objective objective : removedObjectives) {
            // Deletion must be handled before the active objectives are handled - otherwise if a scoreboard display is changed before the current
            // scoreboard is removed, the client can crash
            deleteObjective(objective, true);
        }

        handleObjective(objectiveSlots.get(ScoreboardPosition.PLAYER_LIST), addScores, removeScores);
        handleObjective(correctSidebar, addScores, removeScores);
        handleObjective(objectiveSlots.get(ScoreboardPosition.BELOW_NAME), addScores, removeScores);

        Iterator<Team> teamIterator = teams.values().iterator();
        while (teamIterator.hasNext()) {
            Team current = teamIterator.next();

            switch (current.getCachedUpdateType()) {
                case ADD, UPDATE -> current.markUpdated();
                case REMOVE -> teamIterator.remove();
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

        lastAddScoreCount = addScores.size();
        lastRemoveScoreCount = removeScores.size();
    }

    private void handleObjective(Objective objective, List<ScoreInfo> addScores, List<ScoreInfo> removeScores) {
        if (objective == null || objective.getUpdateType() == REMOVE) {
            return;
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
            return;
        }

        boolean objectiveAdd = objective.getUpdateType() == ADD;
        boolean objectiveUpdate = objective.getUpdateType() == UPDATE;

        for (Score score : objective.getScores().values()) {
            if (score.getUpdateType() == REMOVE) {
                removeScores.add(score.getCachedInfo());
                // score is pending to be removed, so we can remove it from the objective
                objective.removeScore0(score.getName());
                break;
            }

            Team team = score.getTeam();

            boolean add = objectiveAdd || objectiveUpdate;

            if (team != null) {
                if (team.getUpdateType() == REMOVE || !team.hasEntity(score.getName())) {
                    score.setTeam(null);
                    add = true;
                }
            }

            if (score.shouldUpdate()) {
                score.update(objective.getObjectiveName());
                add = true;
            }

            if (add) {
                addScores.add(score.getCachedInfo());
            }

            // we need this as long as MCPE-143063 hasn't been fixed.
            // the checks after 'add' are there to prevent removing scores that
            // are going to be removed anyway / don't need to be removed
            if (add && score.getUpdateType() != ADD && !(objectiveUpdate || objectiveAdd)) {
                removeScores.add(score.getCachedInfo());
            }

            score.setUpdateType(NOTHING);
        }

        if (objectiveUpdate) {
            RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
            removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
            session.sendUpstreamPacket(removeObjectivePacket);
        }

        if (objectiveAdd || objectiveUpdate) {
            SetDisplayObjectivePacket displayObjectivePacket = new SetDisplayObjectivePacket();
            displayObjectivePacket.setObjectiveId(objective.getObjectiveName());
            displayObjectivePacket.setDisplayName(objective.getDisplayName());
            displayObjectivePacket.setCriteria("dummy");
            displayObjectivePacket.setDisplaySlot(objective.getDisplaySlotName());
            displayObjectivePacket.setSortOrder(1); // 0 = ascending, 1 = descending
            session.sendUpstreamPacket(displayObjectivePacket);
        }

        objective.setUpdateType(NOTHING);
    }

    /**
     * @param remove if we should remove the objective from the objectives map.
     */
    public void deleteObjective(Objective objective, boolean remove) {
        if (remove) {
            objectives.remove(objective.getObjectiveName());
        }
        objectiveSlots.remove(objective.getDisplaySlot(), objective);

        objective.removed();

        RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
        removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
        session.sendUpstreamPacket(removeObjectivePacket);
    }

    public Objective getObjective(String objectiveName) {
        return objectives.get(objectiveName);
    }

    public Collection<Objective> getObjectives() {
        return objectives.values();
    }

    public void unregisterObjective(String objectiveName) {
        Objective objective = getObjective(objectiveName);
        if (objective != null) {
            objective.pendingRemove();
        }
    }

    public Objective getSlot(ScoreboardPosition slot) {
        return objectiveSlots.get(slot);
    }

    public Team getTeam(String teamName) {
        return teams.get(teamName);
    }

    public Team getTeamFor(String entity) {
        for (Team team : teams.values()) {
            if (team.hasEntity(entity)) {
                return team;
            }
        }
        return null;
    }

    public void removeTeam(String teamName) {
        Team remove = teams.remove(teamName);
        if (remove != null) {
            remove.setUpdateType(REMOVE);
            // We need to use the direct entities list here, so #refreshSessionPlayerDisplays also updates accordingly
            // With the player's lack of a team in visibility checks
            updateEntityNames(remove, remove.getEntities(), true);
        }
    }

    /**
     * Updates the display names of all entities in a given team.
     * @param teamChange the players have either joined or left the team. Used for optimizations when just the display name updated.
     */
    public void updateEntityNames(Team team, boolean teamChange) {
        Set<String> names = new HashSet<>(team.getEntities());
        updateEntityNames(team, names, teamChange);
    }

    /**
     * Updates the display name of a set of entities within a given team. The team may also be null if the set is being removed
     * from a team.
     */
    public void updateEntityNames(@Nullable Team team, Set<String> names, boolean teamChange) {
        if (names.remove(session.getPlayerEntity().getUsername()) && teamChange) {
            // If the player's team changed, then other entities' teams may modify their visibility based on team status
            refreshSessionPlayerDisplays();
        }
        if (!names.isEmpty()) {
            for (Entity entity : session.getEntityCache().getEntities().values()) {
                // This more complex logic is for the future to iterate over all entities, not just players
                if (entity instanceof PlayerEntity player && names.remove(player.getUsername())) {
                    player.updateDisplayName(team, true);
                    if (names.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * If the team's player was refreshed, then we need to go through every entity and check...
     */
    private void refreshSessionPlayerDisplays() {
        for (Entity entity : session.getEntityCache().getEntities().values()) {
            if (entity instanceof PlayerEntity player) {
                Team playerTeam = session.getWorldCache().getScoreboard().getTeamFor(player.getUsername());
                player.updateDisplayName(playerTeam, true);
            }
        }
    }
}
