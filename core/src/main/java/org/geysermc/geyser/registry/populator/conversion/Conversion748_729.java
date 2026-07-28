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
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.Map;
import java.util.Set;

public class Conversion748_729 {

    /**
     * Flattened skulls (Bedrock 1.21.40+) → legacy {@code minecraft:skull} + item meta.
     * Reverse of Cloudburst {@code BlockStateUpdater_1_21_40}.
     */
    private static final Map<String, Integer> FLATTENED_SKULLS = Map.of(
        "minecraft:skeleton_skull", 0,
        "minecraft:wither_skeleton_skull", 1,
        "minecraft:zombie_head", 2,
        "minecraft:player_head", 3,
        "minecraft:creeper_head", 4,
        "minecraft:dragon_head", 5,
        "minecraft:piglin_head", 6
    );

    /**
     * Pre-1.21.40 cherry/mangrove wood still require {@code stripped_bit} in the Bedrock palette.
     * Modern mappings only send {@code pillar_axis}.
     */
    private static final Set<String> WOOD_WITH_STRIPPED_BIT = Set.of(
        "minecraft:cherry_wood",
        "minecraft:mangrove_wood"
    );

    public static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        String identifier = mapping.getBedrockIdentifier();

        if (FLATTENED_SKULLS.containsKey(identifier)) {
            return mapping.withBedrockIdentifier("minecraft:skull")
                .withBedrockData(FLATTENED_SKULLS.get(identifier));
        }

        // Split out in Bedrock 1.21.40; older palettes use brown_mushroom_block meta 15 (stem).
        if (identifier.equals("minecraft:mushroom_stem")) {
            return mapping.withBedrockIdentifier("minecraft:brown_mushroom_block").withBedrockData(15);
        }

        return mapping;
    }

    public static NbtMap remapBlock(NbtMap tag) {
        String name = tag.getString("name");

        // Skull type lived in the block entity / item data before 1.21.40; palette only has minecraft:skull.
        if (FLATTENED_SKULLS.containsKey(name)) {
            return tag.toBuilder()
                .putString("name", "minecraft:skull")
                .build();
        }

        if (name.equals("minecraft:mushroom_stem")) {
            return tag.toBuilder()
                .putString("name", "minecraft:brown_mushroom_block")
                .putCompound("states", tag.getCompound("states").toBuilder()
                    .putInt("huge_mushroom_bits", 15)
                    .build())
                .build();
        }

        if (!WOOD_WITH_STRIPPED_BIT.contains(name)) {
            return tag;
        }

        NbtMap states = tag.getCompound("states");
        if (states.containsKey("stripped_bit")) {
            return tag;
        }

        return tag.toBuilder()
            .putCompound("states", states.toBuilder().putBoolean("stripped_bit", false).build())
            .build();
    }
}
