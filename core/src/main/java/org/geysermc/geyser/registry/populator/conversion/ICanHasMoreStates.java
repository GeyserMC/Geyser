/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
 * I'm so original
 */
public class ICanHasMoreStates implements BlockRemapper {
    public static final ICanHasMoreStates INSTANCE = new ICanHasMoreStates();

    @Override
    public NbtMap remapBlock(NbtMap tag) {
        String name = tag.getString("name");
        return switch (name) {
            case "minecraft:fire" -> ConversionHelper.removeStates(tag,
                "minecraft:connection_north", "minecraft:connection_east",
                "minecraft:connection_south", "minecraft:connection_west", "minecraft:connection_up");
            case "minecraft:chorus_plant" -> ConversionHelper.removeStates(tag,
                "minecraft:connection_north", "minecraft:connection_east",
                "minecraft:connection_south", "minecraft:connection_west", "minecraft:connection_up",
                "minecraft:connection_down");
            case "minecraft:redstone_wire" -> ConversionHelper.removeStates(tag, "redstone_north",
                "redstone_east", "redstone_south", "redstone_west");
            default -> tag;
        };
    }
}
