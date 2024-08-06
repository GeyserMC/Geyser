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

package org.geysermc.geyser.registry.type;

import lombok.Data;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.TransformationComponent;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockData;
import org.geysermc.geyser.level.block.GeyserGeometryComponent;
import org.geysermc.geyser.level.block.GeyserMaterialInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Data
public class CustomSkull {
    private final String skinHash;

    private CustomBlockData customBlockData;

    private static final String BITS_A_PROPERTY = "geyser_skull:bits_a";
    private static final String BITS_B_PROPERTY = "geyser_skull:bits_b";

    private static final int[] ROTATIONS = {0, -90, 180, 90};

    private static final BoxComponent FLOOR_BOX = new BoxComponent(
            -4, 0, -4,
            8, 8, 8
    );

    private static final BoxComponent WALL_BOX = new BoxComponent(
            -4, 4, 0,
            8, 8, 8
    );

    public CustomSkull(String skinHash) {
        this.skinHash = skinHash;

        CustomBlockComponents components = new GeyserCustomBlockComponents.Builder()
                .destructibleByMining(1.5f)
                .materialInstance("*", new GeyserMaterialInstance.Builder()
                        .texture("geyser." + skinHash + "_player_skin")
                        .renderMethod("alpha_test")
                        .faceDimming(true)
                        .ambientOcclusion(true)
                        .build())
                .lightDampening(0)
                .placeAir(true)
                .build();

        List<CustomBlockPermutation> permutations = new ArrayList<>();
        addDefaultPermutation(permutations);
        addFloorPermutations(permutations);
        addWallPermutations(permutations);

        customBlockData = new GeyserCustomBlockData.Builder()
                .name("player_skull_" + skinHash)
                .components(components)
                .intProperty(BITS_A_PROPERTY, IntStream.rangeClosed(0, 6).boxed().toList()) // This gives us exactly 21 block states
                .intProperty(BITS_B_PROPERTY, IntStream.rangeClosed(0, 2).boxed().toList())
                .permutations(permutations)
                .build();
    }

    public CustomBlockState getWallBlockState(int wallDirection) {
        wallDirection = switch (wallDirection) {
            case 0 -> 2; // South
            case 90 -> 3; // West
            case 180 -> 0; // North
            case 270 -> 1; // East
            default -> throw new IllegalArgumentException("Unknown skull wall direction: " + wallDirection);
        };

        return customBlockData.blockStateBuilder()
                .intProperty(BITS_A_PROPERTY, wallDirection + 1)
                .intProperty(BITS_B_PROPERTY, 0)
                .build();
    }

    public CustomBlockState getFloorBlockState(int floorRotation) {
        return customBlockData.blockStateBuilder()
                .intProperty(BITS_A_PROPERTY, (5 + floorRotation) % 7)
                .intProperty(BITS_B_PROPERTY, (5 + floorRotation) / 7)
                .build();
    }

    private void addDefaultPermutation(List<CustomBlockPermutation> permutations) {
        CustomBlockComponents components = new GeyserCustomBlockComponents.Builder()
                .geometry(new GeyserGeometryComponent.Builder()
                        .identifier("geometry.geyser.player_skull_hand")
                        .build())
                .transformation(new TransformationComponent(0, 180, 0))
                .build();

        String condition = String.format("query.block_property('%s') == 0 && query.block_property('%s') == 0", BITS_A_PROPERTY, BITS_B_PROPERTY);
        permutations.add(new CustomBlockPermutation(components, condition));
    }

    private void addFloorPermutations(List<CustomBlockPermutation> permutations) {
        String[] quadrantNames = {"a", "b", "c", "d"};

        for (int quadrant = 0; quadrant < 4; quadrant++) {
            for (int i = 0; i < 4; i++) {
                int floorRotation = 4 * quadrant + i;
                CustomBlockComponents components = new GeyserCustomBlockComponents.Builder()
                        .selectionBox(FLOOR_BOX)
                        .collisionBox(FLOOR_BOX)
                        .geometry(new GeyserGeometryComponent.Builder()
                                .identifier("geometry.geyser.player_skull_floor_" + quadrantNames[i])
                                .build())
                        .transformation(new TransformationComponent(0, ROTATIONS[quadrant], 0))
                        .build();

                int bitsA = (5 + floorRotation) % 7;
                int bitsB = (5 + floorRotation) / 7;
                String condition = String.format("query.block_property('%s') == %d && query.block_property('%s') == %d", BITS_A_PROPERTY, bitsA, BITS_B_PROPERTY, bitsB);
                permutations.add(new CustomBlockPermutation(components, condition));
            }
        }
    }

    private void addWallPermutations(List<CustomBlockPermutation> permutations) {
        for (int i = 0; i < 4; i++) {
            CustomBlockComponents components = new GeyserCustomBlockComponents.Builder()
                    .selectionBox(WALL_BOX)
                    .collisionBox(WALL_BOX)
                    .geometry(new GeyserGeometryComponent.Builder()
                            .identifier("geometry.geyser.player_skull_wall")
                            .build())
                    .transformation(new TransformationComponent(0, ROTATIONS[i], 0))
                    .build();

            String condition = String.format("query.block_property('%s') == %d && query.block_property('%s') == %d", BITS_A_PROPERTY, i + 1, BITS_B_PROPERTY, 0);
            permutations.add(new CustomBlockPermutation(components, condition));
        }
    }
}
