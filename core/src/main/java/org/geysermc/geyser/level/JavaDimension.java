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

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.util.DimensionUtils;

/**
 * Represents the information we store from the current Java dimension
 * @param piglinSafe Whether piglins and hoglins are safe from conversion in this dimension.
 *      This controls if they have the shaking effect applied in the dimension.
 * @param ultrawarm If this dimension is ultrawarm.
 *      Used when calculating movement in lava for client-side vehicles.
 * @param bedrockId the Bedrock dimension ID of this dimension.
 * As a Java dimension can be null in some login cases (e.g. GeyserConnect), make sure the player
 * is logged in before utilizing this field.
 */
public record JavaDimension(int minY, int height, boolean piglinSafe, boolean ultrawarm, int bedrockId, boolean isNetherLike) {

    public static JavaDimension read(RegistryEntryContext entry) {
        NbtMap dimension = entry.data();
        int minY = dimension.getInt("min_y");
        int height = dimension.getInt("height");
        // Logical height can be ignored probably - seems to be for artificial limits like the Nether.

        // Set if piglins/hoglins should shake
        boolean piglinSafe = dimension.getBoolean("piglin_safe");
        // Entities in lava move faster in ultrawarm dimensions
        boolean ultrawarm = dimension.getBoolean("ultrawarm");

        boolean isNetherLike;
        // Cache the Bedrock version of this dimension, and base it off the ID - THE ID CAN CHANGE!!!
        // https://github.com/GeyserMC/Geyser/issues/4837
        int bedrockId;
        Key id = entry.id();
        if ("minecraft".equals(id.namespace())) {
            String identifier = id.asString();
            bedrockId = DimensionUtils.javaToBedrock(identifier);
            isNetherLike = BedrockDimension.NETHER_IDENTIFIER.equals(identifier);
        } else {
            // Effects should give is a clue on how this (custom) dimension is supposed to look like
            String effects = dimension.getString("effects");
            bedrockId = DimensionUtils.javaToBedrock(effects);
            isNetherLike = BedrockDimension.NETHER_IDENTIFIER.equals(effects);
        }

        if (minY % 16 != 0) {
            throw new RuntimeException("Minimum Y must be a multiple of 16!");
        }
        if (height % 16 != 0) {
            throw new RuntimeException("Height must be a multiple of 16!");
        }

        return new JavaDimension(minY, height, piglinSafe, ultrawarm, bedrockId, isNetherLike);
    }
}
