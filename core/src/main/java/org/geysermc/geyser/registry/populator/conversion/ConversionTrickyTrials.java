/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

/**
 * Remaps Tricky Trials (1.21) blocks that are missing on all 1.20.x Bedrock clients.
 */
public final class ConversionTrickyTrials {

    private ConversionTrickyTrials() {
    }

    public static NbtMap remapBlock(NbtMap tag) {
        final String name = tag.getString("name");

        if (name.equals("minecraft:crafter")) {
            return ConversionHelper.withoutStates("minecraft:crafting_table");
        }
        if (name.equals("minecraft:vault")) {
            return ConversionHelper.asChest(tag);
        }
        if (name.equals("minecraft:trial_spawner")) {
            return ConversionHelper.withoutStates("minecraft:mob_spawner");
        }
        if (name.equals("minecraft:heavy_core")) {
            return ConversionHelper.withoutStates("minecraft:iron_block");
        }

        if (name.contains("tuff") && !name.equals("minecraft:tuff")) {
            String replacement;
            if (name.contains("brick") || name.contains("polished") || name.contains("chiseled")) {
                replacement = name.replace("tuff", "deepslate");
                if (name.contains("chiseled")) {
                    replacement = replacement.replace("_bricks", "");
                }
            } else {
                replacement = name.replace("tuff", "cobbled_deepslate");
            }
            return tag.toBuilder().putString("name", replacement).build();
        }

        if (name.contains("copper")) {
            boolean removeStates = false;
            String replacement;
            if (name.contains("chiseled")) {
                replacement = name.replace("_chiseled", "").replace("chiseled_", "");
            } else if (name.endsWith("bulb")) {
                replacement = name.replace("_bulb", "");
                removeStates = true;
            } else if (name.endsWith("grate")) {
                replacement = "minecraft:raw_iron_block";
            } else if (name.endsWith("door")) {
                replacement = name.contains("trap") ? "minecraft:iron_trapdoor" : "minecraft:iron_door";
            } else {
                return tag;
            }

            if (replacement.endsWith(":copper")) {
                replacement = replacement + "_block";
            }

            NbtMapBuilder builder = tag.toBuilder().putString("name", replacement);
            if (removeStates) {
                builder.putCompound("states", NbtMap.EMPTY);
            }
            return builder.build();
        }

        return tag;
    }
}
