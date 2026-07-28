/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;
import java.util.Set;

/**
 * Bedrock 1.20.40 switched chests/stonecutter from {@code facing_direction} (int)
 * to {@code minecraft:cardinal_direction} (string). Reverse that for 1.20.0–1.20.30 palettes.
 *
 * <p>Furnaces/smokers changed earlier (1.20.30) and are handled in {@link Conversion618_594}.
 */
public final class Conversion622_618 {

    private static final Set<String> CARDINAL_TO_FACING_BLOCKS = Set.of(
        "minecraft:chest",
        "minecraft:trapped_chest",
        "minecraft:ender_chest",
        "minecraft:stonecutter_block"
    );

    private static final Map<String, Integer> CARDINAL_TO_FACING = Map.of(
        "north", 2,
        "south", 3,
        "west", 4,
        "east", 5
    );

    private Conversion622_618() {
    }

    public static NbtMap remapBlock(NbtMap tag) {
        String name = tag.getString("name");
        if (!CARDINAL_TO_FACING_BLOCKS.contains(name)) {
            return tag;
        }

        NbtMap states = tag.getCompound("states");
        if (states.containsKey("facing_direction")) {
            return tag;
        }

        if (!states.containsKey("minecraft:cardinal_direction")) {
            // Fallback for remaps that stripped facing (e.g. vault→chest without states).
            return tag.toBuilder()
                .putCompound("states", states.toBuilder().putInt("facing_direction", 2).build())
                .build();
        }

        String cardinal = states.getString("minecraft:cardinal_direction");
        Integer facing = CARDINAL_TO_FACING.get(cardinal);
        if (facing == null) {
            facing = 2;
        }

        NbtMapBuilder statesBuilder = states.toBuilder();
        statesBuilder.remove("minecraft:cardinal_direction");
        statesBuilder.putInt("facing_direction", facing);
        return tag.toBuilder().putCompound("states", statesBuilder.build()).build();
    }
}
