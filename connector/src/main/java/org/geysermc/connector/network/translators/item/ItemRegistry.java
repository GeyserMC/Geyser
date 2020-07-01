/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.item;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Registry for anything item related.
 */
public class ItemRegistry {

    private static final Map<String, ItemEntry> JAVA_IDENTIFIER_MAP = new HashMap<>();

    public static ItemData[] CREATIVE_ITEMS;

    public static final List<StartGamePacket.ItemEntry> ITEMS = new ArrayList<>();
    public static final Int2ObjectMap<ItemEntry> ITEM_ENTRIES = new Int2ObjectOpenHashMap<>();

    // Shield ID, used in Entity.java
    public static final int SHIELD = 829;
    // Boat ID, used in BedrockInventoryTransactionTranslator.java
    public static final int BOAT = 333;

    public static int BARRIER_INDEX = 0;

    public static void init() {
        /* Load item palette */
        InputStream stream = FileUtils.getResource("data/items.json");

        TypeReference<List<JsonNode>> itemEntriesType = new TypeReference<List<JsonNode>>() {
        };

        List<JsonNode> itemEntries;
        try {
            itemEntries = GeyserConnector.JSON_MAPPER.readValue(stream, itemEntriesType);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
        }

        for (JsonNode entry : itemEntries) {
            ITEMS.add(new StartGamePacket.ItemEntry(entry.get("name").textValue(), (short) entry.get("id").intValue()));
        }

        stream = FileUtils.getResource("mappings/items.json");

        JsonNode items;
        try {
            items = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java runtime item IDs", e);
        }

        int itemIndex = 0;
        Iterator<Map.Entry<String, JsonNode>> iterator = items.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().has("tool_type")) {
                if (entry.getValue().has("tool_tier")) {
                    ITEM_ENTRIES.put(itemIndex, new ToolItemEntry(
                            entry.getKey(), itemIndex,
                            entry.getValue().get("bedrock_id").intValue(),
                            entry.getValue().get("bedrock_data").intValue(),
                            entry.getValue().get("tool_type").textValue(),
                            entry.getValue().get("tool_tier").textValue(),
                            entry.getValue().get("is_block") != null && entry.getValue().get("is_block").booleanValue()));
                } else {
                    ITEM_ENTRIES.put(itemIndex, new ToolItemEntry(
                            entry.getKey(), itemIndex,
                            entry.getValue().get("bedrock_id").intValue(),
                            entry.getValue().get("bedrock_data").intValue(),
                            entry.getValue().get("tool_type").textValue(),
                            "",
                            entry.getValue().get("is_block").booleanValue()));
                }
            } else {
                ITEM_ENTRIES.put(itemIndex, new ItemEntry(
                        entry.getKey(), itemIndex,
                        entry.getValue().get("bedrock_id").intValue(),
                        entry.getValue().get("bedrock_data").intValue(),
                        entry.getValue().get("is_block") != null && entry.getValue().get("is_block").booleanValue()));
            }
            if (entry.getKey().equals("minecraft:barrier")) {
                BARRIER_INDEX = itemIndex;
            }

            itemIndex++;
        }

        // Add the loadstonecompass since it doesn't exist on java but we need it for item conversion
        ITEM_ENTRIES.put(itemIndex, new ItemEntry("minecraft:lodestonecompass", itemIndex, 741, 0, false));

        /* Load creative items */
        stream = FileUtils.getResource("data/creative_items.json");

        JsonNode creativeItemEntries;
        try {
            creativeItemEntries = GeyserConnector.JSON_MAPPER.readTree(stream).get("items");
        } catch (Exception e) {
            throw new AssertionError("Unable to load creative items", e);
        }

        List<ItemData> creativeItems = new ArrayList<>();
        for (JsonNode itemNode : creativeItemEntries) {
            short damage = 0;
            if (itemNode.has("damage")) {
                damage = itemNode.get("damage").numberValue().shortValue();
            }
            if (itemNode.has("nbt_b64")) {
                byte[] bytes = Base64.getDecoder().decode(itemNode.get("nbt_b64").asText());
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                try {
                    com.nukkitx.nbt.tag.CompoundTag tag = (com.nukkitx.nbt.tag.CompoundTag) NbtUtils.createReaderLE(bais).readTag();
                    creativeItems.add(ItemData.of(itemNode.get("id").asInt(), damage, 1, tag));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                creativeItems.add(ItemData.of(itemNode.get("id").asInt(), damage, 1));
            }
        }
        CREATIVE_ITEMS = creativeItems.toArray(new ItemData[0]);
    }

    /**
     * Gets an {@link ItemEntry} from the given {@link ItemStack}.
     *
     * @param stack the item stack
     * @return an item entry from the given item stack
     */
    public static ItemEntry getItem(ItemStack stack) {
        return ITEM_ENTRIES.get(stack.getId());
    }

    /**
     * Gets an {@link ItemEntry} from the given {@link ItemData}.
     *
     * @param data the item data
     * @return an item entry from the given item data
     */
    public static ItemEntry getItem(ItemData data) {
        for (ItemEntry itemEntry : ITEM_ENTRIES.values()) {
            if (itemEntry.getBedrockId() == data.getId() && (itemEntry.getBedrockData() == data.getDamage() || itemEntry.getJavaIdentifier().endsWith("potion"))) {
                return itemEntry;
            }
        }
        // If item find was unsuccessful first time, we try again while ignoring damage
        // Fixes piston, sticky pistons, dispensers and droppers turning into air from creative inventory
        for (ItemEntry itemEntry : ITEM_ENTRIES.values()) {
            if (itemEntry.getBedrockId() == data.getId()) {
                return itemEntry;
            }
        }

        // This will hide the message when the player clicks with an empty hand
        if (data.getId() != 0 && data.getDamage() != 0) {
            GeyserConnector.getInstance().getLogger().debug("Missing mapping for bedrock item " + data.getId() + ":" + data.getDamage());
        }
        return ItemEntry.AIR;
    }

    /**
     * Gets an {@link ItemEntry} from the given Minecraft: Java Edition
     * block state identifier.
     *
     * @param javaIdentifier the block state identifier
     * @return an item entry from the given java edition identifier
     */
    public static ItemEntry getItemEntry(String javaIdentifier) {
        return JAVA_IDENTIFIER_MAP.computeIfAbsent(javaIdentifier, key -> ITEM_ENTRIES.values()
                .stream().filter(itemEntry -> itemEntry.getJavaIdentifier().equals(key)).findFirst().orElse(null));
    }
}
