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

public class Conversion671_662 {
    private static final List<String> NEW_MISC = List.of("minecraft:heavy_core", "minecraft:mace", "minecraft:flow_banner_pattern", "minecraft:guster_banner_pattern", "minecraft:flow_armor_trim_smithing_template", "minecraft:bolt_armor_trim_smithing_template", "minecraft:flow_pottery_sherd", "minecraft:guster_pottery_sherd", "minecraft:scrape_pottery_sherd", "minecraft:breeze_rod");
    private static final List<String> NEW_CORAL_FANS = List.of("minecraft:tube_coral_fan", "minecraft:brain_coral_fan", "minecraft:bubble_coral_fan", "minecraft:fire_coral_fan", "minecraft:horn_coral_fan");
    private static final List<String> NEW_DEAD_CORAL_FANS = List.of("minecraft:dead_tube_coral_fan", "minecraft:dead_brain_coral_fan", "minecraft:dead_bubble_coral_fan", "minecraft:dead_fire_coral_fan", "minecraft:dead_horn_coral_fan");
    private static final List<String> NEW_FLOWERS = List.of("minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley");
    private static final List<String> NEW_SAPLINGS = List.of("minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling", "minecraft:jungle_sapling", "minecraft:acacia_sapling", "minecraft:dark_oak_sapling", "minecraft:bamboo_sapling");
    private static final List<String> NEW_BLOCKS = Stream.of(NEW_MISC, NEW_CORAL_FANS, NEW_DEAD_CORAL_FANS, NEW_FLOWERS, NEW_SAPLINGS).flatMap(List::stream).toList();

    static GeyserMappingItem remapItem(@SuppressWarnings("unused") Item item, GeyserMappingItem mapping) {
        String identifer = mapping.getBedrockIdentifier();

        if (!NEW_BLOCKS.contains(identifer)) {
            return mapping;
        }

        switch (identifer) {
            case "minecraft:bolt_armor_trim_smithing_template" -> { return mapping.withBedrockIdentifier("minecraft:wayfinder_armor_trim_smithing_template"); }
            case "minecraft:breeze_rod" -> { return mapping.withBedrockIdentifier("minecraft:blaze_rod"); }
            case "minecraft:flow_armor_trim_smithing_template" -> { return mapping.withBedrockIdentifier("minecraft:spire_armor_trim_smithing_template"); }
            case "minecraft:flow_banner_pattern", "minecraft:guster_banner_pattern" -> { return mapping.withBedrockIdentifier("minecraft:globe_banner_pattern"); }
            case "minecraft:flow_pottery_sherd" -> { return mapping.withBedrockIdentifier("minecraft:skull_pottery_sherd"); }
            case "minecraft:guster_pottery_sherd" -> { return mapping.withBedrockIdentifier("minecraft:shelter_pottery_sherd"); }
            case "minecraft:scrape_pottery_sherd" -> { return mapping.withBedrockIdentifier("minecraft:heartbreak_pottery_sherd"); }
            case "minecraft:heavy_core" -> { return mapping.withBedrockIdentifier("minecraft:conduit"); }
            case "minecraft:mace" -> { return mapping.withBedrockIdentifier("minecraft:netherite_axe"); }
        }

        if (NEW_FLOWERS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:poppy" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(0); }
                case "minecraft:blue_orchid" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(1); }
                case "minecraft:allium" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(2); }
                case "minecraft:azure_bluet" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(3); }
                case "minecraft:red_tulip" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(4); }
                case "minecraft:orange_tulip" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(5); }
                case "minecraft:white_tulip" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(6); }
                case "minecraft:pink_tulip" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(7); }
                case "minecraft:oxeye_daisy" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(8); }
                case "minecraft:cornflower" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(9); }
                case "minecraft:lily_of_the_valley" -> { return mapping.withBedrockIdentifier("minecraft:red_flower").withBedrockData(10); }
            }
        }

        if (NEW_SAPLINGS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:oak_sapling" -> { return mapping.withBedrockIdentifier("minecraft:sapling").withBedrockData(0); }
                case "minecraft:spruce_sapling" -> { return mapping.withBedrockIdentifier("minecraft:sapling").withBedrockData(1); }
                case "minecraft:birch_sapling" -> { return mapping.withBedrockIdentifier("minecraft:sapling").withBedrockData(2); }
                case "minecraft:jungle_sapling" -> { return mapping.withBedrockIdentifier("minecraft:sapling").withBedrockData(3); }
                case "minecraft:acacia_sapling" -> { return mapping.withBedrockIdentifier("minecraft:sapling").withBedrockData(4); }
                case "minecraft:dark_oak_sapling" -> { return mapping.withBedrockIdentifier("minecraft:sapling").withBedrockData(5); }
            }
        }

        if (NEW_CORAL_FANS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:tube_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan").withBedrockData(0); }
                case "minecraft:brain_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan").withBedrockData(1); }
                case "minecraft:bubble_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan").withBedrockData(2); }
                case "minecraft:fire_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan").withBedrockData(3); }
                case "minecraft:horn_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan").withBedrockData(4); }
            }
        }

        if (NEW_DEAD_CORAL_FANS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:dead_tube_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan_dead").withBedrockData(0); }
                case "minecraft:dead_brain_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan_dead").withBedrockData(1); }
                case "minecraft:dead_bubble_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan_dead").withBedrockData(2); }
                case "minecraft:dead_fire_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan_dead").withBedrockData(3); }
                case "minecraft:dead_horn_coral_fan" -> { return mapping.withBedrockIdentifier("minecraft:coral_fan_dead").withBedrockData(4); }
            }
        }

        return mapping;
    }

    static NbtMap remapBlock(NbtMap tag) {
        final String name = tag.getString("name");
        
        if (!NEW_BLOCKS.contains(name)) {
            return tag;
        }

        if (name.equals("minecraft:bamboo_sapling")) {
            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("sapling_type", "oak")
                .build();

            return tag.toBuilder().putCompound("states", states).build();
        }

        String replacement;

        if (name.equals("minecraft:heavy_core")) {
            replacement = "minecraft:conduit";

            NbtMapBuilder builder = tag.toBuilder();
            builder.putString("name", replacement);

            return builder.build();
        }

        if (NEW_SAPLINGS.contains(name)) {
            replacement = "minecraft:sapling";
            String saplingType = name.replaceAll("minecraft:|_sapling", "");;

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("sapling_type", saplingType)
                .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_FLOWERS.contains(name)) {
            replacement = "minecraft:red_flower";
            String flowerType;

            switch (name) {
                case "minecraft:poppy" -> flowerType = "poppy";
                case "minecraft:blue_orchid" -> flowerType = "orchid";
                case "minecraft:allium" -> flowerType = "allium";
                case "minecraft:azure_bluet" -> flowerType = "houstonia";
                case "minecraft:red_tulip" -> flowerType = "tulip_red";
                case "minecraft:orange_tulip" -> flowerType = "tulip_orange";
                case "minecraft:white_tulip" -> flowerType = "tulip_white";
                case "minecraft:pink_tulip" -> flowerType = "tulip_pink";
                case "minecraft:oxeye_daisy" -> flowerType = "oxeye";
                case "minecraft:cornflower" -> flowerType = "cornflower";
                case "minecraft:lily_of_the_valley" -> flowerType = "lily_of_the_valley";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("flower_type", flowerType)
                .build();
            
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        boolean isLiveCoralFan = NEW_CORAL_FANS.contains(name);
        boolean isDeadCoralFan = NEW_DEAD_CORAL_FANS.contains(name);

        if (isLiveCoralFan || isDeadCoralFan) {
            replacement = isLiveCoralFan ? "minecraft:coral_fan" : "minecraft:coral_fan_dead";
            String coralColor;

            switch (name) {
                case "minecraft:tube_coral_fan", "minecraft:dead_tube_coral_fan" -> coralColor = "blue";
                case "minecraft:brain_coral_fan", "minecraft:dead_brain_coral_fan" -> coralColor = "pink";
                case "minecraft:bubble_coral_fan", "minecraft:dead_bubble_coral_fan" -> coralColor = "purple";
                case "minecraft:fire_coral_fan", "minecraft:dead_fire_coral_fan" -> coralColor = "yellow";
                case "minecraft:horn_coral_fan", "minecraft:dead_horn_coral_fan" -> coralColor = "red";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("coral_color", coralColor)
                .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        return tag;
    }
}