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

import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.ScoreReference;
import org.geysermc.geyser.scoreboard.display.slot.DisplaySlot;

public abstract class DisplayScore {
    protected final DisplaySlot slot;
    protected final long id;
    protected final ScoreReference reference;

    protected long lastTeamUpdate;
    protected long lastUpdate;

    public DisplayScore(DisplaySlot slot, long scoreId, ScoreReference reference) {
        this.slot = slot;
        this.id = scoreId;
        this.reference = reference;
    }

    public boolean shouldUpdate() {
        return reference.lastUpdate() != lastUpdate;
    }

    public abstract void update(Objective objective);

    public String name() {
        return reference.name();
    }

    public int score() {
        return reference.score();
    }

    public boolean referenceRemoved() {
        return reference.isRemoved();
    }

    protected void markUpdated() {
        // with the last update (also for team) we rather have an old lastUpdate
        // (and have to update again the next cycle) than potentially losing information
        // by fetching the lastUpdate after update was performed
        this.lastUpdate = reference.lastUpdate();
    }
}
