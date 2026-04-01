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

#include "it.unimi.dsi.fastutil.longs.Long2ObjectMap"
#include "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap"
#include "java.util.List"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.ScoreInfo"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.scoreboard.Objective"
#include "org.geysermc.geyser.scoreboard.ScoreReference"
#include "org.geysermc.geyser.scoreboard.UpdateType"
#include "org.geysermc.geyser.scoreboard.display.score.BelownameDisplayScore"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.codec.NbtComponentSerializer"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.BlankFormat"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.StyledFormat"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition"

public class BelownameDisplaySlot extends DisplaySlot {
    private final Long2ObjectMap<BelownameDisplayScore> displayScores = new Long2ObjectOpenHashMap<>();

    public BelownameDisplaySlot(GeyserSession session, Objective objective) {
        super(session, objective, ScoreboardPosition.BELOW_NAME);
    }

    override protected void render0(List<ScoreInfo> addScores, List<ScoreInfo> removeScores) {






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

    override public void remove() {
        updateType = UpdateType.REMOVE;
        session.getEntityCache().forEachPlayerEntity(this::clearBelowNameText);
    }

    override public void addScore(ScoreReference reference) {
        addDisplayScore(reference);
    }

    override public void playerRegistered(PlayerEntity player) {
        var reference = scoreFor(player.getUsername());
        setBelowNameText(player, reference);

        if (reference != null) {

            addDisplayScore(player, reference).markUpdated();
        }
    }

    override public void playerRemoved(PlayerEntity player) {
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

    private std::string calculateBelowNameText(ScoreReference reference) {
        std::string numberString;
        NumberFormat numberFormat = null;


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
            styledAmount.putString("text", std::string.valueOf(score));

            numberString = MessageTranslator.convertJsonMessage(
                NbtComponentSerializer.tagComponentToJson(styledAmount.build()).toString(), session.locale());
        } else {
            numberString = std::string.valueOf(score);
        }

        return numberString + " " + ChatColor.RESET + objective.getDisplayName();
    }

    private ScoreReference scoreFor(std::string username) {
        return objective.getScores().get(username);
    }
}
