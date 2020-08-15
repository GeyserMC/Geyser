/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.scoreboard.Objective;
import org.geysermc.connector.scoreboard.Scoreboard;

import java.util.Collection;

@Getter
public class WorldCache {

    private GeyserSession session;

    @Setter
    private Difficulty difficulty = Difficulty.EASY;

    private boolean showCoordinates = true;

    private Scoreboard scoreboard;

    public WorldCache(GeyserSession session) {
        this.session = session;
        this.scoreboard = new Scoreboard(session);
    }

    public void removeScoreboard() {
        if (scoreboard != null) {
            Collection<Objective> objectives = scoreboard.getObjectives().values();
            scoreboard = new Scoreboard(session);

            for (Objective objective : objectives) {
                scoreboard.despawnObjective(objective);
            }
        }
    }

    /**
     * Tell the client to hide or show the coordinates
     *
     * @param value True to show, false to hide
     */
    public void setShowCoordinates(boolean value) {
        showCoordinates = value;
        session.sendGameRule("showcoordinates", value);
    }
}