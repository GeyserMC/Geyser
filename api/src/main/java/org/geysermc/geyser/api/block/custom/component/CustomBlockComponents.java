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

import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to store components for a custom block or custom block permutation.
 * @since 2.2.0
 */
public interface CustomBlockComponents {

    /**
     * Gets the selection box component
     * Equivalent to "minecraft:selection_box"
     *
     * @return the selection box
     * @since 2.2.0
     */
    @Nullable BoxComponent selectionBox();

    /**
     * Gets the collision box component
     * Equivalent to "minecraft:collision_box"
     *
     * @return the collision box
     * @deprecated Use {@link #collisionBoxes()} instead
     * @since 2.2.0
     */
    @Deprecated(since = "2.9.5")
    @Nullable BoxComponent collisionBox();

    /**
     * Gets the collision boxes component.
     * Equivalent to "minecraft:collision_box", which can be either one,
     * none, or up to 16 collision boxes.
     *
     * @return the collision boxes
     * @since 2.9.5
     */
    Set<BoxComponent> collisionBoxes();

    /**
     * Gets the display name component.
     * Equivalent to "minecraft:display_name"
     *
     * @return the display name
     * @since 2.2.0
     */
    @Nullable String displayName();

    /**
     * Gets the geometry component.
     * Equivalent to "minecraft:geometry"
     *
     * @return the geometry
     * @since 2.2.0
     */
    @Nullable GeometryComponent geometry();

    /**
     * Gets the material instances component
     * Equivalent to "minecraft:material_instances"
     *
     * @return the material instances
     * @since 2.2.0
     */
    Map<String, MaterialInstance> materialInstances();

    /**
     * Gets the placement filter component
     * Equivalent to "minecraft:placement_filter"
     *
     * @return the placement filter
     * @since 2.2.0
     */
    @Nullable List<PlacementConditions> placementFilter();

    /**
     * Gets the destructible by mining component
     * Equivalent to "minecraft:destructible_by_mining"
     *
     * @return the destructible by mining value
     * @since 2.2.0
     */
    @Nullable Float destructibleByMining();

    /**
     * Gets the friction component
     * Equivalent to "minecraft:friction"
     *
     * @return the friction value
     * @since 2.2.0
     */
    @Nullable Float friction();

    /**
     * Gets the light emission component
     * Equivalent to "minecraft:light_emission"
     *
     * @return the light emission value
     * @since 2.2.0
     */
    @Nullable Integer lightEmission();

    /**
     * Gets the light dampening component
     * Equivalent to "minecraft:light_dampening"
     *
     * @return the light dampening value
     * @since 2.2.0
     */
    @Nullable Integer lightDampening();

    /**
     * Gets the transformation component
     * Equivalent to "minecraft:transformation"
     *
     * @return the transformation
     * @since 2.2.0
     */
    @Nullable TransformationComponent transformation();

    /**
     * Gets the unit cube component
     * Equivalent to "minecraft:unit_cube"
     *
     * @deprecated Use {@link #geometry()} and compare with `minecraft:geometry.full_block` instead.
     * @return whether this block is a unit cube
     * @since 2.2.0
     */
    @Deprecated(since = "2.2.2")
    boolean unitCube();

    /**
     * Gets if the block should place only air
     * Equivalent to setting a dummy event to run on "minecraft:on_player_placing"
     *
     * @return if the block should place only air
     * @since 2.2.0
     */
    boolean placeAir();

    /**
     * Gets the set of tags
     * Equivalent to "tag:some_tag"
     *
     * @return the set of tags
     * @since 2.2.0
     */
    Set<String> tags();

    /**
     * Create a Builder for CustomBlockComponents
     *
     * @return a {@link CustomBlockComponents.Builder}
     * @since 2.2.0
     */
    static CustomBlockComponents.Builder builder() {
        return GeyserApi.api().provider(CustomBlockComponents.Builder.class);
    }

    interface Builder {
        /**
         * Sets a selection box for the block. Unlike Java Edition, there can only
         * be one selection box.
         *
         * @see CustomBlockComponents#selectionBox()
         * @param selectionBox a selection box, or null for none
         * @return this builder
         * @since 2.2.0
         */
        @This Builder selectionBox(@Nullable BoxComponent selectionBox);

        /**
         * Sets a collision box for the block. Can be null to disable collisions.
         *
         * @deprecated use {@link #collisionBoxes(BoxComponent...)} instead
         * @param collisionBox the collision box to set
         * @return this builder
         * @since 2.2.0
         */
        @Deprecated(since = "2.9.5")
        @This Builder collisionBox(@Nullable BoxComponent collisionBox);

        /**
         * Sets up to 16 different collision boxes for the block. Can be null to disable collisions.
         *
         * @see CustomBlockComponents#collisionBoxes()
         * @param collisionBoxes the collision boxes to set
         * @return this builder
         * @since 2.9.5
         */
        @This Builder collisionBoxes(@Nullable BoxComponent... collisionBoxes);

        /**
         * Convenience method to set collision boxes for the block. Can be null to disable collisions.
         *
         * @see CustomBlockComponents#collisionBoxes()
         * @param collisionBoxes the collection of collision boxes to set
         * @return this builder
         * @since 2.9.5
         */
        @This Builder collisionBoxes(@Nullable Collection<BoxComponent> collisionBoxes);

        /**
         * Sets the display name of the block.
         *
         * @see CustomBlockComponents#displayName()
         * @param displayName the display name to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder displayName(String displayName);

        /**
         * Sets the geometry of the block.
         *
         * @see CustomBlockComponents#geometry()
         * @param geometry the geometry to set
         * @return this builder
         * @since 2.2.0
         */
         @This Builder geometry(GeometryComponent geometry);

        /**
         * Sets the material instances of the block.
         *
         * @see CustomBlockComponents#materialInstances()
         * @param name the name of the material instance
         * @param materialInstance the material instance to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder materialInstance(String name, MaterialInstance materialInstance);

        /**
         * Sets the placement filter of the block.
         *
         * @see CustomBlockComponents#placementFilter()
         * @param placementConditions the placement conditions to set
         * @return this builder
         * @since 2.2.0
         */
         @This Builder placementFilter(List<PlacementConditions> placementConditions);

        /**
         * Sets the destructible by mining value of the block.
         *
         * @see CustomBlockComponents#destructibleByMining()
         * @param destructibleByMining the destructible by mining value to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder destructibleByMining(Float destructibleByMining);

        /**
         * Sets the friction value of the block.
         *
         * @see CustomBlockComponents#friction()
         * @param friction the friction value to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder friction(Float friction);

        /**
         * Sets the light emission value of the block.
         *
         * @see CustomBlockComponents#lightEmission()
         * @param lightEmission the light emission value to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder lightEmission(Integer lightEmission);

        /**
         * Sets the light dampening value of the block.
         *
         * @see CustomBlockComponents#lightDampening()
         * @param lightDampening the light dampening value to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder lightDampening(Integer lightDampening);

        /**
         * Sets the transformation of the block.
         *
         * @see CustomBlockComponents#transformation()
         * @param transformation the transformation to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder transformation(TransformationComponent transformation);

        /**
         * Sets the unit cube value, equivalent to setting a full block geometry.
         *
         * @see CustomBlockComponents#unitCube()
         * @deprecated Use {@link #geometry(GeometryComponent)} with `minecraft:geometry.full_block` instead.
         */
        @Deprecated(since = "2.2.2")
        @This Builder unitCube(boolean unitCube);

        /**
         * Whether the block should place only air, overriding the default behavior.
         *
         * @see CustomBlockComponents#placeAir()
         * @param placeAir whether the block should place only air
         * @return this builder
         * @since 2.2.0
         */
        @This Builder placeAir(boolean placeAir);

        /**
         * Sets the set of tags for the block.
         *
         * @see CustomBlockComponents#tags()
         * @param tags the set of tags to set
         * @return this builder
         * @since 2.2.0
         */
        @This Builder tags(@Nullable Set<String> tags);

        /**
         * Builds these CustomBlockComponents.
         *
         * @return the built CustomBlockComponents
         * @since 2.2.0
         */
        CustomBlockComponents build();
    }
}
