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
import com.github.steveice10.mc.protocol.data.game.Identifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.*;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions.BlockFilterType;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions.Face;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents.CustomBlockComponentsBuilder;
import org.geysermc.geyser.level.block.GeyserCustomBlockData.CustomBlockDataBuilder;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.mappings.util.CustomBlockComponentsMapping;
import org.geysermc.geyser.registry.mappings.util.CustomBlockMapping;
import org.geysermc.geyser.registry.mappings.util.CustomBlockStateBuilderMapping;
import org.geysermc.geyser.registry.mappings.util.CustomBlockStateMapping;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.MathUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
                        String identifier = Identifier.formalize(entry.getKey());
                        CustomBlockMapping customBlockMapping = this.readBlockMappingEntry(identifier, entry.getValue());
                        consumer.accept(identifier, customBlockMapping);
                    } catch (Exception e) {
                        GeyserImpl.getInstance().getLogger().error("Error in registering blocks for custom mapping file: " + file.toString());
                        GeyserImpl.getInstance().getLogger().error("due to entry: " + entry, e);
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

        JsonNode defaultItem = node.get("default");
        if (defaultItem != null && defaultItem.isBoolean()) {
            customItemOptions.defaultItem(defaultItem.asBoolean());
        }

        return customItemOptions.build();
    }

    @Override
    public CustomItemData readItemMappingEntry(JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item mappings entry");
        }

        JsonNode name = node.get("name");
        if (name == null || !name.isTextual() || name.asText().isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item entry has no name");
        }

        CustomItemData.Builder customItemData = CustomItemData.builder()
                .name(name.asText())
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

        if (node.has("display_handheld")) {
            customItemData.displayHandheld(node.get("display_handheld").asBoolean());
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
     * Read a block mapping entry from a JSON node and Java identifier
     * @param identifier The Java identifier of the block
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

        // If this is true, we will only register the states the user has specified rather than all the possible block states
        boolean onlyOverrideStates = node.has("only_override_states") && node.get("only_override_states").asBoolean();

        // Create the data for the overall block
        CustomBlockData.Builder customBlockDataBuilder = new CustomBlockDataBuilder()
                .name(name);

        if (BlockRegistries.JAVA_IDENTIFIER_TO_ID.get().containsKey(identifier)) {
            // There is only one Java block state to override
            CustomBlockComponentsMapping componentsMapping = createCustomBlockComponentsMapping(node, identifier, name);
            CustomBlockData blockData = customBlockDataBuilder
                    .components(componentsMapping.components())
                    .build();
            return new CustomBlockMapping(blockData, Map.of(identifier, new CustomBlockStateMapping(blockData.defaultBlockState(), componentsMapping.extendedCollisionBox())), identifier, !onlyOverrideStates);
        }

        Map<String, CustomBlockComponentsMapping> componentsMap = new LinkedHashMap<>();

        JsonNode stateOverrides = node.get("state_overrides");
        if (stateOverrides != null && stateOverrides.isObject()) {
            // Load components for specific Java block states
            Iterator<Map.Entry<String, JsonNode>> fields = stateOverrides.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> overrideEntry = fields.next();
                String state = identifier + "[" + overrideEntry.getKey() + "]";
                if (!BlockRegistries.JAVA_IDENTIFIER_TO_ID.get().containsKey(state)) {
                    throw new InvalidCustomMappingsFileException("Unknown Java block state: " + state + " for state_overrides.");
                }
                componentsMap.put(state, createCustomBlockComponentsMapping(overrideEntry.getValue(), state, name));
            }
        }
        if (componentsMap.isEmpty() && onlyOverrideStates) {
            throw new InvalidCustomMappingsFileException("Block entry for " + identifier + " has only_override_states set to true, but has no state_overrides.");
        }

        if (!onlyOverrideStates) {
            // Create components for any remaining Java block states
            BlockRegistries.JAVA_IDENTIFIER_TO_ID.get().keySet()
                    .stream()
                    .filter(s -> s.startsWith(identifier + "["))
                    .filter(Predicate.not(componentsMap::containsKey))
                    .forEach(state -> componentsMap.put(state, createCustomBlockComponentsMapping(null, state, name)));
        }

        if (componentsMap.isEmpty()) {
            throw new InvalidCustomMappingsFileException("Unknown Java block: " + identifier);
        }

        // We pass in the first state and just use the hitbox from that as the default
        // Each state will have its own so this is fine
        String firstState = componentsMap.keySet().iterator().next();
        CustomBlockComponentsMapping firstComponentsMapping = createCustomBlockComponentsMapping(node, firstState, name);
        customBlockDataBuilder.components(firstComponentsMapping.components());

        return createCustomBlockMapping(customBlockDataBuilder, componentsMap, identifier, !onlyOverrideStates);
    }

    private CustomBlockMapping createCustomBlockMapping(CustomBlockData.Builder customBlockDataBuilder, Map<String, CustomBlockComponentsMapping> componentsMap, String identifier, boolean overrideItem) {
        Map<String, LinkedHashSet<String>> valuesMap = new Object2ObjectOpenHashMap<>();

        List<CustomBlockPermutation> permutations = new ArrayList<>();
        Map<String, CustomBlockStateBuilderMapping> blockStateBuilders = new Object2ObjectOpenHashMap<>();

        // For each Java block state, extract the property values, create a CustomBlockPermutation,
        // and a CustomBlockState builder
        for (Map.Entry<String, CustomBlockComponentsMapping> entry : componentsMap.entrySet()) {
            String state = entry.getKey();
            String[] pairs = splitStateString(state);

            String[] conditions = new String[pairs.length];
            Function<CustomBlockState.Builder, CustomBlockState.Builder> blockStateBuilder = Function.identity();

            for (int i = 0; i < pairs.length; i++) {
                String[] parts = pairs[i].split("=");
                String property = parts[0];
                String value = parts[1];

                valuesMap.computeIfAbsent(property, k -> new LinkedHashSet<>())
                        .add(value);

                conditions[i] = String.format("q.block_property('%s') == '%s'", property, value);
                blockStateBuilder = blockStateBuilder.andThen(builder -> builder.stringProperty(property, value));
            }

            permutations.add(new CustomBlockPermutation(entry.getValue().components(), String.join(" && ", conditions)));
            blockStateBuilders.put(state, new CustomBlockStateBuilderMapping(blockStateBuilder.andThen(CustomBlockState.Builder::build), entry.getValue().extendedCollisionBox()));
        }

        valuesMap.forEach((key, value) -> customBlockDataBuilder.stringProperty(key, new ArrayList<>(value)));

        CustomBlockData customBlockData = customBlockDataBuilder
                .permutations(permutations)
                .build();
        // Build CustomBlockStates for each Java block state we wish to override
        Map<String, CustomBlockStateMapping> states = blockStateBuilders.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new CustomBlockStateMapping(e.getValue().builder().apply(customBlockData.blockStateBuilder()), e.getValue().extendedCollisionBox())));

        return new CustomBlockMapping(customBlockData, states, identifier, overrideItem);
    }

    /**
     * Creates a {@link CustomBlockComponents} object for the passed state override or base block node, Java block state identifier, and custom block name
     * @param node the state override or base block {@link JsonNode}
     * @param stateKey the Java block state identifier
     * @param name the name of the custom block
     * @return the {@link CustomBlockComponents} object
     */
    private CustomBlockComponentsMapping createCustomBlockComponentsMapping(JsonNode node, String stateKey, String name) {
        // This is needed to find the correct selection box for the given block
        int id = BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(stateKey, -1);
        BoxComponent boxComponent = createBoxComponent(id);
        BoxComponent extendedBoxComponent = createExtendedBoxComponent(id);
        CustomBlockComponents.Builder builder = new CustomBlockComponentsBuilder()
                .collisionBox(boxComponent)
                .selectionBox(boxComponent);

        if (node == null) {
            // No other components were defined
            return new CustomBlockComponentsMapping(builder.build(), extendedBoxComponent);
        }

        BoxComponent selectionBox = createBoxComponent(node.get("selection_box"));
        if (selectionBox != null) {
            builder.selectionBox(selectionBox);
        }
        BoxComponent collisionBox = createBoxComponent(node.get("collision_box"));
        if (collisionBox != null) {
            builder.collisionBox(collisionBox);
        }
        BoxComponent extendedCollisionBox = createBoxComponent(node.get("extended_collision_box"));
        if (extendedCollisionBox != null) {
            extendedBoxComponent = extendedCollisionBox;
        }


        // We set this to max value by default so that we may dictate the correct destroy time ourselves
        float destructibleByMining = Float.MAX_VALUE;
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
        // Ideally we could programmatically extract the tags here https://wiki.bedrock.dev/blocks/block-tags.html
        // This would let us automatically apply the correct vanilla tags to blocks
        // However, its worth noting that vanilla tools do not currently honor these tags anyway
        if (node.get("tags") instanceof ArrayNode tags) {
            Set<String> tagsSet = new ObjectOpenHashSet<>();
            tags.forEach(tag -> tagsSet.add(tag.asText()));
            builder.tags(tagsSet);
        }

        return new CustomBlockComponentsMapping(builder.build(), extendedBoxComponent);
    }

    /**
     * Creates a {@link BoxComponent} based on a Java block's collision with provided bounds and offsets
     * @param javaId the block's Java ID
     * @param heightTranslation the height translation of the box
     * @return the {@link BoxComponent}
     */
    private BoxComponent createBoxComponent(int javaId, float heightTranslation) {
        // Some blocks (e.g. plants) have no collision box
        BlockCollision blockCollision = BlockUtils.getCollision(javaId);
        if (blockCollision == null) {
            return BoxComponent.EMPTY_BOX;
        }

        Set<Float> bottomCornerXs = new ObjectOpenHashSet<>();
        Set<Float> bottomCornerYs = new ObjectOpenHashSet<>();
        Set<Float> bottomCornerZs = new ObjectOpenHashSet<>();
        Set<Float> topCornerXs = new ObjectOpenHashSet<>();
        Set<Float> topCornerYs = new ObjectOpenHashSet<>();
        Set<Float> topCornerZs = new ObjectOpenHashSet<>();
        for (BoundingBox boundingBox : blockCollision.getBoundingBoxes()) {
            float offsetX = (float) boundingBox.getSizeX() * 8;
            float offsetY = (float) boundingBox.getSizeY() * 8;
            float offsetZ = (float) boundingBox.getSizeZ() * 8;

            float bottomCornerX = (float) Math.abs(boundingBox.getMiddleX() - 1) * 16 - 8 - offsetX;
            float bottomCornerY = (float) boundingBox.getMiddleY() * 16 - offsetY;
            float bottomCornerZ = (float) boundingBox.getMiddleZ() * 16 - 8 - offsetZ;
    
            bottomCornerXs.add(bottomCornerX);
            bottomCornerYs.add(bottomCornerY);
            bottomCornerZs.add(bottomCornerZ);
    
            topCornerXs.add((float) boundingBox.getSizeX() * 16 + bottomCornerX);
            topCornerYs.add((float) boundingBox.getSizeY() * 16 + bottomCornerY);
            topCornerZs.add((float) boundingBox.getSizeZ() * 16 + bottomCornerZ);
        }

        float cornerX = MathUtils.clamp(Collections.min(bottomCornerXs), -8, 8);
        float cornerY = MathUtils.clamp(Collections.min(bottomCornerYs) + heightTranslation, 0, 16);
        float cornerZ = MathUtils.clamp(Collections.min(bottomCornerZs), -8, 8);

        float sizeX = MathUtils.clamp(Collections.max(topCornerXs) - cornerX, 0, 16);
        float sizeY = MathUtils.clamp(Collections.max(topCornerYs) - cornerY + heightTranslation, 0, 16);
        float sizeZ = MathUtils.clamp(Collections.max(topCornerZs) - cornerZ, 0 ,16);

        return new BoxComponent(cornerX, cornerY, cornerZ, sizeX, sizeY, sizeZ);
    }

    /**
     * Creates a {@link BoxComponent} based on a Java block's collision
     * @param javaId the block's Java ID
     * @return the {@link BoxComponent}
     */
    private BoxComponent createBoxComponent(int javaId) {
        return createBoxComponent(javaId, 0);
    }

    /**
     * Creates the {@link BoxComponent} for an extended collision box based on a Java block's collision
     * @param javaId the block's Java ID
     * @return the {@link BoxComponent} or null if the block's collision box would not exceed 16 y units
     */
    private BoxComponent createExtendedBoxComponent(int javaId) {
        BlockCollision blockCollision = BlockUtils.getCollision(javaId);
        if (blockCollision == null) {
            return null;
        }
        BoundingBox boundingBox = blockCollision.getBoundingBoxes()[0];
        float offsetY = (float) boundingBox.getSizeY() * 8;
        float cornerY = (float) boundingBox.getMiddleY() * 16 - offsetY;
        float sizeY = (float) boundingBox.getSizeY() * 16;
        if (cornerY > 16 || sizeY > 16) {
            return createBoxComponent(javaId, -16);
        }
        return null;
    }

    /**
     * Creates a {@link BoxComponent} from a JSON Node
     * @param node the JSON node
     * @return the {@link BoxComponent}
     */
    private BoxComponent createBoxComponent(JsonNode node) {
        if (node != null && node.isObject()) {
            if (node.has("origin") && node.has("size")) {
                JsonNode origin = node.get("origin");
                float originX = origin.get(0).floatValue();
                float originY = origin.get(1).floatValue();
                float originZ = origin.get(2).floatValue();

                JsonNode size = node.get("size");
                float sizeX = size.get(0).floatValue();
                float sizeY = size.get(1).floatValue();
                float sizeZ = size.get(2).floatValue();

                return new BoxComponent(originX, originY, originZ, sizeX, sizeY, sizeZ);
            }
        }
        return null;
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
        // Each condition effectively separated into two arrays: one of allowed faces, and one of blocks/block Molang queries
        node.forEach(condition -> {
            Set<Face> faces = EnumSet.noneOf(Face.class);
            if (condition.has("allowed_faces")) {
                JsonNode allowedFaces = condition.get("allowed_faces");
                if (allowedFaces.isArray()) {
                    allowedFaces.forEach(face -> faces.add(Face.valueOf(face.asText().toUpperCase())));
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
     * Splits the given java state identifier into an array of property=value pairs
     * @param state the java state identifier
     * @return the array of property=value pairs
     */
    private String[] splitStateString(String state) {
        int openBracketIndex = state.indexOf("[");

        String states = state.substring(openBracketIndex + 1, state.length() - 1);
        return states.split(",");
    }

}
