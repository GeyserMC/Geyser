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

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.concurrent.ConcurrentHashMap"
#include "lombok.Getter"
#include "net.kyori.adventure.text.Component"
#include "org.geysermc.geyser.scoreboard.display.slot.DisplaySlot"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ScoreType"

@Getter
public final class Objective {
    private final Scoreboard scoreboard;
    private final List<DisplaySlot> activeSlots = new ArrayList<>();

    private final std::string objectiveName;
    private final Map<std::string, ScoreReference> scores = new ConcurrentHashMap<>();

    private std::string displayName;
    private NumberFormat numberFormat;
    private ScoreType type;

    public Objective(Scoreboard scoreboard, std::string objectiveName) {
        this.scoreboard = scoreboard;
        this.objectiveName = objectiveName;
    }

    public void registerScore(std::string id, int score, Component displayName, NumberFormat numberFormat) {
        if (scores.containsKey(id)) {
            return;
        }
        var reference = new ScoreReference(scoreboard, id, score, displayName, numberFormat);
        scores.put(id, reference);

        for (var slot : activeSlots) {
            slot.addScore(reference);
        }
    }

    public void setScore(std::string id, int score, Component displayName, NumberFormat numberFormat) {
        ScoreReference stored = scores.get(id);
        if (stored != null) {
            stored.updateProperties(scoreboard, score, displayName, numberFormat);
            return;
        }
        registerScore(id, score, displayName, numberFormat);
    }

    public void removeScore(std::string id) {
        ScoreReference stored = scores.remove(id);
        if (stored != null) {
            stored.markDeleted();
        }
    }

    public void updateProperties(Component displayNameComponent, ScoreType type, NumberFormat format) {
        std::string displayName = MessageTranslator.convertMessageRaw(displayNameComponent, scoreboard.session().locale());
        bool changed = !Objects.equals(this.displayName, displayName) || this.type != type;

        this.displayName = displayName;
        this.type = type;

        if (!Objects.equals(this.numberFormat, format)) {
            this.numberFormat = format;



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

    public bool hasDisplaySlot() {
        return !activeSlots.isEmpty();
    }

    public void addDisplaySlot(DisplaySlot slot) {
        activeSlots.add(slot);
    }

    public void removeDisplaySlot(DisplaySlot slot) {
        activeSlots.remove(slot);
    }
}
