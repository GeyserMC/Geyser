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

/**
 * This class is used to store a custom block state, which contains CustomBlockData
 * tied to defined properties and values
 */
interface CustomBlockState {
    /**
     * Gets the custom block data associated with the state
     * 
     * @return The custom block data for the state.
     */
    fun block(): CustomBlockData

    /**
     * Gets the name of the state
     * 
     * @return The name of the state.
     */
    fun name(): String

    /**
     * Gets the given property for the state
     * 
     * @param propertyName the property name
     * @return the boolean, int, or string property.
     */
    fun <T> property(propertyName: String): T

    /**
     * Gets a map of the properties for the state
     * 
     * @return The properties for the state.
     */
    fun properties(): MutableMap<String?, Any?>

    interface Builder {
        fun booleanProperty(propertyName: String, value: Boolean): Builder?

        fun intProperty(propertyName: String, value: Int): Builder?

        fun stringProperty(propertyName: String, value: String): Builder?

        fun build(): CustomBlockState?
    }
}
