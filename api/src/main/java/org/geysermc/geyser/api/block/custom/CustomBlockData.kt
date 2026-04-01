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
package org.geysermc.geyser.api.block.custom

import org.geysermc.geyser.api.GeyserApi
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty
import org.geysermc.geyser.api.util.CreativeCategory

/**
 * This class is used to store data for a custom block.
 */
interface CustomBlockData {
    /**
     * Gets the name of the custom block
     * 
     * @return The name of the custom block.
     */
    fun name(): String

    /**
     * Gets the identifier of the custom block
     * 
     * @return The identifier of the custom block.
     */
    fun identifier(): String

    /**
     * Gets if the custom block is included in the creative inventory
     * 
     * @return If the custom block is included in the creative inventory.
     */
    fun includedInCreativeInventory(): Boolean

    /**
     * Gets the block's creative category, or tab id.
     * 
     * @return the block's creative category
     */
    fun creativeCategory(): CreativeCategory?

    /**
     * Gets the block's creative group.
     * 
     * @return the block's creative group
     */
    fun creativeGroup(): String?

    /**
     * Gets the components of the custom block
     * 
     * @return The components of the custom block.
     */
    fun components(): CustomBlockComponents?

    /**
     * Gets the custom block's map of block property names to CustomBlockProperty
     * objects
     * 
     * @return The custom block's map of block property names to CustomBlockProperty objects.
     */
    fun properties(): MutableMap<String?, CustomBlockProperty<*>?>

    /**
     * Gets the list of the custom block's permutations
     * 
     * @return The permutations of the custom block.
     */
    fun permutations(): MutableList<CustomBlockPermutation?>

    /**
     * Gets the custom block's default block state
     * 
     * @return The default block state of the custom block.
     */
    fun defaultBlockState(): CustomBlockState

    /**
     * Gets a builder for a custom block state
     * 
     * @return The builder for a custom block state.
     */
    fun blockStateBuilder(): CustomBlockState.Builder

    interface Builder {
        fun name(name: String): Builder?

        fun includedInCreativeInventory(includedInCreativeInventory: Boolean): Builder?

        fun creativeCategory(creativeCategory: CreativeCategory?): Builder?

        fun creativeGroup(creativeGroup: String?): Builder?

        fun components(components: CustomBlockComponents): Builder?

        fun booleanProperty(propertyName: String): Builder?

        fun intProperty(propertyName: String, values: MutableList<Int?>?): Builder?

        fun stringProperty(propertyName: String, values: MutableList<String?>?): Builder?

        fun permutations(permutations: MutableList<CustomBlockPermutation?>): Builder?

        fun build(): CustomBlockData?
    }

    companion object {
        /**
         * Create a Builder for CustomBlockData
         * 
         * @return A CustomBlockData Builder
         */
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
