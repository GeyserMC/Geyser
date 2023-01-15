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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v560.Bedrock_v560;
import it.unimi.dsi.fastutil.ints.*;
import com.nukkitx.protocol.bedrock.v527.Bedrock_v527;
import com.nukkitx.protocol.bedrock.v534.Bedrock_v534;
import com.nukkitx.protocol.bedrock.v544.Bedrock_v544;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.event.type.GeyserDefineCustomItemsEventImpl;
import org.geysermc.geyser.inventory.item.StoredItemMappings;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.*;
import org.geysermc.geyser.util.ItemUtils;
import org.geysermc.geyser.util.collection.FixedInt2IntMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Populates the item registries.
 */
public class ItemRegistryPopulator {

    private record PaletteVersion(int protocolVersion, Map<String, String> additionalTranslatedItems) {
    }

    public static void populate() {
        Map<String, PaletteVersion> paletteVersions = new Object2ObjectOpenHashMap<>();
        paletteVersions.put("1_19_20", new PaletteVersion(Bedrock_v544.V544_CODEC.getProtocolVersion(), Collections.emptyMap()));
        paletteVersions.put("1_19_50", new PaletteVersion(Bedrock_v560.V560_CODEC.getProtocolVersion(), Collections.emptyMap()));

        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();

        TypeReference<Map<String, GeyserMappingItem>> mappingItemsType = new TypeReference<>() { };

        Map<String, GeyserMappingItem> items;
        try (InputStream stream = bootstrap.getResource("mappings/items.json")) {
            // Load item mappings from Java Edition to Bedrock Edition
            items = GeyserImpl.JSON_MAPPER.readValue(stream, mappingItemsType);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java runtime item IDs", e);
        }

        boolean customItemsAllowed = GeyserImpl.getInstance().getConfig().isAddNonBedrockItems();

        // List values here is important compared to HashSet - we need to preserve the order of what's given to us
        // (as of 1.19.2 Java) to replicate some edge cases in Java predicate behavior where it checks from the bottom
        // of the list first, then ascends.
        Multimap<String, CustomItemData> customItems = MultimapBuilder.hashKeys().arrayListValues().build();
        List<NonVanillaCustomItemData> nonVanillaCustomItems;

        MappingsConfigReader mappingsConfigReader = new MappingsConfigReader();
        if (customItemsAllowed) {
            // Load custom items from mappings files
            mappingsConfigReader.loadMappingsFromJson((key, item) -> {
                if (CustomItemRegistryPopulator.initialCheck(key, item, items)) {
                    customItems.get(key).add(item);
                }
            });

            nonVanillaCustomItems = new ObjectArrayList<>();
            GeyserImpl.getInstance().eventBus().fire(new GeyserDefineCustomItemsEventImpl(customItems, nonVanillaCustomItems) {
                @Override
                public boolean register(@NonNull String identifier, @NonNull CustomItemData customItemData) {
                    if (CustomItemRegistryPopulator.initialCheck(identifier, customItemData, items)) {
                        customItems.get(identifier).add(customItemData);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean register(@NonNull NonVanillaCustomItemData customItemData) {
                    if (customItemData.identifier().startsWith("minecraft:")) {
                        GeyserImpl.getInstance().getLogger().error("The custom item " + customItemData.identifier() +
                                " is attempting to masquerade as a vanilla Minecraft item!");
                        return false;
                    }

                    if (customItemData.javaId() < items.size()) {
                        // Attempting to overwrite an item that already exists in the protocol
                        GeyserImpl.getInstance().getLogger().error("The custom item " + customItemData.identifier() +
                                " is attempting to overwrite a vanilla Minecraft item!");
                        return false;
                    }
                    nonVanillaCustomItems.add(customItemData);
                    return true;
                }
            });
        } else {
            nonVanillaCustomItems = Collections.emptyList();
        }

        int customItemCount = customItems.size() + nonVanillaCustomItems.size();
        if (customItemCount > 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + customItemCount + " custom items");
        }

        // We can reduce some operations as Java information is the same across all palette versions
        boolean firstMappingsPass = true;
        Int2IntMap dyeColors = new FixedInt2IntMap();

        /* Load item palette */
        for (Map.Entry<String, PaletteVersion> palette : paletteVersions.entrySet()) {
            TypeReference<List<PaletteItem>> paletteEntriesType = new TypeReference<>() {};

            // Used to get the Bedrock namespaced ID (in instances where there are small differences)
            Object2IntMap<String> bedrockIdentifierToId = new Object2IntOpenHashMap<>();
            bedrockIdentifierToId.defaultReturnValue(Short.MIN_VALUE);

            List<String> itemNames = new ArrayList<>();

            List<PaletteItem> itemEntries;
            try (InputStream stream = bootstrap.getResource(String.format("bedrock/runtime_item_states.%s.json", palette.getKey()))) {
                itemEntries = GeyserImpl.JSON_MAPPER.readValue(stream, paletteEntriesType);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
            }

            // Used for custom items
            int nextFreeBedrockId = 0;
            List<ComponentItemData> componentItemData = new ObjectArrayList<>();

            Map<String, StartGamePacket.ItemEntry> entries = new Object2ObjectOpenHashMap<>();

            for (PaletteItem entry : itemEntries) {
                int id = entry.getId();
                if (id >= nextFreeBedrockId) {
                    nextFreeBedrockId = id + 1;
                }

                entries.put(entry.getName(), new StartGamePacket.ItemEntry(entry.getName(), (short) id));
                bedrockIdentifierToId.put(entry.getName(), id);
            }

            Object2IntMap<String> bedrockBlockIdOverrides = new Object2IntOpenHashMap<>();
            Object2IntMap<String> blacklistedIdentifiers = new Object2IntOpenHashMap<>();

            // Load creative items
            // We load this before item mappings to get overridden block runtime ID mappings
            JsonNode creativeItemEntries;
            try (InputStream stream = bootstrap.getResource(String.format("bedrock/creative_items.%s.json", palette.getKey()))) {
                creativeItemEntries = GeyserImpl.JSON_MAPPER.readTree(stream).get("items");
            } catch (Exception e) {
                throw new AssertionError("Unable to load creative items", e);
            }

            IntList boats = new IntArrayList();
            IntList buckets = new IntArrayList();
            IntList spawnEggs = new IntArrayList();
            List<ItemData> carpets = new ObjectArrayList<>();

            List<ItemMapping> mappings = new ObjectArrayList<>();
            // Temporary mapping to create stored items
            Map<String, ItemMapping> identifierToMapping = new Object2ObjectOpenHashMap<>();

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
                if (identifier.equals("minecraft:debug_stick")) {
                    // Just shows an empty texture; either way it doesn't exist in the creative menu on Java
                    continue;
                } else if (identifier.equals("minecraft:empty_map") && damage == 2) {
                    // Bedrock-only as its own item
                    continue;
                } else if (identifier.equals("minecraft:bordure_indented_banner_pattern") || identifier.equals("minecraft:field_masoned_banner_pattern")) {
                    // Bedrock-only banner patterns
                    continue;
                }
                StartGamePacket.ItemEntry entry = entries.get(identifier);
                int id = -1;
                if (entry != null) {
                    id = entry.getId();
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
                        .netId(netId++)
                        .build());

                if (blockRuntimeId != 0) {
                    // Add override for item mapping, unless it already exists... then we know multiple states can exist
                    if (!blacklistedIdentifiers.containsKey(identifier)) {
                        if (bedrockBlockIdOverrides.containsKey(identifier)) {
                            bedrockBlockIdOverrides.removeInt(identifier);
                            // Save this as a blacklist, but also as knowledge of what the block state name should be
                            blacklistedIdentifiers.put(identifier, blockRuntimeId);
                        } else {
                            // Unless there's multiple possibilities for this one state, let this be
                            bedrockBlockIdOverrides.put(identifier, blockRuntimeId);
                        }
                    }
                }
            }

            BlockMappings blockMappings = BlockRegistries.BLOCKS.forVersion(palette.getValue().protocolVersion());

            int itemIndex = 0;
            int javaFurnaceMinecartId = 0;

            Set<String> javaOnlyItems = new ObjectOpenHashSet<>();
            Collections.addAll(javaOnlyItems, "minecraft:spectral_arrow", "minecraft:debug_stick",
                    "minecraft:knowledge_book", "minecraft:tipped_arrow", "minecraft:bundle");
            if (!customItemsAllowed) {
                javaOnlyItems.add("minecraft:furnace_minecart");
            }
            // Java-only items for this version
            javaOnlyItems.addAll(palette.getValue().additionalTranslatedItems().keySet());

            Int2ObjectMap<String> customIdMappings = new Int2ObjectOpenHashMap<>();
            Set<String> registeredItemNames = new ObjectOpenHashSet<>(); // This is used to check for duplicate item names

            for (Map.Entry<String, GeyserMappingItem> entry : items.entrySet()) {
                String javaIdentifier = entry.getKey().intern();
                GeyserMappingItem mappingItem;
                String replacementItem = palette.getValue().additionalTranslatedItems().get(javaIdentifier);
                if (replacementItem != null) {
                    mappingItem = items.get(replacementItem);
                } else {
                    // This items has a mapping specifically for this version of the game
                    mappingItem = entry.getValue();
                }

                if (customItemsAllowed && javaIdentifier.equals("minecraft:furnace_minecart")) {
                    javaFurnaceMinecartId = itemIndex;
                    itemIndex++;
                    // Will be added later
                    mappings.add(null);
                    continue;
                }

                String bedrockIdentifier = mappingItem.getBedrockIdentifier();
                int bedrockId = bedrockIdentifierToId.getInt(bedrockIdentifier);
                if (bedrockId == Short.MIN_VALUE) {
                    throw new RuntimeException("Missing Bedrock ID in mappings: " + bedrockIdentifier);
                }
                int stackSize = mappingItem.getStackSize();

                int bedrockBlockId = -1;
                Integer firstBlockRuntimeId = entry.getValue().getFirstBlockRuntimeId();
                if (firstBlockRuntimeId != null) {
                    int blockIdOverride = bedrockBlockIdOverrides.getOrDefault(bedrockIdentifier, -1);
                    if (blockIdOverride != -1) {
                        // Straight from BDS is our best chance of getting an item that doesn't run into issues
                        bedrockBlockId = blockIdOverride;
                    } else {
                        // Try to get an example block runtime ID from the creative contents packet, for Bedrock identifier obtaining
                        int aValidBedrockBlockId = blacklistedIdentifiers.getOrDefault(bedrockIdentifier, -1);
                        if (aValidBedrockBlockId == -1) {
                            // Fallback
                            bedrockBlockId = blockMappings.getBedrockBlockId(firstBlockRuntimeId);
                        } else {
                            // As of 1.16.220, every item requires a block runtime ID attached to it.
                            // This is mostly for identifying different blocks with the same item ID - wool, slabs, some walls.
                            // However, in order for some visuals and crafting to work, we need to send the first matching block state
                            // as indexed by Bedrock's block palette
                            // There are exceptions! But, ideally, the block ID override should take care of those.
                            NbtMapBuilder requiredBlockStatesBuilder = NbtMap.builder();
                            String correctBedrockIdentifier = blockMappings.getBedrockBlockStates().get(aValidBedrockBlockId).getString("name");
                            boolean firstPass = true;
                            // Block states are all grouped together. In the mappings, we store the first block runtime ID in order,
                            // and the last, if relevant. We then iterate over all those values and get their Bedrock equivalents
                            Integer lastBlockRuntimeId = entry.getValue().getLastBlockRuntimeId() == null ? firstBlockRuntimeId : entry.getValue().getLastBlockRuntimeId();
                            for (int i = firstBlockRuntimeId; i <= lastBlockRuntimeId; i++) {
                                int bedrockBlockRuntimeId = blockMappings.getBedrockBlockId(i);
                                NbtMap blockTag = blockMappings.getBedrockBlockStates().get(bedrockBlockRuntimeId);
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

                            NbtMap requiredBlockStates = requiredBlockStatesBuilder.build();
                            if (bedrockBlockId == -1) {
                                int i = -1;
                                // We need to loop around again (we can't cache the block tags above) because Bedrock can include states that we don't have a pairing for
                                // in it's "preferred" block state - I.E. the first matching block state in the list
                                for (NbtMap blockTag : blockMappings.getBedrockBlockStates()) {
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
                                    NbtMap states = blockMappings.getBedrockBlockStates().get(itemData.getBlockRuntimeId()).getCompound("states");
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

                ItemMapping.ItemMappingBuilder mappingBuilder = ItemMapping.builder()
                        .javaIdentifier(javaIdentifier)
                        .javaId(itemIndex)
                        .bedrockIdentifier(bedrockIdentifier.intern())
                        .bedrockId(bedrockId)
                        .bedrockData(mappingItem.getBedrockData())
                        .bedrockBlockId(bedrockBlockId)
                        .stackSize(stackSize)
                        .maxDamage(mappingItem.getMaxDamage())
                        .hasSuspiciousStewEffect(mappingItem.isHasSuspiciousStewEffect());

                if (mappingItem.getRepairMaterials() != null) {
                    mappingBuilder = mappingBuilder.repairMaterials(new ObjectOpenHashSet<>(mappingItem.getRepairMaterials()));
                }

                if (mappingItem.getToolType() != null) {
                    if (mappingItem.getToolTier() != null) {
                        mappingBuilder = mappingBuilder.toolType(mappingItem.getToolType().intern())
                                .toolTier(mappingItem.getToolTier().intern());
                    } else {
                        mappingBuilder = mappingBuilder.toolType(mappingItem.getToolType().intern())
                                .toolTier("");
                    }
                }

                if (javaOnlyItems.contains(javaIdentifier)) {
                    // These items don't exist on Bedrock, so set up a variable that indicates they should have custom names
                    mappingBuilder = mappingBuilder.translationString((bedrockBlockId != -1 ? "block." : "item.") + entry.getKey().replace(":", "."));
                    GeyserImpl.getInstance().getLogger().debug("Adding " + entry.getKey() + " as an item that needs to be translated.");
                }

                // Add the custom item properties, if applicable
                List<ObjectIntPair<CustomItemOptions>> customItemOptions;
                Collection<CustomItemData> customItemsToLoad = customItems.get(javaIdentifier);
                if (customItemsAllowed && !customItemsToLoad.isEmpty()) {
                    customItemOptions = new ObjectArrayList<>(customItemsToLoad.size());

                    for (CustomItemData customItem : customItemsToLoad) {
                        int customProtocolId = nextFreeBedrockId++;

                        String customItemName = "geyser_custom:" + customItem.name();
                        if (!registeredItemNames.add(customItemName)) {
                            if (firstMappingsPass) {
                                GeyserImpl.getInstance().getLogger().error("Custom item name '" + customItem.name() + "' already exists and was registered again! Skipping...");
                            }
                            continue;
                        }

                        GeyserCustomMappingData customMapping = CustomItemRegistryPopulator.registerCustomItem(
                                customItemName, mappingItem, customItem, customProtocolId
                        );
                        // StartGamePacket entry - needed for Bedrock to recognize the item through the protocol
                        entries.put(customMapping.stringId(), customMapping.startGamePacketItemEntry());
                        // ComponentItemData - used to register some custom properties
                        componentItemData.add(customMapping.componentItemData());
                        customItemOptions.add(ObjectIntPair.of(customItem.customItemOptions(), customProtocolId));

                        customIdMappings.put(customMapping.integerId(), customMapping.stringId());
                    }

                    // Important for later to find the best match and accurately replicate Java behavior
                    Collections.reverse(customItemOptions);
                } else {
                    customItemOptions = Collections.emptyList();
                }
                mappingBuilder.customItemOptions(customItemOptions);

                ItemMapping mapping = mappingBuilder.build();

                if (javaIdentifier.contains("boat")) {
                    boats.add(bedrockId);
                } else if (javaIdentifier.contains("bucket") && !javaIdentifier.contains("milk")) {
                    buckets.add(bedrockId);
                } else if (javaIdentifier.contains("_carpet") && !javaIdentifier.contains("moss")) {
                    // This should be the numerical order Java sends as an integer value for llamas
                    carpets.add(ItemData.builder()
                            .id(mapping.getBedrockId())
                            .damage(mapping.getBedrockData())
                            .count(1)
                            .blockRuntimeId(mapping.getBedrockBlockId())
                            .build());
                } else if (javaIdentifier.startsWith("minecraft:music_disc_")) {
                    // The Java record level event uses the item ID as the "key" to play the record
                    Registries.RECORDS.register(itemIndex, SoundEvent.valueOf("RECORD_" +
                            javaIdentifier.replace("minecraft:music_disc_", "").toUpperCase(Locale.ENGLISH)));
                } else if (javaIdentifier.endsWith("_spawn_egg")) {
                    spawnEggs.add(mapping.getBedrockId());
                }

                mappings.add(mapping);
                identifierToMapping.put(javaIdentifier, mapping);

                itemNames.add(javaIdentifier);

                if (firstMappingsPass && mappingItem.getDyeColor() != -1) {
                    dyeColors.put(itemIndex, mappingItem.getDyeColor());
                }

                itemIndex++;
            }

            itemNames.add("minecraft:furnace_minecart");

            int lodestoneCompassId = entries.get("minecraft:lodestone_compass").getId();
            if (lodestoneCompassId == 0) {
                throw new RuntimeException("Lodestone compass not found in item palette!");
            }

            // Add the lodestone compass since it doesn't exist on java but we need it for item conversion
            ItemMapping lodestoneEntry = ItemMapping.builder()
                    .javaIdentifier("")
                    .bedrockIdentifier("minecraft:lodestone_compass")
                    .javaId(-1)
                    .bedrockId(lodestoneCompassId)
                    .bedrockData(0)
                    .bedrockBlockId(-1)
                    .stackSize(1)
                    .customItemOptions(Collections.emptyList())
                    .build();

            if (customItemsAllowed) {
                // Add the furnace minecart as a custom item
                int furnaceMinecartId = nextFreeBedrockId++;

                entries.put("geysermc:furnace_minecart", new StartGamePacket.ItemEntry("geysermc:furnace_minecart", (short) furnaceMinecartId, true));

                mappings.set(javaFurnaceMinecartId, ItemMapping.builder()
                        .javaIdentifier("minecraft:furnace_minecart")
                        .bedrockIdentifier("geysermc:furnace_minecart")
                        .javaId(javaFurnaceMinecartId)
                        .bedrockId(furnaceMinecartId)
                        .bedrockData(0)
                        .bedrockBlockId(-1)
                        .stackSize(1)
                        .customItemOptions(Collections.emptyList()) // TODO check for custom items with furnace minecart
                        .build());

                creativeItems.add(ItemData.builder()
                        .netId(netId++)
                        .id(furnaceMinecartId)
                        .count(1).build());

                NbtMapBuilder builder = NbtMap.builder();
                builder.putString("name", "geysermc:furnace_minecart")
                        .putInt("id", furnaceMinecartId);

                NbtMapBuilder itemProperties = NbtMap.builder();

                NbtMapBuilder componentBuilder = NbtMap.builder();
                // Conveniently, as of 1.16.200, the furnace minecart has a texture AND translation string already.
                itemProperties.putCompound("minecraft:icon", NbtMap.builder()
                        .putString("texture", "minecart_furnace")
                        .putString("frame", "0.000000")
                        .putInt("frame_version", 1)
                        .putString("legacy_id", "").build());
                componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", "item.minecartFurnace.name").build());

                // Indicate that the arm animation should play on rails
                List<NbtMap> useOnTag = Collections.singletonList(NbtMap.builder().putString("tags", "q.any_tag('rail')").build());
                componentBuilder.putCompound("minecraft:entity_placer", NbtMap.builder()
                        .putList("dispense_on", NbtType.COMPOUND, useOnTag)
                        .putString("entity", "minecraft:minecart")
                        .putList("use_on", NbtType.COMPOUND, useOnTag)
                        .build());

                // We always want to allow offhand usage when we can - matches Java Edition
                itemProperties.putBoolean("allow_off_hand", true);
                itemProperties.putBoolean("hand_equipped", false);
                itemProperties.putInt("max_stack_size", 1);
                itemProperties.putString("creative_group", "itemGroup.name.minecart");
                itemProperties.putInt("creative_category", 4); // 4 - "Items"

                componentBuilder.putCompound("item_properties", itemProperties.build());
                builder.putCompound("components", componentBuilder.build());
                componentItemData.add(new ComponentItemData("geysermc:furnace_minecart", builder.build()));

                // Register any completely custom items given to us
                IntSet registeredJavaIds = new IntOpenHashSet(); // Used to check for duplicate item java ids
                for (NonVanillaCustomItemData customItem : nonVanillaCustomItems) {
                    if (!registeredJavaIds.add(customItem.javaId())) {
                        if (firstMappingsPass) {
                            GeyserImpl.getInstance().getLogger().error("Custom item java id " + customItem.javaId() + " already exists and was registered again! Skipping...");
                        }
                        continue;
                    }

                    int customItemId = nextFreeBedrockId++;
                    NonVanillaItemRegistration registration = CustomItemRegistryPopulator.registerCustomItem(customItem, customItemId);

                    componentItemData.add(registration.componentItemData());
                    ItemMapping mapping = registration.mapping();
                    while (mapping.getJavaId() >= mappings.size()) {
                        // Fill with empty to get to the correct size
                        mappings.add(ItemMapping.AIR);
                    }
                    mappings.set(mapping.getJavaId(), mapping);

                    if (customItem.creativeGroup() != null || customItem.creativeCategory().isPresent()) {
                        creativeItems.add(ItemData.builder()
                                .id(customItemId)
                                .netId(netId++)
                                .count(1).build());
                    }
                }
            }

            ItemMappings itemMappings = ItemMappings.builder()
                    .items(mappings.toArray(new ItemMapping[0]))
                    .creativeItems(creativeItems.toArray(new ItemData[0]))
                    .itemEntries(List.copyOf(entries.values()))
                    .itemNames(itemNames.toArray(new String[0]))
                    .storedItems(new StoredItemMappings(identifierToMapping))
                    .javaOnlyItems(javaOnlyItems)
                    .bucketIds(buckets)
                    .boatIds(boats)
                    .spawnEggIds(spawnEggs)
                    .carpets(carpets)
                    .componentItemData(componentItemData)
                    .lodestoneCompass(lodestoneEntry)
                    .customIdMappings(customIdMappings)
                    .build();

            Registries.ITEMS.register(palette.getValue().protocolVersion(), itemMappings);

            firstMappingsPass = false;
        }

        ItemUtils.setDyeColors(dyeColors);
    }
}
