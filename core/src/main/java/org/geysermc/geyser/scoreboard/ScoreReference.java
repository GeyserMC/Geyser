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

package org.geysermc.geyser.scoreboard;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;

public final class ScoreReference {
    public static final long LAST_UPDATE_DEFAULT = -1;
    private static final long LAST_UPDATE_REMOVE = -2;

    private final String name;
    private final boolean hidden;

    private String displayName;
    private int score;
    private NumberFormat numberFormat;

    private long lastUpdate;

    public ScoreReference(
        Scoreboard scoreboard, String name, int score, Component displayName, NumberFormat format) {
        this.name = name;
        // hidden is a sidebar exclusive feature
        this.hidden = name.startsWith("#");

        updateProperties(scoreboard, score, displayName, format);
        this.lastUpdate = LAST_UPDATE_DEFAULT;
    }

    public String name() {
        return name;
    }

    public boolean hidden() {
        return hidden;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(Component displayName, Scoreboard scoreboard) {
        if (this.displayName != null && displayName != null) {
            String convertedDisplayName = MessageTranslator.convertMessage(displayName, scoreboard.session().locale());
            if (!this.displayName.equals(convertedDisplayName)) {
                this.displayName = convertedDisplayName;
                markChanged();
            }
            return;
        }
        // simplified from (this.displayName != null && displayName == null) || (this.displayName == null && displayName != null)
        if (this.displayName != null || displayName != null) {
            this.displayName = MessageTranslator.convertMessage(displayName, scoreboard.session().locale());
            markChanged();
        }
    }

    public int score() {
        return score;
    }

    private void score(int score) {
        boolean changed = this.score != score;
        this.score = score;
        if (changed) {
            markChanged();
        }
    }

    public NumberFormat numberFormat() {
        return numberFormat;
    }

    private void numberFormat(NumberFormat numberFormat) {
        if (Objects.equals(numberFormat(), numberFormat)) {
            return;
        }
        this.numberFormat = numberFormat;
        markChanged();
    }

    public void updateProperties(Scoreboard scoreboard, int score, Component displayName, NumberFormat numberFormat) {
        score(score);
        displayName(displayName, scoreboard);
        numberFormat(numberFormat);
    }

    public long lastUpdate() {
        return lastUpdate;
    }

    public boolean isRemoved() {
        return lastUpdate == LAST_UPDATE_REMOVE;
    }

    public void markChanged() {
        if (lastUpdate == LAST_UPDATE_REMOVE) {
            return;
        }
        lastUpdate = System.currentTimeMillis();
    }

    public void markDeleted() {
        lastUpdate = -1;
    }
}
