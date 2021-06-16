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

package org.geysermc.connector.network.translators.item;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.google.common.collect.ImmutableSet;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.effect.EffectRegistry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator1_17_0;
import org.geysermc.connector.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Registry for anything item related.
 */
public class ItemRegistry {

    private static final Map<String, ItemEntry> JAVA_IDENTIFIER_MAP = new HashMap<>();

    /**
     * A list of all identifiers that only exist on Java. Used to prevent creative items from becoming these unintentionally.
     */
    private static final Set<String> JAVA_ONLY_ITEMS;

    public static final ItemData[] CREATIVE_ITEMS;

    public static final List<StartGamePacket.ItemEntry> ITEMS = new ArrayList<>();
    public static final Int2ObjectMap<ItemEntry> ITEM_ENTRIES = new Int2ObjectOpenHashMap<>();

    /**
     * A list of all Java item names.
     */
    public static final String[] ITEM_NAMES;

    /**
     * Bamboo item entry, used in PandaEntity.java
     */
    public static ItemEntry BAMBOO;
    /**
     * Banner item entry, used in LivingEntity.java
     */
    public static ItemEntry BANNER;
    /**
     * Boat item entries, used in BedrockInventoryTransactionTranslator.java
     */
    public static final IntSet BOATS = new IntArraySet();
    /**
     * Bucket item entries (excluding the milk bucket), used in BedrockInventoryTransactionTranslator.java
     */
    public static final IntSet BUCKETS = new IntArraySet();
    /**
     * Carpet item data, used in LlamaEntity.java
     */
    public static final List<ItemData> CARPETS = new ArrayList<>(16);
    /**
     * Crossbow item entry, used in PillagerEntity.java
     */
    public static ItemEntry CROSSBOW;
    /**
     * Empty item bucket, used in BedrockInventoryTransactionTranslator.java
     */
    public static ItemEntry MILK_BUCKET;
    /**
     * Egg item entry, used in JavaEntityStatusTranslator.java
     */
    public static ItemEntry EGG;
    /**
     * Shield item entry, used in Entity.java and LivingEntity.java
     */
    public static ItemEntry SHIELD;
    /**
     * Wheat item entry, used in AbstractHorseEntity.java
     */
    public static ItemEntry WHEAT;
    /**
     * Writable book item entry, used in BedrockBookEditTranslator.java
     */
    public static ItemEntry WRITABLE_BOOK;

    public static int BARRIER_INDEX = 0;

    /**
     * Stores the properties and data of the "custom" furnace minecart item.
     */
    public static final ComponentItemData FURNACE_MINECART_DATA;

    public static void init() {
        // no-op
    }

    static {
        /* Load item palette */
        InputStream stream = FileUtils.getResource("bedrock/runtime_item_states.json");

        TypeReference<List<JsonNode>> itemEntriesType = new TypeReference<List<JsonNode>>() {
        };

        // Used to get the Bedrock namespaced ID (in instances where there are small differences)
        Int2ObjectMap<String> bedrockIdToIdentifier = new Int2ObjectOpenHashMap<>();

        List<String> itemNames = new ArrayList<>();

        List<JsonNode> itemEntries;
        try {
            itemEntries = GeyserConnector.JSON_MAPPER.readValue(stream, itemEntriesType);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
        }

        int lodestoneCompassId = 0;

        for (JsonNode entry : itemEntries) {
            ITEMS.add(new StartGamePacket.ItemEntry(entry.get("name").textValue(), (short) entry.get("id").intValue()));
            bedrockIdToIdentifier.put(entry.get("id").intValue(), entry.get("name").textValue());
            if (entry.get("name").textValue().equals("minecraft:lodestone_compass")) {
                lodestoneCompassId = entry.get("id").intValue();
            }
        }

        Object2IntMap<String> bedrockBlockIdOverrides = new Object2IntOpenHashMap<>();
        Object2IntMap<String> blacklistedIdentifiers = new Object2IntOpenHashMap<>();

        // Load creative items
        // We load this before item mappings to get overridden block runtime ID mappings
        stream = FileUtils.getResource("bedrock/creative_items.json");

        JsonNode creativeItemEntries;
        try {
            creativeItemEntries = GeyserConnector.JSON_MAPPER.readTree(stream).get("items");
        } catch (Exception e) {
            throw new AssertionError("Unable to load creative items", e);
        }

        int netId = 1;
        List<ItemData> creativeItems = new ArrayList<>();
        for (JsonNode itemNode : creativeItemEntries) {
            int count = 1;
            int damage = 0;
            int blockRuntimeId = 0;
            NbtMap tag = null;
            JsonNode damageNode = itemNode.get("damage");
            if (damageNode != null) {
                damage = damageNode.asInt();
            }
            JsonNode countNode = itemNode.get("count");
            if (countNode != null) {
                count = countNode.asInt();
            }
            JsonNode blockRuntimeIdNode = itemNode.get("blockRuntimeId");
            if (blockRuntimeIdNode != null) {
                blockRuntimeId = blockRuntimeIdNode.asInt();
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

            String identifier = itemNode.get("id").textValue();
            int id = -1;
            for (StartGamePacket.ItemEntry itemEntry : ITEMS) {
                if (itemEntry.getIdentifier().equals(identifier)) {
                    id = itemEntry.getId();
                    break;
                }
            }
            if (id == -1) {
                throw new RuntimeException("Unable to find matching Bedrock item for " + identifier);
            }

            creativeItems.add(ItemData.builder()
                    .id(id)
                    .damage(damage)
                    .count(count)
                    .blockRuntimeId(blockRuntimeId)
                    .tag(tag)
                    .netId(netId++).build());

            if (blockRuntimeId != 0) {
                // Add override for item mapping, unless it already exists... then we know multiple states can exist
                if (!blacklistedIdentifiers.containsKey(identifier)) {
                    if (bedrockBlockIdOverrides.containsKey(identifier)) {
                        bedrockBlockIdOverrides.remove(identifier);
                        // Save this as a blacklist, but also as knowledge of what the block state name should be
                        blacklistedIdentifiers.put(identifier, blockRuntimeId);
                    } else {
                        // Unless there's multiple possibilities for this one state, let this be
                        bedrockBlockIdOverrides.put(identifier, blockRuntimeId);
                    }
                }
            }
        }

        // Load item mappings from Java Edition to Bedrock Edition
        stream = FileUtils.getResource("mappings/items.json");

        JsonNode items;
        try {
            items = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java runtime item IDs", e);
        }

        BlockTranslator blockTranslator = BlockTranslator1_17_0.INSTANCE;

        int itemIndex = 0;
        int javaFurnaceMinecartId = 0;
        boolean usingFurnaceMinecart = GeyserConnector.getInstance().getConfig().isAddNonBedrockItems();
        Iterator<Map.Entry<String, JsonNode>> iterator = items.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (usingFurnaceMinecart && entry.getKey().equals("minecraft:furnace_minecart")) {
                javaFurnaceMinecartId = itemIndex;
                itemIndex++;
                continue;
            }
            int bedrockId = entry.getValue().get("bedrock_id").intValue();
            String bedrockIdentifier = bedrockIdToIdentifier.get(bedrockId);
            if (bedrockIdentifier == null) {
                throw new RuntimeException("Missing Bedrock ID in mappings!: " + bedrockId);
            }
            JsonNode stackSizeNode = entry.getValue().get("stack_size");
            int stackSize = stackSizeNode == null ? 64 : stackSizeNode.intValue();

            int bedrockBlockId = -1;
            JsonNode blockRuntimeIdNode = entry.getValue().get("blockRuntimeId");
            if (blockRuntimeIdNode != null) {
                int blockIdOverride = bedrockBlockIdOverrides.getOrDefault(bedrockIdentifier, -1);
                if (blockIdOverride != -1) {
                    // Straight from BDS is our best chance of getting an item that doesn't run into issues
                    bedrockBlockId = blockIdOverride;
                } else {
                    // Try to get an example block runtime ID from the creative contents packet, for Bedrock identifier obtaining
                    int aValidBedrockBlockId = blacklistedIdentifiers.getOrDefault(bedrockIdentifier, -1);
                    if (aValidBedrockBlockId == -1) {
                        // Fallback
                        bedrockBlockId = blockTranslator.getBedrockBlockId(blockRuntimeIdNode.intValue());
                    } else {
                        // As of 1.16.220, every item requires a block runtime ID attached to it.
                        // This is mostly for identifying different blocks with the same item ID - wool, slabs, some walls.
                        // However, in order for some visuals and crafting to work, we need to send the first matching block state
                        // as indexed by Bedrock's block palette
                        // There are exceptions! But, ideally, the block ID override should take care of those.
                        String javaBlockIdentifier = BlockTranslator.getBlockMapping(blockRuntimeIdNode.intValue()).getCleanJavaIdentifier();
                        NbtMapBuilder requiredBlockStatesBuilder = NbtMap.builder();
                        String correctBedrockIdentifier = blockTranslator.getAllBedrockBlockStates().get(aValidBedrockBlockId).getString("name");
                        boolean firstPass = true;
                        for (Map.Entry<String, Integer> blockEntry : BlockTranslator.getJavaIdBlockMap().entrySet()) {
                            if (blockEntry.getKey().split("\\[")[0].equals(javaBlockIdentifier)) {
                                int bedrockBlockRuntimeId = blockTranslator.getBedrockBlockId(blockEntry.getValue());
                                NbtMap blockTag = blockTranslator.getAllBedrockBlockStates().get(bedrockBlockRuntimeId);
                                String bedrockName = blockTag.getString("name");
                                if (!bedrockName.equals(correctBedrockIdentifier)) {
                                    continue;
                                }
                                NbtMap states = blockTag.getCompound("states");

                                if (firstPass) {
                                    firstPass = false;
                                    if (states.size() == 0) {
                                        // No need to iterate and find all block states - this is the one, as there can't be any others
                                        bedrockBlockId = bedrockBlockRuntimeId;
                                        break;
                                    }
                                    requiredBlockStatesBuilder.putAll(states);
                                    continue;
                                }
                                for (Map.Entry<String, Object> nbtEntry : states.entrySet()) {
                                    Object value = requiredBlockStatesBuilder.get(nbtEntry.getKey());
                                    if (value != null && !nbtEntry.getValue().equals(value)) { // Null means this value has already been removed/deemed as unneeded
                                        // This state can change between different block states, and therefore is not required
                                        // to build a successful block state of this
                                        requiredBlockStatesBuilder.remove(nbtEntry.getKey());
                                    }
                                }
                                if (requiredBlockStatesBuilder.size() == 0) {
                                    // There are no required block states
                                    // E.G. there was only a direction property that is no longer in play
                                    // (States that are important include color for glass)
                                    break;
                                }
                            }
                        }

                        NbtMap requiredBlockStates = requiredBlockStatesBuilder.build();
                        if (bedrockBlockId == -1) {
                            int i = -1;
                            // We need to loop around again (we can't cache the block tags above) because Bedrock can include states that we don't have a pairing for
                            // in it's "preferred" block state - I.E. the first matching block state in the list
                            for (NbtMap blockTag : blockTranslator.getAllBedrockBlockStates()) {
                                i++;
                                if (blockTag.getString("name").equals(correctBedrockIdentifier)) {
                                    NbtMap states = blockTag.getCompound("states");
                                    boolean valid = true;
                                    for (Map.Entry<String, Object> nbtEntry : requiredBlockStates.entrySet()) {
                                        if (!states.get(nbtEntry.getKey()).equals(nbtEntry.getValue())) {
                                            // A required block state doesn't match - this one is not valid
                                            valid = false;
                                            break;
                                        }
                                    }
                                    if (valid) {
                                        bedrockBlockId = i;
                                        break;
                                    }
                                }
                            }
                            if (bedrockBlockId == -1) {
                                throw new RuntimeException("Could not find a block match for " + entry.getKey());
                            }
                        }

                        // Because we have replaced the Bedrock block ID, we also need to replace the creative contents block runtime ID
                        // That way, creative items work correctly for these blocks
                        for (int j = 0; j < creativeItems.size(); j++) {
                            ItemData itemData = creativeItems.get(j);
                            if (itemData.getId() == bedrockId) {
                                if (itemData.getDamage() != 0) {
                                    break;
                                }
                                NbtMap states = blockTranslator.getAllBedrockBlockStates().get(itemData.getBlockRuntimeId()).getCompound("states");
                                boolean valid = true;
                                for (Map.Entry<String, Object> nbtEntry : requiredBlockStates.entrySet()) {
                                    if (!states.get(nbtEntry.getKey()).equals(nbtEntry.getValue())) {
                                        // A required block state doesn't match - this one is not valid
                                        valid = false;
                                        break;
                                    }
                                }
                                if (valid) {
                                    creativeItems.set(j, itemData.toBuilder().blockRuntimeId(bedrockBlockId).build());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            ItemEntry itemEntry;
            if (entry.getValue().has("tool_type")) {
                if (entry.getValue().has("tool_tier")) {
                    itemEntry = new ToolItemEntry(
                            entry.getKey(), bedrockIdentifier, itemIndex, bedrockId,
                            entry.getValue().get("bedrock_data").intValue(),
                            entry.getValue().get("tool_type").textValue(),
                            entry.getValue().get("tool_tier").textValue(),
                            bedrockBlockId,
                            stackSize);
                } else {
                    itemEntry = new ToolItemEntry(
                            entry.getKey(), bedrockIdentifier, itemIndex, bedrockId,
                            entry.getValue().get("bedrock_data").intValue(),
                            entry.getValue().get("tool_type").textValue(),
                            "", bedrockBlockId,
                            stackSize);
                }
            } else if (entry.getKey().equals("minecraft:spectral_arrow") || entry.getKey().equals("minecraft:knowledge_book")
            // To remove later... hopefully
            || entry.getKey().contains("candle") || entry.getKey().equals("minecraft:bundle") || entry.getKey().equals("minecraft:sculk_sensor")) {
                // These items don't exist on Bedrock, so set up a container that indicates they should have custom names
                itemEntry = new TranslatableItemEntry(
                        entry.getKey(), bedrockIdentifier, itemIndex, bedrockId,
                        entry.getValue().get("bedrock_data").intValue(),
                        bedrockBlockId,
                        stackSize);
                GeyserConnector.getInstance().getLogger().debug("Adding " + entry.getKey() + " as an item that needs to be translated.");
            } else {
                itemEntry = new ItemEntry(
                        entry.getKey(), bedrockIdentifier, itemIndex, bedrockId,
                        entry.getValue().get("bedrock_data").intValue(),
                        bedrockBlockId,
                        stackSize);
            }
            ITEM_ENTRIES.put(itemIndex, itemEntry);

            switch (entry.getKey()) {
                case "minecraft:barrier":
                    BARRIER_INDEX = itemIndex;
                    break;
                case "minecraft:bamboo":
                    BAMBOO = itemEntry;
                    break;
                case "minecraft:crossbow":
                    CROSSBOW = itemEntry;
                    break;
                case "minecraft:egg":
                    EGG = itemEntry;
                    break;
                case "minecraft:shield":
                    SHIELD = itemEntry;
                    break;
                case "minecraft:milk_bucket":
                    MILK_BUCKET = itemEntry;
                    break;
                case "minecraft:wheat":
                    WHEAT = itemEntry;
                    break;
                case "minecraft:white_banner": // As of 1.16.220, all banners share the same Bedrock ID and differ their colors through their damage value
                    BANNER = itemEntry;
                    break;
                case "minecraft:writable_book":
                    WRITABLE_BOOK = itemEntry;
                    break;
                default:
                    break;
            }

            if (entry.getKey().contains("boat")) {
                BOATS.add(entry.getValue().get("bedrock_id").intValue());
            } else if (entry.getKey().contains("bucket") && !entry.getKey().contains("milk")) {
                BUCKETS.add(entry.getValue().get("bedrock_id").intValue());
            } else if (entry.getKey().contains("_carpet") && !entry.getKey().contains("moss")) {
                // This should be the numerical order Java sends as an integer value for llamas
                CARPETS.add(ItemData.builder()
                        .id(itemEntry.getBedrockId())
                        .damage(itemEntry.getBedrockData())
                        .count(1)
                        .blockRuntimeId(itemEntry.getBedrockBlockId()).build());
            } else if (entry.getKey().startsWith("minecraft:music_disc_")) {
                // The Java record level event uses the item ID as the "key" to play the record
                EffectRegistry.RECORDS.put(itemIndex, SoundEvent.valueOf("RECORD_" +
                        entry.getKey().replace("minecraft:music_disc_", "").toUpperCase(Locale.ENGLISH)));
            }

            itemNames.add(entry.getKey());

            itemIndex++;
        }

        itemNames.add("minecraft:furnace_minecart");

        if (lodestoneCompassId == 0) {
            throw new RuntimeException("Lodestone compass not found in item palette!");
        }

        // Add the loadstone compass since it doesn't exist on java but we need it for item conversion
        ITEM_ENTRIES.put(itemIndex, new ItemEntry("minecraft:lodestone_compass", "minecraft:lodestone_compass", itemIndex,
                lodestoneCompassId, 0, -1, 1));

        if (usingFurnaceMinecart) {
            // Add the furnace minecart as a custom item
            int furnaceMinecartId = ITEMS.size() + 1;

            ITEMS.add(new StartGamePacket.ItemEntry("geysermc:furnace_minecart", (short) furnaceMinecartId, true));
            ITEM_ENTRIES.put(javaFurnaceMinecartId, new ItemEntry("minecraft:furnace_minecart", "geysermc:furnace_minecart", javaFurnaceMinecartId,
                    furnaceMinecartId, 0, -1, 1));
            creativeItems.add(ItemData.builder()
                    .netId(netId)
                    .id(furnaceMinecartId)
                    .count(1).build());

            NbtMapBuilder builder = NbtMap.builder();
            builder.putString("name", "geysermc:furnace_minecart")
                    .putInt("id", furnaceMinecartId);

            NbtMapBuilder componentBuilder = NbtMap.builder();
            // Conveniently, as of 1.16.200, the furnace minecart has a texture AND translation string already.
            componentBuilder.putCompound("minecraft:icon", NbtMap.builder().putString("texture", "minecart_furnace").build());
            componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", "item.minecartFurnace.name").build());

            // Indicate that the arm animation should play on rails
            List<NbtMap> useOnTag = Collections.singletonList(NbtMap.builder().putString("tags", "q.any_tag('rail')").build());
            componentBuilder.putCompound("minecraft:entity_placer", NbtMap.builder()
                    .putList("dispense_on", NbtType.COMPOUND, useOnTag)
                    .putString("entity", "minecraft:minecart")
                    .putList("use_on", NbtType.COMPOUND, useOnTag)
            .build());

            NbtMapBuilder itemProperties = NbtMap.builder();
            // We always want to allow offhand usage when we can - matches Java Edition
            itemProperties.putBoolean("allow_off_hand", true);
            itemProperties.putBoolean("hand_equipped", false);
            itemProperties.putInt("max_stack_size", 1);
            itemProperties.putString("creative_group", "itemGroup.name.minecart");
            itemProperties.putInt("creative_category", 4); // 4 - "Items"

            componentBuilder.putCompound("item_properties", itemProperties.build());
            builder.putCompound("components", componentBuilder.build());
            FURNACE_MINECART_DATA = new ComponentItemData("geysermc:furnace_minecart", builder.build());
        } else {
            FURNACE_MINECART_DATA = null;
        }

        CREATIVE_ITEMS = creativeItems.toArray(new ItemData[0]);

        ITEM_NAMES = itemNames.toArray(new String[0]);

        Set<String> javaOnlyItems = new ObjectOpenHashSet<>();
        Collections.addAll(javaOnlyItems, "minecraft:spectral_arrow", "minecraft:debug_stick",
                "minecraft:knowledge_book", "minecraft:tipped_arrow", "minecraft:trader_llama_spawn_egg",
                // To be removed in Bedrock 1.17.10... right??? RIGHT???
                "minecraft:candle", "minecraft:white_candle", "minecraft:orange_candle", "minecraft:magenta_candle",
                "minecraft:light_blue_candle", "minecraft:yellow_candle", "minecraft:lime_candle", "minecraft:pink_candle",
                "minecraft:gray_candle", "minecraft:light_gray_candle", "minecraft:cyan_candle", "minecraft:purple_candle",
                "minecraft:blue_candle", "minecraft:brown_candle", "minecraft:green_candle", "minecraft:red_candle", "minecraft:black_candle",
                "minecraft:bundle", "minecraft:sculk_sensor");
        if (!usingFurnaceMinecart) {
            javaOnlyItems.add("minecraft:furnace_minecart");
        }
        JAVA_ONLY_ITEMS = ImmutableSet.copyOf(javaOnlyItems);
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
        boolean isBlock = data.getBlockRuntimeId() != 0;
        boolean hasDamage = data.getDamage() != 0;

        for (ItemEntry itemEntry : ITEM_ENTRIES.values()) {
            if (itemEntry.getBedrockId() == data.getId()) {
                if (isBlock && !hasDamage) { // Pre-1.16.220 will not use block runtime IDs at all, so we shouldn't check either
                    if (data.getBlockRuntimeId() != itemEntry.getBedrockBlockId()) {
                        continue;
                    }
                } else {
                    if (!(itemEntry.getBedrockData() == data.getDamage() ||
                            // Make exceptions for potions and tipped arrows, whose damage values can vary
                            (itemEntry.getJavaIdentifier().endsWith("potion") || itemEntry.getJavaIdentifier().equals("minecraft:arrow")))) {
                        continue;
                    }
                }
                if (!JAVA_ONLY_ITEMS.contains(itemEntry.getJavaIdentifier())) {
                    // From a Bedrock item data, we aren't getting one of these items
                    return itemEntry;
                }
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
        return JAVA_IDENTIFIER_MAP.computeIfAbsent(javaIdentifier, key -> {
            for (ItemEntry entry : ITEM_ENTRIES.values()) {
                if (entry.getJavaIdentifier().equals(key)) {
                    return entry;
                }
            }
            return null;
        });
    }
}
