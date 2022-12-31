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

package org.geysermc.geyser.registry.mappings.versions;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockMapping;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockData;
import org.geysermc.geyser.registry.BlockRegistries;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class MappingsReader_v1 extends MappingsReader {
    @Override
    public void readItemMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemData> consumer) {
        this.readItemMappingsV1(file, mappingsRoot, consumer);
    }

    @Override
    public void readBlockMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockMapping> consumer) {
        this.readBlockMappingsV1(file, mappingsRoot, consumer);
    }

    public void readItemMappingsV1(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemData> consumer) {
        JsonNode itemsNode = mappingsRoot.get("items");

        if (itemsNode != null && itemsNode.isObject()) {
            itemsNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(data -> {
                        try {
                            CustomItemData customItemData = this.readItemMappingEntry(data);
                            consumer.accept(entry.getKey(), customItemData);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in registering items for custom mapping file: " + file.toString(), e);
                        }
                    });
                }
            });
        }
    }

    public void readBlockMappingsV1(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockMapping> consumer) {
        JsonNode blocksNode = mappingsRoot.get("blocks");

        if (blocksNode != null && blocksNode.isObject()) {
            blocksNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isObject()) {
                    entry.getValue().forEach(data -> {
                        try {
                            String identifier = entry.getKey();
                            CustomBlockMapping customBlockMapping = this.readBlockMappingEntry(identifier, data);
                            consumer.accept(identifier, customBlockMapping);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in registering blocks for custom mapping file: " + file.toString(), e);
                        }
                    });
                }
            });
        }
    }

    private CustomItemOptions readItemCustomItemOptions(JsonNode node) {
        CustomItemOptions.Builder customItemOptions = CustomItemOptions.builder();

        JsonNode customModelData = node.get("custom_model_data");
        if (customModelData != null && customModelData.isInt()) {
            customItemOptions.customModelData(customModelData.asInt());
        }

        JsonNode damagePredicate = node.get("damage_predicate");
        if (damagePredicate != null && damagePredicate.isInt()) {
            customItemOptions.damagePredicate(damagePredicate.asInt());
        }

        JsonNode unbreakable = node.get("unbreakable");
        if (unbreakable != null && unbreakable.isBoolean()) {
            customItemOptions.unbreakable(unbreakable.asBoolean());
        }

        return customItemOptions.build();
    }

    @Override
    public CustomItemData readItemMappingEntry(JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item mappings entry");
        }

        String name = node.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item entry has no name");
        }

        CustomItemData.Builder customItemData = CustomItemData.builder()
                .name(name)
                .customItemOptions(this.readItemCustomItemOptions(node));

        //The next entries are optional
        if (node.has("display_name")) {
            customItemData.displayName(node.get("display_name").asText());
        }

        if (node.has("icon")) {
            customItemData.icon(node.get("icon").asText());
        }

        if (node.has("allow_offhand")) {
            customItemData.allowOffhand(node.get("allow_offhand").asBoolean());
        }

        if (node.has("texture_size")) {
            customItemData.textureSize(node.get("texture_size").asInt());
        }

        if (node.has("render_offsets")) {
            JsonNode tmpNode = node.get("render_offsets");

            customItemData.renderOffsets(fromJsonNode(tmpNode));
        }

        return customItemData.build();
    }

    @Override
    public CustomBlockMapping readBlockMappingEntry(String identifier, JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid block mappings entry");
        }

        String name = node.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("A block entry has no name");
        }

        JsonNode stateOverrides = node.get("state_overrides");

        CustomBlockData customBlockData = new GeyserCustomBlockData.CustomBlockDataBuilder()
                .name(name)
                .components(createCustomBlockComponents(node))
                // TODO: need to parse state data to find these, e.g. [east=none,north=none,power=1,south=none,west=none] and note these are range, not value
                // TODO: add possible values for all blockstates to mappings generator
                // .booleanProperty()
                // .intProperty()
                // .stringProperty()
                .permutations(createCustomBlockPermutations(stateOverrides))
                .build();

        Map<String, CustomBlockState> states = new HashMap<>();

        return new CustomBlockMapping(customBlockData, states);
    }

    private List<CustomBlockPermutation> createCustomBlockPermutations(JsonNode node) {
        List<CustomBlockPermutation> permutations = new ArrayList<>();

        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    value.forEach(data -> {
                        permutations.add(new CustomBlockPermutation(createCustomBlockComponents(data), createCustomBlockPropertyQuery(key)));
                    });
                }
            });
        }

        return permutations;
    }

    private Map<String, CustomBlockState> createCustomBlockStatesMap(String identifier, JsonNode node, CustomBlockData customBlockData) {
        // TODO: The goal here is that we need to register state overrides with the same permutations 
        // really we need to be able to figure out what properties correspond to what permutations as they need to match... this is probably going to require some fairly complex parsing of stateKeys and tbh we should probably do it up in readBlockMappingEntry
        // basically going to need to infer what the property type is... which we already sort of do in createCustomBlockPropertyQuery
        // alternatively, it might be easier to just use int properties for everything and just handle the types on the fly... so I think we can just parse stateKeys sort of how we already to in createCustomBlockPropertyQuery in terms of getting the conditions array
        // we will want to make an array list from that of all the possible values for each property
        // then we can just keep them all strings and map each one to a unique int Map<int, String>
        List<String> stateKeys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            stateKeys.add(identifier + fields.next().getKey());
        }

        List<String> defaultStates = List.copyOf(BlockRegistries.JAVA_IDENTIFIERS.get().keySet())
            .stream()
            .filter(s -> s.startsWith(identifier + "["))
            .collect(Collectors.toList());

        defaultStates.removeAll(stateKeys);

        Map<String, CustomBlockState> states = new HashMap<>();

        return states;
    }

    private CustomBlockComponents createCustomBlockComponents(JsonNode node) {
        CustomBlockComponents components = new GeyserCustomBlockComponents.CustomBlockComponentsBuilder()
                // .selectionBox()
                // .collisionBox()
                // .displayName()
                // .geometry()
                // .materialInstance()
                // .destroyTime()
                // .friction()
                // .lightEmission()
                // .lightDampening()
                // .rotation()
                // .placeAir()
                .build();

        JsonNode materialInstances = node.get("material_instances");
        // TODO: loop through material instances and add component for each to components

        return components;
    }

    private String createCustomBlockPropertyQuery(String state) {
        String list = state.substring(1, state.length() - 1);
        String[] conditions = list.split(",");
        String[] queries = new String[conditions.length];

        for (int i = 0; i < conditions.length; i++) {
            String[] keyval = conditions[i].split("=", 2);
            if (keyval[1].equals("true")) {
                queries[i] = String.format("q.block_property('%s') == %b", keyval[0], 1);
            } else if (keyval[1].equals("false")) {
                queries[i] = String.format("q.block_property('%s') == %b", keyval[0], 0);
            } else if (keyval[1].matches("-?\\d+")) {
                queries[i] = String.format("q.block_property('%s') == %b", keyval[0], Integer.parseInt(keyval[1]));
            } else {
                queries[i] = String.format("q.block_property('%s') == '%b'", keyval[0], keyval[1]);
            }
        }

        return String.join(" && ", queries);
    }

}
