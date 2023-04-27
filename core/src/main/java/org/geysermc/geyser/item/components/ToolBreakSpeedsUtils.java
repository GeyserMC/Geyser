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

package org.geysermc.geyser.item.components;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

public class ToolBreakSpeedsUtils {
    public static int toolTierToSpeed(String toolTier) {
        ToolTier tier = ToolTier.getByName(toolTier);
        if (tier != null) {
            return tier.getSpeed();
        }

        return 0;
    }

    private static NbtMap createTagBreakSpeed(int speed, String... tags) {
        StringBuilder builder = new StringBuilder("query.any_tag('");
        builder.append(tags[0]);
        for (int i = 1; i < tags.length; i++) {
            builder.append("', '").append(tags[i]);
        }
        builder.append("')");

        return NbtMap.builder()
                .putCompound("block", NbtMap.builder()
                        .putString("tags", builder.toString())
                        .build())
                .putCompound("on_dig", NbtMap.builder()
                        .putCompound("condition", NbtMap.builder()
                                .putString("expression", "")
                                .putInt("version", -1)
                                .build())
                        .putString("event", "tool_durability")
                        .putString("target", "self")
                        .build())
                .putInt("speed", speed)
                .build();
    }

    private static NbtMap createBreakSpeed(int speed, String block) {
        return NbtMap.builder()
                .putCompound("block", NbtMap.builder()
                        .putString("name", block).build())
                .putCompound("on_dig", NbtMap.builder()
                        .putCompound("condition", NbtMap.builder()
                                .putString("expression", "")
                                .putInt("version", -1)
                                .build())
                        .putString("event", "tool_durability")
                        .putString("target", "self")
                        .build())
                .putInt("speed", speed)
                .build();
    }

    private static NbtMap createDigger(List<NbtMap> speeds) {
        return NbtMap.builder()
                .putList("destroy_speeds", NbtType.COMPOUND, speeds)
                .putCompound("on_dig", NbtMap.builder()
                        .putCompound("condition", NbtMap.builder()
                                .putString("expression", "")
                                .putInt("version", -1)
                                .build())
                        .putString("event", "tool_durability")
                        .putString("target", "self")
                        .build())
                .putBoolean("use_efficiency", true)
                .build();
    }

    public static NbtMap getAxeDigger(int speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createTagBreakSpeed(speed, "wood", "pumpkin", "plant"));

        return createDigger(speeds);
    }

    public static NbtMap getPickaxeDigger(int speed, String toolTier) {
        List<NbtMap> speeds = new ArrayList<>();
        if (toolTier.equals(ToolTier.DIAMOND.toString()) || toolTier.equals(ToolTier.NETHERITE.toString())) {
            speeds.add(createTagBreakSpeed(speed, "iron_pick_diggable", "diamond_pick_diggable"));
        } else {
            speeds.add(createTagBreakSpeed(speed, "iron_pick_diggable"));
        }
        speeds.add(createTagBreakSpeed(speed, "stone", "metal", "rail", "mob_spawner"));

        return createDigger(speeds);
    }

    public static NbtMap getShovelDigger(int speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createTagBreakSpeed(speed, "dirt", "sand", "gravel", "grass", "snow"));

        return createDigger(speeds);
    }

    public static NbtMap getSwordDigger(int speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createBreakSpeed(speed, "minecraft:web"));
        speeds.add(createBreakSpeed(speed, "minecraft:bamboo"));

        return createDigger(speeds);
    }

    public static NbtMap getHoeDigger(int speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createBreakSpeed(speed, "minecraft:leaves"));
        speeds.add(createBreakSpeed(speed, "minecraft:leaves2"));
        speeds.add(createBreakSpeed(speed, "minecraft:azalea_leaves"));
        speeds.add(createBreakSpeed(speed, "minecraft:azalea_leaves_flowered"));

        speeds.add(createBreakSpeed(speed, "minecraft:sculk"));
        speeds.add(createBreakSpeed(speed, "minecraft:sculk_catalyst"));
        speeds.add(createBreakSpeed(speed, "minecraft:sculk_sensor"));
        speeds.add(createBreakSpeed(speed, "minecraft:sculk_shrieker"));
        speeds.add(createBreakSpeed(speed, "minecraft:sculk_vein"));

        speeds.add(createBreakSpeed(speed, "minecraft:nether_wart_block"));
        speeds.add(createBreakSpeed(speed, "minecraft:warped_wart_block"));

        speeds.add(createBreakSpeed(speed, "minecraft:hay_block"));
        speeds.add(createBreakSpeed(speed, "minecraft:moss_block"));
        speeds.add(createBreakSpeed(speed, "minecraft:shroomlight"));
        speeds.add(createBreakSpeed(speed, "minecraft:sponge"));
        speeds.add(createBreakSpeed(speed, "minecraft:target"));

        return createDigger(speeds);
    }

    public static NbtMap getShearsDigger(int speed) {
        List<NbtMap> speeds = new ArrayList<>();
        speeds.add(createBreakSpeed(speed, "minecraft:web"));

        speeds.add(createBreakSpeed(speed, "minecraft:leaves"));
        speeds.add(createBreakSpeed(speed, "minecraft:leaves2"));
        speeds.add(createBreakSpeed(speed, "minecraft:azalea_leaves"));
        speeds.add(createBreakSpeed(speed, "minecraft:azalea_leaves_flowered"));

        speeds.add(createBreakSpeed(speed, "minecraft:wool"));

        speeds.add(createBreakSpeed(speed, "minecraft:glow_lichen"));
        speeds.add(createBreakSpeed(speed, "minecraft:vine"));

        return createDigger(speeds);
    }
}
