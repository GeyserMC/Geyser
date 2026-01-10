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
import java.util.List;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.ScoreReference;
import org.geysermc.geyser.scoreboard.UpdateType;
import org.geysermc.geyser.scoreboard.display.score.BelownameDisplayScore;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.codec.NbtComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.BlankFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.StyledFormat;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;

public class BelownameDisplaySlot extends DisplaySlot {
    private final Long2ObjectMap<BelownameDisplayScore> displayScores = new Long2ObjectOpenHashMap<>();

    public BelownameDisplaySlot(GeyserSession session, Objective objective) {
        super(session, objective, ScoreboardPosition.BELOW_NAME);
    }

    @Override
    protected void render0(List<ScoreInfo> addScores, List<ScoreInfo> removeScores) {
        // how belowname works is that if the player itself has belowname as a display slot,
        // every player entity will show a score below their name.
        // when the objective is added, updated or removed we thus have to update the belowname for every player
        // when an individual score is updated (score or number format) we have to update the individual player

        // remove is handled in #remove()
        if (updateType == UpdateType.ADD) {
            session.getEntityCache().forEachPlayerEntity(this::playerRegistered);
            return;
        }
        if (updateType == UpdateType.UPDATE) {
            session.getEntityCache().forEachPlayerEntity(player -> {
                setBelowNameText(player, scoreFor(player.getUsername()));
            });
            updateType = UpdateType.NOTHING;
            return;
        }

        synchronized (displayScores) {
            for (var score : displayScores.values()) {
                // we don't have to worry about a score not existing, because that's handled by both
                // this method when an objective is added and addScore/playerRegistered.
                // we only have to update them, if they have changed
                // (or delete them, if the score no longer exists)
                if (!score.shouldUpdate()) {
                    continue;
                }

                if (score.referenceRemoved()) {
                    clearBelowNameText(score.player());
                    continue;
                }

                score.markUpdated();
                setBelowNameText(score.player(), score.reference());
            }
        }
    }

    @Override
    public void remove() {
        updateType = UpdateType.REMOVE;
        session.getEntityCache().forEachPlayerEntity(this::clearBelowNameText);
    }

    @Override
    public void addScore(ScoreReference reference) {
        addDisplayScore(reference);
    }

    @Override
    public void playerRegistered(PlayerEntity player) {
        var reference = scoreFor(player.getUsername());
        setBelowNameText(player, reference);
        // keep track of score when the player is active
        if (reference != null) {
            // we already set the text, so we only have to update once the score does
            addDisplayScore(player, reference).markUpdated();
        }
    }

    @Override
    public void playerRemoved(PlayerEntity player) {
        synchronized (displayScores) {
            displayScores.remove(player.geyserId());
        }
    }

    private void addDisplayScore(ScoreReference reference) {
        var players = session.getEntityCache().getPlayersByName(reference.name());
        for (PlayerEntity player : players) {
            addDisplayScore(player, reference);
        }
    }

    private BelownameDisplayScore addDisplayScore(PlayerEntity player, ScoreReference reference) {
        var score = new BelownameDisplayScore(this, objective.getScoreboard().nextId(), reference, player);
        synchronized (displayScores) {
            displayScores.put(player.geyserId(), score);
        }
        return score;
    }

    private void setBelowNameText(PlayerEntity player, ScoreReference reference) {
        player.setBelowNameText(calculateBelowNameText(reference));
        player.updateBedrockMetadata();
    }

    private void clearBelowNameText(PlayerEntity player) {
        player.setBelowNameText(null);
        player.updateBedrockMetadata();
    }

    private String calculateBelowNameText(ScoreReference reference) {
        String numberString;
        NumberFormat numberFormat = null;
        // even if the player doesn't have a score, as long as belowname is on the client Java behaviour is
        // to show them with a score of 0
        int score = 0;
        if (reference != null) {
            score = reference.score();
            numberFormat = reference.numberFormat();
        }
        if (numberFormat == null) {
            numberFormat = objective.getNumberFormat();
        }

        if (numberFormat instanceof BlankFormat) {
            numberString = "";
        } else if (numberFormat instanceof FixedFormat fixedFormat) {
            numberString = MessageTranslator.convertMessage(fixedFormat.getValue(), session.locale());
        } else if (numberFormat instanceof StyledFormat styledFormat) {
            NbtMapBuilder styledAmount = styledFormat.getStyle().toBuilder();
            styledAmount.putString("text", String.valueOf(score));

            numberString = MessageTranslator.convertJsonMessage(
                NbtComponentSerializer.tagComponentToJson(styledAmount.build()).toString(), session.locale());
        } else {
            numberString = String.valueOf(score);
        }

        return numberString + " " + ChatColor.RESET + objective.getDisplayName();
    }

    private ScoreReference scoreFor(String username) {
        return objective.getScores().get(username);
    }
}
