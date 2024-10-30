package org.geysermc.geyser.registry.populator;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.List;
import java.util.stream.Stream;

public class Conversion729_712 {
    private static final List<String> NEW_PURPUR_BLOCKS = List.of("minecraft:purpur_block", "minecraft:purpur_pillar");
    private static final List<String> NEW_WALL_BLOCKS = List.of("minecraft:cobblestone_wall", "minecraft:mossy_cobblestone_wall", "minecraft:granite_wall", "minecraft:diorite_wall", "minecraft:andesite_wall", "minecraft:sandstone_wall", "minecraft:brick_wall", "minecraft:stone_brick_wall", "minecraft:mossy_stone_brick_wall", "minecraft:nether_brick_wall", "minecraft:end_stone_brick_wall", "minecraft:prismarine_wall", "minecraft:red_sandstone_wall", "minecraft:red_nether_brick_wall");
    private static final List<String> NEW_SPONGE_BLOCKS = List.of("minecraft:sponge", "minecraft:wet_sponge");
    private static final List<String> NEW_TNT_BLOCKS = List.of("minecraft:tnt", "minecraft:underwater_tnt");
    private static final List<String> STRUCTURE_VOID = List.of("minecraft:structure_void");
    private static final List<String> NEW_BLOCKS = Stream.of(NEW_PURPUR_BLOCKS, NEW_WALL_BLOCKS, NEW_SPONGE_BLOCKS, NEW_TNT_BLOCKS, STRUCTURE_VOID).flatMap(List::stream).toList();

    static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        mapping = Conversion748_729.remapItem(item, mapping);
        String identifier = mapping.getBedrockIdentifier();

        if (!NEW_BLOCKS.contains(identifier)) {
            return mapping;
        }

        if (identifier.equals("minecraft:underwater_tnt")) {
            return mapping.withBedrockIdentifier("minecraft:tnt").withBedrockData(1);
        }

        if (NEW_PURPUR_BLOCKS.contains(identifier)) {
            switch (identifier) {
                case "minecraft:purpur_block" -> { return mapping.withBedrockIdentifier("minecraft:purpur_block").withBedrockData(0); }
                case "minecraft:purpur_pillar" -> { return mapping.withBedrockIdentifier("minecraft:purpur_block").withBedrockData(1); }
            }
        }

        if (NEW_WALL_BLOCKS.contains(identifier)) {
            switch (identifier) {
                case "minecraft:cobblestone_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(0); }
                case "minecraft:mossy_cobblestone_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(1); }
                case "minecraft:granite_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(2); }
                case "minecraft:diorite_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(3); }
                case "minecraft:andesite_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(4); }
                case "minecraft:sandstone_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(5); }
                case "minecraft:brick_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(6); }
                case "minecraft:stone_brick_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(7); }
                case "minecraft:mossy_stone_brick_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(8); }
                case "minecraft:nether_brick_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(9); }
                case "minecraft:end_stone_brick_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(10); }
                case "minecraft:prismarine_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(11); }
                case "minecraft:red_sandstone_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(12); }
                case "minecraft:red_nether_brick_wall" -> { return mapping.withBedrockIdentifier("minecraft:cobblestone_wall").withBedrockData(13); }
            }
        }

        if (NEW_SPONGE_BLOCKS.contains(identifier)) {
            switch (identifier) {
                case "minecraft:sponge" -> { return mapping.withBedrockIdentifier("minecraft:sponge").withBedrockData(0); }
                case "minecraft:wet_sponge" -> { return mapping.withBedrockIdentifier("minecraft:sponge").withBedrockData(1); }
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

        if (NEW_PURPUR_BLOCKS.contains(name)) {
            replacement = "minecraft:purpur_block";
            String purpurType = name.equals("minecraft:purpur_pillar") ? "lines" : "default";

            NbtMap states = tag.getCompound("states")
                    .toBuilder()
                    .putString("chisel_type", purpurType)
                    .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_WALL_BLOCKS.contains(name)) {
            replacement = "minecraft:cobblestone_wall";
            String wallType;

            switch (name) {
                case "minecraft:cobblestone_wall" -> wallType = "cobblestone";
                case "minecraft:mossy_cobblestone_wall" -> wallType = "mossy_cobblestone";
                case "minecraft:granite_wall" -> wallType = "granite";
                case "minecraft:diorite_wall" -> wallType = "diorite";
                case "minecraft:andesite_wall" -> wallType = "andesite";
                case "minecraft:sandstone_wall" -> wallType = "sandstone";
                case "minecraft:brick_wall" -> wallType = "brick";
                case "minecraft:stone_brick_wall" -> wallType = "stone_brick";
                case "minecraft:mossy_stone_brick_wall" -> wallType = "mossy_stone_brick";
                case "minecraft:nether_brick_wall" -> wallType = "nether_brick";
                case "minecraft:end_stone_brick_wall" -> wallType = "end_brick";
                case "minecraft:prismarine_wall" -> wallType = "prismarine";
                case "minecraft:red_sandstone_wall" -> wallType = "red_sandstone";
                case "minecraft:red_nether_brick_wall" -> wallType = "red_nether_brick";
                default -> throw new IllegalStateException("Unexpected value: " + name);
            }

            NbtMap states = tag.getCompound("states")
                    .toBuilder()
                    .putString("wall_block_type", wallType)
                    .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_SPONGE_BLOCKS.contains(name)) {
            replacement = "minecraft:sponge";
            String spongeType = name.equals("minecraft:wet_sponge") ? "wet" : "dry";

            NbtMap states = tag.getCompound("states")
                    .toBuilder()
                    .putString("sponge_type", spongeType)
                    .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (NEW_TNT_BLOCKS.contains(name)) {
            replacement = "minecraft:tnt";
            byte tntType = (byte) (name.equals("minecraft:underwater_tnt") ? 1 : 0);

            NbtMap states = tag.getCompound("states")
                    .toBuilder()
                    .putByte("allow_underwater_bit", tntType)
                    .build();

            return tag.toBuilder().putString("name", replacement).putCompound("states", states).build();
        }

        if (STRUCTURE_VOID.contains(name)) {
            NbtMap states = tag.getCompound("states")
                .toBuilder()
                .putString("structure_void_type", "air")
                .build();

            return tag.toBuilder().putCompound("states", states).build();
        }

        return tag;
    }
}
