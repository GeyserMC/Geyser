/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.populator.conversion;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.bedrock.BedrockBlock;
import com.gardensmc.gardensfurniture.custom.item.BedrockBlockState;
import com.gardensmc.gardensfurniture.custom.item.CustomCaveVineItem;
import com.gardensmc.gardensfurniture.custom.item.CustomDirectionalItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import it.unimi.dsi.fastutil.Pair;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.TransformationComponent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class FurnitureItemConverter {

    public static Map<String, CustomBlockData> CUSTOM_NAME_TO_BLOCK = new HashMap<>();
    public static Map<String, CustomBlockData> ITEM_MODEL_TO_BLOCK_DATA = new HashMap<>();

    public static List<CustomBlockData> buildGeyserCustomBlocks() {
//        var componentBuilder = CustomBlockComponents.builder()
//            .geometry(GeometryComponent.builder()
//                .identifier("minecraft:geometry.full_block")
//                .build())
//            .materialInstance("north", MaterialInstance.builder().texture("maple_log_side").renderMethod("alpha_test").build())
//            .materialInstance("east", MaterialInstance.builder().texture("maple_log_side").renderMethod("alpha_test").build())
//            .materialInstance("south", MaterialInstance.builder().texture("maple_log_side").renderMethod("alpha_test").build())
//            .materialInstance("west", MaterialInstance.builder().texture("maple_log_side").renderMethod("alpha_test").build())
//            .materialInstance("up", MaterialInstance.builder().texture("maple_log_top").renderMethod("alpha_test").build())
//            .materialInstance("down", MaterialInstance.builder().texture("maple_log_top").renderMethod("alpha_test").build())
//            .destructibleByMining(Float.MAX_VALUE)
//            .placeAir(true);
//        var customBlock = new GeyserCustomBlockData.Builder()
//            .name("maple_log")
//            .stringProperty("axis", List.of("x", "y", "z"))
//            .permutations(List.of(
//                new CustomBlockPermutation(
//                    CustomBlockComponents.builder()
//                        .transformation(new TransformationComponent(90, 90, 0))
//                        .build(),
//                    "q.block_property('axis') == 'x'"
//                ),
//                new CustomBlockPermutation(
//                    CustomBlockComponents.builder()
//                        .transformation(new TransformationComponent(90, 0, 0))
//                        .build(),
//                    "q.block_property('axis') == 'z'"
//                )
//            ))
//            .components(componentBuilder.build())
//            .build();
//        CUSTOM_NAME_TO_BLOCK.put("maple_log", customBlock);
//        return List.of(
//            customBlock
//        );


        return GardensFurniture.itemRegistry.getCustomItems().values()
            .stream()
            .filter(customItem -> customItem instanceof CustomPlaceableItem)
            .map(placeable -> toGeyserBlock((CustomPlaceableItem) placeable))
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    private static CustomBlockData toGeyserBlock(CustomPlaceableItem customItem) {
        var bedrockBlock = customItem.getBedrockBlock();
        if (bedrockBlock == null) {
            return null;
        }
        var customBlockData = buildGeyserBlock(customItem, bedrockBlock);
        var itemModel = customItem.getComponents() == null ? null : customItem.getComponents().getItem_model();
        if (itemModel != null) {
            ITEM_MODEL_TO_BLOCK_DATA.put(itemModel, customBlockData);
        }
        CUSTOM_NAME_TO_BLOCK.put(customItem.getIdentifier(), customBlockData);
        return customBlockData;
    }

//    private static CustomBlockState buildCustomBlockState() {
//        new CustomBlockState
//    }

    private static CustomBlockData buildGeyserBlock(CustomPlaceableItem placeableItem, BedrockBlock bedrockBlock) {
        var blockData = new GeyserCustomBlockData.Builder()
            .name(placeableItem.getIdentifier())
            .components(bedrockBlock.toCustomBlockComponents());
        placeableItem.onBuildGeyserBlockData(blockData);
        blockData.permutations(placeableItem.getPermutations());
        return blockData.build();
    }

    public static void registerItems(BiConsumer<String, CustomItemData> register) {
        GardensFurniture.itemRegistry.getCustomItems().values().forEach(customItem -> {
            var customItemData = buildCustomItemData(customItem);
            if (customItemData != null) {
                register.accept("minecraft:" + customItem.getMaterialName().toLowerCase(), customItemData);
            }
        });
    }

    private static CustomItemData buildCustomItemData(CustomItem customItem) {
        if (customItem.getComponents() == null) {
            return null;
        }
        var itemModel = customItem.getComponents().getItem_model();
        if (itemModel == null) {
            return null;
        }
        return CustomItemData.builder()
            .name(customItem.getIdentifier())
            .customItemOptions(CustomItemOptions.builder()
                .itemModel(itemModel).build()
            )
            .build();
    }
}
