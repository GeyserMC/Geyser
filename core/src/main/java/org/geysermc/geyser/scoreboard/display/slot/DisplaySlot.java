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

package org.geysermc.geyser.scoreboard.display.slot;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.ScoreReference;
import org.geysermc.geyser.scoreboard.UpdateType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreboardPosition;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;

public abstract class DisplaySlot {
    protected final GeyserSession session;
    protected final Objective objective;
    /**
     * Use this instead of objective name because one objective can be shared in multiple slots,
     * but each slot has its own logic and might not contain all scores
     */
    protected final String objectiveId;
    protected final ScoreboardPosition slot;
    protected final TeamColor teamColor;
    protected final String positionName;

    protected UpdateType updateType = UpdateType.ADD;

    public DisplaySlot(GeyserSession session, Objective objective, ScoreboardPosition slot) {
        this.session = session;
        this.objective = objective;
        this.objectiveId = String.valueOf(objective.getScoreboard().nextId());
        this.slot = slot;
        this.teamColor = teamColor(slot);
        this.positionName = positionName(slot);
    }

    public final void render(List<ScoreInfo> addScores, List<ScoreInfo> removeScores) {
        if (updateType == UpdateType.REMOVE) {
            return;
        }
        render0(addScores, removeScores);
    }

    protected abstract void render0(List<ScoreInfo> addScores, List<ScoreInfo> removeScores);

    public abstract void addScore(ScoreReference reference);

    public abstract void playerRegistered(PlayerEntity player);
    public abstract void playerRemoved(PlayerEntity player);

    public void remove() {
        updateType = UpdateType.REMOVE;
        sendRemoveObjective();
    }

    public void markNeedsUpdate() {
        if (updateType == UpdateType.NOTHING) {
            updateType = UpdateType.UPDATE;
        }
    }

    protected void sendDisplayObjective() {
        SetDisplayObjectivePacket packet = new SetDisplayObjectivePacket();
        packet.setObjectiveId(objectiveId());
        packet.setDisplayName(objective.getDisplayName());
        packet.setCriteria("dummy");
        packet.setDisplaySlot(positionName);
        packet.setSortOrder(1); // 0 = ascending, 1 = descending
        session.sendUpstreamPacket(packet);
    }

    protected void sendRemoveObjective() {
        RemoveObjectivePacket packet = new RemoveObjectivePacket();
        packet.setObjectiveId(objectiveId());
        session.sendUpstreamPacket(packet);
    }

    public Objective objective() {
        return objective;
    }

    public String objectiveId() {
        return objectiveId;
    }

    public ScoreboardPosition position() {
        return slot;
    }

    public @Nullable TeamColor teamColor() {
        return teamColor;
    }

    public UpdateType updateType() {
        return updateType;
    }

    public static ScoreboardPosition slotCategory(ScoreboardPosition slot) {
        return switch (slot) {
            case BELOW_NAME -> ScoreboardPosition.BELOW_NAME;
            case PLAYER_LIST -> ScoreboardPosition.PLAYER_LIST;
            default -> ScoreboardPosition.SIDEBAR;
        };
    }

    private static String positionName(ScoreboardPosition slot) {
        return switch (slot) {
            case BELOW_NAME -> "belowname";
            case PLAYER_LIST -> "list";
            default -> "sidebar";
        };
    }

    private static @Nullable TeamColor teamColor(ScoreboardPosition slot) {
        return switch (slot) {
            case SIDEBAR_TEAM_RED -> TeamColor.RED;
            case SIDEBAR_TEAM_AQUA -> TeamColor.AQUA;
            case SIDEBAR_TEAM_BLUE -> TeamColor.BLUE;
            case SIDEBAR_TEAM_GOLD -> TeamColor.GOLD;
            case SIDEBAR_TEAM_GRAY -> TeamColor.GRAY;
            case SIDEBAR_TEAM_BLACK -> TeamColor.BLACK;
            case SIDEBAR_TEAM_GREEN -> TeamColor.GREEN;
            case SIDEBAR_TEAM_WHITE -> TeamColor.WHITE;
            case SIDEBAR_TEAM_YELLOW -> TeamColor.YELLOW;
            case SIDEBAR_TEAM_DARK_RED -> TeamColor.DARK_RED;
            case SIDEBAR_TEAM_DARK_AQUA -> TeamColor.DARK_AQUA;
            case SIDEBAR_TEAM_DARK_BLUE -> TeamColor.DARK_BLUE;
            case SIDEBAR_TEAM_DARK_GRAY -> TeamColor.DARK_GRAY;
            case SIDEBAR_TEAM_DARK_GREEN -> TeamColor.DARK_GREEN;
            case SIDEBAR_TEAM_DARK_PURPLE -> TeamColor.DARK_PURPLE;
            case SIDEBAR_TEAM_LIGHT_PURPLE -> TeamColor.LIGHT_PURPLE;
            default -> null;
        };
    }
}
