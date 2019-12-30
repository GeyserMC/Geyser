package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.ListTag;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.io.InputStream;
import java.util.*;

public class Toolbox {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);

    public static final Collection<StartGamePacket.ItemEntry> ITEMS = new ArrayList<>();
    public static final ListTag<CompoundTag> BLOCKS;

    public static final Int2ObjectMap<ItemEntry> ITEM_ENTRIES = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<BlockEntry> BLOCK_ENTRIES = new Int2ObjectOpenHashMap<>();
    public static final Int2IntMap JAVA_TO_BEDROCK_BLOCK_MAP = new Int2IntOpenHashMap();
    public static final Int2IntMap JAVA_TO_BEDROCK_LIQUID_MAP = new Int2IntOpenHashMap();

    static {

        /* Load block palette */
        InputStream stream = getResource("bedrock/runtime_block_states.dat");

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

        stream = getResource("mappings/blocks.json");
        JsonNode blocks;
        try {
            blocks = JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block mappings", e);
        }
        TObjectIntMap<CompoundTag> stateRuntimeMap = new TObjectIntHashMap<>(512, 0.5f, -1);
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
                stateRuntimeMap.put(blockTag, javaRuntimeId);
                paletteList.add(runtimeTag);
            } else {
                int duplicateRuntimeId = stateRuntimeMap.get(blockTag);
                if (duplicateRuntimeId == -1) {
                    GeyserLogger.DEFAULT.debug("Mapping " + javaId + " was not found for bedrock edition!");
                } else {
                    JAVA_TO_BEDROCK_BLOCK_MAP.put(javaRuntimeId, duplicateRuntimeId);
                }
                continue;
            }
            JAVA_TO_BEDROCK_BLOCK_MAP.put(javaRuntimeId, bedrockRuntimeId);

            bedrockRuntimeId++;
        }

        BLOCKS = new ListTag<>("", CompoundTag.class, paletteList);

        /* Load item palette */
        stream = getResource("bedrock/items.json");

        TypeReference<List<JsonNode>> itemEntriesType = new TypeReference<List<JsonNode>>() {
        };

        List<JsonNode> itemEntries;
        try {
            itemEntries = JSON_MAPPER.readValue(stream, itemEntriesType);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
        }

        for (JsonNode entry : itemEntries) {
            ITEMS.add(new StartGamePacket.ItemEntry(entry.get("name").textValue(), (short) entry.get("id").intValue()));
        }

        stream = getResource("mappings/items.json");

        JsonNode items;
        try {
            items = JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java runtime item IDs", e);
        }

        int itemIndex = 0;
        Iterator<Map.Entry<String, JsonNode>> iterator = items.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            ITEM_ENTRIES.put(itemIndex, new ItemEntry(entry.getKey(), itemIndex,
                    entry.getValue().get("bedrock_id").intValue(), entry.getValue().get("bedrock_data").intValue()));
            itemIndex++;
        }
    }

    private static CompoundTag buildBedrockState(JsonNode node) {
        CompoundTagBuilder tagBuilder = CompoundTag.builder();
        tagBuilder.stringTag("name", node.get("bedrock_identifier").textValue());

        // check for states
        if (node.has("bedrock_states")) {
            Iterator<Map.Entry<String, JsonNode>> statesIterator = node.get("bedrock_states").fields();

            while (statesIterator.hasNext()) {
                Map.Entry<String, JsonNode> stateEntry = statesIterator.next();
                JsonNode stateValue = stateEntry.getValue();
                switch (stateValue.getNodeType()) {
                    case BOOLEAN:
                        tagBuilder.booleanTag(stateEntry.getKey(), stateValue.booleanValue());
                        continue;
                    case STRING:
                        tagBuilder.stringTag(stateEntry.getKey(), stateValue.textValue());
                        continue;
                    case NUMBER:
                        tagBuilder.intTag(stateEntry.getKey(), stateValue.intValue());
                }
            }
        }
        return tagBuilder.build("block");
    }

    private static InputStream getResource(String resource) {
        InputStream stream = Toolbox.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new AssertionError("Unable to find resource: " + resource);
        }
        return stream;
    }

    public static void init() {
        // no-op
    }
}