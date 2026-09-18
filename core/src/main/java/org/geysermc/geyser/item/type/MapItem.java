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

package org.geysermc.geyser.item.type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Map;

public class MapItem extends Item {
    // Map from Java identifier to bedrock metadata: Java has one item for each explorer map, bedrock uses metadata for them.
    private static final Map<String, Integer> EXPLORER_MAP_METADATA = Map.ofEntries(
        Map.entry("map", -1),
        Map.entry("filled_map", -1),
        Map.entry("ocean_monument_map", 3),
        Map.entry("woodland_mansion_map", 4),
        Map.entry("buried_trial_chambers_map", 14),
        Map.entry("jungle_pyramid_map", 12),
        Map.entry("swamp_hut_map", 13),
        Map.entry("desert_village_map", 11),
        Map.entry("plains_village_map", 9),
        Map.entry("savanna_village_map", 10),
        Map.entry("snowy_village_map", 7),
        Map.entry("taiga_village_map", 8),
        Map.entry("buried_treasure_map", 5),
        Map.entry("buried_ancient_city_map", 23),
        Map.entry("buried_mineshaft_map", 24),
        Map.entry("desert_pyramid_map", 25),
        // Note that abandoned camp maps are one item on Java, but multiple metadata on bedrock
        // (one metadata ID for each biome)
        // The textures are all the same, but the names are different.
        // This shouldn't matter much though, because as far as I can tell, Java sets the proper name in the minecraft:custom_name component
        // when generating maps in chest loot. So the only difference will be with spawned-in maps.
        Map.entry("abandoned_camp_map", 18),
        Map.entry("warm_ocean_ruins_map", 26)
    );

    private final int explorerMetadata;

    public MapItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
        Integer explorerMetadata = EXPLORER_MAP_METADATA.get(javaIdentifier);
        if (explorerMetadata == null) {
            throw new IllegalStateException("Missing explorer map metadata for: " + javaIdentifier);
        }
        this.explorerMetadata = explorerMetadata;
    }

    @Override
    public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        ItemData.Builder builder = super.translateToBedrock(session, count, components, mapping, mappings);
        if (explorerMetadata != -1) {
            builder.damage(explorerMetadata);
        }
        return builder;
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull TooltipOptions tooltip, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        Integer mapValue = components.get(DataComponentTypes.MAP_ID);
        if (mapValue == null) {
            return;
        }

        builder.putLong("map_uuid", mapValue);
        builder.putInt("map_name_index", mapValue);
        builder.putByte("map_display_players", (byte) 1);
    }
}
