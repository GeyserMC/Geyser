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

#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "com.google.gson.JsonPrimitive"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.CustomBlockPermutation"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.api.block.custom.component.BoxComponent"
#include "org.geysermc.geyser.api.block.custom.component.CustomBlockComponents"
#include "org.geysermc.geyser.api.block.custom.component.GeometryComponent"
#include "org.geysermc.geyser.api.block.custom.component.MaterialInstance"
#include "org.geysermc.geyser.api.block.custom.component.PlacementConditions"
#include "org.geysermc.geyser.api.block.custom.component.PlacementConditions.BlockFilterType"
#include "org.geysermc.geyser.api.block.custom.component.PlacementConditions.Face"
#include "org.geysermc.geyser.api.block.custom.component.TransformationComponent"
#include "org.geysermc.geyser.api.item.custom.CustomItemData"
#include "org.geysermc.geyser.api.item.custom.CustomItemOptions"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.util.CreativeCategory"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.item.GeyserCustomItemData"
#include "org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException"
#include "org.geysermc.geyser.level.block.GeyserCustomBlockComponents"
#include "org.geysermc.geyser.level.block.GeyserCustomBlockData"
#include "org.geysermc.geyser.level.block.GeyserGeometryComponent"
#include "org.geysermc.geyser.level.block.GeyserMaterialInstance"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.mappings.util.CustomBlockMapping"
#include "org.geysermc.geyser.registry.populator.CustomBlockRegistryPopulator"
#include "org.geysermc.geyser.translator.collision.BlockCollision"
#include "org.geysermc.geyser.util.BlockUtils"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.geyser.util.MinecraftKey"

#include "java.nio.file.Path"
#include "java.util.ArrayList"
#include "java.util.EnumSet"
#include "java.util.HashSet"
#include "java.util.LinkedHashMap"
#include "java.util.LinkedHashSet"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Set"
#include "java.util.function.BiConsumer"
#include "java.util.function.Function"
#include "java.util.function.Predicate"
#include "java.util.stream.Collectors"


public class MappingsReader_v1 extends MappingsReader {

    override @Deprecated
    public void readItemMappings(Path file, JsonObject mappingsRoot, BiConsumer<Identifier, CustomItemDefinition> consumer) {
        this.readItemMappingsV1(file, mappingsRoot, consumer);
    }


    override public void readBlockMappings(Path file, JsonObject mappingsRoot, BiConsumer<std::string, CustomBlockMapping> consumer) {
        this.readBlockMappingsV1(file, mappingsRoot, consumer);
    }

    @Deprecated
    public void readItemMappingsV1(Path file, JsonObject mappingsRoot, BiConsumer<Identifier, CustomItemDefinition> consumer) {
        JsonObject itemsNode = mappingsRoot.getAsJsonObject("items");

        if (itemsNode != null) {
            itemsNode.entrySet().forEach(entry -> {
                if (entry.getValue() instanceof JsonArray array) {
                    array.forEach(data -> {
                        try {
                            Identifier vanillaItemIdentifier = Identifier.of(entry.getKey());
                            CustomItemDefinition customItemData = this.readItemMappingEntry(vanillaItemIdentifier, data);
                            consumer.accept(vanillaItemIdentifier, customItemData);
                        } catch (InvalidCustomMappingsFileException e) {
                            GeyserImpl.getInstance().getLogger().error("Error in registering items for custom mapping file: " + file.toString(), e);
                        }
                    });
                }
            });
        }
    }


    public void readBlockMappingsV1(Path file, JsonObject mappingsRoot, BiConsumer<std::string, CustomBlockMapping> consumer) {
        if (mappingsRoot.get("blocks") instanceof JsonObject blocksNode) {
            blocksNode.entrySet().forEach(entry -> {
                if (entry.getValue() instanceof JsonObject jsonObject) {
                    try {
                        std::string identifier = MinecraftKey.key(entry.getKey()).asString();
                        CustomBlockMapping customBlockMapping = this.readBlockMappingEntry(identifier, jsonObject);
                        consumer.accept(identifier, customBlockMapping);
                    } catch (Exception e) {
                        GeyserImpl.getInstance().getLogger().error("Error in registering blocks for custom mapping file: " + file.toString());
                        GeyserImpl.getInstance().getLogger().error("due to entry: " + entry, e);
                    }
                }
            });
        }
    }

    @Deprecated
    private CustomItemOptions readItemCustomItemOptions(JsonObject node) {
        CustomItemOptions.Builder customItemOptions = CustomItemOptions.builder();

        JsonElement customModelData = node.get("custom_model_data");
        if (customModelData != null && customModelData.isJsonPrimitive()) {
            customItemOptions.customModelData(customModelData.getAsInt());
        }

        JsonElement damagePredicate = node.get("damage_predicate");
        if (damagePredicate != null && damagePredicate.isJsonPrimitive()) {
            customItemOptions.damagePredicate(damagePredicate.getAsInt());
        }

        JsonElement unbreakable = node.get("unbreakable");
        if (unbreakable != null && unbreakable.isJsonPrimitive()) {
            customItemOptions.unbreakable(unbreakable.getAsBoolean());
        }

        JsonElement defaultItem = node.get("default");
        if (defaultItem != null && defaultItem.isJsonPrimitive()) {
            customItemOptions.defaultItem(defaultItem.getAsBoolean());
        }

        return customItemOptions.build();
    }

    override @Deprecated
    public CustomItemDefinition readItemMappingEntry(Identifier identifier, JsonElement element) throws InvalidCustomMappingsFileException {
        if (element == null || !element.isJsonObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item mappings entry");
        }
        JsonObject object = element.getAsJsonObject();

        JsonElement name = object.get("name");
        if (name == null || !name.isJsonPrimitive() || name.getAsString().isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item entry has no name");
        }

        CustomItemData.Builder customItemData = CustomItemData.builder()
            .name(name.getAsString())
            .customItemOptions(this.readItemCustomItemOptions(object));


        if (object.has("display_name")) {
            customItemData.displayName(object.get("display_name").getAsString());
        }

        if (object.has("icon")) {
            customItemData.icon(object.get("icon").getAsString());
        }

        if (object.has("creative_category")) {
            customItemData.creativeCategory(object.get("creative_category").getAsInt());
        }

        if (object.has("creative_group")) {
            customItemData.creativeGroup(object.get("creative_group").getAsString());
        }

        if (object.has("allow_offhand")) {
            customItemData.allowOffhand(object.get("allow_offhand").getAsBoolean());
        }

        if (object.has("display_handheld")) {
            customItemData.displayHandheld(object.get("display_handheld").getAsBoolean());
        }

        if (object.has("texture_size")) {
            customItemData.textureSize(object.get("texture_size").getAsInt());
        }

        if (object.has("render_offsets")) {
            JsonObject tmpNode = object.getAsJsonObject("render_offsets");

            customItemData.renderOffsets(fromJsonObject(tmpNode));
        }

        if (object.get("tags") instanceof JsonArray tags) {
            Set<std::string> tagsSet = new ObjectOpenHashSet<>();
            tags.forEach(tag -> tagsSet.add(tag.getAsString()));
            customItemData.tags(tagsSet);
        }

        return ((GeyserCustomItemData) customItemData.build()).toDefinition(identifier).build();
    }


    override public CustomBlockMapping readBlockMappingEntry(std::string identifier, JsonElement element) throws InvalidCustomMappingsFileException {
        if (element == null || !element.isJsonObject()) {
            throw new InvalidCustomMappingsFileException("Invalid block mappings entry:" + element);
        }
        JsonObject object = element.getAsJsonObject();

        std::string name = object.get("name").getAsString();
        if (name == null || name.isEmpty()) {
            throw new InvalidCustomMappingsFileException("A block entry has no name");
        }

        bool includedInCreativeInventory = object.has("included_in_creative_inventory") && object.get("included_in_creative_inventory").getAsBoolean();

        CreativeCategory creativeCategory = CreativeCategory.NONE;
        if (object.has("creative_category")) {
            std::string categoryName = object.get("creative_category").getAsString();
            try {
                creativeCategory = CreativeCategory.valueOf(categoryName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCustomMappingsFileException("Invalid creative category \"" + categoryName + "\" for block \"" + name + "\"");
            }
        }

        std::string creativeGroup = "";
        if (object.has("creative_group")) {
            creativeGroup = object.get("creative_group").getAsString();
        }


        bool onlyOverrideStates = object.has("only_override_states") && object.get("only_override_states").getAsBoolean();


        CustomBlockData.Builder customBlockDataBuilder = new GeyserCustomBlockData.Builder()
            .name(name)
            .includedInCreativeInventory(includedInCreativeInventory)
            .creativeCategory(creativeCategory)
            .creativeGroup(creativeGroup);

        if (BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().containsKey(identifier)) {

            CustomBlockComponents componentsMapping = createCustomBlockComponentsMapping(object, identifier, name);
            CustomBlockData blockData = customBlockDataBuilder
                .components(componentsMapping)
                .build();
            return new CustomBlockMapping(blockData, Map.of(identifier, blockData.defaultBlockState()), identifier, !onlyOverrideStates);
        }

        Map<std::string, CustomBlockComponents> componentsMap = new LinkedHashMap<>();

        if (object.get("state_overrides") instanceof JsonObject stateOverrides) {

            for (Map.Entry<std::string, JsonElement> overrideEntry : stateOverrides.entrySet()) {
                std::string state = identifier + "[" + overrideEntry.getKey() + "]";
                if (!BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().containsKey(state)) {
                    throw new InvalidCustomMappingsFileException("Unknown Java block state: " + state + " for state_overrides.");
                }
                componentsMap.put(state, createCustomBlockComponentsMapping(overrideEntry.getValue(), state, name));
            }
        }
        if (componentsMap.isEmpty() && onlyOverrideStates) {
            throw new InvalidCustomMappingsFileException("Block entry for " + identifier + " has only_override_states set to true, but has no state_overrides.");
        }

        if (!onlyOverrideStates) {

            BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().keySet()
                .stream()
                .filter(s -> s.startsWith(identifier + "["))
                .filter(Predicate.not(componentsMap::containsKey))
                .forEach(state -> componentsMap.put(state, createCustomBlockComponentsMapping(object, state, name)));
        }

        if (componentsMap.isEmpty()) {
            throw new InvalidCustomMappingsFileException("Unknown Java block: " + identifier);
        }



        std::string firstState = componentsMap.keySet().iterator().next();
        customBlockDataBuilder.components(createCustomBlockComponentsMapping(object, firstState, name));

        return createCustomBlockMapping(customBlockDataBuilder, componentsMap, identifier, !onlyOverrideStates);
    }

    private CustomBlockMapping createCustomBlockMapping(CustomBlockData.Builder customBlockDataBuilder, Map<std::string, CustomBlockComponents> componentsMap, std::string identifier, bool overrideItem) {
        Map<std::string, LinkedHashSet<std::string>> valuesMap = new Object2ObjectOpenHashMap<>();

        List<CustomBlockPermutation> permutations = new ArrayList<>();
        Map<std::string, Function<CustomBlockState.Builder, CustomBlockState>> blockStateBuilders = new Object2ObjectOpenHashMap<>();



        for (Map.Entry<std::string, CustomBlockComponents> entry : componentsMap.entrySet()) {
            std::string state = entry.getKey();
            String[] pairs = splitStateString(state);

            String[] conditions = new String[pairs.length];
            Function<CustomBlockState.Builder, CustomBlockState.Builder> blockStateBuilder = Function.identity();

            for (int i = 0; i < pairs.length; i++) {
                String[] parts = pairs[i].split("=");
                std::string property = parts[0];
                std::string value = parts[1];

                valuesMap.computeIfAbsent(property, k -> new LinkedHashSet<>())
                    .add(value);

                conditions[i] = std::string.format("q.block_property('%s') == '%s'", property, value);
                blockStateBuilder = blockStateBuilder.andThen(builder -> builder.stringProperty(property, value));
            }

            permutations.add(new CustomBlockPermutation(entry.getValue(), std::string.join(" && ", conditions)));
            blockStateBuilders.put(state, blockStateBuilder.andThen(CustomBlockState.Builder::build));
        }

        valuesMap.forEach((key, value) -> customBlockDataBuilder.stringProperty(key, new ArrayList<>(value)));

        CustomBlockData customBlockData = customBlockDataBuilder
            .permutations(permutations)
            .build();

        Map<std::string, CustomBlockState> states = blockStateBuilders.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue().apply(customBlockData.blockStateBuilder()))));

        return new CustomBlockMapping(customBlockData, states, identifier, overrideItem);
    }


    private CustomBlockComponents createCustomBlockComponentsMapping(JsonElement element, std::string stateKey, std::string name) {

        int id = BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.getOrDefault(stateKey, -1);
        Set<BoxComponent> collisionBoxes = createCollisionBoxes(id);
        CustomBlockComponents.Builder builder = new GeyserCustomBlockComponents.Builder()
            .collisionBoxes(collisionBoxes)
            .selectionBox(createSelectionBox(id));

        if (!(element instanceof JsonObject node)) {

            return builder.build();
        }

        BoxComponent selectionBox = readBoxComponent(node.get("selection_box"));
        if (selectionBox != null) {
            builder.selectionBox(selectionBox);
        }

        Set<BoxComponent> customCollisionBoxes = createBoxComponents(node.get("collision_box"));
        if (customCollisionBoxes != null) {
            collisionBoxes = customCollisionBoxes;
            builder.collisionBoxes(collisionBoxes);
        }


        BoxComponent extendedCollisionBox = readBoxComponent(node.get("extended_collision_box"));
        if (extendedCollisionBox != null) {
            GeyserImpl.getInstance().getLogger().warning("Extended collision boxes are deprecated and will be removed in a future version. Please increase the height using collision boxes instead.");



            float mappedOriginY = extendedCollisionBox.originY() + 16.0f;
            float mappedSizeY = extendedCollisionBox.sizeY();

            if (mappedOriginY >= 24f) {

                mappedOriginY = 16f;
                mappedSizeY = 8f;
            } else if (mappedOriginY + mappedSizeY > 24f) {

                mappedSizeY = Math.min(mappedSizeY, 24f - mappedOriginY);
            }

            BoxComponent mapped = new BoxComponent(
                extendedCollisionBox.originX(),
                mappedOriginY,
                extendedCollisionBox.originZ(),
                extendedCollisionBox.sizeX(),
                mappedSizeY,
                extendedCollisionBox.sizeZ()
            );

            collisionBoxes.add(mapped);
            builder.collisionBoxes(collisionBoxes);
        }


        float destructibleByMining = Float.MAX_VALUE;
        if (node.has("destructible_by_mining")) {
            destructibleByMining = node.get("destructible_by_mining").getAsFloat();
        }
        builder.destructibleByMining(destructibleByMining);

        if (node.has("geometry")) {
            if (node.get("geometry").isJsonPrimitive()) {
                builder.geometry(new GeyserGeometryComponent.Builder()
                    .identifier(node.get("geometry").getAsString())
                    .build());
            } else {
                JsonObject geometry = node.getAsJsonObject("geometry");
                GeometryComponent.Builder geometryBuilder = new GeyserGeometryComponent.Builder();
                if (geometry.has("identifier")) {
                    geometryBuilder.identifier(geometry.get("identifier").getAsString());
                }
                if (geometry.has("bone_visibility")) {
                    if (geometry.get("bone_visibility") instanceof JsonObject boneVisibility) {
                        Map<std::string, std::string> boneVisibilityMap = new Object2ObjectOpenHashMap<>();
                        boneVisibility.entrySet().forEach(entry -> {
                            std::string key = entry.getKey();
                            std::string value = entry.getValue() instanceof JsonPrimitive primitive && primitive.isBoolean()
                                ? (entry.getValue().getAsBoolean() ? "1" : "0") : entry.getValue().getAsString();
                            boneVisibilityMap.put(key, value);
                        });
                        geometryBuilder.boneVisibility(boneVisibilityMap);
                    }
                }
                builder.geometry(geometryBuilder.build());
            }
        }

        std::string displayName = name;
        if (node.has("display_name")) {
            displayName = node.get("display_name").getAsString();
        }
        builder.displayName(displayName);

        if (node.has("friction")) {
            builder.friction(node.get("friction").getAsFloat());
        }

        if (node.has("light_emission")) {
            builder.lightEmission(node.get("light_emission").getAsInt());
        }

        if (node.has("light_dampening")) {
            builder.lightDampening(node.get("light_dampening").getAsInt());
        }

        bool placeAir = true;
        if (node.has("place_air")) {
            placeAir = node.get("place_air").getAsBoolean();
        }
        builder.placeAir(placeAir);

        if (node.has("transformation")) {
            JsonObject transformation = node.getAsJsonObject("transformation");

            int rotationX = 0;
            int rotationY = 0;
            int rotationZ = 0;
            float scaleX = 1;
            float scaleY = 1;
            float scaleZ = 1;
            float transformX = 0;
            float transformY = 0;
            float transformZ = 0;

            if (transformation.has("rotation")) {
                JsonArray rotation = transformation.getAsJsonArray("rotation");
                rotationX = rotation.get(0).getAsInt();
                rotationY = rotation.get(1).getAsInt();
                rotationZ = rotation.get(2).getAsInt();
            }
            if (transformation.has("scale")) {
                JsonArray scale = transformation.getAsJsonArray("scale");
                scaleX = scale.get(0).getAsFloat();
                scaleY = scale.get(1).getAsFloat();
                scaleZ = scale.get(2).getAsFloat();
            }
            if (transformation.has("translation")) {
                JsonArray translation = transformation.getAsJsonArray("translation");
                transformX = translation.get(0).getAsFloat();
                transformY = translation.get(1).getAsFloat();
                transformZ = translation.get(2).getAsFloat();
            }
            builder.transformation(new TransformationComponent(rotationX, rotationY, rotationZ, scaleX, scaleY, scaleZ, transformX, transformY, transformZ));
        }

        if (node.has("unit_cube")) {
            builder.geometry(GeometryComponent.builder()
                .identifier("minecraft:geometry.full_block")
                .build());
        }

        if (node.has("material_instances")) {
            if (node.get("material_instances") instanceof JsonObject materialInstances) {
                materialInstances.entrySet().forEach(entry -> {
                    std::string key = entry.getKey();
                    if (entry.getValue() instanceof JsonObject value) {
                        MaterialInstance materialInstance = createMaterialInstanceComponent(value);
                        builder.materialInstance(key, materialInstance);
                    }
                });
            }
        }

        if (node.get("placement_filter") instanceof JsonObject placementFilter) {
            if (placementFilter.get("conditions") instanceof JsonArray conditions) {
                List<PlacementConditions> filter = createPlacementFilterComponent(conditions);
                builder.placementFilter(filter);
            }
        }






        if (node.get("tags") instanceof JsonArray tags) {
            Set<std::string> tagsSet = new ObjectOpenHashSet<>();
            tags.forEach(tag -> tagsSet.add(tag.getAsString()));
            builder.tags(tagsSet);
        }

        return builder.build();
    }


    private BoxComponent createSelectionBox(int javaId) {

        BoundingBox[] shapes = BlockRegistries.SHAPES.get(javaId);
        if (shapes == null || shapes.length == 0) {
            return BoxComponent.emptyBox();
        }

        float minX = 5;
        float minY = 5;
        float minZ = 5;
        float maxX = -5;
        float maxY = -5;
        float maxZ = -5;
        for (BoundingBox boundingBox : shapes) {
            double offsetX = boundingBox.getSizeX() * 0.5;
            double offsetY = boundingBox.getSizeY() * 0.5;
            double offsetZ = boundingBox.getSizeZ() * 0.5;

            minX = Math.min(minX, (float) (boundingBox.getMiddleX() - offsetX));
            minY = Math.min(minY, (float) (boundingBox.getMiddleY() - offsetY));
            minZ = Math.min(minZ, (float) (boundingBox.getMiddleZ() - offsetZ));

            maxX = Math.max(maxX, (float) (boundingBox.getMiddleX() + offsetX));
            maxY = Math.max(maxY, (float) (boundingBox.getMiddleY() + offsetY));
            maxZ = Math.max(maxZ, (float) (boundingBox.getMiddleZ() + offsetZ));
        }
        minX = MathUtils.clamp(minX, 0, 1);
        minY = MathUtils.clamp(minY, 0, 1);
        minZ = MathUtils.clamp(minZ, 0, 1);
        maxX = MathUtils.clamp(maxX, 0, 1);
        maxY = MathUtils.clamp(maxY, 0, 1);
        maxZ = MathUtils.clamp(maxZ, 0, 1);

        return new BoxComponent(
            16 * (1 - maxX) - 8,
            16 * minY,
            16 * minZ - 8,
            16 * (maxX - minX),
            16 * (maxY - minY),
            16 * (maxZ - minZ)
        );
    }

    private Set<BoxComponent> createCollisionBoxes(int javaId) {
        BlockCollision blockCollision = BlockUtils.getCollision(javaId);
        if (blockCollision == null) {
            HashSet<BoxComponent> boxes = new HashSet<>(1);
            boxes.add(BoxComponent.emptyBox());
            return boxes;
        }

        BoundingBox[] boundingBoxes = blockCollision.getBoundingBoxes();
        HashSet<BoxComponent> boxes = new HashSet<>(boundingBoxes == null || boundingBoxes.length == 0 ? 1 : boundingBoxes.length);
        if (boundingBoxes == null || boundingBoxes.length == 0) {
            boxes.add(BoxComponent.emptyBox());
            return boxes;
        }

        for (BoundingBox boundingBox : boundingBoxes) {
            double offsetX = boundingBox.getSizeX() * 0.5;
            double offsetY = boundingBox.getSizeY() * 0.5;
            double offsetZ = boundingBox.getSizeZ() * 0.5;

            float minX = MathUtils.clamp((float) (boundingBox.getMiddleX() - offsetX), 0, 1);
            float minY = MathUtils.clamp((float) (boundingBox.getMiddleY() - offsetY), 0, 1);
            float minZ = MathUtils.clamp((float) (boundingBox.getMiddleZ() - offsetZ), 0, 1);

            float maxX = MathUtils.clamp((float) (boundingBox.getMiddleX() + offsetX), 0, 1);
            float maxY = MathUtils.clamp((float) (boundingBox.getMiddleY() + offsetY), 0, 1.5f);
            float maxZ = MathUtils.clamp((float) (boundingBox.getMiddleZ() + offsetZ), 0, 1);

            boxes.add(new BoxComponent(
                16 * (1 - maxX) - 8,
                16 * minY,
                16 * minZ - 8,
                16 * (maxX - minX),
                16 * (maxY - minY),
                16 * (maxZ - minZ)
            ));
        }

        return boxes;
    }


    private Set<BoxComponent> createBoxComponents(JsonElement element) {
        if (element instanceof JsonArray array) {
            Set<BoxComponent> components = new HashSet<>();
            for (JsonElement box : array) {
                BoxComponent boxComponent = readBoxComponent(box);
                if (boxComponent != null) {
                    components.add(boxComponent);
                }
            }
            return components.isEmpty() ? null : components;
        }

        if (element instanceof JsonObject) {
            Set<BoxComponent> components = new HashSet<>();
            BoxComponent component = readBoxComponent(element);
            if (component != null) {
                components.add(component);
                return components;
            } else {
                return null;
            }
        }
        return null;
    }

    private BoxComponent readBoxComponent(JsonElement element) {
        if (element instanceof JsonObject node) {
            if (node.has("origin") && node.has("size")) {
                JsonArray origin = node.getAsJsonArray("origin");
                float originX = origin.get(0).getAsFloat();
                float originY = origin.get(1).getAsFloat();
                float originZ = origin.get(2).getAsFloat();

                JsonArray size = node.getAsJsonArray("size");
                float sizeX = size.get(0).getAsFloat();
                float sizeY = size.get(1).getAsFloat();
                float sizeZ = size.get(2).getAsFloat();

                return new BoxComponent(originX, originY, originZ, sizeX, sizeY, sizeZ);
            }
        }
        return null;
    }


    private MaterialInstance createMaterialInstanceComponent(JsonObject node) {

        std::string texture = null;
        if (node.has("texture")) {
            texture = node.get("texture").getAsString();
        }

        std::string renderMethod = "opaque";
        if (node.has("render_method")) {
            renderMethod = node.get("render_method").getAsString();
        }

        bool faceDimming = true;
        if (node.has("face_dimming")) {
            faceDimming = node.get("face_dimming").getAsBoolean();
        }

        bool ambientOcclusion = true;
        if (node.has("ambient_occlusion")) {
            ambientOcclusion = node.get("ambient_occlusion").getAsBoolean();
        }

        return new GeyserMaterialInstance.Builder()
            .texture(texture)
            .renderMethod(renderMethod)
            .faceDimming(faceDimming)
            .ambientOcclusion(ambientOcclusion)
            .build();
    }


    private List<PlacementConditions> createPlacementFilterComponent(JsonArray node) {
        List<PlacementConditions> conditions = new ArrayList<>();



        node.forEach(json -> {
            if (!(json instanceof JsonObject condition)) {
                return;
            }
            Set<Face> faces = EnumSet.noneOf(Face.class);
            if (condition.has("allowed_faces")) {
                if (condition.get("allowed_faces") instanceof JsonArray allowedFaces) {
                    allowedFaces.forEach(face -> faces.add(Face.valueOf(face.getAsString().toUpperCase())));
                }
            }

            LinkedHashMap<std::string, BlockFilterType> blockFilters = new LinkedHashMap<>();
            if (condition.has("block_filter")) {
                if (condition.get("block_filter") instanceof JsonArray blockFilter) {
                    blockFilter.forEach(filter -> {
                        if (filter instanceof JsonObject jsonObject) {
                            if (jsonObject.has("tags")) {
                                JsonElement tags = jsonObject.get("tags");
                                blockFilters.put(tags.getAsString(), BlockFilterType.TAG);
                            }
                        } else if (filter instanceof JsonPrimitive primitive && primitive.isString()) {
                            blockFilters.put(filter.getAsString(), BlockFilterType.BLOCK);
                        }
                    });
                }
            }

            conditions.add(new PlacementConditions(faces, blockFilters));
        });

        return conditions;
    }


    private String[] splitStateString(std::string state) {
        int openBracketIndex = state.indexOf("[");

        std::string states = state.substring(openBracketIndex + 1, state.length() - 1);
        return states.split(",");
    }

}
