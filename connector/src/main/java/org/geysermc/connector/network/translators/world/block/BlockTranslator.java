/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world.block;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntity;
import org.geysermc.connector.utils.FileUtils;
import org.reflections.Reflections;

import java.io.InputStream;
import java.util.*;

public class BlockTranslator {
    public static final NbtList<NbtMap> BLOCKS;
    public static final int AIR = 0;
    public static final int BEDROCK_WATER_ID;

    private static final Int2IntMap JAVA_TO_BEDROCK_BLOCK_MAP = new Int2IntOpenHashMap();
    private static final Int2IntMap BEDROCK_TO_JAVA_BLOCK_MAP = new Int2IntOpenHashMap();
    private static final BiMap<String, Integer> JAVA_ID_BLOCK_MAP = HashBiMap.create();
    private static final IntSet WATERLOGGED = new IntOpenHashSet();
    private static final Object2IntMap<NbtMap> ITEM_FRAMES = new Object2IntOpenHashMap<>();

    // Bedrock carpet ID, used in LlamaEntity.java for decoration
    public static final int CARPET = 171;

    private static final Int2ObjectMap<String> JAVA_ID_TO_BLOCK_ENTITY_MAP = new Int2ObjectOpenHashMap<>();

    public static final Int2DoubleMap JAVA_RUNTIME_ID_TO_HARDNESS = new Int2DoubleOpenHashMap();
    public static final Int2BooleanMap JAVA_RUNTIME_ID_TO_CAN_HARVEST_WITH_HAND = new Int2BooleanOpenHashMap();
    public static final Int2ObjectMap<String> JAVA_RUNTIME_ID_TO_TOOL_TYPE = new Int2ObjectOpenHashMap<>();

    // For block breaking animation math
    public static final IntSet JAVA_RUNTIME_WOOL_IDS = new IntOpenHashSet();
    public static final int JAVA_RUNTIME_COBWEB_ID;

    public static final int JAVA_RUNTIME_FURNACE_ID;
    public static final int JAVA_RUNTIME_FURNACE_LIT_ID;

    public static final int JAVA_RUNTIME_SPAWNER_ID;

    private static final int BLOCK_STATE_VERSION = 17825806;

    static {
        /* Load block palette */
        InputStream stream = FileUtils.getResource("bedrock/runtime_block_states.dat");

        NbtList<NbtMap> blocksTag;
        try (NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream)) {
            blocksTag = (NbtList<NbtMap>) nbtInputStream.readTag();
        } catch (Exception e) {
            throw new AssertionError("Unable to get blocks from runtime block states", e);
        }

        Map<NbtMap, NbtMap> blockStateMap = new HashMap<>();

        for (NbtMap tag : blocksTag) {
            if (blockStateMap.putIfAbsent(tag.getCompound("block"), tag) != null) {
                throw new AssertionError("Duplicate block states in Bedrock palette");
            }
        }

        stream = FileUtils.getResource("mappings/blocks.json");
        JsonNode blocks;
        try {
            blocks = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block mappings", e);
        }
        Object2IntMap<NbtMap> addedStatesMap = new Object2IntOpenHashMap<>();
        addedStatesMap.defaultReturnValue(-1);
        List<NbtMap> paletteList = new ArrayList<>();

        Reflections ref = new Reflections("org.geysermc.connector.network.translators.world.block.entity");
        ref.getTypesAnnotatedWith(BlockEntity.class);

        int waterRuntimeId = -1;
        int javaRuntimeId = -1;
        int bedrockRuntimeId = 0;
        int cobwebRuntimeId = -1;
        int furnaceRuntimeId = -1;
        int furnaceLitRuntimeId = -1;
        int spawnerRuntimeId = -1;
        Iterator<Map.Entry<String, JsonNode>> blocksIterator = blocks.fields();
        while (blocksIterator.hasNext()) {
            javaRuntimeId++;
            Map.Entry<String, JsonNode> entry = blocksIterator.next();
            String javaId = entry.getKey();
            NbtMap blockTag = buildBedrockState(entry.getValue());

            // TODO fix this, (no block should have a null hardness)
            JsonNode hardnessNode = entry.getValue().get("block_hardness");
            if (hardnessNode != null) {
                JAVA_RUNTIME_ID_TO_HARDNESS.put(javaRuntimeId, hardnessNode.doubleValue());
            }

            try {
                JAVA_RUNTIME_ID_TO_CAN_HARVEST_WITH_HAND.put(javaRuntimeId, entry.getValue().get("can_break_with_hand").booleanValue());
            } catch (Exception e) {
                JAVA_RUNTIME_ID_TO_CAN_HARVEST_WITH_HAND.put(javaRuntimeId, false);
            }

            JsonNode toolTypeNode = entry.getValue().get("tool_type");
            if (toolTypeNode != null) {
                JAVA_RUNTIME_ID_TO_TOOL_TYPE.put(javaRuntimeId, toolTypeNode.textValue());
            }

            if (javaId.contains("wool")) {
                JAVA_RUNTIME_WOOL_IDS.add(javaRuntimeId);
            }

            if (javaId.contains("cobweb")) {
                cobwebRuntimeId = javaRuntimeId;
            }

            JAVA_ID_BLOCK_MAP.put(javaId, javaRuntimeId);

            // Used for adding all "special" Java block states to block state map
            String identifier;
            String bedrock_identifer = entry.getValue().get("bedrock_identifier").asText();
            for (Class<?> clazz : ref.getTypesAnnotatedWith(BlockEntity.class)) {
                identifier = clazz.getAnnotation(BlockEntity.class).regex();
                // Endswith, or else the block bedrock gets picked up for bed
                if (bedrock_identifer.endsWith(identifier) && !identifier.equals("")) {
                    JAVA_ID_TO_BLOCK_ENTITY_MAP.put(javaRuntimeId, clazz.getAnnotation(BlockEntity.class).name());
                    break;
                }
            }

            BlockStateValues.storeBlockStateValues(entry, javaRuntimeId);

            // Get the tag needed for non-empty flower pots
            if (entry.getValue().get("pottable") != null) {
                BlockStateValues.getFlowerPotBlocks().put(entry.getKey().split("\\[")[0], buildBedrockState(entry.getValue()));
            }

            if (entry.getKey().contains("wall_skull") || entry.getKey().contains("wall_head")) {
                String direction = entry.getKey().substring(entry.getKey().lastIndexOf("facing=") + 7);
                BlockStateValues.getWallSkullDirection().put(javaRuntimeId, direction.substring(0, direction.length() - 1));
            }

            if ("minecraft:water[level=0]".equals(javaId)) {
                waterRuntimeId = bedrockRuntimeId;
            }
            boolean waterlogged = entry.getKey().contains("waterlogged=true")
                    || javaId.contains("minecraft:bubble_column") || javaId.contains("minecraft:kelp") || javaId.contains("seagrass");

            if (waterlogged) {
                BEDROCK_TO_JAVA_BLOCK_MAP.putIfAbsent(bedrockRuntimeId | 1 << 31, javaRuntimeId);
                WATERLOGGED.add(javaRuntimeId);
            } else {
                BEDROCK_TO_JAVA_BLOCK_MAP.putIfAbsent(bedrockRuntimeId, javaRuntimeId);
            }

            NbtMap runtimeTag = blockStateMap.remove(blockTag);
            if (runtimeTag != null) {
                addedStatesMap.put(blockTag, bedrockRuntimeId);
                paletteList.add(runtimeTag);
            } else {
                int duplicateRuntimeId = addedStatesMap.getOrDefault(blockTag, -1);
                if (duplicateRuntimeId == -1) {
                    GeyserConnector.getInstance().getLogger().debug("Mapping " + javaId + " was not found for bedrock edition!");
                } else {
                    JAVA_TO_BEDROCK_BLOCK_MAP.put(javaRuntimeId, duplicateRuntimeId);
                }
                continue;
            }
            JAVA_TO_BEDROCK_BLOCK_MAP.put(javaRuntimeId, bedrockRuntimeId);

            if (javaId.startsWith("minecraft:furnace[facing=north")) {
                if (javaId.contains("lit=true")) {
                    furnaceLitRuntimeId = javaRuntimeId;
                } else {
                    furnaceRuntimeId = javaRuntimeId;
                }
            }

            if (javaId.startsWith("minecraft:spawner")) {
                spawnerRuntimeId = javaRuntimeId;
            }

            bedrockRuntimeId++;
        }

        if (cobwebRuntimeId == -1) {
            throw new AssertionError("Unable to find cobwebs in palette");
        }
        JAVA_RUNTIME_COBWEB_ID = cobwebRuntimeId;

        if (furnaceRuntimeId == -1) {
            throw new AssertionError("Unable to find furnace in palette");
        }
        JAVA_RUNTIME_FURNACE_ID = furnaceRuntimeId;

        if (furnaceLitRuntimeId == -1) {
            throw new AssertionError("Unable to find lit furnace in palette");
        }
        JAVA_RUNTIME_FURNACE_LIT_ID = furnaceLitRuntimeId;

        if (spawnerRuntimeId == -1) {
            throw new AssertionError("Unable to find spawner in palette");
        }
        JAVA_RUNTIME_SPAWNER_ID = spawnerRuntimeId;

        if (waterRuntimeId == -1) {
            throw new AssertionError("Unable to find water in palette");
        }
        BEDROCK_WATER_ID = waterRuntimeId;

        paletteList.addAll(blockStateMap.values()); // Add any missing mappings that could crash the client

        // Loop around again to find all item frame runtime IDs
        int frameRuntimeId = 0;
        for (NbtMap tag : paletteList) {
            NbtMap blockTag = tag.getCompound("block");
            if (blockTag.getString("name").equals("minecraft:frame")) {
                ITEM_FRAMES.put(tag, frameRuntimeId);
            }
            frameRuntimeId++;
        }

        BLOCKS = new NbtList<>(NbtType.COMPOUND, paletteList);
    }

    private BlockTranslator() {
    }

    public static void init() {
        // no-op
    }

    private static NbtMap buildBedrockState(JsonNode node) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        tagBuilder.putString("name", node.get("bedrock_identifier").textValue())
                .putInt("version", BlockTranslator.BLOCK_STATE_VERSION);

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
        tagBuilder.put("states", statesBuilder.build());
        return tagBuilder.build();
    }

    public static int getBedrockBlockId(int state) {
        return JAVA_TO_BEDROCK_BLOCK_MAP.get(state);
    }

    public static int getJavaBlockState(int bedrockId) {
        return BEDROCK_TO_JAVA_BLOCK_MAP.get(bedrockId);
    }

    public static int getItemFrame(NbtMap tag) {
        return ITEM_FRAMES.getOrDefault(tag, -1);
    }

    public static boolean isItemFrame(int bedrockBlockRuntimeId) {
        return ITEM_FRAMES.values().contains(bedrockBlockRuntimeId);
    }

    public static int getBlockStateVersion() {
        return BLOCK_STATE_VERSION;
    }

    public static int getJavaBlockState(String javaId) {
        return JAVA_ID_BLOCK_MAP.get(javaId);
    }

    public static String getBlockEntityString(int javaId) {
        return JAVA_ID_TO_BLOCK_ENTITY_MAP.get(javaId);
    }

    public static boolean isWaterlogged(int state) {
        return WATERLOGGED.contains(state);
    }

    public static BiMap<String, Integer> getJavaIdBlockMap() {
        return JAVA_ID_BLOCK_MAP;
    }

    public static int getJavaWaterloggedState(int bedrockId) {
        return BEDROCK_TO_JAVA_BLOCK_MAP.get(1 << 31 | bedrockId);
    }
}
