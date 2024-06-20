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

package org.geysermc.geyser.inventory.item;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

public enum BedrockEnchantment {
    PROTECTION,
    FIRE_PROTECTION,
    FEATHER_FALLING,
    BLAST_PROTECTION,
    PROJECTILE_PROTECTION,
    THORNS,
    RESPIRATION,
    DEPTH_STRIDER,
    AQUA_AFFINITY,
    SHARPNESS,
    SMITE,
    BANE_OF_ARTHROPODS,
    KNOCKBACK,
    FIRE_ASPECT,
    LOOTING,
    EFFICIENCY,
    SILK_TOUCH,
    UNBREAKING,
    FORTUNE,
    POWER,
    PUNCH,
    FLAME,
    INFINITY,
    LUCK_OF_THE_SEA,
    LURE,
    FROST_WALKER,
    MENDING,
    BINDING_CURSE,
    VANISHING_CURSE,
    IMPALING,
    RIPTIDE,
    LOYALTY,
    CHANNELING,
    MULTISHOT,
    PIERCING,
    QUICK_CHARGE,
    SOUL_SPEED,
    SWIFT_SNEAK,
    WIND_BURST,
    DENSITY,
    BREACH;

    private static final BedrockEnchantment[] VALUES = values();

    private final String javaIdentifier;

    BedrockEnchantment() {
        this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ENGLISH);
    }

    public static @Nullable BedrockEnchantment getByJavaIdentifier(String javaIdentifier) {
        for (BedrockEnchantment enchantment : VALUES) {
            if (enchantment.javaIdentifier.equals(javaIdentifier) || enchantment.name().toLowerCase(Locale.ENGLISH).equalsIgnoreCase(javaIdentifier)) {
                return enchantment;
            }
        }
        return null;
    }

    public static @Nullable BedrockEnchantment getByBedrockId(int bedrockId) {
        if (bedrockId >= 0 && bedrockId < VALUES.length) {
            return VALUES[bedrockId];
        }
        return null;
    }
}
