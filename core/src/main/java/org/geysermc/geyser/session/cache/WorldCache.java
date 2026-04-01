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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater.ScoreboardSession;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public final class WorldCache {
    private final GeyserSession session;
    @Getter
    private final ScoreboardSession scoreboardSession;
    @Getter
    private @NonNull Scoreboard scoreboard;
    @Getter
    @Setter
    private Difficulty difficulty = Difficulty.EASY;

    
    private boolean titleTimesNeedReset = false;
    private int trueTitleFadeInTime;
    private int trueTitleStayTime;
    private int trueTitleFadeOutTime;

    private int currentSequence;
    private final Object2IntMap<Vector3i> unverifiedPredictions = new Object2IntOpenHashMap<>(1);

    private final Map<Vector3i, String> activeRecords = new Object2ObjectOpenHashMap<>(1); 

    @Getter
    @Setter
    private boolean editingSignOnFront;

    private final Object2IntMap<String> activeCooldowns = new Object2IntOpenHashMap<>(2);

    public WorldCache(GeyserSession session) {
        this.session = session;
        this.scoreboard = new Scoreboard(session);
        scoreboardSession = new ScoreboardSession(session);
        resetTitleTimes(false);
    }

    public void resetScoreboard() {
        scoreboard.removeScoreboard();
        scoreboard = new Scoreboard(session);
    }

    public int increaseAndGetScoreboardPacketsPerSecond() {
        int pendingPps = scoreboardSession.getPendingPacketsPerSecond().incrementAndGet();
        int pps = scoreboardSession.getPacketsPerSecond();
        return Math.max(pps, pendingPps);
    }

    public boolean isTitleTimesIncorrect() {
        return titleTimesNeedReset;
    }

    public void markTitleTimesAsIncorrect() {
        titleTimesNeedReset = true;
    }

    
    public void setTitleTimes(int fadeInTime, int stayTime, int fadeOutTime) {
        trueTitleFadeInTime = fadeInTime;
        trueTitleStayTime = stayTime;
        trueTitleFadeOutTime = fadeOutTime;
        
        titleTimesNeedReset = false;
    }

    
    public void synchronizeCorrectTitleTimes() {
        if (titleTimesNeedReset) {
            forceSyncCorrectTitleTimes();
        }
    }

    private void forceSyncCorrectTitleTimes() {
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.TIMES);
        titlePacket.setText("");

        
        
        int tickrateMultiplier = Math.round(session.getMillisecondsPerTick()) / 50;
        titlePacket.setFadeInTime(trueTitleFadeInTime * tickrateMultiplier);
        titlePacket.setStayTime(trueTitleStayTime * tickrateMultiplier);
        titlePacket.setFadeOutTime(trueTitleFadeOutTime * tickrateMultiplier);
        titlePacket.setPlatformOnlineId("");
        titlePacket.setXuid("");

        session.sendUpstreamPacket(titlePacket);
        titleTimesNeedReset = false;
    }

    
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

    
    public void markPositionInSequence(Vector3i position) {
        if (session.isEmulatePost1_18Logic()) {
            
            
            
            
            this.unverifiedPredictions.put(position, currentSequence);
        }
    }

    public void updateServerCorrectBlockState(Vector3i position, int blockState) {
        if (!this.unverifiedPredictions.isEmpty()) {
            this.unverifiedPredictions.removeInt(position);
        }

        
        Vector3i clientBreakPos = session.getBlockBreakHandler().getCurrentBlockPos();
        if (clientBreakPos != null && Objects.equals(clientBreakPos, position)) {
            session.getBlockBreakHandler().setUpdatedServerBlockStateId(blockState);
        }

        ChunkUtils.updateBlock(session, blockState, position);
    }

    public void endPredictionsUpTo(int sequence) {
        if (this.unverifiedPredictions.isEmpty()) {
            return;
        }

        Iterator<Object2IntMap.Entry<Vector3i>> it = Object2IntMaps.fastIterator(this.unverifiedPredictions);
        while (it.hasNext()) {
            Object2IntMap.Entry<Vector3i> entry = it.next();
            if (entry.getIntValue() <= sequence) {
                
                
                Vector3i position = entry.getKey();
                ChunkUtils.updateBlockClientSide(session, session.getGeyser().getWorldManager().blockAt(session, position), position);
                it.remove();
            }
        }
    }

    public void addActiveRecord(Vector3i pos, String bedrockPlaySound) {
        this.activeRecords.put(pos, bedrockPlaySound);
    }

    
    
    @Nullable
    public String removeActiveRecord(Vector3i pos) {
        return this.activeRecords.remove(pos);
    }

    public void setCooldown(Key cooldownGroup, int ticks) {
        if (ticks == 0) {
            
            this.activeCooldowns.removeInt(cooldownGroup.asString());
            return;
        }
        this.activeCooldowns.put(cooldownGroup.asString(), session.getTicks() + ticks);
    }

    public boolean hasCooldown(GeyserItemStack item) {
        UseCooldown cooldown = item.getComponent(DataComponentTypes.USE_COOLDOWN);
        String cooldownGroup;
        if (cooldown != null && cooldown.cooldownGroup() != null) {
            cooldownGroup = cooldown.cooldownGroup().asString();
        } else {
            cooldownGroup = item.asItem().javaIdentifier();
        }
        return this.activeCooldowns.containsKey(cooldownGroup);
    }

    public void tick() {
        
        
        if (!this.activeCooldowns.isEmpty()) {
            int ticks = session.getTicks();
            Iterator<Object2IntMap.Entry<String>> it = Object2IntMaps.fastIterator(this.activeCooldowns);
            while (it.hasNext()) {
                Object2IntMap.Entry<String> entry = it.next();
                if (entry.getIntValue() <= ticks) {
                    it.remove();
                }
            }
        }
    }
}
