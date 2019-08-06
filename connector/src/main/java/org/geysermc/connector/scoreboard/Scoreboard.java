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

import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.packet.RemoveObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetDisplayObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Adapted from: https://github.com/Ragnok123/GTScoreboard
 */
public class Scoreboard {

    @Getter
    private ScoreboardObjective objective;

    private GeyserSession session;

    @Getter
    @Setter
    private long id;

    private Map<String, ScoreboardObjective> objectiveMap = new HashMap<String, ScoreboardObjective>();

    public Scoreboard(GeyserSession session) {
        this.session = session;

        id = new Random().nextLong();
    }

    public ScoreboardObjective registerNewObjective(String objectiveName) {
        ScoreboardObjective objective = new ScoreboardObjective();
        objective.setObjectiveName(objectiveName);
        this.objective = objective;
        if (!objectiveMap.containsKey(objectiveName)) {
            objectiveMap.put(objectiveName, objective);
        }

        return objective;
    }

    public ScoreboardObjective getObjective(String objectiveName) {
        ScoreboardObjective objective = null;
        if (objectiveMap.containsKey(objectiveName) && this.objective.getObjectiveName().contains(objectiveName)) {
            objective = this.objective;
        }

        return objective;
    }

    public void setObjective(String objectiveName) {
        if (objectiveMap.containsKey(objectiveName))
            objective = objectiveMap.get(objectiveName);
    }

    public void unregisterObjective(String objectiveName) {
        if (!objectiveMap.containsKey(objectiveName))
            return;

        if (objective.getObjectiveName().equals(objectiveName)) {
            objective = null;
        }

        objectiveMap.remove(objectiveName);
    }

    public void onUpdate() {
        if (objective == null)
            return;

        RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
        removeObjectivePacket.setObjectiveId(objective.getObjectiveName());
        session.getUpstream().sendPacket(removeObjectivePacket);

        SetDisplayObjectivePacket displayObjectivePacket = new SetDisplayObjectivePacket();
        displayObjectivePacket.setObjectiveId(objective.getObjectiveName());
        displayObjectivePacket.setDisplayName(objective.getDisplayName());
        displayObjectivePacket.setCriteria("dummy");
        displayObjectivePacket.setDisplaySlot("sidebar");
        displayObjectivePacket.setSortOrder(1);
        session.getUpstream().sendPacket(displayObjectivePacket);

        Map<String, Score> fakeMap = new HashMap<String, Score>();
        for (Map.Entry<String, Score> entry : objective.getScores().entrySet()) {
            fakeMap.put(entry.getKey(), entry.getValue());
        }

        for (String string : fakeMap.keySet()) {
            Score score = fakeMap.get(string);
            ScoreInfo scoreInfo = new ScoreInfo(score.getScoreboardId(), objective.getObjectiveName(), score.getScore(), score.getFakePlayer());

            SetScorePacket setScorePacket = new SetScorePacket();
            setScorePacket.setAction(score.getAction());
            setScorePacket.setInfos(Arrays.asList(scoreInfo));
            session.getUpstream().sendPacket(setScorePacket);

            if (score.getAction() == SetScorePacket.Action.REMOVE) {
                String id = score.getFakeId();
                objective.getScores().remove(id);
            }
        }
    }
}
