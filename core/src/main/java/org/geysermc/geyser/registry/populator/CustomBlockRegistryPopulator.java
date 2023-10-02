package org.geysermc.geyser.registry.populator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions.Face;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.block.custom.property.PropertyType;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.level.block.GeyserCustomBlockState;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents.CustomBlockComponentsBuilder;
import org.geysermc.geyser.level.block.GeyserCustomBlockData.CustomBlockDataBuilder;
import org.geysermc.geyser.level.block.GeyserGeometryComponent.GeometryComponentBuilder;
import org.geysermc.geyser.level.block.GeyserMaterialInstance.MaterialInstanceBuilder;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.util.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomBlockRegistryPopulator {
    /**
     * The stage of population
     */
    public enum Stage {
        DEFINITION,
        VANILLA_REGISTRATION,
        NON_VANILLA_REGISTRATION,
        CUSTOM_REGISTRATION;
    }

    /**
     * Populates the custom block registries by stage
     * 
     * @param stage the stage to populate
     */
    public static void populate(Stage stage) {
        if (!GeyserImpl.getInstance().getConfig().isAddNonBedrockItems()) {
            return;
        }
        
        switch (stage) {
            case DEFINITION -> { populateBedrock(); }
            case VANILLA_REGISTRATION -> { populateVanilla(); }
            case NON_VANILLA_REGISTRATION -> { populateNonVanilla(); }
            case CUSTOM_REGISTRATION -> { registration(); }
            default -> { throw new IllegalArgumentException("Unknown stage: " + stage); }
        }
    }

    private static Set<CustomBlockData> CUSTOM_BLOCKS;
    private static Set<String> CUSTOM_BLOCK_NAMES;
    private static Int2ObjectMap<CustomBlockState> BLOCK_STATE_OVERRIDES;
    private static Map<String, CustomBlockData> CUSTOM_BLOCK_ITEM_OVERRIDES;
    private static Map<JavaBlockState, CustomBlockState> NON_VANILLA_BLOCK_STATE_OVERRIDES;

    /**
     * Initializes custom blocks defined by API
     */
    private static void populateBedrock() {
        CUSTOM_BLOCKS = new ObjectOpenHashSet<>();
        CUSTOM_BLOCK_NAMES = new ObjectOpenHashSet<>();
        BLOCK_STATE_OVERRIDES = new Int2ObjectOpenHashMap<>();
        CUSTOM_BLOCK_ITEM_OVERRIDES = new HashMap<>();
        NON_VANILLA_BLOCK_STATE_OVERRIDES = new HashMap<>();

        GeyserImpl.getInstance().getEventBus().fire(new GeyserDefineCustomBlocksEvent() {
            @Override
            public void register(@NonNull CustomBlockData customBlockData) {
                if (customBlockData.name().length() == 0) {
                    throw new IllegalArgumentException("Custom block name must have at least 1 character.");
                }
                if (!CUSTOM_BLOCK_NAMES.add(customBlockData.name())) {
                    throw new IllegalArgumentException("Another custom block was already registered under the name: " + customBlockData.name());
                }
                if (Character.isDigit(customBlockData.name().charAt(0))) {
                    throw new IllegalArgumentException("Custom block can not start with a digit. Name: " + customBlockData.name());
                }
                CUSTOM_BLOCKS.add(customBlockData);
            }

            @Override
            public void registerOverride(@NonNull String javaIdentifier, @NonNull CustomBlockState customBlockState) {
                int id = BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(javaIdentifier, -1);
                if (id == -1) {
                    throw new IllegalArgumentException("Unknown Java block state. Identifier: " + javaIdentifier);
                }
                if (!CUSTOM_BLOCKS.contains(customBlockState.block())) {
                    throw new IllegalArgumentException("Custom block is unregistered. Name: " + customBlockState.name());
                }
                CustomBlockState oldBlockState = BLOCK_STATE_OVERRIDES.put(id, customBlockState);
                if (oldBlockState != null) {
                    GeyserImpl.getInstance().getLogger().debug("Duplicate block state override for Java Identifier: " +
                            javaIdentifier + " Old override: " + oldBlockState.name() + " New override: " + customBlockState.name());
                }
            }

            @Override
            public void registerItemOverride(@NonNull String javaIdentifier, @NonNull CustomBlockData customBlockData) {
                if (!CUSTOM_BLOCKS.contains(customBlockData)) {
                    throw new IllegalArgumentException("Custom block is unregistered. Name: " + customBlockData.name());
                }
                CUSTOM_BLOCK_ITEM_OVERRIDES.put(javaIdentifier, customBlockData);
            }

            @Override
            public void registerOverride(@NonNull JavaBlockState javaBlockState, @NonNull CustomBlockState customBlockState) {
                if (!CUSTOM_BLOCKS.contains(customBlockState.block())) {
                    throw new IllegalArgumentException("Custom block is unregistered. Name: " + customBlockState.name());
                }
                NON_VANILLA_BLOCK_STATE_OVERRIDES.put(javaBlockState, customBlockState);
            }
        });
    }

    /**
     * Registers all vanilla custom blocks and skulls defined by API and mappings
     */
    private static void populateVanilla() {
        for (CustomSkull customSkull : BlockRegistries.CUSTOM_SKULLS.get().values()) {
            CUSTOM_BLOCKS.add(customSkull.getCustomBlockData());
        }

        Map<CustomBlockData, Set<Integer>> extendedCollisionBoxes = new HashMap<>();
        Map<BoxComponent, CustomBlockData> extendedCollisionBoxSet = new HashMap<>();
        MappingsConfigReader mappingsConfigReader = new MappingsConfigReader();
        mappingsConfigReader.loadBlockMappingsFromJson((key, block) -> {
            CUSTOM_BLOCKS.add(block.data());
            if (block.overrideItem()) {
                CUSTOM_BLOCK_ITEM_OVERRIDES.put(block.javaIdentifier(), block.data());
            }
            block.states().forEach((javaIdentifier, customBlockState) -> {
                int id = BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(javaIdentifier, -1);
                BLOCK_STATE_OVERRIDES.put(id, customBlockState.state());
                BoxComponent extendedCollisionBox = customBlockState.extendedCollisionBox();
                if (extendedCollisionBox != null) {
                    CustomBlockData extendedCollisionBlock = extendedCollisionBoxSet.computeIfAbsent(extendedCollisionBox, box -> {
                        CustomBlockData collisionBlock = createExtendedCollisionBlock(box, extendedCollisionBoxSet.size());
                        CUSTOM_BLOCKS.add(collisionBlock);
                        return collisionBlock;
                    });
                    extendedCollisionBoxes.computeIfAbsent(extendedCollisionBlock, k -> new HashSet<>())
                            .add(id);
                }
            });
        });
    
        BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.set(BLOCK_STATE_OVERRIDES);
        if (BLOCK_STATE_OVERRIDES.size() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + BLOCK_STATE_OVERRIDES.size() + " custom block overrides.");
        }

        BlockRegistries.CUSTOM_BLOCK_ITEM_OVERRIDES.set(CUSTOM_BLOCK_ITEM_OVERRIDES);
        if (CUSTOM_BLOCK_ITEM_OVERRIDES.size() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + CUSTOM_BLOCK_ITEM_OVERRIDES.size() + " custom block item overrides.");
        }

        BlockRegistries.EXTENDED_COLLISION_BOXES.set(extendedCollisionBoxes);
        if (extendedCollisionBoxes.size() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + extendedCollisionBoxes.size() + " custom block extended collision boxes.");
        }
    }

    /**
     * Registers all non-vanilla custom blocks defined by API
     */
    private static void populateNonVanilla() {
        BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.set(NON_VANILLA_BLOCK_STATE_OVERRIDES);
        if (NON_VANILLA_BLOCK_STATE_OVERRIDES.size() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + NON_VANILLA_BLOCK_STATE_OVERRIDES.size() + " non-vanilla block overrides.");
        }
    }

    /**
     * Registers all bedrock custom blocks defined in previous stages
     */
    private static void registration() {
        BlockRegistries.CUSTOM_BLOCKS.set(CUSTOM_BLOCKS.toArray(new CustomBlockData[0]));
        if (CUSTOM_BLOCKS.size() != 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + CUSTOM_BLOCKS.size() + " custom blocks.");
        }
    }

    /**
     * Generates and appends all custom block states to the provided list of custom block states
     * Appends the custom block states to the provided list of NBT maps
     * 
     * @param customBlock the custom block data to generate states for
     * @param blockStates the list of NBT maps to append the custom block states to
     * @param customExtBlockStates the list of custom block states to append the custom block states to
     */
    static void generateCustomBlockStates(CustomBlockData customBlock, List<NbtMap> blockStates, List<CustomBlockState> customExtBlockStates) {
        int totalPermutations = 1;
        for (CustomBlockProperty<?> property : customBlock.properties().values()) {
            totalPermutations *= property.values().size();
        }
    
        for (int i = 0; i < totalPermutations; i++) {
            NbtMapBuilder statesBuilder = NbtMap.builder();
            int permIndex = i;
            for (CustomBlockProperty<?> property : customBlock.properties().values()) {
                statesBuilder.put(property.name(), property.values().get(permIndex % property.values().size()));
                permIndex /= property.values().size();
            }
            NbtMap states = statesBuilder.build();
    
            blockStates.add(NbtMap.builder()
                    .putString("name", customBlock.identifier())
                    .putCompound("states", states)
                    .build());
            customExtBlockStates.add(new GeyserCustomBlockState(customBlock, states));
        }
    }

    /**
     * Generates and returns the block property data for the provided custom block
     * 
     * @param customBlock the custom block to generate block property data for
     * @param protocolVersion the protocol version to use for the block property data
     * @return the block property data for the provided custom block
     */
    @SuppressWarnings("unchecked")
    static BlockPropertyData generateBlockPropertyData(CustomBlockData customBlock, int protocolVersion) {
        List<NbtMap> permutations = new ArrayList<>();
        for (CustomBlockPermutation permutation : customBlock.permutations()) {
            permutations.add(NbtMap.builder()
                    .putCompound("components", CustomBlockRegistryPopulator.convertComponents(permutation.components(), protocolVersion))
                    .putString("condition", permutation.condition())
                    .build());
        }
    
        // The order that properties are defined influences the order that block states are generated
        List<NbtMap> properties = new ArrayList<>();
        for (CustomBlockProperty<?> property : customBlock.properties().values()) {
            NbtMapBuilder propertyBuilder = NbtMap.builder()
                    .putString("name", property.name());
            if (property.type() == PropertyType.booleanProp()) {
                propertyBuilder.putList("enum", NbtType.BYTE, List.of((byte) 0, (byte) 1));
            } else if (property.type() == PropertyType.integerProp()) {
                propertyBuilder.putList("enum", NbtType.INT, (List<Integer>) property.values());
            } else if (property.type() == PropertyType.stringProp()) {
                propertyBuilder.putList("enum", NbtType.STRING, (List<String>) property.values());
            }
            properties.add(propertyBuilder.build());
        }
    
        CreativeCategory creativeCategory = customBlock.creativeCategory() != null ? customBlock.creativeCategory() : CreativeCategory.NONE;
        String creativeGroup = customBlock.creativeGroup() != null ? customBlock.creativeGroup() : "";
        NbtMap propertyTag = NbtMap.builder()
                .putCompound("components", CustomBlockRegistryPopulator.convertComponents(customBlock.components(), protocolVersion))
                // this is required or the client will crash
                // in the future, this can be used to replace items in the creative inventory
                // this would require us to map https://wiki.bedrock.dev/documentation/creative-categories.html#for-blocks programatically
                .putCompound("menu_category", NbtMap.builder()
                    .putString("category", creativeCategory.internalName())
                    .putString("group", creativeGroup)
                    .putBoolean("is_hidden_in_commands", false)
                .build())
                // meaning of this version is unknown, but it's required for tags to work and should probably be checked periodically
                .putInt("molangVersion", 1)
                .putList("permutations", NbtType.COMPOUND, permutations)
                .putList("properties", NbtType.COMPOUND, properties)
                .build();
        return new BlockPropertyData(customBlock.identifier(), propertyTag);
    }

    /**
     * Converts the provided custom block components to an {@link NbtMap} to be sent to the client in the StartGame packet
     * 
     * @param components the custom block components to convert
     * @param protocolVersion the protocol version to use for the conversion
     * @return the NBT representation of the provided custom block components
     */
    private static NbtMap convertComponents(CustomBlockComponents components, int protocolVersion) {
        if (components == null) {
            return NbtMap.EMPTY;
        }

        NbtMapBuilder builder = NbtMap.builder();
        if (components.displayName() != null) {
            builder.putCompound("minecraft:display_name", NbtMap.builder()
                    .putString("value", components.displayName())
                    .build());
        }

        if (components.selectionBox() != null) {
            builder.putCompound("minecraft:selection_box", convertBox(components.selectionBox()));
        }

        if (components.collisionBox() != null) {
            builder.putCompound("minecraft:collision_box", convertBox(components.collisionBox()));
        }

        if (components.geometry() != null) {
            NbtMapBuilder geometryBuilder = NbtMap.builder();
            if (protocolVersion >= Bedrock_v594.CODEC.getProtocolVersion()) {
                geometryBuilder.putString("identifier", components.geometry().identifier());
                if (components.geometry().boneVisibility() != null) {
                    NbtMapBuilder boneVisibilityBuilder = NbtMap.builder();
                    components.geometry().boneVisibility().entrySet().forEach(
                        entry -> boneVisibilityBuilder.putString(entry.getKey(), entry.getValue()));
                    geometryBuilder.putCompound("bone_visibility", boneVisibilityBuilder.build());
                }
            } else {
                geometryBuilder.putString("value", components.geometry().identifier());
            }
            builder.putCompound("minecraft:geometry", geometryBuilder.build());
        }

        if (!components.materialInstances().isEmpty()) {
            NbtMapBuilder materialsBuilder = NbtMap.builder();
            for (Map.Entry<String, MaterialInstance> entry : components.materialInstances().entrySet()) {
                MaterialInstance materialInstance = entry.getValue();
                NbtMapBuilder materialBuilder = NbtMap.builder()
                        .putString("render_method", materialInstance.renderMethod())
                        .putBoolean("face_dimming", materialInstance.faceDimming())
                        .putBoolean("ambient_occlusion", materialInstance.faceDimming());
                // Texture can be unspecified when blocks.json is used in RP (https://wiki.bedrock.dev/blocks/blocks-stable.html#minecraft-material-instances)
                if (materialInstance.texture() != null) {
                    materialBuilder.putString("texture", materialInstance.texture());
                }
                materialsBuilder.putCompound(entry.getKey(), materialBuilder.build());
            }

            builder.putCompound("minecraft:material_instances", NbtMap.builder()
                    // we could read these, but there is no functional reason to use them at the moment
                    // they only allow you to make aliases for material instances
                    // but you could already just define the same instance twice if this was really needed
                    .putCompound("mappings", NbtMap.EMPTY)
                    .putCompound("materials", materialsBuilder.build())
                    .build());
        }

        if (components.placementFilter() != null) {
            builder.putCompound("minecraft:placement_filter", NbtMap.builder()
                    .putList("conditions", NbtType.COMPOUND, convertPlacementFilter(components.placementFilter()))
                    .build());
        }

        if (components.destructibleByMining() != null) {
            builder.putCompound("minecraft:destructible_by_mining", NbtMap.builder()
                    .putFloat("value", components.destructibleByMining())
                    .build());
        }

        if (components.friction() != null) {
            builder.putCompound("minecraft:friction", NbtMap.builder()
                    .putFloat("value", components.friction())
                    .build());
        }

        if (components.lightEmission() != null) {
            builder.putCompound("minecraft:light_emission", NbtMap.builder()
                    .putByte("emission", components.lightEmission().byteValue())
                    .build());
        }

        if (components.lightDampening() != null) {
            builder.putCompound("minecraft:light_dampening", NbtMap.builder()
                    .putByte("lightLevel", components.lightDampening().byteValue())
                    .build());
        }

        if (components.transformation() != null) {
            builder.putCompound("minecraft:transformation", NbtMap.builder()
                    .putInt("RX", MathUtils.unwrapDegreesToInt(components.transformation().rx()) / 90)
                    .putInt("RY", MathUtils.unwrapDegreesToInt(components.transformation().ry()) / 90)
                    .putInt("RZ", MathUtils.unwrapDegreesToInt(components.transformation().rz()) / 90)
                    .putFloat("SX", components.transformation().sx())
                    .putFloat("SY", components.transformation().sy())
                    .putFloat("SZ", components.transformation().sz())
                    .putFloat("TX", components.transformation().tx())
                    .putFloat("TY", components.transformation().ty())
                    .putFloat("TZ", components.transformation().tz())
                    .build());
        }

        if (components.unitCube()) {
            builder.putCompound("minecraft:unit_cube", NbtMap.EMPTY);
        }

        // place_air is not an actual component
        // We just apply a dummy event to prevent the client from trying to place a block
        // This mitigates the issue with the client sometimes double placing blocks
        if (components.placeAir()) {
            builder.putCompound("minecraft:on_player_placing", NbtMap.builder()
                    .putString("triggerType", "geyser:place_event")
                    .build());
        }

        if (!components.tags().isEmpty()) {
            components.tags().forEach(tag -> builder.putCompound("tag:" + tag, NbtMap.EMPTY));
        }

        return builder.build();
    }

    /**
     * Converts the provided box component to an {@link NbtMap}
     * 
     * @param boxComponent the box component to convert
     * @return the NBT representation of the provided box component
     */
    private static NbtMap convertBox(BoxComponent boxComponent) {
        return NbtMap.builder()
                .putBoolean("enabled", !boxComponent.isEmpty())
                .putList("origin", NbtType.FLOAT, boxComponent.originX(), boxComponent.originY(), boxComponent.originZ())
                .putList("size", NbtType.FLOAT, boxComponent.sizeX(), boxComponent.sizeY(), boxComponent.sizeZ())
                .build();
    }

    /**
     * Converts the provided placement filter to a list of {@link NbtMap}
     * 
     * @param placementFilter the placement filter to convert
     * @return the NBT representation of the provided placement filter
     */
    private static List<NbtMap> convertPlacementFilter(List<PlacementConditions> placementFilter) {
        List<NbtMap> conditions = new ArrayList<>();
        placementFilter.forEach((condition) -> {
            NbtMapBuilder conditionBuilder = NbtMap.builder();

            // allowed_faces on the network is represented by 6 bits for the 6 possible faces
            // the enum has the proper values for that face only, so we just bitwise OR them together
            byte allowedFaces = 0;
            for (Face face : condition.allowedFaces()) { allowedFaces |= (1 << face.ordinal()); }
            conditionBuilder.putByte("allowed_faces", allowedFaces);

            // block_filters is a list of either blocks or queries for block tags
            // if these match the block the player is trying to place on, the placement is allowed by the client
            List <NbtMap> blockFilters = new ArrayList<>();
            condition.blockFilters().forEach((value, type) -> {
                NbtMapBuilder blockFilterBuilder = NbtMap.builder();
                switch (type) {
                    case BLOCK -> blockFilterBuilder.putString("name", value);
                    // meaning of this version is unknown, but it's required for tags to work and should probably be checked periodically
                    case TAG -> blockFilterBuilder.putString("tags", value).putInt("tags_version", 6);
                }
                blockFilters.add(blockFilterBuilder.build());
            });
            conditionBuilder.putList("block_filters", NbtType.COMPOUND, blockFilters);
            conditions.add(conditionBuilder.build());
        });

        return conditions;
    }

    private static CustomBlockData createExtendedCollisionBlock(BoxComponent boxComponent, int extendedCollisionBlock) {
        CustomBlockData customBlockData = new CustomBlockDataBuilder()
                .name("extended_collision_" + extendedCollisionBlock)
                .components(
                    new CustomBlockComponentsBuilder()
                        .collisionBox(boxComponent)
                        .selectionBox(BoxComponent.emptyBox())
                        .materialInstance("*", new MaterialInstanceBuilder()
                            .texture("glass")
                            .renderMethod("alpha_test")
                            .faceDimming(false)
                            .ambientOcclusion(false)
                            .build())
                        .lightDampening(0)
                        .geometry(new GeometryComponentBuilder()
                            .identifier("geometry.invisible")
                            .build())
                        .build())
                .build();
        return customBlockData;
    }
}
