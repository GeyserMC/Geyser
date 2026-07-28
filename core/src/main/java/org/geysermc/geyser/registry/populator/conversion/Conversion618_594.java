/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Downgrades modern flattened color blocks / state names to Bedrock 1.20.0–1.20.10 palettes.
 *
 * <p>Reverse of Cloudburst {@code BlockStateUpdater_1_20_30}:
 * color flatten (glass/pane/powder/terracotta), direction→cardinal, facing→block_face,
 * and slab {@code minecraft:vertical_half}→{@code top_slot_bit}.
 *
 * <p>Also reverses furnace/smoker cardinal→{@code facing_direction} (changed in 1.20.30,
 * not 1.20.40 like chests).
 */
public final class Conversion618_594 {

    private static final Set<String> CARDINAL_TO_DIRECTION_BLOCKS = Set.of(
        "minecraft:powered_repeater",
        "minecraft:unpowered_repeater",
        "minecraft:powered_comparator",
        "minecraft:unpowered_comparator",
        "minecraft:campfire",
        "minecraft:soul_campfire",
        "minecraft:lectern",
        "minecraft:anvil",
        "minecraft:end_portal_frame",
        "minecraft:big_dripleaf",
        "minecraft:small_dripleaf_block",
        "minecraft:pink_petals",
        "minecraft:calibrated_sculk_sensor"
    );

    private static final Set<String> CARDINAL_TO_FACING_BLOCKS = Set.of(
        "minecraft:furnace",
        "minecraft:lit_furnace",
        "minecraft:blast_furnace",
        "minecraft:lit_blast_furnace",
        "minecraft:smoker",
        "minecraft:lit_smoker"
    );

    private static final Set<String> BLOCK_FACE_TO_FACING_BLOCKS = Set.of(
        "minecraft:amethyst_cluster",
        "minecraft:medium_amethyst_bud",
        "minecraft:large_amethyst_bud",
        "minecraft:small_amethyst_bud"
    );

    private static final Map<String, Integer> CARDINAL_TO_DIRECTION = Map.of(
        "south", 0,
        "west", 1,
        "north", 2,
        "east", 3
    );

    private static final Map<String, Integer> CARDINAL_TO_FACING = Map.of(
        "north", 2,
        "south", 3,
        "west", 4,
        "east", 5
    );

    private static final Map<String, Integer> BLOCK_FACE_TO_FACING = Map.of(
        "down", 0,
        "up", 1,
        "north", 2,
        "south", 3,
        "west", 4,
        "east", 5
    );

    /**
     * Modern flattened id suffix → legacy meta block id.
     * {@code light_gray} becomes Bedrock meta color {@code silver}.
     */
    private static final Map<String, String> FLATTENED_COLOR_SUFFIX_TO_LEGACY = new LinkedHashMap<>();

    static {
        FLATTENED_COLOR_SUFFIX_TO_LEGACY.put("_stained_glass", "minecraft:stained_glass");
        FLATTENED_COLOR_SUFFIX_TO_LEGACY.put("_stained_glass_pane", "minecraft:stained_glass_pane");
        FLATTENED_COLOR_SUFFIX_TO_LEGACY.put("_concrete_powder", "minecraft:concrete_powder");
        FLATTENED_COLOR_SUFFIX_TO_LEGACY.put("_terracotta", "minecraft:stained_hardened_clay");
    }

    private static final Map<String, String> COLOR_TO_LEGACY = new LinkedHashMap<>();
    private static final Map<String, Integer> COLOR_TO_DATA = new LinkedHashMap<>();

    static {
        String[] colors = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
        };
        for (int i = 0; i < colors.length; i++) {
            String color = colors[i];
            COLOR_TO_DATA.put(color, i);
            COLOR_TO_LEGACY.put(color, color.equals("light_gray") ? "silver" : color);
        }
    }

    private Conversion618_594() {
    }

    public static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        return unflattenColorItem(mapping);
    }

    public static NbtMap remapBlock(NbtMap tag) {
        tag = unflattenColorBlock(tag);
        tag = remapVerticalHalf(tag);
        tag = remapBlockFaceToFacing(tag);
        tag = remapCardinalToFacing(tag);
        return remapCardinalToDirection(tag);
    }

    private static GeyserMappingItem unflattenColorItem(GeyserMappingItem mapping) {
        String identifier = mapping.getBedrockIdentifier();
        if (!identifier.startsWith("minecraft:")) {
            return mapping;
        }

        String withoutNamespace = identifier.substring("minecraft:".length());
        for (Map.Entry<String, String> entry : FLATTENED_COLOR_SUFFIX_TO_LEGACY.entrySet()) {
            String suffix = entry.getKey();
            if (!withoutNamespace.endsWith(suffix)) {
                continue;
            }
            String colorPrefix = withoutNamespace.substring(0, withoutNamespace.length() - suffix.length());
            Integer data = COLOR_TO_DATA.get(colorPrefix);
            if (data == null) {
                continue;
            }
            return mapping.withBedrockIdentifier(entry.getValue()).withBedrockData(data);
        }
        return mapping;
    }

    private static NbtMap unflattenColorBlock(NbtMap tag) {
        String name = tag.getString("name");
        if (!name.startsWith("minecraft:")) {
            return tag;
        }

        String withoutNamespace = name.substring("minecraft:".length());
        for (Map.Entry<String, String> entry : FLATTENED_COLOR_SUFFIX_TO_LEGACY.entrySet()) {
            String suffix = entry.getKey();
            if (!withoutNamespace.endsWith(suffix)) {
                continue;
            }
            String colorPrefix = withoutNamespace.substring(0, withoutNamespace.length() - suffix.length());
            String legacyColor = COLOR_TO_LEGACY.get(colorPrefix);
            if (legacyColor == null) {
                continue;
            }

            NbtMapBuilder states = tag.getCompound("states").toBuilder();
            states.putString("color", legacyColor);
            return tag.toBuilder()
                .putString("name", entry.getValue())
                .putCompound("states", states.build())
                .build();
        }
        return tag;
    }

    private static NbtMap remapVerticalHalf(NbtMap tag) {
        NbtMap states = tag.getCompound("states");
        if (!states.containsKey("minecraft:vertical_half")) {
            return tag;
        }

        String half = states.getString("minecraft:vertical_half");
        NbtMapBuilder statesBuilder = states.toBuilder();
        statesBuilder.remove("minecraft:vertical_half");
        // Bedrock 1.20.0–1.20.10 palettes store this as a byte (0/1).
        statesBuilder.putByte("top_slot_bit", (byte) ("top".equals(half) ? 1 : 0));
        return tag.toBuilder().putCompound("states", statesBuilder.build()).build();
    }

    private static NbtMap remapBlockFaceToFacing(NbtMap tag) {
        String name = tag.getString("name");
        if (!BLOCK_FACE_TO_FACING_BLOCKS.contains(name)) {
            return tag;
        }

        NbtMap states = tag.getCompound("states");
        if (!states.containsKey("minecraft:block_face")) {
            return tag;
        }

        Integer facing = BLOCK_FACE_TO_FACING.get(states.getString("minecraft:block_face"));
        if (facing == null) {
            return tag;
        }

        NbtMapBuilder statesBuilder = states.toBuilder();
        statesBuilder.remove("minecraft:block_face");
        statesBuilder.putInt("facing_direction", facing);
        return tag.toBuilder().putCompound("states", statesBuilder.build()).build();
    }

    private static NbtMap remapCardinalToFacing(NbtMap tag) {
        String name = tag.getString("name");
        if (!CARDINAL_TO_FACING_BLOCKS.contains(name)) {
            return tag;
        }

        NbtMap states = tag.getCompound("states");
        if (!states.containsKey("minecraft:cardinal_direction")) {
            return tag;
        }

        Integer facing = CARDINAL_TO_FACING.get(states.getString("minecraft:cardinal_direction"));
        if (facing == null) {
            return tag;
        }

        NbtMapBuilder statesBuilder = states.toBuilder();
        statesBuilder.remove("minecraft:cardinal_direction");
        statesBuilder.putInt("facing_direction", facing);
        return tag.toBuilder().putCompound("states", statesBuilder.build()).build();
    }

    private static NbtMap remapCardinalToDirection(NbtMap tag) {
        String name = tag.getString("name");
        if (!CARDINAL_TO_DIRECTION_BLOCKS.contains(name)) {
            return tag;
        }

        NbtMap states = tag.getCompound("states");
        if (!states.containsKey("minecraft:cardinal_direction")) {
            return tag;
        }

        Integer direction = CARDINAL_TO_DIRECTION.get(states.getString("minecraft:cardinal_direction"));
        if (direction == null) {
            return tag;
        }

        NbtMapBuilder statesBuilder = states.toBuilder();
        statesBuilder.remove("minecraft:cardinal_direction");
        statesBuilder.putInt("direction", direction);
        return tag.toBuilder().putCompound("states", statesBuilder.build()).build();
    }
}
