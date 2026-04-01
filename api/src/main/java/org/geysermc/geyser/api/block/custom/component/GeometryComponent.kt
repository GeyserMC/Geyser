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
package org.geysermc.geyser.api.block.custom.component

import org.geysermc.geyser.api.GeyserApi

/**
 * This class is used to store data for a geometry component.
 */
interface GeometryComponent {
    /**
     * Gets the identifier of the geometry
     * 
     * @return The identifier of the geometry.
     */
    fun identifier(): String

    /**
     * Gets the bone visibility of the geometry
     * 
     * @return The bone visibility of the geometry.
     */
    fun boneVisibility(): MutableMap<String?, String?>?

    interface Builder {
        fun identifier(identifier: String): Builder?

        fun boneVisibility(boneVisibility: MutableMap<String?, String?>?): Builder?

        fun build(): GeometryComponent?
    }

    companion object {
        /**
         * Creates a builder for GeometryComponent
         * 
         * @return a builder for GeometryComponent.
         */
        @kotlin.jvm.JvmStatic
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
