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

import java.util.List;
import java.util.stream.Stream;

public class Conversion662_649 {
    
    private static final List<String> NEW_MISC = List.of("minecraft:grass_block", "minecraft:vault");
    private static final List<String> NEW_WOODS = List.of("minecraft:oak_wood", "minecraft:spruce_wood", "minecraft:birch_wood", "minecraft:jungle_wood", "minecraft:acacia_wood", "minecraft:dark_oak_wood", "minecraft:stripped_oak_wood", "minecraft:stripped_spruce_wood", "minecraft:stripped_birch_wood", "minecraft:stripped_jungle_wood", "minecraft:stripped_acacia_wood", "minecraft:stripped_dark_oak_wood");
    private static final List<String> NEW_LEAVES = List.of("minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves", "minecraft:jungle_leaves");
    private static final List<String> NEW_LEAVES2 = List.of("minecraft:acacia_leaves", "minecraft:dark_oak_leaves");
    private static final List<String> NEW_SLABS = List.of("minecraft:oak_slab", "minecraft:spruce_slab", "minecraft:birch_slab", "minecraft:jungle_slab", "minecraft:acacia_slab", "minecraft:dark_oak_slab", "minecraft:oak_double_slab", "minecraft:spruce_double_slab", "minecraft:birch_double_slab", "minecraft:jungle_double_slab", "minecraft:acacia_double_slab", "minecraft:dark_oak_double_slab");
    private static final List<String> NEW_BLOCKS = Stream.of(NEW_WOODS, NEW_LEAVES, NEW_LEAVES2, NEW_SLABS, NEW_MISC).flatMap(List::stream).toList();


    static GeyserMappingItem remapItem(@SuppressWarnings("unused") Item item, GeyserMappingItem mapping) {
        mapping = Conversion671_662.remapItem(item, mapping);

        String identifer = mapping.getBedrockIdentifier();

        switch (identifer) {
            case "minecraft:bogged_spawn_egg" -> { return mapping.withBedrockIdentifier("minecraft:creeper_spawn_egg"); }
            case "minecraft:grass_block" -> { return mapping.withBedrockIdentifier("minecraft:grass"); }
            case "minecraft:vault" -> { return mapping.withBedrockIdentifier("minecraft:trial_spawner"); }
            case "minecraft:wind_charge" -> { return mapping.withBedrockIdentifier("minecraft:snowball"); }
        };

        if (NEW_WOODS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:oak_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(0); }
                case "minecraft:spruce_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(1); }
                case "minecraft:birch_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(2); }
                case "minecraft:jungle_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(3); }
                case "minecraft:acacia_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(4); }
                case "minecraft:dark_oak_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(5); }
                case "minecraft:stripped_oak_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(8); }
                case "minecraft:stripped_spruce_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(9); }
                case "minecraft:stripped_birch_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(10); }
                case "minecraft:stripped_jungle_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(11); }
                case "minecraft:stripped_acacia_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(12); }
                case "minecraft:stripped_dark_oak_wood" -> { return mapping.withBedrockIdentifier("minecraft:wood").withBedrockData(13); }
            }
        }

        if (NEW_SLABS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:oak_slab" -> { return mapping.withBedrockIdentifier("minecraft:wooden_slab").withBedrockData(0); }
                case "minecraft:spruce_slab" -> { return mapping.withBedrockIdentifier("minecraft:wooden_slab").withBedrockData(1); }
                case "minecraft:birch_slab" -> { return mapping.withBedrockIdentifier("minecraft:wooden_slab").withBedrockData(2); }
                case "minecraft:jungle_slab" -> { return mapping.withBedrockIdentifier("minecraft:wooden_slab").withBedrockData(3); }
                case "minecraft:acacia_slab" -> { return mapping.withBedrockIdentifier("minecraft:wooden_slab").withBedrockData(4); }
                case "minecraft:dark_oak_slab" -> { return mapping.withBedrockIdentifier("minecraft:wooden_slab").withBedrockData(5); }
            }
        }

        if (NEW_LEAVES.contains(identifer) || NEW_LEAVES2.contains(identifer)) {
            switch (identifer) {
                case "minecraft:oak_leaves" -> { return mapping.withBedrockIdentifier("minecraft:leaves").withBedrockData(0); }
                case "minecraft:spruce_leaves" -> { return mapping.withBedrockIdentifier("minecraft:leaves").withBedrockData(1); }
                case "minecraft:birch_leaves" -> { return mapping.withBedrockIdentifier("minecraft:leaves").withBedrockData(2); }
                case "minecraft:jungle_leaves" -> { return mapping.withBedrockIdentifier("minecraft:leaves").withBedrockData(3); }
                case "minecraft:acacia_leaves" -> { return mapping.withBedrockIdentifier("minecraft:leaves2").withBedrockData(0); }
                case "minecraft:dark_oak_leaves" -> { return mapping.withBedrockIdentifier("minecraft:leaves2").withBedrockData(1); }
            }
        }
        
        return mapping;
    }

    static NbtMap remapBlock(NbtMap tag) {
        tag = Conversion671_662.remapBlock(tag);

        final String name = tag.getString("name");
        
        if (!NEW_BLOCKS.contains(name)) {
            return tag;
        }

        String replacement;

        if (name.equals("minecraft:grass_block")) {
            replacement = "minecraft:grass";

            NbtMapBuilder builder = tag.toBuilder();
            builder.putString("name", replacement);

            return builder.build();
        }

        if (name.equals("minecraft:vault")) {
            replacement = "minecraft:trial_spawner";

            NbtMapBuilder statesBuilder = NbtMap.builder()
                    .putInt("trial_spawner_state", 0);

            NbtMapBuilder builder = tag.toBuilder();
            builder.putString("name", replacement);
            builder.putCompound("states", statesBuilder.build());

            return builder.build();
        }

        if (NEW_WOODS.contains(name)) {
            replacement = "minecraft:wood";

            NbtMap states = tag.getCompound("states");
            boolean stripped = name.startsWith("minecraft:stripped_");
            String woodType = name.replaceAll("minecraft:|_wood|stripped_", "");

            NbtMapBuilder statesBuilder = states.toBuilder()
                    .putString("wood_type", woodType)
                    .putBoolean("stripped_bit", stripped);

            NbtMapBuilder builder = tag.toBuilder()
                    .putString("name", replacement)
                    .putCompound("states", statesBuilder.build());

            return builder.build();
        }

        if (NEW_LEAVES.contains(name) || NEW_LEAVES2.contains(name)) {
            boolean leaves2 = NEW_LEAVES2.contains(name);
            replacement = leaves2 ? "minecraft:leaves2" : "minecraft:leaves";

            NbtMap states = tag.getCompound("states");
            String leafType = name.replaceAll("minecraft:|_leaves", "");

            NbtMapBuilder statesBuilder = states.toBuilder()
                .putString(leaves2 ? "new_leaf_type" : "old_leaf_type", leafType);

            NbtMapBuilder builder = tag.toBuilder()
                .putString("name", replacement)
                .putCompound("states", statesBuilder.build());

            return builder.build();
        }


        if (NEW_SLABS.contains(name)) {
            replacement = name.contains("double") ? "minecraft:double_wooden_slab" : "minecraft:wooden_slab";

            NbtMap states = tag.getCompound("states");
            String woodType = name.replaceAll("minecraft:|_double|_slab", "");

            NbtMapBuilder statesBuilder = states.toBuilder()
                .putString("wood_type", woodType);

            NbtMapBuilder builder = tag.toBuilder()
                .putString("name", replacement)
                .putCompound("states", statesBuilder.build());

            return builder.build();
        }

        return tag;
    }
}
