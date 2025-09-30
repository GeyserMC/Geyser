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
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

public class Conversion844_827 {

    public static NbtMap remapBlock(NbtMap nbtMap) {
        final String name = nbtMap.getString("name");
        if (name.equals("minecraft:iron_chain")) {
            return ConversionHelper.withName(nbtMap, "chain");
        } else if (name.equals("minecraft:lightning_rod")) {
            NbtMapBuilder statesWithoutPoweredBit = nbtMap.getCompound("states").toBuilder();
            statesWithoutPoweredBit.remove("powered_bit");
            return nbtMap.toBuilder()
                .putCompound("states", statesWithoutPoweredBit.build())
                .build();
        }

        return nbtMap;
    }

    public static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        if (item == Items.CHAIN) {
            return mapping.withBedrockIdentifier("minecraft:chain");
        }
        return mapping;
    }
}
