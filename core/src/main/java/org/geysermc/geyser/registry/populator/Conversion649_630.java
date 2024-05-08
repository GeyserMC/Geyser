/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.registry.populator;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

public class Conversion649_630 {
    
        static GeyserMappingItem remapItem(@SuppressWarnings("unused") Item item, GeyserMappingItem mapping) {
            mapping = Conversion662_649.remapItem(item, mapping);

            String identifer = mapping.getBedrockIdentifier();

            switch (identifer) {
                case "minecraft:armadillo_scute", "minecraft:turtle_scute" -> { return mapping.withBedrockIdentifier("minecraft:scute"); }
                case "minecraft:armadillo_spawn_egg" -> { return mapping.withBedrockIdentifier("minecraft:rabbit_spawn_egg"); }
                case "minecraft:trial_spawner" -> { return mapping.withBedrockIdentifier("minecraft:mob_spawner"); }
                case "minecraft:trial_key" -> { return mapping.withBedrockIdentifier("minecraft:echo_shard"); }
                case "minecraft:wolf_armor" -> { return mapping.withBedrockIdentifier("minecraft:leather_horse_armor"); }
                default -> { return mapping; }
            }
        }
    
        static NbtMap remapBlock(NbtMap tag) {
            tag = Conversion662_649.remapBlock(tag);

            final String name = tag.getString("name");
    
            if (name.equals("minecraft:trial_spawner")) {
                NbtMapBuilder builder = tag.toBuilder()
                    .putString("name", "minecraft:mob_spawner")
                    .putCompound("states", NbtMap.EMPTY);
    
                return builder.build();
            }
    
            return tag;
        }
}
