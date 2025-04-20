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

import static org.geysermc.geyser.registry.populator.conversion.ConversionHelper.withName;
import static org.geysermc.geyser.registry.populator.conversion.ConversionHelper.withoutStates;

public class Conversion786_776 {

    public static NbtMap remapBlock(NbtMap nbtMap) {

        final String name = nbtMap.getString("name");
        if (name.equals("minecraft:bush")) {
            return withName(nbtMap, "fern");
        }

        if (name.equals("minecraft:firefly_bush")) {
            return withName(nbtMap, "deadbush");
        }

        if (name.equals("minecraft:tall_dry_grass") || name.equals("minecraft:short_dry_grass")) {
            return withName(nbtMap, "short_grass");
        }

        if (name.equals("minecraft:cactus_flower")) {
            return withName(nbtMap, "unknown");
        }

        if (name.equals("minecraft:leaf_litter") || name.equals("minecraft:wildflowers")) {
            return withoutStates("unknown");
        }

        return nbtMap;
    }
}
