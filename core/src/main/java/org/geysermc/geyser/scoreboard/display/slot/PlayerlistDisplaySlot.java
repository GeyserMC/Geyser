/*
 * Copyright (c) 2024-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.scoreboard.display.slot;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.ScoreReference;
import org.geysermc.geyser.scoreboard.UpdateType;
import org.geysermc.geyser.scoreboard.display.score.PlayerlistDisplayScore;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;

public class PlayerlistDisplaySlot extends DisplaySlot {
    private final Long2ObjectMap<PlayerlistDisplayScore> displayScores = new Long2ObjectOpenHashMap<>();
    private final List<PlayerlistDisplayScore> removedScores = Collections.synchronizedList(new ArrayList<>());

    public PlayerlistDisplaySlot(GeyserSession session, Objective objective) {
        super(session, objective, ScoreboardPosition.PLAYER_LIST);
        registerExisting();
    }

    @Override
    protected void render0(List<ScoreInfo> addScores, List<ScoreInfo> removeScores) {
        boolean objectiveAdd = updateType == UpdateType.ADD;
        boolean objectiveUpdate = updateType == UpdateType.UPDATE;
        boolean objectiveNothing = updateType == UpdateType.NOTHING;

        // if 'add' the scores aren't present, if 'update' the objective is re-added so the scores don't have to be
        // manually removed, if 'remove' the scores are removed anyway
        if (objectiveNothing) {
            var removedScoresCopy = new ArrayList<>(removedScores);
            for (var removedScore : removedScoresCopy) {
                //todo idk if this if-statement is needed
                if (removedScore.cachedInfo() != null) {
                    removeScores.add(removedScore.cachedInfo());
                }
            }
            removedScores.removeAll(removedScoresCopy);
        } else {
            removedScores.clear();
        }

        synchronized (displayScores) {
            for (var score : displayScores.values()) {
                if (score.referenceRemoved()) {
                    ScoreInfo cachedInfo = score.cachedInfo();
                    // cachedInfo can be null here when ScoreboardUpdater is being used and a score is added and
                    // removed before a single update cycle is performed
                    if (cachedInfo != null) {
                        removeScores.add(cachedInfo);
                    }
                    continue;
                }

                //todo does an animated title exist on tab?
                boolean add = objectiveAdd || objectiveUpdate;
                boolean exists = score.exists();

                if (score.shouldUpdate()) {
                    score.update(objective);
                    add = true;
                }

                if (add) {
                    addScores.add(score.cachedInfo());
                }

                // we need this as long as MCPE-143063 hasn't been fixed.
                // the checks after 'add' are there to prevent removing scores that
                // are going to be removed anyway / don't need to be removed
                if (add && exists && objectiveNothing) {
                    removeScores.add(score.cachedInfo());
                }
            }
        }

        if (objectiveUpdate) {
            sendRemoveObjective();
        }

        if (objectiveAdd || objectiveUpdate) {
            sendDisplayObjective();
        }

        updateType = UpdateType.NOTHING;
    }

    @Override
    public void addScore(ScoreReference reference) {
        // while it breaks a lot of stuff in Java, scoreboard do work fine with multiple players having
        // the same username
        var players = session.getEntityCache().getPlayersByName(reference.name());
        var selfPlayer = session.getPlayerEntity();
        if (reference.name().equals(selfPlayer.getUsername())) {
            players.add(selfPlayer);
        }

        synchronized (displayScores) {
            for (PlayerEntity player : players) {
                var score = new PlayerlistDisplayScore(this, objective.getScoreboard().nextId(), reference, player.geyserId());
                displayScores.put(player.geyserId(), score);
            }
        }
    }

    private void registerExisting() {
        playerRegistered(session.getPlayerEntity());
        session.getEntityCache().forEachPlayerEntity(this::playerRegistered);
    }

    @Override
    public void playerRegistered(PlayerEntity player) {
        var reference = objective.getScores().get(player.getUsername());
        if (reference == null) {
            return;
        }

        var score = new PlayerlistDisplayScore(this, objective.getScoreboard().nextId(), reference, player.geyserId());
        synchronized (displayScores) {
            displayScores.put(player.geyserId(), score);
        }
    }

    @Override
    public void playerRemoved(PlayerEntity player) {
        PlayerlistDisplayScore score;
        synchronized (displayScores) {
            score = displayScores.remove(player.geyserId());
        }

        if (score == null) {
            return;
        }
        removedScores.add(score);
    }
}
