package org.geysermc.geyser.registry.populator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.block.custom.property.PropertyType;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.level.block.GeyserCustomBlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.CustomSkull;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.BlockPropertyData;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class CustomBlockRegistryPopulator {

    static void registerCustomBedrockBlocks() {
        if (!GeyserImpl.getInstance().getConfig().isAddCustomBlocks()) {
            return;
        }
        Set<String> customBlockNames = new ObjectOpenHashSet<>();
        Set<CustomBlockData> customBlocks = new ObjectOpenHashSet<>();
        Int2ObjectMap<CustomBlockState> blockStateOverrides = new Int2ObjectOpenHashMap<>();
        GeyserImpl.getInstance().getEventBus().fire(new GeyserDefineCustomBlocksEvent() {
            @Override
            public void registerCustomBlock(@NonNull CustomBlockData customBlockData) {
                if (customBlockData.name().length() == 0) {
                    throw new IllegalArgumentException("Custom block name must have at least 1 character.");
                }
                if (!customBlockNames.add(customBlockData.name())) {
                    throw new IllegalArgumentException("Another custom block was already registered under the name: " + customBlockData.name());
                }
                if (Character.isDigit(customBlockData.name().charAt(0))) {
                    throw new IllegalArgumentException("Custom block can not start with a digit. Name: " + customBlockData.name());
                }
                customBlocks.add(customBlockData);
            }
    
            @Override
            public void registerBlockStateOverride(@NonNull String javaIdentifier, @NonNull CustomBlockState customBlockState) {
                int id = BlockRegistries.JAVA_IDENTIFIERS.getOrDefault(javaIdentifier, -1);
                if (id == -1) {
                    throw new IllegalArgumentException("Unknown Java block state. Identifier: " + javaIdentifier);
                }
                if (!customBlocks.contains(customBlockState.block())) {
                    throw new IllegalArgumentException("Custom block is unregistered. Name: " + customBlockState.name());
                }
                CustomBlockState oldBlockState = blockStateOverrides.put(id, customBlockState);
                if (oldBlockState != null) {
                    // TODO should this be an error? Allow extensions to query block state overrides?
                    GeyserImpl.getInstance().getLogger().debug("Duplicate block state override for Java Identifier: " +
                            javaIdentifier + " Old override: " + oldBlockState.name() + " New override: " + customBlockState.name());
                }
            }
        });
    
        for (CustomSkull customSkull : BlockRegistries.CUSTOM_SKULLS.get().values()) {
            customBlocks.add(customSkull.getCustomBlockData());
        }
    
        BlockRegistries.CUSTOM_BLOCKS.set(customBlocks.toArray(new CustomBlockData[0]));
        GeyserImpl.getInstance().getLogger().debug("Registered " + customBlocks.size() + " custom blocks.");
    
        BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.set(blockStateOverrides);
        GeyserImpl.getInstance().getLogger().debug("Registered " + blockStateOverrides.size() + " custom block overrides.");
    }

    static void generateCustomBlockStates(CustomBlockData customBlock, List<NbtMap> blockStates, List<CustomBlockState> customExtBlockStates, int stateVersion) {
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
                    .putInt("version", stateVersion)
                    .putCompound("states", states)
                    .build());
            customExtBlockStates.add(new GeyserCustomBlockState(customBlock, states));
        }
    }

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
            if (property.type() == PropertyType.BOOLEAN) {
                propertyBuilder.putList("enum", NbtType.BYTE, List.of((byte) 0, (byte) 1));
            } else if (property.type() == PropertyType.INTEGER) {
                propertyBuilder.putList("enum", NbtType.INT, (List<Integer>) property.values());
            } else if (property.type() == PropertyType.STRING) {
                propertyBuilder.putList("enum", NbtType.STRING, (List<String>) property.values());
            }
            properties.add(propertyBuilder.build());
        }
    
        NbtMap propertyTag = NbtMap.builder()
                .putCompound("components", CustomBlockRegistryPopulator.convertComponents(customBlock.components(), protocolVersion))
                .putCompound("menu_category", NbtMap.builder()
                    .putString("category", "none")
                    .putString("group", "")
                    .putBoolean("is_hidden_in_commands", false)
                .build())
                .putInt("molangVersion", 1)
                .putList("permutations", NbtType.COMPOUND, permutations)
                .putList("properties", NbtType.COMPOUND, properties)
                .build();
        return new BlockPropertyData(customBlock.identifier(), propertyTag);
    }

    static NbtMap convertComponents(CustomBlockComponents components, int protocolVersion) {
        if (components == null) {
            return NbtMap.EMPTY;
        }
        NbtMapBuilder builder = NbtMap.builder();
        if (components.selectionBox() != null) {
            builder.putCompound("minecraft:selection_box", convertBox(components.selectionBox()));
        }
        if (components.collisionBox() != null) {
            builder.putCompound("minecraft:collision_box", convertBox(components.collisionBox()));
        }
        if (components.geometry() != null) {
            builder.putCompound("minecraft:geometry", NbtMap.builder()
                    .putString("value", components.geometry())
                    .build());
        }
        if (!components.materialInstances().isEmpty()) {
            NbtMapBuilder materialsBuilder = NbtMap.builder();
            for (Map.Entry<String, MaterialInstance> entry : components.materialInstances().entrySet()) {
                MaterialInstance materialInstance = entry.getValue();
                materialsBuilder.putCompound(entry.getKey(), NbtMap.builder()
                        .putString("texture", materialInstance.texture())
                        .putString("render_method", materialInstance.renderMethod())
                        .putBoolean("face_dimming", materialInstance.faceDimming())
                        .putBoolean("ambient_occlusion", materialInstance.faceDimming())
                        .build());
            }
            builder.putCompound("minecraft:material_instances", NbtMap.builder()
                    .putCompound("mappings", NbtMap.EMPTY)
                    .putCompound("materials", materialsBuilder.build())
                    .build());
        }
        if (components.destroyTime() != null) {
            builder.putCompound("minecraft:destructible_by_mining", NbtMap.builder()
                    .putFloat("value", components.destroyTime())
                    .build());
        }
        if (components.friction() != null) {
            builder.putCompound("minecraft:friction", NbtMap.builder()
                    .putFloat("value", components.friction())
                    .build());
        }
        if (components.lightEmission() != null) {
            builder.putCompound("minecraft:light_emission", NbtMap.builder()
                    .putInt("value", components.lightEmission())
                    .build());
        }
        if (components.lightDampening() != null) {
            builder.putCompound("minecraft:block_light_filter", NbtMap.builder()
                    .putByte("value", components.lightDampening().byteValue())
                    .build());
        }
        if (components.rotation() != null) {
            builder.putCompound("minecraft:rotation", NbtMap.builder()
                    .putFloat("x", components.rotation().x())
                    .putFloat("y", components.rotation().y())
                    .putFloat("z", components.rotation().z())
                    .build());
        }
        return builder.build();
    }

    private static NbtMap convertBox(BoxComponent boxComponent) {
        return NbtMap.builder()
                .putBoolean("enabled", !boxComponent.isEmpty())
                .putList("origin", NbtType.FLOAT, boxComponent.originX(), boxComponent.originY(), boxComponent.originZ())
                .putList("size", NbtType.FLOAT, boxComponent.sizeX(), boxComponent.sizeY(), boxComponent.sizeZ())
                .build();
    }
}
