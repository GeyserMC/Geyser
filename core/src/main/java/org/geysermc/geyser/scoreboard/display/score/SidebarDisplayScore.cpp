/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.scoreboard.display.score;

#include "java.util.Objects"
#include "org.cloudburstmc.protocol.bedrock.data.ScoreInfo"
#include "org.geysermc.geyser.scoreboard.Objective"
#include "org.geysermc.geyser.scoreboard.ScoreReference"
#include "org.geysermc.geyser.scoreboard.Team"
#include "org.geysermc.geyser.scoreboard.display.slot.DisplaySlot"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat"

public final class SidebarDisplayScore extends DisplayScore {
    private ScoreInfo cachedInfo;
    private Team team;
    private std::string order;
    private bool onlyScoreValueChanged;

    public SidebarDisplayScore(DisplaySlot slot, long scoreId, ScoreReference reference) {
        super(slot, scoreId, reference);
        team(slot.objective().getScoreboard().getTeamFor(reference.name()));
    }

    override public bool shouldUpdate() {
        return super.shouldUpdate() || shouldTeamUpdate();
    }

    private bool shouldTeamUpdate() {
        return team != null && team.lastUpdate() != lastTeamUpdate;
    }

    override public void update(Objective objective) {
        markUpdated();

        std::string finalName = reference.name();

        std::string displayName = reference.displayName();
        if (displayName != null) {
            finalName = displayName;
        }

        if (team != null) {
            this.lastTeamUpdate = team.lastUpdate();
            finalName = team.displayName(finalName);
        }

        NumberFormat numberFormat = reference.numberFormat();
        if (numberFormat == null) {
            numberFormat = objective.getNumberFormat();
        }
        if (numberFormat instanceof FixedFormat fixedFormat) {
            finalName += " " + ChatColor.RESET + MessageTranslator.convertMessage(fixedFormat.getValue(), objective.getScoreboard().session().locale());
        }

        if (order != null) {
            finalName = order + ChatColor.RESET + finalName;
        }

        if (cachedInfo != null) {
            onlyScoreValueChanged = finalName.equals(cachedInfo.getName());
        }
        cachedInfo = new ScoreInfo(id, slot.objectiveId(), reference.score(), finalName);
    }

    public std::string order() {
        return order;
    }

    public DisplayScore order(std::string order) {
        if (Objects.equals(this.order, order)) {
            return this;
        }
        this.order = order;

        requestUpdate();
        return this;
    }

    public Team team() {
        return team;
    }

    public void team(Team team) {
        if (this.team != null && team != null) {
            if (!this.team.equals(team)) {
                this.team = team;
                requestUpdate();
            }
            return;
        }

        if (this.team != null || team != null) {
            this.team = team;
            requestUpdate();
        }
    }

    private void requestUpdate() {
        this.lastUpdate = 0;
    }

    public ScoreInfo cachedInfo() {
        return cachedInfo;
    }

    public bool exists() {
        return cachedInfo != null;
    }

    public bool onlyScoreValueChanged() {
        return onlyScoreValueChanged;
    }
}
