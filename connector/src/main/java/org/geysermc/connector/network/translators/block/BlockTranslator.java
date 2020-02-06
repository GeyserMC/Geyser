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
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.utils.Toolbox;

import java.io.InputStream;
import java.util.*;

public class BlockTranslator {
    public static final ListTag<CompoundTag> BLOCKS;
    public static final BlockState AIR = new BlockState(0);

    private static final Int2IntMap JAVA_TO_BEDROCK_BLOCK_MAP = new Int2IntOpenHashMap();
    private static final Int2ObjectMap<BlockState> BEDROCK_TO_JAVA_BLOCK_MAP = new Int2ObjectOpenHashMap<>();
    private static final Int2IntMap JAVA_TO_BEDROCK_LIQUID_MAP = new Int2IntOpenHashMap();
    private static final Int2ObjectMap<BlockState> BEDROCK_TO_JAVA_LIQUID_MAP = new Int2ObjectOpenHashMap<>();

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
            if (blockStateMap.putIfAbsent(tag.getAsCompound("block"), tag) != null) {
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
        TObjectIntMap<CompoundTag> addedStatesMap = new TObjectIntHashMap<>(512, 0.5f, -1);
        List<CompoundTag> paletteList = new ArrayList<>();

        int javaRuntimeId = -1;
        int bedrockRuntimeId = 0;
        Iterator<Map.Entry<String, JsonNode>> blocksIterator = blocks.fields();
        while (blocksIterator.hasNext()) {
            javaRuntimeId++;
            Map.Entry<String, JsonNode> entry = blocksIterator.next();
            String javaId = entry.getKey();
            CompoundTag blockTag = buildBedrockState(entry.getValue());

            CompoundTag runtimeTag = blockStateMap.remove(blockTag);
            if (runtimeTag != null) {
                addedStatesMap.put(blockTag, bedrockRuntimeId);
                paletteList.add(runtimeTag);
            } else {
                int duplicateRuntimeId = addedStatesMap.get(blockTag);
                if (duplicateRuntimeId == -1) {
                    GeyserLogger.DEFAULT.debug("Mapping " + javaId + " was not found for bedrock edition!");
                } else {
                    JAVA_TO_BEDROCK_BLOCK_MAP.put(javaRuntimeId, duplicateRuntimeId);
                }
                continue;
            }
            JAVA_TO_BEDROCK_BLOCK_MAP.put(javaRuntimeId, bedrockRuntimeId);
            BEDROCK_TO_JAVA_BLOCK_MAP.put(bedrockRuntimeId, new BlockState(javaRuntimeId));

            bedrockRuntimeId++;
        }

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

    public static BlockState getJavaBlockState(int bedrockId) {
        return BEDROCK_TO_JAVA_BLOCK_MAP.get(bedrockId);
    }

    public static int getBedrockWaterLoggedId(BlockState state) {
        return JAVA_TO_BEDROCK_LIQUID_MAP.getOrDefault(state.getId(), -1);
    }

    public static BlockState getJavaLiquidState(int bedrockId) {
        return BEDROCK_TO_JAVA_LIQUID_MAP.get(bedrockId);
    }
}
