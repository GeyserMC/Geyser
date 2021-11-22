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

package org.geysermc.geyser.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.EnchantData;
import com.nukkitx.protocol.bedrock.data.inventory.EnchantOptionData;
import lombok.Getter;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A mutable "wrapper" around {@link EnchantOptionData}
 */
public class GeyserEnchantOption {
    private static final List<EnchantData> EMPTY = Collections.emptyList();
    /**
     * This: https://cdn.discordapp.com/attachments/613168850925649981/791030657169227816/unknown.png
     * is controlled by the server.
     * So, of course, we have to throw in some easter eggs. ;)
     */
    private static final List<String> ENCHANT_NAMES = Arrays.asList("tougher armor", "lukeeey", "fall better",
            "explode less", "camo toy", "breathe better", "rtm five one six", "armor stab", "water walk", "you are elsa",
            "tim two zero three", "fast walk nether", "oof ouch owie", "enemy on fire", "spider sad", "aj ferguson", "redned",
            "more items thx", "long sword reach", "fast tool", "give me block", "less breaky break", "cube craft",
            "strong arrow", "fist arrow", "spicy arrow", "many many arrows", "geyser", "come here fish", "i like this",
            "stabby stab", "supreme mortal", "avatar i guess", "more arrows", "fly finder seventeen", "in and out",
            "xp heals tools", "dragon proxy waz here");

    @Getter
    private final int javaIndex;

    /**
     * Whether the enchantment details have actually changed.
     * Used to mitigate weird packet spamming pre-1.14, causing the net ID to always update.
     */
    private boolean hasChanged;

    private int xpCost = 0;
    private int javaEnchantIndex = -1;
    private int bedrockEnchantIndex = -1;
    private int enchantLevel = -1;

    public GeyserEnchantOption(int javaIndex) {
        this.javaIndex = javaIndex;
    }

    public EnchantOptionData build(GeyserSession session) {
        this.hasChanged = false;
        return new EnchantOptionData(xpCost, javaIndex + 16, EMPTY,
                enchantLevel == -1 ? EMPTY : Collections.singletonList(new EnchantData(bedrockEnchantIndex, enchantLevel)), EMPTY,
                javaEnchantIndex == -1 ? "unknown" : ENCHANT_NAMES.get(javaEnchantIndex), enchantLevel == -1 ? 0 : session.getNextItemNetId());
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public void setXpCost(int xpCost) {
        if (this.xpCost != xpCost) {
            hasChanged = true;
            this.xpCost = xpCost;
        }
    }

    public void setEnchantIndex(int javaEnchantIndex, int bedrockEnchantIndex) {
        if (this.javaEnchantIndex != javaEnchantIndex) {
            hasChanged = true;
            this.javaEnchantIndex = javaEnchantIndex;
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
