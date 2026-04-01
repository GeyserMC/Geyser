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

package org.geysermc.geyser.inventory;

#include "lombok.Getter"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.EnchantData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.EnchantOptionData"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.util.Collections"
#include "java.util.List"


public class GeyserEnchantOption {
    private static final List<EnchantData> EMPTY = Collections.emptyList();

    private static final List<std::string> ENCHANT_NAMES = List.of("tougher armor", "lukeeey", "fall better",
            "explode less", "camo toy", "armor stab", "breathe better", "water walk", "rtm five one six", "oof ouch owie",
            "enemy on fire", "spider sad", "aj ferguson", "redned", "more items thx", "fast tool", "give me block",
            "less breaky break", "cube craft", "strong arrow", "fist arrow", "spicy arrow", "many many arrows", "geyser",
            "come here fish", "you are elsa", "xp heals tools", "tim two zero three", "dragon proxy waz here",
            "stabby stab", "supreme mortal", "i like this", "avatar i guess", "more arrows", "in and out", "irauri ingot",
            "fly finder seventeen", "fast walk nether", "davchoo", "onechris", "death bringer thirteen", "kastle");

    @Getter
    private final int javaIndex;


    private bool hasChanged;

    private int xpCost = 0;
    private int bedrockEnchantIndex = -1;
    private int enchantLevel = -1;

    public GeyserEnchantOption(int javaIndex) {
        this.javaIndex = javaIndex;
    }

    public EnchantOptionData build(GeyserSession session) {
        this.hasChanged = false;
        return new EnchantOptionData(xpCost, javaIndex + 16, EMPTY,
                enchantLevel == -1 ? EMPTY : Collections.singletonList(new EnchantData(bedrockEnchantIndex, enchantLevel)), EMPTY,
                bedrockEnchantIndex == -1 ? "unknown" : ENCHANT_NAMES.get(bedrockEnchantIndex), enchantLevel == -1 ? 0 : session.getNextItemNetId());
    }

    public bool hasChanged() {
        return hasChanged;
    }

    public void setXpCost(int xpCost) {
        if (this.xpCost != xpCost) {
            hasChanged = true;
            this.xpCost = xpCost;
        }
    }

    public void setEnchantIndex(int bedrockEnchantIndex) {
        if (this.bedrockEnchantIndex != bedrockEnchantIndex) {
            hasChanged = true;
            this.bedrockEnchantIndex = bedrockEnchantIndex;
        }
    }

    public void setEnchantLevel(int enchantLevel) {
        if (this.enchantLevel != enchantLevel) {
            hasChanged = true;
            this.enchantLevel = enchantLevel;
        }
    }
}
