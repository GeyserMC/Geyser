package org.geysermc.geyser.registry.populator;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.List;
import java.util.stream.Stream;

public class Conversion712_685 {
    private static final List<String> NEW_STONE_BLOCK_SLABS_2 = List.of("minecraft:prismarine_slab", "minecraft:dark_prismarine_slab", "minecraft:smooth_sandstone_slab", "minecraft:purpur_slab", "minecraft:red_nether_brick_slab", "minecraft:prismarine_brick_slab", "minecraft:mossy_cobblestone_slab", "minecraft:red_sandstone_slab");
    private static final List<String> NEW_STONE_BLOCK_SLABS_3 = List.of("minecraft:smooth_red_sandstone_slab", "minecraft:polished_granite_slab", "minecraft:granite_slab", "minecraft:polished_diorite_slab", "minecraft:andesite_slab", "minecraft:polished_andesite_slab", "minecraft:diorite_slab", "minecraft:end_stone_brick_slab");
    private static final List<String> NEW_STONE_BLOCK_SLABS_4 = List.of("minecraft:smooth_quartz_slab", "minecraft:cut_sandstone_slab", "minecraft:cut_red_sandstone_slab", "minecraft:normal_stone_slab", "minecraft:mossy_stone_brick_slab");
    private static final List<String> NEW_DOUBLE_STONE_BLOCK_SLABS = List.of("minecraft:quartz_double_slab", "minecraft:petrified_oak_double_slab", "minecraft:stone_brick_double_slab", "minecraft:brick_double_slab", "minecraft:sandstone_double_slab", "minecraft:nether_brick_double_slab", "minecraft:cobblestone_double_slab", "minecraft:smooth_stone_double_slab");
    private static final List<String> NEW_DOUBLE_STONE_BLOCK_SLABS_2 = List.of("minecraft:prismarine_double_slab", "minecraft:dark_prismarine_double_slab", "minecraft:smooth_sandstone_double_slab", "minecraft:purpur_double_slab", "minecraft:red_nether_brick_double_slab", "minecraft:prismarine_brick_double_slab", "minecraft:mossy_cobblestone_double_slab", "minecraft:red_sandstone_double_slab");
    private static final List<String> NEW_DOUBLE_STONE_BLOCK_SLABS_3 = List.of("minecraft:smooth_red_sandstone_double_slab", "minecraft:polished_granite_double_slab", "minecraft:granite_double_slab", "minecraft:polished_diorite_double_slab", "minecraft:andesite_double_slab", "minecraft:polished_andesite_double_slab", "minecraft:diorite_double_slab", "minecraft:end_stone_brick_double_slab");
    private static final List<String> NEW_DOUBLE_STONE_BLOCK_SLABS_4 = List.of("minecraft:smooth_quartz_double_slab", "minecraft:cut_sandstone_double_slab", "minecraft:cut_red_sandstone_double_slab", "minecraft:normal_stone_double_slab", "minecraft:mossy_stone_brick_double_slab");
    private static final List<String> NEW_PRISMARINE_BLOCKS = List.of("minecraft:prismarine_bricks", "minecraft:dark_prismarine", "minecraft:prismarine");
    private static final List<String> NEW_CORAL_FAN_HANGS = List.of("minecraft:tube_coral_wall_fan", "minecraft:brain_coral_wall_fan", "minecraft:dead_tube_coral_wall_fan", "minecraft:dead_brain_coral_wall_fan");
    private static final List<String> NEW_CORAL_FAN_HANGS_2 = List.of("minecraft:bubble_coral_wall_fan", "minecraft:fire_coral_wall_fan", "minecraft:dead_bubble_coral_wall_fan", "minecraft:dead_fire_coral_wall_fan");
    private static final List<String> NEW_CORAL_FAN_HANGS_3 = List.of("minecraft:horn_coral_wall_fan", "minecraft:dead_horn_coral_wall_fan");
    private static final List<String> NEW_MONSTER_EGGS = List.of("minecraft:infested_cobblestone", "minecraft:infested_stone_bricks", "minecraft:infested_mossy_stone_bricks", "minecraft:infested_cracked_stone_bricks", "minecraft:infested_chiseled_stone_bricks", "minecraft:infested_stone");
    private static final List<String> NEW_STONEBRICK_BLOCKS = List.of("minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks", "minecraft:smooth_stone_bricks", "minecraft:stone_bricks");
    private static final List<String> NEW_LIGHT_BLOCKS = List.of("minecraft:light_block_0", "minecraft:light_block_1", "minecraft:light_block_2", "minecraft:light_block_3", "minecraft:light_block_4", "minecraft:light_block_5", "minecraft:light_block_6", "minecraft:light_block_7", "minecraft:light_block_8", "minecraft:light_block_9", "minecraft:light_block_10", "minecraft:light_block_11", "minecraft:light_block_12", "minecraft:light_block_13", "minecraft:light_block_14", "minecraft:light_block_15");
    private static final List<String> NEW_SANDSTONE_BLOCKS = List.of("minecraft:cut_sandstone", "minecraft:chiseled_sandstone", "minecraft:smooth_sandstone", "minecraft:sandstone");
    private static final List<String> NEW_QUARTZ_BLOCKS = List.of("minecraft:chiseled_quartz_block", "minecraft:quartz_pillar", "minecraft:smooth_quartz", "minecraft:quartz_block");
    private static final List<String> NEW_RED_SANDSTONE_BLOCKS = List.of("minecraft:cut_red_sandstone", "minecraft:chiseled_red_sandstone", "minecraft:smooth_red_sandstone", "minecraft:red_sandstone");
    private static final List<String> NEW_SAND_BLOCKS = List.of("minecraft:red_sand", "minecraft:sand");
    private static final List<String> NEW_DIRT_BLOCKS = List.of("minecraft:coarse_dirt", "minecraft:dirt");
    private static final List<String> NEW_ANVILS = List.of("minecraft:damaged_anvil", "minecraft:chipped_anvil", "minecraft:deprecated_anvil", "minecraft:anvil");
    private static final List<String> NEW_YELLOW_FLOWERS = List.of("minecraft:dandelion");
    private static final List<String> NEW_BLOCKS = Stream.of(NEW_STONE_BLOCK_SLABS_2, NEW_STONE_BLOCK_SLABS_3, NEW_STONE_BLOCK_SLABS_4, NEW_DOUBLE_STONE_BLOCK_SLABS, NEW_DOUBLE_STONE_BLOCK_SLABS_2, NEW_DOUBLE_STONE_BLOCK_SLABS_3, NEW_DOUBLE_STONE_BLOCK_SLABS_4, NEW_PRISMARINE_BLOCKS, NEW_CORAL_FAN_HANGS, NEW_CORAL_FAN_HANGS_2, NEW_CORAL_FAN_HANGS_3, NEW_MONSTER_EGGS, NEW_STONEBRICK_BLOCKS, NEW_LIGHT_BLOCKS, NEW_SANDSTONE_BLOCKS, NEW_QUARTZ_BLOCKS, NEW_RED_SANDSTONE_BLOCKS, NEW_SAND_BLOCKS, NEW_DIRT_BLOCKS, NEW_ANVILS, NEW_YELLOW_FLOWERS).flatMap(List::stream).toList();

    static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        String identifer = mapping.getBedrockIdentifier();

        if (!NEW_BLOCKS.contains(identifer)) {
            return mapping;
        }

        if (identifer.equals("minecraft:coarse_dirt")) {
            return mapping.withBedrockIdentifier("minecraft:dirt").withBedrockData(1);
        }

        if (identifer.equals("minecraft:dandelion")) {
            return mapping.withBedrockIdentifier("minecraft:yellow_flower").withBedrockData(0);
        }

        if (identifer.equals("minecraft:red_sand")) {
            return mapping.withBedrockIdentifier("minecraft:sand").withBedrockData(1);
        }

        if (NEW_PRISMARINE_BLOCKS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:prismarine" -> { return mapping.withBedrockIdentifier("minecraft:prismarine").withBedrockData(0); }
                case "minecraft:dark_prismarine" -> { return mapping.withBedrockIdentifier("minecraft:prismarine").withBedrockData(1); }
                case "minecraft:prismarine_bricks" -> { return mapping.withBedrockIdentifier("minecraft:prismarine").withBedrockData(2); }
            }
        }

        if (NEW_SANDSTONE_BLOCKS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:sandstone" -> { return mapping.withBedrockIdentifier("minecraft:sandstone").withBedrockData(0); }
                case "minecraft:chiseled_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:sandstone").withBedrockData(1); }
                case "minecraft:cut_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:sandstone").withBedrockData(2); }
                case "minecraft:smooth_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:sandstone").withBedrockData(3); }
            }
        }

        if (NEW_RED_SANDSTONE_BLOCKS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:red_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:red_sandstone").withBedrockData(0); }
                case "minecraft:chiseled_red_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:red_sandstone").withBedrockData(1); }
                case "minecraft:cut_red_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:red_sandstone").withBedrockData(2); }
                case "minecraft:smooth_red_sandstone" -> { return mapping.withBedrockIdentifier("minecraft:red_sandstone").withBedrockData(3); }
            }
        }

        if (NEW_QUARTZ_BLOCKS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:quartz_block" -> { return mapping.withBedrockIdentifier("minecraft:quartz_block").withBedrockData(0); }
                case "minecraft:chiseled_quartz_block" -> { return mapping.withBedrockIdentifier("minecraft:quartz_block").withBedrockData(1); }
                case "minecraft:quartz_pillar" -> { return mapping.withBedrockIdentifier("minecraft:quartz_block").withBedrockData(2); }
                case "minecraft:smooth_quartz" -> { return mapping.withBedrockIdentifier("minecraft:quartz_block").withBedrockData(3); }
            }
        }

        if (NEW_STONE_BLOCK_SLABS_2.contains(identifer)) {
            switch (identifer) {
                case "minecraft:red_sandstone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(0); }
                case "minecraft:purpur_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(1); }
                case "minecraft:prismarine_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(2); }
                case "minecraft:dark_prismarine_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(3); }
                case "minecraft:prismarine_brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(4); }
                case "minecraft:mossy_cobblestone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(5); }
                case "minecraft:smooth_sandstone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(6); }
                case "minecraft:red_nether_brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab2").withBedrockData(7); }
            }
        }

        if (NEW_STONE_BLOCK_SLABS_3.contains(identifer)) {
            switch (identifer) {
                case "minecraft:end_stone_brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(0); }
                case "minecraft:smooth_red_sandstone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(1); }
                case "minecraft:polished_andesite_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(2); }
                case "minecraft:andesite_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(3); }
                case "minecraft:diorite_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(4); }
                case "minecraft:polished_diorite_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(5); }
                case "minecraft:granite_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(6); }
                case "minecraft:polished_granite_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab3").withBedrockData(7); }
            }
        }

        if (NEW_STONE_BLOCK_SLABS_4.contains(identifer)) {
            switch (identifer) {
                case "minecraft:mossy_stone_brick_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab4").withBedrockData(0); }
                case "minecraft:smooth_quartz_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab4").withBedrockData(1); }
                case "minecraft:normal_stone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab4").withBedrockData(2); }
                case "minecraft:cut_sandstone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab4").withBedrockData(3); }
                case "minecraft:cut_red_sandstone_slab" -> { return mapping.withBedrockIdentifier("minecraft:stone_block_slab4").withBedrockData(4); }
            }
        }

        if (NEW_MONSTER_EGGS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:infested_stone" -> { return mapping.withBedrockIdentifier("minecraft:monster_egg").withBedrockData(0); }
                case "minecraft:infested_cobblestone" -> { return mapping.withBedrockIdentifier("minecraft:monster_egg").withBedrockData(1); }
                case "minecraft:infested_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:monster_egg").withBedrockData(2); }
                case "minecraft:infested_mossy_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:monster_egg").withBedrockData(3); }
                case "minecraft:infested_cracked_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:monster_egg").withBedrockData(4); }
                case "minecraft:infested_chiseled_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:monster_egg").withBedrockData(5); }
            }
        }

        if (NEW_STONEBRICK_BLOCKS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:stonebrick").withBedrockData(0); }
                case "minecraft:mossy_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:stonebrick").withBedrockData(1); }
                case "minecraft:cracked_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:stonebrick").withBedrockData(2); }
                case "minecraft:chiseled_stone_bricks" -> { return mapping.withBedrockIdentifier("minecraft:stonebrick").withBedrockData(3); }
            }
        }

        if (NEW_ANVILS.contains(identifer)) {
            switch (identifer) {
                case "minecraft:anvil" -> { return mapping.withBedrockIdentifier("minecraft:anvil").withBedrockData(0); }
                case "minecraft:chipped_anvil" -> { return mapping.withBedrockIdentifier("minecraft:anvil").withBedrockData(4); }
                case "minecraft:damaged_anvil" -> { return mapping.withBedrockIdentifier("minecraft:anvil").withBedrockData(8); }
            }
        }

        return mapping;
    }

    static NbtMap remapBlock(NbtMap tag) {
        final String name = tag.getString("name");

        if (!NEW_BLOCKS.contains(name)) {
            return tag;
        }

        String replacement;

        if (NEW_DOUBLE_STONE_BLOCK_SLABS.contains(name)) {
            replacement = "minecraft:double_stone_block_slab";
            String stoneSlabType;

            switch (name) {
                case "minecraft:quartz_double_slab" -> stoneSlabType = "quartz";
                case "minecraft:petrified_oak_double_slab" -> stoneSlabType = "wood";
                case "minecraft:stone_brick_double_slab" -> stoneSlabType = "stone_brick";
                case "minecraft:brick_double_slab" -> stoneSlabType = "brick";
                case "minecraft:sandstone_double_slab" -> stoneSlabType = "sandstone";
                case "minecraft:nether_brick_double_slab" -> stoneSlabType = "nether_brick";
                case "minecraft:cobblestone_double_slab" -> stoneSlabType = "cobblestone";
                case "minecraft:smooth_stone_double_slab" -> stoneSlabType = "smooth_stone";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("stone_slab_type", stoneSlabType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_STONE_BLOCK_SLABS_2.contains(name) || NEW_DOUBLE_STONE_BLOCK_SLABS_2.contains(name)) {
            replacement = NEW_STONE_BLOCK_SLABS_2.contains(name) ? "minecraft:stone_block_slab2" : "minecraft:double_stone_block_slab2";
            String stoneSlabType2;

            switch (name) {
                case "minecraft:prismarine_slab", "minecraft:prismarine_double_slab" -> stoneSlabType2 = "prismarine_rough";
                case "minecraft:dark_prismarine_slab", "minecraft:dark_prismarine_double_slab" -> stoneSlabType2 = "prismarine_dark";
                case "minecraft:smooth_sandstone_slab", "minecraft:smooth_sandstone_double_slab" -> stoneSlabType2 = "smooth_sandstone";
                case "minecraft:purpur_slab", "minecraft:purpur_double_slab" -> stoneSlabType2 = "purpur";
                case "minecraft:red_nether_brick_slab", "minecraft:red_nether_brick_double_slab" -> stoneSlabType2 = "red_nether_brick";
                case "minecraft:prismarine_brick_slab", "minecraft:prismarine_brick_double_slab" -> stoneSlabType2 = "prismarine_brick";
                case "minecraft:mossy_cobblestone_slab", "minecraft:mossy_cobblestone_double_slab" -> stoneSlabType2 = "mossy_cobblestone";
                case "minecraft:red_sandstone_slab", "minecraft:red_sandstone_double_slab" -> stoneSlabType2 = "red_sandstone";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("stone_slab_type_2", stoneSlabType2)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_STONE_BLOCK_SLABS_3.contains(name) || NEW_DOUBLE_STONE_BLOCK_SLABS_3.contains(name)) {
            replacement = NEW_STONE_BLOCK_SLABS_3.contains(name) ? "minecraft:stone_block_slab3" : "minecraft:double_stone_block_slab3";
            String stoneSlabType3;

            switch (name) {
                case "minecraft:smooth_red_sandstone_slab", "minecraft:smooth_red_sandstone_double_slab" -> stoneSlabType3 = "smooth_red_sandstone";
                case "minecraft:polished_granite_slab", "minecraft:polished_granite_double_slab" -> stoneSlabType3 = "polished_granite";
                case "minecraft:granite_slab", "minecraft:granite_double_slab" -> stoneSlabType3 = "granite";
                case "minecraft:polished_diorite_slab", "minecraft:polished_diorite_double_slab" -> stoneSlabType3 = "polished_diorite";
                case "minecraft:andesite_slab", "minecraft:andesite_double_slab" -> stoneSlabType3 = "andesite";
                case "minecraft:polished_andesite_slab", "minecraft:polished_andesite_double_slab" -> stoneSlabType3 = "polished_andesite";
                case "minecraft:diorite_slab", "minecraft:diorite_double_slab" -> stoneSlabType3 = "diorite";
                case "minecraft:end_stone_brick_slab", "minecraft:end_stone_brick_double_slab" -> stoneSlabType3 = "end_stone_brick";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("stone_slab_type_3", stoneSlabType3)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_STONE_BLOCK_SLABS_4.contains(name) || NEW_DOUBLE_STONE_BLOCK_SLABS_4.contains(name)) {
            replacement = NEW_STONE_BLOCK_SLABS_4.contains(name) ? "minecraft:stone_block_slab4" : "minecraft:double_stone_block_slab4";
            String stoneSlabType4;

            switch (name) {
                case "minecraft:smooth_quartz_slab", "minecraft:smooth_quartz_double_slab" -> stoneSlabType4 = "smooth_quartz";
                case "minecraft:cut_sandstone_slab", "minecraft:cut_sandstone_double_slab" -> stoneSlabType4 = "cut_sandstone";
                case "minecraft:cut_red_sandstone_slab", "minecraft:cut_red_sandstone_double_slab" -> stoneSlabType4 = "cut_red_sandstone";
                case "minecraft:normal_stone_slab", "minecraft:normal_stone_double_slab" -> stoneSlabType4 = "stone";
                case "minecraft:mossy_stone_brick_slab", "minecraft:mossy_stone_brick_double_slab" -> stoneSlabType4 = "mossy_stone_brick";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("stone_slab_type_4", stoneSlabType4)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_PRISMARINE_BLOCKS.contains(name)) {
            replacement = "minecraft:prismarine";
            String prismarineBlockType;

            switch (name) {
                case "minecraft:prismarine_bricks" -> prismarineBlockType = "bricks";
                case "minecraft:dark_prismarine" -> prismarineBlockType = "dark";
                case "minecraft:prismarine" -> prismarineBlockType = "default";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("prismarine_block_type", prismarineBlockType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_CORAL_FAN_HANGS.contains(name) || NEW_CORAL_FAN_HANGS_2.contains(name) || NEW_CORAL_FAN_HANGS_3.contains(name)) {
            replacement = NEW_CORAL_FAN_HANGS.contains(name) ? "minecraft:coral_fan_hang" : NEW_CORAL_FAN_HANGS_2.contains(name) ? "minecraft:coral_fan_hang2" : "minecraft:coral_fan_hang3";
            boolean deadBit = name.startsWith("minecraft:dead_");
            boolean coralHangTypeBit = name.contains("brain") || name.contains("fire");

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putBoolean("coral_hang_type_bit", coralHangTypeBit)
                .putBoolean("dead_bit", deadBit)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_MONSTER_EGGS.contains(name)) {
            replacement = "minecraft:monster_egg";
            String monsterEggStoneType;

            switch (name) {
                case "minecraft:infested_cobblestone" -> monsterEggStoneType = "cobblestone";
                case "minecraft:infested_stone_bricks" -> monsterEggStoneType = "stone_brick";
                case "minecraft:infested_mossy_stone_bricks" -> monsterEggStoneType = "mossy_stone_brick";
                case "minecraft:infested_cracked_stone_bricks" -> monsterEggStoneType = "cracked_stone_brick";
                case "minecraft:infested_chiseled_stone_bricks" -> monsterEggStoneType = "chiseled_stone_brick";
                case "minecraft:infested_stone" -> monsterEggStoneType = "stone";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("monster_egg_stone_type", monsterEggStoneType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_STONEBRICK_BLOCKS.contains(name)) {
            replacement = "minecraft:stonebrick";
            String stoneBrickType;

            switch (name) {
                case "minecraft:mossy_stone_bricks" -> stoneBrickType = "mossy";
                case "minecraft:cracked_stone_bricks" -> stoneBrickType = "cracked";
                case "minecraft:chiseled_stone_bricks" -> stoneBrickType = "chiseled";
                case "minecraft:smooth_stone_bricks" -> stoneBrickType = "smooth";
                case "minecraft:stone_bricks" -> stoneBrickType = "default";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("stone_brick_type", stoneBrickType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_LIGHT_BLOCKS.contains(name)) {
            replacement = "minecraft:light_block";
            int blockLightLevel = Integer.parseInt(name.split("_")[2]);

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putInt("block_light_level", blockLightLevel)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_SANDSTONE_BLOCKS.contains(name) || NEW_RED_SANDSTONE_BLOCKS.contains(name)) {
            replacement = NEW_SANDSTONE_BLOCKS.contains(name) ? "minecraft:sandstone" : "minecraft:red_sandstone";
            String sandStoneType;

            switch (name) {
                case "minecraft:cut_sandstone", "minecraft:cut_red_sandstone" -> sandStoneType = "cut";
                case "minecraft:chiseled_sandstone", "minecraft:chiseled_red_sandstone" -> sandStoneType = "heiroglyphs";
                case "minecraft:smooth_sandstone", "minecraft:smooth_red_sandstone" -> sandStoneType = "smooth";
                case "minecraft:sandstone", "minecraft:red_sandstone" -> sandStoneType = "default";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("sand_stone_type", sandStoneType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_QUARTZ_BLOCKS.contains(name)) {
            replacement = "minecraft:quartz_block";
            String chiselType;

            switch (name) {
                case "minecraft:chiseled_quartz_block" -> chiselType = "chiseled";
                case "minecraft:quartz_pillar" -> chiselType = "lines";
                case "minecraft:smooth_quartz" -> chiselType = "smooth";
                case "minecraft:quartz_block" -> chiselType = "default";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("chisel_type", chiselType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_SAND_BLOCKS.contains(name)) {
            replacement = "minecraft:sand";
            String sandType = name.equals("minecraft:red_sand") ? "red" : "normal";

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("sand_type", sandType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_DIRT_BLOCKS.contains(name)) {
            replacement = "minecraft:dirt";
            String dirtType = name.equals("minecraft:coarse_dirt") ? "coarse" : "normal";

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("dirt_type", dirtType)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_ANVILS.contains(name)) {
            replacement = "minecraft:anvil";
            String damage;

            switch (name) {
                case "minecraft:damaged_anvil" -> damage = "broken";
                case "minecraft:chipped_anvil" -> damage = "slightly_damaged";
                case "minecraft:deprecated_anvil" -> damage = "very_damaged";
                case "minecraft:anvil" -> damage = "undamaged";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("damage", damage)
                .build();
        
            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_YELLOW_FLOWERS.contains(name)) {
            replacement = "minecraft:yellow_flower";
            return tag.toBuilder().putString("name", replacement).build();
        }

        return tag;
    }
}
