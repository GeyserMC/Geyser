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

import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class BuiltInMappings {
    private static final String[] FACES = {
        "down",
        "east",
        "north",
        "south",
        "up",
        "west"
    };

    public static void registerBlocks(GeyserDefineCustomBlocksEvent event) {
        CustomBlockData obsidianBlockData = CustomBlockData.builder()
            .name("obsidian")
            .components(CustomBlockComponents.builder()
                .tags(Set.of("is_pickaxe_item_destructible", "diamond_tier_destructible"))
                .destructibleByMining(50.0f)
                .build())
            .build();
        event.register(obsidianBlockData);
        event.registerOverride("obsidian", obsidianBlockData.defaultBlockState());

        CustomBlockData cryingObsidianBlockData = CustomBlockData.builder()
            .name("crying_obsidian")
            .components(CustomBlockComponents.builder()
                .tags(Set.of("is_pickaxe_item_destructible", "diamond_tier_destructible"))
                .destructibleByMining(50.0f)
                .build())
            .build();
        event.register(obsidianBlockData);
        event.registerOverride("crying_obsidian", cryingObsidianBlockData.defaultBlockState());

        registerMushroomBlock(event, "brown_mushroom_block", "mushroom_block_inside", "mushroom_block_skin_brown");
        registerMushroomBlock(event, "red_mushroom_block", "mushroom_block_inside", "mushroom_block_skin_red");
        registerMushroomBlock(event, "mushroom_stem", "mushroom_block_inside", "mushroom_block_skin_stem");

        CustomBlockData testBlock = CustomBlockData.builder()
            .name("test_block")
            .build();
        event.register(testBlock);
        event.registerOverride("test_block", testBlock.defaultBlockState());

        CustomBlockData testInstanceBlock = CustomBlockData.builder()
            .name("test_instance_block")
            .build();
        event.register(testInstanceBlock);
        event.registerOverride("test_instance_block", testInstanceBlock.defaultBlockState());
    }

    public static void registerItems(GeyserDefineCustomItemsEvent event) {
        event.register(Identifier.of("furnace_minecart"), CustomItemDefinition.builder(Identifier.of("furnace_minecart"), Identifier.of("furnace_minecart"))
            .build());
    }

    private static void registerMushroomBlock(
        GeyserDefineCustomBlocksEvent event,
        String name,
        String insideTexture,
        String outsideTexture
    ) {
        int permutationCount = 1 << FACES.length;
        List<CustomBlockPermutation> permutations = new ArrayList<>(permutationCount);
        for (int bits = 0; bits < permutationCount; bits++) {
            int outsideCount = Integer.bitCount(bits);
            int insideCount = FACES.length - outsideCount;

            boolean fallbackOutside = outsideCount > insideCount;
            String fallbackTexture = fallbackOutside ? outsideTexture : insideTexture;

            CustomBlockComponents.Builder components = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder()
                    .identifier("geometry.full_block")
                    .build())
                .materialInstance("*", MaterialInstance.builder().texture(fallbackTexture).build());

            for (int i = 0; i < FACES.length; i++) {
                boolean faceOutside = (bits & (1 << i)) != 0;
                if (faceOutside != fallbackOutside) {
                    components.materialInstance(
                        FACES[i],
                        MaterialInstance.builder().texture(faceOutside ? outsideTexture : insideTexture).build()
                    );
                }
            }

            permutations.add(new CustomBlockPermutation(
                components.build(),
                "query.block_property('bits') == " + bits
            ));
        }

        CustomBlockData block = CustomBlockData.builder()
            .name(name)
            .components(CustomBlockComponents.builder()
                .destructibleByMining(0.2f)
                .build())
            .intProperty("bits", IntStream.range(0, permutationCount).boxed().toList())
            .permutations(permutations)
            .build();
        event.register(block);

        for (int bits = 0; bits < permutationCount; bits++) {
            StringBuilder javaIdentifier = new StringBuilder(name + "[");
            for (int i = 0; i < FACES.length; i++) {
                if (i > 0) javaIdentifier.append(",");
                javaIdentifier.append(FACES[i]).append("=").append((bits & (1 << i)) != 0);
            }
            javaIdentifier.append("]");

            event.registerOverride(
                javaIdentifier.toString(),
                block.blockStateBuilder()
                    .intProperty("bits", bits)
                    .build()
            );
        }
    }
}
