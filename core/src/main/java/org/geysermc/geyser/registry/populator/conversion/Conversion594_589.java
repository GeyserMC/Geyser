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

/**
 * Downgrades flattened concrete / shulker boxes and observer facing to Bedrock 1.20.0.
 * Reverse of Cloudburst {@code BlockStateUpdater_1_20_10}.
 */
public final class Conversion594_589 {

    private static final Map<String, String> FLATTENED_COLOR_SUFFIX_TO_LEGACY = Map.of(
        "_concrete", "minecraft:concrete",
        "_shulker_box", "minecraft:shulker_box"
    );

    private static final Map<String, Integer> COLOR_TO_DATA = new LinkedHashMap<>();
    private static final Map<String, String> COLOR_TO_LEGACY = new LinkedHashMap<>();

    private static final Map<String, Integer> FACING_STRING_TO_INT = Map.of(
        "down", 0,
        "up", 1,
        "north", 2,
        "south", 3,
        "west", 4,
        "east", 5
    );

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

    private Conversion594_589() {
    }

    public static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        return unflattenColorItem(mapping);
    }

    public static NbtMap remapBlock(NbtMap tag) {
        tag = unflattenColorBlock(tag);
        return remapObserverFacing(tag);
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

    private static NbtMap remapObserverFacing(NbtMap tag) {
        if (!"minecraft:observer".equals(tag.getString("name"))) {
            return tag;
        }

        NbtMap states = tag.getCompound("states");
        if (!states.containsKey("minecraft:facing_direction")) {
            return tag;
        }

        Integer facing = FACING_STRING_TO_INT.get(states.getString("minecraft:facing_direction"));
        if (facing == null) {
            return tag;
        }

        NbtMapBuilder statesBuilder = states.toBuilder();
        statesBuilder.remove("minecraft:facing_direction");
        statesBuilder.putInt("facing_direction", facing);
        return tag.toBuilder().putCompound("states", statesBuilder.build()).build();
    }
}
