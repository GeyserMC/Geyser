/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import lombok.NonNull;
import org.geysermc.geyser.session.cache.AdvancementsCache;

import java.util.List;

/**
 * A wrapper around MCProtocolLib's {@link Advancement} class so we can control the parent of an advancement
 */
public class GeyserAdvancement {
    private final Advancement advancement;
    private String rootId = null;

    public static GeyserAdvancement from(Advancement advancement) {
        return new GeyserAdvancement(advancement);
    }

    private GeyserAdvancement(Advancement advancement) {
        this.advancement = advancement;
    }

    @NonNull
    public String getId() {
        return this.advancement.getId();
    }

    @NonNull
    public List<String> getCriteria() {
        return this.advancement.getCriteria();
    }

    @NonNull
    public List<List<String>> getRequirements() {
        return this.advancement.getRequirements();
    }

    public String getParentId() {
        return this.advancement.getParentId();
    }

    public Advancement.DisplayData getDisplayData() {
        return this.advancement.getDisplayData();
    }

    public String getRootId(AdvancementsCache advancementsCache) {
        if (rootId == null) {
            if (this.advancement.getParentId() == null) {
                // We are the root ID
                this.rootId = this.advancement.getId();
            } else {
                // Go through our cache, and descend until we find the root ID
                GeyserAdvancement advancement = advancementsCache.getStoredAdvancements().get(this.advancement.getParentId());
                if (advancement.getParentId() == null) {
                    this.rootId = advancement.getId();
                } else {
                    this.rootId = advancement.getRootId(advancementsCache);
                }
            }
        }
        return rootId;
    }
}
