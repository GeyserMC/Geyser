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
package org.geysermc.geyser.api.block.custom.component

import org.checkerframework.common.returnsreceiver.qual.This
import org.geysermc.geyser.api.GeyserApi

/**
 * This class is used to store components for a custom block or custom block permutation.
 * @since 2.2.0
 */
interface CustomBlockComponents {
    /**
     * Gets the selection box component
     * Equivalent to "minecraft:selection_box"
     * 
     * @return the selection box
     * @since 2.2.0
     */
    fun selectionBox(): BoxComponent?

    /**
     * Gets the collision box component
     * Equivalent to "minecraft:collision_box"
     * 
     * @return the collision box
     * @since 2.2.0
     */
    @Deprecated(
        """Use {@link #collisionBoxes()} instead
      """
    )
    fun collisionBox(): BoxComponent?

    /**
     * Gets the collision boxes component.
     * Equivalent to "minecraft:collision_box", which can be either one,
     * none, or up to 16 collision boxes.
     * 
     * @return the collision boxes
     * @since 2.9.5
     */
    fun collisionBoxes(): MutableSet<BoxComponent?>

    /**
     * Gets the display name component.
     * Equivalent to "minecraft:display_name"
     * 
     * @return the display name
     * @since 2.2.0
     */
    fun displayName(): String?

    /**
     * Gets the geometry component.
     * Equivalent to "minecraft:geometry"
     * 
     * @return the geometry
     * @since 2.2.0
     */
    fun geometry(): GeometryComponent?

    /**
     * Gets the material instances component
     * Equivalent to "minecraft:material_instances"
     * 
     * @return the material instances
     * @since 2.2.0
     */
    fun materialInstances(): MutableMap<String?, MaterialInstance?>

    /**
     * Gets the placement filter component
     * Equivalent to "minecraft:placement_filter"
     * 
     * @return the placement filter
     * @since 2.2.0
     */
    fun placementFilter(): MutableList<PlacementConditions?>?

    /**
     * Gets the destructible by mining component
     * Equivalent to "minecraft:destructible_by_mining"
     * 
     * @return the destructible by mining value
     * @since 2.2.0
     */
    fun destructibleByMining(): Float?

    /**
     * Gets the friction component
     * Equivalent to "minecraft:friction"
     * 
     * @return the friction value
     * @since 2.2.0
     */
    fun friction(): Float?

    /**
     * Gets the light emission component
     * Equivalent to "minecraft:light_emission"
     * 
     * @return the light emission value
     * @since 2.2.0
     */
    fun lightEmission(): Int?

    /**
     * Gets the light dampening component
     * Equivalent to "minecraft:light_dampening"
     * 
     * @return the light dampening value
     * @since 2.2.0
     */
    fun lightDampening(): Int?

    /**
     * Gets the transformation component
     * Equivalent to "minecraft:transformation"
     * 
     * @return the transformation
     * @since 2.2.0
     */
    fun transformation(): TransformationComponent?

    /**
     * Gets the unit cube component
     * Equivalent to "minecraft:unit_cube"
     * 
     * @return whether this block is a unit cube
     * @since 2.2.0
     */
    @Deprecated(
        """Use {@link #geometry()} and compare with `minecraft:geometry.full_block` instead.
      """
    )
    fun unitCube(): Boolean

    /**
     * Gets if the block should place only air
     * Equivalent to setting a dummy event to run on "minecraft:on_player_placing"
     * 
     * @return if the block should place only air
     * @since 2.2.0
     */
    fun placeAir(): Boolean

    /**
     * Gets the set of tags
     * Equivalent to "tag:some_tag"
     * 
     * @return the set of tags
     * @since 2.2.0
     */
    fun tags(): MutableSet<String?>

    interface Builder {
        /**
         * Sets a selection box for the block. Unlike Java Edition, there can only
         * be one selection box.
         * 
         * @see CustomBlockComponents.selectionBox
         * @param selectionBox a selection box, or null for none
         * @return this builder
         * @since 2.2.0
         */
        fun selectionBox(selectionBox: BoxComponent?): @This Builder?

        /**
         * Sets a collision box for the block. Can be null to disable collisions.
         * 
         * @param collisionBox the collision box to set
         * @return this builder
         * @since 2.2.0
         */
        @Deprecated(
            """use {@link #collisionBoxes(BoxComponent...)} instead
          """
        )
        fun collisionBox(collisionBox: BoxComponent?): @This Builder?

        /**
         * Sets up to 16 different collision boxes for the block. Can be null to disable collisions.
         * 
         * @see CustomBlockComponents.collisionBoxes
         * @param collisionBoxes the collision boxes to set
         * @return this builder
         * @since 2.9.5
         */
        fun collisionBoxes(vararg collisionBoxes: BoxComponent?): @This Builder?

        /**
         * Convenience method to set collision boxes for the block. Can be null to disable collisions.
         * 
         * @see CustomBlockComponents.collisionBoxes
         * @param collisionBoxes the collection of collision boxes to set
         * @return this builder
         * @since 2.9.5
         */
        fun collisionBoxes(collisionBoxes: MutableCollection<BoxComponent?>?): @This Builder?

        /**
         * Sets the display name of the block.
         * 
         * @see CustomBlockComponents.displayName
         * @param displayName the display name to set
         * @return this builder
         * @since 2.2.0
         */
        fun displayName(displayName: String?): @This Builder?

        /**
         * Sets the geometry of the block.
         * 
         * @see CustomBlockComponents.geometry
         * @param geometry the geometry to set
         * @return this builder
         * @since 2.2.0
         */
        fun geometry(geometry: GeometryComponent?): @This Builder?

        /**
         * Sets the material instances of the block.
         * 
         * @see CustomBlockComponents.materialInstances
         * @param name the name of the material instance
         * @param materialInstance the material instance to set
         * @return this builder
         * @since 2.2.0
         */
        fun materialInstance(name: String, materialInstance: MaterialInstance): @This Builder?

        /**
         * Sets the placement filter of the block.
         * 
         * @see CustomBlockComponents.placementFilter
         * @param placementConditions the placement conditions to set
         * @return this builder
         * @since 2.2.0
         */
        fun placementFilter(placementConditions: MutableList<PlacementConditions?>?): @This Builder?

        /**
         * Sets the destructible by mining value of the block.
         * 
         * @see CustomBlockComponents.destructibleByMining
         * @param destructibleByMining the destructible by mining value to set
         * @return this builder
         * @since 2.2.0
         */
        fun destructibleByMining(destructibleByMining: Float?): @This Builder?

        /**
         * Sets the friction value of the block.
         * 
         * @see CustomBlockComponents.friction
         * @param friction the friction value to set
         * @return this builder
         * @since 2.2.0
         */
        fun friction(friction: Float?): @This Builder?

        /**
         * Sets the light emission value of the block.
         * 
         * @see CustomBlockComponents.lightEmission
         * @param lightEmission the light emission value to set
         * @return this builder
         * @since 2.2.0
         */
        fun lightEmission(lightEmission: Int?): @This Builder?

        /**
         * Sets the light dampening value of the block.
         * 
         * @see CustomBlockComponents.lightDampening
         * @param lightDampening the light dampening value to set
         * @return this builder
         * @since 2.2.0
         */
        fun lightDampening(lightDampening: Int?): @This Builder?

        /**
         * Sets the transformation of the block.
         * 
         * @see CustomBlockComponents.transformation
         * @param transformation the transformation to set
         * @return this builder
         * @since 2.2.0
         */
        fun transformation(transformation: TransformationComponent?): @This Builder?

        /**
         * Sets the unit cube value, equivalent to setting a full block geometry.
         * 
         * @see CustomBlockComponents.unitCube
         */
        @Deprecated("Use {@link #geometry(GeometryComponent)} with `minecraft:geometry.full_block` instead.")
        fun unitCube(unitCube: Boolean): @This Builder?

        /**
         * Whether the block should place only air, overriding the default behavior.
         * 
         * @see CustomBlockComponents.placeAir
         * @param placeAir whether the block should place only air
         * @return this builder
         * @since 2.2.0
         */
        fun placeAir(placeAir: Boolean): @This Builder?

        /**
         * Sets the set of tags for the block.
         * 
         * @see CustomBlockComponents.tags
         * @param tags the set of tags to set
         * @return this builder
         * @since 2.2.0
         */
        fun tags(tags: MutableSet<String?>?): @This Builder?

        /**
         * Builds these CustomBlockComponents.
         * 
         * @return the built CustomBlockComponents
         * @since 2.2.0
         */
        fun build(): CustomBlockComponents?
    }

    companion object {
        /**
         * Create a Builder for CustomBlockComponents
         * 
         * @return a [CustomBlockComponents.Builder]
         * @since 2.2.0
         */
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
