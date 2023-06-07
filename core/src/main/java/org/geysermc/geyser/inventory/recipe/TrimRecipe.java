/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.recipe;

import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hardcoded recipe information about armor trims until further improvements can be made. This information was scraped
 * from BDS 1.19.81 with a world with the next_major_update and sniffer features enabled, using ProxyPass.
 */
public class TrimRecipe {

    // For TrimDataPacket, which BDS sends just before the CraftingDataPacket
    public static final List<TrimPattern> PATTERNS;
    public static final List<TrimMaterial> MATERIALS;

    // For CraftingDataPacket
    public static final String ID = "minecraft:smithing_armor_trim";
    public static final ItemDescriptorWithCount BASE = tagDescriptor("minecraft:trimmable_armors");
    public static final ItemDescriptorWithCount ADDITION = tagDescriptor("minecraft:trim_materials");
    public static final ItemDescriptorWithCount TEMPLATE = tagDescriptor("minecraft:trim_templates");

    static {
        List<TrimPattern> patterns = new ArrayList<>(16);
        patterns.add(new TrimPattern("minecraft:ward_armor_trim_smithing_template", "ward"));
        patterns.add(new TrimPattern("minecraft:sentry_armor_trim_smithing_template", "sentry"));
        patterns.add(new TrimPattern("minecraft:snout_armor_trim_smithing_template", "snout"));
        patterns.add(new TrimPattern("minecraft:dune_armor_trim_smithing_template", "dune"));
        patterns.add(new TrimPattern("minecraft:spire_armor_trim_smithing_template", "spire"));
        patterns.add(new TrimPattern("minecraft:tide_armor_trim_smithing_template", "tide"));
        patterns.add(new TrimPattern("minecraft:wild_armor_trim_smithing_template", "wild"));
        patterns.add(new TrimPattern("minecraft:rib_armor_trim_smithing_template", "rib"));
        patterns.add(new TrimPattern("minecraft:coast_armor_trim_smithing_template", "coast"));
        patterns.add(new TrimPattern("minecraft:shaper_armor_trim_smithing_template", "shaper"));
        patterns.add(new TrimPattern("minecraft:eye_armor_trim_smithing_template", "eye"));
        patterns.add(new TrimPattern("minecraft:vex_armor_trim_smithing_template", "vex"));
        patterns.add(new TrimPattern("minecraft:silence_armor_trim_smithing_template", "silence"));
        patterns.add(new TrimPattern("minecraft:wayfinder_armor_trim_smithing_template", "wayfinder"));
        patterns.add(new TrimPattern("minecraft:raiser_armor_trim_smithing_template", "raiser"));
        patterns.add(new TrimPattern("minecraft:host_armor_trim_smithing_template", "host"));
        PATTERNS = Collections.unmodifiableList(patterns);

        List<TrimMaterial> materials = new ArrayList<>(10);
        materials.add(new TrimMaterial("quartz", "§h", "minecraft:quartz"));
        materials.add(new TrimMaterial("iron", "§i", "minecraft:iron_ingot"));
        materials.add(new TrimMaterial("netherite", "§j", "minecraft:netherite_ingot"));
        materials.add(new TrimMaterial("redstone", "§m", "minecraft:redstone"));
        materials.add(new TrimMaterial("copper", "§n", "minecraft:copper_ingot"));
        materials.add(new TrimMaterial("gold", "§p", "minecraft:gold_ingot"));
        materials.add(new TrimMaterial("emerald", "§q", "minecraft:emerald"));
        materials.add(new TrimMaterial("diamond", "§s", "minecraft:diamond"));
        materials.add(new TrimMaterial("lapis", "§t", "minecraft:lapis_lazuli"));
        materials.add(new TrimMaterial("amethyst", "§u", "minecraft:amethyst_shard"));
        MATERIALS = Collections.unmodifiableList(materials);
    }

    private TrimRecipe() {
        //no-op
    }

    private static ItemDescriptorWithCount tagDescriptor(String tag) {
        return new ItemDescriptorWithCount(new ItemTagDescriptor(tag), 1);
    }
}
