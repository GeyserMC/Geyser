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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater.ScoreboardSession;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.ChunkUtils;

import java.util.Iterator;
import java.util.Map;

public final class WorldCache {
    private final GeyserSession session;
    @Getter
    private final ScoreboardSession scoreboardSession;
    @Getter
    private Scoreboard scoreboard;
    @Getter
    @Setter
    private Difficulty difficulty = Difficulty.EASY;

    /**
     * Whether our cooldown changed the title time, and the true title times need to be re-sent.
     */
    private boolean titleTimesNeedReset = false;
    private int trueTitleFadeInTime;
    private int trueTitleStayTime;
    private int trueTitleFadeOutTime;

    private int currentSequence;
    private final Map<Vector3i, ServerVerifiedState> unverifiedPredictions = new Object2ObjectOpenHashMap<>(1);

    public WorldCache(GeyserSession session) {
        this.session = session;
        this.scoreboard = new Scoreboard(session);
        scoreboardSession = new ScoreboardSession(session);
        resetTitleTimes(false);
    }

    public void removeScoreboard() {
        if (scoreboard != null) {
            scoreboard.removeScoreboard();
            scoreboard = new Scoreboard(session);
        }
    }

    public int increaseAndGetScoreboardPacketsPerSecond() {
        int pendingPps = scoreboardSession.getPendingPacketsPerSecond().incrementAndGet();
        int pps = scoreboardSession.getPacketsPerSecond();
        return Math.max(pps, pendingPps);
    }

    public void markTitleTimesAsIncorrect() {
        titleTimesNeedReset = true;
    }

    /**
     * Store the true active title times.
     */
    public void setTitleTimes(int fadeInTime, int stayTime, int fadeOutTime) {
        trueTitleFadeInTime = fadeInTime;
        trueTitleStayTime = stayTime;
        trueTitleFadeOutTime = fadeOutTime;
        // The translator will sync this for us
        titleTimesNeedReset = false;
    }

    /**
     * If needed, ensure that the Bedrock client will use the correct timings for titles.
     */
    public void synchronizeCorrectTitleTimes() {
        if (titleTimesNeedReset) {
            forceSyncCorrectTitleTimes();
        }
    }

    private void forceSyncCorrectTitleTimes() {
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.TIMES);
        titlePacket.setText("");
        titlePacket.setFadeInTime(trueTitleFadeInTime);
        titlePacket.setStayTime(trueTitleStayTime);
        titlePacket.setFadeOutTime(trueTitleFadeOutTime);
        titlePacket.setPlatformOnlineId("");
        titlePacket.setXuid("");

        session.sendUpstreamPacket(titlePacket);
        titleTimesNeedReset = false;
    }

    /**
     * Reset the true active title times to the (Java Edition 1.18.2) defaults.
     */
    public void resetTitleTimes(boolean clientSync) {
        trueTitleFadeInTime = 10;
        trueTitleStayTime = 70;
        trueTitleFadeOutTime = 20;

        if (clientSync) {
            forceSyncCorrectTitleTimes();
        }
    }

    /* Code to support the prediction structure introduced in Java Edition 1.19.0
    Blocks can be rolled back if invalid, but this requires some client-side information storage. */

    public int nextPredictionSequence() {
        return ++currentSequence;
    }

    /**
     * Stores a record of a block at a certain position to rollback in the event it is incorrect.
     */
    public void addServerCorrectBlockState(Vector3i position, int blockState) {
        if (session.isEmulatePost1_18Logic()) {
            // Cheap hack
            // On non-Bukkit platforms, ViaVersion will always confirm the sequence before the block is updated,
            // meaning we'd send two block updates after (ChunkUtils.updateBlockClientSide in endPredictionsUpTo
            // and the packet updating from the client)
            this.unverifiedPredictions.compute(position, ($, serverVerifiedState) -> serverVerifiedState == null
                    ? new ServerVerifiedState(currentSequence, blockState) : serverVerifiedState.setData(currentSequence, blockState));
        }
    }

    public void updateServerCorrectBlockState(Vector3i position) {
        if (this.unverifiedPredictions.isEmpty()) {
            return;
        }

        this.unverifiedPredictions.remove(position);
    }

    public void endPredictionsUpTo(int sequence) {
        if (this.unverifiedPredictions.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Vector3i, ServerVerifiedState>> it = this.unverifiedPredictions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Vector3i, ServerVerifiedState> entry = it.next();
            ServerVerifiedState serverVerifiedState = entry.getValue();
            if (serverVerifiedState.sequence <= sequence) {
                // This block may be out of sync with the server
                // In 1.19.0 Java, you can verify this by trying to mine in spawn protection
                ChunkUtils.updateBlockClientSide(session, serverVerifiedState.blockState, entry.getKey());
                it.remove();
            }
        }
    }

    private static class ServerVerifiedState {
        private int sequence;
        private int blockState;

        ServerVerifiedState(int sequence, int blockState) {
            this.sequence = sequence;
            this.blockState = blockState;
        }

        ServerVerifiedState setData(int sequence, int blockState) {
            this.sequence = sequence;
            this.blockState = blockState;
            return this;
        }

        @Override
        public String toString() {
            return "ServerVerifiedState{" +
                    "sequence=" + sequence +
                    ", blockState=" + blockState +
                    '}';
        }
    }
}