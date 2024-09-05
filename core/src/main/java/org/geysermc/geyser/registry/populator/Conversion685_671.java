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
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.List;
import java.util.stream.Stream;

public class Conversion685_671 {
    private static final List<String> NEW_CORAL_BLOCKS = List.of("minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block", "minecraft:fire_coral_block", "minecraft:horn_coral_block", "minecraft:dead_tube_coral_block", "minecraft:dead_brain_coral_block", "minecraft:dead_bubble_coral_block", "minecraft:dead_fire_coral_block", "minecraft:dead_horn_coral_block");
    private static final List<String> NEW_DOUBLE_PLANTS = List.of("minecraft:sunflower", "minecraft:lilac", "minecraft:tall_grass", "minecraft:large_fern", "minecraft:rose_bush", "minecraft:peony");
    private static final List<String> NEW_STONE_BLOCK_SLABS = List.of("minecraft:smooth_stone_slab", "minecraft:sandstone_slab", "minecraft:petrified_oak_slab", "minecraft:cobblestone_slab", "minecraft:brick_slab", "minecraft:stone_brick_slab", "minecraft:quartz_slab", "minecraft:nether_brick_slab");
    private static final List<String> NEW_TALLGRASSES = List.of("minecraft:fern", "minecraft:short_grass");
    private static final List<String> OMINOUS_BLOCKS = List.of("minecraft:trial_spawner", "minecraft:vault");
    private static final List<String> NEW_BLOCKS = Stream.of(NEW_CORAL_BLOCKS, NEW_DOUBLE_PLANTS, NEW_STONE_BLOCK_SLABS, NEW_TALLGRASSES).flatMap(List::stream).toList();
    private static final List<String> MODIFIED_BLOCKS = Stream.of(NEW_BLOCKS, OMINOUS_BLOCKS).flatMap(List::stream).toList();
    private static final List<Item> NEW_MUSIC_DISCS = List.of(Items.MUSIC_DISC_CREATOR, Items.MUSIC_DISC_CREATOR_MUSIC_BOX, Items.MUSIC_DISC_PRECIPICE);

    static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        mapping = Conversion712_685.remapItem(item, mapping);

        String identifer = mapping.getBedrockIdentifier();

        if (NEW_MUSIC_DISCS.contains(item)) {
            return mapping.withBedrockIdentifier("minecraft:music_disc_otherside");
        }
        if (item == Items.OMINOUS_TRIAL_KEY) {
            return mapping.withBedrockIdentifier("minecraft:trial_key");
        }
        if (item == Items.OMINOUS_BOTTLE) {
            return mapping.withBedrockIdentifier("minecraft:glass_bottle");
        }

        if (!NEW_BLOCKS.contains(identifer)) {
            return mapping;
        }

        if (NEW_CORAL_BLOCKS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:tube_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(0); }
                case "minecraft:brain_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(1); }
                case "minecraft:bubble_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(2); }
                case "minecraft:fire_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(3); }
                case "minecraft:horn_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(4); }
                case "minecraft:dead_tube_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(8); }
                case "minecraft:dead_brain_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(9); }
                case "minecraft:dead_bubble_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(10); }
                case "minecraft:dead_fire_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(11); }
                case "minecraft:dead_horn_coral_block" -> { return mapping.withBedrockIdentifier("minecraft:coral_block").withBedrockData(12); }
            }
        }

        if (NEW_DOUBLE_PLANTS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:sunflower" -> { return mapping.withBedrockIdentifier("minecraft:double_plant").withBedrockData(0); }
                case "minecraft:lilac" -> { return mapping.withBedrockIdentifier("minecraft:double_plant").withBedrockData(1); }
                case "minecraft:tall_grass" -> { return mapping.withBedrockIdentifier("minecraft:double_plant").withBedrockData(2); }
                case "minecraft:large_fern" -> { return mapping.withBedrockIdentifier("minecraft:double_plant").withBedrockData(3); }
                case "minecraft:rose_bush" -> { return mapping.withBedrockIdentifier("minecraft:double_plant").withBedrockData(4); }
                case "minecraft:peony" -> { return mapping.withBedrockIdentifier("minecraft:double_plant").withBedrockData(5); }
            }
        }

        if (NEW_STONE_BLOCK_SLABS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:smooth_stone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(0); }
                case "minecraft:sandstone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(1); }
                case "minecraft:petrified_oak_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(2); }
                case "minecraft:cobblestone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(3); }
                case "minecraft:brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(4); }
                case "minecraft:stone_brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(5); }
                case "minecraft:quartz_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(6); }
                case "minecraft:nether_brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab").withBedrockData(7); }
            }
        }

        if (NEW_TALLGRASSES.contains(identifer)) {
            switch (identifer) {
                case "minecraft:short_grass" -> { return mapping.withBedrockIdentifier("minecraft:tallgrass").withBedrockData(1); }
                case "minecraft:fern" -> { return mapping.withBedrockIdentifier("minecraft:tallgrass").withBedrockData(2); }
            }
        }

        return mapping;
    }

    static NbtMap remapBlock(NbtMap tag) {
        tag = Conversion712_685.remapBlock(tag);

        final String name = tag.getString("name");
        
        if (!MODIFIED_BLOCKS.contains(name)) {
            return tag;
        }

        if (OMINOUS_BLOCKS.contains(name)) {
            NbtMapBuilder builder = tag.getCompound("states").toBuilder();
            builder.remove("ominous");
            return tag.toBuilder().putCompound("states", builder.build()).build();
        }

        String replacement;

        if (NEW_CORAL_BLOCKS.contains(name)) {
            replacement = "minecraft:coral_block";
            String coralColor;
            boolean deadBit = name.startsWith("minecraft:dead_");

            switch (name) {
                case "minecraft:tube_coral_block", "minecraft:dead_tube_coral_block" -> coralColor = "blue";
                case "minecraft:brain_coral_block", "minecraft:dead_brain_coral_block" -> coralColor = "pink";
                case "minecraft:bubble_coral_block", "minecraft:dead_bubble_coral_block" -> coralColor = "purple";
                case "minecraft:fire_coral_block", "minecraft:dead_fire_coral_block" -> coralColor = "yellow";
                case "minecraft:horn_coral_block", "minecraft:dead_horn_coral_block" -> coralColor = "red";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("coral_color", coralColor)
                .putBoolean("dead_bit", deadBit)
                .build();
            
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_DOUBLE_PLANTS.contains(name)) {
            replacement = "minecraft:double_plant";
            String doublePlantType;

            switch (name) {
                case "minecraft:sunflower" -> doublePlantType = "sunflower";
                case "minecraft:lilac" -> doublePlantType = "syringa";
                case "minecraft:tall_grass" -> doublePlantType = "grass";
                case "minecraft:large_fern" -> doublePlantType = "fern";
                case "minecraft:rose_bush" -> doublePlantType = "rose";
                case "minecraft:peony" -> doublePlantType = "paeonia";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("double_plant_type", doublePlantType)
                .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_STONE_BLOCK_SLABS.contains(name)) {
            replacement = "minecraft:stone_block_slab";
            String stoneSlabType;

            switch (name) {
                case "minecraft:smooth_stone_slab" -> stoneSlabType = "smooth_stone";
                case "minecraft:sandstone_slab" -> stoneSlabType = "sandstone";
                case "minecraft:petrified_oak_slab" -> stoneSlabType = "wood";
                case "minecraft:cobblestone_slab" -> stoneSlabType = "cobblestone";
                case "minecraft:brick_slab" -> stoneSlabType = "brick";
                case "minecraft:stone_brick_slab" -> stoneSlabType = "stone_brick";
                case "minecraft:quartz_slab" -> stoneSlabType = "quartz";
                case "minecraft:nether_brick_slab" -> stoneSlabType = "nether_brick";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("stone_slab_type", stoneSlabType)
                .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_TALLGRASSES.contains(name)) {
            replacement = "minecraft:tallgrass";
            String tallGrassType;

            switch (name) {
                case "minecraft:short_grass" -> tallGrassType = "tall";
                case "minecraft:fern" -> tallGrassType = "fern";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("tall_grass_type", tallGrassType)
                .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        return tag;
    }
}
