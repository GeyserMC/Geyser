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

package org.geysermc.geyser.registry.populator.conversion;

import org.cloudburstmc.nbt.NbtMap;

/**
 * Remaps content introduced after Bedrock 1.21.60 (protocol 776) for older clients.
 */
public final class Conversion786_776 {

    private Conversion786_776() {
    }

    public static NbtMap remapBlock(NbtMap nbtMap) {
        final String name = nbtMap.getString("name");
        if (name.equals("minecraft:bush")) {
            return ConversionHelper.withName(nbtMap, "fern");
        }

        if (name.equals("minecraft:firefly_bush")) {
            return ConversionHelper.withName(nbtMap, "deadbush");
        }

        if (name.equals("minecraft:tall_dry_grass") || name.equals("minecraft:short_dry_grass")) {
            return ConversionHelper.withName(nbtMap, "short_grass");
        }

        if (name.equals("minecraft:cactus_flower")) {
            // Cactus always requires an age state in Bedrock palettes (including 1.20.0).
            return NbtMap.builder()
                .putString("name", "minecraft:cactus")
                .putCompound("states", NbtMap.builder().putInt("age", 0).build())
                .build();
        }

        if (name.equals("minecraft:leaf_litter")) {
            // Same cardinal_direction + growth shape as pink_petals. Keep states so palette
            // lookup succeeds; Conversion618_594 rewrites cardinal→direction for 1.20.0–1.20.10.
            return ConversionHelper.withName(nbtMap, "pink_petals");
        }

        if (name.equals("minecraft:wildflowers")) {
            return ConversionHelper.withoutStates("minecraft:oxeye_daisy");
        }

        return nbtMap;
    }
}
