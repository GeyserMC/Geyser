/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.api.util.CreativeCategory

/**
 * Represents a completely custom block that is not based on an existing vanilla Minecraft block.
 */
interface NonVanillaCustomBlockData : CustomBlockData {
    /**
     * Gets the namespace of the custom block
     * 
     * @return The namespace of the custom block.
     */
    fun namespace(): String


    interface Builder : CustomBlockData.Builder {
        fun namespace(namespace: String): Builder?

        override fun name(name: String): Builder?

        override fun includedInCreativeInventory(includedInCreativeInventory: Boolean): Builder?

        override fun creativeCategory(creativeCategory: CreativeCategory?): Builder?

        override fun creativeGroup(creativeGroup: String?): Builder?

        override fun components(components: CustomBlockComponents): Builder?

        override fun booleanProperty(propertyName: String): Builder?

        override fun intProperty(propertyName: String, values: MutableList<Int?>?): Builder?

        override fun stringProperty(propertyName: String, values: MutableList<String?>?): Builder?

        override fun permutations(permutations: MutableList<CustomBlockPermutation?>): Builder?

        override fun build(): NonVanillaCustomBlockData?
    }

    companion object {
        /**
         * Create a Builder for NonVanillaCustomBlockData
         * 
         * @return A NonVanillaCustomBlockData Builder
         */
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
