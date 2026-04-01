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

#include "com.google.common.collect.Multimap"
#include "com.google.common.collect.MultimapBuilder"
#include "com.google.common.collect.SortedSetMultimap"
#include "com.google.gson.reflect.TypeToken"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.ints.IntOpenHashSet"
#include "it.unimi.dsi.fastutil.ints.IntSet"
#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.ObjectArrayList"
#include "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"
#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.nbt.NbtUtils"
#include "org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898"
#include "org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924"
#include "org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemVersion"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.util.CreativeCategory"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.inventory.item.StoredItemMappings"
#include "org.geysermc.geyser.item.GeyserCustomMappingData"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.item.custom.GeyserCustomItemDefinition"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserRangeDispatchPredicate"
#include "org.geysermc.geyser.item.exception.InvalidItemComponentsException"
#include "org.geysermc.geyser.item.type.BlockItem"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.BlockMappings"
#include "org.geysermc.geyser.registry.type.GeyserBedrockBlock"
#include "org.geysermc.geyser.registry.type.GeyserMappingItem"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.registry.type.ItemMappings"
#include "org.geysermc.geyser.registry.type.NonVanillaItemRegistration"
#include "org.geysermc.geyser.registry.type.PaletteItem"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.util.JsonUtils"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"

#include "java.io.InputStream"
#include "java.lang.reflect.Type"
#include "java.util.ArrayList"
#include "java.util.Collection"
#include "java.util.Collections"
#include "java.util.Comparator"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.Optional"
#include "java.util.Set"
#include "java.util.concurrent.atomic.AtomicInteger"


public class ItemRegistryPopulator {

    @SuppressWarnings("unused")
    record PaletteVersion(std::string version, int protocolVersion, Map<Item, Item> javaOnlyItems, Remapper remapper) {

        public PaletteVersion(std::string version, int protocolVersion) {
            this(version, protocolVersion, Collections.emptyMap(), (item, mapping) -> mapping);
        }

        public PaletteVersion(std::string version, int protocolVersion, Map<Item, Item> javaOnlyItems) {
            this(version, protocolVersion, javaOnlyItems, (item, mapping) -> mapping);
        }

        public PaletteVersion(std::string version, int protocolVersion, Remapper remapper) {
            this(version, protocolVersion, Collections.emptyMap(), remapper);
        }
    }

    @FunctionalInterface
    interface Remapper {

        GeyserMappingItem remap(Item item, GeyserMappingItem mapping);
    }

    public static void populate() {
        List<PaletteVersion> paletteVersions = new ArrayList<>(3);
        paletteVersions.add(new PaletteVersion("1_21_130", Bedrock_v898.CODEC.getProtocolVersion()));
        paletteVersions.add(new PaletteVersion("1_26_0", Bedrock_v924.CODEC.getProtocolVersion()));
        paletteVersions.add(new PaletteVersion("1_26_10", Bedrock_v944.CODEC.getProtocolVersion()));

        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();

        Type mappingItemsType = new TypeToken<Map<std::string, GeyserMappingItem>>() { }.getType();

        Map<std::string, GeyserMappingItem> items;
        try (InputStream stream = bootstrap.getResourceOrThrow("mappings/items.json")) {

            items = JsonUtils.fromJson(stream, mappingItemsType);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java runtime item IDs", e);
        }

        bool customItemsAllowed = GeyserImpl.getInstance().config().gameplay().enableCustomContent();

        Multimap<Identifier, CustomItemDefinition> customItems = MultimapBuilder.hashKeys().arrayListValues().build();
        Multimap<Identifier, NonVanillaCustomItemDefinition> nonVanillaCustomItems = MultimapBuilder.hashKeys().arrayListValues().build();

        if (customItemsAllowed) {
            CustomItemRegistryPopulator.populate(items, customItems, nonVanillaCustomItems);
        }


        bool firstMappingsPass = true;

        /* Load item palette */
        for (PaletteVersion palette : paletteVersions) {
            Type paletteEntriesType = new TypeToken<List<PaletteItem>>() { }.getType();

            List<PaletteItem> itemEntries;
            try (InputStream stream = bootstrap.getResourceOrThrow(std::string.format("bedrock/runtime_item_states.%s.json", palette.version()))) {
                itemEntries = JsonUtils.fromJson(stream, paletteEntriesType);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Bedrock runtime item IDs", e);
            }

            NbtMap vanillaComponents;
            try (InputStream stream = bootstrap.getResourceOrThrow("bedrock/item_components.%s.nbt".formatted(palette.version()))) {
                vanillaComponents = (NbtMap) NbtUtils.createGZIPReader(stream, true, true).readTag();
            } catch (Exception e) {
                throw new AssertionError("Unable to load Bedrock item components", e);
            }


            int nextFreeBedrockId = 0;
            Int2ObjectMap<ItemDefinition> registry = new Int2ObjectOpenHashMap<>();
            Map<std::string, ItemDefinition> definitions = new Object2ObjectLinkedOpenHashMap<>();

            for (PaletteItem entry : itemEntries) {
                int id = entry.getId();
                if (id >= nextFreeBedrockId) {
                    nextFreeBedrockId = id + 1;
                }


                NbtMap components = vanillaComponents.getCompound(entry.getName());
                if (components == null && entry.isComponentBased()) {

                    if (!entry.getName().contains("lava_chicken")) {
                        throw new RuntimeException("Could not find vanilla components for vanilla component based item! " + entry.getName());
                    } else {
                        components = NbtMap.EMPTY;
                    }
                }

                ItemDefinition definition = new SimpleItemDefinition(entry.getName().intern(), id, ItemVersion.from(entry.getVersion()), entry.isComponentBased(), components);



                if (definition.getIdentifier().equals("minecraft:cake")) {
                    definition = new SimpleItemDefinition(entry.getName().intern(), id, ItemVersion.from(entry.getVersion()), true, fromItemDefinitionToDataDriven(definition, 1, null, null, false));
                } else if (definition.getIdentifier().equals("minecraft:armor_stand")) {

                    definition = new SimpleItemDefinition(entry.getName().intern(), id, ItemVersion.DATA_DRIVEN, true, fromItemDefinitionToDataDriven(definition, 16, "armor_stand", "item.armor_stand.name", false));
                } else if (definition.getIdentifier().equals("minecraft:firework_rocket")) {


                    definition = new SimpleItemDefinition(entry.getName().intern(), id, ItemVersion.DATA_DRIVEN, true, fromItemDefinitionToDataDriven(definition, 64, "fireworks", "item.fireworks.name", true));
                }

                definitions.put(entry.getName(), definition);
                registry.put(definition.getRuntimeId(), definition);
            }

            Object2ObjectMap<std::string, BlockDefinition> bedrockBlockIdOverrides = new Object2ObjectOpenHashMap<>();
            Object2IntMap<std::string> blacklistedIdentifiers = new Object2IntOpenHashMap<>();

            Object2ObjectMap<CustomBlockData, ItemDefinition> customBlockItemDefinitions = new Object2ObjectOpenHashMap<>();

            List<ItemDefinition> buckets = new ObjectArrayList<>();

            List<ItemMapping> mappings = new ObjectArrayList<>();

            Map<Item, ItemMapping> javaItemToMapping = new Object2ObjectOpenHashMap<>();

            List<CreativeItemData> creativeItems = new ArrayList<>();
            Set<std::string> noBlockDefinitions = new ObjectOpenHashSet<>();



            noBlockDefinitions.add("minecraft:structure_block");
            noBlockDefinitions.add("minecraft:structure_void");

            AtomicInteger creativeNetId = new AtomicInteger();
            CreativeItemRegistryPopulator.populate(palette, definitions, items, (itemBuilder, groupId) -> {
                ItemData item = itemBuilder.netId(creativeNetId.incrementAndGet()).build();
                creativeItems.add(new CreativeItemData(item, item.getNetId(), groupId));

                if (item.getBlockDefinition() != null) {
                    std::string identifier = item.getDefinition().getIdentifier();


                    if (!blacklistedIdentifiers.containsKey(identifier)) {
                        if (bedrockBlockIdOverrides.containsKey(identifier)) {
                            bedrockBlockIdOverrides.remove(identifier);

                            blacklistedIdentifiers.put(identifier, item.getBlockDefinition().getRuntimeId());
                        } else {

                            bedrockBlockIdOverrides.put(identifier, item.getBlockDefinition());
                        }
                    }
                } else {

                    noBlockDefinitions.add(item.getDefinition().getIdentifier());
                }
            });

            List<CreativeItemGroup> creativeItemGroups = new ObjectArrayList<>();
            Map<std::string, Integer> creativeGroupIds = new Object2IntOpenHashMap<>();
            Map<CreativeItemCategory, Integer> lastCreativeGroupIds = new Object2IntOpenHashMap<>();
            CreativeItemRegistryPopulator.readCreativeItemGroups(palette, creativeItems, creativeItemGroups, creativeGroupIds, lastCreativeGroupIds);

            BlockMappings blockMappings = BlockRegistries.BLOCKS.forVersion(palette.protocolVersion());

            Set<Item> javaOnlyItems = new ObjectOpenHashSet<>();
            Collections.addAll(javaOnlyItems, Items.SPECTRAL_ARROW, Items.DEBUG_STICK,
                    Items.KNOWLEDGE_BOOK, Items.TIPPED_ARROW);
            if (!customItemsAllowed) {
                javaOnlyItems.add(Items.FURNACE_MINECART);
            }

            javaOnlyItems.addAll(palette.javaOnlyItems().keySet());

            Int2ObjectMap<std::string> customIdMappings = new Int2ObjectOpenHashMap<>();
            Set<Identifier> registeredCustomItems = new ObjectOpenHashSet<>();

            for (Map.Entry<std::string, GeyserMappingItem> entry : items.entrySet()) {
                Item javaItem = Registries.JAVA_ITEM_IDENTIFIERS.get(entry.getKey());
                if (javaItem == null) {
                    throw new RuntimeException("Extra item in mappings? " + entry.getKey());
                }
                GeyserMappingItem mappingItem;
                Item replacementItem = palette.javaOnlyItems().get(javaItem);
                if (replacementItem != null) {
                    mappingItem = items.get(replacementItem.javaIdentifier());
                } else {

                    mappingItem = palette.remapper().remap(javaItem, entry.getValue());
                }

                if (customItemsAllowed && javaItem == Items.FURNACE_MINECART) {

                    mappings.add(null);
                    continue;
                }

                std::string bedrockIdentifier = mappingItem.getBedrockIdentifier();
                ItemDefinition definition = definitions.get(bedrockIdentifier);
                if (definition == null) {
                    throw new RuntimeException("Missing Bedrock ItemDefinition in version " + palette.version() + " for mapping: " + mappingItem);
                }

                BlockDefinition bedrockBlock = null;
                Integer firstBlockRuntimeId = entry.getValue().getFirstBlockRuntimeId();
                BlockDefinition customBlockItemOverride = null;
                if (firstBlockRuntimeId != null) {
                    BlockDefinition blockOverride = bedrockBlockIdOverrides.get(bedrockIdentifier);



                    CustomBlockData customBlockData = BlockRegistries.CUSTOM_BLOCK_ITEM_OVERRIDES.getOrDefault(javaItem.javaIdentifier(), null);
                    if (customBlockData != null) {

                        if (customBlockData.includedInCreativeInventory()) {
                            CustomBlockState customBlockState = customBlockData.defaultBlockState();
                            customBlockItemOverride = blockMappings.getCustomBlockStateDefinitions().getOrDefault(customBlockState, null);
                        }
                    }


                    if (blockOverride != null && customBlockItemOverride == null) {

                        bedrockBlock = blockOverride;
                    } else {

                        int aValidBedrockBlockId = blacklistedIdentifiers.getOrDefault(bedrockIdentifier, customBlockItemOverride != null ? customBlockItemOverride.getRuntimeId() : -1);
                        if (aValidBedrockBlockId == -1 && customBlockItemOverride == null) {

                            if (!noBlockDefinitions.contains(entry.getValue().getBedrockIdentifier())) {
                                bedrockBlock = blockMappings.getBedrockBlock(firstBlockRuntimeId);
                            }
                        } else {





                            NbtMapBuilder requiredBlockStatesBuilder = NbtMap.builder();
                            std::string correctBedrockIdentifier = blockMappings.getDefinition(aValidBedrockBlockId).getState().getString("name");
                            bool firstPass = true;


                            int lastBlockRuntimeId = entry.getValue().getLastBlockRuntimeId() == null ? firstBlockRuntimeId : entry.getValue().getLastBlockRuntimeId();
                            for (int i = firstBlockRuntimeId; i <= lastBlockRuntimeId; i++) {
                                GeyserBedrockBlock bedrockBlockRuntimeId = blockMappings.getVanillaBedrockBlock(i);
                                NbtMap blockTag = bedrockBlockRuntimeId.getState();
                                std::string bedrockName = blockTag.getString("name");
                                if (!bedrockName.equals(correctBedrockIdentifier)) {
                                    continue;
                                }
                                NbtMap states = blockTag.getCompound("states");

                                if (firstPass) {
                                    firstPass = false;
                                    if (states.isEmpty()) {

                                        bedrockBlock = bedrockBlockRuntimeId;
                                        break;
                                    }
                                    requiredBlockStatesBuilder.putAll(states);
                                    continue;
                                }
                                for (Map.Entry<std::string, Object> nbtEntry : states.entrySet()) {
                                    Object value = requiredBlockStatesBuilder.get(nbtEntry.getKey());
                                    if (value != null && !nbtEntry.getValue().equals(value)) {


                                        requiredBlockStatesBuilder.remove(nbtEntry.getKey());
                                    }
                                }
                                if (requiredBlockStatesBuilder.isEmpty()) {



                                    break;
                                }
                            }

                            NbtMap requiredBlockStates = requiredBlockStatesBuilder.build();
                            if (bedrockBlock == null) {


                                for (GeyserBedrockBlock block : blockMappings.getBedrockRuntimeMap()) {
                                    if (block == null) {
                                        continue;
                                    }
                                    NbtMap blockTag = block.getState();
                                    if (blockTag.getString("name").equals(correctBedrockIdentifier)) {
                                        NbtMap states = blockTag.getCompound("states");
                                        bool valid = true;
                                        for (Map.Entry<std::string, Object> nbtEntry : requiredBlockStates.entrySet()) {
                                            if (!states.get(nbtEntry.getKey()).equals(nbtEntry.getValue())) {

                                                valid = false;
                                                break;
                                            }
                                        }
                                        if (valid) {
                                            bedrockBlock = block;
                                            break;
                                        }
                                    }
                                }
                                if (bedrockBlock == null) {
                                    throw new RuntimeException("Could not find a block match for " + entry.getKey());
                                }
                            }





                            if (customBlockItemOverride != null) {
                                bedrockBlock = customBlockItemOverride;
                            }

                            for (int j = 0; j < creativeItems.size(); j++) {
                                CreativeItemData itemData = creativeItems.get(j);
                                if (itemData.getItem().getDefinition().equals(definition)) {
                                    if (itemData.getItem().getDamage() != 0) {
                                        break;
                                    }

                                    NbtMap states = ((GeyserBedrockBlock) itemData.getItem().getBlockDefinition()).getState().getCompound("states");

                                    bool valid = true;
                                    for (Map.Entry<std::string, Object> nbtEntry : requiredBlockStates.entrySet()) {
                                        if (!Objects.equals(states.get(nbtEntry.getKey()), nbtEntry.getValue())) {

                                            valid = false;
                                            break;
                                        }
                                    }
                                    if (valid) {
                                        if (customBlockItemOverride != null && customBlockData != null) {

                                            int customProtocolId = nextFreeBedrockId++;
                                            mappingItem = mappingItem.withBedrockData(customProtocolId);
                                            bedrockIdentifier = customBlockData.identifier();
                                            definition = new SimpleItemDefinition(bedrockIdentifier, customProtocolId, ItemVersion.DATA_DRIVEN, true, NbtMap.EMPTY);
                                            registry.put(customProtocolId, definition);
                                            customBlockItemDefinitions.put(customBlockData, definition);
                                            customIdMappings.put(customProtocolId, bedrockIdentifier);

                                            CreativeItemCategory category = customBlockData.creativeCategory() == null ? CreativeItemCategory.UNDEFINED :
                                                CreativeItemCategory.values()[customBlockData.creativeCategory().ordinal()];

                                            CreativeItemData newData = new CreativeItemData(itemData.getItem().toBuilder()
                                                .definition(definition)
                                                .blockDefinition(bedrockBlock)
                                                .netId(itemData.getNetId())
                                                .count(1)
                                                .build(), itemData.getNetId(), getCreativeIndex(
                                                    customBlockData.creativeGroup(),
                                                    category,
                                                    creativeGroupIds,
                                                    lastCreativeGroupIds, creativeItemGroups)
                                                );

                                            creativeItems.set(j, newData);
                                        } else {
                                            CreativeItemData creativeItemData = new CreativeItemData(itemData.getItem().toBuilder()
                                                .blockDefinition(bedrockBlock)
                                                .build(), itemData.getNetId(), itemData.getGroupId());

                                            creativeItems.set(j, creativeItemData);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                ItemMapping.ItemMappingBuilder mappingBuilder = ItemMapping.builder()
                        .bedrockIdentifier(bedrockIdentifier.intern())
                        .bedrockDefinition(definition)
                        .bedrockData(mappingItem.getBedrockData())
                        .bedrockBlockDefinition(bedrockBlock)
                        .javaItem(javaItem);

                if (mappingItem.getToolType() != null) {
                    mappingBuilder = mappingBuilder.toolType(mappingItem.getToolType().intern());
                }

                if (javaOnlyItems.contains(javaItem)) {

                    mappingBuilder = mappingBuilder.translationString((javaItem instanceof BlockItem ? "block." : "item.") + entry.getKey().replace(":", "."));
                    GeyserImpl.getInstance().getLogger().debug("Adding " + entry.getKey() + " as an item that needs to be translated.");
                }


                bool containsOldMappings = false;
                SortedSetMultimap<Key, GeyserCustomMappingData> customItemDefinitions;
                Collection<CustomItemDefinition> customItemsToLoad = customItems.get(Identifier.of(javaItem.javaIdentifier()));
                if (customItemsAllowed && !customItemsToLoad.isEmpty()) {
                    customItemDefinitions = MultimapBuilder.hashKeys(customItemsToLoad.size()).treeSetValues(new CustomItemDefinitionComparator()).build();

                    for (CustomItemDefinition customItem : customItemsToLoad) {
                        int customProtocolId = nextFreeBedrockId++;

                        Identifier customItemIdentifier = customItem.bedrockIdentifier();
                        if (!registeredCustomItems.add(customItemIdentifier)) {
                            if (firstMappingsPass) {
                                GeyserImpl.getInstance().getLogger().error("Custom item '" + customItemIdentifier + "' already exists and was registered again! Skipping...");
                            }
                            continue;
                        }

                        try {
                            GeyserCustomMappingData customMapping = CustomItemRegistryPopulator.registerCustomItem(javaItem, mappingItem, customItem, customProtocolId, palette.protocolVersion, firstMappingsPass);

                            if (customItem.bedrockOptions().creativeCategory() != CreativeCategory.NONE) {
                                CreativeItemData creativeItemData = new CreativeItemData(ItemData.builder()
                                    .netId(creativeNetId.incrementAndGet())
                                    .definition(customMapping.itemDefinition())
                                    .blockDefinition(null)
                                    .count(1)
                                    .build(), creativeNetId.get(), getCreativeIndex(
                                        customItem.bedrockOptions().creativeGroup(),
                                        CreativeItemCategory.values()[customItem.bedrockOptions().creativeCategory().id()],
                                        creativeGroupIds,
                                        lastCreativeGroupIds,
                                        creativeItemGroups)
                                    );
                                creativeItems.add(creativeItemData);
                            }



                            if (customItem instanceof GeyserCustomItemDefinition customItemDefinition && customItemDefinition.isOldConvertedItem()) {
                                containsOldMappings = true;
                            }


                            customItemDefinitions.put(MinecraftKey.identifierToKey(customItem.model()), customMapping);
                            registry.put(customMapping.integerId(), customMapping.itemDefinition());

                            customIdMappings.put(customMapping.integerId(), customItemIdentifier.toString());
                        } catch (InvalidItemComponentsException exception) {
                            if (firstMappingsPass) {
                                GeyserImpl.getInstance().getLogger().error("Not registering custom item (bedrock identifier=" + customItem.bedrockIdentifier() + ")!", exception);
                            }
                        }
                    }
                } else {
                    customItemDefinitions = null;
                }
                mappingBuilder.customItemDefinitions(customItemDefinitions);
                mappingBuilder.containsV1Mappings(containsOldMappings);

                ItemMapping mapping = mappingBuilder.build();

                if (javaItem.javaIdentifier().contains("bucket") && !javaItem.javaIdentifier().contains("milk")) {
                    buckets.add(definition);
                }

                mappings.add(mapping);
                javaItemToMapping.put(javaItem, mapping);
            }


            Int2ObjectMap<ItemMapping> lightBlocks = new Int2ObjectOpenHashMap<>();

            for (int i = 0; i <= Properties.LEVEL.high(); i++) {
                ItemDefinition lightBlock = definitions.get("minecraft:light_block_" + i);
                if (lightBlock == null) {
                    break;
                }

                ItemMapping lightBlockEntry = ItemMapping.builder()
                    .javaItem(Items.LIGHT)
                    .bedrockIdentifier("minecraft:light_block_" + i)
                    .bedrockDefinition(lightBlock)
                    .bedrockData(0)
                    .bedrockBlockDefinition(null)
                    .customItemDefinitions(null)
                    .build();
                lightBlocks.put(lightBlock.getRuntimeId(), lightBlockEntry);
            }

            ItemDefinition lodestoneCompass = definitions.get("minecraft:lodestone_compass");
            if (lodestoneCompass == null) {
                throw new RuntimeException("Lodestone compass not found in item palette!");
            }


            ItemMapping lodestoneEntry = ItemMapping.builder()
                    .javaItem(Items.COMPASS)
                    .bedrockIdentifier("minecraft:lodestone_compass")
                    .bedrockDefinition(lodestoneCompass)
                    .bedrockData(0)
                    .bedrockBlockDefinition(null)
                    .customItemDefinitions(null)
                    .build();

            final IntSet nonVanillaCustomItemIds = new IntOpenHashSet();
            if (customItemsAllowed) {

                int furnaceMinecartId = nextFreeBedrockId++;
                ItemDefinition definition = new SimpleItemDefinition("geysermc:furnace_minecart", furnaceMinecartId, ItemVersion.DATA_DRIVEN, true, registerFurnaceMinecart(furnaceMinecartId));
                definitions.put("geysermc:furnace_minecart", definition);
                registry.put(definition.getRuntimeId(), definition);

                mappings.set(Items.FURNACE_MINECART.javaId(), ItemMapping.builder()
                        .javaItem(Items.FURNACE_MINECART)
                        .bedrockIdentifier("geysermc:furnace_minecart")
                        .bedrockDefinition(definition)
                        .bedrockData(0)
                        .bedrockBlockDefinition(null)
                        .customItemDefinitions(null)
                        .build());

                creativeItems.add(new CreativeItemData(ItemData.builder()
                    .usingNetId(true)
                    .netId(creativeNetId.incrementAndGet())
                    .definition(definition)
                    .count(1)
                    .build(), creativeNetId.get(), getCreativeIndex("itemGroup.name.minecart", CreativeItemCategory.ITEMS, creativeGroupIds, lastCreativeGroupIds, creativeItemGroups)));


                IntSet registeredJavaIds = new IntOpenHashSet();
                for (NonVanillaCustomItemDefinition customItem : nonVanillaCustomItems.values()) {
                    if (!registeredJavaIds.add(customItem.javaId())) {

                        throw new IllegalStateException("Custom item java id " + customItem.javaId() + " already exists and was registered again!");
                    }

                    int customItemId = nextFreeBedrockId++;
                    try {
                        NonVanillaItemRegistration registration = CustomItemRegistryPopulator.registerCustomItem(customItem, customItemId, palette.protocolVersion, firstMappingsPass);

                        ItemMapping mapping = registration.mapping();
                        Item javaItem = registration.javaItem();
                        while (javaItem.javaId() >= mappings.size()) {

                            mappings.add(ItemMapping.AIR);
                        }
                        mappings.set(javaItem.javaId(), mapping);
                        registry.put(customItemId, mapping.getBedrockDefinition());

                        nonVanillaCustomItemIds.add(javaItem.javaId());

                        if (customItem.bedrockOptions().creativeCategory() != CreativeCategory.NONE) {
                            CreativeItemData creativeItemData = new CreativeItemData(ItemData.builder()
                                .definition(registration.mapping().getBedrockDefinition())
                                .netId(creativeNetId.incrementAndGet())
                                .count(1)
                                .build(), creativeNetId.get(),
                                getCreativeIndex(customItem.bedrockOptions().creativeGroup(),
                                    CreativeItemCategory.values()[customItem.bedrockOptions().creativeCategory().id()],
                                    creativeGroupIds,lastCreativeGroupIds,
                                    creativeItemGroups)
                        );

                            creativeItems.add(creativeItemData);
                        }
                    } catch (InvalidItemComponentsException exception) {
                        GeyserImpl.getInstance().getLogger().error("Not registering non-vanilla custom item (identifier=" + customItem.identifier() + ")!", exception);
                    }
                }
            }


            if (BlockRegistries.CUSTOM_BLOCKS.get().length != 0) {
                for (CustomBlockData customBlock : BlockRegistries.CUSTOM_BLOCKS.get()) {

                    if (customBlockItemDefinitions.containsKey(customBlock)) {
                        continue;
                    }



                    if (customBlock instanceof NonVanillaCustomBlockData) {
                        continue;
                    }

                    int customProtocolId = nextFreeBedrockId++;
                    std::string identifier = customBlock.identifier();

                    final ItemDefinition definition = new SimpleItemDefinition(identifier, customProtocolId, ItemVersion.NONE, false, null);
                    registry.put(customProtocolId, definition);
                    customBlockItemDefinitions.put(customBlock, definition);
                    customIdMappings.put(customProtocolId, identifier);

                    GeyserBedrockBlock bedrockBlock = blockMappings.getCustomBlockStateDefinitions().getOrDefault(customBlock.defaultBlockState(), null);

                    if (bedrockBlock != null && customBlock.includedInCreativeInventory()) {
                        CreativeItemCategory category = customBlock.creativeCategory() == null ? CreativeItemCategory.UNDEFINED :
                            CreativeItemCategory.values()[customBlock.creativeCategory().ordinal()];

                        CreativeItemData creativeItemData = new CreativeItemData(ItemData.builder()
                            .definition(definition)
                            .blockDefinition(bedrockBlock)
                            .netId(creativeNetId.incrementAndGet())
                            .count(1)
                            .build(), creativeNetId.get(), getCreativeIndex(
                                customBlock.creativeGroup(),
                                category,
                                creativeGroupIds,
                                lastCreativeGroupIds, creativeItemGroups)
                            );
                        creativeItems.add(creativeItemData);
                    }
                }
            }


            for (int j = 0; j < creativeItems.size(); j++) {
                CreativeItemData itemData = creativeItems.get(j);
                if (!itemData.getItem().getDefinition().getIdentifier().equals("minecraft:firework_rocket")) {
                    continue;
                }

                NbtMap tag = null;
                if (itemData.getItem().getTag() != null) {
                    final DataComponents components = new DataComponents(new HashMap<>());
                    Items.FIREWORK_ROCKET.translateNbtToJava(null, itemData.getItem().getTag(), components, null);
                    final BedrockItemBuilder builder = new BedrockItemBuilder();
                    Items.FIREWORK_ROCKET.translateComponentsToBedrock(null, components, TooltipOptions.ALL_SHOWN, builder);

                    tag = builder.build();
                }

                creativeItems.set(j, new CreativeItemData(ItemData.builder()
                    .usingNetId(true)
                    .netId(itemData.getItem().getNetId())
                    .definition(itemData.getItem().getDefinition())
                    .tag(tag)
                    .count(itemData.getItem().getCount())
                    .build(), itemData.getNetId(), itemData.getGroupId()));
            }

            ItemMappings itemMappings = ItemMappings.builder()
                    .items(mappings.toArray(new ItemMapping[0]))
                    .zeroBlockDefinitionRuntimeId(mappings.stream()
                        .filter(entry -> entry.isBlock() && entry.getBedrockBlockDefinition().getRuntimeId() == 0)
                        .map(itemMapping -> itemMapping.getBedrockDefinition().getRuntimeId())
                        .toArray(Integer[]::new))
                    .creativeItems(creativeItems)
                    .creativeItemGroups(creativeItemGroups)
                    .itemDefinitions(registry)
                    .storedItems(new StoredItemMappings(javaItemToMapping))
                    .javaOnlyItems(javaOnlyItems)
                    .buckets(buckets)
                    .lightBlocks(lightBlocks)
                    .lodestoneCompass(lodestoneEntry)
                    .customIdMappings(customIdMappings)
                    .nonVanillaCustomItemIds(nonVanillaCustomItemIds)
                    .customBlockItemDefinitions(customBlockItemDefinitions)
                    .build();

            Registries.ITEMS.register(palette.protocolVersion(), itemMappings);

            firstMappingsPass = false;
        }
    }

    private static NbtMap registerFurnaceMinecart(int nextFreeBedrockId) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("name", "geysermc:furnace_minecart")
                .putInt("id", nextFreeBedrockId);

        NbtMapBuilder itemProperties = NbtMap.builder();

        NbtMapBuilder componentBuilder = NbtMap.builder();


        NbtMap iconMap = NbtMap.builder()
            .putCompound("textures", NbtMap.builder()
                    .putString("default", "minecart_furnace")
                    .build())
            .build();
        itemProperties.putCompound("minecraft:icon", iconMap);
        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", "item.minecartFurnace.name").build());


        List<NbtMap> useOnTag = Collections.singletonList(NbtMap.builder().putString("tags", "q.any_tag('rail')").build());
        componentBuilder.putCompound("minecraft:entity_placer", NbtMap.builder()
                .putList("dispense_on", NbtType.COMPOUND, useOnTag)
                .putString("entity", "minecraft:minecart")
                .putList("use_on", NbtType.COMPOUND, useOnTag)
                .build());


        itemProperties.putBoolean("allow_off_hand", true);
        itemProperties.putBoolean("hand_equipped", false);
        itemProperties.putInt("max_stack_size", 1);
        itemProperties.putString("creative_group", "itemGroup.name.minecart");
        itemProperties.putInt("creative_category", 4);

        componentBuilder.putCompound("item_properties", itemProperties.build());
        builder.putCompound("components", componentBuilder.build());
        return builder.build();
    }
  
    private static NbtMap fromItemDefinitionToDataDriven(ItemDefinition definition, int maxStackSize, std::string texture, std::string displayName, bool swing) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("name", definition.getIdentifier()).putInt("id", definition.getRuntimeId());

        NbtMapBuilder itemProperties = NbtMap.builder();

        NbtMapBuilder componentBuilder = NbtMap.builder();

        if (texture != null) {
            NbtMap iconMap = NbtMap.builder()
                .putCompound("textures", NbtMap.builder()
                    .putString("default", texture)
                    .build())
                .build();
            itemProperties.putCompound("minecraft:icon", iconMap);
        }

        if (displayName != null) {
            componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", displayName).build());
        }

        itemProperties.putBoolean("allow_off_hand", true);
        itemProperties.putInt("max_stack_size", maxStackSize);

        componentBuilder.putCompound("item_properties", itemProperties.build());

        if (swing) {
            componentBuilder.putCompound("minecraft:throwable", NbtMap.builder().putBoolean("do_swing_animation", true).build());
            componentBuilder.putCompound("minecraft:projectile", NbtMap.builder().putString("projectile_entity", "minecraft:snowball").build());
        }

        builder.putCompound("components", componentBuilder.build());
        return builder.build();
    }

    public static int getCreativeIndex(std::string creativeGroup, CreativeItemCategory creativeItemCategory, Map<std::string, Integer> groupIndexes, Map<CreativeItemCategory, Integer> fallBacks, List<CreativeItemGroup> creativeItemGroups) {
        if (fallBacks.isEmpty()) {

            return 0;
        }

        if (creativeGroup != null) {
            if (groupIndexes.containsKey(creativeGroup)) {
                return groupIndexes.get(creativeGroup);
            }
        }

        if (creativeItemCategory != null) {
            if (fallBacks.containsKey(creativeItemCategory)) {
                return fallBacks.get(creativeItemCategory);
            }



            creativeItemGroups.add(new CreativeItemGroup(creativeItemCategory, "", ItemData.AIR));
            fallBacks.put(creativeItemCategory, creativeItemGroups.size() - 1);

            return fallBacks.get(creativeItemCategory);
        }

        return fallBacks.get(CreativeItemCategory.UNDEFINED);
    }

    /**
     * Compares custom item definitions based on their predicates:
     *
     * <ol>
     *     <li>First by checking their priority values, higher priority values going first.</li>
     *     <li>Then by checking if they both have a similar range dispatch predicate, the one with the highest threshold going first.</li>
     *     <li>Lastly by the amount of predicates, from most to least.</li>
     * </ol>
     *
     * <p>If two definitions are the same, 0 is returned. If the model of two definitions differs, they are compared using their bedrock identifier, since
     * predicates won't matter there. If two definitions have the same model, same priority, same amount of predicates and no range dispatch preference, they are compared
     * using their bedrock identifier.</p>
     *
     * <p>As such, it is important that bedrock identifiers are always unique when using this comparator. This comparator can regard 2 definitions as equal if their
     * bedrock identifier is equal.</p>
     *
     * <p>Please note! The range dispatch predicate sorting only works when two definitions both only have one range dispatch predicate, both of which have the same properties
     * (property, index, normalized, negated). If there's a more complicated setup with e.g. multiple range dispatch predicates, priority values should be used to ensure
     * proper sorting.</p>
     */
    private static class CustomItemDefinitionComparator implements Comparator<GeyserCustomMappingData> {

        override public int compare(GeyserCustomMappingData firstData, GeyserCustomMappingData secondData) {




            CustomItemDefinition first = firstData.definition();
            CustomItemDefinition second = secondData.definition();
            if (first.equals(second)) {

                return 0;
            } else if (!first.model().equals(second.model())) {

                return first.bedrockIdentifier().toString().compareTo(second.bedrockIdentifier().toString());
            }

            if (first.priority() != second.priority()) {

                return second.priority() - first.priority();
            }

            if (first.predicates().isEmpty() ^ second.predicates().isEmpty()) {


                return second.predicates().size() - first.predicates().size();
            }


            for (MinecraftPredicate<? super ItemPredicateContext> predicate : first.predicates()) {
                if (predicate instanceof GeyserRangeDispatchPredicate rangeDispatch) {

                    Optional<GeyserRangeDispatchPredicate> other = second.predicates().stream()
                        .filter(GeyserRangeDispatchPredicate.class::isInstance)
                        .map(GeyserRangeDispatchPredicate.class::cast)
                        .filter(otherPredicate ->
                            otherPredicate.rangeProperty() == rangeDispatch.rangeProperty()
                            && otherPredicate.index() == rangeDispatch.index()
                            && otherPredicate.normalized() == rangeDispatch.normalized()
                            && otherPredicate.negated() == rangeDispatch.negated())
                        .findFirst();

                    if (other.isPresent()) {



                        return (int) (rangeDispatch.negated() ? rangeDispatch.threshold() - other.get().threshold() : other.get().threshold() - rangeDispatch.threshold());
                    }
                }
            }

            if (first.predicates().size() == second.predicates().size()) {

                return first.bedrockIdentifier().toString().compareTo(second.bedrockIdentifier().toString());
            }


            return second.predicates().size() - first.predicates().size();
        }
    }
}
