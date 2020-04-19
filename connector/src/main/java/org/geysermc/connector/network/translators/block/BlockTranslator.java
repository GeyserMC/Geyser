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

package org.geysermc.connector.network.translators.block;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.block.entity.BlockEntity;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.Toolbox;
import org.geysermc.connector.world.chunk.ChunkPosition;
import org.reflections.Reflections;

import java.io.InputStream;
import java.util.*;

public class BlockTranslator {
    public static final ListTag<CompoundTag> BLOCKS;
    public static final BlockState AIR = new BlockState(0);
    public static final int BEDROCK_WATER_ID;

    private static final Int2IntMap JAVA_TO_BEDROCK_BLOCK_MAP = new Int2IntOpenHashMap();
    private static final Int2ObjectMap<BlockState> BEDROCK_TO_JAVA_BLOCK_MAP = new Int2ObjectOpenHashMap<>();
    private static final Map<String, BlockState> JAVA_ID_BLOCK_MAP = new HashMap<>();
    private static final IntSet WATERLOGGED = new IntOpenHashSet();

    // Bedrock carpet ID, used in LlamaEntity.java for decoration
    public static final int CARPET = 171;

    private static final Map<BlockState, String> JAVA_ID_TO_BLOCK_ENTITY_MAP = new HashMap<>();
    private static final Object2ByteMap<BlockState> BED_COLORS = new Object2ByteOpenHashMap<>();
    private static final Object2ByteMap<BlockState> SKULL_VARIANTS = new Object2ByteOpenHashMap<>();
    private static final Object2ByteMap<BlockState> SKULL_ROTATIONS = new Object2ByteOpenHashMap<>();

    public static final Int2DoubleMap JAVA_RUNTIME_ID_TO_HARDNESS = new Int2DoubleOpenHashMap();
    public static final Int2BooleanMap JAVA_RUNTIME_ID_TO_CAN_HARVEST_WITH_HAND = new Int2BooleanOpenHashMap();
    public static final Int2ObjectMap<String> JAVA_RUNTIME_ID_TO_TOOL_TYPE = new Int2ObjectOpenHashMap<>();

    // For block breaking animation math
    public static final IntSet JAVA_RUNTIME_WOOL_IDS = new IntOpenHashSet();
    public static final int JAVA_RUNTIME_COBWEB_ID;

    private static final int BLOCK_STATE_VERSION = 17760256;

    static {
        /* Load block palette */
        InputStream stream = Toolbox.getResource("bedrock/runtime_block_states.dat");

        ListTag<CompoundTag> blocksTag;
        try (NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream)) {
            blocksTag = (ListTag<CompoundTag>) nbtInputStream.readTag();
        } catch (Exception e) {
            throw new AssertionError("Unable to get blocks from runtime block states", e);
        }

        Map<CompoundTag, CompoundTag> blockStateMap = new HashMap<>();

        for (CompoundTag tag : blocksTag.getValue()) {
            if (blockStateMap.putIfAbsent(tag.getCompound("block"), tag) != null) {
                throw new AssertionError("Duplicate block states in Bedrock palette");
            }
        }

        stream = Toolbox.getResource("mappings/blocks.json");
        JsonNode blocks;
        try {
            blocks = Toolbox.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block mappings", e);
        }
        Object2IntMap<CompoundTag> addedStatesMap = new Object2IntOpenHashMap<>();
        addedStatesMap.defaultReturnValue(-1);
        List<CompoundTag> paletteList = new ArrayList<>();

        Reflections ref = new Reflections("org.geysermc.connector.network.translators.block.entity");
        ref.getTypesAnnotatedWith(BlockEntity.class);

        int waterRuntimeId = -1;
        int javaRuntimeId = -1;
        int bedrockRuntimeId = 0;
        int cobwebRuntimeId = -1;
        Iterator<Map.Entry<String, JsonNode>> blocksIterator = blocks.fields();
        while (blocksIterator.hasNext()) {
            javaRuntimeId++;
            Map.Entry<String, JsonNode> entry = blocksIterator.next();
            String javaId = entry.getKey();
            BlockState javaBlockState = new BlockState(javaRuntimeId);
            CompoundTag blockTag = buildBedrockState(entry.getValue());

            // TODO fix this, (no block should have a null hardness)
            JsonNode hardnessNode = entry.getValue().get("block_hardness");
            if (hardnessNode != null) {
                JAVA_RUNTIME_ID_TO_HARDNESS.put(javaRuntimeId, hardnessNode.doubleValue());
            }

            JAVA_RUNTIME_ID_TO_CAN_HARVEST_WITH_HAND.put(javaRuntimeId, entry.getValue().get("can_break_with_hand").booleanValue());

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

            JAVA_ID_BLOCK_MAP.put(javaId, javaBlockState);

            String identifier;
            String bedrock_identifer = entry.getValue().get("bedrock_identifier").asText();
            for (Class<?> clazz : ref.getTypesAnnotatedWith(BlockEntity.class)) {
                if (clazz.getAnnotation(BlockEntity.class).delay()) {
                    identifier = clazz.getAnnotation(BlockEntity.class).regex();
                    // Endswith, or else the block bedrock gets picked up for bed
                    if (bedrock_identifer.endsWith(identifier)) {
                        System.out.println("Putting " + javaId + " on the map because of " + identifier + " with Bedrock " + bedrock_identifer + ".");
                        JAVA_ID_TO_BLOCK_ENTITY_MAP.put(javaBlockState, clazz.getAnnotation(BlockEntity.class).name());
                        break;
                    }
                }
            }


            JsonNode skullVariation = entry.getValue().get("variation");
            if(skullVariation != null) {
                SKULL_VARIANTS.put(javaBlockState, (byte) skullVariation.intValue());
            }

            JsonNode skullRotation = entry.getValue().get("skull_rotation");
            if (skullRotation != null) {
                SKULL_ROTATIONS.put(javaBlockState, (byte) skullRotation.intValue());
            }

            // If the Java ID is bed, signal that it needs a tag to show color
            // The color is in the namespace ID in Java Edition but it's a tag in Bedrock.
            JsonNode bedColor = entry.getValue().get("bed_color");
            if (bedColor != null) {
                // Converting to byte because the final tag value is a byte. bedColor.binaryValue() returns an array
                BED_COLORS.put(javaBlockState, (byte) bedColor.intValue());
            }

            if ("minecraft:water[level=0]".equals(javaId)) {
                waterRuntimeId = bedrockRuntimeId;
            }
            boolean waterlogged = entry.getKey().contains("waterlogged=true")
                    || javaId.contains("minecraft:bubble_column") || javaId.contains("minecraft:kelp") || javaId.contains("seagrass");

            if (waterlogged) {
                BEDROCK_TO_JAVA_BLOCK_MAP.putIfAbsent(bedrockRuntimeId | 1 << 31, javaBlockState);
                WATERLOGGED.add(javaRuntimeId);
            } else {
                BEDROCK_TO_JAVA_BLOCK_MAP.putIfAbsent(bedrockRuntimeId, javaBlockState);
            }

            CompoundTag runtimeTag = blockStateMap.remove(blockTag);
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

            bedrockRuntimeId++;
        }

        if (cobwebRuntimeId == -1) {
            throw new AssertionError("Unable to find cobwebs in palette");
        }
        JAVA_RUNTIME_COBWEB_ID = cobwebRuntimeId;

        if (waterRuntimeId == -1) {
            throw new AssertionError("Unable to find water in palette");
        }
        BEDROCK_WATER_ID = waterRuntimeId;

        paletteList.addAll(blockStateMap.values()); // Add any missing mappings that could crash the client

        BLOCKS = new ListTag<>("", CompoundTag.class, paletteList);
    }

    private BlockTranslator() {
    }

    public static void init() {
        // no-op
    }

    private static CompoundTag buildBedrockState(JsonNode node) {
        CompoundTagBuilder tagBuilder = CompoundTag.builder();
        tagBuilder.stringTag("name", node.get("bedrock_identifier").textValue())
                .intTag("version", BlockTranslator.BLOCK_STATE_VERSION);

        CompoundTagBuilder statesBuilder = CompoundTag.builder();

        // check for states
        if (node.has("bedrock_states")) {
            Iterator<Map.Entry<String, JsonNode>> statesIterator = node.get("bedrock_states").fields();

            while (statesIterator.hasNext()) {
                Map.Entry<String, JsonNode> stateEntry = statesIterator.next();
                JsonNode stateValue = stateEntry.getValue();
                switch (stateValue.getNodeType()) {
                    case BOOLEAN:
                        statesBuilder.booleanTag(stateEntry.getKey(), stateValue.booleanValue());
                        continue;
                    case STRING:
                        statesBuilder.stringTag(stateEntry.getKey(), stateValue.textValue());
                        continue;
                    case NUMBER:
                        statesBuilder.intTag(stateEntry.getKey(), stateValue.intValue());
                }
            }
        }
        return tagBuilder.tag(statesBuilder.build("states")).build("block");
    }

    public static int getBedrockBlockId(BlockState state) {
        return JAVA_TO_BEDROCK_BLOCK_MAP.get(state.getId());
    }

    public static int getBedrockBlockId(int javaId) {
        return JAVA_TO_BEDROCK_BLOCK_MAP.get(javaId);
    }

    public static BlockState getJavaBlockState(int bedrockId) {
        return BEDROCK_TO_JAVA_BLOCK_MAP.get(bedrockId);
    }

    public static BlockState getJavaBlockState(String javaId) {
        return JAVA_ID_BLOCK_MAP.get(javaId);
    }

    public static String getBlockEntityString(BlockState javaId) {
        return JAVA_ID_TO_BLOCK_ENTITY_MAP.get(javaId);
    }

    public static boolean isWaterlogged(BlockState state) {
        return WATERLOGGED.contains(state.getId());
    }

    public static byte getBedColor(BlockState state) {
        if (BED_COLORS.containsKey(state)) {
            return BED_COLORS.getByte(state);
        }
        return -1;
    }

    public static byte getSkullVariant(BlockState state) {
        if (SKULL_VARIANTS.containsKey(state)) {
            return SKULL_VARIANTS.getByte(state);
        }
        return -1;
    }

    public static byte getSkullRotation(BlockState state) {
        if (SKULL_ROTATIONS.containsKey(state)) {
            return SKULL_ROTATIONS.getByte(state);
        }
        return -1;
    }

    public static BlockState getJavaWaterloggedState(int bedrockId) {
        return BEDROCK_TO_JAVA_BLOCK_MAP.get(1 << 31 | bedrockId);
    }
}
