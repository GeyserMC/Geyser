/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.mappings;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.block.custom.property.PropertyType;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BuiltInMappings {
    public static void registerBlocks(GeyserDefineCustomBlocksEvent event) {
        if (!GeyserImpl.getInstance().config().gameplay().enableIntegratedPack()) {
            return;
        }

        // Obsidian and crying obsidian mining speed differ from Java
        registerBlock(event, CustomBlockData.builder()
            .name("obsidian")
            .components(CustomBlockComponents.builder()
                .tags(Set.of("is_pickaxe_item_destructible", "diamond_tier_destructible"))
                .destructibleByMining(50.0f)
                .build())
            .build(), false, null);
        registerBlock(event, CustomBlockData.builder()
            .name("crying_obsidian")
            .components(CustomBlockComponents.builder()
                .tags(Set.of("is_pickaxe_item_destructible", "diamond_tier_destructible"))
                .destructibleByMining(50.0f)
                .build())
            .build(), false, null);

        // Bedrock only has brown_mushroom_block and red_mushroom_block with 16 different variants (nine-slice + stem) but Java allows to toggle each face individually
        registerBlock(event, CustomBlockData.builder()
            .name("brown_mushroom_block")
            .components(CustomBlockComponents.builder()
                .destructibleByMining(0.2f)
                .build())
            .booleanProperty("down")
            .booleanProperty("east")
            .booleanProperty("north")
            .booleanProperty("south")
            .booleanProperty("up")
            .booleanProperty("west")
            .build(), false, mushroomComponentsFromState("mushroom_block_inside", "mushroom_block_skin_brown"));
        registerBlock(event, CustomBlockData.builder()
            .name("red_mushroom_block")
            .components(CustomBlockComponents.builder()
                .destructibleByMining(0.2f)
                .build())
            .booleanProperty("down")
            .booleanProperty("east")
            .booleanProperty("north")
            .booleanProperty("south")
            .booleanProperty("up")
            .booleanProperty("west")
            .build(), false, mushroomComponentsFromState("mushroom_block_inside", "mushroom_block_skin_red"));
        registerBlock(event, CustomBlockData.builder()
            .name("mushroom_stem")
            .components(CustomBlockComponents.builder()
                .destructibleByMining(0.2f)
                .build())
            .booleanProperty("down")
            .booleanProperty("east")
            .booleanProperty("north")
            .booleanProperty("south")
            .booleanProperty("up")
            .booleanProperty("west")
            .build(), false, mushroomComponentsFromState("mushroom_block_inside", "mushroom_block_skin_stem"));

        // Test blocks don't exist on Bedrock
        registerBlock(event, CustomBlockData.builder()
            .name("test_block")
            .components(CustomBlockComponents.builder()
                .destructibleByMining(Float.MAX_VALUE)
                .build())
            .stringProperty("mode", List.of("start", "log", "fail", "accept"))
            .build(), true, null);
        registerBlock(event, CustomBlockData.builder()
            .name("test_instance_block")
            .components(CustomBlockComponents.builder()
                .destructibleByMining(Float.MAX_VALUE)
                .build())
            .build(), true, null);
    }

    public static void registerItems(GeyserDefineCustomItemsEvent event) {
        // Furnace minecarts don't exist on Bedrock
        event.register(Identifier.of("furnace_minecart"), CustomItemDefinition.builder(Identifier.of("geysermc", "furnace_minecart"), Identifier.of("furnace_minecart"))
                .displayName("item.minecartFurnace.name")
                .bedrockOptions(CustomItemBedrockOptions.builder()
                    .icon("minecart_furnace")
                    .creativeCategory(CreativeCategory.ITEMS)
                    .creativeGroup("itemGroup.name.minecart"))
            .build());
    }

    private static void registerBlock(GeyserDefineCustomBlocksEvent event, CustomBlockData block, boolean item, @Nullable Function<CustomBlockState, CustomBlockComponents> permutationsMapper) {
        List<Map.Entry<String, CustomBlockProperty<?>>> properties = block.properties().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();

        int propertyCount = properties.size();
        List<List<?>> propertyValues = new ArrayList<>(propertyCount);
        int statesCount = 1;

        for (Map.Entry<String, CustomBlockProperty<?>> entry : properties) {
            propertyValues.add(entry.getValue().values());
            statesCount *= entry.getValue().values().size();
        }

        int[] propertyIndices = new int[propertyCount];

        if (permutationsMapper != null) {
            CustomBlockData.Builder builder = CustomBlockData.builder()
                .name(block.name())
                .includedInCreativeInventory(block.includedInCreativeInventory())
                .creativeCategory(block.creativeCategory())
                .creativeGroup(block.creativeGroup())
                .components(block.components());

            for (CustomBlockProperty<?> property : block.properties().values()) {
                if (property.type() == PropertyType.booleanProp()) {
                    builder.booleanProperty(property.name());
                } else if (property.type() == PropertyType.integerProp()) {
                    builder.intProperty(property.name(), (List<Integer>) property.values());
                } else if (property.type() == PropertyType.stringProp()) {
                    builder.stringProperty(property.name(), (List<String>) property.values());
                }
            }

            List<CustomBlockPermutation> permutations = new ArrayList<>(statesCount);

            for (int stateIndex = 0; stateIndex < statesCount; stateIndex++) {
                CustomBlockState.Builder stateBuilder = block.blockStateBuilder();

                StringBuilder conditionBuilder = new StringBuilder();

                for (int propertyIndex = 0; propertyIndex < propertyCount; propertyIndex++) {
                    String name = properties.get(propertyIndex).getKey();
                    Object value = propertyValues.get(propertyIndex).get(propertyIndices[propertyIndex]);

                    if (propertyIndex != 0) {
                        conditionBuilder.append("&&");
                    }

                    if (value instanceof Byte booleanValue) {
                        stateBuilder.booleanProperty(name, booleanValue != 0);
                        if (booleanValue == 0) {
                            conditionBuilder.append('!');
                        }
                        conditionBuilder.append("query.block_property('").append(name).append("')");

                    } else if (value instanceof Integer integerValue) {
                        stateBuilder.intProperty(name, integerValue);
                        conditionBuilder.append("query.block_property('").append(name).append("')==").append(integerValue);
                    } else if (value instanceof String stringValue) {
                        stateBuilder.stringProperty(name, stringValue);
                        conditionBuilder.append("query.block_property('").append(name).append("')=='").append(stringValue).append("'");
                    }
                }

                permutations.add(new CustomBlockPermutation(permutationsMapper.apply(stateBuilder.build()), conditionBuilder.toString()));

                for (int propertyIndex = propertyCount - 1; propertyIndex >= 0; propertyIndex--) {
                    propertyIndices[propertyIndex]++;

                    if (propertyIndices[propertyIndex] < propertyValues.get(propertyIndex).size()) {
                        break;
                    }

                    propertyIndices[propertyIndex] = 0;
                }
            }

            block = builder.permutations(permutations).build();
        }

        event.register(block);
        if (item) {
            event.registerItemOverride("minecraft:" + block.name(), block);
        }

        propertyIndices = new int[propertyCount];

        for (int stateIndex = 0; stateIndex < statesCount; stateIndex++) {
            CustomBlockState.Builder stateBuilder = block.blockStateBuilder();
            StringBuilder javaStateBuilder = new StringBuilder("minecraft:");
            javaStateBuilder.append(block.name());
            if (propertyCount != 0) {
                javaStateBuilder.append('[');
            }

            for (int propertyIndex = 0; propertyIndex < propertyCount; propertyIndex++) {
                String name = properties.get(propertyIndex).getKey();
                Object value = propertyValues.get(propertyIndex).get(propertyIndices[propertyIndex]);

                if (propertyIndex != 0) {
                    javaStateBuilder.append(',');
                }
                javaStateBuilder.append(name).append('=');

                if (value instanceof Byte booleanValue) {
                    stateBuilder.booleanProperty(name, booleanValue != 0);
                    javaStateBuilder.append(booleanValue != 0);
                } else if (value instanceof Integer integerValue) {
                    stateBuilder.intProperty(name, integerValue);
                    javaStateBuilder.append(integerValue);
                } else if (value instanceof String stringValue) {
                    stateBuilder.stringProperty(name, stringValue);
                    javaStateBuilder.append(stringValue);
                }
            }

            if (propertyCount != 0) {
                javaStateBuilder.append(']');
            }

            event.registerOverride(javaStateBuilder.toString(), stateBuilder.build());

            for (int propertyIndex = propertyCount - 1; propertyIndex >= 0; propertyIndex--) {
                propertyIndices[propertyIndex]++;

                if (propertyIndices[propertyIndex] < propertyValues.get(propertyIndex).size()) {
                    break;
                }

                propertyIndices[propertyIndex] = 0;
            }
        }
    }

    private static Function<CustomBlockState, CustomBlockComponents> mushroomComponentsFromState(
        String insideTexture,
        String outsideTexture
    ) {
        return (state) -> {
            int outsideCount = 0;
            for (Object value : state.properties().values()) {
                boolean outside = (byte) value != 0;
                if (outside) {
                    outsideCount++;
                }
            }
            boolean fallbackOutside = outsideCount > 3;

            CustomBlockComponents.Builder components = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder()
                    .identifier("minecraft:geometry.full_block")
                    .build())
                .materialInstance(
                    "*",
                    MaterialInstance.builder().texture(fallbackOutside ? outsideTexture : insideTexture).build()
                );

            for (Map.Entry<String, Object> entry : state.properties().entrySet()) {
                boolean outside = (byte) entry.getValue() != 0;
                if (outside != fallbackOutside) {
                    components.materialInstance(
                        entry.getKey(),
                        MaterialInstance.builder().texture(outside ? outsideTexture : insideTexture).build()
                    );
                }
            }

            return components.build();
        };
    }
}
