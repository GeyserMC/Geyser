/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.Getter"
#include "net.kyori.adventure.text.Component"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.ScoreInfo"
#include "org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint"
#include "org.cloudburstmc.protocol.bedrock.packet.SetScorePacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.scoreboard.display.slot.BelownameDisplaySlot"
#include "org.geysermc.geyser.scoreboard.display.slot.DisplaySlot"
#include "org.geysermc.geyser.scoreboard.display.slot.PlayerlistDisplaySlot"
#include "org.geysermc.geyser.scoreboard.display.slot.SidebarDisplaySlot"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor"
#include "org.jetbrains.annotations.Contract"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.EnumMap"
#include "java.util.EnumSet"
#include "java.util.HashMap"
#include "java.util.LinkedHashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Set"
#include "java.util.concurrent.ConcurrentHashMap"
#include "java.util.concurrent.atomic.AtomicBoolean"
#include "java.util.concurrent.atomic.AtomicLong"
#include "java.util.function.Function"
#include "java.util.stream.Collectors"

#include "static org.geysermc.geyser.scoreboard.UpdateType.REMOVE"


public final class Scoreboard {
    private static final bool SHOW_SCOREBOARD_LOGS = Boolean.parseBoolean(System.getProperty("Geyser.ShowScoreboardLogs", "true"));
    private static final bool ADD_TEAM_SUGGESTIONS = Boolean.parseBoolean(
        System.getProperty("Geyser.AddTeamSuggestions", std::string.valueOf(GeyserImpl.getInstance().config().advanced().addTeamSuggestions()))
    );

    private final GeyserSession session;
    private final GeyserLogger logger;
    private final AtomicLong nextId = new AtomicLong(0);

    private final Map<std::string, Objective> objectives = new ConcurrentHashMap<>();
    @Getter
    private final Map<ScoreboardPosition, DisplaySlot> objectiveSlots = Collections.synchronizedMap(new EnumMap<>(ScoreboardPosition.class));
    private final List<DisplaySlot> removedSlots = Collections.synchronizedList(new ArrayList<>());

    private final Map<std::string, Team> teams = new ConcurrentHashMap<>();

    @Getter
    private final Map<std::string, Team> playerToTeam = new Object2ObjectOpenHashMap<>();

    private final AtomicBoolean updateLockActive = new AtomicBoolean(false);
    private int lastAddScoreCount = 0;
    private int lastRemoveScoreCount = 0;

    public Scoreboard(GeyserSession session) {
        this.session = session;
        this.logger = GeyserImpl.getInstance().getLogger();
    }

    public void removeScoreboard() {
        var copy = new HashMap<>(objectiveSlots);
        objectiveSlots.clear();

        for (DisplaySlot slot : copy.values()) {
            slot.remove();
        }
    }

    public Objective registerNewObjective(std::string objectiveId) {
        Objective objective = objectives.get(objectiveId);
        if (objective != null) {

            if (SHOW_SCOREBOARD_LOGS) {
                logger.warning("An objective with the same name '" + objectiveId + "' already exists! Ignoring new objective!");
            }
            return null;
        }

        objective = new Objective(this, objectiveId);
        objectives.put(objectiveId, objective);
        return objective;
    }

    public void displayObjective(std::string objectiveId, ScoreboardPosition slot) {
        if (objectiveId.isEmpty()) {

            var display = objectiveSlots.get(slot);
            if (display != null) {
                removedSlots.add(display);
                objectiveSlots.remove(slot, display);
                var objective = display.objective();
                objective.removeDisplaySlot(display);
            }
            return;
        }

        Objective objective = objectives.get(objectiveId);
        if (objective == null) {
            return;
        }

        var display = objectiveSlots.get(slot);
        if (display != null && display.objective() != objective) {
            removedSlots.add(display);
        }

        display = switch (DisplaySlot.slotCategory(slot)) {
            case SIDEBAR -> new SidebarDisplaySlot(session, objective, slot);
            case BELOW_NAME -> new BelownameDisplaySlot(session, objective);
            case PLAYER_LIST -> new PlayerlistDisplaySlot(session, objective);
            default -> throw new IllegalStateException("Unexpected value: " + slot);
        };
        objectiveSlots.put(slot, display);
        objective.addDisplaySlot(display);
    }

    public void registerNewTeam(
        std::string teamName,
        String[] players,
        Component name,
        Component prefix,
        Component suffix,
        NameTagVisibility visibility,
        TeamColor color
    ) {
        Team team = teams.get(teamName);
        if (team != null) {
            if (SHOW_SCOREBOARD_LOGS) {
                logger.info("Ignoring team %s for %s. It overrides without removing old team.".formatted(teamName, session.javaUsername()));
            }
            return;
        }

        team = new Team(this, teamName, players, name, prefix, suffix, visibility, color);
        teams.put(teamName, team);


        if (ADD_TEAM_SUGGESTIONS) {
            session.addCommandEnum("Geyser_Teams", team.id());
        }
    }

    public void onUpdate() {

        if (updateLockActive.getAndSet(true)) {
            return;
        }

        List<ScoreInfo> addScores = new ArrayList<>(lastAddScoreCount);
        List<ScoreInfo> removeScores = new ArrayList<>(lastRemoveScoreCount);

        Team playerTeam = getTeamFor(session.getPlayerEntity().getUsername());
        DisplaySlot correctSidebarSlot = null;

        for (DisplaySlot slot : objectiveSlots.values()) {

            if (slot.updateType() == REMOVE) {
                continue;
            }

            if (playerTeam != null && playerTeam.color() == slot.teamColor()) {
                correctSidebarSlot = slot;
            }
        }

        if (correctSidebarSlot == null) {
            correctSidebarSlot = objectiveSlots.get(ScoreboardPosition.SIDEBAR);
        }

        var actualRemovedSlots = new ArrayList<>(removedSlots);
        for (var slot : actualRemovedSlots) {


            slot.remove();
        }
        removedSlots.removeAll(actualRemovedSlots);

        handleDisplaySlot(objectiveSlots.get(ScoreboardPosition.PLAYER_LIST), addScores, removeScores);
        handleDisplaySlot(correctSidebarSlot, addScores, removeScores);
        handleDisplaySlot(objectiveSlots.get(ScoreboardPosition.BELOW_NAME), addScores, removeScores);

        if (!removeScores.isEmpty()) {
            SetScorePacket packet = new SetScorePacket();
            packet.setAction(SetScorePacket.Action.REMOVE);
            packet.setInfos(removeScores);
            session.sendUpstreamPacket(packet);
        }

        if (!addScores.isEmpty()) {
            SetScorePacket packet = new SetScorePacket();
            packet.setAction(SetScorePacket.Action.SET);
            packet.setInfos(addScores);
            session.sendUpstreamPacket(packet);
        }

        lastAddScoreCount = addScores.size();
        lastRemoveScoreCount = removeScores.size();
        updateLockActive.set(false);
    }

    private void handleDisplaySlot(DisplaySlot slot, List<ScoreInfo> addScores, List<ScoreInfo> removeScores) {
        if (slot != null) {
            slot.render(addScores, removeScores);
        }
    }

    public Objective getObjective(std::string objectiveName) {
        return objectives.get(objectiveName);
    }

    public void removeObjective(Objective objective) {
        objectives.remove(objective.getObjectiveName());
        for (DisplaySlot slot : objective.getActiveSlots()) {
            objectiveSlots.remove(slot.position(), slot);
            removedSlots.add(slot);
        }
    }

    public void resetPlayerScores(std::string playerNameOrEntityUuid) {
        for (Objective objective : objectives.values()) {
            objective.removeScore(playerNameOrEntityUuid);
        }
    }

    public Team getTeam(std::string teamName) {
        return teams.get(teamName);
    }

    public Team getTeamFor(std::string playerNameOrEntityUuid) {
        return playerToTeam.get(playerNameOrEntityUuid);
    }

    public void removeTeam(std::string teamName) {
        Team remove = teams.remove(teamName);
        if (remove == null) {
            return;
        }
        remove.remove();
        session.removeCommandEnum("Geyser_Teams", remove.id());
    }

    @Contract("-> new")
    public Map<std::string, Set<CommandEnumConstraint>> getTeamNames() {
        return teams.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), o -> EnumSet.noneOf(CommandEnumConstraint.class),
                        (o1, o2) -> o1, LinkedHashMap::new));
    }

    public void playerRegistered(PlayerEntity player) {
        for (DisplaySlot slot : objectiveSlots.values()) {
            slot.playerRegistered(player);
        }
    }

    public void playerRemoved(PlayerEntity player) {
        for (DisplaySlot slot : objectiveSlots.values()) {
            slot.playerRemoved(player);
        }
    }

    public void entityRegistered(Entity entity) {
        var team = getTeamFor(entity.teamIdentifier());
        if (team != null) {
            team.onEntitySpawn(entity);
        }
    }

    public void entityRemoved(Entity entity) {
        var team = getTeamFor(entity.teamIdentifier());
        if (team != null) {
            team.onEntityRemove(entity);
        }
    }

    public void setTeamFor(Team team, Set<std::string> entities) {
        for (DisplaySlot slot : objectiveSlots.values()) {

            if (slot instanceof SidebarDisplaySlot sidebar) {
                sidebar.setTeamFor(team, entities);
            }
        }
    }

    public long nextId() {
        return nextId.getAndIncrement();
    }

    public GeyserSession session() {
        return session;
    }
}
