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
import org.cloudburstmc.nbt.NbtMapBuilder;

public class Conversion776_766 {

    public static NbtMap remapBlock(NbtMap tag) {

        // First: Downgrade from 1.21.70
        tag = Conversion786_776.remapBlock(tag);

        final String name = tag.getString("name");

        if (name.equals("minecraft:creaking_heart")) {
            NbtMapBuilder builder = tag.getCompound("states").toBuilder();
            String value = (String) builder.remove("creaking_heart_state");
            builder.putBoolean("active", value.equals("awake"));

            return tag.toBuilder().putCompound("states", builder.build()).build();
        }

        if (name.endsWith("_door") || name.endsWith("fence_gate")) {
            NbtMapBuilder builder = tag.getCompound("states").toBuilder();
            String cardinalDirection = (String) builder.remove("minecraft:cardinal_direction");
            switch (cardinalDirection) {
                case "south" -> builder.putInt("direction", 0);
                case "west" -> builder.putInt("direction", 1);
                case "east" -> builder.putInt("direction", 3);
                case "north" -> builder.putInt("direction", 2);
                default -> throw new AssertionError("Invalid direction: " + cardinalDirection);
            }
            NbtMap states = builder.build();
            return tag.toBuilder().putCompound("states", states).build();
        }

        return tag;
    }

}
