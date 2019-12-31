package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.io.InputStream;
import java.util.*;

public class Toolbox {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);

    public static final Collection<StartGamePacket.ItemEntry> ITEMS = new ArrayList<>();

    public static final Int2ObjectMap<ItemEntry> ITEM_ENTRIES = new Int2ObjectOpenHashMap<>();

    static {
        /* Load item palette */
        InputStream stream = getResource("bedrock/items.json");

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

    public static InputStream getResource(String resource) {
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