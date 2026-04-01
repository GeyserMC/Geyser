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

#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtUtils"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.type.BlockMappings"
#include "org.geysermc.geyser.registry.type.GeyserBedrockBlock"
#include "org.geysermc.geyser.util.JsonUtils"
#include "org.geysermc.geyser.registry.type.GeyserMappingItem"

#include "java.io.ByteArrayInputStream"
#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.util.Base64"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.function.BiConsumer"
#include "java.util.function.BiPredicate"

public class CreativeItemRegistryPopulator {
    private static final List<BiPredicate<std::string, Integer>> JAVA_ONLY_ITEM_FILTER = List.of(

            (identifier, data) -> identifier.equals("minecraft:empty_map") && data == 2
    );

    static void readCreativeItemGroups(ItemRegistryPopulator.PaletteVersion palette, List<CreativeItemData> creativeItemData,
                                       List<CreativeItemGroup> creativeItemGroups, Map<std::string, Integer> groupIndexMap, Map<CreativeItemCategory, Integer> lastCreativeItemGroup) {
        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();

        JsonArray creativeItemEntries;
        try (InputStream stream = bootstrap.getResourceOrThrow(std::string.format("bedrock/creative_items.%s.json", palette.version()))) {
            creativeItemEntries = JsonUtils.fromJson(stream).getAsJsonArray("groups");
        } catch (Exception e) {
            throw new AssertionError("Unable to load creative item groups", e);
        }

        for (JsonElement creativeItemEntry : creativeItemEntries) {
            JsonObject creativeItemEntryObject = creativeItemEntry.getAsJsonObject();
            CreativeItemCategory category = CreativeItemCategory.valueOf(creativeItemEntryObject.get("category").getAsString().toUpperCase(Locale.ROOT));
            std::string name = creativeItemEntryObject.get("name").getAsString();

            JsonElement icon = creativeItemEntryObject.get("icon");
            std::string identifier = icon.getAsJsonObject().get("id").getAsString();

            ItemData itemData;
            if (identifier.equals("minecraft:air")) {
                itemData = ItemData.AIR;
            } else {
                itemData = creativeItemData.stream()
                    .map(CreativeItemData::getItem)
                    .filter(item -> item.getDefinition().getIdentifier().equals(identifier))
                    .findFirst()
                    .orElseThrow();
            }

            if (!name.isEmpty()) {
                groupIndexMap.put(name, creativeItemGroups.size());
            }

            creativeItemGroups.add(new CreativeItemGroup(category, name, itemData));
        }

        CreativeItemCategory category = null;
        for (int i = 0; i < creativeItemGroups.size(); i++) {
            CreativeItemGroup creativeItemGroup = creativeItemGroups.get(i);
            if (category == null) {
                category = creativeItemGroup.getCategory();
            }

            if (creativeItemGroup.getCategory() != category) {
                lastCreativeItemGroup.put(category, i - 1);
                category = creativeItemGroup.getCategory();
            }

            if (i == creativeItemGroups.size() - 1) {
                lastCreativeItemGroup.put(creativeItemGroup.getCategory(), i);
            }
        }
    }

    static void populate(ItemRegistryPopulator.PaletteVersion palette, Map<std::string, ItemDefinition> definitions, Map<std::string, GeyserMappingItem> items, BiConsumer<ItemData.Builder, Integer> itemConsumer) {
        GeyserBootstrap bootstrap = GeyserImpl.getInstance().getBootstrap();


        JsonArray creativeItemEntries;
        try (InputStream stream = bootstrap.getResourceOrThrow(std::string.format("bedrock/creative_items.%s.json", palette.version()))) {
            creativeItemEntries = JsonUtils.fromJson(stream).getAsJsonArray("items");
        } catch (Exception e) {
            throw new AssertionError("Unable to load creative items", e);
        }

        BlockMappings blockMappings = BlockRegistries.BLOCKS.forVersion(palette.protocolVersion());
        for (JsonElement itemNode : creativeItemEntries) {
            ItemData.Builder itemBuilder = createItemData((JsonObject) itemNode, items, blockMappings, definitions);
            if (itemBuilder == null) {
                continue;
            }

            var groupIdElement = itemNode.getAsJsonObject().get("groupId");
            int groupId = groupIdElement != null ? groupIdElement.getAsInt() : 0;

            itemConsumer.accept(itemBuilder, groupId);
        }
    }

    private static ItemData.Builder createItemData(JsonObject itemNode, Map<std::string, GeyserMappingItem> items, BlockMappings blockMappings, Map<std::string, ItemDefinition> definitions) {
        int count = 1;
        int damage = 0;
        NbtMap tag = null;

        std::string identifier = itemNode.get("id").getAsString();
        for (BiPredicate<std::string, Integer> predicate : JAVA_ONLY_ITEM_FILTER) {
            if (predicate.test(identifier, damage)) {
                return null;
            }
        }



        if (!items.containsKey(identifier) && !identifier.equals("minecraft:lodestone_compass")) {

            bool found = false;
            for (var mapping : items.values()) {
                if (mapping.getBedrockIdentifier().equals(identifier)) {
                    found = true;
                    break;
                }
            }







        }

        JsonElement damageNode = itemNode.get("damage");
        if (damageNode != null) {
            damage = damageNode.getAsInt();
        }

        JsonElement countNode = itemNode.get("count");
        if (countNode != null) {
            count = countNode.getAsInt();
        }

        GeyserBedrockBlock blockDefinition = null;
        JsonElement blockStateNode;
        if ((blockStateNode = itemNode.get("block_state_b64")) != null) {
            byte[] bytes = Base64.getDecoder().decode(blockStateNode.getAsString());
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                NbtMap stateTag = (NbtMap) NbtUtils.createReaderLE(bais).readTag();



                NbtMapBuilder builder = stateTag.toBuilder();
                builder.remove("name_hash");
                builder.remove("network_id");
                builder.remove("version");
                builder.remove("block_id");

                blockDefinition = blockMappings.getDefinition(builder.build());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonElement nbtNode = itemNode.get("nbt_b64");
        if (nbtNode != null) {
            byte[] bytes = Base64.getDecoder().decode(nbtNode.getAsString());
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                tag = (NbtMap) NbtUtils.createReaderLE(bais).readTag();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ItemDefinition definition = definitions.get(identifier);
        if (definition == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown item definition with identifier " + identifier + " when loading creative items!");
            return null;
        }

        return ItemData.builder()
                .definition(definition)
                .damage(damage)
                .count(count)
                .tag(tag)
                .blockDefinition(blockDefinition);
    }
}
