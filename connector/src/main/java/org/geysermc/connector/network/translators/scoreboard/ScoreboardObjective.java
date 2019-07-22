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

package org.geysermc.connector.network.translators.scoreboard;

import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.console.GeyserLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapted from: https://github.com/Ragnok123/GTScoreboard
 */
public class ScoreboardObjective {

    @Getter
    @Setter
    private int scoreboardTick = 0;

    @Getter
    @Setter
    private String objectiveName;

    @Getter
    @Setter
    private DisplaySlot displaySlot;

    @Getter
    @Setter
    private String displayName;

    @Getter
    private Map<String, Score> scores = new HashMap<String, Score>();

    public void registerScore(String id, String fake, int value) {
        registerScore(id, fake, value, SetScorePacket.Action.SET);
    }

    public void registerScore(String id, String fake, int value, SetScorePacket.Action action) {
        Score score = new Score(this, fake);
        score.setScore(value);
        score.setFakeId(id);
        score.setAction(action);
        if (!scores.containsKey(id)) {
            scores.put(id, score);
        } else {
            setScore(id, value);
        }
    }

    public void setScore(String id, int value) {
        if (scores.containsKey(id)) {
            Score modifiedScore = scores.get(id);
            modifiedScore.setScore(value);
            scores.remove(id);
            scores.put(id, modifiedScore);
        }
    }

    public void setScoreText(String id, String text) {
        if (scores.containsKey(id)) {
            Score oldScore = scores.get(id);
            oldScore.setAction(SetScorePacket.Action.REMOVE);
            oldScore.setFakeId(id + "_old_changed");

            Score newScore = new Score(this, text);
            newScore.setScore(oldScore.getScore());
            newScore.setFakeId(id);
            scores.remove(id);
            scores.put(id, newScore);
            scores.put(id + "_old_changed", oldScore);
        }
    }

    public int getScore(String id) {
        int i = 0;
        if (scores.containsKey(id)) {
            Score score = scores.get(id);
            i = score.getScore();
        }

        return i;
    }

    public void resetScore(String id) {
        if (scores.containsKey(id)) {
            Score modifiedScore = scores.get(id);
            modifiedScore.setAction(SetScorePacket.Action.REMOVE);
            scores.remove(id);
            scores.put(id, modifiedScore);
        }
    }

    public enum DisplaySlot {

        SIDEBAR,
        LIST,
        BELOWNAME;

    }
}
