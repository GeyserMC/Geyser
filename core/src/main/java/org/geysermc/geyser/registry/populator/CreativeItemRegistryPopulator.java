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

package org.geysermc.geyser.registry.populator;

import com.fasterxml.jackson.databind.JsonNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class CreativeItemRegistryPopulator {
    private static final List<BiPredicate<String, Integer>> JAVA_ONLY_ITEM_FILTER = List.of(
            // Just shows an empty texture; either way it doesn't exist in the creative menu on Java
            (identifier, data) -> identifier.equals("minecraft:debug_stick"),
            // Bedrock-only as its own item
            (identifier, data) -> identifier.equals("minecraft:empty_map") && data == 2,
            // Bedrock-only banner patterns
            (identifier, data) -> identifier.equals("minecraft:bordure_indented_banner_pattern") || identifier.equals("minecraft:field_masoned_banner_pattern")
    );

    static void populate(ItemRegistryPopulator.PaletteVersion palette, Map<String, ItemDefinition> definitions, Consumer<ItemData.Builder> itemConsumer) {
        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();

        // Load creative items
        JsonNode creativeItemEntries;
        try (InputStream stream = bootstrap.getResourceOrThrow(String.format("bedrock/creative_items.%s.json", palette.version()))) {
            creativeItemEntries = GeyserImpl.JSON_MAPPER.readTree(stream).get("items");
        } catch (Exception e) {
            throw new AssertionError("Unable to load creative items", e);
        }

        BlockMappings blockMappings = BlockRegistries.BLOCKS.forVersion(palette.protocolVersion());
        for (JsonNode itemNode : creativeItemEntries) {
            ItemData.Builder itemBuilder = createItemData(itemNode, blockMappings, definitions);
            if (itemBuilder == null) {
                continue;
            }

            itemConsumer.accept(itemBuilder);
        }
    }

    private static ItemData.@Nullable Builder createItemData(JsonNode itemNode, BlockMappings blockMappings, Map<String, ItemDefinition> definitions) {
        int count = 1;
        int damage = 0;
        int bedrockBlockRuntimeId;
        NbtMap tag = null;

        String identifier = itemNode.get("id").textValue();
        for (BiPredicate<String, Integer> predicate : JAVA_ONLY_ITEM_FILTER) {
            if (predicate.test(identifier, damage)) {
                return null;
            }
        }

        JsonNode damageNode = itemNode.get("damage");
        if (damageNode != null) {
            damage = damageNode.asInt();
        }

        JsonNode countNode = itemNode.get("count");
        if (countNode != null) {
            count = countNode.asInt();
        }

        GeyserBedrockBlock blockDefinition = null;
        JsonNode blockRuntimeIdNode = itemNode.get("blockRuntimeId");
        JsonNode blockStateNode;
        if (blockRuntimeIdNode != null) {
            bedrockBlockRuntimeId = blockRuntimeIdNode.asInt();
            if (bedrockBlockRuntimeId == 0 && !identifier.equals("minecraft:blue_candle")) { // FIXME
                bedrockBlockRuntimeId = -1;
            }

            blockDefinition = bedrockBlockRuntimeId == -1 ? null : blockMappings.getDefinition(bedrockBlockRuntimeId);
        } else if ((blockStateNode = itemNode.get("block_state_b64")) != null) {
            byte[] bytes = Base64.getDecoder().decode(blockStateNode.asText());
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                NbtMap stateTag = (NbtMap) NbtUtils.createReaderLE(bais).readTag();

                // We remove these from the state definition map in
                // BlockMappings, so we need to remove it from here
                NbtMapBuilder builder = stateTag.toBuilder();
                builder.remove("name_hash");
                builder.remove("network_id");
                builder.remove("version");

                blockDefinition = blockMappings.getDefinition(builder.build());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonNode nbtNode = itemNode.get("nbt_b64");
        if (nbtNode != null) {
            byte[] bytes = Base64.getDecoder().decode(nbtNode.asText());
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                tag = (NbtMap) NbtUtils.createReaderLE(bais).readTag();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ItemDefinition definition = definitions.get(identifier);
        if (definition == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown item definition with identifier " + identifier + " when loading creative items!");
            return null;
        }

        return ItemData.builder()
                .definition(definition)
                .damage(damage)
                .count(count)
                .tag(tag)
                .blockDefinition(blockDefinition);
    }
}
