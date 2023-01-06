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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.CharMatcher;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.RotationComponent;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents.CustomBlockComponentsBuilder;
import org.geysermc.geyser.level.block.GeyserCustomBlockData.CustomBlockDataBuilder;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.mappings.util.BlockPropertyTypeMaps;
import org.geysermc.geyser.registry.mappings.util.CustomBlockMapping;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.util.BlockUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                    try {
                        String identifier = entry.getKey();
                        CustomBlockMapping customBlockMapping = this.readBlockMappingEntry(identifier, entry.getValue());
                        consumer.accept(identifier, customBlockMapping);
                    } catch (InvalidCustomMappingsFileException e) {
                        GeyserImpl.getInstance().getLogger().error("Error in registering blocks for custom mapping file: " + file.toString(), e);
                    }
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
            throw new InvalidCustomMappingsFileException("Invalid block mappings entry:" + node);
        }

        String name = node.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("A block entry has no name");
        }

        boolean onlyOverrideStates = node.has("only_override_states") && node.get("only_override_states").asBoolean();
        JsonNode stateOverrides = node.get("state_overrides");


        if (onlyOverrideStates && (stateOverrides == null || !stateOverrides.isObject())) {
            throw new InvalidCustomMappingsFileException("A block entry has only_override_states set to true but no state_overrides");
        }

        List<String> stateKeys = new ArrayList<>();
 
        if (stateOverrides != null && stateOverrides.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = stateOverrides.fields();
            while (fields.hasNext()) {
                stateKeys.add(identifier + fields.next().getKey());
            }
        }

        List<String> defaultStates = List.copyOf(BlockRegistries.JAVA_IDENTIFIERS.get().keySet())
            .stream()
            .filter(s -> s.startsWith(identifier + "["))
            .collect(Collectors.toList());
        if (defaultStates.isEmpty()) defaultStates.add(identifier);

        CustomBlockDataBuilder customBlockDataBuilder = new CustomBlockDataBuilder();
        customBlockDataBuilder.name(name)
                .components(createCustomBlockComponents(node, defaultStates.get(0), name))
                .permutations(createCustomBlockPermutations(stateOverrides, identifier, name))
                .booleanProperty("geyser_custom:default");

        BlockPropertyTypeMaps blockPropertyTypeMaps = createBlockPropertyTypeMaps(onlyOverrideStates ? stateKeys : defaultStates);
        blockPropertyTypeMaps.stringValuesMap().forEach((key, value) -> customBlockDataBuilder.stringProperty(key, new ArrayList<String>(value)));
        blockPropertyTypeMaps.intValuesMap().forEach((key, value) -> customBlockDataBuilder.intProperty(key, new ArrayList<Integer>(value)));
        blockPropertyTypeMaps.booleanValuesSet().forEach((value) -> customBlockDataBuilder.booleanProperty(value));

        CustomBlockData customBlockData = customBlockDataBuilder.build();

        Map<String, CustomBlockState> states = createCustomBlockStatesMap(identifier, stateKeys, defaultStates, onlyOverrideStates, customBlockData, 
            blockPropertyTypeMaps.stateKeyStrings(), blockPropertyTypeMaps.stateKeyInts(), blockPropertyTypeMaps.stateKeyBools());

        return new CustomBlockMapping(customBlockData, states, identifier, !onlyOverrideStates);
    }

    private List<CustomBlockPermutation> createCustomBlockPermutations(JsonNode node, String identifier, String name) {
        List<CustomBlockPermutation> permutations = new ArrayList<>();

        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    permutations.add(new CustomBlockPermutation(createCustomBlockComponents(value, (identifier + key), name), createCustomBlockPropertyQuery(key)));
                }
            });
        }
        
        permutations.add(new CustomBlockPermutation(new GeyserCustomBlockComponents.CustomBlockComponentsBuilder().build(), "q.block_property('geyser_custom:default') == 1"));

        return permutations;
    }

    private Map<String, CustomBlockState> createCustomBlockStatesMap(String identifier, List<String> stateKeys,List<String> defaultStates, boolean onlyOverrideStates, CustomBlockData customBlockData,
        Map<String, Map<String, String>> stateKeyStrings, Map<String, Map<String, Integer>> stateKeyInts, Map<String, Map<String, Boolean>> stateKeyBools) {

        Map<String, CustomBlockState> states = new HashMap<>();

        if (!onlyOverrideStates) {
            defaultStates.removeAll(stateKeys);
            createCustomBlockStates(defaultStates, true, customBlockData, stateKeyStrings, stateKeyInts, stateKeyBools, states);
        }
        createCustomBlockStates(stateKeys, false, customBlockData, stateKeyStrings, stateKeyInts, stateKeyBools, states);

        return states;
    }

    private void createCustomBlockStates(List<String> stateKeys, boolean defaultState, CustomBlockData customBlockData, 
    Map<String, Map<String, String>> stateKeyStrings, Map<String, Map<String, Integer>> stateKeyInts, 
    Map<String, Map<String, Boolean>> stateKeyBools, Map<String, CustomBlockState> states) {
        stateKeys.forEach((key) -> {
            CustomBlockState.Builder builder = customBlockData.blockStateBuilder();
            builder.booleanProperty("geyser_custom:default", defaultState);

            stateKeyStrings.getOrDefault(key, Collections.emptyMap()).forEach((property, stringValue) -> builder.stringProperty(property, stringValue));
            stateKeyInts.getOrDefault(key, Collections.emptyMap()).forEach((property, intValue) -> builder.intProperty(property, intValue));
            stateKeyBools.getOrDefault(key, Collections.emptyMap()).forEach((property, boolValue) -> builder.booleanProperty(property, boolValue));

            CustomBlockState blockState = builder.build();

            states.put(key, blockState);
        });
    }

    private BlockPropertyTypeMaps createBlockPropertyTypeMaps(List<String> usedStateKeys) {
        Map<String, LinkedHashSet<String>> stringValuesMap = new HashMap<>();
        Map<String, Map<String, String>> stateKeyStrings = new HashMap<>();

        Map<String, LinkedHashSet<Integer>> intValuesMap = new HashMap<>();
        Map<String, Map<String, Integer>> stateKeyInts = new HashMap<>();

        Set<String> booleanValuesSet = new HashSet<>();
        Map<String, Map<String, Boolean>> stateKeyBools = new HashMap<>();

        
        for (String state : usedStateKeys) {
            if (!state.contains("[")) continue;

            String[] pairs = splitStateString(state);

            for (String pair : pairs) {
                String[] parts = pair.split("=");
                String property = parts[0];
                String value = parts[1];
                if (value.equals("true") || value.equals("false")) {
                    booleanValuesSet.add(property);
                    Map<String, Boolean> propertyMap = stateKeyBools.getOrDefault(state, new HashMap<>());
                    propertyMap.put(property, Boolean.parseBoolean(value));
                    stateKeyBools.put(state, propertyMap);
                } else if (CharMatcher.inRange('0', '9').matchesAllOf(value)) {
                    int intValue = Integer.parseInt(value);
                    LinkedHashSet<Integer> values = intValuesMap.get(property);
                    if (values == null) {
                        values = new LinkedHashSet<>();
                        intValuesMap.put(property, values);
                    }
                    values.add(intValue);
                    Map<String, Integer> propertyMap = stateKeyInts.getOrDefault(state, new HashMap<>());
                    propertyMap.put(property, intValue);
                    stateKeyInts.put(state, propertyMap);
                } else {
                    LinkedHashSet<String> values = stringValuesMap.get(property);
                    if (values == null) {
                        values = new LinkedHashSet<>();
                        stringValuesMap.put(property, values);
                    }
                    values.add(value);
                    Map<String, String> propertyMap = stateKeyStrings.getOrDefault(state, new HashMap<>());
                    propertyMap.put(property, value);
                    stateKeyStrings.put(state, propertyMap);
                }
            }
        }
        return new BlockPropertyTypeMaps(stringValuesMap, stateKeyStrings, intValuesMap, stateKeyInts, booleanValuesSet, stateKeyBools);
    }

    private CustomBlockComponents createCustomBlockComponents(JsonNode node, String stateKey, String name) {
        int id = BlockRegistries.JAVA_IDENTIFIERS.getOrDefault(stateKey, -1);

        CustomBlockComponentsBuilder builder = new CustomBlockComponentsBuilder();
                // .friction()
        BoxComponent boxComponent = createBoxComponent(id);
        builder.selectionBox(boxComponent).collisionBox(boxComponent);

        // Ideally we would just be able to calculate the right value for this, but it seems that hardness value on bedrock does not follow Java
        // As such this might as well just be configured for now if people so choose
        float destructibleByMining = BlockRegistries.JAVA_BLOCKS.getOrDefault(id, BlockMapping.AIR).getHardness() * 3.25F;
        if (node.has("destructible_by_mining")) {
            destructibleByMining = node.get("destructible_by_mining").floatValue();
        }
        builder.destructibleByMining(destructibleByMining);

        if (node.has("geometry")) {
            builder.geometry(node.get("geometry").asText());
        }

        if (node.has("display_name")) {
            builder.displayName(node.get("display_name").asText());
        }

        if (node.has("light_emission")) {
            builder.lightEmission(node.get("light_emission").asInt());
        }

        if (node.has("light_dampening")) {
            builder.lightDampening(node.get("light_dampening").asInt());
        }

        boolean placeAir = true;
        if (node.has("place_air")) {
            placeAir = node.get("place_air").asBoolean();
        }
        builder.placeAir(placeAir);

        if (node.has("rotation")) {
            JsonNode rotation = node.get("rotation");
            int rotationX = rotation.get(0).asInt();
            int rotationY = rotation.get(1).asInt();
            int rotationZ = rotation.get(2).asInt();
            builder.rotation(new RotationComponent(rotationX, rotationY, rotationZ));
        }

        if (node.has("material_instances")) {
            JsonNode materialInstances = node.get("material_instances");
            if (materialInstances.isObject()) {
                materialInstances.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode value = entry.getValue();
                    if (value.isObject()) {
                        MaterialInstance materialInstance = createMaterialInstanceComponent(value, name);
                        builder.materialInstance(key, materialInstance);
                    }
                });
            }
        }

        if (node.has("tags")) {
            JsonNode tags = node.get("tags");
            if (tags.isArray()) {
                ArrayNode tagsArray = (ArrayNode) tags;
                Set<String> tagsSet = new HashSet<>();
                tagsArray.forEach(tag -> tagsSet.add(tag.asText()));
                builder.tags(tagsSet);
            }
        }

        CustomBlockComponents components = builder.build();

        return components;
    }

    private BoxComponent createBoxComponent(int id) {
        BoundingBox boundingBox = BlockUtils.getCollision(id).getBoundingBoxes()[0];

        float offsetX = (float) boundingBox.getSizeX() * 8;
        float offsetY = (float) boundingBox.getSizeY() * 8;
        float offsetZ = (float) boundingBox.getSizeZ() * 8;

        float cornerX = clamp((float) boundingBox.getMiddleX() * 16 - 8 - offsetX, -8, 8);
        float cornerY = clamp((float) boundingBox.getMiddleY() * 16 - offsetY, 0, 16);
        float cornerZ = clamp((float) boundingBox.getMiddleZ() * 16 - 8 - offsetZ, -8, 8);

        float sizeX = clamp((float) boundingBox.getSizeX() * 16, 0, 16);
        float sizeY = clamp((float) boundingBox.getSizeY() * 16, 0, 16);
        float sizeZ = clamp((float) boundingBox.getSizeZ() * 16, 0, 16);

        BoxComponent boxComponent = new BoxComponent(cornerX, cornerY, cornerZ, sizeX, sizeY, sizeZ);

        return boxComponent;
    }

    private MaterialInstance createMaterialInstanceComponent(JsonNode node, String name) {
        String texture = name;
        if (node.has("texture")) {
            texture = node.get("texture").asText();
        }

        String renderMethod = "alpha_test";
        if (node.has("render_method")) {
            renderMethod = node.get("render_method").asText();
        }

        boolean faceDimming = false;
        if (node.has("face_dimming")) {
            faceDimming = node.get("face_dimming").asBoolean();
        }

        boolean ambientOcclusion = false;
        if (node.has("ambient_occlusion")) {
            ambientOcclusion = node.get("ambient_occlusion").asBoolean();
        }

        return new MaterialInstance(texture, renderMethod, faceDimming, ambientOcclusion);
    }

    private String createCustomBlockPropertyQuery(String state) {
        String[] conditions = splitStateString(state);
        String[] queries = new String[conditions.length];

        for (int i = 0; i < conditions.length; i++) {
            String[] keyval = conditions[i].split("=", 2);

            if (keyval[1].equals("true")) {
                queries[i] = String.format("q.block_property('%1$s') == %2$s", keyval[0], 1);
            } else if (keyval[1].equals("false")) {
                queries[i] = String.format("q.block_property('%1$s') == %2$s", keyval[0], 0);
            } else if (CharMatcher.inRange('0', '9').matchesAllOf(keyval[1])) {
                queries[i] = String.format("q.block_property('%1$s') == %2$s", keyval[0], Integer.parseInt(keyval[1]));
            } else {
                queries[i] = String.format("q.block_property('%1$s') == '%2$s'", keyval[0], keyval[1]);
            }
        }

        String query = String.join(" && ", queries);

        return String.format("q.block_property('geyser_custom:default') == 0 && %s", query);
    }

    private String[] splitStateString(String state) {
        int openBracketIndex = state.indexOf("[");
        int closeBracketIndex = state.indexOf("]");

        String cleanStates = state.substring(openBracketIndex + 1, closeBracketIndex);

        String[] pairs = cleanStates.split("\\s*,\\s*");
        
        return pairs;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

}
