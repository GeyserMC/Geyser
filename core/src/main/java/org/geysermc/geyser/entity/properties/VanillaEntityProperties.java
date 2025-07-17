/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.properties;

import org.geysermc.geyser.entity.type.living.monster.CreakingEntity;

public class VanillaEntityProperties {

    public static final String CLIMATE_VARIANT_ID = "minecraft:climate_variant";

    public static final GeyserEntityProperties ARMADILLO = new GeyserEntityProperties.Builder()
        .addEnum("minecraft:armadillo_state",
            "unrolled",
            "rolled_up",
            "rolled_up_peeking",
            "rolled_up_relaxing",
            "rolled_up_unrolling")
        .build();

    public static final GeyserEntityProperties BEE = new GeyserEntityProperties.Builder()
        .addBoolean("minecraft:has_nectar")
        .build();

    public static final GeyserEntityProperties CLIMATE_VARIANT = new GeyserEntityProperties.Builder()
        .addEnum(CLIMATE_VARIANT_ID,
            "temperate",
            "warm",
            "cold")
        .build();

    public static final GeyserEntityProperties CREAKING = new GeyserEntityProperties.Builder()
        .addEnum(CreakingEntity.CREAKING_STATE,
            "neutral",
            "hostile_observed",
            "hostile_unobserved",
            "twitching",
            "crumbling")
        .addInt(CreakingEntity.CREAKING_SWAYING_TICKS, 0, 6)
        .build();

    public static final GeyserEntityProperties HAPPY_GHAST = new GeyserEntityProperties.Builder()
        .addBoolean("minecraft:can_move")
        .build();

    public static final GeyserEntityProperties WOLF_SOUND_VARIANT = new GeyserEntityProperties.Builder()
        .addEnum("minecraft:sound_variant",
            "default",
            "big",
            "cute",
            "grumpy",
            "mad",
            "puglin",
            "sad")
        .build();
}
