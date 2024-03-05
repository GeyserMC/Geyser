package org.geysermc.geyser.registry.populator;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.List;

/**
 * Forwards maps the blocks of 1.20.60 (649) to 1.20.70 (662)
 */
public class Conversion649_662 {

    private static final List<String> OLD_BLOCKS = List.of("minecraft:grass", "minecraft:wood", "minecraft:leaves", "minecraft:leaves2", "minecraft:double_wooden_slab", "minecraft:wooden_slab");

    static GeyserMappingItem remapItem(@SuppressWarnings("unused") Item item, GeyserMappingItem mapping) {
        String identifer = mapping.getBedrockIdentifier();

        if (identifer.equals("minecraft:scute")) {
            return mapping.withBedrockIdentifier("minecraft:turtle_scute");
        }

        if (identifer.equals("minecraft:grass")) {
            return mapping.withBedrockIdentifier("minecraft:grass_block");
        }

        if (identifer.equals("minecraft:wood")) {
            int bedrockData = mapping.getBedrockData();

            switch (bedrockData) {
                case 0 -> { return newItemMapping(mapping, "minecraft:oak_wood"); }
                case 1 -> { return newItemMapping(mapping, "minecraft:spruce_wood"); }
                case 2 -> { return newItemMapping(mapping, "minecraft:birch_wood"); }
                case 3 -> { return newItemMapping(mapping, "minecraft:jungle_wood"); }
                case 4 -> { return newItemMapping(mapping, "minecraft:acacia_wood"); }
                case 5 -> { return newItemMapping(mapping, "minecraft:dark_oak_wood"); }
                case 8 -> { return newItemMapping(mapping, "minecraft:stripped_oak_wood"); }
                case 9 -> { return newItemMapping(mapping, "minecraft:stripped_spruce_wood"); }
                case 10 -> { return newItemMapping(mapping, "minecraft:stripped_birch_wood"); }
                case 11 -> { return newItemMapping(mapping, "minecraft:stripped_jungle_wood"); }
                case 12 -> { return newItemMapping(mapping, "minecraft:stripped_acacia_wood"); }
                case 13 -> { return newItemMapping(mapping, "minecraft:stripped_dark_oak_wood"); }
            }
        }

        if (identifer.equals("minecraft:leaves")) {
            int bedrockData = mapping.getBedrockData();

            switch (bedrockData) {
                case 0 -> { return newItemMapping(mapping, "minecraft:oak_leaves"); }
                case 1 -> { return newItemMapping(mapping, "minecraft:spruce_leaves"); }
                case 2 -> { return newItemMapping(mapping, "minecraft:birch_leaves"); }
                case 3 -> { return newItemMapping(mapping, "minecraft:jungle_leaves"); }
            }
        }

        if (identifer.equals("minecraft:leaves2")) {
            int bedrockData = mapping.getBedrockData();

            switch (bedrockData) {
                case 0 -> { return newItemMapping(mapping, "minecraft:acacia_leaves"); }
                case 1 -> { return newItemMapping(mapping, "minecraft:dark_oak_leaves"); }
            }
        }

        return mapping;
    }

    static NbtMap remapBlock(NbtMap tag) {
        final String name = tag.getString("name");
        
        if (!OLD_BLOCKS.contains(name)) {
            return tag;
        }

        String replacement;

        if (name.equals("minecraft:grass")) {
            replacement = "minecraft:grass_block";

            NbtMapBuilder builder = tag.toBuilder();
            builder.putString("name", replacement);

            return builder.build();
        }

        if (name.equals("minecraft:wood")) {
            NbtMap states = tag.getCompound("states");
            boolean stripped = states.getBoolean("stripped_bit");
            String woodType = states.getString("wood_type");

            NbtMapBuilder builder = tag.toBuilder();
            NbtMapBuilder statesBuilder = states.toBuilder();

            if (stripped) {
                replacement = "minecraft:stripped_" + woodType + "_wood";
            } else {
                replacement = "minecraft:" + woodType + "_wood";
            }

            statesBuilder.remove("wood_type");
            statesBuilder.remove("stripped_bit");
            builder.putString("name", replacement);
            builder.putCompound("states", statesBuilder.build());

            return builder.build();
        }

        if (name.equals("minecraft:leaves") || name.equals("minecraft:leaves2")) {
            NbtMap states = tag.getCompound("states");

            NbtMapBuilder builder = tag.toBuilder();
            NbtMapBuilder statesBuilder = states.toBuilder();

            if (name.equals("minecraft:leaves")) {
                replacement = "minecraft:" + states.getString("old_leaf_type") + "_leaves";
                statesBuilder.remove("old_leaf_type");
            } else {
                replacement = "minecraft:" + states.getString("new_leaf_type") + "_leaves";
                statesBuilder.remove("new_leaf_type");
            }

            builder.putString("name", replacement);
            builder.putCompound("states", statesBuilder.build());

            return builder.build();
        }

        if (name.equals("minecraft:double_wooden_slab") || name.equals("minecraft:wooden_slab")) {
            NbtMap states = tag.getCompound("states");
            String woodType = states.getString("wood_type");

            NbtMapBuilder builder = tag.toBuilder();
            NbtMapBuilder statesBuilder = states.toBuilder();

            if (name.equals("minecraft:double_wooden_slab")) {
                replacement = "minecraft:" + woodType + "_double_slab";
            } else {
                replacement = "minecraft:" + woodType + "_slab";
            }

            statesBuilder.remove("wood_type");
            builder.putString("name", replacement);
            builder.putCompound("states", statesBuilder.build());

            return builder.build();
        }

        return tag;
    }

    private static GeyserMappingItem newItemMapping(GeyserMappingItem mapping, String identifier) {
        return mapping.withBedrockData(0).withBedrockIdentifier(identifier);
    }
}
