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

package org.geysermc.geyser.api.block.custom.component;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to store components for a custom block or custom block permutation.
 */
public interface CustomBlockComponents {

    /**
     * Gets the selection box component
     * Equivalent to "minecraft:selection_box"
     *
     * @return The selection box.
     */
    @Nullable BoxComponent selectionBox();

    /**
     * Gets the collision box component
     * Equivalent to "minecraft:collision_box"
     * @return The collision box.
     */
    @Nullable BoxComponent collisionBox();

    /**
     * Gets the display name component
     * Equivalent to "minecraft:display_name"
     *
     * @return The display name.
     */
    @Nullable String displayName();

    /**
     * Gets the geometry component
     * Equivalent to "minecraft:geometry"
     *
     * @return The geometry.
     */
    @Nullable GeometryComponent geometry();

    /**
     * Gets the material instances component
     * Equivalent to "minecraft:material_instances"
     *
     * @return The material instances.
     */
    @NonNull Map<String, MaterialInstance> materialInstances();

    /**
     * Gets the placement filter component
     * Equivalent to "minecraft:placement_filter"
     *
     * @return The placement filter.
     */
    @Nullable List<PlacementConditions> placementFilter();

    /**
     * Gets the destructible by mining component
     * Equivalent to "minecraft:destructible_by_mining"
     *
     * @return The destructible by mining value.
     */
    @Nullable Float destructibleByMining();

    /**
     * Gets the friction component
     * Equivalent to "minecraft:friction"
     *
     * @return The friction value.
     */
    @Nullable Float friction();

    /**
     * Gets the light emission component
     * Equivalent to "minecraft:light_emission"
     *
     * @return The light emission value.
     */
    @Nullable Integer lightEmission();

    /**
     * Gets the light dampening component
     * Equivalent to "minecraft:light_dampening"
     *
     * @return The light dampening value.
     */
    @Nullable Integer lightDampening();

    /**
     * Gets the transformation component
     * Equivalent to "minecraft:transformation"
     *
     * @return The transformation.
     */
    @Nullable TransformationComponent transformation();

    /**
     * Gets the unit cube component
     * Equivalent to "minecraft:unit_cube"
     *
     * @deprecated Use {@link #geometry()} and compare with `minecraft:geometry.full_block` instead.
     *
     * @return The rotation.
     */
    @Deprecated
    boolean unitCube();

    /**
     * Gets if the block should place only air
     * Equivalent to setting a dummy event to run on "minecraft:on_player_placing"
     * 
     * @return If the block should place only air.
     */
    boolean placeAir();

    /**
     * Gets the set of tags
     * Equivalent to "tag:some_tag"
     * 
     * @return The set of tags.
     */
    @NonNull Set<String> tags();

    /**
     * Create a Builder for CustomBlockComponents
     *
     * @return A CustomBlockComponents Builder
     */
    static CustomBlockComponents.Builder builder() {
        return GeyserApi.api().provider(CustomBlockComponents.Builder.class);
    }

    interface Builder {
        Builder selectionBox(BoxComponent selectionBox);

        Builder collisionBox(BoxComponent collisionBox);

        Builder displayName(String displayName);

        Builder geometry(GeometryComponent geometry);

        Builder materialInstance(@NonNull String name, @NonNull MaterialInstance materialInstance);

        Builder placementFilter(List<PlacementConditions> placementConditions);

        Builder destructibleByMining(Float destructibleByMining);

        Builder friction(Float friction);

        Builder lightEmission(Integer lightEmission);

        Builder lightDampening(Integer lightDampening);

        Builder transformation(TransformationComponent transformation);

        /**
         * @deprecated Use {@link #geometry(GeometryComponent)} with `minecraft:geometry.full_block` instead.
         */
        @Deprecated
        Builder unitCube(boolean unitCube);

        Builder placeAir(boolean placeAir);

        Builder tags(@Nullable Set<String> tags);

        CustomBlockComponents build();
    }
}
