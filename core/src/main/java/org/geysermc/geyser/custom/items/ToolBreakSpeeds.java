/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.custom.items;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

public class ToolBreakSpeeds {
    public static float toolTierToSpeed(String toolTier) {
        return switch (toolTier) {
            case "wooden" -> 2f;
            case "stone" -> 4f;
            case "iron" -> 6f;
            case "golden" -> 12f;
            case "diamond" -> 8f;
            case "netherite" -> 9f;
            default -> 1f;
        };
    }

    private static NbtMap createTagBreakSpeed(float speed, String... tags) {
        String tagString = "query.any_tag('" + tags[0];
        for (int i = 1; i < tags.length; i++) {
            tagString += "', '" + tags[i];
        }
        tagString += "')";

        return NbtMap.builder()
                .putCompound("block", NbtMap.builder().putString("tags", tagString).build())
                .putFloat("speed", 0.5f)
                .build();
    }

    private static NbtMap createBreakSpeed(float speed, String block) {
        return NbtMap.builder()
                .putString("block", block)
                .putFloat("speed", 0.5f)
                .build();
    }

    private static NbtMap createDigger(List<NbtMap> speeds) {
        return NbtMap.builder()
                .putBoolean("use_efficiency", true)
                .putList("destroy_speeds", NbtType.COMPOUND, speeds)
                .build();
    }

    public static NbtMap getAxeDigger(float speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createTagBreakSpeed(speed, "wood", "pumpkin", "plant"));
        speeds.add(createBreakSpeed(speed, "minecraft:chest"));
        speeds.add(createBreakSpeed(speed, "minecraft:melon_block"));
        //All speeds need adding

        return createDigger(speeds);
    }

    public static NbtMap getPickaxeDigger(float speed, String toolTier) {
        List<NbtMap> speeds = new ArrayList<>();
        if (toolTier.equals("diamond") || toolTier.equals("netherite")) {
            speeds.add(createTagBreakSpeed(speed, "stone", "metal", "gravel", "iron_pick_diggable", "diamond_pick_diggable"));
        } else {
            speeds.add(createTagBreakSpeed(speed, "stone", "metal", "gravel", "iron_pick_diggable"));
        }
        speeds.add(createBreakSpeed(speed, "minecraft:ice"));
        speeds.add(createBreakSpeed(speed, "minecraft:sandstone"));
        //All speeds need adding

        return createDigger(speeds);
    }

    public static NbtMap getShovelDigger(float speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createTagBreakSpeed(speed, "dirt", "sand", "gravel", "grass", "snow"));
        //All speeds need adding

        return createDigger(speeds);
    }

    public static NbtMap getSwordDigger(float speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createBreakSpeed(speed, "minecraft:web"));
        speeds.add(createBreakSpeed(speed, "minecraft:bamboo"));
        //All speeds need adding

        return createDigger(speeds);
    }
}
