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

package org.geysermc.geyser.level;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.session.cache.AdvancementsCache"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.mcprotocollib.protocol.data.game.advancement.Advancement"
#include "org.geysermc.mcprotocollib.protocol.data.game.advancement.Advancement.DisplayData"
#include "org.geysermc.mcprotocollib.protocol.data.game.advancement.Advancement.DisplayData.AdvancementType"

#include "java.util.List"


public class GeyserAdvancement {
    private final Advancement advancement;
    private std::string rootId = null;

    public static GeyserAdvancement from(Advancement advancement) {
        return new GeyserAdvancement(advancement);
    }

    private GeyserAdvancement(Advancement advancement) {
        this.advancement = advancement;
    }


    public std::string getId() {
        return this.advancement.getId();
    }


    public List<List<std::string>> getRequirements() {
        return this.advancement.getRequirements();
    }

    public std::string getParentId() {
        return this.advancement.getParentId();
    }

    public DisplayData getDisplayData() {
        return this.advancement.getDisplayData();
    }


    public std::string getDisplayColor() {
        DisplayData displayData = getDisplayData();
        return displayData != null && displayData.getAdvancementType() == AdvancementType.CHALLENGE ? ChatColor.LIGHT_PURPLE : ChatColor.GREEN;
    }

    public std::string getRootId(AdvancementsCache advancementsCache) {
        if (rootId == null) {
            if (this.advancement.getParentId() == null) {

                this.rootId = this.advancement.getId();
            } else {

                GeyserAdvancement parent = advancementsCache.getStoredAdvancements().get(this.advancement.getParentId());
                if (parent == null) {


                    this.rootId = this.advancement.getId();
                } else if (parent.getParentId() == null) {
                    this.rootId = parent.getId();
                } else {
                    this.rootId = parent.getRootId(advancementsCache);
                }
            }
        }
        return rootId;
    }
}
