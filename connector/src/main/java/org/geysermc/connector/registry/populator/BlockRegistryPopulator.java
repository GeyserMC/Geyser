/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.registry.populator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.v440.Bedrock_v440;
import com.nukkitx.protocol.bedrock.v448.Bedrock_v448;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.chunk.BlockStorage;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.registry.BlockRegistries;
import org.geysermc.connector.registry.type.BlockMapping;
import org.geysermc.connector.registry.type.BlockMappings;
import org.geysermc.connector.utils.BlockUtils;
import org.geysermc.connector.utils.FileUtils;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.zip.GZIPInputStream;

/**
 * Populates the block registries.
 */
public class BlockRegistryPopulator {
    private static final ImmutableMap<String, BiFunction<String, NbtMapBuilder, String>> STATE_MAPPER;

    private static final Object2IntMap<String> PALETTE_VERSIONS;

    static {
        ImmutableMap.Builder<String, BiFunction<String, NbtMapBuilder, String>> stateMapperBuilder = ImmutableMap.<String, BiFunction<String, NbtMapBuilder, String>>builder()
                .put("1_17_10", (bedrockIdentifier, statesBuilder) -> null);
        if (!GeyserConnector.getInstance().getConfig().isExtendedWorldHeight()) {
            stateMapperBuilder.put("1_17_0", (bedrockIdentifier, statesBuilder) -> {
                if (bedrockIdentifier.contains("candle")) {
                    // Replace candles with sea pickles or cake
                    if (bedrockIdentifier.contains("cake")) {
                        statesBuilder.remove("lit");
                        statesBuilder.putInt("bite_counter", 0);
                        return "minecraft:cake";
                    } else {
                        statesBuilder.put("cluster_count", statesBuilder.remove("candles"));
                        statesBuilder.putBoolean("dead_bit", ((byte) (statesBuilder.remove("lit"))) != 0);
                        return "minecraft:sea_pickle";
                    }
                }
                return null;
            });
        }
        STATE_MAPPER = stateMapperBuilder.build();

        PALETTE_VERSIONS = new Object2IntOpenHashMap<>();
        if (!GeyserConnector.getInstance().getConfig().isExtendedWorldHeight()) {
            PALETTE_VERSIONS.put("1_17_0", Bedrock_v440.V440_CODEC.getProtocolVersion());
        }
        PALETTE_VERSIONS.put("1_17_10", Bedrock_v448.V448_CODEC.getProtocolVersion());
    }

    /**
     * Stores the raw blocks JSON until it is no longer needed.
     */
    private static JsonNode BLOCKS_JSON;

    public static void populate() {
        registerJavaBlocks();
        registerBedrockBlocks();

        BLOCKS_JSON = null;
    }

    private static void registerBedrockBlocks() {
        for (Map.Entry<String, BiFunction<String, NbtMapBuilder, String>> palette : STATE_MAPPER.entrySet()) {
            InputStream stream = FileUtils.getResource(String.format("bedrock/block_palette.%s.nbt", palette.getKey()));
            NbtList<NbtMap> blocksTag;
            try (NBTInputStream nbtInputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(stream)))) {
                NbtMap blockPalette = (NbtMap) nbtInputStream.readTag();
                blocksTag = (NbtList<NbtMap>) blockPalette.getList("blocks", NbtType.COMPOUND);
            } catch (Exception e) {
                throw new AssertionError("Unable to get blocks from runtime block states", e);
            }
            Map<String, NbtMap> javaIdentifierToBedrockTag = new Object2ObjectOpenHashMap<>(blocksTag.size());
            // New since 1.16.100 - find the block runtime ID by the order given to us in the block palette,
            // as we no longer send a block palette
            Object2IntMap<NbtMap> blockStateOrderedMap = new Object2IntOpenHashMap<>(blocksTag.size());

            int stateVersion = -1;
            for (int i = 0; i < blocksTag.size(); i++) {
                NbtMap tag = blocksTag.get(i);
                if (blockStateOrderedMap.containsKey(tag)) {
                    throw new AssertionError("Duplicate block states in Bedrock palette: " + tag);
                }
                blockStateOrderedMap.put(tag, i);
                if (stateVersion == -1) {
                    stateVersion = tag.getInt("version");
                }
            }
            int airRuntimeId = -1;
            int commandBlockRuntimeId = -1;
            int javaRuntimeId = -1;
            int waterRuntimeId = -1;
            Iterator<Map.Entry<String, JsonNode>> blocksIterator = BLOCKS_JSON.fields();

            BiFunction<String, NbtMapBuilder, String> stateMapper = STATE_MAPPER.getOrDefault(palette.getKey(), (i, s) -> null);

            Int2IntMap javaToBedrockBlockMap = new Int2IntOpenHashMap();
            Int2IntMap bedrockToJavaBlockMap = new Int2IntOpenHashMap();

            Map<String, NbtMap> flowerPotBlocks = new Object2ObjectOpenHashMap<>();
            Object2IntMap<NbtMap> itemFrames = new Object2IntOpenHashMap<>();

            BlockMappings.BlockMappingsBuilder builder = BlockMappings.builder();
            while (blocksIterator.hasNext()) {
                javaRuntimeId++;
                Map.Entry<String, JsonNode> entry = blocksIterator.next();
                String javaId = entry.getKey();

                NbtMap blockTag = buildBedrockState(entry.getValue(), stateVersion, stateMapper);
                int bedrockRuntimeId = blockStateOrderedMap.getOrDefault(blockTag, -1);
                if (bedrockRuntimeId == -1) {
                    throw new RuntimeException("Unable to find " + javaId + " Bedrock runtime ID! Built compound tag: \n" + blockTag);
                }

                switch (javaId) {
                    case "minecraft:air":
                        airRuntimeId = bedrockRuntimeId;
                        break;
                    case "minecraft:water[level=0]":
                        waterRuntimeId = bedrockRuntimeId;
                        break;
                    case "minecraft:command_block[conditional=false,facing=north]":
                        commandBlockRuntimeId = bedrockRuntimeId;
                        break;
                }

                boolean waterlogged = entry.getKey().contains("waterlogged=true")
                        || javaId.contains("minecraft:bubble_column") || javaId.contains("minecraft:kelp") || javaId.contains("seagrass");

                if (waterlogged) {
                    bedrockToJavaBlockMap.putIfAbsent(bedrockRuntimeId | 1 << 31, javaRuntimeId);
                    int finalJavaRuntimeId = javaRuntimeId;
                    BlockRegistries.WATERLOGGED.register(set -> set.add(finalJavaRuntimeId));
                } else {
                    bedrockToJavaBlockMap.putIfAbsent(bedrockRuntimeId, javaRuntimeId);
                }

                String cleanJavaIdentifier = BlockUtils.getCleanIdentifier(entry.getKey());

                // Get the tag needed for non-empty flower pots
                if (entry.getValue().get("pottable") != null) {
                    flowerPotBlocks.put(cleanJavaIdentifier, blockTag);
                }

                if (!cleanJavaIdentifier.equals(entry.getValue().get("bedrock_identifier").asText())) {
                    javaIdentifierToBedrockTag.put(cleanJavaIdentifier, blockTag);
                }

                javaToBedrockBlockMap.put(javaRuntimeId, bedrockRuntimeId);
            }

            if (commandBlockRuntimeId == -1) {
                throw new AssertionError("Unable to find command block in palette");
            }
            builder.commandBlockRuntimeId(commandBlockRuntimeId);

            if (waterRuntimeId == -1) {
                throw new AssertionError("Unable to find water in palette");
            }
            builder.bedrockWaterId(waterRuntimeId);

            if (airRuntimeId == -1) {
                throw new AssertionError("Unable to find air in palette");
            }
            builder.bedrockAirId(airRuntimeId);

            // Loop around again to find all item frame runtime IDs
            for (Object2IntMap.Entry<NbtMap> entry : blockStateOrderedMap.object2IntEntrySet()) {
                String name = entry.getKey().getString("name");
                if (name.equals("minecraft:frame") || name.equals("minecraft:glow_frame")) {
                    itemFrames.put(entry.getKey(), entry.getIntValue());
                }
            }
            builder.bedrockBlockStates(blocksTag);

            BlockRegistries.BLOCKS.register(PALETTE_VERSIONS.getInt(palette.getKey()), builder.blockStateVersion(stateVersion)
                    .emptyChunkSection(new ChunkSection(new BlockStorage[]{new BlockStorage(airRuntimeId)}))
                    .javaToBedrockBlockMap(javaToBedrockBlockMap)
                    .bedrockToJavaBlockMap(bedrockToJavaBlockMap)
                    .javaIdentifierToBedrockTag(javaIdentifierToBedrockTag)
                    .itemFrames(itemFrames)
                    .flowerPotBlocks(flowerPotBlocks)
                    .build());
        }
    }

    private static void registerJavaBlocks() {
        InputStream stream = FileUtils.getResource("mappings/blocks.json");

        JsonNode blocksJson;
        try {
            blocksJson = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block mappings", e);
        }

        Set<String> cleanIdentifiers = new HashSet<>();

        int javaRuntimeId = -1;
        int bellBlockId = -1;
        int cobwebBlockId = -1;
        int furnaceRuntimeId = -1;
        int furnaceLitRuntimeId = -1;
        int spawnerRuntimeId = -1;
        int uniqueJavaId = -1;
        int waterRuntimeId = -1;
        Iterator<Map.Entry<String, JsonNode>> blocksIterator = blocksJson.fields();
        while (blocksIterator.hasNext()) {
            javaRuntimeId++;
            Map.Entry<String, JsonNode> entry = blocksIterator.next();
            String javaId = entry.getKey();

            // TODO fix this, (no block should have a null hardness)
            BlockMapping.BlockMappingBuilder builder = BlockMapping.builder();
            JsonNode hardnessNode = entry.getValue().get("block_hardness");
            if (hardnessNode != null) {
                builder.hardness(hardnessNode.doubleValue());
            }

            JsonNode canBreakWithHandNode = entry.getValue().get("can_break_with_hand");
            if (canBreakWithHandNode != null) {
                builder.canBreakWithHand(canBreakWithHandNode.booleanValue());
            } else {
                builder.canBreakWithHand(false);
            }

            JsonNode collisionIndexNode = entry.getValue().get("collision_index");
            if (hardnessNode != null) {
                builder.collisionIndex(collisionIndexNode.intValue());
            }

            JsonNode pickItemNode = entry.getValue().get("pick_item");
            if (pickItemNode != null) {
                builder.pickItem(pickItemNode.textValue());
            }

            BlockStateValues.storeBlockStateValues(entry.getKey(), javaRuntimeId, entry.getValue());

            String cleanJavaIdentifier = BlockUtils.getCleanIdentifier(entry.getKey());
            String bedrockIdentifier = entry.getValue().get("bedrock_identifier").asText();

            if (!cleanIdentifiers.contains(cleanJavaIdentifier)) {
                uniqueJavaId++;
                cleanIdentifiers.add(cleanJavaIdentifier);
            }

            builder.javaIdentifier(javaId);
            builder.javaBlockId(uniqueJavaId);

            BlockRegistries.JAVA_IDENTIFIERS.register(javaId, javaRuntimeId);
            BlockRegistries.JAVA_BLOCKS.register(javaRuntimeId, builder.build());

            // Keeping this here since this is currently unchanged between versions
            if (!cleanJavaIdentifier.equals(bedrockIdentifier)) {
                BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.register(cleanJavaIdentifier, bedrockIdentifier);
            }

            if (javaId.startsWith("minecraft:bell[")) {
                bellBlockId = uniqueJavaId;

            } else if (javaId.contains("cobweb")) {
                cobwebBlockId = uniqueJavaId;

            } else if (javaId.startsWith("minecraft:furnace[facing=north")) {
                if (javaId.contains("lit=true")) {
                    furnaceLitRuntimeId = javaRuntimeId;
                } else {
                    furnaceRuntimeId = javaRuntimeId;
                }

            } else if (javaId.startsWith("minecraft:spawner")) {
                spawnerRuntimeId = javaRuntimeId;

            } else if ("minecraft:water[level=0]".equals(javaId)) {
                waterRuntimeId = javaRuntimeId;
            }
        }
        if (bellBlockId == -1) {
            throw new AssertionError("Unable to find bell in palette");
        }
        BlockStateValues.JAVA_BELL_ID = bellBlockId;

        if (cobwebBlockId == -1) {
            throw new AssertionError("Unable to find cobwebs in palette");
        }
        BlockStateValues.JAVA_COBWEB_ID = cobwebBlockId;

        if (furnaceRuntimeId == -1) {
            throw new AssertionError("Unable to find furnace in palette");
        }
        BlockStateValues.JAVA_FURNACE_ID = furnaceRuntimeId;

        if (furnaceLitRuntimeId == -1) {
            throw new AssertionError("Unable to find lit furnace in palette");
        }
        BlockStateValues.JAVA_FURNACE_LIT_ID = furnaceLitRuntimeId;

        if (spawnerRuntimeId == -1) {
            throw new AssertionError("Unable to find spawner in palette");
        }
        BlockStateValues.JAVA_SPAWNER_ID = spawnerRuntimeId;

        if (waterRuntimeId == -1) {
            throw new AssertionError("Unable to find Java water in palette");
        }
        BlockStateValues.JAVA_WATER_ID = waterRuntimeId;

        BLOCKS_JSON = blocksJson;
    }

    private static NbtMap buildBedrockState(JsonNode node, int blockStateVersion, BiFunction<String, NbtMapBuilder, String> statesMapper) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        String bedrockIdentifier = node.get("bedrock_identifier").textValue();
        tagBuilder.putString("name", bedrockIdentifier)
                .putInt("version", blockStateVersion);

        NbtMapBuilder statesBuilder = NbtMap.builder();

        // check for states
        if (node.has("bedrock_states")) {
            Iterator<Map.Entry<String, JsonNode>> statesIterator = node.get("bedrock_states").fields();

            while (statesIterator.hasNext()) {
                Map.Entry<String, JsonNode> stateEntry = statesIterator.next();
                JsonNode stateValue = stateEntry.getValue();
                switch (stateValue.getNodeType()) {
                    case BOOLEAN:
                        statesBuilder.putBoolean(stateEntry.getKey(), stateValue.booleanValue());
                        continue;
                    case STRING:
                        statesBuilder.putString(stateEntry.getKey(), stateValue.textValue());
                        continue;
                    case NUMBER:
                        statesBuilder.putInt(stateEntry.getKey(), stateValue.intValue());
                }
            }
        }
        String newIdentifier = statesMapper.apply(bedrockIdentifier, statesBuilder);
        if (newIdentifier != null) {
            tagBuilder.putString("name", newIdentifier);
        }
        tagBuilder.put("states", statesBuilder.build());
        return tagBuilder.build();
    }
}
