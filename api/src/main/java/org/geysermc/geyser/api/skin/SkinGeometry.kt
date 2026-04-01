/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.api.skin

/**
 * Represents geometry of a skin.
 * 
 * @param geometryName The name of the geometry (JSON)
 * @param geometryData The geometry data (JSON)
 */
@kotlin.jvm.JvmRecord
data class SkinGeometry(@kotlin.jvm.JvmField val geometryName: String?, @kotlin.jvm.JvmField val geometryData: String?) {
    companion object {
        @kotlin.jvm.JvmField
        var WIDE: SkinGeometry = getLegacy(false)
        @kotlin.jvm.JvmField
        var SLIM: SkinGeometry = getLegacy(true)

        /**
         * Generate generic geometry
         * 
         * @param isSlim if true, it will be the slimmer alex model
         * @return The generic geometry object
         */
        private fun getLegacy(isSlim: Boolean): SkinGeometry {
            return SkinGeometry(
                "{\"geometry\" :{\"default\" :\"geometry.humanoid.custom" + (if (isSlim) "Slim" else "") + "\"}}",
                ""
            )
        }
    }
}
