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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.scoreboard.display.slot.DisplaySlot;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType;

@Getter
public final class Objective {
    private final Scoreboard scoreboard;
    private final List<DisplaySlot> activeSlots = new ArrayList<>();

    private final String objectiveName;
    private final Map<String, ScoreReference> scores = new ConcurrentHashMap<>();

    private String displayName;
    private NumberFormat numberFormat;
    private ScoreType type;

    public Objective(Scoreboard scoreboard, String objectiveName) {
        this.scoreboard = scoreboard;
        this.objectiveName = objectiveName;
    }

    public void registerScore(String id, int score, Component displayName, NumberFormat numberFormat) {
        if (scores.containsKey(id)) {
            return;
        }
        var reference = new ScoreReference(scoreboard, id, score, displayName, numberFormat);
        scores.put(id, reference);

        for (var slot : activeSlots) {
            slot.addScore(reference);
        }
    }

    public void setScore(String id, int score, Component displayName, NumberFormat numberFormat) {
        ScoreReference stored = scores.get(id);
        if (stored != null) {
            stored.updateProperties(scoreboard, score, displayName, numberFormat);
            return;
        }
        registerScore(id, score, displayName, numberFormat);
    }

    public void removeScore(String id) {
        ScoreReference stored = scores.remove(id);
        if (stored != null) {
            stored.markDeleted();
        }
    }

    public void updateProperties(Component displayNameComponent, ScoreType type, NumberFormat format) {
        String displayName = MessageTranslator.convertMessageRaw(displayNameComponent, scoreboard.session().locale());
        boolean changed = !Objects.equals(this.displayName, displayName) || this.type != type;

        this.displayName = displayName;
        this.type = type;

        if (!Objects.equals(this.numberFormat, format)) {
            this.numberFormat = format;
            // update the number format for scores that are following this objective's number format,
            // but only if the objective itself doesn't need to be updated.
            // When the objective itself has to update all scores are updated anyway
            if (!changed) {
                for (ScoreReference score : scores.values()) {
                    if (score.numberFormat() == null) {
                        score.markChanged();
                    }
                }
            }
        }

        if (changed) {
            for (DisplaySlot slot : activeSlots) {
                slot.markNeedsUpdate();
            }
        }
    }

    public boolean hasDisplaySlot() {
        return !activeSlots.isEmpty();
    }

    public void addDisplaySlot(DisplaySlot slot) {
        activeSlots.add(slot);
    }

    public void removeDisplaySlot(DisplaySlot slot) {
        activeSlots.remove(slot);
    }
}
