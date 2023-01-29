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

import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.RotationComponent;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions.BlockFilterType;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions.Face;
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
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.MathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    /**
     * Read a block mapping entry from a JSON node and java identifier
     * @param identifier The java identifier of the block
     * @param node The {@link JsonNode} containing the block mapping entry
     * @return The {@link CustomBlockMapping} record to be read by {@link org.geysermc.geyser.registry.populator.CustomBlockRegistryPopulator#registerCustomBedrockBlocks}
     * @throws InvalidCustomMappingsFileException If the JSON node is invalid
     */
    @Override
    public CustomBlockMapping readBlockMappingEntry(String identifier, JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid block mappings entry:" + node);
        }

        String name = node.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("A block entry has no name");
        }

        // If this is true, we will only register the states the user has specified rather than all of the blocks possible states
        boolean onlyOverrideStates = node.has("only_override_states") && node.get("only_override_states").asBoolean();
        JsonNode stateOverrides = node.get("state_overrides");


        if (onlyOverrideStates && (stateOverrides == null || !stateOverrides.isObject())) {
            throw new InvalidCustomMappingsFileException("A block entry has only_override_states set to true but no state_overrides");
        }

        List<String> stateKeys = new ArrayList<>();
 
        // Add the block's identifier to the object so we can use the resulting string to search the block mappings
        if (stateOverrides != null && stateOverrides.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = stateOverrides.fields();
            while (fields.hasNext()) {
                stateKeys.add(identifier + fields.next().getKey());
            }
        }

        // Find all the default states for the block we wish to override
        List<String> defaultStates = List.copyOf(BlockRegistries.JAVA_IDENTIFIERS.get().keySet())
            .stream()
            .filter(s -> s.startsWith(identifier + "["))
            .collect(Collectors.toList());
        // If no states were found, the block must only have one state, so we add its plain identifier
        if (defaultStates.isEmpty()) defaultStates.add(identifier);

        // Create the data for the overall block
        CustomBlockDataBuilder customBlockDataBuilder = new CustomBlockDataBuilder();
        customBlockDataBuilder.name(name)
                // We pass in the first state and just use the hitbox from that as the default
                // Each state will have its own so this is fine
                .components(createCustomBlockComponents(node, defaultStates.get(0), name))
                // We must create permutation for every state override
                .permutations(createCustomBlockPermutations(stateOverrides, identifier, name))
                // This property is use to display the default state when onlyOverrideStates is false 
                .booleanProperty(String.format("%s:default", Constants.GEYSER_NAMESPACE));

        // We need to have three property type maps, one for each type of block property
        // Each contains the properties this block has for that type and its possible values, except for boolean since the values must be true/false
        // If we are only overriding states, we pass in only the state keys supplied in the mapping
        // Otherwise, we pass in all possible states for the block
        BlockPropertyTypeMaps blockPropertyTypeMaps = createBlockPropertyTypeMaps(onlyOverrideStates ? stateKeys : defaultStates);
        blockPropertyTypeMaps.stringValuesMap().forEach((key, value) -> customBlockDataBuilder.stringProperty(key, new ArrayList<String>(value)));
        blockPropertyTypeMaps.intValuesMap().forEach((key, value) -> customBlockDataBuilder.intProperty(key, new ArrayList<Integer>(value)));
        blockPropertyTypeMaps.booleanValuesSet().forEach((value) -> customBlockDataBuilder.booleanProperty(value));

        // Finally, build the custom block data
        CustomBlockData customBlockData = customBlockDataBuilder.build();

        // Create a map of the custom block states for this block, which contains the full state identifier mapped to the custom block state data
        Map<String, CustomBlockState> states = createCustomBlockStatesMap(stateKeys, defaultStates, onlyOverrideStates, customBlockData, 
            blockPropertyTypeMaps.stateKeyStrings(), blockPropertyTypeMaps.stateKeyInts(), blockPropertyTypeMaps.stateKeyBools());

        // Create the custom block mapping record to be passed into the custom block registry populator
        return new CustomBlockMapping(customBlockData, states, identifier, !onlyOverrideStates);
    }

    /**
     * Creates a list of {@link CustomBlockPermutation} from the given mappings node containing permutations, java identifier, and custom block name
     * @param node an {@link JsonNode} from the mappings file containing the permutations
     * @param identifier the java identifier of the block
     * @param name the name of the custom block
     * @return the list of custom block permutations
     */
    private List<CustomBlockPermutation> createCustomBlockPermutations(JsonNode node, String identifier, String name) {
        List<CustomBlockPermutation> permutations = new ArrayList<>();

        // Create a custom block permutation record for each permutation passed into the mappings for the given block
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    // Each permutation has its own components, which override the base components when explicitly set
                    // Based on the input states, we construct a molang query that will evaluate to true when the block properties corresponding to the state are active
                    permutations.add(new CustomBlockPermutation(createCustomBlockComponents(value, (identifier + key), name), createCustomBlockPropertyQuery(key)));
                }
            });
        }
        
        // We also need to create a permutation for the default state of the block with no components
        // Functionally, this means the default components will be used
        permutations.add(new CustomBlockPermutation(new GeyserCustomBlockComponents.CustomBlockComponentsBuilder().build(), String.format("q.block_property('%s:default') == 1", Constants.GEYSER_NAMESPACE)));

        return permutations;
    }

    /**
     * Create a map of java block state identifiers to {@link CustomBlockState} so that {@link #readBlockMappingEntry} can include it in the {@link CustomBlockMapping} record
     * @param stateKeys the list of java block state identifiers explicitly passed in the mappings
     * @param defaultStates the list of all possible java block state identifiers for the block
     * @param onlyOverrideStates whether or not we are only overriding the states passed in the mappings
     * @param customBlockData the {@link CustomBlockData} for the block
     * @param stateKeyStrings the map of java block state identifiers to their string properties
     * @param stateKeyInts the map of java block state identifiers to their int properties
     * @param stateKeyBools the map of java block state identifiers to their boolean properties
     * @return the custom block states maps
     */
    private Map<String, CustomBlockState> createCustomBlockStatesMap(List<String> stateKeys,List<String> defaultStates, boolean onlyOverrideStates, CustomBlockData customBlockData,
        Map<String, Map<String, String>> stateKeyStrings, Map<String, Map<String, Integer>> stateKeyInts, Map<String, Map<String, Boolean>> stateKeyBools) {

        Map<String, CustomBlockState> states = new HashMap<>();

        // If not only overriding specified states, we must include the default states in the custom block states map 
        if (!onlyOverrideStates) {
            defaultStates.removeAll(stateKeys);
            createCustomBlockStates(defaultStates, true, customBlockData, stateKeyStrings, stateKeyInts, stateKeyBools, states);
        }
        createCustomBlockStates(stateKeys, false, customBlockData, stateKeyStrings, stateKeyInts, stateKeyBools, states);

        return states;
    }

    /**
     * Create the custom block states for the given state keys and append them to the passed states map
     * @param stateKeys the list of java block state identifiers
     * @param defaultState whether or not this is the default state
     * @param customBlockData the {@link CustomBlockData} for the block
     * @param stateKeyStrings the map of java block state identifiers to their string properties
     * @param stateKeyInts the map of java block state identifiers to their int properties
     * @param stateKeyBools the map of java block state identifiers to their boolean properties
     * @param states the map of java block state identifiers to their {@link CustomBlockState} to append
     */
    private void createCustomBlockStates(List<String> stateKeys, boolean defaultState, CustomBlockData customBlockData, 
    Map<String, Map<String, String>> stateKeyStrings, Map<String, Map<String, Integer>> stateKeyInts, 
    Map<String, Map<String, Boolean>> stateKeyBools, Map<String, CustomBlockState> states) {
        stateKeys.forEach((key) -> {
            CustomBlockState.Builder builder = customBlockData.blockStateBuilder();
            // We always include the default property, which is used to set the default state when onlyOverrideStates is false
            builder.booleanProperty(String.format("%s:default", Constants.GEYSER_NAMESPACE), defaultState);

            // The properties must be added to the builder seperately for each type
            stateKeyStrings.getOrDefault(key, Collections.emptyMap()).forEach((property, stringValue) -> builder.stringProperty(property, stringValue));
            stateKeyInts.getOrDefault(key, Collections.emptyMap()).forEach((property, intValue) -> builder.intProperty(property, intValue));
            stateKeyBools.getOrDefault(key, Collections.emptyMap()).forEach((property, boolValue) -> builder.booleanProperty(property, boolValue));

            CustomBlockState blockState = builder.build();

            states.put(key, blockState);
        });
    }

    /**
     * Creates a record of {@link BlockPropertyTypeMaps} for the given list of java block state identifiers that are being actively used by the custom block
     * @param usedStateKeys the list of java block state identifiers that are being actively used by the custom block
     * @return the {@link BlockPropertyTypeMaps} record
     */
    private BlockPropertyTypeMaps createBlockPropertyTypeMaps(List<String> usedStateKeys) {
        // Each of the three property type has two maps
        // The first map is used to store the possible values for each property
        // The second map is used to store the value for each property for each state
        Map<String, LinkedHashSet<String>> stringValuesMap = new HashMap<>();
        Map<String, Map<String, String>> stateKeyStrings = new HashMap<>();

        Map<String, LinkedHashSet<Integer>> intValuesMap = new HashMap<>();
        Map<String, Map<String, Integer>> stateKeyInts = new HashMap<>();

        Set<String> booleanValuesSet = new HashSet<>();
        Map<String, Map<String, Boolean>> stateKeyBools = new HashMap<>();

        
        for (String state : usedStateKeys) {
            // No bracket means that there is only one state, so the maps should be empty
            if (!state.contains("[")) continue;

            // Split the state string into an array containing each property=value pair
            String[] pairs = splitStateString(state);

            for (String pair : pairs) {
                // Get the property and value individually
                String[] parts = pair.split("=");
                String property = parts[0];
                String value = parts[1];

                // Figure out what property type we are dealing with
                if (value.equals("true") || value.equals("false")) {
                    booleanValuesSet.add(property);
                    Map<String, Boolean> propertyMap = stateKeyBools.getOrDefault(state, new HashMap<>());
                    propertyMap.put(property, Boolean.parseBoolean(value));
                    stateKeyBools.put(state, propertyMap);
                } else if (CharMatcher.inRange('0', '9').matchesAllOf(value)) {
                    int intValue = Integer.parseInt(value);
                    LinkedHashSet<Integer> values = intValuesMap.get(property);
                    // Initialize the property to values map if it doesn't exist
                    if (values == null) {
                        values = new LinkedHashSet<>();
                        intValuesMap.put(property, values);
                    }
                    values.add(intValue);
                    Map<String, Integer> propertyMap = stateKeyInts.getOrDefault(state, new HashMap<>());
                    propertyMap.put(property, intValue);
                    stateKeyInts.put(state, propertyMap);
                } else {
                    // If it's n not a boolean or int it must be a string
                    LinkedHashSet<String> values = stringValuesMap.get(property);
                    // Initialize the property to values map if it doesn't exist
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
        // We should now have all of the maps
        return new BlockPropertyTypeMaps(stringValuesMap, stateKeyStrings, intValuesMap, stateKeyInts, booleanValuesSet, stateKeyBools);
    }

    /**
     * Creates a {@link CustomBlockComponents} object for the passed permutation or base block node, java block state identifier, and custom block name
     * @param node the permutation or base block {@link JsonNode}
     * @param stateKey the java block state identifier
     * @param name the custom block name
     * @return the {@link CustomBlockComponents} object
     */
    private CustomBlockComponents createCustomBlockComponents(JsonNode node, String stateKey, String name) {
        // This is needed to find the correct selection box for the given block
        int id = BlockRegistries.JAVA_IDENTIFIERS.getOrDefault(stateKey, -1);
        CustomBlockComponentsBuilder builder = new CustomBlockComponentsBuilder();
        BoxComponent boxComponent = createBoxComponent(id);

        BoxComponent selectionBox = boxComponent;
        if (node.has("selection_box")) {
            JsonNode selectionBoxNode = node.get("selection_box");
            if (selectionBoxNode.isObject()) {
                if (selectionBoxNode.has("origin") && selectionBoxNode.has("size")) {
                    JsonNode origin = selectionBoxNode.get("origin");
                    int originX = origin.get(0).intValue();
                    int originY = origin.get(1).intValue();
                    int originZ = origin.get(2).intValue();

                    JsonNode size = selectionBoxNode.get("size");
                    int sizeX = size.get(0).intValue();
                    int sizeY = size.get(1).intValue();
                    int sizeZ = size.get(2).intValue();

                    selectionBox = new BoxComponent(originX, originY, originZ, sizeX, sizeY, sizeZ);
                }
            }
        }
        builder.selectionBox(selectionBox);

        BoxComponent collisionBox = boxComponent;
        if (node.has("collision_box")) {
            JsonNode collisionBoxNode = node.get("collision_box");
            if (collisionBoxNode.isObject()) {
                if (collisionBoxNode.has("origin") && collisionBoxNode.has("size")) {
                    JsonNode origin = collisionBoxNode.get("origin");
                    int originX = origin.get(0).intValue();
                    int originY = origin.get(1).intValue();
                    int originZ = origin.get(2).intValue();

                    JsonNode size = collisionBoxNode.get("size");
                    int sizeX = size.get(0).intValue();
                    int sizeY = size.get(1).intValue();
                    int sizeZ = size.get(2).intValue();

                    collisionBox = new BoxComponent(originX, originY, originZ, sizeX, sizeY, sizeZ);
                }
            }
        }
        builder.collisionBox(collisionBox);

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

        String displayName = name;
        if (node.has("display_name")) {
            displayName = node.get("display_name").asText();
        }
        builder.displayName(displayName);

        if (node.has("friction")) {
            builder.friction(node.get("friction").floatValue());
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

        if (node.has("unit_cube")) {
            builder.unitCube(node.get("unit_cube").asBoolean());
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

        if (node.has("placement_filter")) {
            JsonNode placementFilter = node.get("placement_filter");
            if (placementFilter.isObject()) {
                if (placementFilter.has("conditions")) {
                    JsonNode conditions = placementFilter.get("conditions");
                    if (conditions.isArray()) {
                        List<PlacementConditions> filter = createPlacementFilterComponent(conditions);
                        builder.placementFilter(filter);
                    }
                }
            }
        }

        // Tags can be applied so that blocks will match return true when queried for the tag
        // Potentially useful for resource pack creators
        // Ideally we could programatically extract the tags here https://wiki.bedrock.dev/blocks/block-tags.html
        // This would let us automatically apply the correct valilla tags to blocks
        // However, its worth noting that vanilla tools do not currently honor these tags anyways
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

    /**
     * Creates the {@link BoxComponent} for the passed collision box index
     * @param id the collision box index
     * @return the {@link BoxComponent}
     */
    private BoxComponent createBoxComponent(int id) {
        // Some blocks (e.g. plants) have no collision box
        BlockCollision blockCollision = BlockUtils.getCollision(id);
        if (blockCollision == null) {
            return new BoxComponent(0, 0, 0, 0, 0, 0);
        }

        BoundingBox boundingBox = blockCollision.getBoundingBoxes()[0];

        float offsetX = (float) boundingBox.getSizeX() * 8;
        float offsetY = (float) boundingBox.getSizeY() * 8;
        float offsetZ = (float) boundingBox.getSizeZ() * 8;

        // Unfortunately we need to clamp the values here to a an effective size of one block
        // This is quite a pain for anything like fences, as the player can just jump over them
        // One possible solution would be to create invisible blocks that we use only for collision box
        // These could be placed above the block when a custom block exceeds this limit
        // I am hopeful this will be extended slightly since the geometry of blocks can be 1.875^3
        float cornerX = MathUtils.clamp((float) boundingBox.getMiddleX() * 16 - 8 - offsetX, -8, 8);
        float cornerY = MathUtils.clamp((float) boundingBox.getMiddleY() * 16 - offsetY, 0, 16);
        float cornerZ = MathUtils.clamp((float) boundingBox.getMiddleZ() * 16 - 8 - offsetZ, -8, 8);

        float sizeX = MathUtils.clamp((float) boundingBox.getSizeX() * 16, 0, 16);
        float sizeY = MathUtils.clamp((float) boundingBox.getSizeY() * 16, 0, 16);
        float sizeZ = MathUtils.clamp((float) boundingBox.getSizeZ() * 16, 0, 16);

        BoxComponent boxComponent = new BoxComponent(cornerX, cornerY, cornerZ, sizeX, sizeY, sizeZ);

        return boxComponent;
    }

    /**
     * Creates the {@link MaterialInstance} for the passed material instance node and custom block name
     * The name is used as a fallback if no texture is provided by the node
     * @param node the material instance node
     * @param name the custom block name
     * @return the {@link MaterialInstance}
     */
    private MaterialInstance createMaterialInstanceComponent(JsonNode node, String name) {
        // Set default values, and use what the user provides if they have provided something
        String texture = name;
        if (node.has("texture")) {
            texture = node.get("texture").asText();
        }

        String renderMethod = "opaque";
        if (node.has("render_method")) {
            renderMethod = node.get("render_method").asText();
        }

        boolean faceDimming = true;
        if (node.has("face_dimming")) {
            faceDimming = node.get("face_dimming").asBoolean();
        }

        boolean ambientOcclusion = true;
        if (node.has("ambient_occlusion")) {
            ambientOcclusion = node.get("ambient_occlusion").asBoolean();
        }

        return new MaterialInstance(texture, renderMethod, faceDimming, ambientOcclusion);
    }

    /**
     * Creates the list of {@link PlacementConditions} for the passed conditions node
     * @param node the conditions node
     * @return the list of {@link PlacementConditions}
     */
    private List<PlacementConditions> createPlacementFilterComponent(JsonNode node) {
        List<PlacementConditions> conditions = new ArrayList<>();

        // The structure of the placement filter component is the most complex of the current components
        // Each condition effectively seperated into an two arrays: one of allowed faces, and one of blocks/block molang queries
        node.forEach(condition -> {
            Set<Face> faces = new HashSet<>();
            if (condition.has("allowed_faces")) {
                JsonNode allowedFaces = condition.get("allowed_faces");
                if (allowedFaces.isArray()) {
                    allowedFaces.forEach(face -> {
                        faces.add(Face.valueOf(face.asText().toUpperCase()));
                    });
                }
            }

            LinkedHashMap<String, BlockFilterType> blockFilters = new LinkedHashMap<>();
            if (condition.has("block_filter")) {
                JsonNode blockFilter = condition.get("block_filter");
                if (blockFilter.isArray()) {
                    blockFilter.forEach(filter -> {
                        if (filter.isObject()) {
                            if (filter.has("tags")) {
                                JsonNode tags = filter.get("tags");
                                blockFilters.put(tags.asText(), BlockFilterType.TAG);
                            }
                        } else if (filter.isTextual()) {
                            blockFilters.put(filter.asText(), BlockFilterType.BLOCK);
                        }
                    });
                }
            }

            conditions.add(new PlacementConditions(faces, blockFilters));
        });

        return conditions;
    }

    /**
     * Creates a molang query that returns true when the given java state identifier is the active state
     * @param state the java state identifier
     * @return the molang query
     */
    private String createCustomBlockPropertyQuery(String state) {
        // This creates a molang query from the given input blockstate string
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

        // Appends the default property to ensure it can be disabled when a state without specific overrides is active
        return String.format("q.block_property('%1$s:default') == 0 && %2$s", Constants.GEYSER_NAMESPACE, query);
    }

    /**
     * Splits the given java state identifier into an array of property=value pairs
     * @param state the java state identifier
     * @return the array of property=value pairs
     */
    private String[] splitStateString(String state) {
        // Split the given state string into an array of property=value pairs
        int openBracketIndex = state.indexOf("[");
        int closeBracketIndex = state.indexOf("]");

        String cleanStates = state.substring(openBracketIndex + 1, closeBracketIndex);

        String[] pairs = cleanStates.split("\\s*,\\s*");
        
        return pairs;
    }

}
